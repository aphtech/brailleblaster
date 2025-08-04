/*
 * Copyright (C) 2025 American Printing House for the Blind
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.brailleblaster.perspectives.mvc.modules.misc

import com.google.common.util.concurrent.MoreExecutors
import org.apache.commons.lang3.exception.ExceptionUtils
import org.brailleblaster.BBIni
import org.brailleblaster.userHelp.Project
import org.brailleblaster.userHelp.javaVersion
import org.brailleblaster.userHelp.oSVersion
import org.brailleblaster.util.Utils.httpPost
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.ConnectException
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Consumer

object ExceptionReportingModule /*implements SimpleListener*/ {
    private const val ERROR_SUFFIX: String = ", please contact bb-support@tech.aph.org"
    private val log: Logger = LoggerFactory.getLogger(ExceptionReportingModule::class.java)
    private val DEFAULT_REPORTING_VALUE: ExceptionReportingLevel = ExceptionReportingLevel.USER_FRIENDLY
    private val DEFAULT_RECOVERY_VALUE: ExceptionRecoveryLevel = ExceptionRecoveryLevel.RECOVER
    private const val EXCEPTION_LEVEL_PROPERTY: String = "exceptionLevel"
    private const val EXCEPTION_RECOVERY_PROPERTY: String = "exceptionRecovery"
    private val REPORTER_THREAD: ExecutorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool())

    /**
     * ABSOLUTELY DO NOT USE THIS SETTING OUTSIDE THIS CLASS.
     *
     * @see .isAutoUploadEnabledOrNull
     */
    private const val USER_SETTING_AUTO_UPLOAD = "exceptionReporter.autoUpload"

    private const val USER_SETTING_REPORT_UUID: String = "notify.reportUuid"
    private val AUTO_REPORT_INSTALLER_CONFIG = BBIni.programDataPath.resolve(Paths.get("settings", "reportErrors")).toFile()
    private const val ERROR_REPORT_ADDRESS: String = "https://brailleblaster.org/errorReport/?v=2"

    var exceptionReportingLevel: ExceptionReportingLevel
        get() {
            val propertyValue = BBIni.propertyFileManager.getProperty(EXCEPTION_LEVEL_PROPERTY)
            val level = getEquivalentReportingLevel(propertyValue)
                ?: return DEFAULT_REPORTING_VALUE
            return level
        }
        set(level) {
            BBIni.propertyFileManager.save(EXCEPTION_LEVEL_PROPERTY, level.propertyValue)
        }

    private fun getEquivalentReportingLevel(value: String?): ExceptionReportingLevel? {
        if (value != null) {
            for (enumValue in ExceptionReportingLevel.entries) {
                if (enumValue.propertyValue == value) {
                    return enumValue
                }
            }
        }
        return null
    }

    @JvmStatic
    var exceptionRecoveryLevel: ExceptionRecoveryLevel
        get() {
            val propertyValue = BBIni.propertyFileManager.getProperty(EXCEPTION_RECOVERY_PROPERTY)
            val level = getEquivalentRecoveryLevel(propertyValue)
                ?: return DEFAULT_RECOVERY_VALUE
            return level
        }
        set(newLevel) {
            BBIni.propertyFileManager.save(EXCEPTION_RECOVERY_PROPERTY, newLevel.propertyValue)
        }

    private fun getEquivalentRecoveryLevel(value: String?): ExceptionRecoveryLevel? {
        if (value != null) {
            for (enumValue in ExceptionRecoveryLevel.entries) {
                if (enumValue.propertyValue == value) {
                    return enumValue
                }
            }
        }
        return null
    }

    /**
     * @return 3-state boolean: User said true, User said false, or User hasn't been asked yet (null)
     */
    fun isAutoUploadEnabledOrNull(): Boolean? {
        // BBIni may not be initted yet

        val userSetting = BBIni.propertyFileManager.getProperty(USER_SETTING_AUTO_UPLOAD)
        if (userSetting != null) {
            return userSetting.toBoolean()
        }

        // Fallback to installer selected option
        if (AUTO_REPORT_INSTALLER_CONFIG.exists()) {
            try {
                return (AUTO_REPORT_INSTALLER_CONFIG.readText(Charsets.UTF_8)  // text is ascii only so should be fine
                    .trim { it <= ' ' }
                        == "true")
            } catch (e: Exception) {
                log.error("Unable to read installer auto-report config at {}", AUTO_REPORT_INSTALLER_CONFIG, e)
                // fallback to asking the user
            }
        }

        return null
    }

    fun setAutoUploadEnabled(enabled: Boolean) {
        BBIni.propertyFileManager.saveAsBoolean(USER_SETTING_AUTO_UPLOAD, enabled)
    }

    fun reportException(
        t: Throwable?, description: String?, callback: Consumer<ErrorReportResponse>?
    ) {
        REPORTER_THREAD.execute(ExceptionReporterRunnable(t, description, callback))
    }

    private fun reportErrorAsUserMessage(e: Throwable?, description: String?): ErrorReportResponse {
        try {
            val response = reportError(e, description)
            return if (response == "0") {
                ErrorReportResponse("Error Report Successful", response, true)
            } else if (response.startsWith("2")) {
                if (BBIni.isReleaseBuild) {
                    ErrorReportResponse(
                        "Error Report Successful with old version but is not a release build",
                        response,
                        true
                    )
                } else {
                    ErrorReportResponse(
                        "Error Report Successful but old version detected",
                        response.trimStart('2', ' '),
                        false
                    )
                }
            } else {
                ErrorReportResponse("Error Report Unsuccessful", response, false)
            }
        } catch (reportException: Exception) {
            log.error("Failed to send error report", reportException)
            return ErrorReportResponse(
                "Error Report Unsuccessful",
                String.format(
                    "%s%s%s%s%s",
                    if (ExceptionUtils.getRootCause(reportException) is ConnectException) "Cannot connect" else "Unknown error when reporting",
                    ERROR_SUFFIX,
                    System.lineSeparator(),
                    System.lineSeparator(),
                    ExceptionUtils.getRootCauseMessage(reportException)
                ),
                false
            )
        }
    }

    @Throws(IOException::class, InterruptedException::class)
    private fun reportError(exception: Throwable?, description: String?): String {
        var userId = BBIni.propertyFileManager.getProperty(USER_SETTING_REPORT_UUID)
        if (userId == null) {
            // UUID as usernames might contain too much personal information
            // e.g. dumb companies that put last 4 digits of SSN
            userId = UUID.randomUUID().toString()
            BBIni.propertyFileManager.save(USER_SETTING_REPORT_UUID, userId)
        }

        return httpPost(
            ERROR_REPORT_ADDRESS,
            mapOf(
                "exception" to ExceptionUtils.getStackTrace(exception),
                "description" to (description ?: ""),
                "versionBb" to Project.BB.version,
                "versionUtd" to "Unknown",
                "versionJLouis" to "Unknown",
                    "versionLibLouis" to Project.LIBLOUIS.versionWithRev.split("]".toRegex(), limit = 2).toTypedArray()[0] + "]",
                "versionOs" to oSVersion,
                "versionJava" to javaVersion,
                "newLineSize" to "" + System.lineSeparator().length,
                "userId" to userId,
                )
        )
    }

    enum class ExceptionReportingLevel(val propertyValue: String) {
        DEBUG("debug"),
        USER_FRIENDLY("userFriendly"),
        STATUS_BAR("statusBar"),
        HIDE_ALL("hideAll")
    }

    enum class ExceptionRecoveryLevel(val propertyValue: String) {
        RECOVER("recover"),
        DO_NOT_RECOVER("doNotRecover")
    }

    @JvmRecord
    private data class ExceptionReporterRunnable(
        val exception: Throwable?, val description: String?,
        val callback: Consumer<ErrorReportResponse>?
    ) : Runnable {
        override fun run() {
            val response = reportErrorAsUserMessage(exception, description)
            callback?.accept(response)
        }
    }

    class ErrorReportResponse(val title: String, val text: String, val success: Boolean)
}

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
package org.brailleblaster.usage

import org.brailleblaster.BBIni
import org.brailleblaster.userHelp.Project
import org.brailleblaster.util.InstallId
import org.brailleblaster.utils.PropertyFileManager
import org.brailleblaster.util.Utils
import java.io.Closeable
import java.io.StringWriter
import java.util.concurrent.*

interface UsageManager {
    val logger: UsageLogger
    var trackingEnabled: Boolean
}

class SimpleUsageManager @JvmOverloads constructor(logger: UsageLogger = ListUsageLogger(SizeLimit.Max(0))) : UsageManager {
    override var trackingEnabled: Boolean = true
    override val logger: UsageLogger = logger.filterLogger { trackingEnabled }
}

const val USAGE_TRACKING_SETTING = "usageTracking"

private const val DEFAULT_REPORTING_URL = "https://www.brailleblaster.org/usage/"

class BBUsageManager(
    connectionString: String,
    private val settings: PropertyFileManager,
    private val executorService: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
) : UsageManager, AutoCloseable, Closeable {
    private val sqlLogger: SqliteUsageLogger = SqliteUsageLogger(connectionString)
    override val logger: UsageLogger = sqlLogger.filterLogger { trackingEnabled }
    override var trackingEnabled: Boolean
        get() = settings.getProperty(USAGE_TRACKING_SETTING)?.toBooleanStrictOrNull() == true
        set(value) {
            settings.saveAsBoolean(USAGE_TRACKING_SETTING, value)
        }
    private var reportHandler: ScheduledFuture<*>  ? = null
    val isReportingData: Boolean
    get() = reportHandler != null

    override fun close() {
        sqlLogger.close()
        stopPeriodicDataReporting()
        executorService.shutdown()
    }

    fun reportDataAsync(url: String = DEFAULT_REPORTING_URL): Future<Boolean> {
        return executorService.submit<Boolean> {
            reportData(url)
        }
    }

    fun startPeriodicDataReporting(
        initial: Long,
        period: Long,
        units: TimeUnit = TimeUnit.SECONDS,
        url: String = DEFAULT_REPORTING_URL
    ) {
        reportHandler?.cancel(false)
        reportHandler = executorService.scheduleWithFixedDelay({ reportData(url) }, initial, period, units)
    }
    fun stopPeriodicDataReporting() {
        reportHandler?.cancel(false)
    }

    private fun reportData(url: String): Boolean {
        return if (trackingEnabled) {
            val records = sqlLogger.toList()
            val json = StringWriter()
            JsonUsageWriter().write(records, json)
            val lastRecordTime = records.maxOfOrNull { it.time }
            if (lastRecordTime != null) {
                try {
                    Utils.httpPost(
                        url,
                        mapOf(
                            "uid" to InstallId.id.toString(),
                            "records" to json.toString(),
                            "version" to Project.BB.version
                        )
                    )
                    sqlLogger.clearTo(lastRecordTime)
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            } else true
        } else true
    }

}

fun createDefaultBBUsageManager(): BBUsageManager {
    val settings = BBIni.propertyFileManager
    val dbFile = BBIni.getUserProgramDataFile("usage.db").absoluteFile
    val usageDb = if (dbFile.parentFile.exists() || dbFile.parentFile.mkdirs()) dbFile.absolutePath else ":memory:"
    val connectionString = "jdbc:sqlite:$usageDb"
    return BBUsageManager(connectionString, settings = settings)
}

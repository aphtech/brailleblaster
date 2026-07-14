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
package org.brailleblaster

import org.brailleblaster.util.FileUtils.create
import org.brailleblaster.utils.PropertyFileManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.Path
import kotlin.io.path.pathString

/**
 * Determine and set initial conditions. This class takes care of most platform
 * dependencies. Its get methods should be used rather than coding platform
 * determining code in other classes.
 */
object BBIni {
    private val log: Logger = LoggerFactory.getLogger(BBIni::class.java)
    private lateinit var impl: BBIniImpl
    @JvmStatic
    var debugging = false
        private set


    /**
     * Calls a private constructor, making this class a singleton.
     */
    @JvmOverloads
    fun initialize(
        debugArgs: List<String>,
        bbPath: File,
        userbbPath: File,
        propManager: PropertyFileManager? = null
    ): Boolean {
        if (!::impl.isInitialized || debugging) {
            if (debugArgs.isNotEmpty()) {
                debugging = true
            }
            impl = BBIniImpl(
                bbPath.toPath().toAbsolutePath(),
                userbbPath.toPath().toAbsolutePath(),
                propManager,
                debugArgs
            )

            recentDocs = impl.recentDocsPath

            val dictDir = impl.userProgramDataPath.resolve("dictionaries")
            if (!dictDir.exists()) {
                try {
                    dictDir.createDirectories()
                } catch (e: IOException) {
                    log.warn("Unable to create dictionary directory", e)
                }
            }


            return true
        } else {
            return false
        }
    }

    /**
     * Must be set before initialize is called to have any effect
     */
    @JvmField
    var bootDialogsEnabled: Boolean = true


    lateinit var recentDocs: Path
        private set
    val charset: Charset = StandardCharsets.UTF_8


    @JvmStatic
    fun setDebuggingEnabled() {
        debugging = true
    }

    @JvmStatic
    val debugFilePath: Path?
        get() = impl.debugFilePath

    @JvmStatic
    val debugSavePath: Path?
        get() = impl.debugSavePath

    @JvmStatic
    fun setTestData(recentDocumentsPath: Path) {
        recentDocs = recentDocumentsPath.toAbsolutePath()
    }

    val isReleaseBuild: Boolean
        get() = impl.releaseBuild

    val nativeBinPath: Path
        get() = impl.nativeBinPath
    val nativeLibraryPath: Path
        get() = impl.nativeLibraryPath

    @JvmStatic
    val programDataPath: Path
        get() = impl.programDataPath

    val helpDocsPath: Path
        get() = impl.helpDocsPath

    val nativeLibrarySuffix: String
        get() = impl.nativeLibrarySuffix

    @JvmStatic
    val userProgramDataPath: Path
        get() = impl.userProgramDataPath

    fun getUserProgramDataFile(vararg pathSuffixParts: String): File {
        return Path(impl.userProgramDataPath.pathString, *pathSuffixParts).toFile()
    }

    val logFilesPath: Path
        get() = impl.logFilesPath

    @JvmStatic
    val recentSaves: Path
        get() = impl.recentSaves

    @JvmStatic
    val autoSavePath: Path
        get() = impl.autoSavePath

    val autoSaveCrashPath: String
        get() = impl.autoSaveCrashPath.toString()

    @JvmStatic
    val propertyFileManager: PropertyFileManager
        get() = impl.propertyFileManager

    /**
     * Try to get file from users program data folder first and then try global
     * program data, throwing an exception if not found
     *
     * @param pathSuffixParts the file parts inside the program data directory.
     * @return A file that exists or throws an Exception
     */
    @JvmStatic
    fun loadAutoProgramDataFile(vararg pathSuffixParts: String): File {
        val result = loadAutoProgramDataFileOrNull(*pathSuffixParts)
        if (result != null) return result
        else throw RuntimeException(
            ("Cannot find file " + Path("", *pathSuffixParts)
                    + " in " + userProgramDataPath + " or " + programDataPath)
        )
    }

    fun loadAutoProgramDataFileOrNull(vararg pathSuffixParts: String): File? {
        var file = Path(impl.userProgramDataPath.toString(), *pathSuffixParts)
        if (file.exists()) return file.toFile()

        file = Path(impl.programDataPath.toString(), *pathSuffixParts)
        if (file.exists()) return file.toFile()
        return null
    }

    val bbDistPath: Path
        get() = impl.bbDistPath

    fun createAutoSaveCrashFile() {
        create(impl.autoSaveCrashPath.toString())
    }
}

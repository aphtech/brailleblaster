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
package org.brailleblaster.utils

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Files
import kotlin.io.path.Path

object BBData {

    private const val USERDATA_VERSION: Int = 7
    val brailleblasterPath: File by lazy {
        File(
            System.getenv("BBLASTER_WORK") ?: System.getProperty("org.brailleblaster.distdir")
            ?: System.getProperty("app.dir", "")
        ).absoluteFile
    }

    fun getBrailleblasterPath(vararg pathParts: String): File = newFile(brailleblasterPath, *pathParts)

    const val USERDATA_VERSION_FILE: String = ".bb_userdata_version"
    private const val BB_USERDATA_NAME: String = "brlblst"
    val userHome: String = System.getProperty("user.home")
    /**
     * Unit tests: Disable to prevent multiple jobs racing re-making the user data folder
     */
    var moveUserDataEnabled = true
    private fun makeUserDataFolder(path: File, reason: String) {
        if (!moveUserDataEnabled)
            throw RuntimeException("Unexpected make user data$reason")
        path.mkdirs()
    }

    val userDataPath: File by lazy {
        val userData = if (os == OS.Windows) newFile(System.getenv("APPDATA"), BB_USERDATA_NAME)
        else {
            val oldPath = File(userHome, ".$BB_USERDATA_NAME")
            if (!oldPath.exists()) {
                if (os == OS.Mac) {
                    val root = File(userHome, "Library/Application Support")
                    File(root, BB_USERDATA_NAME)
                } else {
                    val xdgHome = System.getenv("XDG_CONFIG_HOME") ?: ""
                    val root = if (xdgHome.isNotBlank()) File(xdgHome)
                    else File(userHome, ".config")
                    File(root, BB_USERDATA_NAME)
                }
            } else oldPath
        }
        val versionFile = File(userData, USERDATA_VERSION_FILE)
        if (!userData.exists()) makeUserDataFolder(userData, "User data folder does not exist.")
        else if (!versionFile.exists()) {
            moveOldUserDataFolder(userData)
            makeUserDataFolder(userData, "User data too old")
        } else {
            val version = BufferedReader(FileReader(versionFile)).use { it.readLine().toInt() }
            if (version < USERDATA_VERSION) {
                moveOldUserDataFolder(userData)
                makeUserDataFolder(userData, "User data too old")
            }
        }
        val programData = File(userData, "programData")
        if (!programData.exists()) programData.mkdir()
        val utdDir = File(programData, "utd")
        if (!utdDir.exists()) utdDir.mkdir()
        val settingsDir = File(programData, "settings")
        if (!settingsDir.exists()) {
            settingsDir.mkdir()
            val defaultUserSettings = getBrailleblasterPath("programData", "settings", "user_settings.properties")
            defaultUserSettings.copyTo(settingsDir.resolve("usr_settings.properties"))
        }
        if (!versionFile.exists()) {
            FileWriter(versionFile).use {
                it.append(USERDATA_VERSION.toString())
                it.flush()
            }
        }
        userData
    }

    fun getUserDataPath(vararg pathParts: String): File = newFile(userDataPath, *pathParts)

    private fun moveOldUserDataFolder(userData: File): File {
        val oldDataDir = (0..(Int.MAX_VALUE)).asSequence().map { File("${userData.absolutePath}.old$it") }.first { !it.exists() }
        Files.move(userData.toPath(), oldDataDir.toPath())
        return oldDataDir
    }

    private fun newFile(first: String, vararg parts: String): File = Path(first, *parts).toFile()
    private fun newFile(first: File, vararg parts: String): File = newFile(first.canonicalPath, *parts)
}
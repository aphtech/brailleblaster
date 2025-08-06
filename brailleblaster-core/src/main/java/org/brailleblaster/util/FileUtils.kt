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
package org.brailleblaster.util

import nu.xom.Document
import org.brailleblaster.BBIni
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.util.Notify.showException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.file.StandardCopyOption
import kotlin.io.path.copyTo
import kotlin.io.path.exists

/**
 * This class contains methods for creating, deleting and testing files
 * and for searching directories.
 */
object FileUtils {
    fun exists(fileName: String): Boolean {
        val file = File(fileName)
        return file.exists()
    }

    fun create(fileName: String) {
        val f = File(fileName)
        if (!f.exists()) {
            try {
                f.createNewFile()
            } catch (e: IOException) {
                showException("Could not create file$fileName", e)
            }
        }
    }

    fun copyFile(inputFileName: String, outputFileName: String) {
        logger.debug("COPY input {} output {}", inputFileName, outputFileName)
        val inputFile = File(inputFileName)
        val outputFile = File(outputFileName)
        try {
            inputFile.toPath().copyTo(
                outputFile.toPath(),
                StandardCopyOption.COPY_ATTRIBUTES,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (e: Exception) {
            throw RuntimeException(
                "Unable to copy file " + inputFile.absolutePath
                        + " to " + outputFile.absolutePath, e
            )
        }
    }

    /**
     * Search for partialPathName first in the user's programData directory
     * and then in the built-in programData directory.
     *
     * @param partialPath: a name like
     * liblouisutdml/lbu_files/preferences.cfg
     */
    fun findInProgramData(partialPath: String): String? {
        val completePath = BBIni.userProgramDataPath.resolve(partialPath)
        if (completePath.exists()) {
            return completePath.toString()
        }
        return BBIni.programDataPath.resolve(partialPath).takeIf { it.exists() }?.toString()
    }

    fun appendToFile(path: String, text: String) {
        val f = File(path)
        if (f.exists()) {
            try {
                FileWriter(f, true).use { out ->
                    out.write(text)
                }
            } catch (e: IOException) {
                showException(e)
            }
        }
    }

    //Writes XML files or logs an error if it fails
    fun createXMLFile(xmlDoc: Document, path: String) {
        XMLHandler().save(xmlDoc, File(path))
    }

    val logger: Logger = LoggerFactory.getLogger(FileUtils::class.java)
}


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
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object MoreFileUtils {
    /**
     * Properly open files with UTF-8 encoding
     *
     * @return
     */
    @Throws(FileNotFoundException::class)
    fun newReaderUTF8(file: File): BufferedReader {
        return BufferedReader(
            InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8)
        )
    }

    @Throws(FileNotFoundException::class)
    fun newWriterUTF8(filename: String): BufferedWriter {
        return BufferedWriter(
            OutputStreamWriter(FileOutputStream(filename), StandardCharsets.UTF_8)
        )
    }

    /**
     * Make a file in dir/prefix_0suffix, or if it exists dir/prefix_1suffix. eg output/failure_0.xml,
     * output/failure_1.xml
     *
     * @param dir
     * @param prefix
     * @param suffix
     * @return
     */
    fun newFileIncrimented(dir: File, prefix: String, suffix: String): File {
        require(dir.exists()) { "Given directory doesn't exist: " + dir.absolutePath }
        require(dir.isDirectory) { "Expected directory, given file: " + dir.absolutePath }
        var outputFile: File
        var counter = 0
        do {
            val filenameFull = prefix + "_" + counter++ + suffix
            outputFile = File(dir, filenameFull)
        } while (outputFile.exists())
        return outputFile
    }
}
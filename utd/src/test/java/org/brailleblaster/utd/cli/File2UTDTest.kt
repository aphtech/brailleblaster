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
package org.brailleblaster.utd.cli

import org.brailleblaster.utd.testutils.UTDConfigUtils
import org.slf4j.LoggerFactory
import org.testng.annotations.Test
import java.io.File
import java.io.FileOutputStream

class File2UTDTest {
    @Test
    @Throws(Exception::class)
    fun parseBasicDTBook() {
        LoggerFactory.getLogger(javaClass).debug("Path: " + System.getProperty("jlouis.data.path"))
        // Copy the content of the input to a temp file as potentially could be in a jar.
        // Also fixes the issue of building from a path containing a space.
        // (See: RT4407)
        val tempIn = File.createTempFile("File2UTD", "basicDTBook.xml")
        FileOutputStream(tempIn).use { outStream ->
            javaClass.getResourceAsStream("basicDTBook.xml")?.use { it.transferTo(outStream) }
        }
        val input = tempIn.absolutePath
        val output = File.createTempFile("file2utd", "basicDTBook").toString()

        val app = File2UTD(
            input, output, null,
            UTDConfigUtils.TEST_ACTION_FILE.absolutePath,
            UTDConfigUtils.TEST_STYLE_FILE.absolutePath,
            UTDConfigUtils.TEST_STYLEDEFS_FILE.absolutePath, false
        )
        app.run()
    }
}

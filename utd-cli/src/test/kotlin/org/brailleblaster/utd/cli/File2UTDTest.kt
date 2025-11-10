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

import org.slf4j.LoggerFactory
import org.testng.annotations.Test
import java.io.File
import java.io.FileOutputStream

private val TEST_FOLDER = File("../utd/src/test/resources/org/brailleblaster/utd/testutils")
private val TEST_ACTION_FILE = File(TEST_FOLDER, "test.actionMap.xml")
private val TEST_STYLE_FILE = File(TEST_FOLDER, "test.styleMap.xml")
private val TEST_STYLEDEFS_FILE = File(TEST_FOLDER, "styleDefs.xml")

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
            TEST_ACTION_FILE.absolutePath,
            TEST_STYLE_FILE.absolutePath,
            TEST_STYLEDEFS_FILE.absolutePath, false
        )
        app.run()
    }
}

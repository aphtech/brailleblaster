/*
 * Copyright (C) 2026 American Printing House for the Blind
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

import picocli.CommandLine
import org.testng.Assert.assertEquals
import org.testng.Assert.assertFalse
import org.testng.Assert.assertTrue
import org.testng.Assert.expectThrows
import org.testng.annotations.Test
import java.nio.file.Files

class MainCliParsingTest {
    @Test
    fun parseHelpOption() {
        val parsed = Main.parseStartupArgs(arrayOf("--help"))
        assertTrue(parsed.showHelp)
        assertEquals(parsed.fileToOpen, null)
    }

    @Test
    fun parseVersionOption() {
        val parsed = Main.parseStartupArgs(arrayOf("-v"))
        assertTrue(parsed.showVersion)
        assertEquals(parsed.fileToOpen, null)
    }

    @Test
    fun parseUnknownOption() {
        expectThrows(CommandLine.ParameterException::class.java) {
            Main.parseStartupArgs(arrayOf("--not-a-real-option"))
        }
    }

    @Test
    fun parsePositionalInputFile() {
        val inputFile = Files.createTempFile("bb-main-cli", ".txt")
        try {
            val parsed = Main.parseStartupArgs(arrayOf(inputFile.toString()))
            assertEquals(parsed.fileToOpen, inputFile)
            assertTrue(parsed.remainingArgs.isEmpty())
        } finally {
            Files.deleteIfExists(inputFile)
        }
    }

    @Test
    fun renderVersionTextDoesNotUseUnknownVersion() {
        assertFalse(Main.renderVersionText().contains("Unknown"))
    }
}

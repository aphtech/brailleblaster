/*
 * Copyright (C) 2026 Michael Whapples
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, version 3.
 */
package org.brailleblaster.cli

import org.testng.Assert.assertEquals
import org.testng.Assert.assertNull
import org.testng.annotations.Test
import picocli.CommandLine
import java.nio.file.Paths

class BrfCommandTest {
    @Test
    fun parsesBrailleProfileOption() {
        val command = BrfCommand()

        CommandLine(command).parseArgs("input.xml", "output.brf", "--braille-profile", "UEB")

        assertEquals(command.inputFile, Paths.get("input.xml"))
        assertEquals(command.outputFile, Paths.get("output.brf"))
        assertEquals(command.brailleProfile, "UEB")
    }

    @Test
    fun omittingBrailleProfilePreservesDefaultBehavior() {
        val command = BrfCommand()

        CommandLine(command).parseArgs("input.xml", "output.brf")

        assertNull(command.brailleProfile)
    }
}
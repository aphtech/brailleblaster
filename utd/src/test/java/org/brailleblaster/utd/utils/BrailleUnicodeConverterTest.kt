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
package org.brailleblaster.utd.utils

import org.brailleblaster.utd.UTDTranslationEngine
import org.brailleblaster.utils.braille.BrailleUnicodeConverter.asciiToUnicodeLouis
import org.brailleblaster.utils.braille.BrailleUnicodeConverter.unicodeToAsciiLouis
import org.mwhapples.jlouis.Louis.TranslationModes
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.testng.annotations.Test
import kotlin.math.max

class BrailleUnicodeConverterTest {



    @Test
    @Throws(Exception::class)
    fun convertTest() {
        val engine = UTDTranslationEngine()

        val louisResultAscii = engine.brailleTranslator.translate(
            engine.brailleSettings.mainTranslationTable,
            TEST_STRING,
            0,
            0
        )
        val louisResultUnicode = engine.brailleTranslator.translate(
            engine.brailleSettings.mainTranslationTable,
            TEST_STRING,
            0,
            TranslationModes.DOTS_IO or TranslationModes.UC_BRL
        )


        log.debug("Ascii " + louisResultAscii.translation)
        log.debug("Unicode " + louisResultUnicode.translation)

        log.debug("Ascii " + Character.getName(louisResultAscii.translation[2].code))
        log.debug("Unicode " + Character.getName(louisResultUnicode.translation[2].code))

        //		Assert.assertEquals(
        assertEqualsDetails(
            unicodeToAsciiLouis(louisResultUnicode.translation),
            louisResultAscii.translation,
            "unicode-to-ascii doesn't match liblouis ascii"
        )
        //		Assert.assertEquals(
        assertEqualsDetails(
            asciiToUnicodeLouis(louisResultAscii.translation),
            louisResultUnicode.translation,
            "ascii-to-unicode doesn't match liblouis unicode"
        )
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(BrailleUnicodeConverterTest::class.java)
        /**
         * Test with random character that isn't in the tables and so will translate as '\x308A'
         */
        const val TEST_STRING: String = "\\ word \u308A testing | this is a test"
        fun compareStringDetails(given: String, expected: String): String? {
            var error: String? = null
            for (i in 0..<max(given.length.toDouble(), expected.length.toDouble()).toInt()) {
                val givenChar: Char
                if (i < given.length) {
                    givenChar = given[i]
                } else {
                    error = "given ends at " + i + " while expected ends at " + expected.length
                    break
                }

                val expectedChar: Char
                if (i < expected.length) {
                    expectedChar = expected[i]
                } else {
                    error = "expected ends at " + i + " while given ends at " + given.length
                    break
                }

                if (givenChar != expectedChar) {
                    error = ("char " + i + " given '" + givenChar + "'" + Character.getName(givenChar.code)
                            + "  expected '" + expectedChar + "' " + Character.getName(expectedChar.code))
                    break
                }
            }
            return error
        }

        fun assertEqualsDetails(given: String, expected: String, reason: String?) {
            if (given != expected) {
                throw AssertionError(
                    ("\n"
                            + "Expected: " + expected
                            + System.lineSeparator()
                            + "Given   : " + given
                            + System.lineSeparator()
                            + "Reason: " + compareStringDetails(given, expected)
                            + System.lineSeparator()
                            + reason)
                )
            }
        }
    }
}

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
package org.brailleblaster.utd.formatting

import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.testutils.UTDDocumentAsserter
import org.brailleblaster.utd.testutils.UTDTestUtils
import org.testng.annotations.Test

/**
 * newLines pending at a forced page break are dropped (#7243), unless the editor marked them as belonging
 * to the top of the new page (issue 70359).
 */
class NewLineAfterPageBreakTest {
    private val newLine = UTDTestUtils.elementToString(UTDElements.NEW_LINE, "")
    private val keptNewLine =
        "<${UTDElements.UTD_PREFIX}:${UTDElements.NEW_LINE.elementName} " +
                "${UTDElements.UTD_PREFIX}:${UTDElements.UTD_KEEP_AFTER_PAGE_BREAK_ATTRIB}='true'/>"

    private fun assertFormat(body: String): UTDDocumentAsserter {
        val doc = UTDTestUtils.generateBookDoc("", body)
        return UTDDocumentAsserter(doc, UTDTestUtils.translateAndFormat(doc))
    }

    @Test
    fun unmarkedNewLinesAfterPageBreakAreDropped() {
        assertFormat("<p newPagesAfter='1'>Test</p>$newLine$newLine$newLine<p>Test 2</p>")
            .hasTextAt("Test", 0, 0)
            .nextPage()
            .hasTextAt("Test 2", 0, 0)
            .hasNoOtherMoveTo()
    }

    @Test
    fun unmarkedNewLinesBeforePageBreakAreDropped() {
        assertFormat("<p>Test</p>$newLine$newLine<p newPagesBefore='1'>Test 2</p>")
            .hasTextAt("Test", 0, 0)
            .nextPage()
            .hasTextAt("Test 2", 0, 0)
            .hasNoOtherMoveTo()
    }

    @Test
    fun keptNewLineAfterNewPagesAfter() {
        assertFormat("<p newPagesAfter='1'>Test</p>$keptNewLine<p>Test 2</p>")
            .hasTextAt("Test", 0, 0)
            .nextPage()
            .hasTextAt("Test 2", 0, 1)
            .hasNoOtherMoveTo()
    }

    /** The reported file: stray newLines from earlier attempts stay dropped, only the marked one counts */
    @Test
    fun keptNewLineAfterDroppedNewLines() {
        assertFormat("<p newPagesAfter='1'>Test</p>$newLine$newLine$newLine$keptNewLine<p linesBefore='1'>Test 2</p>")
            .hasTextAt("Test", 0, 0)
            .nextPage()
            .hasTextAt("Test 2", 0, 1)
            .hasNoOtherMoveTo()
    }

    @Test
    fun keptNewLinesBeforeNewPagesBefore() {
        assertFormat("<p>Test</p>$keptNewLine$keptNewLine<p newPagesBefore='1'>Test 2</p>")
            .hasTextAt("Test", 0, 0)
            .nextPage()
            .hasTextAt("Test 2", 0, 2)
            .hasNoOtherMoveTo()
    }

    /** Without a page break, marked newLines are ordinary newLines */
    @Test
    fun keptNewLinesWithoutPageBreak() {
        assertFormat("<p>Test</p>$newLine$newLine<p>Test 2</p>")
            .hasTextAt("Test", 0, 0)
            .hasTextAt("Test 2", 0, 2)
            .hasNoOtherMoveTo()
        assertFormat("<p>Test</p>$keptNewLine$keptNewLine<p>Test 2</p>")
            .hasTextAt("Test", 0, 0)
            .hasTextAt("Test 2", 0, 2)
            .hasNoOtherMoveTo()
    }
}

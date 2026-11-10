/*
 * Copyright (C) 2026 American Printing House for the Blind
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 */
package org.brailleblaster.printView

import nu.xom.Element
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.findBlockOrNull
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.testrunners.BBTestRunner
import org.brailleblaster.testrunners.ViewTestRunner
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.properties.UTDElements
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.ST
import org.eclipse.swt.widgets.Event
import org.testng.Assert.assertEquals
import org.testng.Assert.assertFalse
import org.testng.Assert.assertNotNull
import org.testng.Assert.assertTrue
import org.testng.annotations.Test

/**
 * Issue 68392: Styles > Miscellaneous > Page on a selected number that follows blank lines in a new
 * document. The blank lines (utd:newLine) were still pending when the formatter reached the page
 * number, so it treated the number as the first thing on the braille page and drew no print page
 * indicator. With nothing else in the document the views then had nothing but blank lines, and caret
 * restoration crashed looking for a text element.
 */
class PageStyleAfterBlankLinesTest {
    // ---- driving the editor, see EnterAtEndOfDocumentTest for why the listeners are called directly

    private fun BBTestRunner.verifyKey(character: Char): Boolean {
        val event = Event().apply {
            this.character = character
            keyCode = character.code
            doit = true
        }
        textViewWidget.notifyListeners(ST.VerifyKey, event)
        ViewTestRunner.doPendingSWTWork()
        return event.doit
    }

    private fun BBTestRunner.pressEnter() {
        assertFalse(verifyKey(SWT.CR), "Enter is always handled by the editor")
    }

    private fun BBTestRunner.type(text: String) {
        text.forEach { c ->
            assertTrue(verifyKey(c), "Key '$c' was blocked at offset ${textViewWidget.caretOffset}")
            val offset = textViewWidget.caretOffset
            textViewWidget.replaceTextRange(offset, 0, c.toString())
            textViewWidget.caretOffset = offset + 1
        }
        manager.text.update(false)
        ViewTestRunner.doPendingSWTWork()
    }

    private fun BBTestRunner.moveCaret(offset: Int) {
        manager.text.setCurrentElement(offset)
        ViewTestRunner.doPendingSWTWork()
    }

    private fun BBTestRunner.selectText(text: String) {
        val start = textViewWidget.text.indexOf(text)
        assertTrue(start >= 0, "'$text' not in text view")
        manager.text.highlight(start, start + text.length)
        ViewTestRunner.doPendingSWTWork()
    }

    /** The real menu item, which runs StylesMenuModule.wrapSelectedPage and reformats the document */
    private fun BBTestRunner.applyPageStyle() = openMenuItem(TopMenu.STYLES, "Miscellaneous", "Page")

    // ---- inspecting the result

    private fun Element.textContent(): String = FastXPath.descendant(this).filterIsInstance<Text>()
        .filterNot { t -> generateSequence(t.parent) { it.parent }.any { UTDElements.BRL.isA(it) } }
        .joinToString("") { it.value }

    /** Content of the document section: "NL" for a blank line, otherwise block type and text */
    private fun BBTestRunner.sectionSnapshot(): List<String> =
        FastXPath.descendant(doc.rootElement).filterIsInstance<Element>()
            .filter { UTDElements.NEW_LINE.isA(it) || BBX.BLOCK.isA(it) }
            .map { if (UTDElements.NEW_LINE.isA(it)) "NL" else "${BBX.BLOCK.getSubType(it).name}:${it.textContent()}" }
            .toList()

    private fun BBTestRunner.assertPageMarker(number: String) {
        val marker = FastXPath.descendant(doc.rootElement).filterIsInstance<Element>()
            .firstOrNull { BBX.BLOCK.PAGE_NUM.isA(it) || BBX.SPAN.PAGE_NUM.isA(it) }
        assertNotNull(marker, "Page marker created: ${sectionSnapshot()}")
        assertEquals(marker!!.textContent(), number)
        assertEquals(marker.getAttributeValue("page"), "bbAdded")
    }

    /** Caret restoration (the call that threw) works, and the caret is on the page number or editable text */
    private fun BBTestRunner.assertCaretUsable() {
        val caret = manager.text.createNodeCaret(manager.mapList.current, 0)
        listOf(caret, manager.simpleManager.currentCaret).forEach {
            val target = it.node.findBlockOrNull() ?: it.node
            assertTrue(BBX.BLOCK.isA(target), "Caret on ${it.node.toXML()}")
        }
    }

    private val BBTestRunner.brailleLines: List<String> get() = brailleViewWidget.text.lines()
    private val BBTestRunner.textLines: List<String> get() = textViewWidget.text.lines()

    private val BBTestRunner.cellsPerLine: Int
        get() = manager.document.engine.let {
            (it.pageSettings.drawableWidth / it.brailleSettings.cellType.width.toDouble()).toInt()
        }

    /**
     * Print page change indicator for page 12 on the given braille line: dots 3-6 across the line, the print
     * page number #ab at the right margin. The print view shows the same line with the print number.
     */
    private fun BBTestRunner.assertIndicatorOnLine(line: Int) {
        val indicator = "-".repeat(cellsPerLine - 3) + "#ab"
        assertEquals(brailleLines.indexOf(indicator), line, "Braille view: $brailleLines")
        assertEquals(textLines.indexOfFirst { Regex("-{3,}12").matches(it) }, line, "Text view: $textLines")
        // The page number is not also placed in the top corner, as when a print page starts a braille page
        assertFalse(brailleLines.first().contains("#ab"), "Top line: ${brailleLines.first()}")
    }

    private fun newDocWithNumberAfterBlankLines(blankLines: Int): BBTestRunner =
        BBTestRunner(Manager.DEFAULT_FILE.toFile()).apply {
            repeat(blankLines) { pressEnter() }
            type("12")
        }

    private fun assertPageAfterBlankLines(blankLines: Int) {
        val bbTest = newDocWithNumberAfterBlankLines(blankLines)
        bbTest.selectText("12")
        bbTest.applyPageStyle()

        bbTest.assertPageMarker("12")
        bbTest.assertCaretUsable()
        val blankLinesKept = List(blankLines) { "NL" }
        assertEquals(bbTest.sectionSnapshot(), blankLinesKept + "PAGE_NUM:12")
        // The blank lines still come first, the page number is on the line after them
        bbTest.assertIndicatorOnLine(blankLines)

        // Editing continues on the line below the indicator, nothing before it is disturbed
        val belowIndicator = bbTest.textViewWidget.getOffsetAtLine(blankLines + 1)
        bbTest.moveCaret(belowIndicator)
        bbTest.type("Next")
        val after = bbTest.sectionSnapshot()
        assertEquals(after.dropLast(1), blankLinesKept + "PAGE_NUM:12", "Blocks: $after")
        assertTrue(after.last().endsWith(":Next"), "Typed text goes into a new block: $after")
        bbTest.assertIndicatorOnLine(blankLines)
        assertTrue(bbTest.textLines.drop(blankLines + 1).any { it.contains("Next") }, "Text view: ${bbTest.textLines}")
    }

    @Test
    fun pageAfterFiveBlankLines_issue68392() = assertPageAfterBlankLines(5)

    @Test
    fun pageAfterThreeBlankLines_issue68392() = assertPageAfterBlankLines(3)

    /** First thing on the braille page: no indicator line, the number goes in the top corner (unchanged) */
    @Test
    fun pageWithoutBlankLines() {
        val bbTest = newDocWithNumberAfterBlankLines(0)
        bbTest.selectText("12")
        bbTest.applyPageStyle()

        bbTest.assertPageMarker("12")
        bbTest.assertCaretUsable()
        assertFalse(bbTest.brailleLines.any { it.startsWith("---") }, "Braille view: ${bbTest.brailleLines}")
    }

    @Test
    fun pageAfterBlankLinesWithTextFollowing() {
        val bbTest = newDocWithNumberAfterBlankLines(5)
        bbTest.pressEnter()
        bbTest.type("After")
        bbTest.selectText("12")
        bbTest.applyPageStyle()

        bbTest.assertPageMarker("12")
        bbTest.assertCaretUsable()
        assertEquals(bbTest.sectionSnapshot(), List(5) { "NL" } + listOf("PAGE_NUM:12", "STYLE:After"))
        bbTest.assertIndicatorOnLine(5)
        assertTrue(bbTest.brailleLines[6].contains(",af"), "'After' follows the indicator: ${bbTest.brailleLines}")
    }

    /** Text before and after the number: the marker lands mid braille page (unchanged behavior) */
    @Test
    fun pageInMiddleOfBraillePage() {
        val bbTest = BBTestRunner(Manager.DEFAULT_FILE.toFile())
        bbTest.type("Before")
        repeat(6) { bbTest.pressEnter() }
        bbTest.type("12")
        bbTest.pressEnter()
        bbTest.type("After")
        val before = bbTest.sectionSnapshot()
        bbTest.selectText("12")
        bbTest.applyPageStyle()

        bbTest.assertPageMarker("12")
        bbTest.assertCaretUsable()
        assertEquals(bbTest.sectionSnapshot(), before.map { if (it == "STYLE:12") "PAGE_NUM:12" else it })

        val indicator = bbTest.brailleLines.indexOf("-".repeat(bbTest.cellsPerLine - 3) + "#ab")
        assertTrue(indicator > 0, "No print page change indicator in braille view: ${bbTest.brailleLines}")
        assertTrue(bbTest.brailleLines.take(indicator).any { it.contains(",2f") }, "'Before' precedes the indicator")
        assertTrue(bbTest.brailleLines.drop(indicator + 1).any { it.contains(",af") }, "'After' follows the indicator")
    }
}

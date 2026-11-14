/*
 * Copyright (C) 2025 American Printing House for the Blind
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

import nu.xom.Attribute
import nu.xom.Element
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.utd.BBXDynamicOptionStyleMap.Companion.OPTION_ATTRIB_PREFIX
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.modules.misc.UndoRedoModule
import org.brailleblaster.testrunners.BBTestRunner
import org.brailleblaster.testrunners.BBXDocFactory
import org.brailleblaster.testrunners.ViewTestRunner
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.util.LINE_BREAK
import org.brailleblaster.util.Utils
import org.brailleblaster.utils.xml.BB_NS
import org.brailleblaster.utils.xml.UTD_NS
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.ST
import org.eclipse.swt.widgets.Event
import org.testng.Assert.assertEquals
import org.testng.Assert.assertFalse
import org.testng.Assert.assertNotNull
import org.testng.Assert.assertTrue
import org.testng.annotations.Test
import java.io.File

/**
 * Issue 70359: newLines between a forced page break and the next block are dropped by the formatter (#7243),
 * so Enter or blank lines typed at the top of the new page had no effect ("stuck" heading), while the view
 * still mapped the dropped newLines as real lines, corrupting caret offsets (undeletable heading, crash on
 * Enter after deleting across the page break).
 */
class EditAfterPageBreakTest {
    private val reportedFile = File("src/test/resources/org/brailleblaster/printView/headingAfterPageBreak_issue70359.bbz")

    // ---- driving the editor, see EnterAtEndOfDocumentTest for why the listeners are called directly

    private fun BBTestRunner.moveCaret(offset: Int) {
        manager.text.setCurrentElement(offset)
        ViewTestRunner.doPendingSWTWork()
        assertEquals(textViewWidget.caretOffset, offset, "Caret not at requested offset")
    }

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

    /** Replace the selection [start, end) with text, typed key by key like StyledText does */
    private fun BBTestRunner.typeOver(start: Int, end: Int = start, text: String) {
        if (end > start) manager.text.highlight(start, end) else moveCaret(start)
        text.forEachIndexed { i, c ->
            assertTrue(verifyKey(c), "Key '$c' was blocked at offset ${textViewWidget.caretOffset}")
            // Typing into whitespace re-renders the view, so continue from wherever the caret ended up
            val offset = if (i == 0) start else textViewWidget.caretOffset
            textViewWidget.replaceTextRange(offset, if (i == 0) end - start else 0, c.toString())
            textViewWidget.caretOffset = offset + 1
            ViewTestRunner.doPendingSWTWork()
        }
        manager.text.update(false)
        ViewTestRunner.doPendingSWTWork()
    }

    private fun BBTestRunner.deleteRange(start: Int, end: Int) {
        manager.text.highlight(start, end)
        ViewTestRunner.doPendingSWTWork()
        assertTrue(verifyKey(SWT.BS), "Backspace over [$start, $end) was blocked")
        textViewWidget.replaceTextRange(start, end - start, "")
        ViewTestRunner.doPendingSWTWork()
    }

    private fun BBTestRunner.undo() {
        manager.simpleManager.getModule(UndoRedoModule::class.java)!!
            .undo(BBSelectionData(textViewWidget, wpManager))
        ViewTestRunner.doPendingSWTWork()
    }

    // ---- inspecting the result

    private val BBTestRunner.text: String get() = textViewWidget.text

    private fun BBTestRunner.lineOf(text: String): Int {
        val offset = this.text.indexOf(text)
        assertTrue(offset >= 0, "'$text' not in view")
        return textViewWidget.getLineAtOffset(offset)
    }

    private fun BBTestRunner.block(text: String): Element =
        FastXPath.descendant(doc.rootElement).filterIsInstance<Element>()
            .filter { BBX.BLOCK.isA(it) }
            .firstOrNull { it.textContent().contains(text) }
            .let { assertNotNull(it, "No block containing '$text'"); it!! }

    private fun Element.textContent(): String = FastXPath.descendant(this).filterIsInstance<Text>()
        .filterNot { t -> generateSequence(t.parent) { it.parent }.any { UTDElements.BRL.isA(it) } }
        .joinToString("") { it.value }

    /** Braille position of a block: (starts a new page, line on the page) */
    private fun BBTestRunner.braillePosition(text: String): Pair<Boolean, Int> {
        val brl = FastXPath.descendant(block(text)).filterIsInstance<Element>().first { UTDElements.BRL.isA(it) }
        val children = brl.childElements.toList()
        val moveTo = children.firstOrNull { UTDElements.MOVE_TO.isA(it) }
        val line = moveTo?.let { Utils.vPosToLines(manager, it.getAttributeValue("vPos").toDouble()) } ?: 0
        return children.any { UTDElements.NEW_PAGE.isA(it) } to line
    }

    private fun BBTestRunner.assertOnNewPage(text: String, line: Int) {
        assertEquals(braillePosition(text), true to line, "Braille page start and line of '$text'")
    }

    private fun Element.newPagesAfter() = getAttributeValue(OPTION_ATTRIB_PREFIX + "newPagesAfter", BB_NS)

    /** Every map element starts at or after the previous one, and the caret on each text maps back to it */
    private fun BBTestRunner.assertMapListConsistent() {
        val list = manager.mapList
        list.zipWithNext().forEach { (a, b) ->
            assertTrue(
                b.getStart(list) >= a.getStart(list),
                "Map list out of order: ${a.javaClass.simpleName} [${a.getStart(list)}] before " +
                        "${b.javaClass.simpleName} [${b.getStart(list)}]"
            )
        }
    }

    private fun BBTestRunner.bulletEnd(): Int =
        text.indexOf("sentence.", text.indexOf(LAST_BULLET)) + "sentence.".length

    /** Steps 1-2 of the crash report: delete from the end of page 1 through page 2, then Enter */
    private fun BBTestRunner.deleteAcrossPageBreakAndEnter(): Int {
        val bulletLine = textViewWidget.getLineAtOffset(bulletEnd())
        deleteRange(bulletEnd(), textViewWidget.charCount)
        assertFalse(text.contains("Practice 1"))
        assertEquals(block(LAST_BULLET).newPagesAfter(), "1", "Page break kept")
        moveCaret(textViewWidget.charCount)
        pressEnter()
        assertEquals(lineOf(PILCROW), bulletLine + 1, "Paragraph symbol on line 1 of page 2")
        assertOnNewPage(PILCROW, 0)
        return bulletLine
    }

    // ---- the reported file

    @Test
    fun reportedFile_strayNewLinesStayInvisible_issue70359() {
        val bbTest = BBTestRunner(reportedFile)
        val bulletLine = bbTest.textViewWidget.getLineAtOffset(bbTest.bulletEnd())
        assertEquals(bbTest.lineOf("Practice 1"), bulletLine + 1, "Heading on line 1 of page 2")
        bbTest.assertOnNewPage("Practice 1", 0)
        bbTest.assertMapListConsistent()

        bbTest.moveCaret(bbTest.text.indexOf("Practice 1"))
        assertEquals(bbTest.manager.mapList.current.text, "Practice 1", "Caret at heading maps to the heading")
    }

    @Test
    fun enterMovesHeadingToLine2_issue70359() {
        val bbTest = BBTestRunner(reportedFile)
        val headingLine = bbTest.lineOf("Practice 1")
        bbTest.moveCaret(bbTest.text.indexOf("Practice 1"))
        bbTest.pressEnter()

        assertEquals(bbTest.lineOf("Practice 1"), headingLine + 1)
        bbTest.assertOnNewPage("Practice 1", 1)
        assertEquals(BBX._ATTRIB_OVERRIDE_STYLE[bbTest.block("Practice 1")], "Cell 5 Heading", "Heading style kept")
        assertEquals(bbTest.block(LAST_BULLET).newPagesAfter(), "1", "Page break kept")
        bbTest.assertMapListConsistent()

        bbTest.moveCaret(bbTest.text.indexOf("Practice 1"))
        bbTest.pressEnter()
        assertEquals(bbTest.lineOf("Practice 1"), headingLine + 2)
        bbTest.assertOnNewPage("Practice 1", 2)

        bbTest.undo()
        bbTest.undo()
        assertEquals(bbTest.lineOf("Practice 1"), headingLine, "Undo moves the heading back")
        bbTest.assertOnNewPage("Practice 1", 0)
    }

    @Test
    fun backspaceMovesHeadingBackToLine1_issue70359() {
        val bbTest = BBTestRunner(reportedFile)
        val headingLine = bbTest.lineOf("Practice 1")
        bbTest.moveCaret(bbTest.text.indexOf("Practice 1"))
        bbTest.pressEnter()
        assertEquals(bbTest.lineOf("Practice 1"), headingLine + 1)

        val heading = bbTest.text.indexOf("Practice 1")
        bbTest.moveCaret(heading)
        if (bbTest.verifyKey(SWT.BS)) {
            // Not handled by the editor, so StyledText deletes the line break before the caret
            bbTest.textViewWidget.replaceTextRange(heading - LINE_BREAK.length, LINE_BREAK.length, "")
            ViewTestRunner.doPendingSWTWork()
        }
        assertEquals(bbTest.lineOf("Practice 1"), headingLine, "Heading back on line 1 of page 2")
        bbTest.assertOnNewPage("Practice 1", 0)
        assertEquals(BBX._ATTRIB_OVERRIDE_STYLE[bbTest.block("Practice 1")], "Cell 5 Heading", "Heading style kept")
        assertEquals(bbTest.block(LAST_BULLET).newPagesAfter(), "1", "Page break kept")
        bbTest.assertMapListConsistent()
    }

    @Test
    fun deleteHeadingAlone_issue70359() {
        val bbTest = BBTestRunner(reportedFile)
        val start = bbTest.text.indexOf("Practice 1")
        bbTest.deleteRange(start, start + "Practice 1".length)
        bbTest.manager.text.update(false)
        ViewTestRunner.doPendingSWTWork()

        assertFalse(bbTest.text.contains("Practice 1"), "Heading deleted")
        assertTrue(bbTest.text.contains("A. Directions"), "Following paragraph kept")
        assertEquals(bbTest.block(LAST_BULLET).newPagesAfter(), "1", "Page break kept")
        bbTest.assertOnNewPage("A. Directions", 0)
        bbTest.assertMapListConsistent()
    }

    @Test
    fun replacementTextStaysOnLine2_issue70359() {
        val bbTest = BBTestRunner(reportedFile)
        val bulletLine = bbTest.deleteAcrossPageBreakAndEnter()

        bbTest.moveCaret(bbTest.text.indexOf(PILCROW))
        bbTest.pressEnter()
        assertEquals(bbTest.lineOf(PILCROW), bulletLine + 2)

        val pilcrow = bbTest.text.indexOf(PILCROW)
        bbTest.typeOver(pilcrow, pilcrow + 1, "Replacement")
        assertEquals(bbTest.lineOf("Replacement"), bulletLine + 2, "Replacement text stays on line 2")
        bbTest.assertOnNewPage("Replacement", 1)
        bbTest.assertMapListConsistent()
    }

    @Test
    fun textTypedOnBlankLineOfNewPageStaysThere_issue70359() {
        val bbTest = BBTestRunner(reportedFile)
        val bulletLine = bbTest.deleteAcrossPageBreakAndEnter()
        repeat(2) {
            bbTest.moveCaret(bbTest.text.indexOf(PILCROW))
            bbTest.pressEnter()
        }
        assertEquals(bbTest.lineOf(PILCROW), bulletLine + 3)

        // Line 2 of page 2, on the blank lines above the paragraph symbol
        bbTest.typeOver(bbTest.textViewWidget.getOffsetAtLine(bulletLine + 2), text = "Replacement")
        assertEquals(bbTest.lineOf("Replacement"), bulletLine + 2, "Typed text stays on line 2")
        bbTest.assertOnNewPage("Replacement", 1)
        bbTest.assertMapListConsistent()
    }

    @Test
    fun enterTwiceAtEndOfPage1AfterDeletion_pilcrow_issue70359() {
        val bbTest = BBTestRunner(reportedFile)
        val bulletLine = bbTest.deleteAcrossPageBreakAndEnter()
        bbTest.assertEnterTwiceAtEndOfPage1(bulletLine, PILCROW)
    }

    @Test
    fun enterTwiceAtEndOfPage1AfterDeletion_replacementText_issue70359() {
        val bbTest = BBTestRunner(reportedFile)
        val bulletLine = bbTest.deleteAcrossPageBreakAndEnter()
        val pilcrow = bbTest.text.indexOf(PILCROW)
        bbTest.typeOver(pilcrow, pilcrow + 1, "Replacement")
        assertEquals(bbTest.lineOf("Replacement"), bulletLine + 1)
        bbTest.assertEnterTwiceAtEndOfPage1(bulletLine, "Replacement")
    }

    private fun BBTestRunner.assertEnterTwiceAtEndOfPage1(bulletLine: Int, page2Text: String) {
        moveCaret(bulletEnd())
        pressEnter()
        moveCaret(textViewWidget.caretOffset)
        pressEnter()

        assertEquals(lineOf(page2Text), bulletLine + 3)
        assertOnNewPage(page2Text, 2)
        assertEquals(block(LAST_BULLET).newPagesAfter(), "1", "Page break kept")
        assertMapListConsistent()

        // Still editable: the page 2 text can be moved back up
        moveCaret(text.indexOf(page2Text))
        pressEnter()
        assertEquals(lineOf(page2Text), bulletLine + 4)
    }

    // ---- ordinary paragraphs

    private fun bodyText(text: String, vararg options: Pair<String, String>): Element =
        BBX.BLOCK.STYLE.create("Body Text").apply {
            appendChild(text)
            options.forEach { (option, value) -> addAttribute(Attribute("bb:$OPTION_ATTRIB_PREFIX$option", BB_NS, value)) }
        }

    private fun BBTestRunner.assertEnterMovesToLine2(text: String) {
        val line = lineOf(text)
        assertOnNewPage(text, 0)
        moveCaret(this.text.indexOf(text))
        pressEnter()
        assertEquals(lineOf(text), line + 1)
        assertOnNewPage(text, 1)
        assertMapListConsistent()
        val kept = FastXPath.descendant(doc.rootElement).filterIsInstance<Element>()
            .filter { UTDElements.NEW_LINE.isA(it) }
            .map { it.getAttributeValue(UTDElements.UTD_KEEP_AFTER_PAGE_BREAK_ATTRIB, UTD_NS) }
            .toList()
        assertEquals(kept, listOf("true"), "One newLine kept after the page break")
    }

    @Test
    fun enterBeforeParagraphAfterNewPagesAfter() {
        BBTestRunner(
            BBXDocFactory()
                .append(bodyText("First paragraph", "newPagesAfter" to "1"))
                .append(bodyText("Second paragraph"))
        ).assertEnterMovesToLine2("Second paragraph")
    }

    /** Page breaks inserted with the Page Break tool are stored as newPagesBefore on the following block */
    @Test
    fun enterBeforeParagraphWithNewPagesBefore() {
        BBTestRunner(
            BBXDocFactory()
                .append(bodyText("First paragraph"))
                .append(bodyText("Second paragraph", "newPagesBefore" to "1"))
        ).assertEnterMovesToLine2("Second paragraph")
    }

    @Test
    fun enterBetweenParagraphsWithoutPageBreakUnchanged() {
        val bbTest = BBTestRunner(
            BBXDocFactory()
                .append(bodyText("First paragraph"))
                .append(bodyText("Second paragraph"))
        )
        val line = bbTest.lineOf("Second paragraph")
        bbTest.moveCaret(bbTest.text.indexOf("Second paragraph"))
        bbTest.pressEnter()
        assertEquals(bbTest.lineOf("Second paragraph"), line + 1)
        val newLines = FastXPath.descendant(bbTest.doc.rootElement).filterIsInstance<Element>()
            .filter { UTDElements.NEW_LINE.isA(it) }.toList()
        assertTrue(newLines.isNotEmpty())
        assertTrue(newLines.none { it.getAttribute(UTDElements.UTD_KEEP_AFTER_PAGE_BREAK_ATTRIB, UTD_NS) != null })
    }

    companion object {
        private const val PILCROW = "¶"
        private const val LAST_BULLET = "If the thought is incomplete"
    }
}

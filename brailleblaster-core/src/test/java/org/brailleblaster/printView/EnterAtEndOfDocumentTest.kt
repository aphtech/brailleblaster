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

import nu.xom.Element
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.findBlockOrNull
import org.brailleblaster.perspectives.braille.mapping.elements.BoxLineTextMapElement
import org.brailleblaster.testrunners.BBTestRunner
import org.brailleblaster.testrunners.BBXDocFactory
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.perspectives.braille.mapping.interfaces.Uneditable
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.modules.misc.UndoRedoModule
import org.brailleblaster.testrunners.ViewTestRunner
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.ST
import org.eclipse.swt.widgets.Event
import org.testng.Assert.assertEquals
import org.testng.Assert.assertFalse
import org.testng.Assert.assertNotNull
import org.testng.Assert.assertNull
import org.testng.Assert.assertTrue
import org.testng.annotations.Test
import java.io.File

/**
 * Enter at the very end of a document. Issue 70480: when the document ends with a box,
 * the caret sits on the bottom box line, whose node is the box container rather than
 * anything inside a block.
 */
class EnterAtEndOfDocumentTest {
    private fun bodyText(text: String): Element = BBX.BLOCK.STYLE.create("Body Text").apply { appendChild(text) }

    private fun box(vararg blocks: Element): Element = BBX.CONTAINER.BOX.create().apply {
        BBX._ATTRIB_OVERRIDE_STYLE[this] = "Box"
        blocks.forEach { appendChild(it) }
    }

    private fun docEndingWithBox() = BBTestRunner(
        BBXDocFactory()
            .append(bodyText("First paragraph"))
            .append(box(bodyText("Boxed text")))
    )

    /** Blocks under the root section as (parent type, text) pairs, ignoring translated braille */
    private fun BBTestRunner.blockSnapshot(): List<Pair<String, String>> =
        FastXPath.descendant(doc.rootElement)
            .filterIsInstance<Element>()
            .filter { BBX.BLOCK.isA(it) }
            .map { block ->
                val parent = block.parent as Element
                val parentType = if (BBX.CONTAINER.BOX.isA(parent)) "BOX" else parent.localName
                parentType to FastXPath.descendant(block)
                    .filterIsInstance<Text>()
                    .filterNot { text -> generateSequence(text.parent) { it.parent }.any { UTDElements.BRL.isA(it) } }
                    .joinToString("") { it.value }
            }
            .toList()

    /*
     * SWTBot keystrokes do not reliably reach the text view (most keyboard-driven tests in this
     * module are disabled for that reason), so drive the real listeners directly: place the caret
     * through TextView like a caret move does, then deliver a VerifyKey event to the StyledText,
     * which runs TextVerifyKeyListener.verifyKey -> checkEnter -> checkLineBreak.
     */
    private fun BBTestRunner.moveCaret(offset: Int) {
        manager.text.setCurrentElement(offset)
        ViewTestRunner.doPendingSWTWork()
        assertEquals(textViewWidget.caretOffset, offset, "Caret not at requested offset")
    }

    private fun BBTestRunner.moveToEndOfDocument() = moveCaret(textViewWidget.charCount)

    /** Returns whether the key was left for StyledText to apply (VerifyEvent.doit) */
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

    /** Type text the way StyledText does after the verify listener allows the key */
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

    private fun BBTestRunner.undo() {
        manager.simpleManager.getModule(UndoRedoModule::class.java)!!
            .undo(BBSelectionData(textViewWidget, wpManager))
        ViewTestRunner.doPendingSWTWork()
    }

    /** The last non-whitespace TME, which Enter on trailing whitespace is redirected to */
    private fun BBTestRunner.lastContentTME() =
        manager.mapList.findPreviousNonWhitespace(manager.mapList.size - 1)!!

    private fun BBTestRunner.assertEndsWithBlocklessBoxLine() {
        // Bottom box line: its brl is placed after the box container, so it has no enclosing block
        val last = lastContentTME()
        assertTrue(last is BoxLineTextMapElement, "Expected bottom box line, got ${last.javaClass.simpleName}")
        assertNull(last.node.findBlockOrNull(), "Bottom box line node has no enclosing block")
    }

    private fun BBTestRunner.assertEnterAddsBlockAfterBox() {
        val before = blockSnapshot()
        pressEnter()

        // A new block is added after the box, not inside it
        val blocks = blockSnapshot()
        assertEquals(blocks.size, before.size + 1, "Blocks: $blocks")
        assertEquals(blocks.dropLast(1), before, "Existing content unaffected")
        assertFalse(blocks.last().first == "BOX", "New block must be outside the box: $blocks")

        // The box stays whole: its bottom line still directly follows its last text, and the new block is not boxed
        val ml = manager.mapList
        val boxedText = ml.indexOfFirst { it.node?.value == before.last().second }
        assertTrue(ml[boxedText + 1] is BoxLineTextMapElement, "Page break must not split the box")
        assertFalse(ml.drop(boxedText + 2).any { it is BoxLineTextMapElement }, "New block must not be boxed")

        // Caret is in the new block, ready for continued editing
        val newCurrent = manager.mapList.current
        assertFalse(newCurrent is Uneditable, "Caret left on ${newCurrent.javaClass.simpleName}")
        assertNull(
            XMLHandler.ancestorVisitorElement(newCurrent.node) { BBX.CONTAINER.BOX.isA(it) },
            "Caret must be outside the box"
        )
    }

    private fun BBTestRunner.assertTypingGoesToNewBlock() {
        val before = blockSnapshot().dropLast(1)
        type("Next")
        val after = blockSnapshot()
        assertEquals(after.size, before.size + 1, "Blocks: $after")
        assertFalse(after.last().first == "BOX", "Typed text must be outside the box: $after")
        assertTrue(after.last().second.contains("Next"), "Typed text goes into the new block: $after")
        assertEquals(after.dropLast(1), before, "Existing content unaffected: $after")
    }

    @Test
    fun enterAtEndOfDocumentEndingWithBox_issue70480() {
        val bbTest = docEndingWithBox()
        bbTest.assertEndsWithBlocklessBoxLine()
        bbTest.moveToEndOfDocument()
        bbTest.assertEnterAddsBlockAfterBox()
        bbTest.assertTypingGoesToNewBlock()
    }

    /**
     * With the caret on the end of the last content line itself (rather than the blank line after it),
     * Enter is a no-op: addLineBreak treats a missing next element as the end of the document. A bottom
     * box line must behave the same as an ordinary last paragraph there, and must not crash.
     */
    @Test
    fun enterAtEndOfBottomBoxLine_issue70480() {
        val bbTest = docEndingWithBox()
        bbTest.assertEndsWithBlocklessBoxLine()
        val before = bbTest.blockSnapshot()
        bbTest.moveCaret(bbTest.lastContentTME().getEnd(bbTest.manager.mapList))
        assertTrue(bbTest.manager.mapList.current is BoxLineTextMapElement)

        bbTest.pressEnter()
        assertEquals(bbTest.blockSnapshot(), before)
    }

    @Test
    fun enterAtEndOfLastParagraphText() {
        val bbTest = BBTestRunner(BBXDocFactory().append(bodyText("Last paragraph")))
        val before = bbTest.blockSnapshot()
        bbTest.moveCaret("Last paragraph".length)

        bbTest.pressEnter()
        assertEquals(bbTest.blockSnapshot(), before)
    }

    @Test
    fun enterAtEndOfDocumentEndingWithBox_undo_issue70480() {
        val bbTest = docEndingWithBox()
        val before = bbTest.blockSnapshot()
        bbTest.moveToEndOfDocument()
        bbTest.pressEnter()
        assertEquals(bbTest.blockSnapshot().size, before.size + 1)

        bbTest.undo()
        assertEquals(bbTest.blockSnapshot(), before, "Undo removes the added block")
    }

    @Test
    fun enterAtEndOfDocumentEndingWithParagraph() {
        val bbTest = BBTestRunner(
            BBXDocFactory()
                .append(bodyText("First paragraph"))
                .append(bodyText("Last paragraph"))
        )
        assertNotNull(bbTest.lastContentTME().node.findBlockOrNull(), "Ordinary paragraph is inside a block")
        bbTest.moveToEndOfDocument()
        bbTest.pressEnter()

        val blocks = bbTest.blockSnapshot()
        assertEquals(blocks.size, 3, "Blocks: $blocks")
        assertEquals(blocks.take(2).map { it.second }, listOf("First paragraph", "Last paragraph"))
    }

    @Test
    fun enterAtEndOfParagraph() {
        val bbTest = BBTestRunner(
            BBXDocFactory()
                .append(bodyText("First paragraph"))
                .append(bodyText("Last paragraph"))
        )
        val text = bbTest.textViewWidget.text
        bbTest.moveCaret(text.indexOf("First paragraph") + "First paragraph".length)
        bbTest.pressEnter()

        val blocks = bbTest.blockSnapshot()
        assertEquals(blocks.map { it.second }, listOf("First paragraph", "Last paragraph"), "Blocks: $blocks")
        val firstLine = bbTest.textViewWidget.getLineAtOffset(bbTest.textViewWidget.text.indexOf("First paragraph"))
        assertTrue(bbTest.textViewWidget.getLine(firstLine + 1).isEmpty(), "Blank line added after paragraph")
    }

    @Test
    fun enterInsideParagraph() {
        val bbTest = BBTestRunner(BBXDocFactory().append(bodyText("First paragraph")))
        bbTest.moveCaret(bbTest.textViewWidget.text.indexOf("paragraph"))
        bbTest.pressEnter()

        assertEquals(bbTest.blockSnapshot().map { it.second.trim() }, listOf("First", "paragraph"))
    }

    @Test
    fun enterAtEndOfReportedBbz_issue70480() {
        val bbTest = BBTestRunner(File("src/test/resources/org/brailleblaster/printView/enterAtEndAfterBox_issue70480.bbz"))
        val before = bbTest.blockSnapshot()
        bbTest.assertEndsWithBlocklessBoxLine()
        bbTest.moveToEndOfDocument()

        bbTest.assertEnterAddsBlockAfterBox()

        bbTest.undo()
        assertEquals(bbTest.blockSnapshot(), before, "Undo removes the added block")
    }
}

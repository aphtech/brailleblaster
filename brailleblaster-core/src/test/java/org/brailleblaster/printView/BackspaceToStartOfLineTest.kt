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
import org.brailleblaster.bbx.BBX
import org.brailleblaster.perspectives.braille.document.BrailleDocument
import org.brailleblaster.testrunners.BBTestRunner
import org.brailleblaster.testrunners.ViewTestRunner
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.util.LINE_BREAK
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.ST
import org.eclipse.swt.widgets.Event
import org.testng.Assert.assertEquals
import org.testng.Assert.assertFalse
import org.testng.Assert.assertTrue
import org.testng.annotations.Test
import java.io.File

/**
 * Issue 70995: holding Backspace in a new document after typing a sentence on line 2 (or 5) and pressing Enter
 * crashed once the sentence was erased and the caret reached the start of its line.
 */
class BackspaceToStartOfLineTest {
    /** The template File > New opens, see Manager.DEFAULT_FILE */
    private val blankTemplate = File("src/main/dist/programData/xmlTemplates/blankTemplate.bbz")
    private val sentence = "This is a test."

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

    /** Type text the way StyledText does after the verify listener allows the key, leaving it uncommitted */
    private fun BBTestRunner.type(text: String) {
        text.forEach { c ->
            assertTrue(verifyKey(c), "Key '$c' was blocked at offset ${textViewWidget.caretOffset}")
            val offset = textViewWidget.caretOffset
            textViewWidget.replaceTextRange(offset, 0, c.toString())
            textViewWidget.caretOffset = offset + 1
            ViewTestRunner.doPendingSWTWork()
        }
    }

    /** One Backspace: if the editor does not handle it, StyledText deletes the character or line break before the caret */
    private fun BBTestRunner.backspace() {
        if (verifyKey(SWT.BS)) {
            val offset = textViewWidget.caretOffset
            if (offset == 0) return
            val length = if (textViewWidget.text.substring(0, offset).endsWith(LINE_BREAK)) LINE_BREAK.length else 1
            textViewWidget.replaceTextRange(offset - length, length, "")
            textViewWidget.caretOffset = offset - length
            ViewTestRunner.doPendingSWTWork()
        }
    }

    // ---- inspecting the result

    private val BBTestRunner.caretLine: Int get() = textViewWidget.getLineAtOffset(textViewWidget.caretOffset)

    private fun BBTestRunner.assertCaretAtStartOfLine(line: Int) {
        assertEquals(caretLine, line, "Caret line")
        assertEquals(textViewWidget.caretOffset, textViewWidget.getOffsetAtLine(line), "Caret at start of line $line")
    }

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

    /** Steps 1-3 of the report: Enter [blankLines] times, type the sentence and press Enter */
    private fun sentenceOnLine(blankLines: Int): BBTestRunner {
        val bbTest = BBTestRunner(blankTemplate)
        bbTest.moveCaret(0)
        repeat(blankLines) { bbTest.pressEnter() }
        bbTest.type(sentence)
        bbTest.pressEnter()
        assertEquals(bbTest.textViewWidget.getLine(blankLines), sentence, "Sentence on line $blankLines")
        bbTest.assertCaretAtStartOfLine(blankLines + 1)
        return bbTest
    }

    private fun BBTestRunner.describe() =
        "caret ${textViewWidget.caretOffset} text '${textViewWidget.text.replace(LINE_BREAK, "\\n")}'"

    /** Step 4: hold Backspace through the line break and the sentence until the caret is at the start of its line */
    private fun BBTestRunner.eraseSentence(line: Int) {
        backspace()
        assertEquals(caretLine, line, "Backspace joins the empty line to the sentence: ${describe()}")
        assertEquals(textViewWidget.getLine(line), sentence, "Sentence intact after removing the line break")
        sentence.indices.forEach { i ->
            backspace()
            assertEquals(
                textViewWidget.getLine(line), sentence.dropLast(i + 1),
                "Line $line after ${i + 1} backspaces in the sentence: ${describe()}"
            )
        }
        assertCaretAtStartOfLine(line)
        assertMapListConsistent()
    }

    /** The document is back to a new, blank document: only the placeholder, no blank lines left behind */
    private fun BBTestRunner.assertBlankDocument() {
        assertEquals(textViewWidget.text, "", "Print view is empty: ${describe()}")
        assertCaretAtStartOfLine(0)
        val nodes = FastXPath.descendant(doc.rootElement).filterIsInstance<Element>()
        assertFalse(nodes.any { UTDElements.NEW_LINE.isA(it) }, "Blank lines left in the document")
        assertTrue(
            nodes.filter { BBX.BLOCK.isA(it) }.all { BrailleDocument.isEmptyPlaceholder(it) },
            "Only the blank document placeholder is left"
        )
        assertMapListConsistent()
    }

    /** The editor still works after the document was erased: typing goes on line 1 */
    private fun BBTestRunner.assertCanTypeAgain() {
        type(sentence)
        pressEnter()
        assertEquals(textViewWidget.getLine(0), sentence, "Sentence typed again on line 1")
        assertCaretAtStartOfLine(1)
    }

    /**
     * Step 4 on the line the sentence was typed on. Blank lines are only kept in the document in front of text,
     * so once the only sentence is erased its blank lines go with it and the next Backspace leaves an empty document
     */
    private fun holdBackspace(line: Int) {
        val bbTest = sentenceOnLine(line)
        bbTest.eraseSentence(line)

        // The key event that crashed: Backspace at the start of the now empty line
        bbTest.backspace()
        bbTest.assertBlankDocument()

        // Keep holding Backspace at the top of the document
        repeat(3) { bbTest.backspace() }
        bbTest.assertBlankDocument()
        bbTest.assertCanTypeAgain()
    }

    @Test
    fun holdBackspaceOnLine2_issue70995() = holdBackspace(1)

    @Test
    fun holdBackspaceOnLine5_issue70995() = holdBackspace(4)

    /**
     * Not affected by the issue: with the caret at the very start of the document Backspace only rings the bell,
     * and the empty line is committed later like any other edit. The rest of the page shows as empty lines.
     */
    @Test
    fun holdBackspaceOnLine1() {
        val bbTest = sentenceOnLine(0)
        bbTest.eraseSentence(0)

        repeat(4) { bbTest.backspace() }
        assertTrue(bbTest.textViewWidget.text.isBlank(), "Everything erased: ${bbTest.describe()}")
        bbTest.assertCaretAtStartOfLine(0)
        bbTest.assertMapListConsistent()
        bbTest.assertCanTypeAgain()
    }

    /** Erasing the last paragraph after other text still only removes the blank lines after that text */
    @Test
    fun holdBackspaceAfterOtherText() {
        val bbTest = BBTestRunner(blankTemplate)
        bbTest.moveCaret(0)
        bbTest.type("First.")
        bbTest.pressEnter()
        bbTest.pressEnter()
        bbTest.type(sentence)
        bbTest.pressEnter()
        assertEquals(bbTest.textViewWidget.getLine(2), sentence, "Sentence on line 3")

        bbTest.eraseSentence(2)
        bbTest.backspace()

        assertEquals(bbTest.textViewWidget.getLine(0), "First.", "First paragraph kept: ${bbTest.describe()}")
        assertTrue(bbTest.caretLine <= 1, "Caret moved up from the erased line: ${bbTest.describe()}")
        bbTest.assertMapListConsistent()
    }
}

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
import org.brailleblaster.testrunners.BBTestRunner
import org.brailleblaster.testrunners.ViewTestRunner
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.util.Utils
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.ST
import org.eclipse.swt.widgets.Event
import org.testng.Assert.assertEquals
import org.testng.Assert.assertFalse
import org.testng.Assert.assertTrue
import org.testng.annotations.Test
import java.io.File

/**
 * Enter in a new, blank document. A new document only holds an empty placeholder block, so Enter on it adds
 * nothing to the document and the blank line exists only in the view until text typed after it is committed.
 */
class EnterOnNewDocumentTest {
    /** The template File > New opens, see Manager.DEFAULT_FILE */
    private val blankTemplate = File("src/main/dist/programData/xmlTemplates/blankTemplate.bbz")
    private val sentence = "This is a test sentence."

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

    // ---- inspecting the result

    private val BBTestRunner.caretLine: Int get() = textViewWidget.getLineAtOffset(textViewWidget.caretOffset)

    private fun BBTestRunner.assertCaretAtStartOfLine(line: Int) {
        assertEquals(caretLine, line, "Caret line")
        assertEquals(textViewWidget.caretOffset, textViewWidget.getOffsetAtLine(line), "Caret at start of line $line")
    }

    private fun BBTestRunner.sentenceBlock(): Element =
        FastXPath.descendant(doc.rootElement).filterIsInstance<Element>()
            .single { BBX.BLOCK.isA(it) && it.value.contains(sentence) }

    /** Line of the sentence on the braille page, from the moveTo the formatter placed in front of it */
    private fun BBTestRunner.brailleLineOfSentence(): Int {
        val brl = sentenceBlock().childElements.single { UTDElements.BRL.isA(it) }
        val moveTo = brl.childElements.first { UTDElements.MOVE_TO.isA(it) }
        return Utils.vPosToLines(manager, moveTo.getAttributeValue("vPos").toDouble())
    }

    private fun BBTestRunner.leadingNewLines(): Int {
        val block = sentenceBlock()
        val siblings = block.parent
        return (siblings.indexOf(block) - 1 downTo 0)
            .map { siblings.getChild(it) }
            .takeWhile { UTDElements.NEW_LINE.isA(it) }
            .count()
    }

    private fun BBTestRunner.assertSentenceOnLine(line: Int) {
        assertEquals(textViewWidget.getLine(line), sentence, "Print view line $line")
        (0 until line).forEach { assertEquals(textViewWidget.getLine(it), "", "Print view line $it is blank") }
        assertEquals(leadingNewLines(), line, "Blank lines kept in the document before the sentence")
        assertEquals(brailleLineOfSentence(), line, "Braille line of the sentence")
    }

    @Test
    fun enterBeforeFirstSentenceKeepsBlankLine() {
        val bbTest = BBTestRunner(blankTemplate)
        bbTest.moveCaret(0)

        bbTest.pressEnter()
        bbTest.assertCaretAtStartOfLine(1)

        bbTest.type(sentence)
        bbTest.pressEnter()

        bbTest.assertSentenceOnLine(1)
        bbTest.assertCaretAtStartOfLine(2)
    }

    @Test
    fun enterAfterSentenceOnFirstLine() {
        val bbTest = BBTestRunner(blankTemplate)
        bbTest.moveCaret(0)

        bbTest.type(sentence)
        bbTest.pressEnter()

        bbTest.assertSentenceOnLine(0)
        bbTest.assertCaretAtStartOfLine(1)
    }

    @Test
    fun twoEntersBeforeFirstSentenceKeepBothBlankLines() {
        val bbTest = BBTestRunner(blankTemplate)
        bbTest.moveCaret(0)

        bbTest.pressEnter()
        bbTest.pressEnter()
        bbTest.assertCaretAtStartOfLine(2)

        bbTest.type(sentence)
        bbTest.pressEnter()

        bbTest.assertSentenceOnLine(2)
        bbTest.assertCaretAtStartOfLine(3)
    }
}

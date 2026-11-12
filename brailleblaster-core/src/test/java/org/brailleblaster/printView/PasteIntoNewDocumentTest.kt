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
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.testrunners.BBTestRunner
import org.brailleblaster.testrunners.BBXDocFactory
import org.brailleblaster.testrunners.ViewTestRunner
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.properties.UTDElements
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.ST
import org.eclipse.swt.widgets.Event
import org.testng.Assert.assertEquals
import org.testng.Assert.assertFalse
import org.testng.Assert.assertSame
import org.testng.annotations.Test

/**
 * Issue 70999: copying a heading and the paragraph under it, opening a new document, pressing Enter once and pasting
 * crashed BrailleBlaster. Pasting the first paragraph removes the new document's empty placeholder, which paste
 * still used to look up the list it was pasting into for the following paragraphs.
 */
class PasteIntoNewDocumentTest {
    private val heading = "GED TEST TIP"
    private val tip = "Read every answer choice."
    private val after = "Not copied."

    private fun styled(style: String, text: String): Element =
        BBX.BLOCK.STYLE.create(style).apply { appendChild(text) }

    private fun sourceDocument() = BBTestRunner(
        BBXDocFactory()
            .append(styled("Cell 5 Heading", heading))
            .append(styled("Body Text", tip))
            .append(styled("Body Text", after))
    )

    /** Blocks as (style, print text) pairs, ignoring translated braille */
    private fun Manager.blockSnapshot(): List<Pair<String?, String>> =
        FastXPath.descendant(doc.rootElement)
            .filterIsInstance<Element>()
            .filter { BBX.BLOCK.isA(it) }
            .map { block ->
                block.getAttributeValue("utd-style") to FastXPath.descendant(block)
                    .filterIsInstance<Text>()
                    .filterNot { text -> generateSequence(text.parent) { it.parent }.any { UTDElements.BRL.isA(it) } }
                    .joinToString("") { it.value }
            }
            .toList()

    /** Select the heading through the end of the tip paragraph and use the Copy menu */
    private fun BBTestRunner.copyHeadingAndTip() {
        val end = textViewWidget.getOffsetAtLine(1) + tip.length
        manager.text.highlight(0, end)
        ViewTestRunner.doPendingSWTWork()
        assertEquals(textViewWidget.selectionText.lines(), listOf(heading, tip))
        copy()
        ViewTestRunner.doPendingSWTWork()
    }

    /** What File > New does, keeping the source document open */
    private fun BBTestRunner.openNewDocument(): Manager {
        wpManager.addDocumentManager(null)
        ViewTestRunner.doPendingSWTWork()
        val newDoc = wpManager.controller
        assertEquals(wpManager.list.size, 2, "Source and new document are open")
        newDoc.text.setCurrentElement(0)
        ViewTestRunner.doPendingSWTWork()
        return newDoc
    }

    /** Deliver Enter to the text view's key listener, see EnterAtEndOfDocumentTest for why */
    private fun Manager.pressEnter() {
        val event = Event().apply {
            character = SWT.CR
            keyCode = SWT.CR.code
            doit = true
        }
        textView.notifyListeners(ST.VerifyKey, event)
        ViewTestRunner.doPendingSWTWork()
        assertFalse(event.doit, "Enter is always handled by the editor")
    }

    private fun BBTestRunner.pasteAndCheck(newDoc: Manager, expectedLines: List<String>) {
        val source = manager
        val sourceBlocks = source.blockSnapshot()

        paste()
        ViewTestRunner.doPendingSWTWork()

        // Both documents are still open and the paste went into the new one
        assertEquals(wpManager.list.size, 2, "Source and new document are still open")
        assertSame(wpManager.controller, newDoc)
        assertEquals(source.blockSnapshot(), sourceBlocks, "Source document is unchanged")

        val textView = newDoc.textView
        assertEquals((0 until textView.lineCount).map { textView.getLine(it) }.take(expectedLines.size), expectedLines)
        assertEquals(
            newDoc.blockSnapshot(),
            listOf("Cell 5 Heading" to heading, "Body Text" to tip),
            "Pasted paragraphs keep their styles and replace the empty placeholder"
        )

        // The editor is still usable: the caret can move onto every pasted line
        expectedLines.indices.forEach {
            val offset = textView.getOffsetAtLine(it)
            newDoc.text.setCurrentElement(offset)
            ViewTestRunner.doPendingSWTWork()
            assertEquals(textView.caretOffset, offset, "Caret on line $it")
        }
    }

    @Test
    fun pasteHeadingAndParagraphAfterEnterInNewDocument() {
        val bbTest = sourceDocument()
        bbTest.copyHeadingAndTip()
        val newDoc = bbTest.openNewDocument()
        newDoc.pressEnter()

        bbTest.pasteAndCheck(newDoc, listOf("", heading, tip))
        val section = FastXPath.descendant(newDoc.doc.rootElement)
            .filterIsInstance<Element>()
            .first { BBX.BLOCK.isA(it) }
            .parent
        assertEquals(UTDElements.NEW_LINE.isA(section.getChild(0)), true, "Blank line from Enter is kept: ${section.toXML()}")
    }

    @Test
    fun pasteHeadingAndParagraphIntoNewDocument() {
        val bbTest = sourceDocument()
        bbTest.copyHeadingAndTip()
        val newDoc = bbTest.openNewDocument()

        bbTest.pasteAndCheck(newDoc, listOf(heading, tip))
    }
}

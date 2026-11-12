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
import org.brailleblaster.utd.properties.EmphasisType
import org.testng.Assert.assertEquals
import org.testng.Assert.assertTrue
import org.testng.annotations.Test
import java.io.File

/**
 * Copying the second line of the document and pasting it right after the direct translated `;;` on the first line
 * crashed BrailleBlaster.
 */
class PasteAfterDirectTranslationTest {
    private val file = File("src/test/resources/org/brailleblaster/printView/pasteAfterDirectTranslation.bbz")
    private val directText = ";;"
    private val copiedText = "(c./n)"

    @Test
    fun pasteSecondLineAfterDirectTranslatedText() {
        val bbTest = BBTestRunner(file)
        assertEquals(bbTest.textViewBot.getTextOnLine(0), directText)
        assertEquals(bbTest.textViewBot.getTextOnLine(1), copiedText)

        // Copy the second line
        bbTest.copySecondLine(bbTest.textViewWidget.getOffsetAtLine(1))

        // Paste right after ;; on the first line
        bbTest.moveCaret(directText.length)
        bbTest.paste()
        ViewTestRunner.doPendingSWTWork()

        assertEquals(bbTest.textViewBot.getTextOnLine(0), directText + copiedText)
        assertEquals(bbTest.textViewBot.getTextOnLine(1), copiedText)

        val blocks = bbTest.blocks()
        assertEquals(blocks.size, 2, "Paste must not add or remove paragraphs")

        // The pasted paragraph's style moves to the destination paragraph, never onto the inline emphasis
        val firstBlock = blocks.first()
        assertTrue(BBX.BLOCK.STYLE.isA(firstBlock), "First paragraph takes the pasted style: ${firstBlock.toXML()}")

        // ;; keeps its direct translation, and the inline is still a direct translation (NO_TRANSLATE) emphasis
        val directTranslated = FastXPath.descendant(firstBlock)
            .filterIsInstance<Element>()
            .filter { BBX.INLINE.EMPHASIS.isA(it) }
            .toList()
        assertEquals(directTranslated.size, 1, "One direct translation inline: ${firstBlock.toXML()}")
        val emphasis = directTranslated.single()
        assertTrue(
            BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS.get(emphasis).contains(EmphasisType.NO_TRANSLATE),
            "Inline stays direct translated: ${emphasis.toXML()}"
        )
        assertTrue(emphasis.value.startsWith(directText), "Direct translation still covers ;; : ${emphasis.toXML()}")

        // The editor is still usable: the caret can move onto both lines again
        bbTest.moveCaret(0)
        bbTest.moveCaret(bbTest.textViewWidget.getOffsetAtLine(1))
    }

    @Test
    fun pasteSecondLineIntoPlainTextStillStylesTheParagraph() {
        val bbTest = BBTestRunner(file)
        val secondLine = bbTest.textViewWidget.getOffsetAtLine(1)
        bbTest.copySecondLine(secondLine)

        // Paste into the middle of the plain (not direct translated) second line, after "(c."
        bbTest.moveCaret(secondLine + 3)
        bbTest.paste()
        ViewTestRunner.doPendingSWTWork()

        assertEquals(bbTest.textViewBot.getTextOnLine(0), directText)
        assertEquals(bbTest.textViewBot.getTextOnLine(1), "(c.$copiedText/n)")
        val blocks = bbTest.blocks()
        assertEquals(blocks.size, 2, "Paste must not add or remove paragraphs")
        assertTrue(BBX.BLOCK.STYLE.isA(blocks[1]), "Second paragraph keeps its style: ${blocks[1].toXML()}")
        assertEquals(BBX._ATTRIB_OVERRIDE_STYLE[blocks[1]], "Body Text")
        assertTrue(
            FastXPath.descendant(blocks[1]).none { BBX.INLINE.isA(it) },
            "No inline is created in plain text: ${blocks[1].toXML()}"
        )
    }

    private fun BBTestRunner.copySecondLine(secondLine: Int) {
        manager.text.highlight(secondLine, secondLine + copiedText.length)
        ViewTestRunner.doPendingSWTWork()
        assertEquals(textViewWidget.selectionText, copiedText)
        copy()
    }

    private fun BBTestRunner.blocks(): List<Element> = FastXPath.descendant(doc.rootElement)
        .filterIsInstance<Element>()
        .filter { BBX.BLOCK.isA(it) }
        .toList()

    /** Keyboard navigation through SWTBot is unreliable on local Windows runs, so move the caret directly */
    private fun BBTestRunner.moveCaret(offset: Int) {
        manager.text.setCurrentElement(offset)
        ViewTestRunner.doPendingSWTWork()
        assertEquals(textViewWidget.caretOffset, offset, "Caret not at requested offset")
    }
}

/*
 * Copyright (C) 2026 American Printing House for the Blind
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
package org.brailleblaster.wordprocessor

import org.brailleblaster.bbx.BBX
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.testrunners.BBTestRunner
import org.brailleblaster.testrunners.BBXDocFactory
import org.brailleblaster.testrunners.ViewTestRunner
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utils.localization.LocaleHandler
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.widgets.MenuItem
import org.testng.Assert.assertEquals
import org.testng.Assert.assertFalse
import org.testng.Assert.assertSame
import org.testng.Assert.assertTrue
import org.testng.annotations.Test
import java.io.File

/**
 * Issue 71009: Window > Next Document (F6) and Previous Document (Shift+F6) switch documents through the same
 * tab folder selection path as a tab click, wrapping at both ends.
 */
class DocumentSwitchTest {
    private val next = LocaleHandler.getDefault()["&NextDocument"]
    private val previous = LocaleHandler.getDefault()["&PreviousDocument"]

    private fun factory(text: String) = BBXDocFactory()
        .append(BBX.BLOCK.STYLE.create("Body Text").apply { appendChild(text) })

    /** What File > Open does, keeping the other documents open */
    private fun BBTestRunner.open(text: String): Manager {
        val file = File.createTempFile("bbTest71009", ".bbx").apply { deleteOnExit() }
        XMLHandler().save(factory(text).root.document, file)
        wpManager.addDocumentManager(file.toPath())
        ViewTestRunner.doPendingSWTWork()
        return wpManager.controller
    }

    /** Documents A, B and C in tabs 0, 1 and 2, with A selected */
    private fun threeDocuments(): Pair<BBTestRunner, List<Manager>> {
        val bbTest = BBTestRunner(factory("Alpha document"))
        val docs = listOf(bbTest.manager, bbTest.open("Bravo document"), bbTest.open("Charlie document"))
        bbTest.selectWithMenu(previous) // C -> B
        bbTest.selectWithMenu(previous) // B -> A
        bbTest.assertCurrent(docs[0])
        return bbTest to docs
    }

    private fun BBTestRunner.selectWithMenu(title: String) = openMenuItem(TopMenu.WINDOW, title)

    private fun BBTestRunner.assertCurrent(expected: Manager) {
        assertSame(wpManager.controller, expected, "Current document")
        assertEquals(wpManager.folder.selectionIndex, wpManager.list.indexOf(expected), "Selected tab")
        assertSame(wpManager.currentManager, expected, "Document of the selected tab")
    }

    @Test
    fun nextDocumentWrapsFromLastToFirst() {
        val (bbTest, docs) = threeDocuments()
        listOf(1, 2, 0).forEach {
            bbTest.selectWithMenu(next)
            bbTest.assertCurrent(docs[it])
        }
    }

    @Test
    fun previousDocumentWrapsFromFirstToLast() {
        val (bbTest, docs) = threeDocuments()
        listOf(2, 1, 0).forEach {
            bbTest.selectWithMenu(previous)
            bbTest.assertCurrent(docs[it])
        }
    }

    @Test
    fun oneDocumentDoesNothing() {
        val bbTest = BBTestRunner(factory("Only document"))
        val only = bbTest.manager
        listOf(next, previous).forEach {
            bbTest.selectWithMenu(it)
            assertEquals(bbTest.wpManager.list.size, 1)
            bbTest.assertCurrent(only)
        }
    }

    @Test
    fun menuItemsUseF6AndShiftF6() {
        val bbTest = BBTestRunner(factory("Only document"))
        val accelerators = listOf(next, previous).associateWith {
            val item: MenuItem = bbTest.bot.menu("Window", false).menu(it, false, 0).widget
            item.accelerator
        }
        assertEquals(accelerators, mapOf(next to SWT.F6, previous to (SWT.SHIFT or SWT.F6)))
    }

    @Test
    fun pasteGoesToTheDocumentSwitchedTo() {
        val (bbTest, docs) = threeDocuments()
        val (alpha, bravo) = docs
        val copied = "Alpha document"
        alpha.text.highlight(0, copied.length)
        ViewTestRunner.doPendingSWTWork()
        bbTest.copy()
        ViewTestRunner.doPendingSWTWork()

        bbTest.selectWithMenu(next)
        bbTest.assertCurrent(bravo)
        bravo.textView.caretOffset = 0
        bravo.text.setCurrentElement(0)
        ViewTestRunner.doPendingSWTWork()
        bbTest.paste()
        ViewTestRunner.doPendingSWTWork()

        assertSame(bbTest.wpManager.controller, bravo)
        assertTrue(bravo.textView.text.contains(copied), "Pasted into Bravo: ${bravo.textView.text}")
        assertTrue(bravo.textView.text.contains("Bravo document"), "Bravo keeps its text: ${bravo.textView.text}")
        assertEquals(alpha.textView.text.trim(), copied, "Alpha is unchanged")
        assertFalse(docs[2].textView.text.contains(copied), "Charlie is unchanged")
    }

    /** The Print, Braille or Style view that had focus in the document left has focus in the document switched to */
    @Test
    fun focusMovesToTheSameView() {
        val (bbTest, docs) = threeDocuments()
        val views: Map<String, (Manager) -> StyledText> =
            mapOf("Print" to { it.textView }, "Braille" to { it.brailleView }, "Style" to { it.stylePane.widget })
        views.forEach { (name, view) ->
            val from = bbTest.wpManager.controller
            view(from).setFocus()
            ViewTestRunner.doPendingSWTWork()
            assertTrue(view(from).isFocusControl, "$name view has focus before switching")

            bbTest.selectWithMenu(next)
            val to = bbTest.wpManager.controller
            assertSame(to, docs[(docs.indexOf(from) + 1) % docs.size])
            assertTrue(view(to).isFocusControl, "$name view has focus after switching")
        }
    }
}

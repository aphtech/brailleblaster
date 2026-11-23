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
@file:JvmName("DirectInput")

package org.brailleblaster.testrunners

import nu.xom.Element
import org.brailleblaster.bbx.BBX
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.modules.misc.UndoRedoModule
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.util.LINE_BREAK
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.ST
import org.eclipse.swt.widgets.Event
import org.testng.Assert.assertEquals
import org.testng.Assert.assertFalse
import org.testng.Assert.assertTrue

/*
 * Drives the editor through its own listeners instead of OS-level keystrokes.
 *
 * SWTBot keystrokes only arrive when the BrailleBlaster window has OS focus, which local Windows runs can't
 * guarantee ("Same offset for 20 times"). These call the same code the keyboard would: ST.VerifyKey for keys
 * (TextVerifyKeyListener.verifyKey -> checkEnter -> checkLineBreak for Enter), TextView for caret and selection
 * like a caret move does, and the modules for undo/redo.
 */

private fun pump() = ViewTestRunner.doPendingSWTWork()

/** Lets the background Reformatter finish before input, so edits don't race a view rebuild (same as ViewTools.settle) */
private fun BBTestRunner.settle() {
    manager.waitForFormatting(true)
    pump()
}

// ---- caret and selection

/** Moves the caret through the text view, as a click would. Asserts the caret landed on [offset] unless told not to. */
@JvmOverloads
fun BBTestRunner.moveCaret(offset: Int, assertLanded: Boolean = true) {
    settle()
    manager.text.setCurrentElement(offset)
    pump()
    if (assertLanded) assertEquals(textViewWidget.caretOffset, offset, "Caret not at requested offset")
}

fun BBTestRunner.moveToEndOfDocument() = moveCaret(textViewWidget.charCount)

/** Selects [start, end) in the text view */
fun BBTestRunner.select(start: Int, end: Int) {
    manager.text.highlight(start, end)
    pump()
}

/** Selects the first occurrence of [text] in the text view */
fun BBTestRunner.selectText(text: String) {
    val start = textViewWidget.text.indexOf(text)
    assertTrue(start >= 0, "'$text' not in text view")
    select(start, start + text.length)
}

// ---- keys

/** Sends [character] through the text view's VerifyKey listeners and returns whether the editor let it through */
fun BBTestRunner.verifyKey(character: Char): Boolean {
    val event = Event().apply {
        this.character = character
        keyCode = character.code
        doit = true
    }
    textViewWidget.notifyListeners(ST.VerifyKey, event)
    pump()
    return event.doit
}

fun BBTestRunner.pressEnter() {
    assertFalse(verifyKey(SWT.CR), "Enter is always handled by the editor")
}

/** Backspace at the caret; a line break counts as one key. Does nothing at offset 0 or when the editor blocks the key. */
fun BBTestRunner.pressBackspace() {
    if (!verifyKey(SWT.BS)) return
    val offset = textViewWidget.caretOffset
    if (offset == 0) return
    val length = if (textViewWidget.text.substring(0, offset).endsWith(LINE_BREAK)) LINE_BREAK.length else 1
    textViewWidget.replaceTextRange(offset - length, length, "")
    textViewWidget.caretOffset = offset - length
    pump()
}

/** Delete at the caret; a line break counts as one key. Does nothing at the end or when the editor blocks the key. */
fun BBTestRunner.pressDelete() {
    if (!verifyKey(SWT.DEL)) return
    val offset = textViewWidget.caretOffset
    if (offset >= textViewWidget.charCount) return
    val length = if (textViewWidget.text.startsWith(LINE_BREAK, offset)) LINE_BREAK.length else 1
    textViewWidget.replaceTextRange(offset, length, "")
    pump()
}

/** Types [text] at the caret, then commits the edit to the document as leaving the block would */
fun BBTestRunner.type(text: String) {
    settle()
    text.forEach { c -> typeKey(c) }
    manager.text.update(false)
    pump()
}

/** Types [text] at the caret one key at a time without committing, so the edit is still pending in the view */
fun BBTestRunner.typeKeys(text: String) {
    settle()
    text.forEach { c ->
        typeKey(c)
        pump()
    }
}

private fun BBTestRunner.typeKey(c: Char) {
    assertTrue(verifyKey(c), "Key '$c' was blocked at offset ${textViewWidget.caretOffset}")
    val offset = textViewWidget.caretOffset
    textViewWidget.replaceTextRange(offset, 0, c.toString())
    textViewWidget.caretOffset = offset + 1
}

/** Replaces [start, end) with [text] (or inserts at [start]) as typing over a selection would, then commits */
fun BBTestRunner.typeOver(start: Int, end: Int = start, text: String) {
    if (end > start) manager.text.highlight(start, end) else moveCaret(start)
    text.forEachIndexed { i, c ->
        assertTrue(verifyKey(c), "Key '$c' was blocked at offset ${textViewWidget.caretOffset}")
        // Typing into whitespace re-renders the view, so continue from wherever the caret ended up
        val offset = if (i == 0) start else textViewWidget.caretOffset
        textViewWidget.replaceTextRange(offset, if (i == 0) end - start else 0, c.toString())
        textViewWidget.caretOffset = offset + 1
        pump()
    }
    manager.text.update(false)
    pump()
}

/** Selects [start, end) and presses Backspace over it */
fun BBTestRunner.deleteRange(start: Int, end: Int) {
    select(start, end)
    assertTrue(verifyKey(SWT.BS), "Backspace over [$start, $end) was blocked")
    textViewWidget.replaceTextRange(start, end - start, "")
    pump()
}

// ---- undo and redo

private val BBTestRunner.undoRedo: UndoRedoModule
    get() = manager.simpleManager.getModule(UndoRedoModule::class.java)!!

fun BBTestRunner.undo() {
    undoRedo.undo(BBSelectionData(textViewWidget, wpManager))
    pump()
}

fun BBTestRunner.redo() {
    undoRedo.redo(BBSelectionData(textViewWidget, wpManager))
    pump()
}

// ---- inspecting the result

/** Every BBX block in document order */
fun BBTestRunner.blocks(): List<Element> = FastXPath.descendant(doc.rootElement)
    .filterIsInstance<Element>()
    .filter { BBX.BLOCK.isA(it) }
    .toList()

/** The map list must stay in text order; out-of-order entries are what crashed the issue 70359/70995 edits */
fun BBTestRunner.assertMapListConsistent() {
    val list = manager.mapList
    list.zipWithNext().forEach { (a, b) ->
        assertTrue(
            b.getStart(list) >= a.getStart(list),
            "Map list out of order: ${a.javaClass.simpleName} [${a.getStart(list)}] before " +
                "${b.javaClass.simpleName} [${b.getStart(list)}]"
        )
    }
}

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
package org.brailleblaster.testrunners

import org.eclipse.jface.bindings.keys.KeyStroke
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.widgets.Event
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.MenuItem

/**
 * Key presses delivered to a widget without OS focus, for [BBTestRunner.ViewTools].
 *
 * A key goes where Windows would send it: an enabled menu item with that accelerator first, otherwise a
 * KeyDown/KeyUp pair on the widget. StyledText's KeyDown handler then runs ST.VerifyKey, its key bindings
 * (arrows, Home/End, Backspace, Delete, shift-selection) or the text insert, and BrailleBlaster's key and caret
 * listeners see the same events as with a real keyboard. Display-level key filters are not reached.
 */
object DirectKeys {
    private val MODIFIER_KEYS = setOf(SWT.CTRL, SWT.SHIFT, SWT.ALT, SWT.COMMAND)

    /** The character SWT reports for [keyCode] pressed alone */
    @JvmStatic
    fun characterFor(keyCode: Int): Char = when (keyCode) {
        SWT.CR.code, SWT.LF.code, SWT.KEYPAD_CR -> SWT.CR
        SWT.BS.code -> SWT.BS
        SWT.DEL.code -> SWT.DEL
        SWT.ESC.code -> SWT.ESC
        SWT.TAB.code -> SWT.TAB
        in 0x20..0x7E -> keyCode.toChar()
        else -> '\u0000'
    }

    /** The character SWT reports for [c] pressed with [modifiers]: Ctrl+letter gives a control character */
    @JvmStatic
    fun characterFor(modifiers: Int, c: Char): Char =
        if (modifiers and SWT.CTRL != 0 && c.isLetter()) (c.lowercaseChar() - 'a' + 1).toChar() else c

    @JvmStatic
    fun press(widget: StyledText, stateMask: Int, keyCode: Int, character: Char) {
        // SWTBot sends LF for some "Enter" presses; the editor only knows CR
        val key = if (keyCode == SWT.LF.code) SWT.CR.code else keyCode
        val char = if (keyCode == SWT.LF.code) SWT.CR else character
        if (mayBeAccelerator(stateMask, key) && fireAccelerator(widget, stateMask, key)) return
        send(widget, SWT.KeyDown, stateMask, key, char)
        if (!widget.isDisposed) send(widget, SWT.KeyUp, stateMask, key, char)
    }

    /** A chord given as JFace key strokes, e.g. (Keystrokes.CTRL, Keystrokes.CR) */
    @JvmStatic
    fun press(widget: StyledText, vararg keys: KeyStroke) {
        val modifiers = keys.fold(0) { acc, k ->
            acc or k.modifierKeys or (if (k.naturalKey in MODIFIER_KEYS) k.naturalKey else 0)
        }
        val key = keys.map { it.naturalKey }.lastOrNull { it != 0 && it !in MODIFIER_KEYS } ?: return
        val character = if (key in 0x20..0x7E) characterFor(modifiers, key.toChar()) else characterFor(key)
        press(widget, modifiers, if (key in 0x41..0x5A) key + 0x20 else key, character)
    }

    /** Types [text]; a line separator (CRLF, CR or LF) is one Enter */
    @JvmStatic
    fun type(widget: StyledText, text: String) {
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '\r' || c == '\n') {
                press(widget, 0, SWT.CR.code, SWT.CR)
                i += if (c == '\r' && text.getOrNull(i + 1) == '\n') 2 else 1
            } else {
                press(widget, if (c.isUpperCase()) SWT.SHIFT else 0, c.code, c)
                i++
            }
        }
    }

    /** Plain and shifted printable keys are never menu accelerators */
    private fun mayBeAccelerator(stateMask: Int, keyCode: Int) =
        stateMask and (SWT.CTRL or SWT.ALT or SWT.COMMAND) != 0 || keyCode !in 0x20..0x7E

    private fun fireAccelerator(widget: StyledText, stateMask: Int, keyCode: Int): Boolean {
        val key = if (keyCode in 0x61..0x7A) keyCode - 0x20 else keyCode
        val wanted = setOf(stateMask or key, stateMask or keyCode)
        val item = widget.shell.menuBar?.let { items(it) }
            ?.firstOrNull { it.accelerator != 0 && it.accelerator in wanted && it.isEnabled }
            ?: return false
        if (item.style and SWT.CHECK != 0) item.selection = !item.selection
        if (item.style and SWT.RADIO != 0) item.selection = true
        item.notifyListeners(SWT.Selection, Event())
        return true
    }

    private fun items(menu: Menu): Sequence<MenuItem> = menu.items.asSequence().flatMap { item ->
        sequenceOf(item) + (item.menu?.let { items(it) } ?: emptySequence())
    }

    private fun send(widget: StyledText, type: Int, stateMask: Int, keyCode: Int, character: Char) {
        widget.notifyListeners(type, Event().apply {
            this.character = character
            this.keyCode = keyCode
            this.stateMask = stateMask
            doit = true
            display = widget.display
            time = System.currentTimeMillis().toInt()
        })
    }
}

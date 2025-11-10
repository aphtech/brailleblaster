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
package org.brailleblaster.perspectives.braille.views.wp

import com.ibm.icu.lang.UCharacter
import org.brailleblaster.util.Utils
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.custom.VerifyKeyListener
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.events.VerifyEvent
import org.eclipse.swt.events.VerifyListener
import org.eclipse.swt.widgets.Text

class SixKeyHandler(private val keyListener: KeyListener?, private val verifyKeyListener: VerifyKeyListener?, var sixKeyMode: Boolean = false) : KeyListener, VerifyKeyListener, VerifyListener {
    private var dotState = 0
    private var dotChar = 0
    override fun keyPressed(event: KeyEvent) {
        keyListener?.keyPressed(event)
        if (sixKeyMode) {
            dotState = dotState or KEYS_TO_DOTS.getOrDefault(event.character, 0)
            dotChar = dotChar or dotState
        }
    }

    override fun keyReleased(event: KeyEvent) {
        keyListener?.keyReleased(event)
        if (sixKeyMode) {
            dotState = if (Utils.isWindows) {
                0
            } else {
                dotState and (KEYS_TO_DOTS.getOrDefault(event.character, 0).inv())
            }
            if (dotState == 0 && dotChar != 0) {
                val c = dotChar.toChar()
                val widget = event.widget
                if (widget is StyledText) {
                    widget.insert(c.toString())
                    widget.caretOffset += 1
                } else if (widget is Text) {
                    widget.insert(c.toString())
                }
                dotChar = 0
            }
        }
    }

    override fun verifyText(event: VerifyEvent) {
        verifyKey(event)
    }
    override fun verifyKey(event: VerifyEvent) {
        verifyKeyListener?.verifyKey(event)
        if (sixKeyMode && UCharacter.isPrintable(event.character.code)) {
            event.doit = false
        }
    }
    companion object {
        private val KEYS_TO_DOTS = mapOf(
            ' ' to 0x2800,
            'f' to 0x2801,
            'd' to 0x2802,
            's' to 0x2804,
            'j' to 0x2808,
            'k' to 0x2810,
            'l' to 0x2820
        )
    }
}
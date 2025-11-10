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
package org.brailleblaster.math.mathml

import nu.xom.Element
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.stylers.StyleHandler
import org.brailleblaster.perspectives.braille.ui.BlockSelectionInTextSymbols
import org.brailleblaster.perspectives.braille.ui.WrapSelectionInTextSymbols
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.slf4j.LoggerFactory

object NemethIndicators {
    const val BEGINNING_INDICATOR = "_%"
    const val END_INDICATOR = "_:"
    const val INLINE_BEGINNING_INDICATOR = BEGINNING_INDICATOR + MathModuleUtils.NBS
    const val INLINE_END_INDICATOR = MathModuleUtils.NBS + END_INDICATOR
    private val log = LoggerFactory.getLogger(NemethIndicators::class.java)
    @JvmStatic
	fun block(m: Manager) {
        log.error("Nemeth blocking ")
        if (m.simpleManager.currentSelection.isTextNoSelection) {
            return
        }
        val array = BlockSelectionInTextSymbols.block(
            m, BEGINNING_INDICATOR, END_INDICATOR,
            WrapSelectionInTextSymbols.direct
        )
        if (array != null) {
            for (n in array) {
                StyleHandler.addStyle(n as Element, "1-1", m)
            }
            m.simpleManager.dispatchEvent(ModifyEvent(Sender.MATH, array, true))
        }
    }

    @JvmStatic
	fun inline(m: Manager) {
        log.error("Nemeth inlining ")
        if (m.simpleManager.currentSelection.isTextNoSelection) {
            return
        }
        WrapSelectionInTextSymbols(
            INLINE_BEGINNING_INDICATOR, INLINE_END_INDICATOR, m,
            WrapSelectionInTextSymbols.direct
        ).add()
    }
}
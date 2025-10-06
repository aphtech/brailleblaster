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
package org.brailleblaster.perspectives.braille.ui

import nu.xom.Element
import org.brailleblaster.bbx.BBX
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.BBSimpleManager.SimpleListener
import org.brailleblaster.perspectives.mvc.menu.EnableListener
import org.brailleblaster.perspectives.mvc.menu.MenuManager
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.XMLNodeCaret
import org.brailleblaster.perspectives.mvc.events.BuildMenuEvent
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.events.XMLCaretEvent
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.tools.MenuTool
import org.brailleblaster.utd.utils.dom.BoxUtils
import org.brailleblaster.exceptions.BBNotifyException
import org.brailleblaster.utd.internal.xml.XMLHandler

object UnwrapElementTool : MenuTool, SimpleListener {
    override val topMenu: TopMenu = TopMenu.TOOLS
    override val title: String = "Unwrap Element"
    override val enableListener: EnableListener = EnableListener.SINGLE_SPAN_INLINE_OR_CONTAINER
    override fun onRun(bbData: BBSelectionData) {
        unwrap(bbData.manager)
    }
    override fun onEvent(event: SimpleEvent) {
        if (event is BuildMenuEvent) {
            MenuManager.add(
                this
            )
        } else if (event is XMLCaretEvent) {
            MenuManager.notifyEnableListeners(
                EnableListener.SINGLE_SPAN_INLINE_OR_CONTAINER,
                isValid(event.start, event.end)
            )
        }
    }

    private fun isValid(start: XMLNodeCaret, end: XMLNodeCaret): Boolean {
        return if (start !== end) {
            //selected multiple items
            false
        } else {
            val node = start.node
            BBX.SPAN.isA(node) || BBX.INLINE.isA(node) || BBX.CONTAINER.isA(node)
        }
    }

    private fun unwrap(m: Manager) {
        //TODO: magic unwrap
        val sel = m.simpleManager.currentSelection
        if (!isValid(sel.start, sel.end)) {
            throw BBNotifyException("Must place caret on SPAN, INLINE, or CONTAINER")
        }

        //EnableListener should of validated this
        val elementToUnwrap = m.simpleManager.currentCaret.node as Element
        val parentOfUnwrap = elementToUnwrap.parent as Element
        BoxUtils.unbox(elementToUnwrap)
        XMLHandler.unwrapElement(elementToUnwrap)
        m.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, true, parentOfUnwrap))
    }
}
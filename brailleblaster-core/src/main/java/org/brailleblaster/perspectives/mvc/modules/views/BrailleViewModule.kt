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
package org.brailleblaster.perspectives.mvc.modules.views

import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.BBSimpleManager.SimpleListener
import org.brailleblaster.perspectives.mvc.menu.MenuManager
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.XMLTextCaret
import org.brailleblaster.perspectives.mvc.events.BuildMenuEvent
import org.brailleblaster.perspectives.mvc.events.XMLCaretEvent
import org.brailleblaster.tools.CheckMenuTool
import org.brailleblaster.wordprocessor.FontManager
import org.eclipse.swt.widgets.MenuItem

private val localeHandler = getDefault()

class BrailleViewModule(private val manager: Manager) : AbstractModule(), CheckMenuTool, SimpleListener {
    init {
        sender = Sender.BRAILLE
    }

    override val topMenu = TopMenu.VIEW
    override val title = localeHandler["&ViewBraille"]
    override val active = FontManager.isShowBraille
    override fun onRun(bbData: BBSelectionData) {
        bbData.manager.fontManager.toggleBrailleFont((bbData.widget as MenuItem).selection)
    }
    override fun onEvent(event: SimpleEvent) {
        if (event is XMLCaretEvent) {
            //TODO: selection
            if (event.start !is XMLTextCaret) {
                //TODO: Unhandlable
                return
            }
            val caret = event.start
            val mapList = manager.mapList
            //TODO: Speed of always searching from start
            var tmeIndex = mapList.findNodeIndex(caret.node, 0)
            if (manager.needsMapListUpdate() && tmeIndex < 0) {
                manager.waitForFormatting(true)
                tmeIndex = mapList.findNodeIndex(caret.node, 0)
            }
            if (tmeIndex >= 0) {
                val tme = mapList[tmeIndex]
                manager.checkView(tme)
                mapList.setCurrent(tmeIndex)
                if (event.sender != Sender.BRAILLE) {
                    manager.braille.positionFromStart = 0
                    manager.braille.cursorOffset = 0
                    manager.braille.updateCursor(tme, caret.offset)
                }
            }
        }
        if (event is BuildMenuEvent) {
            MenuManager.add(this)
        }
    }
}
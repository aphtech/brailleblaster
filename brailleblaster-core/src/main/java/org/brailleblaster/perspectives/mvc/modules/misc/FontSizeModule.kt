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
package org.brailleblaster.perspectives.mvc.modules.misc

import com.sun.jna.Platform
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.perspectives.mvc.BBSimpleManager.SimpleListener
import org.brailleblaster.perspectives.mvc.menu.MenuManager
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.events.BuildMenuEvent
import org.brailleblaster.tools.MenuTool
import org.eclipse.swt.SWT

private val localeHandler = getDefault()

object FontSizeModule : SimpleListener {
    override fun onEvent(event: SimpleEvent) {
        if (event is BuildMenuEvent) {
            MenuManager.add(IncreaseFontSizeTool)
            MenuManager.add(DecreaseFontSizeTool)
        }
    }

        //On Ubuntu 16.10 with i3wm shift must be pressed to trigger +
		@JvmField
		val HOTKEY_INCREASE_FONT = SWT.MOD1 + (if (Platform.isLinux()) '=' else '+').code
        @JvmField
		val HOTKEY_DECREASE_FONT = SWT.MOD1 + '-'.code
}

object IncreaseFontSizeTool : MenuTool {
    override val topMenu: TopMenu = TopMenu.VIEW
    override val title: String = localeHandler["&IncreaseFontSize"]
    override val accelerator: Int = FontSizeModule.HOTKEY_INCREASE_FONT
    override fun onRun(bbData: BBSelectionData) {
        bbData.manager.fontManager.increaseFont()
    }
}
object DecreaseFontSizeTool : MenuTool {
    override val topMenu: TopMenu = TopMenu.VIEW
    override val title: String = localeHandler["&DecreaseFontSize"]
    override val accelerator: Int = FontSizeModule.HOTKEY_DECREASE_FONT
    override fun onRun(bbData: BBSelectionData) {
        bbData.manager.fontManager.decreaseFont()
    }
}
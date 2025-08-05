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

import org.brailleblaster.utils.localization.LocaleHandler
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.SharedItem
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.tools.CheckMenuTool
import org.brailleblaster.tools.MenuToolModule
import org.eclipse.swt.SWT

private val localeHandler = LocaleHandler.getDefault()

class SixKeyModeModule(manager: Manager) : CheckMenuTool, MenuToolModule {
    override val topMenu = TopMenu.TOOLS
    override val title = localeHandler["SixKeyMode.menuItem"]
    override val accelerator = SWT.MOD3 or 'X'.code
    override val active = manager.text.sixKeyMode
    override val sharedItem = SharedItem.SIX_KEY
    override fun onRun(bbData: BBSelectionData) {
        bbData.manager.text.run { sixKeyMode = !sixKeyMode }
        bbData.wpManager.buildToolBar()
    }
}
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

import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.search.GoToPageDialog
import org.brailleblaster.tools.MenuToolModule
import org.eclipse.swt.SWT

private val localeHandler = getDefault()

object GoToPageTool : MenuToolModule {
    override val topMenu = TopMenu.NAVIGATE
    override val title = localeHandler["&GoToPage"]
    override val accelerator = SWT.MOD1 or 'G'.code
    override fun onRun(bbData: BBSelectionData) {
        GoToPageDialog(bbData.manager)
    }
}
object HomeTool : MenuToolModule {
    override val topMenu = TopMenu.NAVIGATE
    override val title = localeHandler["&Home"]
    override val accelerator = SWT.MOD1 or SWT.HOME
    override fun onRun(bbData: BBSelectionData) {
        bbData.manager.home()
    }
}
object EndTool : MenuToolModule {
    override val topMenu = TopMenu.NAVIGATE
    override val title = localeHandler["&End"]
    override val accelerator = SWT.MOD1 or SWT.END
    override fun onRun(bbData: BBSelectionData) {
        bbData.manager.end()
    }
}
object BookTreeTool : MenuToolModule {
    override val topMenu = TopMenu.NAVIGATE
    override val title = localeHandler["&BookTree"]
    override val accelerator = SWT.MOD3 or SWT.END
    override fun onRun(bbData: BBSelectionData) {
        bbData.manager.openBookTree()
    }
}

object NavigateModule {
    val tools = listOf(GoToPageTool, HomeTool, EndTool, BookTreeTool)
}
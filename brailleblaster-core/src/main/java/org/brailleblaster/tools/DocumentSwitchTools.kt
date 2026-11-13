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
package org.brailleblaster.tools

import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.eclipse.swt.SWT

private val localeHandler = getDefault()

/** Window > Next Document (F6): switch to the next open document, wrapping to the first */
object NextDocumentTool : MenuToolModule {
    override val topMenu = TopMenu.WINDOW
    override val title = localeHandler["&NextDocument"]
    override val accelerator = SWT.F6
    override fun onRun(bbData: BBSelectionData) = bbData.wpManager.selectNextDocument()
}

/** Window > Previous Document (Shift+F6): switch to the previous open document, wrapping to the last */
object PreviousDocumentTool : MenuToolModule {
    override val topMenu = TopMenu.WINDOW
    override val title = localeHandler["&PreviousDocument"]
    override val accelerator = SWT.SHIFT or SWT.F6
    override fun onRun(bbData: BBSelectionData) = bbData.wpManager.selectPreviousDocument()
}

object DocumentSwitchTools {
    val tools = listOf(NextDocumentTool, PreviousDocumentTool)
}

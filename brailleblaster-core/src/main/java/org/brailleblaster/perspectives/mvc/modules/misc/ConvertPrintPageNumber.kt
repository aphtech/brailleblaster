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

import nu.xom.Element
import org.brailleblaster.bbx.BBX
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.tools.MenuToolModule
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.eclipse.swt.SWT

object ConvertPrintPageNumber : MenuToolModule {
    override val topMenu = TopMenu.TOOLS
    override val title = "Convert to Print Page Number"
    override val accelerator = SWT.MOD1 or '5'.code
    override fun onRun(bbData: BBSelectionData) {
        val currentSelection = bbData.manager.simpleManager.currentSelection
        val start = currentSelection.start.node
        val end = currentSelection.end.node
        val page: Element = if (XMLHandler.ancestorElementIs(start) { node: Element? -> BBX.BLOCK.isA(node) }) {
            BBX.SPAN.PAGE_NUM.create()
        } else {
            BBX.BLOCK.PAGE_NUM.create()
        }
        StylesMenuModule(bbData.manager).wrapSelectedPage(page, start, end)
    }

}
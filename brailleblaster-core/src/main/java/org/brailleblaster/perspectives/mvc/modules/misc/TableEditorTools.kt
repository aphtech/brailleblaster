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

import org.brailleblaster.perspectives.braille.views.wp.tableEditor.TableEditor
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.SharedItem
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.tools.MenuToolModule

enum class TableEditorTools(override val topMenu: TopMenu, override val title: String, val action: (BBSelectionData) -> Unit, override val sharedItem: SharedItem? = null) : MenuToolModule {
    InsertTableTool(TopMenu.INSERT, TableEditor.INSERT_MENUITEM, { TableEditor(
        it.wpManager.shell
    ).initTable(it.manager, true) }),
    EditTableTool(TopMenu.TOOLS, TableEditor.EDIT_MENUITEM, { TableEditor(
        it.wpManager.shell
    ).initTable(it.manager, false) }, sharedItem = SharedItem.EDIT_TABLE),
    ConvertTextToTableTool(TopMenu.TOOLS, TableEditor.CONVERT_MENU_ITEM, { TableEditor(
        it.wpManager.shell
    ).convertTextToTable(it.manager) });

    override val id: String
        get() = name
    override fun onRun(bbData: BBSelectionData) {
        action(bbData)
    }
    companion object {
        val tools = entries
    }
}
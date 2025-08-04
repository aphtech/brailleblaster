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
package org.brailleblaster.perspectives.mvc.menu

import org.brailleblaster.perspectives.mvc.menu.MenuManager.addToListenerMap
import org.brailleblaster.perspectives.mvc.menu.MenuManager.menuItemAcceleratorSuffix
import org.brailleblaster.util.FormUIUtils
import org.brailleblaster.wordprocessor.WPManager
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.MenuItem

/**
 * Interface for all types of items that can be added through MenuManager
 */
interface IBBMenu {
    val topMenu: TopMenu?
    fun build(parentMenu: Menu): MenuItem
    fun copy(): IBBMenu
}

interface IBBMenuItem : IBBMenu {
    val title: String
    val accelerator: Int
    val onActivated: (BBSelectionData) -> Unit
    val enabled: Boolean
    val swtOpts: Int
    val sharedItem: SharedItem?
    val enableListener: EnableListener?
    override fun build(parentMenu: Menu): MenuItem {
        val item = MenuItem(parentMenu, swtOpts)
        val textBuilder = StringBuilder()
        textBuilder.append(title)
        if (accelerator > 0) {
            menuItemAcceleratorSuffix(textBuilder, accelerator)
            item.accelerator = accelerator
        }
        item.text = textBuilder.toString()
        FormUIUtils.addSelectionListener(item) { e: SelectionEvent ->
            //SWT dispatches two selection events when a radio button is clicked, one for the new selection
            //and one for the previous selection
            if (this !is IBBRadioMenuItem || (e.widget as MenuItem).selection) {
                val data = BBSelectionData(e.widget, WPManager.getInstance())
                data.menuItem = item
                if (sharedItem != null && MenuManager.sharedToolBars.containsKey(
                        sharedItem
                    )
                ) data.toolBarItem = MenuManager.sharedToolBars[sharedItem]
                onActivated(data)
            }
        }
        enableListener?.let { addToListenerMap(it, item) }
        sharedItem?.let { MenuManager.sharedMenuItems[it] = item }
        item.isEnabled = enabled
        return item
    }
    override fun copy(): IBBMenuItem = this
}

interface IBBCheckMenuItem : IBBMenuItem {
    val active: Boolean
    override fun build(parentMenu: Menu): MenuItem = super.build(parentMenu).apply {
        selection = active
    }

    override fun copy(): IBBCheckMenuItem = this
}

interface IBBRadioMenuItem : IBBMenuItem {
    val active: Boolean
    override fun build(parentMenu: Menu): MenuItem = super.build(parentMenu).apply {
        selection = active
    }

    override fun copy(): IBBRadioMenuItem = this
}
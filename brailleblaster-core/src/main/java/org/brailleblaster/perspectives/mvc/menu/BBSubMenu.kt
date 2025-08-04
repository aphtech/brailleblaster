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

import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.MenuItem

/**
 * Tracks submenus added to menus
 */
class BBSubMenu internal constructor(override val topMenu: TopMenu?, val text: String, val subMenuItems: MutableList<IBBMenu> = mutableListOf()) : IBBMenu, MutableList<IBBMenu> by subMenuItems {

    fun addItem(newItem: IBBMenu) {
        subMenuItems.add(newItem)
    }

    override fun build(parentMenu: Menu): MenuItem {
        val parentSubMenu = MenuItem(parentMenu, SWT.CASCADE)
        parentSubMenu.text = text
        val subMenu = Menu(parentMenu.shell, SWT.DROP_DOWN)
        for (item in subMenuItems) {
            item.build(subMenu)
        }
        // empty menu looks ugly
        if (subMenuItems.isEmpty()) {
            parentSubMenu.isEnabled = false
        }
        parentSubMenu.menu = subMenu
        return parentSubMenu
    }

    override fun copy(): BBSubMenu {
        val copy = BBSubMenu(topMenu, text, subMenuItems = subMenuItems.map { it.copy() }.toMutableList())
        return copy
    }
}

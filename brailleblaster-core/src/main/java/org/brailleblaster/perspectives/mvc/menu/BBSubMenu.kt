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

/**
 * Tracks submenus added to menus
 */
class BBSubMenu internal constructor(override val topMenu: TopMenu?, override val text: String, override val subMenuItems: MutableList<IBBMenu> = mutableListOf()) : IBBSubMenu {

    fun addItem(newItem: IBBMenu) {
        subMenuItems.add(newItem)
    }

    override fun copy(): BBSubMenu {
        val copy = BBSubMenu(topMenu, text, subMenuItems = subMenuItems.map { it.copy() }.toMutableList())
        return copy
    }
}

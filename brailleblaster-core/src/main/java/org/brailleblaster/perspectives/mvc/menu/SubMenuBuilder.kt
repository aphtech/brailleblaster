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

import org.brailleblaster.tools.CheckMenuTool
import org.brailleblaster.tools.MenuTool
import org.eclipse.swt.SWT
import java.util.function.Consumer

/**
 * Build submenus for use in MenuManager.addSubMenu()
 */
class SubMenuBuilder private constructor(val menu: TopMenu?, val name: String, val parentSubMenu: SubMenuBuilder?) {
    private val items: MutableList<IBBMenu> = mutableListOf()

    /**
     * Create a new submenu
     *
     * @param menu Top-level menu to be categorized under
     * @param name Name of submenu that appears in menu
     */
    constructor(menu: TopMenu?, name: String) : this(menu, name, null)

    /**
     * Create a nested submenu
     *
     * @param parentSubMenu Submenu this menu will be under
     * @param name          Name that will appear in the parent submenu
     */
    constructor(parentSubMenu: SubMenuBuilder?, name: String) : this(null, name, parentSubMenu)

    fun add(item: IBBMenu): SubMenuBuilder {
        items.add(item)
        when(item) {
            is IBBMenuItem -> item.sharedItem?.let { MenuManager.sharedItems[it] = item }
        }
        return this
    }

    /**
     * Add an item to the submenu
     *
     * @param text        Text of the menu item
     * @param accelerator Either the SWT.MOD_ constants plus a character, or 0 for no accelerator
     * @param onSelect    Selection behavior when item is selected
     */
    fun addItem(
        text: String,
        accelerator: Int,
        swtOpts: Int = SWT.NONE,
        sharedItem: SharedItem? = null,
        onSelect: Consumer<BBSelectionData>
    ): SubMenuBuilder = add(object : MenuTool {
        override val topMenu: TopMenu = TopMenu.DEBUG // Does not matter as not used for subitems.
        override val title: String = text
        override val swtOpts: Int = swtOpts
        override val accelerator: Int = accelerator
        override val sharedItem: SharedItem? = sharedItem

        override fun onRun(bbData: BBSelectionData) = onSelect.accept(bbData)
    })

    /**
     * Add an item with the checkbox style to the submenu
     *
     * @param text        Text of the menu item
     * @param accelerator Either the SWT.MOD_ constants plus a character, or 0 for no accelerator
     * @param selected    If true, will be checked on creation
     * @param onSelect    Selection behavior when item is selected
     */
    fun addCheckItem(
        text: String,
        accelerator: Int,
        selected: Boolean,
        sharedItem: SharedItem? = null,
        onSelect: (BBSelectionData) -> Unit
    ): SubMenuBuilder = add(object : CheckMenuTool {
        override val active: Boolean = selected

        override fun onRun(bbData: BBSelectionData) = onSelect(bbData)

        override val topMenu: TopMenu = TopMenu.DEBUG // This actually isn't used for submenus so can be any value
        override val title: String = text
        override val id: String = "${title} (generated)"

        override val accelerator: Int = accelerator
        override val sharedItem: SharedItem? = sharedItem
    })

    /**
     * Add an item with the radio style to the submenu
     *
     * @param text        Text of the menu item
     * @param accelerator Either the SWT.MOD_ constants plus a character, or 0 for no accelerator
     * @param selected    If true, will be selected on creation
     * @param onSelect    Selection behavior when item is selected
     */
    fun addRadioItem(
        text: String?,
        accelerator: Int,
        selected: Boolean,
        onSelect: (BBSelectionData) -> Unit
    ): SubMenuBuilder = add(BBRadioMenuItem(null, text, accelerator, selected, onSelect))

    fun addSeparator(): SubMenuBuilder = add(BBSeparator(null))

    /**
     * Add a nested submenu to this submenu. Note: Nested submenu's parent must be set to this SubMenuBuilder
     *
     * @param newSubMenu New sub menu to inserted under the current sub menu
     */
    fun addSubMenu(newSubMenu: BBSubMenu): SubMenuBuilder = add(newSubMenu)

    fun build(): BBSubMenu = BBSubMenu(menu, name, subMenuItems = items.toMutableList())
}

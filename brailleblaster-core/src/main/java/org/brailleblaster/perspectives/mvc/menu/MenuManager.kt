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

import org.brailleblaster.settings.ui.BrailleSettingsDialog
import org.brailleblaster.settings.ui.PagePropertiesTab
import org.brailleblaster.tools.EmphasisMenuTool
import org.brailleblaster.tools.MenuTool
import org.brailleblaster.userHelp.AboutTool
import org.brailleblaster.util.ui.MenuUtils.addSystemMenuItem
import org.brailleblaster.wordprocessor.WPManager
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.*
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Consumer

object MenuManager {
    private val log = LoggerFactory.getLogger(MenuManager::class.java)

    /**
     * Map to link tool bar selection behavior to menu
     */
    //Welcome to hashmap hell
	@JvmField
	val sharedItems: HashMap<SharedItem, IBBMenuItem> = LinkedHashMap()

    /**
     * Map to link menu items with submenus across the menu, toolbar, and context menu
     */
    private val sharedSubMenus: HashMap<SharedItem, BBSubMenu> = LinkedHashMap()

    /**
     * Map to retrieve tool bar item widget that dispatched selection event
     */
	@JvmField
	val sharedToolBars: HashMap<SharedItem, ToolItem> = LinkedHashMap()

    /**
     * Map to retrieve menu items when dispatching selection events
     */
	@JvmField
	val sharedMenuItems: HashMap<SharedItem, MenuItem> = LinkedHashMap()

    /**
     * Internal list to build menu
     */
    private val items: MutableList<IBBMenu> = ArrayList()

    /**
     * Internal map to handle emphasis buttons
     */
    private val emphasisHandlers: HashMap<EmphasisItem, (BBSelectionData) -> Unit> = LinkedHashMap()

    /**
     * Internal map to handle style buttons
     */
    private val styleMenus: HashMap<String, BBSubMenu> = LinkedHashMap()

    /**
     * Internal map to enable or disable items
     */
    private val enableItems: HashMap<EnableListener, MutableList<MenuItem>> = LinkedHashMap()
    private var disposeDisplayListener: Listener? = null

    /**
     * Iterates through all added menu items to create the menu. Adding any other menu items
     * after this method is called will have no effect on the menu.
     * @param shellMenu
     */
	fun buildMenu(shellMenu: Menu) {
        //RT#4377: Unit tests dispose the Display, so clear the state on dispose
        if (disposeDisplayListener == null) {
            disposeDisplayListener = Listener {
                disposeDisplayListener = null
                sharedItems.clear()
                sharedSubMenus.clear()
                items.clear()
                emphasisHandlers.clear()
                styleMenus.clear()
                enableItems.clear()
            }
            Display.getCurrent().addListener(SWT.Dispose, disposeDisplayListener)
        }
        enableItems.clear()
        //Build top level menus
        for (menu in TopMenu.entries) {
            val newMenuItem = MenuItem(shellMenu, SWT.CASCADE)
            newMenuItem.text = menu.menuName
            val newMenu = Menu(shellMenu.shell, SWT.DROP_DOWN)
            newMenuItem.menu = newMenu
            items.filter { it.topMenu == menu }.forEach { it.build(newMenu) }
        }
        val existingAccelerators = HashMap<Int, String>()
        checkMenuAcceleratorsForDuplicates(shellMenu, existingAccelerators)

        /*
		 * Check alt hotkeys of top level menu items against global hotkeys.
		 * Submenu hotkey's do not need to be checked as when they are enabled 
		 * focus is on the submenu, not the shell.
		 */
        for (curItem in shellMenu.items) {
            val altHotkeyStart = curItem.text.indexOf("&")
            if (altHotkeyStart != -1) {
                val altChar = curItem.text.substring(altHotkeyStart + 1, altHotkeyStart + 2)[0]
                log.debug("checking hotkey alt + " + altChar + " in " + curItem.text)
                val altHotkey = SWT.ALT or altChar.code
                if (existingAccelerators.containsKey(altHotkey)) {
                    throw RuntimeException(
                        "Menu Item alt hotkey " + curItem.text
                                + " conflicts with accelerator " + existingAccelerators[altHotkey]
                    )
                } else {
                    existingAccelerators[altHotkey] = curItem.text
                }
            }
        }

        //Remove empty menus
        //TODO: This is currently only for the DEBUG menu, is there a better way?
        for (item in shellMenu.items) {
            if (item.menu.items.isEmpty()) {
                item.dispose()
            }
        }
        // Deal with Mac specific menus
        addSystemMenuItem(
            shellMenu.display, SWT.ID_ABOUT
        ) { e: Event -> AboutTool.run(BBSelectionData(e.widget, WPManager.getInstance())) }
        addSystemMenuItem(
            shellMenu.display, SWT.ID_PREFERENCES
        ) { BrailleSettingsDialog(WPManager.getInstance().controller, PagePropertiesTab::class.java) }
    }

    private fun checkMenuAcceleratorsForDuplicates(rootMenu: Menu, existingAccelerators: MutableMap<Int, String>) {
        for (curItem in rootMenu.items) {
            val accelerator = curItem.accelerator
            if (accelerator == 0) {
                // skip
            } else if (existingAccelerators.containsKey(accelerator)) {
                throw RuntimeException(
                    "Menu Item '" + curItem.text
                            + "' accelerator conflicts with "
                            + existingAccelerators[accelerator]
                )
            } else {
                existingAccelerators[accelerator] = curItem.text
            }
            if (curItem.menu != null) {
                checkMenuAcceleratorsForDuplicates(curItem.menu, existingAccelerators)
            }
        }
    }

    @JvmStatic
	fun addMenuItem(tool: MenuTool) {
        if (tool is EmphasisMenuTool) {
            val emphasis = tool.emphasis
            emphasisHandlers[emphasis] = tool::run
        }
        items.add(tool)
        tool.sharedItem?.let {
            sharedItems[it] = tool
        }
    }

    /**
     * Adds a horizontal line to visually separate menu items
     * @param menu Top-level menu the separator should appear under
     */
	@JvmStatic
	fun addSeparator(menu: TopMenu?) {
        items.add(BBSeparator(menu))
    }

    /**
     * Adds a sub menu.
     * @param subMenu SubMenuBuilder to create the sub menu.
     * @see SubMenuBuilder
     */
	@JvmStatic
	fun addSubMenu(subMenu: SubMenuBuilder) {
        val newMenu = subMenu.build()

        // Merge duplicate sub menus, allows submenus to be defined in multiple modules
        var mergedWithExisting = false
        for (item in items) {
            if (item !is BBSubMenu) {
                continue
            }
            if (item.text == newMenu.text) {
                mergedWithExisting = true
                for (subMenuItem in newMenu.subMenuItems) {
                    item.addItem(subMenuItem)
                }
                break
            }
        }
        if (!mergedWithExisting) {
            items.add(newMenu)
        }
    }

    /**
     * Adds a category submenu to the style menu
     * @param category Category of the id of the style
     * @param subMenu SubMenuBuilder containing the styles for that category
     * @see SubMenuBuilder
     */
    fun addToStyleMenu(category: String, subMenu: SubMenuBuilder) {
        styleMenus[category] = subMenu.build().copy()
    }

    fun addToSharedSubMenus(item: SharedItem, subMenu: SubMenuBuilder) {
        sharedSubMenus[item] = subMenu.build().copy()
    }

    /**
     * Create a new menu generated from the styles
     * @param category Category of desired style menu
     * @param parent Shell or composite to be the parent of the menu
     * @return A menu object, or null if category does not exist
     */
	@JvmStatic
	fun getFromStyleMenu(category: String, parent: Composite?): Menu? {
        if (styleMenus.containsKey(category)) {
            val subMenu = styleMenus[category]
            val newMenu = Menu(parent)
            subMenu!!.subMenuItems.forEach(Consumer { i: IBBMenu -> i.build(newMenu) })
            return newMenu
        }
        return null
    }

    @JvmStatic
	val styleMenuCategories: Set<String>
        /**
         * Get all categories of styles defined in styledefs
         */
        get() = styleMenus.keys

    /**
     * Create a new menu from a shared sub menu
     * @return
     */
	@JvmStatic
	fun createSharedSubMenu(item: SharedItem, shell: Shell?, floating: Boolean): Menu {
        val newMenu = Menu(shell, if (floating) SWT.NONE else SWT.DROP_DOWN)
        val foundSMB = sharedSubMenus[item]
            ?: throw UnsupportedOperationException("Operation $item not yet supported")
        foundSMB.subMenuItems.forEach(Consumer { i: IBBMenu -> i.build(newMenu) })
        return newMenu
    }
    //The following methods are made so that the values of the maps are looked up when
    //the user clicks on the buttons, not when the buttons are initialized.
    /**
     * Get the selection behavior of an item shared between the menu and tool bar
     * @param item Shared item
     * @return Selection behavior assigned to that item
     */
	@JvmStatic
    fun getSharedSelection(item: SharedItem): (BBSelectionData) -> Unit {
        return { e -> lookupSelection(item)(e) }
    }

    private fun lookupSelection(item: SharedItem): (BBSelectionData) -> Unit {
        return if (sharedItems.containsKey(item)) {
            sharedItems[item]!!.onActivated
        } else {
            throw UnsupportedOperationException(
                "Operation $item not yet supported"
            )
        }
    }

    /**
     * Get the selection behavior of an emphasis item shared between multiple views
     * @param item Shared emphasis
     * @return Selection behavior assigned to that emphasis
     */
	@JvmStatic
	fun getEmphasisSelection(item: EmphasisItem): Consumer<BBSelectionData> {
        return Consumer { e: BBSelectionData -> lookupEmphasis(item)(e) }
    }

    private fun lookupEmphasis(item: EmphasisItem): (BBSelectionData) -> Unit {
        return if (emphasisHandlers.containsKey(item)) {
            emphasisHandlers[item]!!
        } else {
            throw UnsupportedOperationException(
                "Operation $item not yet supported"
            )
        }
    }

    /**
     * Used by MenuModule to enable or disable menu items based on their listeners. Not
     * intended for general use.
     */
    fun notifyEnableListeners(listener: EnableListener, value: Boolean) {
        if (enableItems.containsKey(listener)) {
            val items: List<MenuItem> = enableItems[listener]!!
            items.forEach(Consumer { mi: MenuItem -> mi.isEnabled = value })
        }
    }

    @JvmStatic
	fun addToListenerMap(listener: EnableListener, item: MenuItem) {
        if (enableItems.containsKey(listener)) {
            val items = enableItems[listener]!!
            items.add(item)
        } else {
            val items: MutableList<MenuItem> = ArrayList()
            items.add(item)
            enableItems[listener] = items
        }
    }

    /**
     * Dispose the given menu and clear MenuManager's internal lists.
     */
	@JvmStatic
	fun disposeMenu(shellMenu: Menu?) {
        if (shellMenu != null && !shellMenu.isDisposed) {
            shellMenu.dispose()
            items.clear()
        }
    }

    @JvmStatic
	fun menuItemAcceleratorSuffix(accelerator: Int): String {
        val sb = StringBuilder()
        menuItemAcceleratorSuffix(sb, accelerator)
        return sb.toString()
    }

    fun menuItemAcceleratorSuffix(textBuilder: StringBuilder, accelerator: Int) {
        textBuilder.append("\t")
        if (accelerator and SWT.MOD1 != 0) {
            val onMac =
                System.getProperty("os.name") != null && System.getProperty("os.name").lowercase(Locale.getDefault())
                    .contains("mac")
            if (onMac) {
                textBuilder.append("\u2318 + ")
            } else {
                textBuilder.append("Ctrl + ")
            }
        }
        if (accelerator and SWT.MOD2 != 0) {
            textBuilder.append("Shift + ")
        }
        if (accelerator and SWT.MOD3 != 0) {
            textBuilder.append("Alt + ")
        }
        if (accelerator and SWT.MOD4 != 0) { //Mac only
            textBuilder.append("Ctrl + ")
        }
        val charName: String
        val value =
            accelerator - (accelerator and SWT.MOD1) - (accelerator and SWT.MOD2) - (accelerator and SWT.MOD3) - (accelerator and SWT.MOD4)
        when (value) {
            SWT.F1 -> charName = "F1"
            SWT.F2 -> charName = "F2"
            SWT.F3 -> charName = "F3"
            SWT.F4 -> charName = "F4"
            SWT.F5 -> charName = "F5"
            SWT.F6 -> charName = "F6"
            SWT.F7 -> charName = "F7"
            SWT.F8 -> charName = "F8"
            SWT.F9 -> charName = "F9"
            SWT.F10 -> charName = "F10"
            SWT.F11 -> charName = "F11"
            SWT.F12 -> charName = "F12"
            SWT.ARROW_UP -> charName = "Up"
            SWT.ARROW_DOWN -> charName = "Down"
            SWT.ARROW_LEFT -> charName = "Left"
            SWT.ARROW_RIGHT -> charName = "Right"
            SWT.HOME -> charName = "Home"
            SWT.END -> charName = "End"
            SWT.PAGE_DOWN -> charName = "Page Down"
            SWT.PAGE_UP -> charName = "Page Up"
            SWT.INSERT -> charName = "Insert"
            SWT.CR.code -> charName = "Enter"
            SWT.TAB.code -> charName = "Tab"
            else -> {
                val character = (255 and accelerator).toChar()
                charName = "" + character
                /**
                 * Require uppercase for consistency in menus, aid in duplicate key detection
                 */
                if (Character.isLowerCase(character)) {
                    textBuilder.append(charName)
                    throw RuntimeException("Letter must be uuppercase in $textBuilder")
                }
            }
        }
        textBuilder.append(charName)
    }
}

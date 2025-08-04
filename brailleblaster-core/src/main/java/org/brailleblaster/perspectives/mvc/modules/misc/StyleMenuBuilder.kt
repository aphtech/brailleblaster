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

import org.brailleblaster.BBIni
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getBanaStyles
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.mvc.menu.*
import org.brailleblaster.perspectives.mvc.menu.MenuManager.addSeparator
import org.brailleblaster.perspectives.mvc.menu.MenuManager.addSubMenu
import org.brailleblaster.perspectives.mvc.menu.MenuManager.addToStyleMenu
import org.brailleblaster.settings.ui.Loadout
import org.brailleblaster.settings.ui.Loadout.Companion.getAcc
import org.brailleblaster.settings.ui.Loadout.Companion.listLoadouts
import org.brailleblaster.utd.IStyle
import org.brailleblaster.utd.Shortcut
import org.brailleblaster.utd.Style
import org.brailleblaster.utd.config.ShortcutDefinitions
import org.brailleblaster.utd.config.UTDConfig
import org.brailleblaster.util.StyleId
import org.brailleblaster.util.StyleId.Companion.getWholeFromMain
import org.brailleblaster.util.ui.StyleSelector
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.FormAttachment
import org.eclipse.swt.layout.FormData
import org.eclipse.swt.widgets.*
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Consumer

/**
 * Generates the Styles menu
 */
class StyleMenuBuilder(shell: Shell, manager: Manager) : StylesBuilder(shell, manager) {
    private val bbShell: Shell

    init {
        val utdMan = manager.document.settingsManager
        bbShell = shell
        StyleId(utdMan)

        // label to display loadouts information
        if (label == null) {
            label = Label(bbShell, SWT.NONE)
            label!!.text = ""
            label!!.update()
            val labelData = FormData()
            labelData.left = FormAttachment(80)
            labelData.right = FormAttachment(100)
            labelData.bottom = FormAttachment(100)
            label!!.layoutData = labelData
        }
    }

    fun generateStylesMenu(
        onStyleSelect: Consumer<BBStyleSelection>?,
        onTypeformSelect: Consumer<BBActionSelection>,
        onOptionSelect: Consumer<BBStyleOptionSelection>?,
        onLevelSelect: Consumer<BBSelectionData>
    ) {
        addStyleConfiguration(onStyleSelect, onTypeformSelect, onLevelSelect)
        addSeparator(TopMenu.STYLES)
        if (onOptionSelect != null) {
            addStyleOptions(onOptionSelect, onStyleSelect)
            addSeparator(TopMenu.STYLES)
        }
        onStyleSelect?.let { getStyleCategories(it, onTypeformSelect) }
        addLoadoutListener(onStyleSelect)
    }

    private fun getStyleCategories(
        onStyleSelect: Consumer<BBStyleSelection>,
        onTypeformSelect: Consumer<BBActionSelection>
    ) {
        val userSettings = BBIni.propertyFileManager.getProperty(USER_SETTINGS_STYLE_LEVELS)
        val maxLevels = userSettings?.toInt() ?: DEFAULT_STYLE_LEVELS
        var cont = 1
        val styles = styleDefs.styles
        val categories: HashMap<String, SubMenuBuilder> = LinkedHashMap()
        val subCategories: HashMap<String, SubMenuBuilder> = LinkedHashMap()
        for (style in styles) {
            var id = style.id
            if (id.isEmpty() || id == "/") { //If id is empty or is just a slash, skip it
                continue
            }
            require(id[id.length - 1] != '/') { "Style id should not end with a slash. Id=$id" }
            if (id.indexOf('/') == 0) { //Remove leading slashes
                id = id.substring(1)
            }
            val cat = id.substring(0, id.indexOf('/'))
            if (RESTRICTED_CATEGORIES.contains(cat)) continue
            var subMenu: SubMenuBuilder?
            if (categories.containsKey(cat)) {
                subMenu = categories[cat]
            } else {
                subMenu = SubMenuBuilder(TopMenu.STYLES, localeHandler[cat])
                categories[cat] = subMenu
            }
            var afterCat = id.substring(id.indexOf('/') + 1)
            if (afterCat.indexOf('/') != -1) { //Id has a subcategory
                afterCat = afterCat.substring(0, afterCat.lastIndexOf('/'))
                if (afterCat.contains("Levels")) {
                    val levelsIndex = afterCat.indexOf("Levels")
                    if (levelsIndex > 0 && "0123456789".contains(afterCat.substring(levelsIndex - 1, levelsIndex))) {
                        val curLevel = afterCat.substring(levelsIndex - 1, levelsIndex).toInt()
                        if (curLevel > maxLevels) {
                            continue
                        }
                    }
                }
                val subId = "$cat/$afterCat"
                if (subCategories.containsKey(subId)) {
                    subMenu = subCategories[subId]
                } else {
                    subMenu = SubMenuBuilder(subMenu, localeHandler[afterCat])
                    subCategories[subId] = subMenu
                }
            }
            id = id.substring(id.lastIndexOf('/') + 1)
            val myCurrentLoadout = BBIni.propertyFileManager.getProperty("currentStyleLoadout", "")

            //TODO: Re-add shortcut functionality
            //puttting shortcut in menu
            val tempComp: String = if (myCurrentLoadout.contains("/")) "$cat/$afterCat" else if (afterCat == id) cat else "$cat/$afterCat"
            if (myCurrentLoadout == tempComp && cont <= 9) {
                subMenu!!.addItem(
                    lhb[style.name], SWT.ALT + '0'.code + cont
                ) { e: BBSelectionData -> onStyleSelect.accept(
                    BBStyleSelection(
                        style,
                        e.widget
                    )
                ) }
                cont++
            } else {
                subMenu!!.addItem(
                    lhb[style.name], 0
                ) { e: BBSelectionData -> onStyleSelect.accept(
                    BBStyleSelection(
                        style,
                        e.widget
                    )
                ) }
            }
        }

        //Add the combination TRNote style/action to the miscellaneous category
        if (categories.containsKey(MISCELLANEOUS_CATEGORY_NAME)) {
            val style = styleDefs.getStyleByName(StylesMenuModule.TRNOTE_ACTION_STYLE_NAME)!!
            categories[MISCELLANEOUS_CATEGORY_NAME]!!.addItem(
                StylesMenuModule.TRNOTE_ACTION_STYLE_DISPLAY_NAME,
                0
            ) { e: BBSelectionData -> onStyleSelect.accept(
                BBStyleSelection(
                    style,
                    e.widget
                )
            ) }
        } else {
            logger.error("Miscellaneous style category not found")
        }
        for (subCategory in subCategories.values) {
            subCategory.parentSubMenu!!.addSubMenu(subCategory)
        }
        for ((key, category) in categories) {
            addToStyleMenu(key, category)
            addSubMenu(category)
        }
    }

    private fun addStyleOptions(
        onOptionSelect: Consumer<BBStyleOptionSelection>,
        onStyleSelect: Consumer<BBStyleSelection>?
    ) {
        val smb = SubMenuBuilder(TopMenu.STYLES, "Options")
        getStyleOptions(smb, onOptionSelect, onStyleSelect!!)
        addSubMenu(smb)
    }

    private fun getStyleName(styles: List<Style>, styleId: String): String {
        var styleName = ""
        for (curStyle in styles) {
            if (styleId == curStyle.id) {
                styleName = localeHandler[curStyle.name]
                break
            }
        }
        return styleName
    }

    private fun addStyleConfiguration(
        onStyleSelect: Consumer<BBStyleSelection>?,
        onTypeformSelect: Consumer<BBActionSelection>,
        onLevelSelect: Consumer<BBSelectionData>
    ) {
        listLoadouts()
        val curSetting = BBIni.propertyFileManager.getProperty(USER_SETTINGS_STYLE_LEVELS)
        val curLoadout = BBIni.propertyFileManager.getProperty(CURRENT_STYLE_LOADOUT)
        val curLevel = curSetting?.toInt() ?: DEFAULT_STYLE_LEVELS
        val curAcc = if (curLoadout == null) DEFAULT_LOADOUT else getAcc(curLoadout)
        val configureMenu = SubMenuBuilder(TopMenu.STYLES, "Configure")
        val styleLevelsMenu = SubMenuBuilder(configureMenu, "Style Levels")
        val loadoutsMenu = SubMenuBuilder(configureMenu, "Loadouts")

        // set level options from 3 to 8
        for (i in 3..8) {
            styleLevelsMenu.addCheckItem(i.toString(), 0, i == curLevel) { e: BBSelectionData ->
                val styleLevels = i.toString() //Lambdas are fun
                BBIni.propertyFileManager.save(USER_SETTINGS_STYLE_LEVELS, styleLevels)
                onLevelSelect.accept(e)
            }
        }
        val list = Loadout.list
        for (loadout in list) {
            val name = loadout.name
            val accelerator = loadout.accelerator
            loadoutsMenu.addCheckItem(
                loadout.name, loadout.accelerator, loadout.accelerator
                        == curAcc
            ) { e: BBSelectionData ->
                BBIni.propertyFileManager.save(CURRENT_STYLE_LOADOUT, name)
                showLoadoutDialog(accelerator)
                onLevelSelect.accept(e)
            }
        }
        configureMenu.addSubMenu(loadoutsMenu)
        configureMenu.addSubMenu(styleLevelsMenu)
        addSubMenu(configureMenu)
    }

    private fun showLoadoutDialog(accelerator: Int) {
        val shortcut = getShortcut(accelerator)
        if (shortcut != null) {
            var shortcutId = shortcut.id
            shortcutId = shortcutId.substring(shortcutId.indexOf("/") + 1)
            if (shortcutId == "plays") {
                BBIni.propertyFileManager.save("currentStyleLoadout", shortcutId)
                updateLabel()
            } else {
                // set current style loadout
                var styleIds = getChildren(shortcutId)
                if (styleIds.isNotEmpty()) {
                    BBIni.propertyFileManager.save("currentStyleLoadout", shortcutId)
                    updateLabel()
                }
                styleIds = getSubCategories(shortcutId)
                if (styleIds.isNotEmpty()) {
                    //select subcategory
                    styleIds = getSubCategories(shortcutId)
                    val number = showStyleSelector(styleIds, shortcutId)
                    if (number != -1) {
                        BBIni.propertyFileManager.save("currentStyleLoadout", styleIds[number])
                        updateLabel()
                    }
                }
            }
        }
    }

    private fun addLoadoutListener(onStyleSelect: Consumer<BBStyleSelection>?) {
        if (loadoutListener != null) {
            Display.getCurrent().removeFilter(SWT.KeyDown, loadoutListener)
        }
        loadoutListener = Listener { e: Event ->
            try {
                // CTRL/CMD + SHIFT + letter shortcut: set the style loadout
                if (e.stateMask == SWT.MOD1 + SWT.MOD2 && e.keyCode >= 97 && e.keyCode <= 122) {
                    val accelerator = SWT.MOD1 + SWT.MOD2 + e.keyCode
                    showLoadoutDialog(accelerator)
                }

                // CTRL/CMD + a number 1-8: apply style
                if (e.stateMask == SWT.MOD3 && e.keyCode >= 49 && e.keyCode <= 56) {
                    val currentLoadout = BBIni.propertyFileManager.getProperty("currentStyleLoadout")
                    if (!currentLoadout.isNullOrEmpty()) {
                        val styleIds: List<String> = if (currentLoadout == "plays") {
                            getWholeFromMain(currentLoadout)
                        } else {
                            getChildren(currentLoadout)
                        }
                        if (styleIds.isNotEmpty()) {
                            val index = e.keyCode - 48
                            if (index <= styleIds.size) {
                                val sId = styleIds[index - 1]
                                if (sId.isNotEmpty()) {
                                    val styleName = getStyleName(styleDefs.styles, sId)
                                    val style: IStyle? = styleDefs.getStyleByName(styleName)
                                    if (style != null) {
                                        onStyleSelect!!.accept(
                                            BBStyleSelection(
                                                style,
                                                e.widget
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (ex: Exception) {
                logger.error("Error applying style loadout. " + ex.message)
                throw ex
            }
        }
        Display.getCurrent().addFilter(SWT.KeyDown, loadoutListener)
    }

    private fun updateLabel() {
        label!!.text = currentLoadedStyle
        label!!.update()
    }

    private val currentLoadedStyle: String
        get() {
            val label = StringBuilder()
            val currentLoadout = BBIni.propertyFileManager.getProperty("currentStyleLoadout")
            if (!currentLoadout.isNullOrEmpty()) {
                label.append(localeHandler["loadedStyle"]).append(": ")
                if (currentLoadout.contains("/")) {
                    val styleCategory = currentLoadout.substring(0, currentLoadout.indexOf("/"))
                    val styleSubCategory = currentLoadout.substring(currentLoadout.indexOf("/") + 1)
                    label.append(localeHandler[styleCategory]).append("/").append(
                        localeHandler[styleSubCategory]
                    )
                } else label.append(localeHandler[currentLoadout])
            }
            return label.toString()
        }

    private fun getStyleAccelerator(keyCombination: String): Int {
        return keyCombination.split("+").fold(0) { acc, k -> acc + when(k.uppercase()) {
            "CTRL", "CMD" -> SWT.MOD1
            "SHIFT" -> SWT.MOD2
            "ALT" -> SWT.MOD3
            else -> if (k.length == 1) {
                k[0].code
            } else 0
        } }
    }

    private fun getShortcut(accelerator: Int): Shortcut? {
        var curKeyCombination: String
        var curAcc: Int
        for (curShortcut in shortcutDefinitions.shortcuts) {
            curKeyCombination = curShortcut.keyCombination //.toLowerCase();
            curAcc = getStyleAccelerator(curKeyCombination)
            if (curAcc > 0 && curAcc == accelerator) {
                return curShortcut
            }
        }
        return null
    }

    /*
   * Gets style options for the current loaded style category
   */
    private fun getChildren(styleCategory: String): List<String> {
        val idList: MutableList<String> = ArrayList()
        val styles = styleDefs.styles
        var styleId: String
        var optionId: String
        for (curStyle in styles) {
            styleId = curStyle.id
            if (styleId.startsWith(styleCategory)) {
                optionId = styleId.substring(styleId.indexOf(styleCategory) + styleCategory.length + 1)
                if (!optionId.contains("/")) {
                    idList.add(styleId)
                }
            }
        }
        idList.sortWith(StyleIdComparator())
        return idList
    }

    /*
   * Gets style subcategories
   */
    private fun getSubCategories(styleCategory: String): List<String> {
        val idList: MutableList<String> = ArrayList()
        val styles = styleDefs.styles
        var styleId: String
        var optionId: String
        for (curStyle in styles) {
            styleId = curStyle.id
            if (styleId.startsWith(styleCategory)) {
                //remove category
                optionId = styleId.substring(styleId.indexOf("/") + 1)
                if (optionId.contains("/")) {
                    optionId = styleId.substring(0, styleId.lastIndexOf("/"))
                    if (!idList.contains(optionId)) {
                        idList.add(optionId)
                    }
                }
            }
        }
        return idList
    }

    private fun showStyleSelector(styleIds: List<String>, categoryName: String): Int {
        val shell = Shell()
        val dialog = StyleSelector(shell)
        val ids = styleIds.toTypedArray<String>()
        return dialog.open(localeHandler["styleLoadouts"], ids, categoryName)
    }

    companion object {
        private val localeHandler = getDefault()
        private val lhb = getBanaStyles()
        private val logger = LoggerFactory.getLogger(StyleMenuBuilder::class.java)
        private const val INTERNAL_CATEGORY_NAME = "internal"
        private const val OPTIONS_CATEGORY_NAME = "options"
        private const val MISCELLANEOUS_CATEGORY_NAME = "miscellaneous"
        private const val USER_SETTINGS_STYLE_LEVELS = "styleLevels"
        private const val DEFAULT_STYLE_LEVELS = 8
        private const val DEFAULT_LOADOUT = 393324 //List
        private const val CURRENT_STYLE_LOADOUT = "loadoutName"
        private val RESTRICTED_CATEGORIES: Set<String> =
            HashSet(listOf(INTERNAL_CATEGORY_NAME, OPTIONS_CATEGORY_NAME))
        private val shortcutDefinitions: ShortcutDefinitions
        private const val SHORTCUTS_DEFS_NAME = "shortcutDefs.xml"
        private const val UTD_FOLDER = "utd"
        private var loadoutListener: Listener? = null
        var label: Label? = null

        init {
            shortcutDefinitions =
                UTDConfig.loadShortcutDefinitions(BBIni.loadAutoProgramDataFile(UTD_FOLDER, SHORTCUTS_DEFS_NAME))
        }
    }
}

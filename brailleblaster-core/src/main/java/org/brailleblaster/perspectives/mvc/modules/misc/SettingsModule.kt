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
import org.brailleblaster.settings.ui.*
import org.brailleblaster.tools.MenuToolModule

private val localeHandler = getDefault()

enum class SettingsModule(override val title: String, private val clazz: Class<out SettingsUITab>) : MenuToolModule {
    PagePropertiesTool(
        localeHandler["SettingsModule.pageProperties"],
        PagePropertiesTab::class.java
    ),
    TranslationSettingsTool(
        localeHandler["SettingsModule.translationSettings"],
        TranslationSettingsTab::class.java
    ),
    PageNumberSettingsTool(
        localeHandler["SettingsModule.pageNumbers"],
        PageNumbersTab::class.java
    ),
    FormatSettingstool(
        localeHandler["SettingsModule.formatSettings"],
        FormatSettingsTab::class.java
    ),
    EmbosserSettingsTool(
        localeHandler["SettingsModule.embosserSettings"],
        EmbosserSettingsTab::class.java
    ),
    StatusBarSettingsTool(
        localeHandler["SettingsModule.statusBar"],
        StatusBarSettingsTab::class.java
    );

    override val topMenu = TopMenu.SETTINGS
    override fun onRun(bbData: BBSelectionData) {
        BrailleSettingsDialog(bbData.manager, clazz)
    }

    companion object {
        val tools = entries
    }
}
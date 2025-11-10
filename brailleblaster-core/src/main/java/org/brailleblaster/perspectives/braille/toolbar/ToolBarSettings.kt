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
package org.brailleblaster.perspectives.braille.toolbar

import org.brailleblaster.BBIni
import org.brailleblaster.util.LINE_BREAK
import java.util.*

object ToolBarSettings {
    var defaultSettings = listOf(
        Settings.FILE, Settings.DOCUMENT,
        Settings.VIEW, Settings.NEWLINE, Settings.STYLE
    )
    val DEFAULT_SCALE = Scale.LARGE
    fun getEquivalentSetting(setting: String): Settings? {
        for (value in Settings.entries) {
            if (value.name == setting) return value
        }
        return null
    }

    val userSettings: MutableList<Settings>
        get() {
            var settings = BBIni.propertyFileManager.getProperty("toolBar")
                ?: return ArrayList(defaultSettings)
            settings = settings.replace(" ".toRegex(), "").replace(LINE_BREAK, "")
            val parsedSettings = parseToolBarSettings(settings)
            verifySettings(parsedSettings)
            return parsedSettings
        }

    fun saveUserSettings(settings: MutableList<Settings>) {
        verifySettings(settings)
        val settingBuilder = StringBuilder()
        for (setting in settings) {
            settingBuilder.append(setting.name)
            if (settings.indexOf(setting) != settings.size - 1) settingBuilder.append(",")
        }
        BBIni.propertyFileManager.save("toolBar", settingBuilder.toString())
    }

    fun enableSetting(setting: Settings?) {
        if (setting == null) return
        val settings = userSettings
        if (!settings.contains(setting)) {
            settings.add(setting)
        }
        saveUserSettings(settings)
    }

    fun disableSetting(setting: Settings?) {
        if (setting == null) return
        val settings = userSettings
        settings.remove(setting)
        saveUserSettings(settings)
    }

    fun insertSetting(setting: Settings?, index: Int) {
        if (setting == null) return
        val settings: MutableList<Settings> = userSettings
        require(!(index < 0 || index >= settings.size)) { "Invalid index " + index + " for settings size " + settings.size }
        settings.add(index, setting)
        saveUserSettings(settings)
    }

    fun verifySettings(settings: MutableList<Settings>) {
        if (settings.isEmpty()) {
            return
        }
        if (settings[0] == Settings.NEWLINE) settings.removeAt(0)
        var prevSetting: Settings
        run {
            var i = 1
            while (i < settings.size) {
                //Remove neighboring newlines
                prevSetting = settings[i - 1]
                if (prevSetting == Settings.NEWLINE && settings[i] == Settings.NEWLINE) {
                    settings.removeAt(i)
                    i--
                }
                i++
            }
        }
        var i = 0
        while (i < settings.size) {
            //Remove duplicates
            if (settings[i] == Settings.NEWLINE) {
                i++
                continue
            }
            if (settings.indexOf(settings[i]) != settings.lastIndexOf(settings[i])) {
                settings.removeAt(i)
                i--
            }
            i++
        }
        if (settings.isNotEmpty() && settings[settings.size - 1] == Settings.NEWLINE) //Remove trailing newlines
            settings.removeAt(settings.size - 1)
    }

    var scale: Scale
        get() {
            val scale = BBIni.propertyFileManager.getProperty("toolBarScale")
            return if (scale != null) {
                when (scale.uppercase(Locale.getDefault())) {
                    "SMALL" -> Scale.SMALL
                    "MEDIUM" -> Scale.MEDIUM
                    "LARGE" -> Scale.LARGE
                    else -> DEFAULT_SCALE
                }
            } else DEFAULT_SCALE
        }
        set(scale) {
            BBIni.propertyFileManager.save("toolBarScale", scale.name)
        }

    private fun parseToolBarSettings(settings: String): MutableList<Settings> {
        val settingList: MutableList<Settings> = ArrayList()
        val split = settings.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (setting in split) {
            val newSetting = getEquivalentSetting(setting)
            if (newSetting != null) settingList.add(newSetting)
        }
        return settingList
    }

    /**
     * WARNING: The names of these enums are directly used to make the menu options
     * under the Window menu.
     *
     */
    enum class Settings {
        FILE,
        DOCUMENT,
        VIEW,
        STYLE,
        TOOLS,
        EMPHASIS,
        NEWLINE,
        MATH
    }

    /**
     * WARNING: The names of these enums are directly used to make the menu options
     * under the Window menu.
     *
     */
    enum class Scale(val scale: String) {
        SMALL("small"),
        MEDIUM("medium"),
        LARGE("large")
    }
}

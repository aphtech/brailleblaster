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

import org.brailleblaster.perspectives.braille.toolbar.ToolBarSettings
import org.brailleblaster.perspectives.mvc.BBSimpleManager.SimpleListener
import org.brailleblaster.perspectives.mvc.menu.MenuManager
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.events.BuildMenuEvent
import org.brailleblaster.perspectives.mvc.menu.SubMenuBuilder
import org.brailleblaster.wordprocessor.WPManager
import org.eclipse.swt.widgets.MenuItem
import java.util.*

class ToolBarModule(private val wp: WPManager) : SimpleListener {
    override fun onEvent(event: SimpleEvent) {
        if (event is BuildMenuEvent) {
            val smb =
                SubMenuBuilder(TopMenu.VIEW, "Toolbar")
            for (setting in ToolBarSettings.Settings.entries) {
                if (setting == ToolBarSettings.Settings.NEWLINE) continue
                smb.addCheckItem(
                    (setting.name.lowercase(Locale.getDefault())).replaceFirstChar { it.titlecase() }, 0,
                    ToolBarSettings.userSettings.contains(setting)
                ) { e: BBSelectionData ->
                    val widget = e.widget as MenuItem
                    if (widget.selection) {
                        ToolBarSettings.enableSetting(setting)
                    } else {
                        ToolBarSettings.disableSetting(setting)
                    }
                    wp.buildToolBar()
                }
            }
            MenuManager.add(smb.build())
            val userScale = ToolBarSettings.scale
            val iconSMB =
                SubMenuBuilder(TopMenu.VIEW, "Icon Size")
            for (scale in ToolBarSettings.Scale.entries) {
                iconSMB.addRadioItem(
                    (scale.name.lowercase(Locale.getDefault())).replaceFirstChar { it.titlecase() }, 0,
                    scale == userScale
                ) {
                    ToolBarSettings.scale = scale
                    wp.buildToolBar()
                }
            }
            MenuManager.add(iconSMB.build())
        }
    }
}

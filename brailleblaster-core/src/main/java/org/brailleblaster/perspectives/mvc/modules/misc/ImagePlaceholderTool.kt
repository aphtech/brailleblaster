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
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.ImagePlaceholderTextMapElement
import org.brailleblaster.perspectives.braille.mapping.interfaces.Uneditable
import org.brailleblaster.perspectives.braille.stylers.ImagePlaceholderHandler
import org.brailleblaster.perspectives.braille.ui.ImagePlaceholder
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.tools.MenuToolModule

object ImagePlaceholderTool : MenuToolModule {
    private val localeHandler = getDefault()
    @JvmField
    val MENU_ITEM_NAME = localeHandler["&ImagePlaceholder"]
    override val topMenu = TopMenu.INSERT
    override val title = MENU_ITEM_NAME
    override fun onRun(bbData: BBSelectionData) {
        insertImagePlaceholder(bbData.manager)
    }

    private fun insertImagePlaceholder(manager: Manager) {
        if (!manager.isEmptyDocument) {
            if (manager.mapList.current is Uneditable) {
                (manager.mapList.current as Uneditable).blockEdit(manager)
                return
            }
            manager.stopFormatting()
            manager.text.update(false)
            val trh = ImagePlaceholderHandler(manager, manager.viewInitializer, manager.mapList)
            ImagePlaceholder(manager.wpManager.shell, manager) { list: ArrayList<String?> ->
                val imagePath = list[1]
                if (list[0] != null) {
                    val lines = list[0]!!.toInt()
                    if (manager.mapList.current is ImagePlaceholderTextMapElement) {
                        trh.adjustImagePlaceholder(lines, imagePath)
                    } else {
                        trh.insertNewImagePlaceholder(lines, imagePath)
                    }
                } else {
                    trh.updateImagePlaceholderPath(imagePath)
                }
            }
        } else {
            manager.notify(localeHandler["emptyDocMenuWarning"])
        }
    }



}
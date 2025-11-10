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
package org.brailleblaster.math.spatial

import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.math.spatial.SpatialMathEnum.Passage
import org.brailleblaster.math.template.TemplateConstants
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.MenuItem
import org.eclipse.swt.widgets.Shell
import java.util.function.Consumer

interface ISpatialMathDialog {
    fun open()
    fun safeClose()
    fun register()
    val settingsMenu: Menu
    fun createSettingsMenu(menu: Menu, shell: Shell): Menu {
        val cascadeMenu = MenuItem(menu, SWT.CASCADE)
        cascadeMenu.text = TemplateConstants.SETTINGS
        val dropDownMenu = Menu(shell, SWT.DROP_DOWN)
        cascadeMenu.menu = dropDownMenu
        return dropDownMenu
    }

    fun createMenu(shell: Shell): Menu {
        val menu = Menu(shell, SWT.BAR)
        shell.menuBar = menu
        return menu
    }

    fun addPassages(shell: Shell, menu: Menu, settings: ISpatialMathSettings, callback: Consumer<Passage>) {
        if (MathModuleUtils.isNemeth) {
            val nemethButton = MenuItem(menu, SWT.CHECK)
            nemethButton.text = Passage.NEMETH.prettyName
            nemethButton.selection = settings.passage == Passage.NEMETH
            nemethButton.addListener(SWT.Selection) {
                if (nemethButton.selection) {
                    callback.accept(Passage.NEMETH)
                } else {
                    callback.accept(Passage.NONE)
                }
                nemethButton.selection = settings.passage == Passage.NEMETH
            }
        } else {
            val cascadeMenu = MenuItem(menu, SWT.CASCADE)
            cascadeMenu.text = GridConstants.PASSAGE_TYPE
            val dropDownMenu = Menu(shell, SWT.DROP_DOWN)
            cascadeMenu.menu = dropDownMenu
            val grade1Button = MenuItem(dropDownMenu, SWT.RADIO)
            grade1Button.text = Passage.GRADE1.prettyName
            grade1Button.selection = settings.passage == Passage.GRADE1
            grade1Button.addListener(SWT.Selection) {
                if (grade1Button.selection) {
                    callback.accept(Passage.GRADE1)
                    grade1Button.selection = settings.passage == Passage.GRADE1
                }
            }
            val numericButton = MenuItem(dropDownMenu, SWT.RADIO)
            numericButton.text = Passage.NUMERIC.prettyName
            numericButton.selection = settings.passage == Passage.NUMERIC
            numericButton.addListener(SWT.Selection) {
                if (numericButton.selection) {
                    callback.accept(Passage.NUMERIC)
                    numericButton.selection = settings.passage == Passage.NUMERIC
                }
            }
            val noneButton = MenuItem(dropDownMenu, SWT.RADIO)
            noneButton.text = Passage.NONE.prettyName
            noneButton.selection = settings.passage == Passage.NONE
            noneButton.addListener(SWT.Selection) {
                if (noneButton.selection) {
                    callback.accept(Passage.NONE)
                    noneButton.selection = settings.passage == Passage.NONE
                }
            }
        }
    }
}

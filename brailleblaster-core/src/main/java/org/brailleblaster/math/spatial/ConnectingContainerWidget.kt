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

import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.math.spatial.SpatialMathEnum.HorizontalJustify
import org.brailleblaster.math.spatial.SpatialMathEnum.VerticalJustify
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.MenuItem
import org.eclipse.swt.widgets.Shell

class ConnectingContainerWidget : ISpatialMathWidget {
    private var st: StyledText? = null
    var cc: ConnectingContainer? = null
    override fun fillDebug(t: ISpatialMathContainer) {
        cc = t as ConnectingContainer
        cc!!.text = MathText(print=DEBUG_TEXT,
            braille=
                if (cc!!.settings.isTranslateAsMath) MathModuleUtils.translateMathPrint(DEBUG_TEXT) else MathModuleUtils.translateMainPrint(
                    DEBUG_TEXT
                )
            )
    }

    override fun getWidget(parent: Composite, container: ISpatialMathContainer): Composite {
        cc = container as ConnectingContainer
        st = StyledText(parent, SWT.NONE)
        st!!.data = GridData(SWT.FILL, SWT.FILL, true, true)
        st!!.text = " ".repeat(50) + "\n\n"
        return parent
    }

    override fun onOpen() {
        st!!.text = cc!!.printText
    }

    override fun extractText() {
        if (cc!!.settings.isTranslateAsMath) {
            cc!!.text = MathText(print=st!!.text,
                braille=MathModuleUtils.translateMathPrint(st!!.text))
        } else {
            cc!!.text = MathText(print=st!!.text,
                braille=MathModuleUtils.translateMainPrint(st!!.text))
        }
    }

    override fun addMenuItems(shell: Shell, menu: Menu, settingsMenu: Menu): Menu {
        addVerticalMenu(shell, menu)
        addHorizontalMenu(shell, menu)
        addTranslationMenu(shell, settingsMenu)
        return menu
    }

    private fun addTranslationMenu(shell: Shell, menu: Menu) {
        val numericPassage = MenuItem(menu, SWT.CHECK)
        numericPassage.text = TRANSLATE_AS_MATH
        numericPassage.selection = cc!!.settings.isTranslateAsMath
        numericPassage.addListener(SWT.Selection) {
            cc!!.settings.isTranslateAsMath = !cc!!.settings.isTranslateAsMath
        }
    }

    private fun addHorizontalMenu(shell: Shell, menu: Menu) {
        val cascadeMenu = MenuItem(menu, SWT.CASCADE)
        cascadeMenu.text = HORIZONTAL_LABEL
        val dropDownMenu = Menu(shell, SWT.DROP_DOWN)
        cascadeMenu.menu = dropDownMenu
        for (vj in HorizontalJustify.entries.toTypedArray()) {
            val blankBlock = MenuItem(dropDownMenu, SWT.RADIO)
            blankBlock.text = vj.name
            blankBlock.selection = vj == cc!!.settings.horizontal
            blankBlock.addListener(SWT.Selection) { cc!!.settings.horizontal = vj }
        }
    }

    private fun addVerticalMenu(shell: Shell, menu: Menu) {
        val cascadeMenu = MenuItem(menu, SWT.CASCADE)
        cascadeMenu.text = VERTICAL_LABEL
        val dropDownMenu = Menu(shell, SWT.DROP_DOWN)
        cascadeMenu.menu = dropDownMenu
        for (vj in VerticalJustify.entries.toTypedArray()) {
            val blankBlock = MenuItem(dropDownMenu, SWT.RADIO)
            blankBlock.text = vj.name
            blankBlock.selection = vj == cc!!.settings.vertical
            blankBlock.addListener(SWT.Selection) { cc!!.settings.vertical = vj }
        }
    }

    companion object {
        private val localeHandler = getDefault()
        private val VERTICAL_LABEL = localeHandler["verticalLabel"]
        private val HORIZONTAL_LABEL = localeHandler["horizontalLabel"]
        private val TRANSLATE_AS_MATH = localeHandler["translateAsMath"]
        private const val DEBUG_TEXT = "Debug text"
    }
}

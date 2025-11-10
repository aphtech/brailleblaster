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
package org.brailleblaster.math.numberLine

import org.brailleblaster.math.spatial.ISpatialMathContainer
import org.brailleblaster.math.spatial.MathFormattingException
import org.brailleblaster.math.spatial.MathText
import org.brailleblaster.math.spatial.SpatialMathDispatcher
import org.brailleblaster.math.spatial.SpatialMathEnum.LabelPosition
import org.brailleblaster.math.spatial.SpatialMathEnum.NumberLineSection
import org.brailleblaster.math.spatial.SpatialMathUtils.translate
import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.swt.SWT
import org.eclipse.swt.events.ModifyEvent
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.MenuItem
import org.eclipse.swt.widgets.Shell

class NumberLineWidgetAddLabels : NumberLineWidget() {
    override fun getWidget(parent: Composite, container: ISpatialMathContainer): Composite {
        numberLine = container as NumberLine
        makeLabelEditBoxes(parent)
        addDoneButton(parent)
        return parent
    }

    private fun makeLabelEditBoxes(shell: Composite) {
        val g = EasySWT.makeGroup(shell, SWT.NONE, 3, false)
        for (i in numberLine.settings.userDefinedArray.indices) {
            val interval = numberLine.settings.userDefinedArray[i]
            EasySWT.makeLabel(g, NumberLineConstants.MARKER_LABEL + " " + (i + 1), 1)
            val t = EasySWT.makeText(g, 50, 1)
            t.editable = false
            if (!interval.mathText.isEmpty) {
                try {
                    t.text = NumberLineMathUtils.getFractionString(
                        numberLine, interval.mathText.fraction
                    )
                } catch (e1: MathFormattingException) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace()
                }
            } else {
                t.text = interval.userText.print
            }
            val t2 = EasySWT.makeText(g, 50, 1)
            EasySWT.addModifyListener(
                t2
            ) { it: ModifyEvent ->
                interval.labelText = NumberLinePoint(
                    format = false,
                    mathText =
                        MathText(
                            print = t2.text,
                            braille = translate(numberLine.settings.translationLabel, t2.text)
                        )
                )
            }
            t2.text = interval.labelText.mathText.print
        }
    }

    override fun onOpen() {
        // TODO Auto-generated method stub
    }

    override fun extractText() {
        // TODO Auto-generated method stub
    }

    override fun fillDebug(t: ISpatialMathContainer) {
        // TODO Auto-generated method stub
    }

    override fun addMenuItems(shell: Shell, menu: Menu, settingsMenu: Menu): Menu {
        addNumberLineType(shell, menu)
        addOptionsMenu(shell, menu, settingsMenu)
        addPositionOptions(shell, menu)
        addSectionTypeMenu(shell, menu)
        if (NumberLineSection.SEGMENT == numberLine.settings.sectionType) {
            addStartSegmentType(shell, menu)
            addEndSegmentType(shell, menu)
        } else {
            addAdditionalSegmentDropDown(shell, menu)
            addPointsType(shell, menu)
        }
        return menu
    }

    private fun addPositionOptions(shell: Shell, menu: Menu) {
        val cascadeMenu = MenuItem(menu, SWT.CASCADE)
        cascadeMenu.text = NumberLineConstants.LABEL_POSITION
        val dropDownMenu = Menu(shell, SWT.DROP_DOWN)
        cascadeMenu.menu = dropDownMenu
        for (i in LabelPosition.entries.toTypedArray().indices) {
            val position = LabelPosition.entries[i]
            val b = MenuItem(dropDownMenu, SWT.RADIO)
            b.text = position.prettyName
            b.selection = position == numberLine.settings.labelPosition
            b.addListener(
                SWT.Selection
            ) {
                if (!b.selection) {
                    return@addListener
                }
                numberLine.settings.labelPosition = position
                SpatialMathDispatcher.dispatch()
            }
        }
    }
}

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
import org.brailleblaster.math.spatial.NemethTranslations
import org.brailleblaster.math.spatial.SpatialMathEnum.BlankOptions
import org.brailleblaster.math.spatial.SpatialMathEnum.NumberLineSection
import org.brailleblaster.math.spatial.UebTranslations
import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.Shell

class NumberLineWidgetAddBlanks : NumberLineWidget() {
    override fun getWidget(parent: Composite, container: ISpatialMathContainer): Composite {
        numberLine = container as NumberLine
        //Formatting is required to re-compute the number of points in the array.
        numberLine.format()
        addIntervalEdit(parent)
        addDoneButton(parent)
        return parent
    }

    private fun addIntervalEdit(shell: Composite) {
        if (numberLine.settings.userDefinedArray.size != numberLine.points.size) {
            numberLine.settings.numUserDefinedIntervals = numberLine.points.size
            //Could it really be this simple? Yes!
        }
        for (i in numberLine.settings.userDefinedArray.indices) {
            val interval = numberLine.settings.userDefinedArray[i]
            val g2 = EasySWT.makeGroup(shell, SWT.NONE, 3, false)
            val mathText = numberLine.points[i].mathText.print
            val intervalText = when {
            mathText == UebTranslations.OMISSION || mathText == NemethTranslations.OMISSION
                -> BlankOptions.OMISSION.prettyName
            mathText.isBlank() -> BlankOptions.BLANK.prettyName
            else -> mathText
            }
            g2.text = NumberLineConstants.MARKER_LABEL + " (" + intervalText + ")"
            for (j in BlankOptions.entries.toTypedArray().indices) {
                val option = BlankOptions.entries[j]
                val b = EasySWT.makeRadioButton(g2, option.prettyName, 1) { }
                EasySWT.addSelectionListener(b) { it: SelectionEvent ->
                    if (b.selection) {
                        interval.blankType = option
                    }
                }
                b.selection = interval.blankType == option
            }
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
}

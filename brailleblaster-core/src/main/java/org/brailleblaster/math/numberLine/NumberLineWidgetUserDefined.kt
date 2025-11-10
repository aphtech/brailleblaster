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
import org.brailleblaster.math.spatial.MathText
import org.brailleblaster.math.spatial.SpatialMathDispatcher
import org.brailleblaster.math.spatial.SpatialMathEnum.NumberLineSection
import org.brailleblaster.math.spatial.SpatialMathUtils.translate
import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.swt.SWT
import org.eclipse.swt.events.ModifyEvent
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.*

class NumberLineWidgetUserDefined : NumberLineWidget() {
    private fun addIntervalNumbers(shell: Shell, menu: Menu) {
        val cascadeMenu = MenuItem(menu, SWT.CASCADE)
        cascadeMenu.text = NumberLineConstants.MARKER_LABEL
        val dropDownMenu = Menu(shell, SWT.DROP_DOWN)
        cascadeMenu.menu = dropDownMenu
        for (i in 2..INTERVAL_LIMIT) {
            val s = i.toString()
            val b = MenuItem(dropDownMenu, SWT.RADIO)
            b.text = s
            b.selection = numberLine.settings.userDefinedArray.size == i
            b.addListener(SWT.Selection) {
                if (!b.selection) {
                    return@addListener
                }
                numberLine.settings.numUserDefinedIntervals = i
                SpatialMathDispatcher.dispatch()
            }
        }
    }

    override fun addMenuItems(shell: Shell, menu: Menu, settingsMenu: Menu): Menu {
        addNumberLineType(shell, menu)
        addOptionsMenu(shell, menu, settingsMenu)
        addIntervalNumbers(shell, menu)
        addSectionTypeMenu(shell, menu)
        if (NumberLineSection.SEGMENT == numberLine.settings.sectionType) {
            addStartSegmentType(shell, menu)
            addEndSegmentType(shell, menu)
        } else if (NumberLineSection.POINTS == numberLine.settings.sectionType) {
            addAdditionalSegmentDropDown(shell, menu)
            addPointsType(shell, menu)
        }
        return menu
    }

    private fun setTextData() {
        // TODO
    }

    private fun extractInfoFromTextBoxes() {
        // TODO
    }

    private fun defaultDebug() {
        for (i in 0..3) {
            if (numberLine.settings.userDefinedArray.size < i) {
                numberLine.settings.userDefinedArray
                    .add(
                        NumberLineInterval(
                            userText =
                            MathText(
                                print = (i + 1).toString(),
                                braille =
                                translate(
                                    numberLine.settings.translationUserDefined, (i + 1).toString()
                                )
                            )
                        )
                    )
            }
        }
        SpatialMathDispatcher.dispatch()
    }

    override fun fillDebug(t: ISpatialMathContainer) {
        numberLine = t as NumberLine
        defaultDebug()
    }

    override fun onOpen() {
        setTextData()
    }

    override fun extractText() {
        extractInfoFromTextBoxes()
    }

    override fun getWidget(parent: Composite, container: ISpatialMathContainer): Composite {
        numberLine = container as NumberLine
        makeSegmentEditBoxes(parent)
        if (NumberLineSection.SEGMENT == numberLine.settings.sectionType) {
            makeStartSegmentCombo(parent)
            makeEndSegmentCombo(parent)
        } else if (NumberLineSection.POINTS == numberLine.settings.sectionType) {
            numberLine.initializePoints()
            makePointCombo(parent)
        }
        addBlankAndLabels(parent)
        return parent
    }

    private fun makePointCombo(shell: Composite) {
        val g = EasySWT.makeGroup(shell, SWT.NONE, 1, true)
        g.text = NumberLineConstants.POINT
        for (j in numberLine.numberLineText.points.indices) {
            val point = numberLine.segmentPoints[j]
            val items = arrayOfNulls<String>(numberLine.settings.numUserDefinedIntervals)
            for (i in 0 until numberLine.settings.numUserDefinedIntervals) {
                items[i] = NumberLineConstants.MARKER_LABEL + " " + (i + 1)
            }
            val c = Combo(g, SWT.DROP_DOWN or SWT.READ_ONLY)
            c.data = GridData(SWT.FILL, SWT.FILL, true, true)
            c.setItems(*items)
            c.select(c.indexOf(NumberLineConstants.MARKER_LABEL + " " + point.interval))
            EasySWT.addSelectionListener(c) { it: SelectionEvent ->
                val index = c.selectionIndex
                point.interval = index + 1
            }
        }
    }

    private fun makeSegmentEditBoxes(shell: Composite) {
        val g = EasySWT.makeGroup(shell, SWT.NONE, 2, true)
        for (i in 0 until numberLine.settings.numUserDefinedIntervals) {
            EasySWT.makeLabel(g, NumberLineConstants.MARKER_LABEL + " " + (i + 1), 1)
            val t = EasySWT.makeText(g, 50, 1)
            if (numberLine.settings.userDefinedArray.size > i) {
                t.text = numberLine.settings.userDefinedArray[i].userText.print
            }
            EasySWT.addModifyListener(t) { it: ModifyEvent ->
                val print = t.text
                val braille = translate(
                    numberLine.settings.translationUserDefined,
                    print
                )
                if (numberLine.settings.userDefinedArray.size > i) {
                    numberLine.settings.userDefinedArray[i]
                        .userText = MathText(print = print, braille = braille)
                } else {
                    numberLine.settings.userDefinedArray
                        .add(
                            NumberLineInterval(userText = MathText(print = print, braille = braille))
                        )
                }
            }
        }
    }

    private fun makeEndSegmentCombo(shell: Composite) {
        val g = EasySWT.makeGroup(shell, SWT.NONE, 2, true)
        EasySWT.makeLabel(g, NumberLineConstants.SEGMENT_END_LABEL, 1)
        val c = Combo(g, SWT.READ_ONLY or SWT.DROP_DOWN)
        c.data = GridData(SWT.FILL, SWT.FILL, true, true)
        val items = arrayOfNulls<String>(numberLine.settings.numUserDefinedIntervals)
        for (i in 0 until numberLine.settings.numUserDefinedIntervals) {
            items[i] = NumberLineConstants.MARKER_LABEL + " " + (i + 1)
        }
        c.setItems(*items)
        c.select(
            c.indexOf(
                NumberLineConstants.MARKER_LABEL + " "
                        + numberLine.settings.segmentEndInterval
            )
        )
        c.addSelectionListener(object : SelectionListener {
            override fun widgetSelected(e: SelectionEvent) {
                val s = c.text
                val i = s.substring(s.length - 1).toInt()
                numberLine.settings.segmentEndInterval = i
                numberLine.numberLineText.segment.endInterval = i
            }

            override fun widgetDefaultSelected(e: SelectionEvent) {}
        })
    }

    private fun makeStartSegmentCombo(shell: Composite) {
        val g = EasySWT.makeGroup(shell, SWT.NONE, 2, true)
        EasySWT.makeLabel(g, NumberLineConstants.SEGMENT_START_LABEL, 1)
        val c = Combo(g, SWT.READ_ONLY or SWT.DROP_DOWN)
        c.data = GridData(SWT.FILL, SWT.FILL, true, true)
        val items = arrayOfNulls<String>(numberLine.settings.numUserDefinedIntervals)
        for (i in 0 until numberLine.settings.numUserDefinedIntervals) {
            items[i] = NumberLineConstants.MARKER_LABEL + " " + (i + 1)
        }
        c.setItems(*items)
        c.select(
            c.indexOf(
                NumberLineConstants.MARKER_LABEL + " "
                        + numberLine.settings.segmentStartInterval
            )
        )
        c.addSelectionListener(object : SelectionListener {
            override fun widgetSelected(e: SelectionEvent) {
                val s = c.text
                val i = s.substring(s.length - 1).toInt()
                numberLine.settings.segmentStartInterval = i
                numberLine.numberLineText.segment.startInterval = i
            }

            override fun widgetDefaultSelected(e: SelectionEvent) {}
        })
    }

    companion object {
        private const val INTERVAL_LIMIT = 10
    }
}

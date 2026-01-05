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
package org.brailleblaster.debug

import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.TableTextMapElement
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyleRange
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell

class MapListDebugger(private val m: Manager) {
    private var dialog: Shell? = null
    private lateinit var mapListViewer: StyledText
    private var showBrailleList = false

    fun open() {
        val dialog = EasySWT.makeDialogFloating(m.wpManager.shell)
        this.dialog = dialog
        dialog.setSize(400, 300)
        dialog.layout = GridLayout(3, false)

        mapListViewer = StyledText(dialog, SWT.MULTI or SWT.BORDER or SWT.V_SCROLL or SWT.READ_ONLY)
        EasySWT.setGridData(mapListViewer)
        (mapListViewer.layoutData as GridData).horizontalSpan = 3
        (mapListViewer.layoutData as GridData).grabExcessVerticalSpace = true

        val updateButton = Button(dialog, SWT.PUSH)
        updateButton.text = "Update"
        updateButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                mapListViewer.text = setMapText(m.mapList.current, m.mapList.currentIndex)
            }
        })

        val seeWholeList = Button(dialog, SWT.PUSH)
        seeWholeList.text = "See Whole List"
        EasySWT.addSelectionListener(seeWholeList) { it: SelectionEvent -> reload() }

        val showBrailleButton = Button(dialog, SWT.CHECK)
        showBrailleButton.text = "Show Braille MapList"
        showBrailleButton.selection = showBrailleList
        EasySWT.addSelectionListener(showBrailleButton) { it: SelectionEvent ->
            showBrailleList = showBrailleButton.selection
            reload()
        }

        dialog.open()
    }

    private fun reload() {
        val output = m.mapList.printContents(showBrailleList)
        mapListViewer.text = output

        var start = 0
        var nextStart: Int
        while ((output.indexOf("\n", start).also { nextStart = it }) != -1) {
            val nextNewLine = output.indexOf("\n", nextStart + 1)
            if (nextNewLine == -1) {
                break
            }
            if (output[nextStart + 1] == '-') {
                val range = StyleRange()
                range.start = nextStart
                range.length = nextNewLine - nextStart

                range.background = Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA)
                range.background = Display.getCurrent().getSystemColor(SWT.COLOR_GREEN)

                mapListViewer.setStyleRange(range)
            } else if (output.substring(nextStart, nextNewLine).contains("Offsets:-1")
                || output.substring(nextStart, nextNewLine).contains("--1")
            ) {
                //Detects improperly set maplist offsets
                val range = StyleRange()
                range.start = nextStart
                range.length = nextNewLine - nextStart

                range.background = Display.getCurrent().getSystemColor(SWT.COLOR_RED)

                mapListViewer.setStyleRange(range)
            }
            start = nextNewLine
        }
        EasySWT.setLargeDialogSize(dialog!!)
    }

    fun setMapText(newElem: TextMapElement, curIndex: Int): String {
        val newText = StringBuilder()
        newText.append("Current TextMapElement: ").append(newElem.node.toXML()).append("\nSize of BrailleList: ")
            .append(newElem.brailleList.size).append("\nIndex: ").append(curIndex).append("  Offsets: Start: ").append(
            newElem.getStart(
                m.mapList
            )
        ).append(" End: ").append(newElem.getEnd(m.mapList)).append("\nContents of BrailleList:")
        for (i in newElem.brailleList.indices) {
            newText.append("\n\t").append(newElem.brailleList[i].text).append(" Braille View Offsets: ").append(
                newElem.brailleList[i].getStart(
                    m.mapList
                )
            ).append("-").append(newElem.brailleList[i].getEnd(m.mapList))
        }
        if (newElem is TableTextMapElement) {
            newText.append("\n----- Table Elements -----\n")
            for ((k, cell) in newElem.tableElements.withIndex()) {
                newText.append(setMapText(cell, k)).append("\n-----\n")
            }
            newText.append("\n--------------------------")
        }
        return newText.toString()
    }
}

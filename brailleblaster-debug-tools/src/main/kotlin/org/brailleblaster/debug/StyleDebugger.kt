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

import nu.xom.Node
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text

class StyleDebugger(parent: Shell?, style: Int, private val m: Manager) {
    private var styleViewer: Text? = null
    private var curNode: Node? = null

    fun open() {
        val dialog = EasySWT.makeDialogFloating(m.wpManager.shell)
        dialog.text = "Debug: Style Viewer"
        dialog.setSize(400, 600)
        dialog.layout = GridLayout(2, false)

        styleViewer = Text(dialog, SWT.MULTI or SWT.BORDER or SWT.V_SCROLL or SWT.WRAP or SWT.READ_ONLY)
        styleViewer!!.layoutData = GridData(SWT.FILL, SWT.FILL, true, true, 2, 1)

        val updateButton = Button(dialog, SWT.PUSH)
        updateButton.layoutData = GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 1, 1)
        updateButton.text = "Update"
        updateButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                setStyleText(m.mapList.current.node)
            }
        })

        val getParentButton = Button(dialog, SWT.PUSH)
        getParentButton.layoutData = GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 1, 1)
        getParentButton.text = "Get Parent Style"
        getParentButton.addSelectionListener(object : SelectionListener {
            override fun widgetDefaultSelected(arg0: SelectionEvent) {
            }

            override fun widgetSelected(arg0: SelectionEvent) {
                if (curNode!!.parent != null) {
                    setStyleText(curNode!!.parent)
                }
            }
        })

        dialog.open()
    }

    fun setStyleText(n: Node?) {
        curNode = n
        val style = m.getStyle(curNode!!)
        if (style != null) {
            styleViewer!!.text = style.toString()
        }
    }
}

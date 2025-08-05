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

import org.brailleblaster.bbx.BBX
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.tools.MenuToolModule
import org.brailleblaster.utd.PageSettings
import org.brailleblaster.utd.config.DocumentUTDConfig
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text

class RunningHeadTool : MenuToolModule {
    override val topMenu = TopMenu.INSERT
    override val title = "Running Head"
    var documentTitle = ""
    override fun onRun(bbData: BBSelectionData) {
        insertRunningHead(bbData.manager)
    }

    fun insertRunningHead(manager: Manager) {
        val pageSettings = manager.document.engine.pageSettings
        val rhShell = Shell(manager.wpManager.shell, SWT.APPLICATION_MODAL or SWT.CLOSE).apply {
            setSize(300, 200)
            text = "Running Head"
            layout = GridLayout(1, false)
        }
        val noButton = Button(rhShell, SWT.RADIO)
        val titleButton = Button(rhShell, SWT.RADIO)
        val runHeadText = Text(rhShell, SWT.BORDER)
        val buttonOK = Button(rhShell, SWT.PUSH)
        runHeadText.isEnabled = false
        val gridLayout = GridLayout()
        gridLayout.numColumns = 2
        rhShell.layout = gridLayout
        noButton.text = "No running head"
        noButton.layoutData = GridData(SWT.FILL, SWT.FILL, true, false, 2, 1)
        if (pageSettings.defaultRunningHeadOption == PageSettings.RunningHeadOptions.NONE) {
            noButton.selection = true
        }
        noButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                runHeadText.isEnabled = false
                documentTitle = ""
                pageSettings.defaultRunningHeadOption = PageSettings.RunningHeadOptions.NONE
            }
        })
        titleButton.text = "Enter title:"
        titleButton.layoutData = GridData(SWT.FILL, SWT.FILL, false, false, 1, 1)
        if (pageSettings.defaultRunningHeadOption == PageSettings.RunningHeadOptions.TEXT) {
            titleButton.selection = true
            runHeadText.isEnabled = true
            runHeadText.text = pageSettings.runningHead
            documentTitle = pageSettings.runningHead
        }
        titleButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                runHeadText.isEnabled = true
                pageSettings.defaultRunningHeadOption = PageSettings.RunningHeadOptions.TEXT
            }
        })
        runHeadText.layoutData = GridData(SWT.FILL, SWT.FILL, true, false, 1, 1)
        runHeadText.addListener(SWT.Modify) { documentTitle = runHeadText.text }
        buttonOK.text = "OK"
        rhShell.defaultButton = buttonOK
        buttonOK.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
//            	stopFormatting();
                //Update the pageSettings and reformat
                pageSettings.runningHead = documentTitle
                DocumentUTDConfig.NIMAS.savePageSettings(manager.doc, pageSettings)
                manager.simpleManager.dispatchEvent(
                    ModifyEvent(
                        Sender.NO_SENDER, true, BBX.getRoot(
                            manager.doc
                        )
                    )
                )
                rhShell.close()
            }
        })
        rhShell.open()
        rhShell.pack()
    }

}
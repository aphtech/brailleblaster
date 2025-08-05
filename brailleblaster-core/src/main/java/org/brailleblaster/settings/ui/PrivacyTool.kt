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
package org.brailleblaster.settings.ui

import org.brailleblaster.utils.localization.LocaleHandler
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.tools.MenuToolModule
import org.brailleblaster.usage.BBUsageManager
import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.swt.SWT
import org.eclipse.swt.events.TraverseEvent
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell

object PrivacyTool : MenuToolModule {
    override val topMenu: TopMenu = TopMenu.HELP
    override val title: String = LocaleHandler.getDefault()["PrivacySettingsTab.title"]
    override fun onRun(bbData: BBSelectionData) {
        val usage = bbData.manager.wpManager.usageManager
        if (usage is BBUsageManager) {
            usage.reportDataAsync()
        }
        val shell = Shell(
            Display.getDefault(),
            SWT.APPLICATION_MODAL or SWT.RESIZE or SWT.CLOSE or SWT.TITLE or SWT.MIN
        ).apply {
            text = LocaleHandler.getDefault()["PrivacySettingsTab.title"]
            layout = GridLayout(1, true)
        }
        val privacyTab = PrivacySettingsTab(shell)
        // Button panel at the bottom
        val buttonPanel = Composite(shell, SWT.NONE)
        EasySWT.setGridData(buttonPanel)
        buttonPanel.layout = RowLayout(SWT.HORIZONTAL)
        val okButton = Button(buttonPanel, SWT.PUSH)
        okButton.text = LocaleHandler.getDefault()["buttonOk"]
        val cancelButton = Button(buttonPanel, SWT.PUSH)
        cancelButton.text = "Cancel"
        // --------------- Listeners -------------
        shell.addTraverseListener { e: TraverseEvent -> if (e.keyCode == SWT.ESC.code) shell.close() }
        okButton.addSelectionListener(
            EasySWT.makeSelectedListener { saveConfig(bbData.manager, privacyTab, shell) })
        cancelButton.addSelectionListener(EasySWT.makeSelectedListener { shell.close() })
        // Autosize shell based on what the internal elements require
        EasySWT.setLargeDialogSize(shell)
        // Show the window
        shell.open()
    }

    private fun saveConfig(m: Manager, settingsTab: SettingsUITab, shell: Shell) {
        settingsTab.updateEngine(m.document.engine)
        shell.close()
    }
}
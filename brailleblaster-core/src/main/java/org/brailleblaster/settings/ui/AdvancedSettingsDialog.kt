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

import ch.qos.logback.classic.Level
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.logging.getLogLevel
import org.brailleblaster.logging.setLogLevel
import org.brailleblaster.logging.updateLogSettings
import org.brailleblaster.utils.swt.EasySWT
import org.brailleblaster.wordprocessor.WPManager
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Group
import org.eclipse.swt.widgets.Shell

/**
 * Allows user to configure logging
 */
class AdvancedSettingsDialog {
    private val dialog: Shell = Shell(WPManager.getInstance().shell, SWT.DIALOG_TRIM or SWT.APPLICATION_MODAL)
    private val logDebug: Button
    private val logInfo: Button
    private val logWarn: Button
    private val logError: Button

    init {
        //Make the window
        dialog.layout = GridLayout(1, true)
        dialog.text = localeHandler["settingsTitle"]

        //Log level
        val logLevelGroup = Group(dialog, SWT.SHADOW_IN)
        logLevelGroup.text = "Log Level"
        logLevelGroup.layout = RowLayout(SWT.HORIZONTAL)
        val logAll = Button(logLevelGroup, SWT.RADIO)
        logAll.text = "All"
        logDebug = Button(logLevelGroup, SWT.RADIO)
        logDebug.text = "Debug"
        logInfo = Button(logLevelGroup, SWT.RADIO)
        logInfo.text = "Info"
        logWarn = Button(logLevelGroup, SWT.RADIO)
        logWarn.text = "Warn"
        logError = Button(logLevelGroup, SWT.RADIO)
        logError.text = "Error (Recommended)"

        //Save/close button
        val submitGroup = Composite(dialog, 0)
        val submitGroupLayout = RowLayout()
        submitGroupLayout.center = true
        submitGroup.layout = submitGroupLayout
        val saveButton = Button(submitGroup, SWT.PUSH)
        saveButton.text = localeHandler["&Save"]
        val cancelButton = Button(submitGroup, SWT.PUSH)
        cancelButton.text = localeHandler["&Cancel"]

        //------ Set listenrs ------
        saveButton.addSelectionListener(EasySWT.makeSelectedListener { it: SelectionEvent -> onSave() })
        cancelButton.addSelectionListener(EasySWT.makeSelectedListener { it: SelectionEvent -> dialog.close() })

        //------ Set default values -----
        when (getLogLevel()) {
            Level.ERROR -> logError.selection = true
            Level.WARN -> logWarn.selection =
                true

            Level.INFO -> logInfo.selection =
                true

            Level.DEBUG -> logDebug.selection = true
            else -> logAll.selection = true
        }

        //Shrink dialog to minimum size and display
        EasySWT.setLargeDialogSize(dialog)
        dialog.open()
    }

    private fun onSave() {
        if (logError.selection) setLogLevel(Level.ERROR) else if (logWarn.selection) setLogLevel(Level.WARN) else if (logInfo.selection) setLogLevel(
            Level.INFO
        ) else if (logDebug.selection) setLogLevel(Level.DEBUG) else setLogLevel(Level.ALL)
        updateLogSettings()
        dialog.close()
    }

    companion object {
        private val localeHandler = getDefault()
    }
}

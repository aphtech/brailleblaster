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

import org.brailleblaster.BBIni
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.settings.UTDManager
import org.brailleblaster.settings.UTDManager.Companion.userPageSettingsFile
import org.brailleblaster.utd.UTDTranslationEngine
import org.brailleblaster.utd.config.DocumentUTDConfig
import org.brailleblaster.utd.config.UTDConfig
import org.brailleblaster.util.FormUIUtils
import org.brailleblaster.util.Notify
import org.brailleblaster.util.Notify.notify
import org.brailleblaster.util.Notify.showMessage
import org.brailleblaster.util.WorkingDialog
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.utils.swt.EasySWT
import org.brailleblaster.utils.swt.EasySWT.addSwtBotKey
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.TraverseEvent
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.*

class BrailleSettingsDialog(parent: Shell?, m: Manager?, tabToOpen: Class<out SettingsUITab?>) {
    private val engine: UTDTranslationEngine?
    private val m: Manager?
    val shell: Shell?
    private val pageProperties: SettingsUITab?
    private val pageNumTab: SettingsUITab?
    private var embosserTab: SettingsUITab? = null
    private val translationTab: TranslationSettingsTab?
    private val formatTab: FormatSettingsTab?
    private val statusBarSettingsTab: SettingsUITab?

    constructor(m: Manager?, tabToOpen: Class<out SettingsUITab?>) : this(null, m, tabToOpen)

    init {
        if (m == null) {
            showMessage("Must open document first")
            this.engine = null
            this.shell = null
            this.m = null
            translationTab = null
            pageNumTab = null
            pageProperties = null
            embosserTab = null
            formatTab = null
            statusBarSettingsTab = null
        } else {
            m.waitForFormatting(true)
            this.engine = m.document.engine
            this.m = m
            shell = if (parent != null) {
                EasySWT.makeDialog(parent)
            } else {
                FormUIUtils.makeDialog(m)
            }
            shell.text = localeHandler["settings"]
            shell.layout = GridLayout(1, true)

            val folder = TabFolder(shell, SWT.NONE)
            EasySWT.setGridData(folder)
            (folder.layoutData as GridData).grabExcessVerticalSpace = true

            pageProperties = PagePropertiesTab.create(folder, engine, this.shell)
            translationTab = TranslationSettingsTab(folder, m.document)
            pageNumTab = PageNumbersTab(folder, engine.pageSettings)
            formatTab = FormatSettingsTab(folder, m.document)
            embosserTab = EmbosserSettingsTab(folder)
            statusBarSettingsTab = StatusBarSettingsTab(folder)

            //Button panel at the bottom
            val buttonPanel = Composite(shell, SWT.NONE)
            EasySWT.setGridData(buttonPanel)
            buttonPanel.layout = RowLayout(SWT.HORIZONTAL)

            val okButton = Button(buttonPanel, SWT.PUSH)
            okButton.text = localeHandler[localeHandler["buttonOk"]]
            addSwtBotKey(okButton, SWTBOT_OK_BUTTON)

            val okDefaultButton = Button(buttonPanel, SWT.PUSH)
            okDefaultButton.text = "Make Default"
            addSwtBotKey(okDefaultButton, SWTBOT_OK_DEFAULT_BUTTON)

            val cancelButton = Button(buttonPanel, SWT.PUSH)
            cancelButton.text = "Cancel"

            //--------------- Listeners -------------
            shell.addTraverseListener { e: TraverseEvent ->
                if (e.keyCode == SWT.ESC.code) shell.close()
            }
            shell.addListener(SWT.Close) { close() }

            okButton.addSelectionListener(EasySWT.makeSelectedListener { it: SelectionEvent -> saveConfig(false) })
            okDefaultButton.addSelectionListener(EasySWT.makeSelectedListener { it: SelectionEvent -> saveConfig(true) })
            cancelButton.addSelectionListener(EasySWT.makeSelectedListener { it: SelectionEvent -> close() })

            //--------------- Data ---------------
            when (tabToOpen) {
                PagePropertiesTab::class.java -> folder.setSelection(0)
                TranslationSettingsTab::class.java -> folder.setSelection(1)
                PageNumbersTab::class.java -> folder.setSelection(2)
                FormatSettingsTab::class.java -> folder.setSelection(3)
                EmbosserSettingsTab::class.java -> folder.setSelection(4)
                StatusBarSettingsTab::class.java -> folder.setSelection(5)
            }

            //Autosize shell based on what the internal elements require
            EasySWT.setLargeDialogSize(shell)

            //Show the window
            shell.open()
        }
    }

    private fun saveConfig(saveAsDefault: Boolean) {
        var errorStr: String?
        //This will validate each tab, not using && due to short-circut evaluation
        if ((pageProperties!!.validate().also { errorStr = it }) != null) notify(
            localeHandler[errorStr!!], Notify.EXCEPTION_SHELL_NAME
        )
        else if ((translationTab!!.validate().also { errorStr = it }) != null) notify(
            localeHandler[errorStr!!], Notify.EXCEPTION_SHELL_NAME
        )
        else if ((pageNumTab!!.validate().also { errorStr = it }) != null) notify(
            localeHandler[errorStr!!], Notify.EXCEPTION_SHELL_NAME
        )
        else {
            //Only save if setting was changed
            var updated = false
            if (pageProperties.updateEngine(engine!!)) {
                DocumentUTDConfig.NIMAS.savePageSettings(m!!.doc, engine.pageSettings)
                if (saveAsDefault) UTDConfig.savePageSettings(userPageSettingsFile, engine.pageSettings)
                updated = true
            }
            if (translationTab.updateEngine(engine)) {
                m!!.document.settingsManager.updateBrailleStandard(m.doc, translationTab.selectedName)
                if (saveAsDefault) BBIni.propertyFileManager.save(
                    UTDManager.USER_SETTINGS_BRAILLE_STANDARD,
                    translationTab.selectedName
                )
                updated = true
            }
            if (formatTab!!.updateEngine(engine)) {
                m!!.document.settingsManager.updatePredominantQuote(m.doc, formatTab.predominantQuote)
                if (saveAsDefault) {
                    BBIni.propertyFileManager
                        .save(UTDManager.USER_SETTINGS_PREDOMINANT_QUOTE, formatTab.predominantQuote.name)
                }
                updated = true
            }
            var tryAgain = false
            try {
                if (pageNumTab.updateEngine(engine)) {
                    DocumentUTDConfig.NIMAS.savePageSettings(m!!.doc, engine.pageSettings)
                    if (saveAsDefault) UTDConfig.savePageSettings(userPageSettingsFile, engine.pageSettings)
                    updated = true
                }
            } catch (_: IllegalArgumentException) {
                val messageBox = MessageBox(shell, SWT.ICON_WARNING or SWT.OK)
                messageBox.text = "Warning"
                messageBox.message = "Braille and Print page numbers cannot be on the same location"
                messageBox.open()
                tryAgain = true
            }


            // note: doesn't actually change braille settings
            if (embosserTab!!.updateEngine(engine)) {
                updated = true
            }
            if (updated) {
                WorkingDialog("Refreshing view due to settings changes").use {
                    m!!.refresh()
                }
            }

            if (!tryAgain) {
                close()
            }
        }
    }

    fun close() {
        shell!!.dispose()
    }

    companion object {
        private val localeHandler = getDefault()
        const val SWTBOT_OK_BUTTON: String = "translationSettingsTab.ok"
        const val SWTBOT_OK_DEFAULT_BUTTON: String = "translationSettingsTab.okDefault"
    }
}

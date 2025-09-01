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

import org.brailleblaster.embossers.EmbosserConfig
import org.brailleblaster.embossers.EmbosserConfigList
import org.brailleblaster.embossers.EmbosserEditDialog
import org.brailleblaster.embossers.EmbossingUtils.embossersFile
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.utd.UTDTranslationEngine
import org.brailleblaster.util.FormUIUtils
import org.eclipse.jface.window.Window
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.*
import java.io.IOException

class EmbosserSettingsTab(folder: TabFolder?) : SettingsUITab {
    private val embosserList: EmbosserConfigList
    private val defaultEmbosserCombo: Combo
    private val btnRemoveEmbosser: Button
    private val btnEditEmbosser: Button

    init {
        val tab = TabItem(folder, 0)
        tab.text = localeHandler["EmbosserSettingsTab.title"]
        val parent = Composite(folder, 0)
        parent.layout = GridLayout(1, true)
        tab.control = parent

        val defaultEmbosserGroup = Group(parent, SWT.NONE)
        defaultEmbosserGroup.text = localeHandler["EmbosserSettingsTab.defaultEmbosserSettings"]
        defaultEmbosserGroup.text = localeHandler["EmbosserSettingsTab.defaultEmbosserSettings"]
        defaultEmbosserGroup.layout = GridLayout(2, true)
        FormUIUtils.addLabel(defaultEmbosserGroup, localeHandler["EmbosserSettingsTab.defaultEmbosser"])
        var embosserComboBuilder = FormUIUtils.makeComboDropdown(defaultEmbosserGroup)
            .add(localeHandler["EmbosserSettingsTab.lastUsedEmbosser"])
        embosserList = EmbosserConfigList.loadEmbossers(embossersFile)
        val defaultSelection =
            if (embosserList.isUseLastEmbosser) 0
            else embosserList.indexOf(embosserList.defaultEmbosser) + 1

        for (e in embosserList) {
            embosserComboBuilder = embosserComboBuilder.add(e.name)
        }
        defaultEmbosserCombo = embosserComboBuilder.select(defaultSelection).get()
        val embosserButtonsContainer = Composite(defaultEmbosserGroup, SWT.NONE)
        val gd = GridData(SWT.RIGHT, SWT.CENTER, true, true, 2, 1)
        embosserButtonsContainer.layoutData = gd
        embosserButtonsContainer.layout = RowLayout()
        FormUIUtils.makeButton(embosserButtonsContainer).text(localeHandler["EmbosserSettingsTab.addEmbosser"])
            .onSelection { addEmbosser() }
            .get()
        btnEditEmbosser =
            FormUIUtils.makeButton(embosserButtonsContainer).text(localeHandler["EmbosserSettingsTab.editEmbosser"])
                .onSelection { editEmbosser() }
                .get()
        btnRemoveEmbosser =
            FormUIUtils.makeButton(embosserButtonsContainer).text(localeHandler["EmbosserSettingsTab.removeEmbosser"])
                .onSelection { removeEmbosser() }
                .get()
        FormUIUtils.addSelectionListener(defaultEmbosserCombo) { defaultEmbosserSelected() }
        // make sure add/edit/remove buttons match current selection.
        defaultEmbosserSelected()
    }

    private fun removeEmbosser() {
        val selIndex = defaultEmbosserCombo.selectionIndex
        embosserList.removeAt(selIndex - 1)
        defaultEmbosserCombo.remove(selIndex)
        // We can always be certain of there being selIndex-1 in the combo due to "last used" being at the start of the combo
        defaultEmbosserCombo.select(selIndex - 1)
        defaultEmbosserCombo.setFocus()
        defaultEmbosserSelected()
    }

    private fun editEmbosser() {
        val selIndex = defaultEmbosserCombo.selectionIndex
        var embosser = embosserList[selIndex - 1]
        val existingNames = embosserList.filter { it != embosser }.map { it.name }.toSet()
        val d = EmbosserEditDialog(defaultEmbosserCombo.shell, existingNames)
        d.embosser = embosser
        val result = d.open()
        if (result == Window.OK) {
            embosser = d.embosser
            embosserList[selIndex - 1] = embosser
            defaultEmbosserCombo.setItem(selIndex, embosser.name)
        }
    }

    private fun defaultEmbosserSelected() {
        if (defaultEmbosserCombo.selectionIndex == 0) {
            // Option for last used embosser
            // Therefore cannot edit or remove this option
            btnEditEmbosser.isEnabled = false
            btnRemoveEmbosser.isEnabled = false
        } else {
            // An embosser is selected.
            // Only allow editing of active embosser profiles.
            val active = embosserList
                .any { p: EmbosserConfig -> p.name == defaultEmbosserCombo.text && p.isActive }
            btnEditEmbosser.isEnabled = active
            btnRemoveEmbosser.isEnabled = true
        }
    }

    private fun addEmbosser() {
        val existingNames = embosserList.map { it.name }.toSet()
        val d = EmbosserEditDialog(defaultEmbosserCombo.shell, existingNames)
        val result = d.open()
        if (result == Window.OK) {
            val embosser = d.embosser
            embosserList.add(embosser)
            defaultEmbosserCombo.add(embosser.name)
            defaultEmbosserCombo.select(defaultEmbosserCombo.itemCount - 1)
            defaultEmbosserCombo.setFocus()
            defaultEmbosserSelected()
        }
    }

    override fun validate(): String? {
        // Nothing to validate
        return null
    }

    override fun updateEngine(engine: UTDTranslationEngine): Boolean {
        val embosserSelection = defaultEmbosserCombo.selectionIndex
        // First index is "Last used embosser"
        // Therefore the actual connected embossers is 1 greater than in the printerList.
        if (embosserSelection == 0) {
            embosserList.isUseLastEmbosser = true
        } else {
            embosserList.isUseLastEmbosser = false
            val embosserIndex = embosserSelection - 1
            embosserList.defaultEmbosser = embosserList[embosserIndex]
        }
        try {
            embosserList.saveEmbossers(embossersFile)
        } catch (_: IOException) {
            // May be we should warn the user?
        }
        // Do not refresh document.
        return false
    }

    companion object {
        private val localeHandler = getDefault()
    }
}

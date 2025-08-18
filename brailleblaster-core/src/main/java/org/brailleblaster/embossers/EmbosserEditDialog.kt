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
package org.brailleblaster.embossers

import org.brailleblaster.libembosser.EmbosserService
import org.brailleblaster.libembosser.spi.Embosser
import org.brailleblaster.utils.localization.LocaleHandler
import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.jface.dialogs.IDialogConstants
import org.eclipse.jface.dialogs.IMessageProvider
import org.eclipse.jface.dialogs.TitleAreaDialog
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import java.util.*
import java.util.stream.Collectors
import javax.print.DocFlavor
import javax.print.PrintService
import javax.print.PrintServiceLookup

class EmbosserEditDialog(shell: Shell?, private val existingNames: Set<String>) : TitleAreaDialog(shell) {
    private lateinit var txtName: Text
    private lateinit var cbEmbosserDevice: Combo
    private lateinit var cbEmbosserManufacturer: Combo
    private val printerList: Array<PrintService>
    var embosser: EmbosserConfig
    private var embosserMap: Map<String, List<Embosser?>>? = null
    private var embosserDriver: Embosser? = null
    private lateinit var cbEmbosserModel: Combo
    private lateinit var embosserOptionsView: EmbosserOptionsView
    override fun configureShell(newShell: Shell) {
        super.configureShell(newShell)
        newShell.text = LocaleHandler.getDefault()["EmbosserEditDialog.shellText"]
    }

    override fun create() {
        super.create()
        setTitle(LocaleHandler.getDefault()["EmbosserEditDialog.title"])
        setMessage(LocaleHandler.getDefault()["EmbosserEditDialog.msg"], IMessageProvider.INFORMATION)
        validate()
    }

    override fun createDialogArea(parent: Composite): Control {
        val area = super.createDialogArea(parent) as Composite
        area.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        val folder = TabFolder(area, SWT.BORDER)
        val generalTab = TabItem(folder, SWT.NONE)
        generalTab.text = LocaleHandler.getDefault()["EmbosserEditDialog.generalTabTitle"]
        generalTab.control = createGeneralSettingsComposite(folder)
        val advancedTab = TabItem(folder, SWT.NONE)
        advancedTab.text = LocaleHandler.getDefault()["EmbosserEditDialog.advancedTabTitle"]
        embosserOptionsView = EmbosserOptionsView(folder, SWT.BORDER or SWT.H_SCROLL or SWT.V_SCROLL, emptyMap())
        embosserOptionsView.addValidateListener { validate() }
        advancedTab.control = embosserOptionsView.control
        updateFromEmbosser()
        return area
    }

    private fun createGeneralSettingsComposite(parent: Composite): Control {
        val container = Composite(parent, SWT.NONE)
        container.layout = GridLayout(2, false)
        EasySWT.makeLabel(container).text(LocaleHandler.getDefault()["EmbosserEditDialog.name"])
        val txtName = EasySWT.makeTextBuilder(container).get()
        this.txtName = txtName
        EasySWT.makeLabel(container).text(LocaleHandler.getDefault()["EmbosserEditDialog.device"]).get()
        var cb = EasySWT.makeComboDropdown(container)
                .add(LocaleHandler.getDefault()["EmbosserEditDialog.selectPrinter"])
        for (pd in printerList) {
            cb = cb.add(pd.name)
        }
        cbEmbosserDevice = cb.get()
        EasySWT.makeLabel(container)
                .text(LocaleHandler.getDefault()["EmbosserEditDialog.embosserManufacturer"])
                .get()
        cb = EasySWT.makeComboDropdown(container)
                .add(LocaleHandler.getDefault()["EmbosserEditDialog.selectEmbosserManufacturer"])
        val embosserMap = EmbosserService.getInstance()
                .embosserStream
                .collect(Collectors.groupingBy { obj: Embosser -> obj.manufacturer })
        this.embosserMap = embosserMap
        for (m in embosserMap.keys) {
            cb.add(m)
        }
        cbEmbosserManufacturer = cb.get()
        EasySWT.makeLabel(container).text(LocaleHandler.getDefault()["EmbosserEditDialog.embosserModel"])
        cbEmbosserModel = EasySWT.makeComboDropdown(container).get()
        txtName.addModifyListener { validate() }
        EasySWT.addSelectionListener(cbEmbosserDevice) { validate() }
        EasySWT.addSelectionListener(
                cbEmbosserManufacturer
        ) {
            embosserManufacturerChanged()
            validate()
        }
        EasySWT.addSelectionListener(
                cbEmbosserModel
        ) {
            updateEmbosserDriver()
            // May be we want to automatically change it to the nearest valid value, but this may also
            // annoy some users.
            validate()
        }
        return container
    }

    private fun selectEmbossermanufacturer(index: Int) {
        cbEmbosserManufacturer.select(index)
        embosserManufacturerChanged()
    }

    private fun updateEmbosserDriver() {
        var selIndex = cbEmbosserManufacturer.selectionIndex
        if (selIndex < 1) {
            embosser.embosserDriver = null
        } else {
            val manufacturer = cbEmbosserManufacturer.getItem(selIndex)
            selIndex = cbEmbosserModel.selectionIndex
            val model = cbEmbosserModel.getItem(selIndex)
            embosserDriver = embosserMap!![manufacturer]!!.firstOrNull { e: Embosser? -> e!!.model == model }
            if (embosserDriver != null) {
                embosserOptionsView.embosserOptions = embosserDriver!!.options
            }
        }
    }

    private fun updateFromEmbosser() {
        txtName.text = embosser.name
        var defaultIndex = 0
        for (i in printerList.indices) {
            val ps = printerList[i]
            if (ps.name == embosser.printerName) {
                defaultIndex = i + 1
                break
            }
        }
        cbEmbosserDevice.select(defaultIndex)
        defaultIndex = 0
        val ed = embosser.embosserDriver
        if (ed != null) {
            for (i in 1 until cbEmbosserManufacturer.itemCount) {
                if (cbEmbosserManufacturer.getItem(i) == ed.manufacturer) {
                    defaultIndex = i
                    break
                }
            }
        }
        selectEmbossermanufacturer(defaultIndex)
        if (ed != null) {
            cbEmbosserModel.select(cbEmbosserModel.indexOf(ed.model))
            embosserOptionsView.embosserOptions = ed.options
        }
        embosserDriver = ed
    }

    private fun embosserManufacturerChanged() {
        cbEmbosserModel.removeAll()
        val selIndex = cbEmbosserManufacturer.selectionIndex
        if (selIndex < 1) {
            cbEmbosserModel.isEnabled = false
        } else {
            cbEmbosserModel.isEnabled = true
            val el = embosserMap!![cbEmbosserManufacturer.getItem(selIndex)]!!
            for (e in el) {
                cbEmbosserModel.add(e!!.model)
            }
            cbEmbosserModel.select(0)
        }
        // Update the embosser driver
        updateEmbosserDriver()
    }

    private fun validate(): Boolean {
        var errorMsg: String? = null
        // Check the fields in opposite order to which they appear in the dialog.
        // This means that if there are multiple errors then the earlier field error message will be
        // shown first, thus leading the user through the errors in tab order.
        val invalidOption = embosserOptionsView.optionErrors.lastOrNull()
        if (invalidOption != null) {
            errorMsg = LocaleHandler.getDefault().format("EmbosserEditDialog.optionsView.errorInvalidOptionValue", invalidOption.getDisplayName(
                Locale.getDefault()))
        }
        if (cbEmbosserManufacturer.selectionIndex < 1) {
            errorMsg = LocaleHandler.getDefault()["EmbosserEditDialog.errSelectEmbosserManufacturer"]
        }
        if (cbEmbosserDevice.selectionIndex < 1) {
            errorMsg = LocaleHandler.getDefault()["EmbosserEditDialog.errSelectDevice"]
        }
        if (existingNames.contains(txtName.text)) {
            errorMsg = LocaleHandler.getDefault()["EmbosserEditDialog.errExistingName"]
        } else if ("" == txtName.text) {
            errorMsg = LocaleHandler.getDefault()["EmbosserEditDialog.errBlankName"]
        }
        errorMessage = errorMsg
        val button = getButton(IDialogConstants.OK_ID)
        if (button != null) {
            button.isEnabled = errorMsg == null
        }
        return errorMsg == null
    }

    override fun okPressed() {
        saveInput()
        super.okPressed()
    }

    private fun saveInput() {
        val printerName = cbEmbosserDevice.text
        // int selIndex = cbEmbosserDevice.getSelectionIndex();
        // Whilst it should not occur, if no printer is selected then we should set the driver and name
        // to null
        // PrintService ps = selIndex > 0? printerList[selIndex-1] : null;
        embosser = EmbosserConfig(txtName.text, printerName)
        var driver = embosserDriver
        if (driver != null) {
            driver = driver.customize(embosserOptionsView.embosserOptions)
        }
        embosser.embosserDriver = driver
    }

    init {
        embosser = EmbosserConfig()
        printerList = PrintServiceLookup.lookupPrintServices(DocFlavor.INPUT_STREAM.AUTOSENSE, null)
    }
}
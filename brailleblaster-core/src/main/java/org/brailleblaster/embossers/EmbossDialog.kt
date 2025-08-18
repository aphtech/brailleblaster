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

import org.brailleblaster.utils.localization.LocaleHandler
import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.jface.dialogs.Dialog
import org.eclipse.jface.dialogs.IDialogConstants
import org.eclipse.jface.dialogs.MessageDialog
import org.eclipse.swt.SWT
import org.eclipse.swt.accessibility.ACC
import org.eclipse.swt.events.*
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.printing.PrinterData
import org.eclipse.swt.widgets.*
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.function.Consumer

class EmbossDialog @JvmOverloads constructor(parentShell: Shell? = Display.getCurrent().activeShell, private val openSettings: Consumer<Shell>? = null) : Dialog(parentShell) {
    private var embosserList: EmbosserConfigList? = null
    var embosser: EmbosserConfig? = null
    private var cbPrinter: Combo? = null
    var copies = 1
    var startPage = 1
    var endPage = 1
    var scope = PrinterData.ALL_PAGES
        private set
    var isCreateDebugFile = false
    private fun loadEmbosserList() {
        embosserList = EmbosserConfigList.loadEmbossers(
                EmbossingUtils.embossersFile)
        if (!embosserList!!.isEmpty()) {
            embosser = embosserList!!.preferredEmbosser
        }
    }

    override fun createDialogArea(parent: Composite): Control {
        val area = super.createDialogArea(parent) as Composite
        area.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        val container = Composite(area, SWT.NONE)
        container.layout = GridLayout(2, false)
        EasySWT.addLabel(container, LocaleHandler.getDefault()["EmbossDialog.embosser"])
        val cbPrinter = EasySWT.makeComboDropdown(container).get()
        this.cbPrinter = cbPrinter
        updateEmbosserCombo()
        EasySWT.addSelectionListener(cbPrinter) {
            val selEmbosser = cbPrinter.text
            embosserList!!.firstOrNull { p: EmbosserConfig -> p.name == selEmbosser }
                ?.let { emb: EmbosserConfig? -> embosser = emb }
        }

        // Blank label for blank grid cell
        EasySWT.addLabel(container, "")
        // When emboss dialog becomes active, reload the embosser list to ensure it is up to date (eg.
        // after embosser settings was opened).
        val activationListener: ShellAdapter = object : ShellAdapter() {
            override fun shellActivated(e: ShellEvent) {
                cbPrinter.removeAll()
                loadEmbosserList()
                updateEmbosserCombo()
                // remove this listener so that it only fires the once.
                shell.removeShellListener(this)
            }
        }
        val btnManageEmbossers = Button(container, SWT.PUSH)
        btnManageEmbossers.text = LocaleHandler.getDefault()["EmbossDialog.manageEmbossers"]
        if (openSettings == null) {
            btnManageEmbossers.isEnabled = false
        } else {
            btnManageEmbossers.addSelectionListener(
                    object : SelectionAdapter() {
                        override fun widgetSelected(e: SelectionEvent) {
                            shell.addShellListener(activationListener)
                            openSettings.accept(shell)
                        }
                    })
        }
        val chkEmbossFile = EasySWT.makeCheckbox(container)
                .gridDataHorizontalSpan(2)
                .text(LocaleHandler.getDefault()["EmbossDialog.createDebugFile"])
                .selected(false)
                .get()
        EasySWT.addSelectionListener(
                chkEmbossFile) { isCreateDebugFile = chkEmbossFile.selection }
        val grpPageRange = Group(container, SWT.NONE)
        grpPageRange.text = LocaleHandler.getDefault()["EmbossDialog.pageRange"]
        grpPageRange.layoutData = GridData(SWT.FILL, SWT.FILL, true, false, 2, 1)
        grpPageRange.layout = GridLayout(2, false)
        val rbAllPages = Button(grpPageRange, SWT.RADIO)
        rbAllPages.text = LocaleHandler.getDefault()["EmbossDialog.allPages"]
        // All pages has no options, need blank grid cell
        EasySWT.makeLabel(grpPageRange)
        val rbPageRange = Button(grpPageRange, SWT.RADIO)
        rbPageRange.text = LocaleHandler.getDefault()["EmbossDialog.pages"]
        val cmpPages = Composite(grpPageRange, SWT.NONE)
        cmpPages.layout = RowLayout()
        EasySWT.makeLabel(cmpPages).text(LocaleHandler.getDefault()["EmbossDialog.pagesFrom"])
        val txtPagesFrom = EasySWT.makeTextBuilder(cmpPages)
                .text(startPage.toString())
                .onModify { e: ModifyEvent -> startPage = (e.widget as Text).text.toInt() }
                .rowDataWidth(convertWidthInCharsToPixels(5))
                .get()
        EasySWT.addIntegerFilter(txtPagesFrom)
        EasySWT.makeLabel(cmpPages).text(LocaleHandler.getDefault()["EmbossDialog.pagesTo"])
        val txtPagesTo = EasySWT.makeTextBuilder(cmpPages)
                .text(endPage.toString())
                .onModify { e: ModifyEvent -> endPage = (e.widget as Text).text.toInt() }
                .rowDataWidth(convertWidthInCharsToPixels(5))
                .get()
        EasySWT.addIntegerFilter(txtPagesTo)
        EasySWT.addSelectionListener(
                rbAllPages
        ) {
            scope = PrinterData.ALL_PAGES
            txtPagesFrom.editable = false
            txtPagesTo.editable = false
        }
        EasySWT.addSelectionListener(
                rbPageRange
        ) {
            scope = PrinterData.PAGE_RANGE
            txtPagesFrom.editable = true
            txtPagesTo.editable = true
        }
        rbAllPages.selection = true
        val grpCopies = Group(container, SWT.NONE)
        grpCopies.text = LocaleHandler.getDefault()["EmbossDialog.copies"]
        grpCopies.layoutData = GridData(SWT.FILL, SWT.FILL, true, false, 2, 1)
        grpCopies.layout = GridLayout(2, false)
        val lblCopies = EasySWT.addLabel(grpCopies, LocaleHandler.getDefault()["EmbossDialog.numOfCopies"])
        val spCopies = Spinner(grpCopies, SWT.NONE)
        spCopies.minimum = 1
        spCopies.maximum = 99
        EasySWT.addSelectionListener(spCopies) { e: SelectionEvent -> copies = (e.widget as Spinner).selection }
        spCopies.selection = copies
        // NOTE: The below relation does not fully work because of a SWT bug.
        // Keep it so that once the bug is fixed (when/if) this will work.
        val accLabel = lblCopies.accessible
        val accCtrl = spCopies.accessible
        accLabel.addRelation(ACC.RELATION_LABEL_FOR, accCtrl)
        accCtrl.addRelation(ACC.RELATION_LABELLED_BY, accLabel)
        return area
    }

    private fun updateEmbosserCombo() {
        embosserList!!.filter(EmbosserConfig::isActive).forEach { e: EmbosserConfig -> cbPrinter!!.add(e.name) }
        var defaultIndex = 0
        if (embosser != null) {
            defaultIndex = cbPrinter!!.indexOf(embosser!!.name)
            defaultIndex = defaultIndex.coerceAtLeast(0)
        }
        cbPrinter!!.select(defaultIndex)
    }

    override fun configureShell(newShell: Shell) {
        super.configureShell(newShell)
        newShell.text = LocaleHandler.getDefault()["EmbossDialog.title"]
    }

    override fun create() {
        super.create()
        if (embosserList!!.isEmpty()) {
            getButton(IDialogConstants.OK_ID).isEnabled = false
        }
    }

    override fun okPressed() {
        embosserList!!.lastUsedEmbosser = embosser!!
        try {
            embosserList!!.saveEmbossers(EmbossingUtils.embossersFile)
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
        super.okPressed()
    }

    fun ensureEmbossersAvailable(parent: Shell?): Boolean {
        val embosserList = embosserList
        if (embosserList!!.isEmpty()) {
            // Ask the user if they want to configure an embosser.
            val result =
                MessageDialog.openQuestion(
                    parent,
                    LocaleHandler.getDefault()["EmbossersManager.noEmbosserTitle"],
                    LocaleHandler.getDefault()["EmbossersManager.noEmbosserMsg"]
                )
            if (!result) {
                return false
            }
            val d = EmbosserEditDialog(parent, emptySet())
            val res = d.open()
            if (res != OK) {
                return false
            }
            val de = d.embosser
            embosserList.add(de)
            // 2020-05-21: Must set the selected embosser.
            embosser = de
            try {
                embosserList.saveEmbossers()
            } catch (_: IOException) {
                // Not much we really can do, log it for now.
                logger.error("Unable to save embossers file")
            }
        }
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EmbossDialog::class.java)
    }

    init {
        loadEmbosserList()
    }
}
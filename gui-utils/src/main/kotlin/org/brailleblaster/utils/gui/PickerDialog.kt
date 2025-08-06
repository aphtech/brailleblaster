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
package org.brailleblaster.utils.gui

import org.apache.commons.lang3.mutable.MutableBoolean
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.events.KeyListener
import org.eclipse.swt.events.ModifyListener
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Label
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Table
import org.eclipse.swt.widgets.TableColumn
import org.eclipse.swt.widgets.TableItem
import org.eclipse.swt.widgets.Text
import java.util.Locale
import java.util.function.Consumer

/**
 * Dialog for easy to use selection from a list of options
 */
class PickerDialog {
    var headings: Array<String> = emptyArray()
    var contents: List<Array<String>>? = null
    var message: String? = null

    val cancelled: MutableBoolean = MutableBoolean(true)

    fun open(parent: Shell, callback: Consumer<Int>): Shell {
        return createContents(parent, callback)
    }

    private fun createContents(parent: Shell, callback: Consumer<Int>): Shell {
        require(headings.isNotEmpty()) { "Headings is empty" }
        requireNotNull(contents) { "Contents is null" }

        val shell = Shell(parent, SWT.APPLICATION_MODAL or SWT.DIALOG_TRIM)
        shell.text = SHELL_TITLE
        shell.layout = GridLayout(1, false)
        val container = Composite(shell, SWT.NONE)
        container.layout = GridLayout(1, false)
        container.layout = GridLayout(2, true)
        val desc = Label(container, SWT.NONE)
        applyGridData(2, desc)
        desc.text = if (message == null) DEFAULT_MESSAGE else message
        val selection = Text(container, SWT.SINGLE or SWT.BORDER)
        applyGridData(TABLE_WIDTH, 2, selection)
        val table = Table(container, SWT.VIRTUAL or SWT.BORDER or SWT.FULL_SELECTION)
        table.linesVisible = true
        table.headerVisible = true
        val columns = headings.size
        val colWidth = TABLE_WIDTH / columns
        for (heading in headings) {
            val newColumn = TableColumn(table, SWT.NONE)
            newColumn.width = colWidth
            newColumn.text = heading
        }
        for (row in contents!!) {
            val newItem = TableItem(table, SWT.NONE)
            newItem.setText(row)
        }
        val newGridData = GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1)
        newGridData.heightHint = TABLE_HEIGHT
        newGridData.widthHint = TABLE_WIDTH
        table.layoutData = newGridData
        table.addSelectionListener(makeTableListener(selection, table))
        table.addKeyListener(makeEnterListener(table, shell, callback))
        selection.addModifyListener(makeModifyListener(selection, table))
        selection.addKeyListener(makeEnterListener(table, shell, callback))
        val okButton = Button(container, SWT.PUSH)
        okButton.text = "Ok"
        applyGridData(80, 1, okButton)
        okButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                if (table.selectionCount > 0) {
                    callback.accept(table.selectionIndex)
                    if (!parent.isDisposed) {
                        cancelled.setFalse()
                        shell.close()
                    }
                }
            }
        })
        val cancelButton = Button(container, SWT.PUSH)
        cancelButton.text = "Cancel"
        applyGridData(80, 1, cancelButton)
        cancelButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                cancelled.setTrue()
                shell.close()
            }
        })

        shell.addDisposeListener {
            if (cancelled.isTrue) callback.accept(-1)
        }
        shell.pack()
        shell.open()
        return shell
    }

    private fun makeModifyListener(text: Text, table: Table): ModifyListener {
        return ModifyListener {
            if (text.text.isNotEmpty()) {
                table.removeListener(SWT.Selection, table.getListeners(SWT.Selection)[0])
                val search = text.text.lowercase(Locale.getDefault())
                var selection = -1
                var startItem = 0
                for ((i, curChar) in search.withIndex()) {
                    var found = false
                    for (item in startItem until table.itemCount) {
                        val listItem = table.getItem(item).getText(0).lowercase(Locale.getDefault())
                        if (listItem.length <= i) {
                            continue
                        }
                        if (listItem[i] == curChar) {
                            if (i > 0 && listItem.take(i) != search.take(i)) {
                                continue
                            }
                            selection = item
                            startItem = item
                            found = true
                            break
                        }
                    }
                    if (!found) break
                }
                if (selection >= 0) {
                    table.setSelection(selection)
                }
                table.addSelectionListener(makeTableListener(text, table))
            }
        }
    }

    private fun makeEnterListener(table: Table, dialog: Shell, callback: Consumer<Int>): KeyListener {
        return object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if ((e.keyCode == SWT.CR.code || e.keyCode == SWT.KEYPAD_CR) && table.selectionCount > 0) {
                    callback.accept(table.selectionIndex)
                    if (!dialog.isDisposed) dialog.close()
                }
            }
        }
    }

    private fun makeTableListener(text: Text, table: Table): SelectionAdapter {
        return object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                if (table.selectionCount > 0) {
                    text.removeListener(SWT.Modify, text.getListeners(SWT.Modify)[0])
                    text.text = table.selection[0].text
                    text.addModifyListener(makeModifyListener(text, table))
                }
            }
        }
    }

    private fun applyGridData(columns: Int, receiver: Control): GridData {
        val returnData = GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, columns, 1)
        receiver.layoutData = returnData
        return returnData
    }

    private fun applyGridData(widthHint: Int, columns: Int, receiver: Control): GridData {
        val newGridData = GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, columns, 1)
        newGridData.widthHint = widthHint
        receiver.layoutData = newGridData
        return newGridData
    }

    companion object {
        const val SHELL_TITLE: String = "Select"
        const val TABLE_HEIGHT: Int = 200
        const val TABLE_WIDTH: Int = 400
        const val DEFAULT_MESSAGE: String = "Select one:"
    }
}
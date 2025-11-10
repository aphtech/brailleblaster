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
package org.brailleblaster.perspectives.braille.views.wp.tableEditor

import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Spinner
import java.util.function.Consumer

internal class ConversionColumnSelectorDialog {
    fun open(callback: Consumer<Int?>, parent: Shell?, numOfItems: Int) {
        val shell = Shell(parent, SWT.DIALOG_TRIM or SWT.APPLICATION_MODAL)
        shell.text = TableEditor.CONVERT_MENU_ITEM
        shell.layout = GridLayout(1, true)
        val container = EasySWT.makeComposite(shell, 1)
        val columnCont = EasySWT.makeComposite(container, 2)
        EasySWT.makeLabel(columnCont, "Number of columns:", 1)

        val spinner = Spinner(columnCont, SWT.NONE)
        spinner.minimum = 2
        spinner.selection = 2

        EasySWT.buildComposite(container).apply {
            this.columns = 2
            this.addButton(TableEditor.OK_BUTTON, 1) {
                callback.accept(spinner.selection)
                shell.close()
            }
            this.addButton(TableEditor.CANCEL_BUTTON, 1) {
                callback.accept(null)
                shell.close()
            }
        }.build()
        shell.open()
        EasySWT.setLargeDialogSize(shell)
    }
}

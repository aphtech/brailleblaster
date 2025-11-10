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
package org.brailleblaster.util

import org.brailleblaster.utils.swt.EasySWT
import org.brailleblaster.wordprocessor.WPManager
import org.eclipse.swt.SWT
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class WorkingDialog(message: String?) : AutoCloseable {
    //Don't use FormUIUtils, user cannot close this window
    private val shell = Shell(Display.getCurrent(), SWT.TITLE or SWT.BORDER or SWT.ON_TOP)
    private val shellMessage: Text

    init {
        shell.layout = GridLayout(1, false)
        shell.text = "Working..."
        shell.images = WPManager.newShellIcons()

        shellMessage = Text(shell, SWT.NONE)
        updateMessage(message)

        EasySWT.setLargeDialogSize(shell)

        shell.open()
        doPendingSWTWork()
    }

    fun updateMessage(message: String?) {
        log.info(message)
        shellMessage.text = message
        doPendingSWTWork()
    }

    private fun doPendingSWTWork() {
        while (Display.getCurrent().readAndDispatch()) {
            //Keep going until there isn't anything to do
        }
    }

    fun finished() {
        shell.dispose()
    }

    override fun close() {
        finished()
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(WorkingDialog::class.java)
    }
}

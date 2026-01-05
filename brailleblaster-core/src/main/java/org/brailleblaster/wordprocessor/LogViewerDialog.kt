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
package org.brailleblaster.wordprocessor

import org.brailleblaster.BBIni
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.FormAttachment
import org.eclipse.swt.layout.FormData
import org.eclipse.swt.layout.FormLayout
import org.eclipse.swt.widgets.*
import org.slf4j.LoggerFactory
import java.io.*

class LogViewerDialog @JvmOverloads constructor(parent: Shell?, style: Int = SWT.NONE) : Dialog(parent, style) {
    var result = false
    fun open(): Boolean {
        val parent = parent
        val display = parent.display
        val dialogShell = Shell(
            parent, SWT.DIALOG_TRIM
                    or SWT.APPLICATION_MODAL or SWT.RESIZE or SWT.MAX
        )
        dialogShell.text = text
        val dialogLayout = FormLayout()
        dialogShell.layout = dialogLayout

        // Create the control objects first so we create them in the order for
        // tabbing
        val logText = Text(
            dialogShell, SWT.BORDER
                    or SWT.V_SCROLL or SWT.MULTI or SWT.WRAP
        )
        val saveButton = Button(dialogShell, SWT.PUSH)
        val closeButton = Button(dialogShell, SWT.PUSH)
        val closeData = FormData()
        closeData.height = 20
        closeData.bottom = FormAttachment(100, -10)
        // closeData.left = new FormAttachment(saveButton, 5);
        closeData.right = FormAttachment(100, -15)
        closeButton.layoutData = closeData
        closeButton.text = localeHandler["buttonClose"]
        closeButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(arg0: SelectionEvent) {
                result = true
                dialogShell.dispose()
            }
        })
        val saveData = FormData()
        saveData.height = 20
        saveData.right = FormAttachment(closeButton, -10)
        saveData.bottom = FormAttachment(closeButton, 0, SWT.BOTTOM)
        saveButton.layoutData = saveData
        saveButton.text = localeHandler["LogViewer.SaveLog"]
        saveButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(arg0: SelectionEvent) {
                val saveDialog = FileDialog(dialogShell, SWT.SAVE)
                saveDialog.setFilterNames("Log file (*.log)")
                saveDialog.setFilterExtensions("*.log")
                saveDialog.filterPath = System.getProperty("user.home")
                saveDialog.overwrite = true
                val saveResult = saveDialog.open()
                if (saveResult != null) {
                    try {
                        writeStringToFile(File(saveResult), logText.text)
                        val savedMsg = MessageBox(dialogShell, SWT.ICON_INFORMATION or SWT.OK)
                        savedMsg.text = localeHandler["LogViewer.SavedMsgBox.Title"]
                        savedMsg.message = localeHandler["LogViewer.SavedMsgBox.Message"]
                        savedMsg.open()
                    } catch (_: IOException) {
                        val saveErrorMsg = MessageBox(dialogShell, SWT.ICON_ERROR or SWT.OK)
                        saveErrorMsg.text = localeHandler["LogViewer.SaveErrorMsgBox.Title"]
                        saveErrorMsg.message = localeHandler["LogViewer.SaveErrorMsgBox.Message"]
                        saveErrorMsg.open()
                    }
                }
            }
        })
        saveButton.pack()
        closeData.width = saveButton.size.x
        val logTextData = FormData()
        logTextData.top = FormAttachment(0, 5)
        logTextData.left = FormAttachment(0, 5)
        logTextData.right = FormAttachment(100, -5)
        logTextData.width = 400
        logTextData.bottom = FormAttachment(closeButton, -15)
        logTextData.height = 400
        logText.layoutData = logTextData
        logText.editable = false
        try {
            logText.text = readFileToString(
                BBIni.logFilesPath.resolve(
                    "bb.log"
                ).toFile()
            )
        } catch (e: IOException) {
            log.error("Problem opening the log file", e)
            val msgBox = MessageBox(parent, SWT.ICON_ERROR or SWT.OK)
            msgBox.text = "Unable to open log file"
            msgBox.message = "There was a problem in reading the log file, so the log viewer will not be opened."
            result = false
            return false
        }
        // logText.setKeyBinding('a' | SWT.MOD1, ST.SELECT_ALL);
        logText.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                e.doit = true
                if (e.stateMask == SWT.MOD1 && e.keyCode == 'a'.code) {
                    logText.selectAll()
                    e.doit = false
                }
            }
        })
        dialogShell.pack()
        dialogShell.open()
        while (!dialogShell.isDisposed) {
            if (!display.readAndDispatch()) {
                display.sleep()
            }
        }
        return result
    }

    companion object {
        private val log = LoggerFactory.getLogger(LogViewerDialog::class.java)
        private val localeHandler = getDefault()
        @Throws(IOException::class)
        private fun readFileToString(inFile: File): String {
            val buf = StringBuilder()
            var line: String?
            BufferedReader(FileReader(inFile)).use { br ->
                while (br.readLine().also { line = it } != null) {
                    buf.append(line)
                    buf.append('\n')
                }
            }
            return buf.toString()
        }

        @Throws(IOException::class)
        private fun writeStringToFile(outFile: File, content: String) {
            BufferedWriter(FileWriter(outFile)).use { bw -> bw.write(content) }
        }
    }
}
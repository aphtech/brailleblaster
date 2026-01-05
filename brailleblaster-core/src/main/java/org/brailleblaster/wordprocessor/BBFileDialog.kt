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
import org.brailleblaster.utils.OS
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.utils.os
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.FileDialog
import org.eclipse.swt.widgets.MessageBox
import org.eclipse.swt.widgets.Shell
import org.slf4j.LoggerFactory
import java.io.File

class BBFileDialog @JvmOverloads constructor(
    shell: Shell?,
    type: Int,
    suggestedFileName: String?,
    filterNames: Array<String>,
    filterExtensions: Array<String>,
    filterIndex: Int = 0,
    filterPath: String = getDefaultFilterPath()
) {
    val widget: FileDialog = FileDialog(shell, type).also { w ->
        log.debug("Path {} Names {} Extensions {}", filterPath, filterNames, filterExtensions)
        log.debug("suggested file name: $suggestedFileName")
        if (suggestedFileName != null) {
            w.fileName = suggestedFileName
        }
        w.filterPath = filterPath
        w.setFilterNames(*filterNames)
        w.setFilterExtensions(*filterExtensions)
        w.overwrite = true
        w.filterPath = filterPath
    }

    fun open(): String? {
        return if (widget.style and SWT.OPEN == SWT.OPEN) {
            // used when called from open; the caller can handle a null return
            var validResult = false
            var openFileName: String? = null
            while (!validResult) {
                openFileName = widget.open()
                if (openFileName == null) {
                    // the user didn't select a file, don't do anything
                    validResult = true
                } else {
                    val file = File(openFileName)
                    if (file.canRead()) {
                        // the user selected a valid file, give back the string
                        // of the
                        // address
                        validResult = true
                    } else {
                        val errMsg = MessageBox(widget.parent, SWT.ICON_ERROR or SWT.OK)
                        if (!file.exists()) {
                            // file doesn't exist
                            errMsg.text = localeHandler["fileNotExist"]
                            errMsg.message = String.format(
                                localeHandler["fileNotExistMsg"],
                                openFileName
                            )
                        } else {
                            // can't read file
                            errMsg.text = localeHandler["couldNotReadFile"]
                            errMsg.message = String.format(
                                localeHandler["couldNotReadFileMsg"],
                                openFileName
                            )
                        }
                        errMsg.open()
                    }
                }
            }
            openFileName.also { savePath(it) }
        } else {
            // used when called from save as
            widget.open().also { savePath(it) }
        }
    }

    private fun savePath(path: String?) {
        if (path == null) {
            return  // will be null if user cancels
        }
        val f = File(path)
        if (f.exists()) {
            val parent = f.parent
            BBIni.propertyFileManager.save(LAST, parent)
        }
    }

    companion object {
        private val localeHandler = getDefault()
        private val log = LoggerFactory.getLogger(BBFileDialog::class.java)
        private const val LAST = "lastFileLocation"
        private fun getDefaultFilterPath(): String = (BBIni.propertyFileManager.getProperty(LAST)
            ?: if (OS.Windows == os) {
                System.getProperty("user.home", "c:\\")
            } else {
                "/"
            })
    }
}
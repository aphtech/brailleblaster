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
package org.brailleblaster.perspectives.braille.ui

import com.sun.jna.Platform
import org.brailleblaster.BBIni
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.utils.swt.EasyListeners
import org.brailleblaster.utils.swt.EasySWT
import org.brailleblaster.wordprocessor.WPManager
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import java.util.function.Consumer
import kotlin.math.floor

class ImagePlaceholder(parent: Shell?, manager: Manager, private val callback: Consumer<ArrayList<String?>>) {
    var shell: Shell
    var text: Text
    var cancel: Button
    var submit: Button
    var insertImageButton: Button
    var path: Label
    var lines: Int?
    var closeListener: Listener? = null
    var linesPerPage: Int = floor(
        manager.document.engine.pageSettings.drawableHeight /
                manager.document.engine.brailleSettings.cellType.height.toDouble()
    ).toInt()
    var imagePath: String? = null

    init {
        lines = null
        shell = Shell(parent, SWT.RESIZE or SWT.CLOSE or SWT.APPLICATION_MODAL)
        shell.text = "Insert Image Placeholder"
        EasySWT.addEscapeCloseListener(shell)
        val layout = GridLayout(2, false)
        shell.layout = layout
        val label = Label(shell, SWT.NONE)
        label.text = "Number of lines (<$linesPerPage):"
        label.layoutData = GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1)
        text = Text(shell, SWT.SINGLE or SWT.BORDER)
        val gd = GridData(GridData.FILL_HORIZONTAL or GridData.GRAB_HORIZONTAL)
        gd.horizontalSpan = 2
        text.layoutData = gd

//		Label enterImageLabel = new Label(shell, SWT.NONE);
//		enterImageLabel.setText("Insert Image Location:");
        insertImageButton = Button(shell, SWT.PUSH)
        insertImageButton.text = "Insert Image Location"
        insertImageButton.layoutData = gd
        path = Label(shell, SWT.NONE)
        path.text = "Location: "
        path.layoutData = GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1)
        submit = Button(shell, SWT.PUSH)
        submit.text = "Submit"
        cancel = Button(shell, SWT.PUSH)
        cancel.text = "Cancel"
        addListeners()
        shell.pack()
        shell.open()
    }

    private fun addListeners() {
        shell.addListener(SWT.Close, Listener {
            imagePath = null
            lines = null
        }.also { closeListener = it })
        cancel.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                lines = null
                imagePath = null
                shell.close()
            }
        })
        submit.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                submit()
            }
        })
        text.addListener(SWT.Verify) { e: Event ->
            val string = e.text
            val chars = CharArray(string.length)
            string.toCharArray(chars, 0, 0, chars.size)
            for (aChar in chars) {
                if ((aChar !in '0'..'9') && e.keyCode != SWT.CR.code) {
                    e.doit = false
                    return@addListener
                }
            }
        }
        insertImageButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                retrieveImagePath()
            }
        })
        EasyListeners.keyPress(text) { e: KeyEvent ->
            if (e.keyCode == SWT.CR.code || e.keyCode == SWT.KEYPAD_CR) {
                submit()
            }
        }
    }

    private fun submit() {
        if (text.text.isNotEmpty()) {
            val input = text.text.toInt()
            if (input > linesPerPage) {
                val message = MessageBox(WPManager.getInstance().shell)
                message.message = "Please enter a number below $linesPerPage."
                message.open()
                shell.forceActive()
                return
            }
            lines = input
        } else {
            lines = null
        }
        shell.removeListener(SWT.Close, closeListener)
        shell.close()
        if (imagePath != null || lines != null) {
            val list = ArrayList<String?>()
            list.add(if (lines != null) lines.toString() else null)
            list.add(imagePath)
            callback.accept(list)
        }
    }

    private fun retrieveImagePath() {
        if (!BBIni.debugging) {
            // Use the image placeholder dialog as parent so focus returns correctly, RT#8361
            val dialog = FileDialog(shell, SWT.OPEN)
            var filterPath: String? = "/"
            val updates = BBIni.propertyFileManager.getProperty("lastFileLocation")
            if (updates != null) {
                filterPath = updates
            }
            if (Platform.isWindows()) {
                filterPath = System.getProperty("user.home", "c:\\")
            }
            dialog.filterPath = filterPath
            val imagePath = dialog.open()
            if (imagePath != null) {
                path.text += imagePath
                this.imagePath = imagePath
            }
        }
    }
}
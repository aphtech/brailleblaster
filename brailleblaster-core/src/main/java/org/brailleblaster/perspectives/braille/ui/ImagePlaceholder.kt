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

import nu.xom.Element
import org.brailleblaster.BBIni
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.ImagePlaceholderTextMapElement
import org.brailleblaster.perspectives.braille.mapping.interfaces.Uneditable
import org.brailleblaster.perspectives.braille.stylers.ImagePlaceholderHandler
import org.brailleblaster.utils.OS
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.utils.os
import org.brailleblaster.utils.swt.EasyListeners
import org.brailleblaster.utils.swt.EasySWT
import org.brailleblaster.utils.xml.UTD_NS
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.floor

class ImagePlaceholder(parent: Shell?, manager: Manager) {
    var shell: Shell
    var linesText: Text
    var cancelButton: Button
    var submitButton: Button
    var insertImageButton: Button
    var pathLabel: Label
    var altTextText: Text
    var lines: Int? = null
    var closeListener: Listener? = null
    var linesPerPage: Int = floor(
        manager.document.engine.pageSettings.drawableHeight /
                manager.document.engine.brailleSettings.cellType.height.toDouble()
    ).toInt()
    var imagePath: String? = null
    val localeHandler = getDefault()

    init {
        //Check for existing placeholder and set lines and path accordingly
        val isImage = manager.mapList.current is ImagePlaceholderTextMapElement
        if (isImage){
            try {
                val cur = manager.mapList.current.nodeParent
                //println("Current image placeholder element: ${cur.toXML()}")
                lines = cur.getAttributeValue("skipLines", UTD_NS).toInt()
                imagePath = cur.getAttributeValue("src", UTD_NS)
            }
            catch (e: Exception){
                //println("Error retrieving existing image placeholder attributes: ${e.message}")
                lines = null
                imagePath = null
            }
        }

        ////println(getImageList(manager))

        shell = Shell(parent, SWT.RESIZE or SWT.CLOSE or SWT.APPLICATION_MODAL)
        shell.text = "Insert Image Placeholder"
        EasySWT.addEscapeCloseListener(shell)
        val layout = GridLayout(2, false)
        shell.layout = layout
        val linesLabel = Label(shell, SWT.NONE)
        linesLabel.text = "Number of lines (<$linesPerPage):"
        linesLabel.layoutData = GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1)
        linesText = Text(shell, SWT.SINGLE or SWT.BORDER)
        if (lines != null) {
            linesText.text = lines.toString()
        }
        val gd = GridData(GridData.FILL_HORIZONTAL or GridData.GRAB_HORIZONTAL)
        gd.horizontalSpan = 2
        gd.widthHint = 100

        pathLabel = Label(shell, SWT.NONE)
        if (imagePath != null)
            pathLabel.text = "Path: $imagePath"
        else
            pathLabel.text = "No image selected"
        pathLabel.layoutData = GridData(SWT.FILL, SWT.BEGINNING, true, false, 2, 1)

        insertImageButton = Button(shell, SWT.PUSH)
        insertImageButton.text = "Select Image"
        insertImageButton.layoutData = gd

        val altTextLabel = Label(shell, SWT.NONE)
        altTextLabel.text = "Alt Text (optional):"
        val gd2 = GridData(GridData.FILL_HORIZONTAL or GridData.GRAB_HORIZONTAL)
        gd2.horizontalSpan = 2
        gd2.heightHint = 100

        altTextText = Text(shell, SWT.MULTI or SWT.BORDER or SWT.V_SCROLL or SWT.WRAP)
        altTextText.layoutData = gd2
        if (isImage){
            altTextText.text = manager.mapList.current.nodeParent.getAttributeValue("altText", UTD_NS) ?: ""
        }

        submitButton = Button(shell, SWT.PUSH)
        if (isImage){
            submitButton.text = "Update"
        }
        else{
            submitButton.text = "Insert"
        }

        cancelButton = Button(shell, SWT.PUSH)
        cancelButton.text = "Cancel"
        addListeners(manager)
        shell.pack()
        shell.open()
    }

    private fun addListeners(manager: Manager) {
        shell.addListener(SWT.Close, Listener {
            //imagePath = null
            //lines = null
            //println("Closing image placeholder dialog")
        }.also { closeListener = it })

        cancelButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                //Change nothing, just close.
                //lines = null
                //imagePath = null
                //println("Cancelling image placeholder dialog")
                shell.close()
            }
        })
        submitButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                submit(manager)
            }
        })

        linesText.addListener(SWT.Verify) { e: Event ->
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
        EasyListeners.keyPress(linesText) { e: KeyEvent ->
            if (e.keyCode == SWT.CR.code || e.keyCode == SWT.KEYPAD_CR) {
                submit(manager)
            }
        }
    }

    fun submit(manager: Manager) {
        //Three cases:
        //New placeholder with lines and path
        //Modify existing placeholder's lines and/or path
        //Nothing entered / invalid input - do nothing
        //Likewise, cancelling the dialog should do nothing if on an existing placeholder.
        lines = linesText.text.toInt()

        if (manager.isEmptyDocument){
            manager.notify(localeHandler["emptyDocMenuWarning"])
            return
        }
        else if (lines == null || imagePath == null) {
            //println("No input provided; closing dialog without changes")
            manager.notify("Please provide a number of lines and an image path to insert an image placeholder.")
            return
        }
        else if (manager.mapList.current is ImagePlaceholderTextMapElement) {
            //println("Updating existing image placeholder")
            ImagePlaceholderHandler(manager, manager.viewInitializer, manager.mapList)
                .updateImagePlaceholder(lines, imagePath, altTextText.text)
        }
        else if (manager.mapList.current !is Uneditable){
            //println("Inserting new image placeholder")
            ImagePlaceholderHandler(manager, manager.viewInitializer, manager.mapList)
                .insertNewImagePlaceholder(lines, imagePath, altTextText.text)
        }
        else{
            //Un-editable position selected; do nothing.
            //println("Cannot insert image placeholder at current position; closing dialog without changes")
            return
        }

        shell.removeListener(SWT.Close, closeListener)
        shell.close()
    }

    private fun retrieveImagePath() {
        val dialog = FileDialog(shell, SWT.OPEN)
        dialog.setFilterExtensions("*.jpg;*.jpeg;*.pdf;*.png;*.svg")
        var filterPath: String? = "/"
        val updates = BBIni.propertyFileManager.getProperty("lastFileLocation")
        if (updates != null) {
            filterPath = updates
        }
        if (OS.Windows == os) {
            filterPath = System.getProperty("user.home", "c:\\")
        }
        dialog.filterPath = filterPath
        val imagePath = dialog.open()
        if (imagePath != null) {
            pathLabel.text = "Path: $imagePath"
            pathLabel.redraw()
            this.imagePath = imagePath
        }
    }

    private fun getImageList(manager: Manager): List<String> {
        //Find all ImagePlaceholder elements in the document
        //Probably trim down a string from the end to the last '/' or '\'
        //Looking for BLOCK bb:type="IMAGE_PLACEHOLDER"
        //This isn't working, despite my testing with the same XPath in an external tool.
        //What gives?
        val xpath = "/BLOCK[@bb:type='IMAGE_PLACEHOLDER']"
        val results = manager.simpleManager.doc.query(xpath).toList()
        //println("Found ${results.size} image placeholders in document.")
        val elementList = results.map{
            it as Element
        } as MutableList<Element>
        val delim = if (OS.Windows == os) '\\' else '/'
        //Use a truncated version of the src path for display
        val imageStrings = elementList.map{
            it.getAttributeValue("src", UTD_NS).substringAfterLast(delim)
        }

        return imageStrings
    }
}
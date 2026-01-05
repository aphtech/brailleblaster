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
package org.brailleblaster.debug

import nu.xom.Document
import nu.xom.Element
import nu.xom.Node
import nu.xom.Serializer
import org.apache.commons.lang3.exception.ExceptionUtils
import org.brailleblaster.bbx.BBX
import org.brailleblaster.perspectives.mvc.BBSimpleManager
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.UTDHelper
import org.brailleblaster.util.ColorManager
import org.brailleblaster.utils.swt.EasySWT
import org.brailleblaster.utils.xml.UTD_NS
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyleRange
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.events.*
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets

class XMLDebugger(private val parent: Shell, private val simpleManager: BBSimpleManager) {
    lateinit var dialog: Shell
    private var xmlViewer: StyledText? = null
    private lateinit var curNode: Node
    private var showBrlValue = true
    private var formatXML = true
    private var warnEmptyText = true
    private var colorTagsButton: Button? = null
    private val styles: MutableList<StyleRange> = ArrayList()
    private var activeDocument: Document? = null
    private lateinit var findButton: Text

    init {
        open()
        setNode(simpleManager.currentCaret.node)
    }

    fun open() {
        dialog = EasySWT.makeDialogFloating(parent)
        dialog.text = "Debug: XML Viewer"
        //		dialog.setSize(400, 200);
        val gridLayoutCols = 10
        dialog.layout = GridLayout(gridLayoutCols, false)
        xmlViewer = StyledText(dialog, SWT.V_SCROLL or SWT.WRAP or SWT.READ_ONLY)
        xmlViewer!!.layoutData = GridData(SWT.FILL, SWT.FILL, true, true, gridLayoutCols, 1)
        val updateButton = Button(dialog, SWT.PUSH)
        updateButton.layoutData = GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 1, 1)
        updateButton.text = "Update"
        updateButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                setNode(simpleManager.currentCaret.node)
            }
        })
        val getParentButton = Button(dialog, SWT.PUSH)
        getParentButton.layoutData = GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 1, 1)
        getParentButton.text = "Get Parent"
        getParentButton.addSelectionListener(object : SelectionListener {
            override fun widgetDefaultSelected(arg0: SelectionEvent) {}
            override fun widgetSelected(arg0: SelectionEvent) {
                if (curNode.parent != null) {
                    setNode(curNode.parent)
                }
            }
        })
        val showBrl = Button(dialog, SWT.CHECK)
        showBrl.text = "Show <brl> tags"
        showBrl.selection = showBrlValue
        EasySWT.addSelectionListener(showBrl) {
            showBrlValue = !showBrlValue
            setNode(curNode)
        }
        colorTagsButton = Button(dialog, SWT.CHECK)
        colorTagsButton!!.text = "Show tags in different color"
        colorTagsButton!!.selection = true
        EasySWT.addSelectionListener(colorTagsButton!!) {
            setColorTagsEnabled(
                colorTagsButton!!.selection
            )
        }
        val formattedButton = Button(dialog, SWT.CHECK)
        formattedButton.text = "Format XML"
        formattedButton.selection = formatXML
        EasySWT.addSelectionListener(formattedButton) {
            formatXML = !formatXML
            setNode(curNode)
        }
        val warnEmptyTextButton = Button(dialog, SWT.CHECK)
        warnEmptyTextButton.text = "Warn Empty Text"
        warnEmptyTextButton.selection = warnEmptyText
        EasySWT.addSelectionListener(warnEmptyTextButton) {
            warnEmptyText = !warnEmptyText
            setNode(curNode)
        }
        val saveButton = Button(dialog, SWT.PUSH)
        saveButton.text = "Save this to BBX File"
        EasySWT.addSelectionListener(saveButton) { save() }
        findButton = Text(dialog, SWT.BORDER)
        findButton.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == SWT.CR.code) {
                    if (e.stateMask == SWT.SHIFT) {
                        findPrev()
                    } else {
                        findNext()
                    }
                }
            }
        })
        val nextButton = Button(dialog, SWT.NONE)
        nextButton.text = "Next"
        EasySWT.addSelectionListener(nextButton) { findNext() }
        val prevButton = Button(dialog, SWT.NONE)
        prevButton.text = "Previous"
        EasySWT.addSelectionListener(prevButton) { findPrev() }
        dialog.open()
    }

    private fun findNext() {
        val relevantText = xmlViewer!!.getText(xmlViewer!!.caretOffset, xmlViewer!!.charCount - 1)
        var offset = relevantText.indexOf(findButton.text)
        if (offset == 0) {
            offset = relevantText.indexOf(findButton.text, 1)
        }
        if (offset == -1) {
            findRed(true)
        } else {
            findRed(false)
            xmlViewer!!.caretOffset += offset
            EasySWT.scrollViewToCursor(xmlViewer!!)
            xmlViewer!!.setSelectionRange(xmlViewer!!.caretOffset, findButton.text.length)
            xmlViewer!!.setFocus()
        }
    }

    private fun findPrev() {
        val relevantText = xmlViewer!!.getText(0, xmlViewer!!.caretOffset)
        var offset = relevantText.lastIndexOf(findButton.text)
        if (offset == xmlViewer!!.caretOffset) {
            offset = relevantText.dropLast(1).lastIndexOf(findButton.text, 1)
        }
        if (offset == -1) {
            findRed(true)
        } else {
            findRed(false)
            xmlViewer!!.caretOffset = offset
            EasySWT.scrollViewToCursor(xmlViewer!!)
            xmlViewer!!.setSelectionRange(xmlViewer!!.caretOffset, findButton.text.length)
            xmlViewer!!.setFocus()
        }
    }

    private fun findRed(red: Boolean) {
        val color: Color = if (red) {
            ColorManager.getColor(ColorManager.Colors.RED, findButton)
        } else {
            ColorManager.getColor(ColorManager.Colors.WHITE, findButton)
        }
        findButton.background = color
    }

    fun setNode(newNode: Node) {
        var n: Node? = newNode
        styles.clear()
        setColorTagsEnabled(false)
        val bytes = ByteArrayOutputStream()
        val serial = Serializer(bytes)
        if (formatXML) {
            serial.indent = 4
        }
        //		serial.setMaxLength(100);
        if (n is nu.xom.Text) n = n.parent
        if (n == null) return
        curNode = n
        activeDocument = n as? Document ?: Document(n.copy() as Element)
        if (!showBrlValue) UTDHelper.getDescendantBrlFast(activeDocument) { it.detach() }
        if (warnEmptyText) {
            FastXPath.descendant(activeDocument)
                .filterIsInstance<nu.xom.Text>()
                .filter { it.value.isEmpty() }
                .forEach { it.value = "[!!!! EMPTY TEXT NODE !!!!]" }
        }
        activeDocument!!.rootElement.addNamespaceDeclaration(UTDElements.UTD_PREFIX, UTD_NS)
        var newText: String
        newText = try {
            serial.write(activeDocument)
            bytes.toString(StandardCharsets.UTF_8)
        } catch (e: IOException) {
            ExceptionUtils.getStackTrace(e)
        }
        if (newText.contains("?>")) {
            newText = newText.substring(newText.indexOf('>') + 1)
        }
        xmlViewer!!.text = newText

        //XOM serializer doesn't expose an easy way to get indexes of elements as they
        //are written. Reflection runs into into weird exceptions from itternal xom and 
        //xerces classes. Try #3: Regex
//		Pattern pattern = Pattern.compile("<(?:\"[^\"]*\"['\"]*|'[^']*'['\"]*|[^'\">])+(?<!/\\s*)>", Pattern.DOTALL);
//		Matcher matcher = pattern.matcher(newText);
//		while(matcher.find()) {
//			String match = matcher.group();
//			
//			StyleRange range = new StyleRange();
//			range.start = matcher.start();
//			range.length = matcher.end() - matcher.start();
//			range.background = Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA);
//			
//			styles.add(range);
//		}

        //Try #4: dumb brute force way because regex
        val textChars = newText.toCharArray()
        var start = -1
        for (i in textChars.indices) {
            val curChar = textChars[i]
            if (curChar == '<' && start == -1) {
                start = i
            } else if (curChar == '>' && start != -1) {
                //Just found a tag
                val range = StyleRange()
                range.start = start
                range.length = i - start +  /*exclusive*/1
                val content = newText.substring(start, i)
                if (content.startsWith("<utd:") || content.startsWith("</utd:")) range.background =
                    Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA) else range.background =
                    Display.getCurrent().getSystemColor(SWT.COLOR_GREEN)
                styles.add(range)
                start = -1
            }
        }
        setColorTagsEnabled(colorTagsButton!!.selection)
        EasySWT.setLargeDialogSize(dialog)
    }

    fun setColorTagsEnabled(enabled: Boolean) {
        if (enabled) {
            xmlViewer!!.styleRanges = styles.toTypedArray<StyleRange>()
        } else {
            xmlViewer!!.replaceStyleRanges(0, xmlViewer!!.charCount, arrayOfNulls(0))
        }
    }

    private fun save() {
        //Restricted version of BBFileDialog
        val saveAs = FileDialog(dialog, SWT.SAVE)
        saveAs.setFilterExtensions("*.xml")
        val path = saveAs.open()
        if (path == null) log.debug("canceled save") else {
            val doc = BBX.newDocument()
            val root = BBX.SECTION.ROOT.create()
            root.appendChild(activeDocument!!.rootElement.copy())
            doc.rootElement.appendChild(root)
            XMLHandler().save(doc, File(path))
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(XMLDebugger::class.java)
    }
}

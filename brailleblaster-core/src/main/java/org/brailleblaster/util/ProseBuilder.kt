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

import nu.xom.*
import nu.xom.Text
import org.brailleblaster.BBIni
import org.brailleblaster.bbx.BBX
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.math.mathml.MathModule
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.toolbar.CustomToolBarBuilder
import org.brailleblaster.perspectives.braille.toolbar.CustomToolBarBuilder.CustomButton
import org.brailleblaster.perspectives.braille.toolbar.CustomToolBarBuilder.CustomText
import org.brailleblaster.perspectives.braille.views.wp.TextView
import org.brailleblaster.perspectives.mvc.menu.MenuManager
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.XMLTextCaret
import org.brailleblaster.perspectives.mvc.events.BuildMenuEvent
import org.brailleblaster.perspectives.mvc.events.BuildToolBarEvent
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.tools.MenuToolListener
import org.brailleblaster.utd.internal.xml.XMLHandler2
import org.brailleblaster.utd.utils.TextTranslator
import org.brailleblaster.utd.utils.UTDHelper
import org.brailleblaster.utils.BB_NS
import org.brailleblaster.utils.swt.AccessibilityUtils.setName
import org.brailleblaster.wordprocessor.WPManager
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.VerifyKeyListener
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.VerifyEvent
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.*
import java.util.function.Consumer

class ProseBuilder : MenuToolListener {
    /**
     * Note: instance variable to reset in between tests
     * @see .isEnabled
     */
    private var toolbarEnabled = BBIni.propertyFileManager.getPropertyAsBoolean(SETTING_ENABLED, false)
    private var toolbarEnabledChanged = false
    private var toolbarPlaced = false
    private var listenerAdded = false
    private var lineNumberInt = 1
    private var incrementInt = 1
    private var wrapButton: CustomButton? = null
    var textView: TextView? = null
    private var manager: Manager? = null
    private var startNode: Node? = null
    var keyListener: VerifyKeyListener? = null
    private var shell: Shell? = null
    override val topMenu = TopMenu.TOOLS
    override val title = "Line Number Tools"
    override fun onRun(bbData: BBSelectionData) {
        enableToolbar(bbData.manager)
    }

    override fun onEvent(event: SimpleEvent) {
        if (event is BuildMenuEvent) {
            MenuManager.addMenuItem(this)
        }
        if (event is BuildToolBarEvent) {
            if (toolbarEnabled) {
                manager = event.manager.manager
                textView = event.manager.manager.text
                addToolbar()
                toolbarPlaced = true
            }
        }
    }

    fun addToolbar() {
        val builder = object : CustomToolBarBuilder() {
            override fun build(parent: Composite): Composite {
                val result = super.build(parent)!!
                if (toolbarEnabledChanged) {
                    wrapButton!!.widget.setFocus()
                    toolbarEnabledChanged = false
                }
                return result
            }
        }
        wrapButton =
            builder.addButton("Wrap Prose (CTRL + F2)", 0, { wrapSelectedElements() })
        builder.addLabel("Line Number:", 0)
        val lineNumber = builder.addText(50, lineNumberInt.toString())
        builder.addLabel("Increment by:", 0)
        val increment = builder.addText(50, incrementInt.toString())
        builder.addButton("Insert (F2)", 0, { insertLineNumber(lineNumber, increment) })
        builder.addButton(
            "Edit Line Number (CTRL + SHIFT + F2)",
            0,
            { editLineNumber() })
        object : CustomButton(builder, 0, "X", Consumer { close() }, null) {
            override fun doBuild(parent: Composite?): Control {
                val control = super.doBuild(parent)
                setName(control, "Close Line Number Tools")
                return control
            }
        }
        if (keyListener != null && listenerAdded && toolbarPlaced && !textView!!.view.isDisposed) {
            textView!!.view.removeVerifyKeyListener(keyListener)
        }
        if (!textView!!.view.isDisposed) {
            addKeyListener(lineNumber, increment)
        }
        WPManager.getInstance().currentPerspective.addToToolBar(builder, keyListener)
        listenerAdded = true
    }

    fun addKeyListener(lineNumber: CustomText, increment: CustomText) {
        keyListener = VerifyKeyListener { event: VerifyEvent ->
            if (event.stateMask == SWT.CTRL && event.keyCode == SWT.F2) {
                wrapSelectedElements()
                event.doit = false
            } else if (event.stateMask == SWT.CTRL or SWT.SHIFT && event.keyCode == SWT.F2) {
                editLineNumber()
                event.doit = false
            } else if (event.keyCode == SWT.F2) {
                insertLineNumber(lineNumber, increment)
                event.doit = false
            } else if (event.keyCode == SWT.ESC.code) {
                close()
            }
        }
        textView!!.view.forceFocus()
        textView!!.view.addVerifyKeyListener(keyListener)
    }

    fun enableToolbar(manager: Manager) {
        this.manager = manager
        textView = manager.text
        toolbarEnabled = !toolbarEnabled
        toolbarEnabledChanged = true
        toolbarPlaced = !toolbarPlaced
        BBIni.propertyFileManager.saveAsBoolean(SETTING_ENABLED, toolbarEnabled)
        WPManager.getInstance().buildToolBar()
    }

    private fun close() {
        toolbarEnabled = false
        toolbarPlaced = false
        BBIni.propertyFileManager.saveAsBoolean(SETTING_ENABLED, toolbarEnabled)
        WPManager.getInstance().buildToolBar()
    }

    private fun insertLineNumber(text: CustomText, increment: CustomText) {
        val lineNumberText = text.widget
        val incrementText = increment.widget

        //Check increments first. Raise a message if you have 0
        if (incrementText.text.toInt() < 1) {
            val message = MessageBox(WPManager.getInstance().shell)
            message.message = "Increments must be 1 or higher."
            message.open()
            return
        }
        val pastedSpan = addLineElement(lineNumberText.text) as Element? ?: return
        try {
            checkForSpace(pastedSpan)
        } catch (_: Exception) {
            val parentShell = manager!!.wpManager.shell
            val errorShell = Shell(parentShell)
            errorShell.setSize(250, 70)
            errorShell.text = "Warning!"
            errorShell.layout = RowLayout()
            Label(errorShell, SWT.NONE).text = "Invalid location for a line number."
            val buttonOK = Button(errorShell, SWT.PUSH)
            buttonOK.text = "OK"
            buttonOK.addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent) {
                    errorShell.close()
                }
            })
            errorShell.pack()
            errorShell.open()
            return
        }
        lineNumberText.text = checkLineNumber(lineNumberText, incrementText)
        manager!!.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, true, pastedSpan))
    }

    private fun checkLineNumber(
        lineNumberText: org.eclipse.swt.widgets.Text,
        incrementText: org.eclipse.swt.widgets.Text
    ): String {
        //Allow insertion of letters (for line lettered poetry), but do not increment
        return try {
            val nextInt = incrementLineNumber(lineNumberText.text.toInt(), incrementText.text.toInt())
            lineNumberInt = nextInt
            incrementInt = incrementText.text.toInt()
            nextInt.toString()
        } catch (_: NumberFormatException) {
            lineNumberText.text
        }
    }

    private fun incrementLineNumber(curr: Int, inc: Int): Int {
        return curr + inc
    }

    private fun addLineElement(lineNumber: String): Node? {
        val currentNode = manager!!.simpleManager.currentSelection.start.node
        val p = currentNode.parent
        val index = p.indexOf(currentNode)

        //Cannot insert numbers that are longer than 10 digits
        if (lineNumber.length > 10) {
            lineNumberTooLong()
            return null
        }

        //Does not allow multiple line numbers to be inserted at same position
        if (currentNode is Text) {
            val startOffset = (manager!!.simpleManager.currentSelection.start as XMLTextCaret).offset
            //Check for both kinds of line numbers
            val instance1 =
                index < p.childCount - 2 && (BBX.SPAN.PROSE_LINE_NUMBER.isA(p.getChild(index + 2)) || BBX.SPAN.POEM_LINE_NUMBER.isA(
                    p.getChild(index + 2)
                )) && startOffset == 0
            val instance2 =
                index > 1 && (BBX.SPAN.PROSE_LINE_NUMBER.isA(p.getChild(index - 1)) || BBX.SPAN.POEM_LINE_NUMBER.isA(
                    p.getChild(index - 1)
                )) && startOffset == 0
            if (instance1 || instance2) {
                val message = MessageBox(WPManager.getInstance().shell)
                message.message = localeHandler["invalidPositionMessage"]
                message.open()
                return null
            }
        }

        //Create element
        var lineSpan = BBX.SPAN.PROSE_LINE_NUMBER.create()

        //Find out if the you're inside a poem. If so, the line number should be a poem line number
        val poemParents = currentNode.query("ancestor::node()[@utd-style='Poetic Stanza' or @utd-style='Poetry Line']")
        if (poemParents.size() > 0) {
            lineSpan = BBX.SPAN.POEM_LINE_NUMBER.create()
            lineSpan.addAttribute(Attribute("type", "poem"))
            lineSpan.appendChild(lineNumber)
        } else {
            lineSpan.addAttribute(Attribute("type", "prose"))
            lineSpan.addAttribute(Attribute("class", "linenum"))
            lineSpan.addAttribute(Attribute("printLineNum", lineNumber))
            val lineNum = TextTranslator.translateText(lineNumber, manager!!.document.engine)
            lineSpan.addAttribute(Attribute("linenum", lineNum))
        }

//		List<Node> toInsert = new ArrayList<Node>();
//		toInsert.add(lineSpan);
        try {
            if (currentNode is Text) {
                //Empty string-> don't split, don't process, give error
                if (currentNode.toXML().isEmpty()) {
                    throw IndexOutOfBoundsException()
                } else if (currentNode.toXML().length == 1) {
                    if (index == p.childCount - 1) p.appendChild(lineSpan) else p.insertChild(lineSpan, index + 1)
                } else {
                    val splitTextNode =
                        XMLHandler2.splitTextNode(
                            currentNode,
                            (manager!!.simpleManager.currentSelection.end as XMLTextCaret).offset
                        )
                    if (splitTextNode.size > 1) {
                        splitTextNode[0].parent.insertChild(
                            lineSpan,
                            splitTextNode[0].parent.indexOf(splitTextNode[1])
                        )
                    } else {
                        Utils.insertChildCountSafe(splitTextNode[0].parent, lineSpan, index + 1)
                    }

                    //Take care of empty text nodes
                    if (splitTextNode[0].value.isEmpty()) {
                        splitTextNode[0].detach()
                    }
                    //					if(splitTextNode.get(1).getValue().isEmpty()){
//						splitTextNode.get(1).detach();
//					}
                }
            }
        } catch (_: IndexOutOfBoundsException) {
            val message = MessageBox(WPManager.getInstance().shell)
            message.message = "Node was not recognized. Unable to add a line number."
            message.open()
            return null
        }
        return lineSpan
    }

    private fun checkForSpace(span: Element) {
        val parent = span.parent
        val spanIndex = parent.indexOf(span)
        var spacePresent = 0
        if (spanIndex > 0) {
            //If it's before, check the last character for a space
            val nodeBefore = parent.getChild(spanIndex - 1)
            if (nodeBefore is Text) {
                val text = nodeBefore.value
                if (text.isNotEmpty() && text[text.length - 1] == ' ') {
                    spacePresent++
                }
            }
        }
        span.addAttribute(Attribute("space", spacePresent.toString()))
    }

    private fun wrapSelectedElements() {
        val div = BBX.CONTAINER.PROSE.create()
        div.addAttribute(Attribute("class", "linedProse"))
        startNode = manager!!.simpleManager.currentSelection.start.node
        var endNode = manager!!.simpleManager.currentSelection.end.node
        if (startNode is Text
            || MathModule.isMath(startNode)
        ) {
            startNode = getBlockParent(startNode)
        }
        if (endNode is Text
            || MathModule.isMath(endNode)
        ) {
            endNode = getBlockParent(endNode)
        }

        //Check if start and/or end node is already inside a prose tag. If so, unwrap and update
        if (unwrapProseParent(startNode) || unwrapProseParent(endNode)) {
            manager!!.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, true, startNode!!.parent))
            return
        }

        //Get common parent of start and end node
        val parent = getCommonParent(startNode, endNode) as ParentNode
        if (manager!!.simpleManager.currentSelection.isSingleNode) {
            div.appendChild(startNode!!.copy())
            parent.replaceChild(startNode, div)
        } else {
            val followingStart = startNode!!.query("following-sibling::*")
            val precedingEnd = endNode.query("preceding-sibling::*")
            val selection = getIntersection(followingStart, precedingEnd)
            selection.add(0, startNode)
            if (startNode !== endNode) {
                selection.add(endNode)
            }
            val index = parent.indexOf(startNode)
            for (node in selection) {
                parent.removeChild(node)
                div.appendChild(node)
            }
            parent.insertChild(div, index)
        }
        manager!!.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, true, div.parent))
    }

    private fun editLineNumber() {
        val currentNode = manager!!.simpleManager.currentSelection.start.node

        //!!!For some reason, span and text aren't wrapped together in one block anymore!!!
//		//Check the current node. If it's a text node, get block parent
//		if (currentNode instanceof Text) {
//			currentNode = getBlockParent(currentNode);
//		}
//		
//		//Find a line number inside the current node, if any
//		Nodes linenums = currentNode.query("descendant::node()[@bb:type='PROSE_LINE_NUMBER' or @bb:type='POEM_LINE_NUMBER']",
//											BBX.XPATH_CONTEXT);
//		if (linenums.size() != 0) {
//			lineNum = (Element) linenums.get(0);
//		}
        var lineNum = findNearestLineNumber(currentNode) as Element?
        if (lineNum == null) {
            //If you're still not able to find any, retrieve the next available line number
            //but make sure to limit it such that the current node and the new line number
            //are in the same BLOCK
            val nextLineNum = retrieveFollowingLineNumber(currentNode)
            if (nextLineNum != null && getBlockParent(currentNode) === getBlockParent(nextLineNum)) {
                lineNum = nextLineNum as Element?
            }
        }
        if (lineNum == null) {
            val message = MessageBox(WPManager.getInstance().shell)
            message.message = "Found no line numbers on this line."
            message.open()
        } else {
            //Open a window where user can edit the number
            openEditWindow(lineNum)
        }
    }

    private fun findNearestLineNumber(node: Node): Node? {
        val parent = node.parent
        val index = parent.indexOf(node)
        if (node is Text) {
            if (index > 0 && (BBX.SPAN.PROSE_LINE_NUMBER.isA(parent.getChild(index - 1)) || BBX.SPAN.POEM_LINE_NUMBER.isA(
                    parent.getChild(index - 1)
                ))
            ) {
                return parent.getChild(index - 1)
            }
        } else if (node is Element) {
            return findNearestLineNumber(UTDHelper.getFirstTextDescendant(node))
        }
        return null
    }

    var activeLineNum: Element? = null
    private fun openEditWindow(lineNum: Element) {
        activeLineNum = lineNum
        if (shell != null && !shell!!.isDisposed) {
            shell!!.close()
        }
        shell = Shell(manager!!.wpManager.shell, SWT.CLOSE or SWT.ON_TOP)
        shell!!.setSize(200, 125)
        shell!!.text = "Edit Line Number"
        val gridLayout = GridLayout()
        gridLayout.numColumns = 2
        shell!!.layout = gridLayout
        val editText = org.eclipse.swt.widgets.Text(shell, SWT.BORDER)
        editText.layoutData = GridData(GridData.FILL_HORIZONTAL)
        if (isProseLineNumber(activeLineNum)) {
            editText.text = activeLineNum!!.getAttributeValue("printLineNum")
        } else if (isPoemLineNumber(activeLineNum)) {
            editText.text = activeLineNum!!.value
        }
        val okButton = Button(shell, SWT.PUSH)
        okButton.text = "Apply"
        okButton.layoutData = GridData(GridData.FILL_HORIZONTAL)
        okButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                if (isPoemLineNumber(activeLineNum)) {
                    if (!editPoemLineNumber(activeLineNum, editText.text)) {
                        shell!!.setFocus()
                    }
                    if (activeLineNum != null) {
                        editText.text = activeLineNum!!.value
                    } else {
                        editText.text = ""
                    }
                } else if (isProseLineNumber(activeLineNum)) {
                    if (!editProseLineNumber(activeLineNum, editText.text)) {
                        shell!!.setFocus()
                    }
                    if (activeLineNum != null) {
                        editText.text = activeLineNum!!.getAttributeValue("printLineNum")
                    } else {
                        editText.text = ""
                    }
                }

                //If you're deleting line numbers and it's the last line number in the document, close the window.
                if (activeLineNum == null && editText.text.isEmpty()) {
                    shell!!.close()
                }
            }
        })
        val prevButton = Button(shell, SWT.PUSH)
        prevButton.text = "Previous"
        prevButton.layoutData = GridData(GridData.FILL_HORIZONTAL)
        prevButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                val newLineNum = retrievePrecedingLineNumber(activeLineNum)
                if (newLineNum != null || editText.text.isEmpty()) {
                    if (isProseLineNumber(activeLineNum)) {
                        if (!editProseLineNumber(activeLineNum, editText.text)) {
                            shell!!.setFocus()
                        }
                        activeLineNum = newLineNum as Element?
                        if (activeLineNum != null) {
                            editText.text = activeLineNum!!.getAttributeValue("printLineNum")
                        } else {
                            editText.text = ""
                        }
                    } else if (isPoemLineNumber(activeLineNum)) {
                        if (!editPoemLineNumber(activeLineNum, editText.text)) {
                            shell!!.setFocus()
                        }
                        activeLineNum = newLineNum as Element?
                        if (activeLineNum != null) {
                            editText.text = activeLineNum!!.value
                        } else {
                            editText.text = ""
                        }
                    }
                }
                if (newLineNum == null) {
                    val message = MessageBox(WPManager.getInstance().shell)
                    message.message = "Unable to find previous line number."
                    message.open()
                    shell!!.close()
                }
            }
        })
        val nextButton = Button(shell, SWT.PUSH)
        nextButton.text = "Next"
        nextButton.layoutData = GridData(GridData.FILL_HORIZONTAL)
        nextButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                val newLineNum = retrieveFollowingLineNumber(activeLineNum)
                if (newLineNum != null || editText.text.isEmpty()) {
                    if (isProseLineNumber(activeLineNum)) {
                        if (!editProseLineNumber(activeLineNum, editText.text)) {
                            shell!!.setFocus()
                        }
                        activeLineNum = newLineNum as Element?
                        if (activeLineNum != null) {
                            editText.text = activeLineNum!!.getAttributeValue("printLineNum")
                        } else {
                            editText.text = ""
                        }
                    } else if (isPoemLineNumber(activeLineNum)) {
                        if (!editPoemLineNumber(activeLineNum, editText.text)) {
                            shell!!.setFocus()
                        }
                        activeLineNum = newLineNum as Element?
                        if (activeLineNum != null) {
                            editText.text = activeLineNum!!.value
                        } else {
                            editText.text = ""
                        }
                    }
                }
                if (newLineNum == null) {
                    val message = MessageBox(WPManager.getInstance().shell)
                    message.message = "Unable to find next line number."
                    message.open()
                    shell!!.close()
                }
            }
        })
        val doneButton = Button(shell, SWT.PUSH)
        doneButton.text = "DONE"
        doneButton.layoutData = GridData(GridData.FILL_HORIZONTAL)
        doneButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                shell!!.close()
            }
        })
        shell!!.open()
    }

    private fun retrieveFollowingLineNumber(lineNumber: Node?): Node? {
        var linenums = lineNumber!!.query(
            "following-sibling::node()[@bb:type='PROSE_LINE_NUMBER' or @bb:type='POEM_LINE_NUMBER']",
            BBX.XPATH_CONTEXT
        )
        if (linenums.size() == 0) {
            linenums = lineNumber.query(
                "following::node()[@bb:type='PROSE_LINE_NUMBER' or @bb:type='POEM_LINE_NUMBER']",
                BBX.XPATH_CONTEXT
            )
        }
        return if (linenums.size() > 0) {
            linenums[0]
        } else null
    }

    private fun retrievePrecedingLineNumber(lineNumber: Node?): Node? {
        var linenums = lineNumber!!.query(
            "preceding-sibling::node()[@bb:type='PROSE_LINE_NUMBER' or @bb:type='POEM_LINE_NUMBER']",
            BBX.XPATH_CONTEXT
        )
        if (linenums.size() == 0) {
            linenums = lineNumber.query(
                "preceding::node()[@bb:type='PROSE_LINE_NUMBER' or @bb:type='POEM_LINE_NUMBER']",
                BBX.XPATH_CONTEXT
            )
        }
        return if (linenums.size() > 0) {
            linenums[linenums.size() - 1]
        } else null
    }

    private fun editPoemLineNumber(lineNum: Element?, newNum: String): Boolean {
        val parent = lineNum!!.parent
        if (newNum.isEmpty()) {
            activeLineNum = retrieveFollowingLineNumber(activeLineNum) as Element?
            lineNum.detach()
        } else if (newNum.length > 10) {
            lineNumberTooLong()
            return false
        } else {
            lineNum.removeChild(0)
            lineNum.appendChild(newNum)
            activeLineNum = lineNum
        }
        manager!!.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, true, parent))
        return true
    }

    private fun editProseLineNumber(lineNum: Element?, newNum: String): Boolean {
        val parent: Node = lineNum!!.parent
        if (newNum.isEmpty()) {
            activeLineNum = retrieveFollowingLineNumber(activeLineNum) as Element?
            lineNum.detach()
        } else if (newNum.length > 10) {
            lineNumberTooLong()
            return false
        } else {
            lineNum.addAttribute(Attribute("printLineNum", newNum))
            val newLineNum = TextTranslator.translateText(newNum, manager!!.document.engine)
            lineNum.addAttribute(Attribute("linenum", newLineNum))
            activeLineNum = lineNum
        }
        manager!!.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, false, parent))
        return true
    }

    private fun isProseLineNumber(lineNum: Element?): Boolean {
        return lineNum!!.getAttributeValue("type", BB_NS) == "PROSE_LINE_NUMBER"
    }

    private fun isPoemLineNumber(lineNum: Element?): Boolean {
        return lineNum!!.getAttributeValue("type", BB_NS) == "POEM_LINE_NUMBER"
    }

    private fun getBlockParent(child: Node?): Node {
        return if (BBX.BLOCK.isA(child!!.parent)) {
            child.parent
        } else getBlockParent(child.parent)
    }

    private fun getCommonParent(start: Node?, end: Node): Node {
        if (start!!.parent != end.parent) {
            val startAncestors = start.query("ancestor::node()")
            val endAncestors = end.query("ancestor::node()")
            for (i in startAncestors.size() - 1 downTo -1 + 1) {
                for (j in 0 until endAncestors.size()) {
                    if (startAncestors[i] == endAncestors[j]) {
                        startNode = startAncestors[i + 1]
                        return startAncestors[i]
                    }
                }
            }
        }
        return start.parent
    }

    private fun unwrapProseParent(node: Node?): Boolean {
        if (BBX.CONTAINER.PROSE.isA(node)) {
            XMLHandler2.unwrapElement(node as Element)
            return true
        }
        val ancestors = node!!.query("ancestor::node()")
        for (i in ancestors.size() - 1 downTo -1 + 1) {
            if (BBX.CONTAINER.PROSE.isA(ancestors[i])) {
                XMLHandler2.unwrapElement(ancestors[i] as Element)
                return true
            }
        }
        return false
    }

    private fun getIntersection(start: Nodes, end: Nodes): MutableList<Node?> {
        val intersection: MutableList<Node?> = ArrayList()
        for (i in 0 until start.size()) {
            if (end.contains(start[i])) {
                intersection.add(start[i])
            }
        }
        return intersection
    }

    private fun lineNumberTooLong() {
        val message = MessageBox(WPManager.getInstance().shell)
        message.message = "Line numbers must be 10 digits or less."
        message.open()
    }

    companion object {
        private val localeHandler = getDefault()
        private const val SETTING_ENABLED = "proseBuilder.enabled"
        fun isEnabled(m: Manager): Boolean {
            return m.simpleManager.getModule(ProseBuilder::class.java)!!.toolbarEnabled
        }
    }
}

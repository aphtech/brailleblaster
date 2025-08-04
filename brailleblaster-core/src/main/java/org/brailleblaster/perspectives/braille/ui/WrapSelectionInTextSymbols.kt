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

import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBXUtils
import org.brailleblaster.math.mathml.MathModule
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.XMLNodeCaret
import org.brailleblaster.perspectives.mvc.XMLTextCaret
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.utd.internal.xml.XMLHandler2
import org.brailleblaster.utd.properties.EmphasisType
import org.brailleblaster.util.Notify
import org.brailleblaster.util.Notify.notify
import org.brailleblaster.util.Utils

class WrapSelectionInTextSymbols(
        private val start: String,
        private val end: String,
        private val m: Manager,
        callback: TextWrapCallBack?
) {
    var callback = defaultNoWrap
    fun add() {
        if (m.simpleManager.currentSelection.isTextNoSelection) {
            return
        }
        val startNode = m.simpleManager.currentSelection.start
        val endNode = m.simpleManager.currentSelection.end
        val startParent: Node = startNode.node.parent
        val endParent: Node = endNode.node.parent
        val startIsUneditable = BBXUtils.isUneditable(startNode.node)
        val endIsUneditable = BBXUtils.isUneditable(endNode.node)
        if (startIsUneditable || endIsUneditable) {
            notify(Notify.GENERIC_UNEDITABLE, Notify.ALERT_SHELL_NAME)
            return
        }
        val startIsText = startNode is XMLTextCaret
        val endIsText = endNode is XMLTextCaret
        val startIsMath = MathModule.isMath(startNode.node)
        val endIsMath = MathModule.isMath(endNode.node)
        val singleNode = m.simpleManager.currentSelection.isSingleNode
        if (singleNode && startIsText && endIsText) {
            modifySameNodeText(startNode, endNode)
        } else if (singleNode && startIsMath && endIsMath) {
            modifySameNodeMath(startNode, endNode)
        } else {
            if (startIsMath) {
                modifyMath(startNode, false)
            } else if (startIsText) {
                modifyText(startNode, false)
            } else {
                modifyElement(startNode, false)
            }
            if (endIsMath) {
                modifyMath(endNode, true)
            } else if (endIsText) {
                modifyText(endNode, true)
            } else {
                modifyElement(endNode, true)
            }
        }
        m.simpleManager.dispatchEvent(ModifyEvent(Sender.MATH, true, startParent, endParent))
    }

    private fun modifySameNodeText(start: XMLNodeCaret, end: XMLNodeCaret) {
        val string = start.node.value
        val startIndex = (start as XMLTextCaret).offset
        val endIndex = (end as XMLTextCaret).offset
        val startString = string.substring(0, startIndex)
        val middleString = string.substring(startIndex, endIndex)
        val endString = string.substring(endIndex)
        var parent = start.node.parent
        var index = parent.indexOf(start.node)
        if (!BBX.BLOCK.isA(start.node)) {
            val newBlock = BBX.BLOCK.create(BBX.BLOCK.DEFAULT)
            Utils.insertChildCountSafe(parent, newBlock, index)
            parent = newBlock
        }
        Utils.insertChildCountSafe(parent, Text(startString), index++)
        val newStart = Text(this.start)
        Utils.insertChildCountSafe(parent, newStart, index++)
        callback.wrap(newStart)
        Utils.insertChildCountSafe(parent, Text(middleString), index++)
        val newEnd = Text(this.end)
        Utils.insertChildCountSafe(parent, newEnd, index++)
        callback.wrap(newEnd)
        Utils.insertChildCountSafe(parent, Text(endString), index)
        start.node.detach()
    }

    private fun modifySameNodeMath(node: XMLNodeCaret, endNode: XMLNodeCaret) {
        val startText = Text(start)
        val endText = Text(end)
        val parent = node.node.parent
        var index = parent.indexOf(node.node)
        val parentBlock = BBXUtils.findBlockOrNull(node.node)
        if (parentBlock != null) {
            index = BBXUtils.getIndexInBlock(node.node)
            Utils.insertChildCountSafe(parentBlock, startText, index)
            Utils.insertChildCountSafe(parentBlock, endText, index + 2)
        } else {
            val newStartBlock = BBX.BLOCK.create(BBX.BLOCK.DEFAULT)
            val newEndBlock = BBX.BLOCK.create(BBX.BLOCK.DEFAULT)
            Utils.insertChildCountSafe(parent, newStartBlock, index)
            Utils.insertChildCountSafe(parent, newEndBlock, index + 2)
            Utils.insertChildCountSafe(newStartBlock, startText, 0)
            Utils.insertChildCountSafe(newEndBlock, endText, 0)
        }
        callback.wrap(endText)
        callback.wrap(startText)
    }

    private fun modifyElement(node: XMLNodeCaret, end: Boolean) {
        val newText = Text(if (end) this.end else start)
        val parent = node.node.parent
        val index = parent.indexOf(node.node) + if (end) 1 else 0
        if (BBX.BLOCK.isA(node.node)) {
            Utils.insertChildCountSafe(parent, newText, index)
        } else {
            val newBlock = BBX.BLOCK.create(BBX.BLOCK.DEFAULT)
            Utils.insertChildCountSafe(parent, newBlock, index)
            Utils.insertChildCountSafe(newBlock, newText, 0)
        }
        callback.wrap(newText)
    }

    private fun modifyMath(node: XMLNodeCaret, end: Boolean) {
        val newText = Text(if (end) this.end else start)
        val parent = node.node.parent
        var index = parent.indexOf(node.node) + if (end) 1 else 0
        val parentBlock = BBXUtils.findBlockOrNull(node.node)
        if (parentBlock != null) {
            index = BBXUtils.getIndexInBlock(node.node) + if (end) 1 else 0
            Utils.insertChildCountSafe(parentBlock, newText, index)
        } else {
            val newBlock = BBX.BLOCK.create(BBX.BLOCK.DEFAULT)
            Utils.insertChildCountSafe(parent, newBlock, index)
            Utils.insertChildCountSafe(newBlock, newText, 0)
        }
        callback.wrap(newText)
    }

    private fun modifyText(start: XMLNodeCaret, end: Boolean) {
        val string = start.node.value
        val stringIndex = (start as XMLTextCaret).offset
        val startString = string.substring(0, stringIndex)
        val endString = string.substring(stringIndex)
        var parent = start.node.parent
        var index = parent.indexOf(start.node)
        if (!BBX.BLOCK.isA(parent)) {
            val newBlock = BBX.BLOCK.create(BBX.BLOCK.DEFAULT)
            Utils.insertChildCountSafe(parent, newBlock, if (!end) index else index + 1)
            parent = newBlock
        }
        Utils.insertChildCountSafe(parent, Text(startString), index++)
        val newText = Text(if (end) this.end else this.start)
        Utils.insertChildCountSafe(parent, newText, index++)
        callback.wrap(newText)
        Utils.insertChildCountSafe(parent, Text(endString), index)
        start.node.detach()
    }

    fun interface TextWrapCallBack {
        fun wrap(textNode: Node): Node
    }

    init {
        if (callback != null) {
            this.callback = callback
        }
    }

    companion object {
        var defaultNoWrap: TextWrapCallBack = TextWrapCallBack { n: Node -> n }
        var direct: TextWrapCallBack = TextWrapCallBack { n: Node ->
            XMLHandler2.wrapNodeWithElement(
                n,
                BBX.INLINE.EMPHASIS.create(EmphasisType.NO_TRANSLATE)
            )
            n.parent
        }
    }
}
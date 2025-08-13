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
package org.brailleblaster.perspectives.braille.stylers

import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.findBlock
import org.brailleblaster.exceptions.EditingException
import org.brailleblaster.math.mathml.MathModule
import org.brailleblaster.math.numberLine.NumberLine.Companion.getNumberLineParent
import org.brailleblaster.math.numberLine.NumberLine.Companion.isNumberLine
import org.brailleblaster.math.spatial.Matrix
import org.brailleblaster.math.spatial.SpatialMathUtils.getSpatialPageParent
import org.brailleblaster.math.spatial.SpatialMathUtils.isSpatialMathPage
import org.brailleblaster.math.template.Template.Companion.getTemplateParent
import org.brailleblaster.math.template.Template.Companion.isTemplate
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.BoxLineTextMapElement
import org.brailleblaster.perspectives.braille.mapping.elements.LineBreakElement
import org.brailleblaster.perspectives.braille.mapping.elements.WhiteSpaceElement
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.messages.WhitespaceMessage
import org.brailleblaster.perspectives.mvc.XMLTextCaret
import org.brailleblaster.perspectives.mvc.events.XMLCaretEvent
import org.brailleblaster.util.WhitespaceUtils.appendLineBreakElement
import org.brailleblaster.util.WhitespaceUtils.prependLineBreakElement
import org.brailleblaster.util.WhitespaceUtils.removeLineBreakElements

class WhitespaceTransformer(manager: Manager) : Handler(manager, manager.viewInitializer, manager.mapList) {
    fun transformWhiteSpace(message: WhitespaceMessage) {
        //if(isInline(list.getCurrent()))
        //	transformInlineText((WhiteSpaceElement)list.get(message.getIndex()), message.getNewText());
        //else
        transformWhitespace(list[message.index] as WhiteSpaceElement, message.newText)
    }

    fun transformWhiteSpace(wse: WhiteSpaceElement, node: Node) {
        val index = list.indexOf(wse)

        var curElement = list.findPreviousNonWhitespace(index)
        val nextElement = list.findNextNonWhitespace(index)
        var atBeginning = false
        var betweenSpatialMath = false
        if (curElement == null && nextElement == null) throw NullPointerException("Empty maplist?")
        //If we're at the beginning of the map list, use nextElement as the current element
        if (curElement == null) {
            curElement = nextElement
            atBeginning = true
        }
        if (curElement!!.isSpatialMath && nextElement != null && nextElement.isSpatialMath) {
            betweenSpatialMath = true
        }
        //Calculate how many blank lines come before and after the new element
        var blankLinesBefore = if (atBeginning) index else index - list.indexOf(curElement)
        var blankLinesAfter = if (nextElement == null) -1 else list.indexOf(nextElement) - index

        if (wse is LineBreakElement && !atBeginning) {
            blankLinesBefore-- //An extra line break element is added to the previous tme
        }

        if (nextElement != null) { //nextElement is null at the end of the document
            //Remove all existing line breaks between the two elements
            try {
                removeLineBreakElements(
                    list,
                    if (atBeginning) 0 else list.indexOf(curElement),
                    list.indexOf(nextElement)
                )
            } catch (e: RuntimeException) {
                throw EditingException(
                    "Error removing lines breaks from index " + list.indexOf(curElement) + " to index "
                            + list.indexOf(nextElement) + ". AtBeginning: " + atBeginning, e
                )
            }
        }

        var targetElement: Element?
        val isSidebar = curElement is BoxLineTextMapElement
        var offset = 1
        if (isSidebar) {
            if ((curElement as BoxLineTextMapElement?)!!.isStartSeparator) {
                //Text should be inserted after BoxLineTME node
                targetElement = curElement.node as Element
            } else {
                //Text should be inserted after box container
                targetElement = curElement.nodeParent
                //Increase offset to put text after the container and bottom boxline
                offset = 2
            }
        } else if (Manager.getTableParent(curElement.nodeParent) != null) {
            targetElement = if (!atBeginning) {
                //If we're inside a table, we want the new node to go after the table copy, if it exists
                if (Manager.getTableBrlCopy(curElement.nodeParent) != null) {
                    Manager.getTableBrlCopy(curElement.nodeParent)
                } else {
                    Manager.getTableParent(curElement.nodeParent)
                }
            } else {
                Manager.getTableParent(curElement.nodeParent)
            }
        } else if (isSpatialMathPage(curElement.node)) {
            targetElement = getSpatialPageParent(curElement.node)
        } else if (isNumberLine(curElement.node)) {
            targetElement = getNumberLineParent(curElement.node)
        } else if (Matrix.isMatrix(curElement.node)) {
            targetElement = Matrix.getMatrixParent(curElement.node)
        } else if (isTemplate(curElement.node)) {
            targetElement = getTemplateParent(curElement.node)
        } else {
            targetElement = curElement.node.findBlock()
            if (targetElement == null) {
                //Catch for things like top box lines
                targetElement = findParent(curElement)
                if (targetElement === targetElement.document.rootElement) {
                    throw EditingException("CurElement has no block or translation block ancestor: " + curElement.node)
                }
            }
        }

        val parent = targetElement!!.parent
        if (MathModule.isSpatialMath(targetElement) && betweenSpatialMath) {
            offset = 0
        }
        val insertIndex = if (atBeginning) parent.indexOf(targetElement) else parent.indexOf(targetElement) + offset
        //		Element result = manager.getDocument().insertElement((Element)parent, insertIndex,t);
        parent.insertChild(node, insertIndex)

        //Only add line breaks if there is a blank line between the new text and the previous tme
        //(Unless we're at the beginning of the document. In that case the math changes a little)
        //Otherwise don't add anything and rely on automatic formatting
        if (blankLinesBefore > 1 || (atBeginning && blankLinesBefore > 0)) {
            while (blankLinesBefore > 0) {
                if (atBeginning) {
                    try {
                        prependLineBreakElement(node)
                    } catch (e: RuntimeException) {
                        throw EditingException("Error prepending line breaks to node " + node.toXML(), e)
                    }
                } else {
                    try {
                        appendLineBreakElement(curElement.node)
                    } catch (e: RuntimeException) {
                        throw EditingException("Error appending line breaks to node " + node.toXML(), e)
                    }
                }
                blankLinesBefore--
            }
        }
        if (blankLinesAfter > 1) {
            while (blankLinesAfter > 0) {
                try {
                    appendLineBreakElement(node)
                } catch (e: RuntimeException) {
                    throw EditingException("Error appending line breaks to node " + node.toXML(), e)
                }
                blankLinesAfter--
            }
        }
        reformat(node, true)
    }

    private fun transformWhitespace(wse: WhiteSpaceElement, text: String) {
        val index = list.indexOf(wse)

        val curElement = list.findPreviousNonWhitespace(index)
        val nextElement = list.findNextNonWhitespace(index)

        //Elements consisting only of whitespace are ignored by UTD, so stop here
        if (text.trim { it <= ' ' }.isEmpty()) {
            if (curElement != null) {
                reformat(curElement.node, false)
            } else {
                reformat(nextElement.node, false)
            }
        } else {
            var style: String? = "Body Text"
            if (curElement != null) {
                val block = curElement.block
                val curStyle = block?.getAttributeValue("utd-style")
                if (curStyle != null && !STYLES_NOT_TO_BE_RETAINED.contains(curStyle)) {
                    style = curStyle
                }
            }
            val newElement = BBX.BLOCK.STYLE.create(style)
            val textNode = Text(text)
            newElement.appendChild(textNode)
            transformWhiteSpace(wse, newElement)
            if (curElement != null && curElement.isSpatialMath) {
                manager.simpleManager.dispatchEvent(
                    XMLCaretEvent(
                        Sender.NO_SENDER,
                        XMLTextCaret(textNode, 0)
                    )
                )
            }
        }
    }


    companion object {
        val STYLES_NOT_TO_BE_RETAINED: Set<String> = setOf("Spatial Math")
    }
}

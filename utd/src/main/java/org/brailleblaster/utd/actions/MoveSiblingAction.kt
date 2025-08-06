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
package org.brailleblaster.utd.actions

import nu.xom.Attribute
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.utd.ITranslationEngine
import org.brailleblaster.utd.TextSpan
import org.brailleblaster.utd.utils.TextTranslator

/**
 * This action will move the specified node after its sibling to the right.
 * For example, `21`<linegroup>This is a line.</linegroup>
 * will result to <linegroup>This is a line.</linegroup>`21`
 */
class MoveSiblingAction : IBlockAction {
    private val action = GenericBlockAction()
    override fun applyTo(node: Node, context: ITranslationEngine): List<TextSpan> {
        return if (node is Element) {
            processNode(node, context)
        } else emptyList()
    }

    private fun processNode(node: Element, engine: ITranslationEngine): List<TextSpan> {
        val processedInput: MutableList<TextSpan> = ArrayList()
        val parent = node.parent
        val nodeCopy: Node = node.copy()
        //Get the sibling of the node to the right
        if (node.parent == null) {
            return emptyList()
        }

        //If it's the only child, move it to the sibling of the parent if any
        if (parent.childCount == 1) {
            val p2 = parent.parent
            if (p2.indexOf(parent) + 1 < p2.childCount) {
                val sibling = p2.getChild(p2.indexOf(parent) + 1) as Element
                sibling.appendChild(nodeCopy)
                //Add an attribute to the <line>
                sibling.addAttribute(Attribute("linenum", TextTranslator.translateText(node.value, engine)))
                parent.detach()
                processedInput.addAll(action.applyTo(sibling.getChild(0), engine))
            }
            return emptyList()
        }
        val line = parent as Element
        try {
            node.value.toInt()
            line.addAttribute(Attribute("linenum", TextTranslator.translateText(node.value, engine)))
        } catch (_: NumberFormatException) {
            line.addAttribute(Attribute("lineletter", TextTranslator.translateText(node.value, engine)))
        }

        //If it's the last child, don't do anything.
        if (parent.indexOf(node) + 1 >= parent.getChildCount()) {
            return emptyList()
        }
        val siblingIndex = parent.indexOf(node) + 1
        val nodeSibling = parent.getChild(siblingIndex)

        //If it's a line letter, then there could already be a line number present
        //Check for a line number sibling, make sure that still returns empty
        if (nodeSibling is Element && node.attributeCount > 0 && nodeSibling.getAttribute(
                "type",
                node.getAttribute(0).namespaceURI
            ) != null && nodeSibling.getAttributeValue(
                "type",
                node.getAttribute(0).namespaceURI
            ) == "POEM_LINE_NUMBER"
        ) {
            return emptyList()
        }
        parent.insertChild(nodeCopy, siblingIndex + 1)

        //removes extra space in front of sibling, if any
        if (nodeSibling is Text) {
            var lineValue = nodeSibling.value
            if (lineValue.isNotEmpty() && lineValue.first().isWhitespace()) {
                lineValue = lineValue.substring(1)
            }
            val newText = Text(lineValue)
            parent.replaceChild(nodeSibling, newText)
        }

        //Only translates the line itself
        processedInput.addAll(action.applyTo(parent.getChild(siblingIndex), engine))
        parent.removeChild(node)
        return processedInput
    }
}
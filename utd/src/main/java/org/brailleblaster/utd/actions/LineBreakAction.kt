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

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.utd.ITranslationEngine
import org.brailleblaster.utd.TextSpan
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.getAssociatedBrlElement
import org.brailleblaster.utd.utils.getTextChild

@Suppress("UNUSED")
class LineBreakAction : IBlockAction {
    // In order to separate Braille we need to return an empty translated TextSpan for the node.
    // The below method creates such a TextSpan object.
    private fun createTextSpanForLineBreak(node: Node): TextSpan {
        val span = TextSpan(node, "")
        span.isTranslated = true
        return span
    }

    override fun applyTo(node: Node, context: ITranslationEngine): List<TextSpan> {
        return if (node is Element) {
            processNode(node, context)
        } else listOf(createTextSpanForLineBreak(node))
    }

    private fun processNode(node: Element, engine: ITranslationEngine): List<TextSpan> {
        //If the node already has a brl for a child, then return
        val textChild = getTextChild(node)
        if (getAssociatedBrlElement(textChild) != null) {
            return listOf(createTextSpanForLineBreak(node))
        }
        val brl = UTDElements.BRL.create()
        //brl.addAttribute(new Attribute("linesBefore", "1")); not needed, made in bb
        node.appendChild(brl)
        return listOf(createTextSpanForLineBreak(node))
    }
}
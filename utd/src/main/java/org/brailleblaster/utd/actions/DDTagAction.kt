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

class DDTagAction : GenericAction() {
    override fun applyTo(node: Node, context: ITranslationEngine): List<TextSpan> {
        if (node is Element) {

            val value = node.value
            if (value.isEmpty() || value[value.length - 1] == ' ') {
                return super.applyTo(node, context)
            }

            val firstText = findFirstTextAfter(node)
            if (firstText != null) {
                val text = firstText.value
                if (text[text.length - 1] == ' ') {
                    return super.applyTo(node, context)
                }
                val attribute = node.getAttribute("spaced")
                if (attribute == null) {
                    node.addAttribute(Attribute("spaced", "true"))
                    val newText: Node = Text(firstText.value + " ")
                    firstText.parent.replaceChild(firstText, newText)
                }
            }
        }
        return super.applyTo(node, context)
    }


    /*
	 * 	This should find the first text node after the element
	 */
    private fun findFirstTextAfter(dd: Element): Node? {
        if (dd.query("descendant::text()").size() > 0) {
            return dd.query("descendant::text()")[0]
        }

        return null
    }
}

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
import org.brailleblaster.utd.ITranslationEngine
import org.brailleblaster.utd.TextSpan
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.containsBrl

class TabAction : IBlockAction {
    override fun applyTo(node: Node, context: ITranslationEngine): List<TextSpan> {
        return if (node is Element) {
            processPageNode(node, context)
        } else emptyList()
    }

    private fun processPageNode(node: Element, engine: ITranslationEngine?): List<TextSpan> {
        //If the node already has a brl for a child, then return
        if (node.containsBrl()) {
            return emptyList()
        }
        val brl = UTDElements.BRL.create()
        brl.addAttribute(Attribute("tabValue", node.getAttributeValue("tabValue")))
        node.appendChild(brl)
        return emptyList()
    }
}
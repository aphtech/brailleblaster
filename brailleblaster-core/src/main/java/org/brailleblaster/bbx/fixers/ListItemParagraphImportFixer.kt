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
package org.brailleblaster.bbx.fixers

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.bbx.BBX
import org.brailleblaster.utd.internal.xml.XMLHandler

@Suppress("UNUSED")
class ListItemParagraphImportFixer : AbstractFixer() {
    override fun fix(matchedNode: Node) {
        BBX.BLOCK.assertIsA(matchedNode)
        val block = matchedNode as Element
        val listItem = XMLHandler.ancestorVisitorElement(
            matchedNode.parent
        ) { node: Element -> BBX.BLOCK.LIST_ITEM.isA(node) }
        BBX.transform(block, BBX.BLOCK.LIST_ITEM)
        //Do not incriment nested paragraphs per Rez, they are essentially siblings
        BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL[block] = BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL[listItem]
        NodeTreeSplitter.split(listItem, block)
    }
}
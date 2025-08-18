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
import org.brailleblaster.utd.NamespaceMap
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler2
import org.brailleblaster.utd.matchers.INodeMatcher
import org.brailleblaster.utils.xom.childNodes

@Suppress("UNUSED")
class NestedListImportFixer : AbstractFixer() {
    override fun fix(matchedNode: Node) {
        BBX.CONTAINER.LIST.assertIsA(matchedNode)
        val nestedList = matchedNode as Element

        //auto incriment nested list levels
        for (curChild in nestedList.childNodes) {
            if (BBX.BLOCK.LIST_ITEM.isA(curChild)) {
                BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL[curChild as Element] = { level: Int -> level + 1 }
            }
        }

        //A seperate Fixer should of put us under the list
        val parentList = nestedList.parent as Element
        BBX.CONTAINER.LIST.assertIsA(parentList)
        XMLHandler2.unwrapElement(nestedList)
    }

    @Suppress("UNUSED")
    class NoDescendantListMatcher : INodeMatcher {
        override fun isMatch(node: Node, namespaces: NamespaceMap): Boolean {
            if (!BBX.CONTAINER.LIST.isA(node)) {
                return false
            }
            val nestedList = node as Element
            return FastXPath.descendant(nestedList)
                .any { BBX.CONTAINER.LIST.isA(it) }
        }
    }
}
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
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.utd.NamespaceMap
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.matchers.INodeMatcher

@Suppress("UNUSED")
class DefinitionTermImportFixer : AbstractFixer() {
    override fun fix(matchedNode: Node) {
        BBX.SPAN.DEFINITION_TERM.assertIsA(matchedNode)
        val definitionDescNode = nextUsableListItem(matchedNode)
            ?: throw NodeException("matched but no definition to move term inside of", matchedNode)
        BBX.BLOCK.LIST_ITEM.assertIsA(definitionDescNode)
        matchedNode.detach()
        definitionDescNode.insertChild(matchedNode, 0)

        //Note: Adding a space after the term has been moved to DDTagAction
    }

    @Suppress("UNUSED")
    class HasFollowingListItemMatcher : INodeMatcher {
        override fun isMatch(node: Node, namespaces: NamespaceMap): Boolean {
            return nextUsableListItem(node) != null
        }
    }

    companion object {
        /**
         * skip spaces that might of entered the document
         * @param node
         * @return
         */
        fun nextUsableListItem(node: Node): Element? {
            var curNode = node
            while (true) {
                curNode = XMLHandler.nextSiblingNode(curNode) ?: return null
                return if (curNode is Text) {
                    if (curNode.value.isNotBlank()) {
                        null
                    } else {
                        continue
                    }
                } else if (BBX.BLOCK.LIST_ITEM.isA(curNode)) {
                    curNode as Element?
                } else {
                    null
                }
            }
        }
    }
}
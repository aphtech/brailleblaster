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

@Suppress("UNUSED")
class ImageGroupToSpanImportFixer : AbstractFixer() {
    override fun fix(matchedNode: Node) {
        BBX.CONTAINER.IMAGE.assertIsA(matchedNode)
        getDescendantBlock(matchedNode)
            .firstOrNull()
            ?.let { elem: Element ->
                XMLHandler2.unwrapElement(
                    elem
                )
            }
        getDescendantContainer(matchedNode)
            .firstOrNull()
            ?.let { elem: Element ->
                XMLHandler2.unwrapElement(
                    elem
                )
            }
        BBX.transform(matchedNode as Element, BBX.SPAN.IMAGE)
    }

    @Suppress("UNUSED")
    class SingleDescendantBlockMatcher : INodeMatcher {
        override fun isMatch(node: Node, namespaces: NamespaceMap): Boolean {
            return node is Element && getDescendantBlock(node).count() <= 1 && getDescendantContainer(node).count() <= 1
        }
    }

    companion object {
        private fun getDescendantBlock(node: Node): List<Element> {
            return FastXPath.descendant(node)
                .filterIsInstance<Element>()
                .filter { BBX.BLOCK.isA(it) }
        }

        private fun getDescendantContainer(node: Node): List<Element> {
            return FastXPath.descendant(node)
                .filterIsInstance<Element>()
                .filter { BBX.CONTAINER.isA(it) }
        }
    }
}
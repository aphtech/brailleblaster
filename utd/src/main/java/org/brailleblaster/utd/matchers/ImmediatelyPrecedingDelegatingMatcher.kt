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
package org.brailleblaster.utd.matchers

import nu.xom.Node
import org.brailleblaster.utd.NamespaceMap

class ImmediatelyPrecedingDelegatingMatcher : DelegatingMatcher() {
    var ignoreMatcher: INodeMatcher? = null
    override fun isMatch(node: Node, namespaces: NamespaceMap): Boolean {
        return matcher == null || matchParent(node, namespaces)
    }

    private fun matchParent(node: Node, namespaces: NamespaceMap): Boolean {
        val parent = node.parent ?: return false
        var nodeIndex = parent.indexOf(node)
        while (nodeIndex > 0) {
            nodeIndex--
            val tmpNode = parent.getChild(nodeIndex)
            if (ignoreMatcher == null || !ignoreMatcher!!.isMatch(tmpNode, namespaces)) {
                return matchLastDescendants(tmpNode, namespaces)
            }
        }
        return matchParent(parent, namespaces)
    }

    private fun matchLastDescendants(node: Node, namespaces: NamespaceMap): Boolean {
        return matcher?.let { m ->
            var result: Boolean
            var testNode = node
            var childCount: Int
            do {
                result = m.isMatch(testNode, namespaces)
                if (result) {
                    break
                }
                childCount = testNode.childCount
                while (childCount > 0) {
                    childCount--
                    val tempNode = testNode.getChild(childCount)
                    if (ignoreMatcher == null || !ignoreMatcher!!.isMatch(tempNode, namespaces)) {
                        testNode = tempNode
                        // Set childCount to 1 to allow looping in the outer loop
                        childCount = 1
                        break
                    }
                }
            } while(childCount > 0)
            result
        } != false
    }
}

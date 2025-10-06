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

/**
 * Fast way to see if an element is immediately preceeded as
 * preceding::element[last()] can be very slow
 */
class ImmediatelyPrecedingMatcher : NodeAncestorMatcher() {
    override fun isMatch(node: Node, namespaces: NamespaceMap): Boolean {
        return if (selfMatcher?.isMatch(node, namespaces) != false) {
            matchParent(node, namespaces)
        } else false
    }

    private fun matchParent(node: Node, namespaces: NamespaceMap): Boolean {
        val parent = node.parent ?: return false
        val nodeIndex = parent.indexOf(node)
        return if (nodeIndex > 0) {
            //Something is right before this
            parentMatcher?.isMatch(parent.getChild(nodeIndex - 1), namespaces) != false
        } else {
            //First element, need to up a level
            matchParent(parent, namespaces)
        }
    }
}

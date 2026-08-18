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

import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.utd.NamespaceMap

/**
 * This matcher determines if the node only wraps an element meeting the criteria of the delegate
 * matcher.
 */
class WrapperMatcher : HasChildDelegatingMatcher() {
    override fun isMatch(node: Node, namespaces: NamespaceMap): Boolean {
        // May be a case where there are things like comments or other ignorable child nodes
        var contentChildren = 0
        var i = 0
        while (i < node.childCount && contentChildren < 2) {
            val child = node.getChild(i)
            if (child is Element || child is Text) {
                contentChildren++
            }
            i++
        }
        return contentChildren == 1 && super.isMatch(node, namespaces)
    }
}
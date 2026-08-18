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
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.matchers.INodeMatcher

@Suppress("UNUSED")
class PoemLineGroupMatcher : INodeMatcher {
    override fun isMatch(node: Node, namespaces: NamespaceMap): Boolean {
        return BBX.CONTAINER.LIST.isA(node) && (BBX.CONTAINER.LIST.ATTRIB_LIST_TYPE[node as Element] == BBX.ListType.POEM_LINE_GROUP
                && XMLHandler.ancestorVisitorElement(
            node.parent
        ) { curAncestor: Element? ->
            BBX.CONTAINER.LIST.isA(curAncestor)
                    && BBX.CONTAINER.LIST.ATTRIB_LIST_TYPE[curAncestor] == BBX.ListType.POEM
        } != null)
    }
}
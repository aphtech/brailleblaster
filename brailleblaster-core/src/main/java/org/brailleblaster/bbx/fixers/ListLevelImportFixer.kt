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
import org.brailleblaster.bbx.BookToBBXConverter
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.XMLHandler2
import org.brailleblaster.utils.xom.childNodes
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.math.max

@Suppress("UNUSED")
class ListLevelImportFixer : AbstractFixer() {
    override fun fix(matchedNode: Node) {
        BBX.CONTAINER.LIST.assertIsA(matchedNode)
        val listContainer = matchedNode as Element
        val childrenToProcess: Deque<Node> = ArrayDeque()
        childrenToProcess.addAll(listContainer.childNodes)
        var maxLevel = 0
        while (!childrenToProcess.isEmpty()) {
            val curChildNode = childrenToProcess.pop() as? Element ?: continue
            if (BBX.CONTAINER.isA(curChildNode)) {
                childrenToProcess.addAll(curChildNode.childNodes)
            } else if (BBX.BLOCK.isA(curChildNode)) {
                var curLevel: Int
                if (BBX.BLOCK.LIST_ITEM.isA(curChildNode)) {
                    curLevel = BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL[curChildNode]
                } else if (BBX.CONTAINER.LIST.ATTRIB_LIST_TYPE.has(curChildNode)
                    && BBX.CONTAINER.LIST.ATTRIB_LIST_TYPE[curChildNode] == BBX.ListType.POEM_LINE_GROUP
                ) {
                    curLevel = BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL[curChildNode]
                    BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.detach(curChildNode)
                } else {
                    if (BookToBBXConverter.STRICT_MODE) {
                        throw NodeException("Unhandled element", curChildNode)
                    } else {
                        log.warn("Unhandled element {}",
                            XMLHandler2.toXMLSimple(curChildNode)
                        )
                    }
                    curLevel = 0
                }
                maxLevel = max(maxLevel, curLevel)
            } else {
                if (BookToBBXConverter.STRICT_MODE) {
                    throw NodeException("Unexpected list child", curChildNode)
                } else {
                    log.warn("Unexpected list child {}",
                        XMLHandler2.toXMLSimple(curChildNode)
                    )
                }
            }
        }
        BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL[listContainer] = maxLevel
    }

    companion object {
        private val log = LoggerFactory.getLogger(ListLevelImportFixer::class.java)
    }
}
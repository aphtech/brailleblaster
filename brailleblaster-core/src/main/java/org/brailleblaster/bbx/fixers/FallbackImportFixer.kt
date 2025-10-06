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
import org.slf4j.LoggerFactory

@Suppress("UNUSED")
class FallbackImportFixer : AbstractFixer() {
    override fun fix(matchedNode: Node) {
        if (BBX.CONTAINER.isA(matchedNode)) {
            val ancestorBlock = XMLHandler.ancestorVisitorElement(
                matchedNode.parent
            ) { node: Element -> BBX.BLOCK.isA(node) }!!
            log.debug(
                "Splitting {} out of {}",
                XMLHandler.toXMLStartTag(matchedNode as Element),
                XMLHandler.toXMLStartTag(ancestorBlock)
            )
            NodeTreeSplitter.split(ancestorBlock, matchedNode)
        } else if (BBX.BLOCK.isA(matchedNode)) {
            val ancestorBlock = XMLHandler.ancestorVisitorElement(
                matchedNode.parent
            ) { node: Element? -> BBX.BLOCK.isA(node) }!!
            log.debug(
                "Splitting {} out of {}",
                XMLHandler.toXMLStartTag(matchedNode as Element),
                XMLHandler.toXMLStartTag(ancestorBlock)
            )
            NodeTreeSplitter.split(ancestorBlock, matchedNode)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(FallbackImportFixer::class.java)
    }
}
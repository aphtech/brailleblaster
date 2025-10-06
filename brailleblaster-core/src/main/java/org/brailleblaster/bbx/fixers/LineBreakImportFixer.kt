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
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.slf4j.LoggerFactory

open class LineBreakImportFixer : AbstractFixer() {
    override fun fix(matchedNode: Node) {
        val brElement = matchedNode as Element
        log.trace("found br {}",
            XMLHandler.toXMLSimple(brElement)
        )
        val oldBlock = XMLHandler.ancestorVisitorElement(
            brElement
        ) { node: Element? -> BBX.BLOCK.isA(node) } ?: throw NodeException("oldBlock", matchedNode)
        BBX.BLOCK.assertIsA(oldBlock)
        val newBlock = NodeTreeSplitter.split(oldBlock, brElement)
        postSplit(oldBlock, newBlock)
        brElement.detach()
    }

    protected open fun postSplit(oldBlock: Element?, newBlock: Element?) {}

    companion object {
        private val log = LoggerFactory.getLogger(LineBreakImportFixer::class.java)
    }
}
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
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler

/**
 * Issue #5390: Treat linebreaks in Definition lists specially
 */
@Suppress("UNUSED")
class DefinitionListLineBreakImportFixer : LineBreakImportFixer() {
    override fun postSplit(oldBlock: Element?, newBlock: Element?) {
        // LineBreak may of been put at start or end of block, causing newly empty block to be detached
        if (oldBlock != null && oldBlock.document == null || newBlock != null && newBlock.document == null) {
            return
        }
        if (newBlock != null && oldBlock != null && FastXPath.descendant(oldBlock)
                .filterIsInstance<Text>()
                .none { curNode ->
                    XMLHandler.ancestorElementNot(curNode) { node: Element? ->
                        BBX.SPAN.DEFINITION_TERM.isA(
                            node
                        )
                    }
                }
        ) {
            //No need to move empty br
            newBlock.detach()
            oldBlock.appendChild(newBlock)
            XMLHandler.unwrapElement(newBlock)
        } else if (newBlock != null) {
            val incriment = if (BBX.FixerMarker.ATTRIB_FIXER_MARKER.has(newBlock)
                && BBX.FixerMarker.ATTRIB_FIXER_MARKER[newBlock] == BBX.FixerMarker.DEFINITION_LINE_BREAK_SPLIT
            ) 0 else 1
            BBX.BLOCK.LIST_ITEM.assertIsA(newBlock)
            BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL[newBlock] = { curValue: Int -> curValue + incriment }
            BBX.FixerMarker.ATTRIB_FIXER_MARKER[newBlock] = BBX.FixerMarker.DEFINITION_LINE_BREAK_SPLIT
        }
    }
}
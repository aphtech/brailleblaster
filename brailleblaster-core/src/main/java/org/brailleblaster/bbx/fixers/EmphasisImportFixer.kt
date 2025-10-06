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

import nu.xom.Comment
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BookToBBXConverter
import org.brailleblaster.utd.NamespaceMap
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.matchers.INodeMatcher
import org.brailleblaster.utd.properties.EmphasisType
import org.brailleblaster.utils.xom.childNodes
import org.slf4j.LoggerFactory
import java.util.*

@Suppress("UNUSED")
class EmphasisImportFixer : AbstractFixer() {
    override fun fix(matchedNode: Node) {
        BBX.INLINE.EMPHASIS.assertIsA(matchedNode)
        val emphasis = matchedNode as Element
        recursiveApplyEmphasis(emphasis, emphasis)
        XMLHandler.unwrapElement(emphasis)
    }

    private fun recursiveApplyEmphasis(emphasisBeingUnwrapped: Element, curNode: Node) {
        val emphasisBits = BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS[emphasisBeingUnwrapped]
        if (curNode is Text) {
            val textParent = curNode.parent as Element
            if (BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS.has(textParent) && textParent !== emphasisBeingUnwrapped) {
                BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS[textParent] = { types: EnumSet<EmphasisType> ->
                    types.addAll(emphasisBits)
                    types
                }
            } else {
                val newInline = BBX.INLINE.EMPHASIS.create(emphasisBits)
                XMLHandler.wrapNodeWithElement(
                    curNode,
                    newInline
                )
            }
        } else if (curNode is Element) {
            for (curChild in curNode.childNodes) {
                if (BBX.INLINE.MATHML.isA(curChild)) {
                    //Only set on the root INLINE wrapper
                    BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS[curChild as Element] = emphasisBits
                } else {
                    recursiveApplyEmphasis(emphasisBeingUnwrapped, curChild)
                }
            }
        } else if (curNode is Comment) {
            //do nothing
        } else {
            if (BookToBBXConverter.STRICT_MODE) {
                throw NodeException("Unhandled node", curNode)
            } else {
                log.warn("Unhandled node {}",
                    XMLHandler.toXMLSimple(curNode)
                )
            }
        }
    }

    @Suppress("UNUSED")
    class HasElementChildrenMatcher : INodeMatcher {
        override fun isMatch(node: Node, namespaces: NamespaceMap): Boolean {
            return if (node !is Element) {
                false
            } else node.childElements.size() != 0
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(EmphasisImportFixer::class.java)
    }
}
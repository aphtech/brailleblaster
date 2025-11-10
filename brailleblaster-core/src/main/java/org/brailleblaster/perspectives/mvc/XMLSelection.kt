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
package org.brailleblaster.perspectives.mvc

import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.utils.TableUtils.isTableCopy
import org.brailleblaster.utils.xml.UTD_NS
import org.brailleblaster.utils.xom.childNodes

class XMLSelection(@JvmField val start: XMLNodeCaret, @JvmField val end: XMLNodeCaret) {
    val isSingleNode: Boolean
        get() =//extra validation to catch bugs
            start.node === end.node
    val isTextNoSelection: Boolean
        get() {
            if (!isSingleNode) {
                return false
            }
            return if (!(start is XMLTextCaret && end is XMLTextCaret)) {
                false
            } else start.offset == end.offset
        }
    val selectedBlocks: MutableList<Element>
        /**
         * Returns a list of all blocks between start.node() and end.node().
         * If either start.node() or end.node() is a text element, their ancestor
         * block is added to the returned list. If either is a container/section,
         * all of their descendant blocks are added to the returned list.
         */
        get() {
            val returnList: MutableList<Element> = ArrayList()
            var startBlock = start.node
            if (startBlock is Text || BBX.SPAN.isA(startBlock) || BBX.INLINE.isA(startBlock)
                || MathModuleUtils.isMath(startBlock) || BBX.BLOCK.isA(startBlock)
            ) {
                while (startBlock !is Element || !BBX.BLOCK.isA(startBlock) && !BBX.CONTAINER.isA(startBlock) && !BBX.SECTION.isA(
                        startBlock
                    )
                ) {
                    startBlock = startBlock.parent
                }
                returnList.add(startBlock)
            }
            if (BBX.CONTAINER.isA(startBlock) || BBX.SECTION.isA(startBlock)) {
                //Add all descendant blocks to the list
                FastXPath.descendant(startBlock)
                    .filterIsInstance<Element>()
                    .filter { node -> BBX.BLOCK.isA(node) }
                    .forEach { e: Element -> returnList.add(e) }
                if (returnList.isNotEmpty()) startBlock = returnList[0]
            }
            if (start.node === end.node) {
                return returnList
            }

            //Convert end.node to a block
            var endBlock = end.node
            if (BBX.CONTAINER.isA(endBlock) || BBX.SECTION.isA(endBlock)) { //Mark the final block in its descendants as the ending block
                val childBlocks = FastXPath.descendant(endBlock)
                    .filter { node -> BBX.BLOCK.isA(node) }
                if (childBlocks.isEmpty()) {
                    for (next in FastXPath.precedingAndSelf(endBlock)) {
                        if (next === startBlock) {
                            returnList.add(startBlock as Element)
                            return returnList
                        }
                        if (BBX.BLOCK.isA(next)) {
                            endBlock = next
                            break
                        }
                    }
                } else {
                    endBlock = childBlocks[childBlocks.size - 1]
                }
            } else if (!BBX.BLOCK.isA(endBlock)) {
                while (!BBX.BLOCK.isA(endBlock)) {
                    endBlock = endBlock.parent
                }
            }

            //Iterate through the xml from the starting block to the ending block,
            //adding all blocks found along the way to the return list.
            var curBlock: Node = startBlock
            while (curBlock !== endBlock) {
                curBlock = XMLHandler.followingVisitor(curBlock) { node: Node -> BBX.BLOCK.isA(node) } ?: break
                returnList.add(curBlock as Element)
            }
            return returnList
        }
    val isValidTreeSelection: List<Node>?
        get() = isValidTreeSelection(start.node, end.node)

    companion object {
        /**
         * Figure out if complete tree is effectively selected. Eg in the below XML a
         * selection (either the element or the text node)
         * from paragraph to first is false but from paragraph to second is true:
         *
         * <pre> `<p>paragraph</p>
         * <list>
         * <li>first</li>
         * <li>second</li>
         * </list>
        ` *  </pre>
         *
         * @return Sibling elements under common parent for start and end, or null if invalid
         */
		@JvmStatic
		fun isValidTreeSelection(start: Node, end: Node): List<Node>? {
            val commonParent = XMLHandler.findCommonParent(
                listOf(
                    XMLHandler.nodeToElementOrParentOrDocRoot(
                        start
                    )!!,
                    XMLHandler.nodeToElementOrParentOrDocRoot(
                        end
                    )!!
                )
            )

            //Start
            val startElement: Element
            if (start is Text) {
                startElement = start.parent as Element
                //Initial check if start is first in it's own parent
                if (FastXPath.descendant(startElement)
                        .filterIsInstance<Text>()
                        .first { node -> (node.parent as Element).namespaceURI != UTD_NS } !== start
                ) {
                    return null
                }
            } else {
                startElement = start as Element
            }
            var startCursor = startElement
            if (startElement !== commonParent) {
                for (ancestor in  /*first is cursors parent*/FastXPath.ancestor(startCursor)) {
                    if (ancestor === commonParent) {
                        break
                    }
                    for (ancestorChild in ancestor.childNodes) {
                        if (ancestorChild is Text) {
                            //not first
                            return null
                        } else if (ancestorChild is Element) {
                            return if (ancestorChild === startCursor) {
                                break
                            } else {
                                //not first
                                null
                            }
                        }
                    }
                    startCursor = ancestor
                }
            }
            //End
            val endElement: Element
            if (end is Text) {
                endElement = end.parent as Element
                //Initial check if start is first in it's own parent
                if (FastXPath.descendant(endElement)
                        .filterIsInstance<Text>()
                        .lastOrNull { node -> (node.parent as Element).namespaceURI != UTD_NS } !== end
                ) {
                    return null
                }
            } else {
                endElement = end as Element
            }
            var endCursor = endElement
            for (ancestor in  /*first is cursors parent*/FastXPath.ancestor(endCursor)) {
                if (ancestor === commonParent) {
                    break
                }
                for (ancestorChild in ancestor.childNodes.reversed()) {
                    if (ancestorChild is Text) {
                        //not first
                        return null
                    } else if (ancestorChild is Element) {
                        return if (ancestorChild === endCursor) {
                            break
                        } else {
                            //not first
                            null
                        }
                    }
                }
                endCursor = ancestor
            }
            val cursorParent = startCursor.parent
            val startIndex = cursorParent.indexOf(startCursor)
            var endIndex = cursorParent.indexOf(endCursor)
            if (startIndex == -1) {
                throw NodeException("Start not found in parent", startCursor)
            } else if (endIndex == -1) {
                throw NodeException("End not found in parent", endCursor)
            } else if (startIndex > endIndex) {
                throw NodeException("start $startIndex end $endIndex", endCursor)
            }
            // When the end is a table, we should include the table copy as well.
            if (BBX.CONTAINER.TABLE.isA(endCursor) && endIndex + 1 < cursorParent.childCount && cursorParent.getChild(
                    endIndex + 1
                ) is Element
                && isTableCopy((cursorParent.getChild(endIndex + 1) as Element))
            ) {
                endIndex++
            }
            val elements: MutableList<Node> = ArrayList()
            for (i in startIndex..endIndex) {
                elements.add(cursorParent.getChild(i))
            }
            return elements
        }
    }
}

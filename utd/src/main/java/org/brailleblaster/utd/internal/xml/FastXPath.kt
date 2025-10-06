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
package org.brailleblaster.utd.internal.xml

import nu.xom.Attribute
import nu.xom.Document
import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.NodeIterator.Companion.itrNextNode
import org.brailleblaster.utd.internal.xml.XMLHandler.Companion.nodeToElementOrParentOrDocRoot
import org.brailleblaster.utils.xom.childNodes
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Predicate

/**
 * Fast pure Java implementations of common XPath queries
 */
object FastXPath {
    @JvmStatic
    fun descendantOrSelf(startNode: Node?): Iterable<Node> {
        return Iterable { NodeIterator(startNode, stayInsideStartNode = true, forward = true) }
    }

    fun <N : Node> descendantFindList(
        startNode: Node,
        matcher: (MutableList<N>, Node) -> Boolean
    ): List<N> {
        return buildList {
            descendantFindFirst(startNode) { curNode: Node ->
                if (matcher(this, curNode)) {
                    @Suppress("UNCHECKED_CAST")
                    add(curNode as N)
                }
                true
            }
        }
    }

    fun <N : Node> descendantFindOnly(startNode: Node, matcher: Predicate<Node>): N {
        val mutableObject = AtomicReference<Node?>()
        @Suppress("UNCHECKED_CAST")
        return descendantFindFirst(startNode) { curNode ->
            if (matcher.test(curNode)) {
                if (mutableObject.get() != null) {
                    nodeToElementOrParentOrDocRoot(startNode)?.addAttribute(Attribute("first", "match"))
                    throw NodeException("Already matched element with first=match attrib, matched again: ", startNode)
                }
                mutableObject.set(startNode)
            }
            true
        } as N
    }

    /**
     * Keep processing until predicate returns false, then return stopped node
     */
    fun descendantFindFirst(startNode: Node, matcher: Predicate<Node>): Node? {
        if (!matcher.test(startNode)) {
            return startNode
        }
        for (curNode in startNode.childNodes) {
            val subResult = descendantFindFirst(curNode, matcher)
            if (subResult != null) {
                return subResult
            }
        }
        return null
    }

    @JvmStatic
    fun descendant(startNode: Node?): Iterable<Node> {
        return descendantOrSelf(startNode).drop(1)
    }

    @JvmStatic
    fun following(startNode: Node): Iterable<Node> {
        return Iterable {
            NodeIterator(
                itrNextNode(startNode, null, true),
                stayInsideStartNode = false,
                forward = true
            )
        }
    }

    fun followingAndSelf(startNode: Node?): Iterable<Node> {
        return Iterable {
            NodeIterator(
                startNode,
                stayInsideStartNode = false,
                forward = true
            )
        }
    }

    /**
     * Iterate backwards through the xml. Note: This is different from XPath's preceding
     * because it will match ancestors.
     */
    fun preceding(startNode: Node): Iterable<Node> {
        return Iterable {
            NodeIterator(
                itrNextNode(startNode, null, false),
                stayInsideStartNode = false,
                forward = false
            )
        }
    }

    /**
     * Iterate backwards through the xml, starting with the given node.
     * Note: This is different from XPath's preceding because it will
     * match ancestors.
     */
    fun precedingAndSelf(startNode: Node?): Iterable<Node> {
        return Iterable { NodeIterator(startNode, stayInsideStartNode = false, forward = false) }
    }

    @JvmStatic
    fun descendantAndFollowing(startNode: Node?): Iterable<Node> {
        return Iterable { NodeIterator(startNode, stayInsideStartNode = false, forward = true) }
    }

    @JvmStatic
    fun ancestor(startNode: Node): Iterable<Element> {
        return Iterable { ancestorIterator(
                if (startNode.parent is Element)
                    startNode.parent
                else
                    null
            )
        }.map { it as Element }
    }

    @JvmStatic
    fun ancestorOrSelf(startNode: Node?): Iterable<Node> {
        return Iterable { ancestorIterator(startNode) }
    }

    private fun ancestorIterator(actualStart: Node?): Iterator<Node> {
        return object : Iterator<Node> {
                var curElement: Node? = actualStart

                override fun hasNext(): Boolean {
                    return curElement != null
                }

                override fun next(): Node {
                    return curElement?.also {
                        val parent = it.parent
                        curElement = if (parent is Document) {
                            null
                        } else {
                            parent
                        }
                    } ?: throw NoSuchElementException()
                }
            }
    }
}

private class NodeIterator(private val startNode: Node?, private val stayInsideStartNode: Boolean, private val forward: Boolean) :
    Iterator<Node> {
    private var nextNode: Node? = startNode

    override fun hasNext(): Boolean {
        return nextNode != null
    }

    override fun next(): Node {
        return nextNode?.also { doAdvance() } ?: throw NoSuchElementException()
    }

    fun doAdvance() {
        nextNode = if (nextNode!!.childCount != 0) {
            nextNode!!.getChild(if (forward) 0 else nextNode!!.childCount - 1)
        } else {
            itrNextNode(
                nextNode!!,
                if (stayInsideStartNode) startNode else null,
                forward
            )
        }
    }

    companion object {
        /**
         * Safe following node impl that stops once outside of the given start node
         *
         * @param stopNode     Parent we are not escaping from
         * @param inputCurNode Assumed to be some (maybe nested) child of startNode
         */
        @JvmStatic
        fun itrNextNode(inputCurNode: Node, stopNode: Node?, forward: Boolean): Node? {
            var curNode: Node = inputCurNode
            //TODO: This will break if inputCurNode is not descendant from stopNode
            while (stopNode == null || stopNode !== curNode) {
                val parent = curNode.parent ?: break
                val index = parent.indexOf(curNode)
                if (forward && index != parent.getChildCount() - 1) {
                    return parent.getChild(index + 1)
                } else if (!forward && index > 0) {
                    return parent.getChild(index - 1)
                }
                //Last entry in parent, get parents sibling
                curNode = parent
            }
            //Finished getting all childrens parents
            return null
        }
    }
}

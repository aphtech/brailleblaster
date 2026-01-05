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
package org.brailleblaster.search

import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.splitNode

class DOMControl(private val click: Click) {
    val possiblesCorrectAttributes: MutableList<Node> = ArrayList()
    private val possiblesPastStart: MutableList<Node> = ArrayList()

    init {
        val allNodes = FastXPath.following(click.initialView.mapElement.node).filterIsInstance<Text>()
        possiblesPastStart += allNodes
        if (SearchUtils.checkCorrectAttributes(click.initialView.mapElement.node, click)) {
            possiblesCorrectAttributes.add(0, click.initialView.mapElement.node)
            // following::text will not add the current element
        }
        possiblesCorrectAttributes += allNodes.filter { SearchUtils.checkCorrectAttributes(it, click) }
    }

    fun searchNoView(array: List<Node>): String {
        return array.joinToString(separator = "") { "${it.value} " }
    }

    fun searchNoViewNoSpaces(array: List<Node>): String {
        return array.joinToString(separator = "") { it.value }
    }

    fun split(theNode: Node, nodeStart: Int, nodeEnd: Int, selectionStart: Int, selectionEnd: Int): Node {
        val toReturn: Node
        val n = theNode as Text
        toReturn = if (nodeStart == selectionStart) {
            if (nodeEnd == selectionEnd) {
                n
            } else {
                val list = n.splitNode(
                    selectionEnd - nodeStart
                )
                list[0]
            }
        } else {
            val list: List<Text> = if (nodeEnd == selectionEnd) {
                n.splitNode(
                    selectionStart - nodeStart
                )
            } else {
                n.splitNode(
                    selectionStart - nodeStart,
                    selectionEnd - nodeStart
                )
            }
            list[1]
        }
        return toReturn
    }
}

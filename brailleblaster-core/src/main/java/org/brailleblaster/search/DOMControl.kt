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
import nu.xom.Nodes
import nu.xom.Text
import org.brailleblaster.utd.internal.xml.XMLHandler

class DOMControl(private val click: Click) {
    val possiblesCorrectAttributes: MutableList<Node> = ArrayList()
    private val possiblesPastStart: MutableList<Node> = ArrayList()

    init {
        val allNodes = click.initialView.mapElement.node.query("following::text()")
        nodesToArrayList(allNodes)
        if (SearchUtils.checkCorrectAttributes(click.initialView.mapElement.node, click)) {
            possiblesCorrectAttributes.add(0, click.initialView.mapElement.node)
            // following::text will not add the current element
        }
        getNodes(allNodes)
    }

    private fun nodesToArrayList(nodes: Nodes) {
        for (i in 0 until nodes.size()) {
            possiblesPastStart.add(nodes[i])
        }
    }

    private fun getNodes(nodes: Nodes) {
        for (i in 0 until nodes.size()) {
            val node = nodes[i]
            if (SearchUtils.checkCorrectAttributes(node, click)) {
                possiblesCorrectAttributes.add(node)
            }
        }
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
                val list = XMLHandler.splitTextNode(
                    n,
                    selectionEnd - nodeStart
                )
                list[0]
            }
        } else {
            val list: List<Text> = if (nodeEnd == selectionEnd) {
                XMLHandler.splitTextNode(
                    n,
                    selectionStart - nodeStart
                )
            } else {
                XMLHandler.splitTextNode(
                    n,
                    selectionStart - nodeStart,
                    selectionEnd - nodeStart
                )
            }
            list[1]
        }
        return toReturn
    }
}

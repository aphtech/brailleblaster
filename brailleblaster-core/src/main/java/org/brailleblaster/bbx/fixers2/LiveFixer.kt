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
package org.brailleblaster.bbx.fixers2

import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler

/**
 * Cleanup documents while user is editing them
 */
object LiveFixer {
    const val PILCROW = "\u00B6"
    const val NEWPAGE_PLACEHOLDER_ATTRIB = "newPagePlaceholder"
    @JvmStatic
	fun fix(root: Element) {
        ImportFixerCommon.applyToDescendantBlocks(
            root,
            { obj: Element, block: MutableList<Text>, descendantTextNodes: MutableList<Node> ->
                detachEmptyBlocks(
                    obj,
                    block,
                    descendantTextNodes
                )
            },
            { obj: Element, block: MutableList<Text>, descendantTextNodes: MutableList<Node> ->
                trimBlockText(
                    obj,
                    block,
                    descendantTextNodes
                )
            },
            { obj: Element, block: List<Text>, descendantTextNodes: List<Node> ->
                cleanupNewPagePlaceholder(
                    obj,
                    block,
                    descendantTextNodes
                )
            },
            { obj: Element, block: MutableList<Text>, descendantTextNodes: MutableList<Node> ->
                detachEmptyTextNodes(
                    obj,
                    block,
                    descendantTextNodes
                )
            })
        removeEmptyContainers(root)
    }

    private fun detachEmptyBlocks(
        block: Element,
        descendantTextNodes: MutableList<Text>,
        nodesToDetach: MutableList<Node>
    ) {
        for (text in descendantTextNodes) {
            if (!text.value.isNullOrBlank()) {
                return
            }
        }
        if (BBX.BLOCK.IMAGE_PLACEHOLDER.isA(block) || BBX.BLOCK.TABLE_CELL.isA(block) || BBX.BLOCK.PAGE_NUM.isA(block)) {
            return
        }

        // only contains blank text nodes
        if (FastXPath.descendant(block).stream().anyMatch { node: Node? -> BBX.SPAN.IMAGE.isA(node) }) {
            // leave image
            nodesToDetach.addAll(descendantTextNodes)
        } else {
            nodesToDetach.add(block)
        }
        descendantTextNodes.clear()
    }

    private fun trimBlockText(
        block: Element,
        descendantTextNodes: MutableList<Text>,
        nodesToDetach: MutableList<Node>
    ) {
        if (descendantTextNodes.isEmpty()) {
            return
        }
        while (descendantTextNodes.isNotEmpty()) {
            val textNode = descendantTextNodes.removeAt(0)
            if (textNode.value.isBlank()) {
                nodesToDetach.add(textNode)
                continue
            }
            val newValue = textNode.value.trimStart()
            if (newValue != textNode.value) {
                textNode.value = newValue
            }
            // re-add for potential further processing by stripEnd below
            descendantTextNodes.add(0, textNode)
            break
        }
        while (descendantTextNodes.isNotEmpty()) {
            val textNode = descendantTextNodes.removeAt(descendantTextNodes.size - 1)
            if (textNode.value.isNullOrBlank() && XMLHandler.ancestorElementNot(textNode) { node: Element? ->
                    BBX.BLOCK.TABLE_CELL.isA(node)
                }) {
                nodesToDetach.add(textNode)
                continue
            }
            val newValue = textNode.value.trimEnd()
            if (newValue != textNode.value) {
                textNode.value = newValue
            }
            // re-add for potential further processing
            descendantTextNodes.add(textNode)
            break
        }
    }

    private fun detachEmptyTextNodes(
        block: Element,
        descendantTextNodes: MutableList<Text>,
        nodesToDetach: MutableList<Node>
    ) {
        val itr = descendantTextNodes.iterator()
        while (itr.hasNext()) {
            val next = itr.next()
            if (next.value.isEmpty()) {
                itr.remove()
                nodesToDetach.add(next)
            }
        }
    }

    private fun cleanupNewPagePlaceholder(block: Element, descendantTextNodes: List<Text>, nodesToDetach: List<Node>) {
        val attrib = block.getAttribute(NEWPAGE_PLACEHOLDER_ATTRIB) ?: return
        var stripped = false
        for (descendantTextNode in descendantTextNodes) {
            var value = descendantTextNode.value
            if (value.length > 1 && value.contains("" + PILCROW)) {
                value = value.replace(PILCROW, "")
                descendantTextNode.value = value
                stripped = true
            }
        }
        if (stripped) {
            attrib.detach()
        }
    }

    private fun removeEmptyContainers(root: Element) {
        val toDetach = FastXPath.descendant(root)
            .stream()
            .filter { node: Node? -> BBX.CONTAINER.isA(node) }
            .filter { container: Node? ->
                !BBX.CONTAINER.IMAGE.isA(container) && !BBX.CONTAINER.TPAGE_SECTION.isA(
                    container
                )
            }
            .filter { container: Node -> container.childCount == 0 }.toList()
        for (node in toDetach) {
            node.detach()
        }
    }
}
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
package org.brailleblaster.perspectives.mvc.modules.misc

import nu.xom.Element
import nu.xom.Node
import nu.xom.Nodes
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.utils.xml.BB_NS

object FindGuideWordModule {
    var manager: Manager? = null
    fun findGuideWords(selected: Node?, manager: Manager) {
        FindGuideWordModule.manager = manager
        var selectedNode = selected ?: manager.simpleManager.currentSelection.start.node
        if (selectedNode is Text) {
            selectedNode = selectedNode.parent
        }
        if (selectedNode is Element) {
            var blocks =
                findBlocks(selectedNode.query("descendant::*[contains(name(), 'BLOCK')]", BBX.XPATH_CONTEXT))
            if (blocks.isEmpty()) {
                blocks = findBlocks(
                    selectedNode.query(
                        "following-sibling::*[contains(name(), 'BLOCK')]",
                        BBX.XPATH_CONTEXT
                    )
                )
                blocks.addAll(
                    findBlocks(
                        selectedNode.query(
                            "self::*[contains(name(), 'BLOCK')]",
                            BBX.XPATH_CONTEXT
                        )
                    )
                )
            }
            wrapPotentialGW(blocks)
            manager.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, true, selectedNode.parent))
        }
    }

    private fun findBlocks(nodes: Nodes): MutableList<Element> {
        val indexWords: MutableList<Element> = ArrayList()
        for (i in 0 until nodes.size()) {
            if (nodes[i] is Element) {
                if ((nodes[i] as Element).getAttribute(
                        "itemLevel",
                        BB_NS
                    ) != null && (nodes[i] as Element).getAttributeValue(
                        "itemLevel",
                        BB_NS
                    ) == "0"
                    || (nodes[i] as Element).getAttribute("itemLevel", BB_NS) == null
                ) {
                    indexWords.add(nodes[i] as Element)
                }
            }
        }
        return indexWords
    }

    private fun wrapPotentialGW(blocks: List<Element>) {
        for (block in blocks) {
            val word = getFirstTextChild(block)
            val span = BBX.SPAN.GUIDEWORD.create()
            val words = word.value.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            span.appendChild(words[0])
            val remainingStr = word.value.substring(words[0].length)
            val textStr = Text(remainingStr)
            val index = word.parent.indexOf(word)
            word.parent.replaceChild(word, span)
            if (remainingStr.isNotEmpty()) {
                span.parent.insertChild(textStr, index + 1)
            }
        }
    }

    private fun getFirstTextChild(element: Element): Text {
        val text = element.query("descendant::text()")[0]
        return text as Text
    }
}
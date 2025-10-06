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

import nu.xom.*
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBXUtils
import org.brailleblaster.bbx.findBlock
import org.brailleblaster.math.mathml.MathMLElement
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.TableTextMapElement
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.perspectives.braille.mapping.maps.MapList
import org.brailleblaster.search.SavedSearches.lastMemory
import org.brailleblaster.search.SearchController.Companion.logIt
import org.brailleblaster.search.SearchUtils.checkCorrectAttributes
import org.brailleblaster.search.SearchUtils.checkUneditable
import org.brailleblaster.search.SearchUtils.isBraille
import org.brailleblaster.search.SearchUtils.matchPhrase
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.utils.UTDHelper.Companion.stripUTDRecursive
import org.brailleblaster.util.Utils.insertChildCountSafe

class ViewControl(val m: Manager, val click: Click) {
    /**
     * Need to check current element in the case of a user locating a word with
     * find and then pressing replace.
     */
    fun checkToReplaceCurrent(): Node? {
        if (m.textView.selectionText.isEmpty()) {
            return null
        }

        var current = m.mapList.getClosest(m.textView.caretOffset, true).node
        val x = m.textView.selectionRange.x
        val y = m.textView.selectionRange.y + x

        if (checkUneditable(current)) {
            return null
        }

        /*
     * RT 5280 - deal with a cursor between elements -- getClosest will not
     * guarantee the highlighted passage's node will be the one returned.
     * i.e. <p><strong>HI</strong>[cursor here]THERE</p> may return THERE
     * even if your selection is HI
     */
        val allElements = m.getAllTextMapElementsInRange(x, y)
        val array = allElements.toTypedArray()
        if (allElements.isEmpty()) {
            return null
        } else if (allElements.size == 1) {
            current = array[0]!!.node
        } else if (allElements.size == 2) {
            if (x == array[1]!!.getEnd(m.mapList)) {
                current = array[0]!!.node
            } else if (y == array[0]!!.getStart(m.mapList)) {
                current = array[1]!!.node
            } else if (x == array[1]!!.getStart(m.mapList)) {
                current = array[1]!!.node
            }
        }
        if (checkCorrectAttributes(current, click)) {
            if (matchPhrase(m.textView.selectionText, click, false).hasNext()) {
                if (lastMemory != null && lastMemory!!.replaced != null) {
                    if (current == lastMemory!!.replaced!!.mapElement.node) {
                        if (x != lastMemory!!.replaced!!.cursorOffset) {
                            return current
                        }
                    } else {
                        return current
                    }
                }
                return current
            }
        }
        return null
    }

    val currentViewState: ViewState
        get() {
            val index = m.textView.caretOffset
            val mapElement = m.mapList.getClosest(index, true)
            val section = m.getSection(mapElement.node)
            return ViewState(index, section, mapElement)
        }

    fun findNodeAndHighlight(node: Node) {
        val startSection = click.initialView.section
        for (i in startSection until m.sectionList.size) {
            val section = m.sectionList[i]
            val list = section.list
            val listSize = list.size
            for (j in 0 until listSize) {
                val mapElement = list[j]
                if (mapElement.node != null && mapElement.node == node) {
                    if (!section.isVisible) {
                        m.useResetSectionMethod(i)
                        m.mapList.setCurrent(0)
                        m.waitForFormatting(true)
                    }
                    //If not finding emphasis...
                    if ((click.settings.findHasStyle() || click.settings.findHasContainer()) && !(click.settings.findHasEmphasis())) {
                        highlightMultipleElements(
                            node, mapElement.getStart(m.mapList),
                            mapElement.getEnd(m.mapList), j, list
                        )
                    } else {
                        m.textView.topIndex = m.textView.getLineAtOffset(mapElement.getStart(m.mapList))
                        m.text.highlight(mapElement.getStart(m.mapList), mapElement.getEnd(m.mapList))
                    }
                    return
                }
            }
        }
    }

    fun highlightMultipleElements(node: Node, start: Int, end: Int, mapIndex: Int, list: MapList) {
        var curEnd = end
        var block: Node? = node
        while (block != null && !BBX.BLOCK.isA(block)) {
            block = block.parent
        }
        if (block == null) {
            logIt("Node unattached to document")
            return
        }
        var siblings = node.query("following::text()")
        siblings = removeBraille(siblings)
        if (siblings.size() > 0) {
            var unexaminedChildren = true
            var siblingsExamined = 0
            var previous: Node? = node
            while (unexaminedChildren && siblingsExamined < siblings.size()) {
                val child = siblings[siblingsExamined]
                if (parentChildRelationship(child, block)) {
                    siblingsExamined++
                } else {
                    unexaminedChildren = false
                    // get map element offset for this last child
                    if (previous != node) {
                        for (j in mapIndex until list.size) {
                            val mapElement = list[j]
                            if (mapElement.node != null) { //Beware the null nodes
                                if (mapElement.node == previous) {
                                    curEnd = mapElement.getEnd(m.mapList)
                                }
                            }
                        }
                    }
                }
                previous = child
            }

            if (unexaminedChildren) {
                SearchController.log.debug("Last child of parent included in sibling text condensation")
                // get map element offset for this last child
                if (previous != node) {
                    for (j in mapIndex until list.size) {
                        val mapElement = list[j]
                        if (mapElement.node == previous) {
                            curEnd = mapElement.getEnd(m.mapList)
                        }
                    }
                }
            }
        }
        m.textView.topIndex = m.textView.getLineAtOffset(start)
        m.text.highlight(start, curEnd)
    }

    private fun removeBraille(siblings: Nodes): Nodes {
        val notBraille = Nodes()
        for (i in 0 until siblings.size()) {
            if (!XMLHandler.ancestorElementIs(siblings[i]) { obj: Element -> isBraille(obj) }) {
                notBraille.append(siblings[i])
            }
        }
        return notBraille
    }

    private fun parentChildRelationship(child: Node?, block: Node): Boolean {
        var node = child
        while (node != null) {
            if (block == node) {
                return true
            }
            node = node.parent
        }
        return false
    }

    fun findInSection(section: Int, possiblesCorrectAttributes: List<Node?>): Boolean {
        var buffer = 0
        val view: String
        if (section == click.initialView.section) {
            view = m.textView.text.substring(
                m.textView.caretOffset,
                m.textView.charCount
            )
            buffer = m.textView.caretOffset
        } else {
            logIt(" resetting to $section")
            m.useResetSectionMethod(section)
            m.waitForFormatting(true)
            m.mapList.setCurrent(0)
            view = m.textView.text
        }
        val match = matchPhrase(view, click, false)
        while (match.hasNext()) {
            val pair = match.next

            logIt(
                (" looking at match pair " + m.textView.getText(pair.start + buffer, pair.end + buffer)
                        + " at " + pair.start + buffer + ", " + pair.end + buffer)
            )

            val list = m.mapList.getElementInSelectedRange(pair.start + buffer, pair.end + buffer)
            var current = true
            for (mapElement in list) {
                if (!(possiblesCorrectAttributes.contains(mapElement.node)
                            || mapElement is TableTextMapElement || mapElement is MathMLElement)
                ) {
                    current = false
                    break
                }
            }
            if (current) {
                m.textView.topIndex = m.textView.getLineAtOffset(pair.start + buffer)
                logIt(
                    "highlighting match pair " + m.textView.getText(pair.start + buffer, pair.end + buffer)
                )

                m.text.highlight(pair.start + buffer, pair.end + buffer)
                return true
            }
        }
        return false
    }

    fun getNodeFromBeginning(section: Int): List<Node> {
        val array: MutableList<Node> = ArrayList()
        val sectionElement = m.sectionList[section]
        val maplist = sectionElement.list
        for (textMapElement in maplist) {
            if (textMapElement.node != null) {
                array.add(textMapElement.node)
            }
        }
        return array
    }

    val nodeFromCurrent: List<Node>
        get() {
            val array: MutableList<Node> = ArrayList()
            val maplist = m.mapList
            val currentIndex = m.mapList
                .getNodeIndex(m.mapList.getClosest(m.textView.caretOffset, true))
            for (i in currentIndex until maplist.size) {
                if (maplist[i].node != null) {
                    array.add(maplist[i].node)
                }
            }
            return array
        }

    fun acrossNodeReplace(array: Array<TextMapElement>, x: Int, y: Int): Node {
        val sBefore = StringBuilder()
        val sTheNode = StringBuilder()
        val sAfter = StringBuilder()
        var pastX = false
        var pastY = false
        for (textMapElement in array) {
            val start = textMapElement.getStart(m.mapList)
            val end = textMapElement.getEnd(m.mapList)
            if (end < 1) {
                SearchController.log.debug("Text node is one character.")
                continue
            }
            val string = m.textView.getText(start, end - 1)
            if (!pastY) {
                // is y in this node?
                if (end >= y) {
                    // y is in this node
                    sTheNode.append(string, 0, y - start)
                    sAfter.append(string.substring(y - start))
                    pastY = true
                } else if (!pastX) {
                    // is x in this node?
                    if (end >= x) {
                        // x is in this node
                        sBefore.append(string, 0, x - start)
                        sTheNode.append(string.substring(x - start))
                        pastX = true
                    } else {
                        sBefore.append(string)
                    }
                }
            } else if (!pastX) {
                // is x in this node?
                if (end >= x) {
                    // x is in this node
                    sBefore.append(string, 0, end - x)
                    sTheNode.append(string.substring(end - x))
                    pastX = true
                } else {
                    sBefore.append(string)
                }
            }
        }
        val before = Text(sBefore.toString().replace("\r".toRegex(), "").replace("\n".toRegex(), ""))
        val theNode = Text(sTheNode.toString().replace("\r".toRegex(), "").replace("\n".toRegex(), ""))
        val after = Text(sAfter.toString().replace("\r".toRegex(), "").replace("\n".toRegex(), ""))
        // juggle the xml
        val blocks: MutableList<Element> = ArrayList()
        for (textMapElement in array) {
            val block = textMapElement.node.findBlock()
            if (!blocks.contains(block)) {
                blocks.add(block)
            }
        }
        val parent = blocks[0]
        var index = BBXUtils.getIndexInBlock(array[0].node)
        if (before.value.isNotEmpty()) {
            insertChildCountSafe(parent, before, index++)
        }
        insertChildCountSafe(parent, theNode, index++)
        if (after.value.isNotEmpty()) {
            insertChildCountSafe(parent, after, index)
        }
        for (tme in array) {
            tme.node.detach()
        }
        stripUTDRecursive(parent)
        if (blocks.size > 1) {
            for (block in blocks) {
                stripUTDRecursive(block)
                for (i in 0 until block.childCount) {
                    val n = block.getChild(0)
                    n.detach()
                    parent.appendChild(n)
                }
            }
        }

        return theNode
    }
}

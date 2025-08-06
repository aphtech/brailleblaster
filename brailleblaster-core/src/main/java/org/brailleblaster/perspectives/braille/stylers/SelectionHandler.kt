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
package org.brailleblaster.perspectives.braille.stylers

import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBXUtils
import org.brailleblaster.math.mathml.MathMLElement
import org.brailleblaster.math.mathml.MathUtils.deleteMathFromSelectionHandlerEvent
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.*
import org.brailleblaster.perspectives.braille.mapping.maps.MapList
import org.brailleblaster.perspectives.braille.messages.RemoveNodeMessage
import org.brailleblaster.perspectives.braille.messages.SelectionMessage
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.searcher.Searcher.streamCurrentBufferFromCurrentCursor
import org.brailleblaster.perspectives.braille.searcher.Searcher.streamCurrentBufferReverseFromCurrentCursor
import org.brailleblaster.perspectives.braille.viewInitializer.ViewInitializer
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.utd.actions.GenericBlockAction
import org.brailleblaster.util.WhitespaceUtils.removeLineBreakElements

class SelectionHandler(manager: Manager?, vi: ViewInitializer?, list: MapList?) : Handler(
    manager!!, vi!!, list!!
) {
    fun removeSelection(m: SelectionMessage) {
        val startPos = m.start
        val endPos = m.end
        val replacedText = m.replacedText
        val replacementText = m.replacementText
        var startIndex = getIndex(startPos)
        var endIndex = getIndex(endPos)

        // trim whitespace from beginning
        while (startIndex < endIndex && isWhitespace(list[startIndex])) startIndex++

        // trim read only and whitespace from end
        while (endIndex > startIndex && (list[endIndex] is BoxLineTextMapElement
                    || list[endIndex] is WhiteSpaceElement)
        ) endIndex--

        // If only whitespace has been selected, reformat to restore deleted
        // whitespace
        if (startIndex == endIndex && isWhitespace(list[startIndex])) {
            var nearest = streamCurrentBufferReverseFromCurrentCursor(manager)
                .filter { t: TextMapElement -> t.node != null && t.node.document != null }.findFirst().orElse(null)
            if (nearest == null) {
                nearest = streamCurrentBufferFromCurrentCursor(manager)
                    .filter { t: TextMapElement -> t.node != null && t.node.document != null }.findFirst().orElse(null)
            }
            if (nearest != null && nearest.node != null) {
                reformat(nearest.node, false)
            }
            return
        }

        val firstEl = getBlockElement(startIndex)
        val lastEl = getBlockElement(endIndex)
        val firstList = getBlockMapElements(startIndex, firstEl)
        val first = list[startIndex]
        val last = list[endIndex]
        val endOfDocument = isFinalTextTME(last, list)

        var textStart = first.getStart(list)

        if (firstEl === lastEl) {
            // The selection is within the same block

            if (textStart > startPos) textStart = startPos

            updateFirstNode(firstList, firstEl, first, startPos, endPos, replacedText, replacementText)
            var index = endIndex
            if (first != last && endPos < last.getEnd(list)) {
                list.setCurrent(endIndex)
                updateSecondNode(last, startPos, endPos, replacedText)
            } else if (endPos >= last.getEnd(list)) {
                index++
            }

            for (i in startIndex + 1 until index) removeElement(i)

            val parent: Node? = firstEl.parent
            BBXUtils.cleanupBlock(firstEl)
            if (firstEl.document == null) {
                // Node was detached
                if (endOfDocument && list.getPrevious(list.indexOf(first), true) != null) {
                    removeLineBreakElements(
                        list,
                        list.indexOf(list.getPrevious(list.indexOf(first), true)), list.size - 1
                    )
                }
                val changedNodes: Array<Node> = if (parent == null) emptyArray() else arrayOf(parent)
                manager.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, false, *changedNodes))
            } else {
                reformat(firstEl, true)
            }
        } else {
            // Selection is in two different block

            var clearAll = false
            var removeFirst = false
            var removeLast = false

            val lastList = getBlockMapElements(endIndex, lastEl)

            // if whole document is selected
            if (list.indexOf(first) == 0 && list.indexOf(last) == list.size - 1) {
                if (endPos >= last.getEnd(list)) clearAll = true
            }

            // startPos <= first.getStart(list) changed to < for RT6424
            if ((!clearAll // Next TME after first block is not readonly
                        && !readOnly(list[list.indexOf(firstList[firstList.size - 1]) + 1]) // selection starts before first TME (?!) and ends after
                        // first TME but before last TME end
                        && (startPos < first.getStart(list) && endPos > first.getEnd(list))) && endPos < last.getEnd(
                    list
                ) && replacementText.isEmpty()
            ) removeFirst = true

            if (startIndex > list.indexOf(firstList[0])) {
                startIndex = list.indexOf(firstList[0])
                textStart = list[startIndex].getStart(list)
            }

            updateFirstNode(firstList, firstEl, first, startPos, endPos, replacedText, replacementText)

            if (!readOnly(first)) {
                val index = if (removeFirst) list.indexOf(first)
                else list.indexOf(first) + 1

                val removed = clearElement(index, list.indexOf(firstList[firstList.size - 1]) + 1)
                endIndex -= removed
            }

            // if last TME of first selected block != TME before last selected
            // block
            if (list.indexOf(firstList[firstList.size - 1]) != list.indexOf(lastList[0]) - 1) {
                val readOnly = removeElements(
                    list.indexOf(firstList[firstList.size - 1]) + 1,
                    list.indexOf(lastList[0])
                )
                endIndex -= readOnly.size
                var j = 0
                while (j < readOnly.size) {
                    if (readOnly[j] !is BoxLineTextMapElement
                        && readOnly[j] !is PageIndicatorTextMapElement
                    ) {
                        readOnly.removeAt(j)
                        j--
                    } else if (readOnly[j] is PageIndicatorTextMapElement) {
                        readOnly[j].node.parent.removeChild(readOnly[j].node)
                    }
                    j++
                }
            }

            if (!readOnly(last) && !clearAll) {
                list.setCurrent(endIndex)
                updateSecondNode(last, startPos, endPos, replacedText)
                val removed = clearElement(list.indexOf(lastList[0]), list.indexOf(last))
                endIndex -= removed
            } else if (clearAll) {
                removeLast = true
                removeElement(list.indexOf(last))
            }

            if (endIndex < list.indexOf(lastList[lastList.size - 1])) endIndex =
                list.indexOf(lastList[lastList.size - 1])

            if (textStart > startPos) textStart = startPos
            val parent: Node? = firstEl.parent
            if (firstEl.document != null) {
                BBXUtils.cleanupBlock(firstEl)
            }
            if (lastEl.document != null) {
                BBXUtils.cleanupBlock(lastEl)
            }
            val toTranslate = ArrayList<Node>()
            if (firstEl.document != null) {
                if (!readOnly(firstEl) && !isBoxLine(firstEl) && !removeFirst) toTranslate.add(firstEl)
                else if (!readOnly(firstEl) && !isBoxLine(firstEl) && removeFirst) firstEl.parent.removeChild(firstEl)
            }

            if (lastEl.document != null) {
                if (!readOnly(lastEl) && !isBoxLine(lastEl) && !removeLast) toTranslate.add(lastEl)
            } else if (endOfDocument) {
                // Last node was the end of the document and was detached, so
                // end of document line breaks
                // need to be cleaned up
                if (firstEl.document == null) {
                    if (list.getPrevious(list.indexOf(first), true) != null) {
                        // First element was also detached
                        removeLineBreakElements(
                            list,
                            list.indexOf(list.getPrevious(list.indexOf(first), true)), list.size - 1
                        )
                    } else {
                        // Entire document was deleted
                        removeLineBreakElements(list, 0, list.size - 1)
                    }
                } else if (firstEl.document != null && list.getPrevious(list.indexOf(last), true) != null) {
                    // Just the last element was detached
                    removeLineBreakElements(list, list.indexOf(first), list.size - 1)
                }
            }

            if (toTranslate.isNotEmpty()) {
                reformat(toTranslate, true)
            } else {
                if (firstEl.document != null || lastEl.document != null) {
                    reformat(if (firstEl.parent != null) firstEl else lastEl, false)
                } else {
                    val changedNodes: Array<Node> = if (parent == null || parent.document == null) emptyArray()
                    else arrayOf(parent)
                    manager.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, false, *changedNodes))
                }
            }
        }
    }

    private fun updateFirstNode(
        elList: List<TextMapElement>, e: Element, t: TextMapElement, start: Int, end: Int,
        replacedText: String, replacementText: String
    ) {
        var mutStart = start
        if (t is TabTextMapElement) {
            t.getNodeParent().removeChild(t.node)
        } else if (t is PageIndicatorTextMapElement) {
            t.node.parent.removeChild(t.node)
        } else if (t is MathMLElement) {
            deleteMathFromSelectionHandlerEvent(manager, t, mutStart, end)
        } else if (!readOnly(t)) {
            if (mutStart <= t.getStart(list) && end >= t.getEnd(list) && replacementText.isEmpty()) {
                // remove whole node
                clearText(t)
                t.nodeParent.removeChild(t.node)
            } else if (mutStart < t.getStart(list) && end < t.getEnd(list)) {
                // remove first half of node
                val offset = end - t.getStart(list)
                val newText = if ((0 <= offset)) t.text.substring(offset) else ""
                val node = t.node as Text
                node.value = newText
            } else {
                // remove a piece of the node
                if (mutStart <= t.getStart(list)) mutStart = t.getStart(list)

                if (t.getEnd(list) != mutStart) {
                    val rTxt = replacedText.take(t.getEnd(list) - mutStart).replace(LINE_BREAK.toRegex(), "")
                    val selStart = t.text.indexOf(rTxt)
                    var unEditedText = ""
                    if (0 <= selStart) {
                        unEditedText = t.text.substring(0, selStart)
                    }

                    val newText = unEditedText + replacementText
                    val node = t.node as Text
                    node.value = newText
                } else if (replacementText.isNotEmpty()) {
                    // insert
                    val newText = t.text + replacementText
                    val node = t.node as Text
                    node.value = newText
                }
            }
        }
    }

    private fun updateSecondNode(t: TextMapElement, start: Int, end: Int, replacedText: String) {
        if (t is TabTextMapElement) {
            t.getNodeParent().removeChild(t.node)
        }
        if (t is MathMLElement) {
            deleteMathFromSelectionHandlerEvent(manager, t, start, end)
        } else {
            val newText: String
            if (end >= t.getEnd(list)) {
                // remove whole node
                clearText(t)
                t.nodeParent.removeChild(t.node)
            } else if (end <= t.getStart(list)) {
                // don't remove anything
                newText = t.text
                val node = t.node as Text
                node.value = newText
            } else {
                // remove line breaks
                val offset = end - t.getStart(list)
                val startOffset = t.getStart(list) - start
                val lineBreaks = (replacedText.substring(startOffset).length
                        - replacedText.substring(startOffset).replace(LINE_BREAK.toRegex(), "").length)
                newText = t.text.substring(offset - lineBreaks)
                val node = t.node as Text
                node.value = newText
            }
        }
    }

    private fun clearText(t: TextMapElement) {
        val textNode = t.node as Text
        textNode.value = ""
    }

    private fun clearElement(startIndex: Int, endIndex: Int): Int {
        var removed = 0
        for (i in startIndex until endIndex) {
            removeElement(i)
            removed++
        }

        return removed
    }

    /*
     * remove parent element of this index in the maplist
     */
    private fun removeElement(listIndex: Int) {
        val t = list[listIndex]
        if (!readOnly(t) && t.nodeParent != null) {
            var e = t.nodeParent
            val m = RemoveNodeMessage(listIndex, t.getEnd(list) - t.getStart(list))
            manager.document.removeNode(list, m)
            if (e.childCount == 0) {
                if (manager.getAction(e) !is GenericBlockAction) {
                    if (e.parent.childCount == 1) e = e.parent as Element
                }
                val parent = e.parent
                parent.removeChild(e)
            }
        }
    }

    private fun removeElements(start: Int, end: Int): ArrayList<TextMapElement> {
        var index = start
        val elList = ArrayList<TextMapElement>()
        while (index < end) {
            if (isWhitespace(list[index])) {
                index++
            } else if (!readOnly(list[index])) {
                val e = getBlockElement(index)
                val local = getBlockMapElements(index, e)
                elList.addAll(local)
                index = list.indexOf(elList[elList.size - 1]) + 1
            } else {
                elList.add(list[index])
                index++
            }
        }

        // Issue #6497: Workaround for duplicate TME's, no idea how they are
        // getting into elList
        val elListUnique = LinkedHashSet(elList)
        elList.clear()
        elList.addAll(elListUnique)

        for (textMapElement in elList) removeElement(list.indexOf(textMapElement))

        clearListItems(elList)
        return elList
    }

    private fun clearListItems(elList: ArrayList<TextMapElement>) {
        for (textMapElement in elList) {
            vi.remove(list, list.indexOf(textMapElement))
        }
    }

    private fun getIndex(pos: Int): Int {
        return list.findClosest(pos, list.current, 0, list.size - 1)
    }

    private fun getBlockElement(index: Int): Element {
        val t = list[index]
        if (t is BoxLineTextMapElement || t is PageIndicatorTextMapElement) return list[index].nodeParent
        var n = list[index].node
        while (!BBX.BLOCK.isA(n)) {
            n = n.parent
        }
        return n as Element
    }

    private fun getBlockMapElements(index: Int, el: Element): List<TextMapElement> {
        return list.findTextMapElements(index, el)
    }

    private fun isFinalTextTME(tme: TextMapElement, list: MapList): Boolean {
        for (index in list.indexOf(tme) + 1 until list.size) {
            if (list[index] !is WhiteSpaceElement) {
                return false
            }
        }
        return true
    }
}

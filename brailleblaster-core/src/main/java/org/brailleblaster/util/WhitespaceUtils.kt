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
package org.brailleblaster.util

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.findBlock
import org.brailleblaster.math.mathml.MathModule
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.*
import org.brailleblaster.perspectives.braille.mapping.maps.MapList
import org.brailleblaster.utd.properties.UTDElements

object WhitespaceUtils {
    @JvmStatic
    fun convertWhiteSpaceToLineBreaks(t1: TextMapElement, t2: TextMapElement, list: MapList) {
        val index1 = list.indexOf(t1)
        val index2 = list.indexOf(t2)
        var addEndOfLine = t1 !is ImagePlaceholderTextMapElement
        for (i in index1 + 1 until index2) {
            if (list[i] is FormattingWhiteSpaceElement && list[i] !is PaintedWhiteSpaceElement
                && list[i] !is PageBreakWhiteSpaceElement
            ) {
                val lineBreak = appendLineBreakElement(t1.node)
                list.removeAt(i)
                list.add(i, LineBreakElement(lineBreak))
            } else if (list[i] is LineBreakElement || list[i] is PageBreakWhiteSpaceElement) {
                addEndOfLine = false
            }
        }
        if (addEndOfLine || areAdjacentTMEs(t1, t2, list)) {
            val endOfLineBreak = appendLineBreakElement(t1.node)
            val lbe = LineBreakElement(endOfLineBreak)
            lbe.isEndOfLine = true
            list.add(index1 + 1, lbe)
        }
    }

    @JvmStatic
    fun appendLineBreakElement(n: Node): Element {
        return addLineBreakElement(n, 1)
    }

    @JvmStatic
    fun prependLineBreakElement(n: Node): Element {
        return addLineBreakElement(n, 0)
    }

    private fun addLineBreakElement(n: Node, offset: Int): Element {
        var offset = offset
        var block = n.findBlock()
        if (block == null || BBX.BLOCK.TABLE_CELL.isA(block)) {
            if (Manager.getTableParent(n) != null) {
                block = Manager.getTableParent(n)
                if (offset == 1) offset++
            } else if (n is Element) {
                //Could be a boxline, so let the rest of this method apply to the container
                block = n
            } else {
                //Otherwise we have a floating text element outside of a block
                throw IllegalStateException("Text node without parent block")
            }
        }
        if (MathModule.isSpatialMath(n)) {
            block = MathModule.getSpatialMathParent(n)
        }
        val parent = block.parent
        val lineBreak = UTDElements.NEW_LINE.create()
        Utils.insertChildCountSafe(parent, lineBreak, parent.indexOf(block) + offset)
        return lineBreak
    }

    @JvmStatic
    fun countLineBreaks(t1: TextMapElement, t2: TextMapElement, list: MapList, countEndOfLine: Boolean): Int {
        var count = 0
        for (i in list.indexOf(t1) until list.indexOf(t2)) {
            if (list[i] is LineBreakElement) {
                if (countEndOfLine || !(list[i] as LineBreakElement).isEndOfLine) {
                    count++
                }
            }
        }
        return count
    }

    /**
     * Check if t1 and t2 are adjacent to each other, or if they only have running head
     * whitespace between them
     */
    private fun areAdjacentTMEs(t1: TextMapElement, t2: TextMapElement, list: MapList): Boolean {
        val index1 = list.indexOf(t1)
        val index2 = list.indexOf(t2)
        if (index2 == index1 + 1) return true
        for (i in index1 + 1 until index2) {
            if (list[i] !is PaintedWhiteSpaceElement) return false
        }
        return true
    }

    @JvmStatic
    fun removeLineBreakElements(list: MapList, start: Int, end: Int) {
        if (start in 0..end && end < list.size) {
            list.subList(start, end + 1).removeIf {
                if (it is LineBreakElement) {
                    it.node.detach()
                    true
                } else false
            }
        }
    }
}
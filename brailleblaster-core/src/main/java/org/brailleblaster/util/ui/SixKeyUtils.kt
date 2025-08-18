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
package org.brailleblaster.util.ui

import nu.xom.Element
import nu.xom.Node
import nu.xom.ParentNode
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBXUtils
import org.brailleblaster.bbx.findBlock
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.LineBreakElement
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.settings.UTDManager.Companion.getCellsPerLine
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.properties.EmphasisType
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utils.braille.BrailleUnicodeConverter.unicodeToAsciiLouis

/**
 * Add support for 6 key input
 */
object SixKeyUtils {
    const val SPACE = '\u2800'

    /**
     * Give this method a parent, and you will get back a formatted six key
     * element that contains line break elements and span tab elements to
     * maintain spacing
     *
     * @param inline
     * @param m
     * @param brailleText
     * @return
     */
    fun saveBraille(inline: Boolean, m: Manager, brailleText: String, unicode: Boolean): Element {
        val direct = BBX.INLINE.EMPHASIS.create(EmphasisType.NO_TRANSLATE)
        val array = brailleText.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var preceedingSpaceCount = 0
        var text = ""
        var beginning = false
        val cursorOffset = m.textView.caretOffset
        var tme: TextMapElement? = m.mapList.current
        if (tme!!.node != null && tme !is LineBreakElement) {
            val currentBlock: ParentNode = tme.node.findBlock()
            while (tme != null && currentBlock == m.getBlock(tme.node) && BBXUtils.getIndexInBlock(tme.node) != 0) {
                tme = m.mapList.getPrevious(m.mapList.indexOf(tme), true)
            }
            if (tme == null || tme.getStart(m.mapList) == cursorOffset) {
                beginning = true
            }
        } else {
            beginning = true
        }
        for (i in array.indices) {
            val temp = array[i]
            var j = 0
            while (j < temp.length) {
                var c = temp[j]
                if (j == 0 && c == SPACE && beginning) {
                    // get a space count to create a tab element
                    while (preceedingSpaceCount < temp.length - 1 && c == SPACE) {
                        preceedingSpaceCount++
                        c = temp[preceedingSpaceCount]
                    }
                    val tab = BBX.SPAN.TAB.create()
                    val cellsPerLine = getCellsPerLine(m)
                    if (preceedingSpaceCount + 1 > cellsPerLine) {
                        while (preceedingSpaceCount + 1 > cellsPerLine) {
                            /*
                             * RT 6916
                             */
                            preceedingSpaceCount -= cellsPerLine
                        }
                        if (direct.childCount != 0) {
                            val newline: Node = BBX.INLINE.LINE_BREAK.create()
                            direct.appendChild(newline)
                        }
                    }
                    val att = BBX.SPAN.TAB.ATTRIB_VALUE.newAttribute(preceedingSpaceCount + 1)
                    tab.addAttribute(att)
                    direct.appendChild(tab)
                    j = preceedingSpaceCount - 1
                    preceedingSpaceCount = 0
                } else {
                    text += c
                }
                j++
            }
            if (text.isNotEmpty()) {
                if (unicode) {
                    direct.appendChild(text)
                } else {
                    direct.appendChild(unicodeToAsciiLouis(text))
                }
            }
            text = ""
            if (i != array.size - 1 || !inline) {
                val newline: Node = BBX.INLINE.LINE_BREAK.create()
                direct.appendChild(newline)
            }
        }
        return direct
    }

    /**
     * Give this method the parent node created with six key braille, span tabs,
     * and line breaks , and it will return a string to put into a styled text
     * widget.
     *
     * @param brailleString
     * @return
     */
    fun formatPreviousImageDescription(brailleString: Node?): String {
        return FastXPath.descendant(brailleString).filter { node: Node? ->
            node is Text && XMLHandler.ancestorElementNot(node) {
                UTDElements.BRL.isA(it)
            } || BBX.INLINE.LINE_BREAK.isA(node) || BBX.SPAN.TAB.isA(node)
        }.joinToString(separator = "") { node ->
            if (node is Text) {
                node.value
            } else if (BBX.SPAN.TAB.isA(node)) {
                SPACE.toString().repeat(BBX.SPAN.TAB.ATTRIB_VALUE[node as Element] - 1)
            } else {
                "\n"
            }
        }
    }
}

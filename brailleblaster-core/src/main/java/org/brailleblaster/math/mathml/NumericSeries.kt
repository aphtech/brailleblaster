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
package org.brailleblaster.math.mathml

import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.bbx.BBXUtils
import org.brailleblaster.bbx.findBlock
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.views.wp.MathEditHandler.translateAndReplace
import org.brailleblaster.perspectives.mvc.XMLTextCaret
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.modules.views.DebugModule
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.util.Notify
import org.brailleblaster.util.Notify.notify

object NumericSeries {
    const val BRAILLE_DOT_5 = "\u2810"
    @JvmStatic
	fun toggleNumeric(m: Manager) {
        val selectedText = m.textView.selectionText
        if (selectedText.isEmpty()) {
            return
        }
        val selection = m.simpleManager.currentSelection
        val start = selection.start.node
        val end = selection.end.node
        val startIsUneditable = BBXUtils.isUneditable(start)
        val endIsUneditable = BBXUtils.isUneditable(end)
        if (startIsUneditable || endIsUneditable) {
            notify(Notify.GENERIC_UNEDITABLE, Notify.ALERT_SHELL_NAME)
            return
        }
        val toggleOn = !selectedText.contains(BRAILLE_DOT_5)
        val textArray = ArrayList<Node>()
        val mathArray = ArrayList<Node>()
        val startParent: Node = start.findBlock().parent
        val endParent: Node = end.findBlock().parent
        val startIsText = start is Text
        val endIsText = end is Text
        val startIsMath = MathModule.isMath(start)
        val endIsMath = MathModule.isMath(end)
        var mathStartIndex = 0
        var mathEndIndex = 0
        if (startIsText) {
            textArray.add(start)
        } else if (startIsMath) {
            val mapElement = m.mapList.getClosest(m.textView.selection.x, true)
            if (mapElement.node == start) {
                mathStartIndex = m.textView.selection.x - mapElement.getStart(m.mapList)
            }
            mathArray.add(start)
        }
        if (endIsText) {
            addNotAlreadyThere(textArray, end)
        } else if (endIsMath) {
            val mapElement = m.mapList.getClosest(m.textView.selection.y, true)
            if (mapElement.node == end) {
                mathEndIndex = m.textView.selection.y - mapElement.getStart(m.mapList)
            }
            addNotAlreadyThere(mathArray, end)
        }
        var current = start
        while (current !== end) {
            if (MathModule.isMath(current)) {
                addNotAlreadyThere(mathArray, MathModule.getMathParent(current))
            } else {
                addNotAlreadyThere(textArray, current)
            }
            current = XMLHandler.followingVisitor(current) { n: Node? ->
                (n is Text
                        && XMLHandler.ancestorElementNot(n) { n2: Element? -> n2 != null && UTDElements.BRL.isA(n2) })
            }
        }
        for (i in textArray.indices) {
            val n = textArray[i]
            val nodeLength = n.value.length
            val currentStartIndex = if (startIsText) (selection.start as XMLTextCaret).offset else 0
            val currentEndIndex = if (endIsText) (selection.end as XMLTextCaret).offset else nodeLength
            if (n == start) {
                if (textArray.size == 1) {
                    toggleText(currentStartIndex, currentEndIndex, n, toggleOn)
                } else {
                    toggleText(currentStartIndex, nodeLength, n, toggleOn)
                }
            } else if (n == end) {
                toggleText(0, currentEndIndex, n, toggleOn)
            } else {
                toggleText(0, nodeLength, n, toggleOn)
            }
        }
        for (i in mathArray.indices) {
            val n = mathArray[i]
            val nodeLength = MathModule.getMathText(n).length
            if (n == start) {
                if (mathArray.size == 1) {
                    toggleMath(mathStartIndex, mathEndIndex, n, toggleOn)
                } else {
                    toggleMath(mathStartIndex, nodeLength, n, toggleOn)
                }
            } else if (n == end) {
                toggleMath(0, mathEndIndex, n, toggleOn)
            } else {
                toggleMath(0, nodeLength, n, toggleOn)
            }
        }
        m.simpleManager.dispatchEvent(ModifyEvent(Sender.MATH, true, startParent, endParent))
    }

    private fun addNotAlreadyThere(array: ArrayList<Node>, n: Node) {
        if (!array.contains(n)) {
            array.add(n)
        }
    }

    private fun toggleMath(start: Int, end: Int, n: Node, on: Boolean) {
        if (DebugModule.enabled) {
            val asciiMath = MathModule.getMathText(n)
            val b = asciiMath.take(start)
            var s = asciiMath.substring(start, end)
            val a = asciiMath.drop(end)
            s = if (on) {
                s.replace(" ".toRegex(), BRAILLE_DOT_5)
            } else {
                s.replace(BRAILLE_DOT_5.toRegex(), " ")
            }
            val newValue = b + s + a
            translateAndReplace(MathSubject(newValue), n)
        }
    }

    private fun toggleText(start: Int, end: Int, n: Node, on: Boolean) {
        val b = (n as Text).value.substring(0, start)
        var s = n.value.substring(start, end)
        val a = n.value.substring(end)
        s = if (on) {
            s.replace(" ".toRegex(), BRAILLE_DOT_5)
        } else {
            s.replace(BRAILLE_DOT_5.toRegex(), " ")
        }
        n.value = b + s + a
    }
}
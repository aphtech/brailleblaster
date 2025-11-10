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
import nu.xom.ParentNode
import org.brailleblaster.bbx.BBX
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.perspectives.braille.mapping.maps.MapList
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.messages.TabInsertionMessage
import org.brailleblaster.perspectives.braille.viewInitializer.ViewInitializer
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.util.LINE_BREAK
import org.brailleblaster.utils.xom.TextSplitter

class TabInsertionHandler(manager: Manager?, vi: ViewInitializer?, list: MapList?) : Handler(
    manager!!, vi!!, list!!
) {
    fun insertTab(m: TabInsertionMessage) {
        insertTab(m.tabValue, m.xIndex, m.currentElement)
    }

    fun adjustTab(m: TabInsertionMessage) {
        adjustTab(m.tabValue, m.parent)
    }

    private fun insertTab(tabValue: Int, xInd: Int, currentElement: TextMapElement?) {
        val xIndex = xInd - calculateLineBreaks(currentElement, xInd)

        val split = TextSplitter(xIndex, currentElement!!.node)

        val parent: ParentNode = currentElement.nodeParent

        var curIndex = parent.indexOf(currentElement.node)
        parent.insertChild(split.last, curIndex)

        if (xIndex > 0) parent.insertChild(split.first, curIndex)

        parent.removeChild(currentElement.node)

        val tab = BBX.SPAN.TAB.create()
        BBX.SPAN.TAB.ATTRIB_VALUE[tab] = tabValue

        curIndex = parent.indexOf(split.last)
        parent.insertChild(tab, curIndex)


        // Find the nearest translation block for retranslation
        val brlDoc = manager.document
        val transBlock = brlDoc.engine.findTranslationBlock(parent) as Element

        reformat(transBlock, true)
    }

    private fun adjustTab(value: Int, t: Element?) {
        val e = findTab(t)
        if (e != null) {
            val num = BBX.SPAN.TAB.ATTRIB_VALUE[e] + value
            BBX.SPAN.TAB.ATTRIB_VALUE[e] = num


            //only translate element, then refresh for performance
            manager.simpleManager.dispatchEvent(ModifyEvent(Sender.TAB, listOf(e), true))
        }
    }

    fun removeTab(m: TabInsertionMessage) {
        val e = m.tab
        if (e != null) {
            val p = e.parent
            p.removeChild(e)
            reformat(p, false)
        }
    }

    private fun findTab(e: Element?): Element? {
        if (BBX.SPAN.TAB.isA(e)) {
            return e
        }
        val els = e!!.childElements

        for (i in 0 until els.size()) if (BBX.SPAN.TAB.isA(els[i])) return els[i]

        return null
    }

    private fun calculateLineBreaks(t: TextMapElement?, pos: Int): Int {
        val text = manager.text.view.getTextRange(t!!.getStart(list), pos)
        return text.length - text.replace(LINE_BREAK, "").length
    }
}

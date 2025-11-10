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
package org.brailleblaster.perspectives.mvc.events

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.eventQueue.EventFrame
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.utils.xml.BB_NS
import org.brailleblaster.utils.xml.UTD_NS

class Queuer(val manager: Manager) {
    fun handleEvent(f: EventFrame, sender: Sender) {
        val modNodes: MutableList<Node> = mutableListOf()

        var pos: Int? = null
        var sectionIndex: Int? = null
        while (true) {
            val event = f.peek()
            if (event is ModularEvent) {
                f.pop()
                val e = replaceSection(event.element, event.indexes)
                replaceMaps(event.getStyleMap(), event.getActionMap())
                modNodes.add(e)
                sectionIndex = event.sectionIndex
                pos = event.textOffset
            } else {
                break
            }
        }

        val ev = ModifyEvent(sender, modNodes, false)
        if (sectionIndex != null && (sender == Sender.UNDO_QUEUE || sender == Sender.REDO_QUEUE)) {
            ev.setQueueData(sectionIndex, pos!!)
        }

        manager.simpleManager.dispatchEvent(ev)

        if (pos != null) {
            if (manager.viewInitializer.startIndex != sectionIndex) manager.buffer(sectionIndex!!)

            if (pos > manager.text.view.charCount) pos = manager.text.view.charCount
            else if (manager.text.inLineBreak(pos)) pos++

            manager.text.setCurrentElement(pos)
        }
    }

    private fun replaceSection(e: Element, indexes: List<Int?>): Element {
        val currentSection = getParent(indexes)
        currentSection!!.parent.replaceChild(currentSection, e)
        return e
    }

    private fun replaceMaps(styleMap: Element?, actionMap: Element?) {
        val root = manager.document.rootElement
        val head = root.getFirstChildElement("head", BB_NS) ?: return

        val oldStyleMap = head.getFirstChildElement("styleMap", UTD_NS)
        if (styleMap != null) {
            if (oldStyleMap != null) oldStyleMap.parent.replaceChild(oldStyleMap, styleMap)
            else head.appendChild(styleMap)
        } else {
            if (oldStyleMap != null) head.removeChild(oldStyleMap)
        }

        val oldActionMap = head.getFirstChildElement("actionMap", UTD_NS)
        if (actionMap != null) {
            if (oldActionMap != null) oldActionMap.parent.replaceChild(oldActionMap, actionMap)
            else head.appendChild(actionMap)
        } else {
            if (oldActionMap != null) head.removeChild(oldActionMap)
        }

        manager.document.settingsManager.reloadMapsFromDoc()
    }

    fun getParent(indexes: List<Int?>): Element? {
        var e = if (indexes.isNotEmpty()) manager.document.doc.document.getChild(indexes[0]!!) as Element else null

        for (i in 1 until indexes.size) e = e!!.getChild(indexes[i]!!) as Element

        return e
    }
}

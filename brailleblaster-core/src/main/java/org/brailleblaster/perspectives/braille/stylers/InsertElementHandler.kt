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
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBX.SubType
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.document.BrailleDocument
import org.brailleblaster.perspectives.braille.mapping.maps.MapList
import org.brailleblaster.perspectives.braille.messages.InsertNodeMessage
import org.brailleblaster.perspectives.braille.viewInitializer.ViewInitializer
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.properties.EmphasisType
import org.brailleblaster.utils.UTD_NS

class InsertElementHandler(manager: Manager, vi: ViewInitializer?, list: MapList?) : Handler(manager, vi!!, list!!) {
    val doc: BrailleDocument = manager.document

    fun insertElement(m: InsertNodeMessage) {
        val e = if (m.atStart) insertElementAtBeginning(m.subType, m.getValue("beginNewElement") as String?)
        else insertElementAtEnd(m.subType, m.getValue("beginNewElement") as String?)

        if (m.attributes != null) {
            for (atr in m.attributes!!) e.addAttribute(atr)
        }

        if (m.text != null) {
            (e.getChild(0) as Text).value = m.text
        }

        if (isTRNote(e)) {
            XMLHandler.wrapNodeWithElement(
                e.getChild(0),
                BBX.INLINE.EMPHASIS.create(EmphasisType.TRANS_NOTE)
            )
        }

        reformat(e, true)
    }

    private fun insertElementAtBeginning(subType: SubType, beginNewElement: String?): Element {
        val e = if (list.currentIndex > 0 && list.current.getStart(list) != 0) doc.insertElement(
            list.current,
            0,
            subType,
            true
        )
        else doc.insertElement(list.current, 1, subType, true)

        if (beginNewElement != null) list[list.currentIndex].nodeParent.insertChild(Element(beginNewElement), 0)

        return e
    }

    private fun insertElementAtEnd(subType: SubType, beginNewElement: String?): Element {
        val orig = list.current.nodeParent
        val e = doc.insertElement(list.current, 1, subType, false)

        if (beginNewElement != null) orig.appendChild(Element(beginNewElement))

        return e
    }

    private fun isTRNote(e: Element): Boolean {
        val atr = e.getAttribute("overrideStyle", UTD_NS)
        if (atr != null) return atr.value == "TRNote"

        return false
    }
}

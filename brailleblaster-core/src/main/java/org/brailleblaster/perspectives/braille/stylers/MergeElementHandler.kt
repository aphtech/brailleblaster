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
import org.brailleblaster.bbx.findBlock
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.document.BrailleDocument
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.XMLTextCaret
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.events.XMLCaretEvent

class MergeElementHandler {
    var document: BrailleDocument? = null

    companion object {
        @JvmStatic
		fun merge(t1: TextMapElement, t2: TextMapElement, manager: Manager) {
            val mergeTo = t1.node.findBlock()
            val merging = t2.node.findBlock()
            val t2Length = t2.node.value.length
            val list = manager.mapList

            val mergeList1 = list.findTextMapElements(list.indexOf(t1), mergeTo)
            val mergeList2 = list.findTextMapElements(list.indexOf(t2), merging)
            saveOriginalElements(mergeTo, mergeList1[0], merging, mergeList2[0], manager)
            manager.stopFormatting()
            val mergedElement = manager.document.mergeElements(mergeTo, merging)
            manager.simpleManager.dispatchEvent(ModifyEvent(Sender.HANDLER, true, mergedElement))
            if (mergedElement.document === manager.doc) {
                val current = manager.text.currentElement
                if (current != null && current.node.findBlock() === mergedElement && current.node is Text) {
                    manager.simpleManager.dispatchEvent(
                        XMLCaretEvent(
                            Sender.HANDLER,
                            XMLTextCaret((current.node as Text), current.node.value.length - t2Length)
                        )
                    )
                }
            }
        }

        private fun saveOriginalElements(
            mergeTo: Element,
            t1: TextMapElement,
            merging: Element,
            t2: TextMapElement,
            manager: Manager
        ) {
            val pos = manager.text.view.caretOffset
            manager.text.setCurrentElement(t1.getStart(manager.mapList))
            manager.text.setCurrentElement(pos)
        }
    }
}

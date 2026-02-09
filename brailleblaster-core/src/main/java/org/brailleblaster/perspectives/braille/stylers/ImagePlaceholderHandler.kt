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

import nu.xom.Attribute
import org.brailleblaster.bbx.BBX
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.WhiteSpaceElement
import org.brailleblaster.perspectives.braille.mapping.maps.MapList
import org.brailleblaster.perspectives.braille.messages.InsertNodeMessage
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.viewInitializer.ViewInitializer
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import java.util.*

class ImagePlaceholderHandler(manager: Manager?, vi: ViewInitializer?, list: MapList?) : Handler(
    manager!!, vi!!, list!!
) {
    fun insertNewImagePlaceholder(lines: Int?, imagePath: String?, altText: String?) {
        var atStart = false
        var atEnd = false
        val posList: List<Int>

        if (lines == null || imagePath == null || altText.isNullOrEmpty()) return

        if (list.current is WhiteSpaceElement) {
            val newPlaceholder = BBX.BLOCK.IMAGE_PLACEHOLDER.create()
            newPlaceholder.addAttribute(BBX.BLOCK.IMAGE_PLACEHOLDER.ATTRIB_SKIP_LINES.newAttribute(lines))
            newPlaceholder.addAttribute(BBX.BLOCK.IMAGE_PLACEHOLDER.ATTRIB_IMG_PATH.newAttribute(imagePath))
            newPlaceholder.addAttribute(BBX.BLOCK.IMAGE_PLACEHOLDER.ATTRIB_ALT_TEXT.newAttribute(altText))

            val wt = WhitespaceTransformer(manager)
            wt.transformWhiteSpace(list.current as WhiteSpaceElement, newPlaceholder)
        } else {
            posList = list.findTextMapElementRange(
                list.currentIndex,
                list.current.nodeParent
            )
            val caretPos = text.view.caretOffset
            if (caretPos < list[posList[0]].getStart(list) && caretPos > list[posList[posList.size - 1]].getEnd(list)) {
                atEnd = true
            } else {
                val startDistance = caretPos - list[posList[0]].getStart(list)
                val endDistance = list[posList[posList.size - 1]].getEnd(list) - caretPos

                if (startDistance <= endDistance) atStart = true
                else atEnd = true
            }

            val attrs = LinkedList<Attribute>()
            attrs.add(BBX.BLOCK.IMAGE_PLACEHOLDER.ATTRIB_SKIP_LINES.newAttribute(lines))
            attrs.add(BBX.BLOCK.IMAGE_PLACEHOLDER.ATTRIB_IMG_PATH.newAttribute(imagePath))
            attrs.add(BBX.BLOCK.IMAGE_PLACEHOLDER.ATTRIB_ALT_TEXT.newAttribute(altText))

            val m = InsertNodeMessage(atStart, atEnd, BBX.BLOCK.IMAGE_PLACEHOLDER, null, attrs)
            val ieh = InsertElementHandler(manager, vi, list)
            ieh.insertElement(m)
            //TODO: Per bug #67524, ensure lines after placeholder return to default style
            //This would be a good place for alt-text to show. And it should with NIMAS files too (#30992)

        }
    }

    //This method assumes the current map element is an image placeholder
    //It also assumes that the attributes to be updated are passed in, and that null means "do not change"
    //Worst case is it overrides each attribute with the same value again.
    fun updateImagePlaceholder(lines: Int?, imagePath: String?, altText: String?) {
        val current = list.current.nodeParent
        if (BBX.BLOCK.IMAGE_PLACEHOLDER.isA(current)) {
            if (lines != null)
                current.addAttribute(BBX.BLOCK.IMAGE_PLACEHOLDER.ATTRIB_SKIP_LINES.newAttribute(lines))
            if (imagePath != null)
                current.addAttribute(BBX.BLOCK.IMAGE_PLACEHOLDER.ATTRIB_IMG_PATH.newAttribute(imagePath))
            if (altText != null)
                current.addAttribute(BBX.BLOCK.IMAGE_PLACEHOLDER.ATTRIB_ALT_TEXT.newAttribute(altText))
            //println("Image updated: ${current.toXML()}")
            manager.simpleManager.dispatchEvent(ModifyEvent(Sender.TEXT, true, current))
        }
    }

}

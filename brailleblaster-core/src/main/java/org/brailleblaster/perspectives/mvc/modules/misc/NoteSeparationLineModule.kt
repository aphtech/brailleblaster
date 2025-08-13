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
package org.brailleblaster.perspectives.mvc.modules.misc

import nu.xom.Attribute
import nu.xom.ParentNode
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.findBlock
import org.brailleblaster.perspectives.braille.mapping.elements.WhiteSpaceElement
import org.brailleblaster.perspectives.braille.mapping.interfaces.Uneditable
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.stylers.WhitespaceTransformer
import org.brailleblaster.perspectives.mvc.XMLTextCaret
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.tools.MenuToolModule
import org.brailleblaster.utd.properties.EmphasisType
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import java.util.*

object NoteSeparationLineModule : MenuToolModule {
    private val localeHandler = getDefault()
     override val topMenu = TopMenu.INSERT
     override val title = localeHandler["&NoteSeparationLine"]
     override fun onRun(bbData: BBSelectionData) {
         if (!bbData.manager.isEmptyDocument) {
             val noteSep = BBX.BLOCK.STYLE.create("Note Separation Line")
             val span = BBX.INLINE.EMPHASIS.create(EnumSet.of(EmphasisType.NO_TRANSLATE))
             noteSep.appendChild(span)
             span.appendChild("\"333333")
             when (bbData.manager.mapList.current) {
                 is Uneditable -> {
                     (bbData.manager.mapList.current as Uneditable).blockEdit(bbData.manager)
                 }

                 is WhiteSpaceElement -> {
                     span.addAttribute(Attribute("class", "noteSepNoBlank"))
                     val wtf = WhitespaceTransformer(bbData.manager)
                     wtf.transformWhiteSpace(bbData.manager.mapList.current as WhiteSpaceElement, noteSep)
                 }

                 else -> {
                     //Append a new element for the note sep line as a sibling of the current element to the left
                     var currNode = bbData.manager.text.currentElement!!.node
                     /** The reason why it looks like it's separating a big paragraph is because that block contains inline elements
                      * If it does, the recognized node will not be the entire block but only a part of it
                      * Note separation line will then be added after that specific node and not after the block
                      * But you need a special case for page indicators inserted in the middle of a block which registers as a <span>
                      * The note separation line should be added after the text and before the indicator</span> */
                     /** The reason why it looks like it's separating a big paragraph is because that block contains inline elements
                      * If it does, the recognized node will not be the entire block but only a part of it
                      * Note separation line will then be added after that specific node and not after the block
                      * But you need a special case for page indicators inserted in the middle of a block which registers as a <span>
                      * The note separation line should be added after the text and before the indicator</span> */
                     /** The reason why it looks like it's separating a big paragraph is because that block contains inline elements
                      * If it does, the recognized node will not be the entire block but only a part of it
                      * Note separation line will then be added after that specific node and not after the block
                      * But you need a special case for page indicators inserted in the middle of a block which registers as a <span>
                      * The note separation line should be added after the text and before the indicator</span> */
                     if ((bbData.manager.mapList.getNext(true) != null
                                 && !BBX.SPAN.PAGE_NUM.isA(bbData.manager.mapList.getNext(true).node)) //For end of document
                         || !BBX.BLOCK.isA(currNode)
                     ) {
                         //To prevent the aforementioned from happening:
                         currNode = currNode.findBlock()
                     }
                     if (currNode == null) {
                         span.addAttribute(Attribute("class", "noteSepNoBlank"))
                         currNode = bbData.manager.mapList.getPrevious(true).nodeParent
                     }
                     val index: Int
                     var offset: Int
                     offset = if (bbData.manager.simpleManager.currentSelection.end is XMLTextCaret) {
                         (bbData.manager.simpleManager.currentSelection.end as XMLTextCaret).offset
                     } else {
                         bbData.manager.simpleManager.currentSelection.end.node.value.length
                     }
                     if (offset == currNode!!.value.length + 1) { // RT 6783
                         bbData.manager.splitElement()
                         currNode = bbData.manager.text.currentElement!!.node
                     } else {
                         offset = currNode.value.length + 1
                         bbData.manager.setTextCaret(offset)
                         bbData.manager.textView.update()
                         bbData.manager.splitElement()
                     }
                     val currParent: ParentNode = currNode!!.parent
                     index = currParent.indexOf(currNode)
                     currParent.insertChild(noteSep, index + 1)
                     bbData.manager.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, true, currParent))
                 }
             }
         } else {
             bbData.manager.notify(localeHandler["emptyDocMenuWarning"])
         }
     }
}
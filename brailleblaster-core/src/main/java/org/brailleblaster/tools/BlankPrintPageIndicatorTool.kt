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
package org.brailleblaster.tools

import nu.xom.Attribute
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.utils.localization.LocaleHandler
import org.brailleblaster.perspectives.braille.mapping.elements.WhiteSpaceElement
import org.brailleblaster.perspectives.braille.mapping.interfaces.Uneditable
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.stylers.WhitespaceTransformer
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu

object BlankPrintPageIndicatorTool : MenuToolModule {
    private val localeHandler = LocaleHandler.getDefault()
    override val topMenu: TopMenu = TopMenu.INSERT
    override val title: String = "Blank Print Page Indicator"
    override fun onRun(bbData: BBSelectionData) {
        if (!bbData.manager.isEmptyDocument && !bbData.manager.documentIsOnlyNewlines()) {
            val parent = BBX.BLOCK.PAGE_NUM.create()
            parent.appendChild(" ")
            parent.addAttribute(Attribute("page", "bbAdded"))
            when (val currentElement = bbData.manager.mapList.current) {
                is Uneditable -> {
                    currentElement.blockEdit(bbData.manager)
                }

                is WhiteSpaceElement -> {
                    val transformer = WhitespaceTransformer(bbData.manager)
                    transformer.transformWhiteSpace(currentElement, parent)
                }

                else -> {
                    var currNode = bbData.manager.text.currentElement!! .node
                    var currParent = currNode.parent
                    if (currNode is Text) {
                        currNode = currParent
                        currParent = currParent.parent
                    }
                    val index = currParent.indexOf(currNode)
                    currParent.insertChild(parent, index + 1)
                    bbData.manager.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, true, currParent))
                }
            }
        } else {
            bbData.manager.notify(localeHandler["emptyDocMenuWarning"])
        }
    }
}
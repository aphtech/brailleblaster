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

import nu.xom.Element
import nu.xom.Node
import nu.xom.ParentNode
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.findBlock
import org.brailleblaster.exceptions.EditingException
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.AdjustLocalStyleMessage
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.XMLTextCaret
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.utd.Style
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler2
import org.brailleblaster.util.Utils

object LineBreakTool : MenuToolModule {
    override val topMenu: TopMenu = TopMenu.EDIT
    override val title: String = "Line Break\tShift + Enter"
    override fun onRun(bbData: BBSelectionData) {
        insertInlineLineBreak(bbData.manager)
    }
    fun insertInlineLineBreak(manager: Manager) {
        val parent: ParentNode?
        try {
            manager.checkForUpdatedViews() //RT 6080
            val currentNode = manager.simpleManager.currentSelection.start.node
            val currentTme = manager.mapList.current

//			RT 4901 work around for shift + enter before spaces
            while (currentTme.text.length > manager.textView.caretOffset && currentTme.text[manager.textView.caretOffset] == ' ') {
                manager.textView.caretOffset += 1
                manager.updateFormatting()
            }
            parent = currentNode.findBlock()
            val lineBreak = BBX.INLINE.LINE_BREAK.create()
            val utdMan = manager.document.settingsManager
            utdMan.applyStyleWithOption(
                manager.getStyle(lineBreak) as Style,
                Style.StyleOption.LINES_BEFORE,
                1,
                lineBreak
            )
            //Don't do nothing inside a table
            if (BBX.CONTAINER.TABLE.isA(currentNode)) {
                return
            }
            if (currentNode is Text) {
                // See RT 6399. A line break at the beginning of the document causes an exception and doesn't make sense
                // anyway because it has the same behavior as enter
                if (manager.mapList[0].node != null && manager.mapList[0].node == currentNode && (manager.simpleManager.currentSelection.start as XMLTextCaret).offset == 0) {
                    return
                }
                // Empty string-> don't split, don't process, give error
                // Corey: Blank documents contain an empty text node. Don't think we
                // need to throw an exception
                if (currentNode.toXML().isEmpty()) {
                    // throw new IndexOutOfBoundsException();
                    return
                } else {
                    val offset = (manager.simpleManager.currentSelection.end as XMLTextCaret).offset
                    var index = parent.indexOf(currentNode)
                    /*
                 * if the offset is 0 or node.length(), splitTextNode will return an empty node
                 */if (index == 0 && offset == 0) {
                        utdMan.applyStyleWithOption(
                            manager.getStyle(lineBreak) as Style,
                            Style.StyleOption.LINES_BEFORE,
                            2,
                            lineBreak
                        )
                        Utils.insertChildCountSafe(parent, lineBreak, index)
                    } else if (offset == 0) {
                        val previousNode = FastXPath.precedingAndSelf(currentNode)
                            .stream()
                            .filter { node: Node? -> BBX.INLINE.LINE_BREAK.isA(node) }
                            .findFirst()
                            .orElse(null)
                        if (previousNode != null && BBX.INLINE.LINE_BREAK.isA(previousNode) && previousNode.findBlock() == parent
                        ) {
                            val style = manager.getStyle(previousNode)
                            if (style != null) {
                                try {
                                    manager.dispatch(
                                        AdjustLocalStyleMessage.AdjustLinesMessage(
                                            previousNode as Element,
                                            true,
                                            style.linesBefore + 1
                                        )
                                    )
                                } catch (e: RuntimeException) {
                                    throw EditingException("An error occurred while adding Lines Before.", e)
                                }
                            }
                        } else {
                            Utils.insertChildCountSafe(parent, lineBreak, index)
                        }
                    } else if (offset == currentNode.value.length) {
                        val nextNode = FastXPath.followingAndSelf(currentNode)
                            .stream()
                            .filter { node: Node? -> BBX.INLINE.LINE_BREAK.isA(node) }
                            .findFirst()
                            .orElse(null)
                        if (nextNode != null && BBX.INLINE.LINE_BREAK.isA(nextNode) && nextNode.findBlock() == parent) {
                            val style = manager.getStyle(nextNode)
                            if (style != null) {
                                try {
                                    manager.dispatch(
                                        AdjustLocalStyleMessage.AdjustLinesMessage(
                                            nextNode as Element,
                                            true,
                                            style.linesBefore + 1
                                        )
                                    )
                                } catch (e: RuntimeException) {
                                    throw EditingException("An error occurred while adding Lines Before.", e)
                                }
                            }
                        } else {
                            Utils.insertChildCountSafe(parent, lineBreak, index + 1)
                        }
                    } else {
                        val splitTextNode =
                            XMLHandler2.splitTextNode(
                                currentNode,
                                offset
                            )
                        if (splitTextNode.size < 2) {
                            println("Split text node in the middle of node, expected 2 nodes but got less.")
                            return
                        }
                        index = splitTextNode[0].parent.indexOf(splitTextNode[1])
                        Utils.insertChildCountSafe(splitTextNode[0].parent, lineBreak, index)
                    }
                }
            }
        } catch (ex: RuntimeException) {
            throw EditingException("An error occurred while adding an inline line break", ex)
        }
        manager.stopFormatting()
        manager.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, true, parent))
    }
}

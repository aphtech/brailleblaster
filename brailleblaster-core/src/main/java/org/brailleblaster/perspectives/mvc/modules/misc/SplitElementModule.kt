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

import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.exceptions.EditingException
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.BBSimpleManager
import org.brailleblaster.perspectives.mvc.BBSimpleManager.SimpleListener
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.XMLTextCaret
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.utd.Style
import org.brailleblaster.utd.utils.stripUTDRecursive
import org.slf4j.LoggerFactory

class SplitElementModule : SimpleListener {
    //We don't want to actually set Enter as the accelerator but we want this feature documented,
    //so put the shortcut in the menu name
    //	private final static String MENU_ITEM_NAME = "Split Element\tEnter";
    //	private final static int ACCELERATOR = 0;
    override fun onEvent(event: SimpleEvent) {
//		if(event instanceof BuildMenuEvent){
//			MenuManager.addMenuItem(TopMenu.EDIT, MENU_ITEM_NAME, ACCELERATOR, e -> splitElement(e.manager.getSimpleManager()), null);
//		}
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SplitElementModule::class.java)

        /*
	 * Returns true when split was successful
	 */
		@JvmStatic
		fun splitElement(simpleManager: BBSimpleManager): Boolean {
            val modifiedNodes: List<Node>? = try {
                val selection = simpleManager.currentSelection
                //If something was selected or the selection is not text, do nothing
                if (selection.isTextNoSelection) {
                    val currentCaret = selection.start as XMLTextCaret
                    val textNode = currentCaret.node
                    val textIndex = currentCaret.offset
                    logger.debug("Splitting text node: " + textNode.value + " at offset: " + textIndex)

                    //Split the original text node into two text nodes
                    val newText = textNode.value.substring(textIndex)
                    val oldText = textNode.value.substring(0, textIndex)
                    val newTextNode = Text(newText)
                    textNode.value = oldText

                    //Map out the path of indexes from the text node to the block
                    val pathToBlock: MutableList<Int> = ArrayList()
                    var curNode: Node = textNode
                    while (!BBX.BLOCK.isA(curNode)) {
                        val newParent = curNode.parent as Element
                        newParent.stripUTDRecursive()
                        pathToBlock.add(newParent.indexOf(curNode))
                        curNode = newParent
                    }
                    var newNodeCopy: Node = newTextNode
                    var origNode: Node = textNode
                    for (integer in pathToBlock) {
                        //Copy the parent and append the previous copy to this new parent
                        origNode = origNode.parent
                        val newCopy = if (BBX.BLOCK.isA(origNode)) copyBlockWithoutStyleOptions(
                            origNode as Element,
                            simpleManager
                        ) else (origNode.copy() as Element)
                        newCopy.removeChildren()
                        newCopy.appendChild(newNodeCopy)
                        newNodeCopy = newCopy

                        //Detach each child that comes after the split text node and attach
                        //it to the new parent.
                        var curIndex = integer + 1
                        while (curIndex < origNode.childCount) {
                            val childToMove = origNode.getChild(curIndex)
                            childToMove.detach()
                            newNodeCopy.appendChild(childToMove)
                            curIndex--
                            curIndex++
                        }
                    }

                    //Insert copied block adjacent to the original block
                    val blockParent = origNode.parent
                    blockParent.insertChild(newNodeCopy, blockParent.indexOf(origNode) + 1)

                    //If we made any empty text nodes, detach them
                    val modifiedNodes: MutableList<Node> = mutableListOf()
                    if (newNodeCopy.document != null) {
                        modifiedNodes.add(newNodeCopy)
                    }
                    if (origNode.document != null) {
                        modifiedNodes.add(origNode)
                    }
                    modifiedNodes
                } else {
                    null
                }
            } catch (e: RuntimeException) {
                throw EditingException("Error splitting node", e)
            }
            if (modifiedNodes != null) {
                simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, modifiedNodes, true))
                return true
            }
            return false
        }

        private fun copyBlockWithoutStyleOptions(block: Element, simpleManager: BBSimpleManager): Element {
            val style = simpleManager.utdManager.engine.getStyle(block) as Style?
            val baseStyle = style?.baseStyle
            if (baseStyle != null) {
                return if (baseStyle.name == Style().name) {
                    //getBaseStyleName returned the default style, so this style had no style options
                    //applied to it. Just use the style name
                    createBlockCopy(block, style.name)
                } else {
                    //TODO: Hard-coded internal category
                    if (baseStyle.id.contains("internal")) {
                        //This style derives from an internal style like Heading or List Item,
                        //so don't use the base style
                        createBlockCopy(block, style.name)
                    } else createBlockCopy(block, baseStyle.name)
                    //This style had some sort of style option applied to it. Find its base style
                    //so that the new block does not have those style options
                }
            }
            logger.warn("No style found for block: " + block.toXML())
            return block.copy()
        }

        private fun createBlockCopy(block: Element, styleName: String): Element {
            return if (BBX.BLOCK.DEFAULT.isA(block)) BBX.BLOCK.DEFAULT.create() else BBX.BLOCK.STYLE.create(styleName)
        }
    }
}
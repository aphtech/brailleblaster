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

import nu.xom.Node
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.findBlock
import org.brailleblaster.perspectives.braille.mapping.elements.ReadOnlyTableTextMapElement
import org.brailleblaster.perspectives.braille.mapping.elements.TableTextMapElement
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.tools.MenuToolModule
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.util.FormUIUtils
import org.eclipse.swt.SWT

object PreviousElementTool : MenuToolModule {
    override val topMenu: TopMenu = TopMenu.NAVIGATE
    override val title: String = "Previous Element"
    override val accelerator: Int = SWT.CTRL or SWT.ARROW_UP
    override fun onRun(bbData: BBSelectionData) {
        //		XMLSelection currentSelection = m.getSimpleManager().getCurrentSelection();
        val currentNode = bbData.manager.simpleManager.currentSelection.start.node
        var currentBlock: Node? = currentNode.findBlock()
        var newTME = bbData.manager.mapList.findNode(currentNode)
        if (newTME == null) {
            while (newTME == null) {
                newTME = bbData.manager.mapList.getPrevious(true)
            }
            currentBlock = newTME.node
        }
        if (!newTME.isFullyVisible) bbData.manager.decrementView()
        if (currentBlock != null) {
            while (newTME != null && currentBlock == newTME.node.findBlock()) {
                newTME = bbData.manager.mapList.getPrevious(bbData.manager.mapList.indexOf(newTME), true)
            }
        }
        var targetBlock: Node? = null
        if (newTME != null) targetBlock = newTME.node.findBlock()
        if (targetBlock != null) {
            if (BBX.BLOCK.TABLE_CELL.isA(targetBlock)) {
                targetBlock =
                    XMLHandler.ancestorVisitor(targetBlock.parent) { node: Node? -> BBX.CONTAINER.TABLE.isA(node) }
                newTME = bbData.manager.mapList.findNode(targetBlock!!.getChild(0))
            } else {
                while (newTME != null && targetBlock == newTME.node.findBlock()) {
                    newTME = bbData.manager.mapList.getPrevious(bbData.manager.mapList.indexOf(newTME), true)
                }
                newTME = bbData.manager.mapList.getNext(bbData.manager.mapList.indexOf(newTME), true)
            }
        }
        if (BBX.CONTAINER.TABLE.isA(currentNode)) {
            if (bbData.manager.textView.getLineAtOffset(bbData.manager.textView.caretOffset) == 1) {
                bbData.manager.decrementView()
            }
            while (newTME is TableTextMapElement || newTME is ReadOnlyTableTextMapElement) {
                newTME = bbData.manager.mapList.getPrevious(bbData.manager.mapList.indexOf(newTME), true)
            }
        }
        if (newTME != null) {
            bbData.manager.setTextCaret(newTME.getStart(bbData.manager.mapList))
            FormUIUtils.scrollViewToCursor(bbData.manager.textView)
        }
    }
}
object NextElementTool : MenuToolModule {
    override val topMenu: TopMenu = TopMenu.NAVIGATE
    override val title: String = "Next Element"
    override val accelerator: Int = SWT.CTRL or SWT.ARROW_DOWN
    override fun onRun(bbData: BBSelectionData) {
        //		XMLSelection currentSelection = m.getSimpleManager().getCurrentSelection();
        val currentNode = bbData.manager.simpleManager.currentSelection.start.node
        var currentBlock: Node? = currentNode.findBlock()
        var newTME = bbData.manager.mapList.findNode(currentNode)
        if (newTME == null) {
            while (newTME == null) {
                newTME = bbData.manager.mapList.getNext(true)
            }
            currentBlock = newTME.node
        }
        if (!newTME.isFullyVisible) bbData.manager.incrementView()
        if (currentBlock != null) {
            while (newTME != null && currentBlock == newTME.node.findBlock()) {
                newTME = bbData.manager.mapList.getNext(bbData.manager.mapList.indexOf(newTME), true)
            }
        }
        if (BBX.CONTAINER.TABLE.isA(currentNode)) {
            while (newTME is TableTextMapElement || newTME is ReadOnlyTableTextMapElement) {
                newTME = bbData.manager.mapList.getNext(bbData.manager.mapList.indexOf(newTME), true)
            }
        }
        if (newTME == null && bbData.manager.getSection(currentNode) < bbData.manager.sectionList.size - 1) {
            bbData.manager.incrementView()
            while (newTME == null) {
                newTME = bbData.manager.mapList.getNext(true)
            }
        }
        if (newTME != null) {
            bbData.manager.text.setCurrentElement(newTME.getStart(bbData.manager.mapList))
            FormUIUtils.scrollViewToCursor(bbData.manager.textView)
        }
    }
}

object ElementNavigationModule {
    val tools = listOf(PreviousElementTool, NextElementTool)
}
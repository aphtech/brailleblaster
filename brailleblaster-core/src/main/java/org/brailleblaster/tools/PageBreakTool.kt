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

import nu.xom.*
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBXUtils
import org.brailleblaster.exceptions.EditingException
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.AdjustLocalStyleMessage
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.perspectives.mvc.XMLNodeCaret
import org.brailleblaster.perspectives.mvc.XMLNodeCaret.CursorPosition
import org.brailleblaster.perspectives.mvc.XMLSelection
import org.brailleblaster.perspectives.mvc.XMLTextCaret
import org.brailleblaster.perspectives.mvc.events.ModifyEvent.Companion.cannotUndoNextEvent
import org.brailleblaster.perspectives.mvc.events.XMLCaretEvent
import org.brailleblaster.perspectives.mvc.modules.misc.TableSelectionModule
import org.brailleblaster.settings.UTDManager
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.util.Notify.notify
import org.eclipse.swt.SWT
import org.slf4j.LoggerFactory
import java.util.*

object PageBreakTool : MenuToolModule {
    override val topMenu: TopMenu = TopMenu.EDIT
    override val title: String = "Page Break"
    override val accelerator: Int = SWT.CTRL or SWT.CR.code
    override fun onRun(bbData: BBSelectionData) {
        pageBreak(bbData.manager)
    }

    private fun existsFollowingText(m: Manager, currentSelection: XMLSelection): Boolean {
        val lastNode = currentSelection.end.node
        val moreNodes = lastNode.query("following::text()")
        if (inBlock(m)) {
            return true
        }
        if (beginning(m)) {
            if (!(currentSelection.isSingleNode && currentSelection.start.node.value.isEmpty())) {
                return true
            }
        }
        for (i in 0 until moreNodes.size()) {
            if (!isBraille(moreNodes[i])) {
                return true
            }
        }
        return false
    }

    private fun pageBreak(m: Manager) {
        var currentSelection = m.simpleManager.currentSelection
        if (!existsFollowingText(m, currentSelection)) {
            notify("Cannot add a page break without text after it.", "Cannot apply")
            return
        }
        if (!m.isValidPageBreak) {
            return
        }
        log.debug("Page break normal mode")
        val inTable = insideTable(currentSelection.end)
        if (inTable) {
            TableSelectionModule.displayInvalidTableMessage(m.display.activeShell)
            return
        }
        if (inBlock(m)) {
            log.debug("Splitting element")
            m.splitElement()
            currentSelection = m.simpleManager.currentSelection
            if (currentSelection.start is XMLTextCaret) {
                val startIndex = (currentSelection.start as XMLTextCaret).offset
                m.simpleManager.dispatchEvent(
                    XMLCaretEvent(
                        Sender.EMPHASIS,
                        XMLTextCaret(currentSelection.start.node as Text, startIndex)
                    )
                )
            }
        }
        currentSelection = m.simpleManager.currentSelection
        val beginning = beginning(m)
        var nodeToBreak: Node? = currentSelection.start.node
        if (!beginning) {
            nodeToBreak =
                if (BBX.CONTAINER.TABLE.isA(nodeToBreak) && currentSelection.start.cursorPosition == CursorPosition.BEFORE) {
                    //If cursor is before a table, use the table
                    Manager.getTableParent(nodeToBreak)
                } else {
                    if (BBX.CONTAINER.TABLE.isA(nodeToBreak)) {
                        //Cursor is after table, so find the next non-table node
                        val tableParent: Node = Manager.getTableParent(nodeToBreak)
                        FastXPath.following(nodeToBreak)
                            .stream()
                            .filter { n: Node ->
                                (n is Text
                                        && !UTDElements.BRL.isA(n.parent)
                                        && XMLHandler.ancestorElementNot(n) { e: Element? -> Manager.getTableParent(e) === tableParent })
                            }
                            .findFirst()
                            .orElse(null)
                    } else {
                        FastXPath.following(currentSelection.start.node)
                            .stream()
                            .filter { n: Node ->
                                n is Text && !UTDElements.BRL.isA(n.parent) && !UTDElements.BRLONLY.isA(
                                    n.parent
                                )
                            }
                            .findFirst()
                            .orElse(null)
                    }
                }
            if (nodeToBreak == null) {
                // we can't find a node to add a page break to end of document
                return
            }
        }
        if (isBraille(nodeToBreak)) {
            val nodes = m.simpleManager.currentSelection.start.node.query("following::text()")
            nodeToBreak = getFirstNonBraille(nodes, nodeToBreak)
        }

        //Place the catch for a boxline after you get the correct text nodeToBreak
        if (XMLHandler.ancestorElementIs(nodeToBreak) { node: Element? -> BBX.CONTAINER.BOX.isA(node) }) {
            nodeToBreak = XMLHandler.ancestorVisitor(nodeToBreak) { node: Node? -> BBX.CONTAINER.BOX.isA(node) }
        }
        var block = if (BBX.CONTAINER.BOX.isA(nodeToBreak)) nodeToBreak else m.getBlock(nodeToBreak)
        if (BBX.BLOCK.TABLE_CELL.isA(block) || BBX.CONTAINER.TABLE.isA(block)) {
            val tableParent = Manager.getTableParent(block)
            val style = m.getStyle(tableParent)
            if (style != null) {
                try {
                    changePagesOfTable(tableParent, style.newPagesBefore + 1, 0, m)
                } catch (e: RuntimeException) {
                    throw EditingException("An error occurred while adding a page before a table", e)
                }
            }
            m.simpleManager.dispatchEvent(XMLCaretEvent(Sender.NO_SENDER, XMLNodeCaret(Manager.getTableBrlCopy(block))))
            return
        }
        if (BBX.SPAN.PAGE_NUM.isA(block)) {
            while (block != null && BBX.SPAN.PAGE_NUM.isA(block)) {
                block = block.parent
            }
        }
        if (block != null) {
            val style = m.getStyle(block)
            if (style != null) {
                val pagesBefore = style.newPagesBefore + 1
                m.dispatch(AdjustLocalStyleMessage.adjustPages(block as Element, pagesBefore, 0))
                if (!BBX.CONTAINER.BOX.isA(nodeToBreak)) {
                    nodeToBreak = block.query("descendant::text()")[0]
                }
                if (nodeToBreak != null && !isPage(nodeToBreak)) {
                    // simple table nodes are not in the maplist
                    m.simpleManager.dispatchEvent(
                        XMLCaretEvent(
                            Sender.EMPHASIS,
                            if (nodeToBreak is Text) XMLTextCaret(nodeToBreak, 0) else XMLNodeCaret(nodeToBreak)
                        )
                    )
                }
            }
        }
    }

    private fun isPage(nodeToBreak: Node): Boolean {
        return XMLHandler.ancestorElementIs(nodeToBreak) { node: Element? -> BBX.SPAN.PAGE_NUM.isA(node) }
    }

    private fun insideTable(selection: XMLNodeCaret): Boolean {
        return if (BBX.CONTAINER.TABLE.isA(selection.node) && selection.cursorPosition == CursorPosition.ALL) true else XMLHandler.ancestorElementIs(
            selection.node
        ) { node: Element? -> BBX.CONTAINER.TABLETN.isA(node) }
    }

    private fun getFirstNonBraille(nodes: Nodes, startNode: Node?): Node? {
        if (nodes.size() == 0) {
            return null
        }
        var i = 0
        var returnNode = nodes[i]
        while (i < nodes.size() && isBraille(returnNode)) {
            returnNode = nodes[i]
            i++
        }
        return if (isBraille(returnNode)) {
            null
        } else returnNode
    }

    private fun inBlock(m: Manager): Boolean {
        val currentSelection = m.simpleManager.currentSelection
        if (currentSelection.end !is XMLTextCaret) {
            return false
        }
        val textCaret = currentSelection.end
        val block = BBXUtils.findBlock(currentSelection.end.node)
        return if (textCaret.offset == 0 && textCaret.node === getFirstTextNode(block)) {
            false
        } else textCaret.offset != textCaret.node.value.length
                || textCaret.node !== getLastTextNode(block)
    }

    private fun getFirstTextNode(node: ParentNode): Text? {
        val children = FastXPath.descendant(node).stream()
            .filter { n: Node? -> n is Text }.toList()
        for (child in children) {
            if (XMLHandler.ancestorElementIs(child) { e -> UTDElements.BRL.isA(e) }) {
                continue
            }
            return child as Text
        }
        return null
    }

    private fun getLastTextNode(node: ParentNode): Text? {
        val children = FastXPath.descendant(node)
            .filterIsInstance<Text>()
        for (i in children.indices.reversed()) {
            val child = children[i]
            if (XMLHandler.ancestorElementIs(child) { e -> UTDElements.BRL.isA(e) }) {
                continue
            }
            return child
        }
        return null
    }

    private fun isBraille(node: Node?): Boolean {
        return XMLHandler.ancestorElementIs(
            node
        ) { e: Element ->
            e.localName.lowercase(Locale.getDefault()).contains("brl") || UTDManager.hasUtdActionTag(
                e,
                "PageAction"
            )
        }
    }

    private fun beginning(m: Manager): Boolean {
        val currentSelection = m.simpleManager.currentSelection
        val endblock = m.getBlock(currentSelection.end.node)
        val start = currentSelection.start
        var startIndex = 1
        if (start is XMLTextCaret) {
            startIndex = start.offset
        }
        val nodesInBlock = endblock.query(".//text()")
        return if (nodesInBlock.size() > 0) {
            startIndex == 0
        } else false
    }

        private val log = LoggerFactory.getLogger(PageBreakTool::class.java)

        /**
         * Styles have to be applied to both the original table and the copy of the table.
         * Yes, I know that this is dumb and bad.
         */
		@JvmStatic
		fun changePagesOfTable(table: Element?, pagesBefore: Int, pagesAfter: Int, m: Manager) {
            val originalTable = Manager.getTableParent(table)
            val tableCopy = Manager.getTableBrlCopy(table)
            if (tableCopy != null) {
                //We skip an undo frame here because otherwise undo would leave the original table with a pageBefore
                cannotUndoNextEvent()
                m.dispatch(AdjustLocalStyleMessage.adjustPages(originalTable, pagesBefore, pagesAfter))
                m.dispatch(AdjustLocalStyleMessage.adjustPages(tableCopy, pagesBefore, pagesAfter))
            } else {
                m.dispatch(AdjustLocalStyleMessage.adjustPages(originalTable, pagesBefore, pagesAfter))
            }
        }
}
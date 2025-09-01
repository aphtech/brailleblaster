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

import nu.xom.*
import org.brailleblaster.BBIni.debugging
import org.brailleblaster.archiver2.TextArchiveLoader.Companion.getUsableText
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBXUtils.findBlock
import org.brailleblaster.bbx.BBXUtils.findBlockChild
import org.brailleblaster.bbx.findBlockOrNull
import org.brailleblaster.bbx.fixers.NodeTreeSplitter.split
import org.brailleblaster.bbx.fixers2.LiveFixer
import org.brailleblaster.math.mathml.MathModule.Companion.getMathText
import org.brailleblaster.math.mathml.MathModule.Companion.isMath
import org.brailleblaster.math.mathml.MathModule.Companion.makeMathFromSelection
import org.brailleblaster.math.mathml.MathModule.Companion.selectionContainsMath
import org.brailleblaster.math.mathml.MathSubject
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.perspectives.braille.mapping.elements.WhiteSpaceElement
import org.brailleblaster.perspectives.braille.mapping.interfaces.Uneditable
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.stylers.WhitespaceTransformer
import org.brailleblaster.perspectives.braille.views.wp.MathEditHandler.insertNew
import org.brailleblaster.perspectives.braille.views.wp.TextVerifyKeyListener.Companion.isBeginningOfBlock
import org.brailleblaster.perspectives.braille.views.wp.TextVerifyKeyListener.Companion.isEndOfBlock
import org.brailleblaster.perspectives.mvc.*
import org.brailleblaster.perspectives.mvc.BBSimpleManager.SimpleListener
import org.brailleblaster.perspectives.mvc.events.BuildMenuEvent
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.events.ModifyEvent.Companion.cannotUndoNextEvent
import org.brailleblaster.perspectives.mvc.events.ModifyEvent.Companion.resetUndoable
import org.brailleblaster.perspectives.mvc.events.XMLCaretEvent
import org.brailleblaster.perspectives.mvc.menu.MenuManager.add
import org.brailleblaster.tools.*
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.TableUtils
import org.brailleblaster.utd.utils.UTDHelper.Companion.stripUTDRecursive
import org.brailleblaster.util.Utils.combineAdjacentTextNodes
import org.brailleblaster.utils.braille.BrailleUnicodeConverter.asciiToUnicodeLouis
import org.eclipse.swt.SWT
import org.eclipse.swt.dnd.Clipboard
import org.eclipse.swt.dnd.TextTransfer
import org.eclipse.swt.dnd.Transfer
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Event
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Consumer
import kotlin.math.min

class ClipboardModule(private val manager: BBSimpleManager) : SimpleListener {
    private val clips: MutableList<Clip> = mutableListOf()
    private var lastCopiedString: String? = null
    val paste: Paste = Paste()

    override fun onEvent(event: SimpleEvent) {
        if (event is BuildMenuEvent) {
            add(CutTool(this))
            add(CopyTool(this))
            add(CopyUnicodeBrailleTool(this))
            add(PasteTool(this))
            add(PasteAsMathTool(this))
        }
    }

    @JvmOverloads
    fun copy(manager: Manager, returnUnicodeBraille: Boolean = false) {
        val sel = manager.simpleManager.currentSelection
        val isBrailleView = manager.brailleView.isTextSelected
        val isTextView = manager.textView.isTextSelected
        val textViewSelection = manager.textView.selectionText
        var brailleViewSelection = manager.braille.view.selectionText

        //Sanity checks
        if (textViewSelection.isEmpty() && isTextView) return
        else if (brailleViewSelection.isEmpty() && isBrailleView) return

        clips.clear()

        //System.out.println("Selected text: " + sel.toString());
        //Stupid, simple solution is just to copy the ascii braille to the clipboard
        //Things like tables might throw it off, but I could be wrong about that - MNS
        if (isBrailleView) {
            if (returnUnicodeBraille) {
                brailleViewSelection = asciiToUnicodeLouis(brailleViewSelection)
            }
            val cb = Clipboard(Display.getCurrent())
            val tt = TextTransfer.getInstance()
            cb.setContents(arrayOf<Any>(brailleViewSelection), arrayOf<Transfer?>(tt))
            return
        }

        val start = sel.start
        val end = sel.end
        if (sel.isSingleNode) {
            // Convert anything that isn't a container or section into a block
            if (!(BBX.CONTAINER.isA(start.node) || BBX.SECTION.isA(start.node))) {
                var block = findBlock(start.node)
                if (BBX.BLOCK.LIST_ITEM.isA(block)) {
                    addSimpleListItemToClipboard(block, start.node, end.node, sel)
                } else if (BBX.BLOCK.SPATIAL_MATH.isA(block)) {
                    val node: Node? = XMLHandler.ancestorVisitorElement(
                        block
                    ) { node: Element? -> BBX.CONTAINER.isA(node) }
                    if (node == null) {
                        log.error("Spatial Math block is not in a Spatial math container")
                        return
                    }
                    clips.add(Clip(node))
                } else {
                    var node: Node?
                    if (start is XMLTextCaret) {
                        // Trim the text down to only be what is selected
                        val textValue = start.node.value.substring(
                            start.offset,
                            (end as XMLTextCaret).offset
                        )
                        val inlineParent =
                            XMLHandler.ancestorVisitor(start.node) { node: Node? -> BBX.INLINE.isA(node) }
                        // If text is descended from an emphasis, take the emphasis with it
                        if (inlineParent != null) {
                            node = inlineParent.copy()
                            (node as Element).removeChildren()
                            node.appendChild(textValue)
                        } else {
                            node = Text(textValue)
                        }
                    } else if (isMath(start.node)) {
                        // will get the inline.mathml element
                        node = makeMathFromSelection(manager)
                    } else {
                        node = start.node.copy()
                    }
                    if (!BBX.BLOCK.isA(node)) {
                        block = block.copy()
                        block.removeChildren()
                        block.appendChild(node)
                        node = block
                    }
                    clips.add(Clip(requireNotNull(node)))
                }
            } else {
                // If it's a container or a section, we don't need to do
                // anything but just copy it
                clips.add(Clip(start.node.copy()))
            }
        } else {
            val startNode = start.node
            val endNode = end.node

            var startElement: Node? = startNode
            var endElement: Node? = endNode
            if (XMLHandler.ancestorElementIs(
                    startElement
                ) { node: Element? -> BBX.BLOCK.SPATIAL_MATH.isA(node) }
            ) {
                startElement = XMLHandler.ancestorVisitorElement(
                    startElement
                ) { node: Element? -> BBX.CONTAINER.isA(node) }
                if (startElement == null) {
                    log.error("Spatial Math block does not start in a Spatial math container")
                    return
                }
            }
            if (XMLHandler.ancestorElementIs(
                    endElement
                ) { node: Element? -> BBX.BLOCK.SPATIAL_MATH.isA(node) }
            ) {
                endElement = XMLHandler.ancestorVisitorElement(
                    endElement
                ) { node: Element? -> BBX.CONTAINER.isA(node) }
                if (endElement == null) {
                    log.error("Spatial Math block does not end in a Spatial math container")
                    return
                }
            }
            // If either element is text, inline, or span, convert it to its parent block
            if (!BBX.CONTAINER.isA(startElement) && !BBX.SECTION.isA(startElement) && !BBX.BLOCK.isA(startElement)) {
                startElement = findBlock(startNode)
            }
            if (!BBX.CONTAINER.isA(endElement) && !BBX.SECTION.isA(endElement) && !BBX.BLOCK.isA(endElement)) {
                endElement = findBlock(endNode)
            }

            if (startElement === endElement) { // Text is selected. Parent must be blocks
                addNodeToClipboard(startElement!!, startNode, endNode, sel)
            } else {
                // Keep track of the sibling of startElement (or startElement's parent)
                var curNode = XMLHandler.followingNode(startElement)

                // Add the initially selected block
                addNodeToClipboard(startElement!!, startNode, endNode, sel)

                // Loop through following nodes until endElement is reached
                while (curNode != null) {
                    if (curNode === endNode || curNode === endElement) break
                    // If the node is an ancestor of endElement, start looping through children
                    if (FastXPath.descendant(curNode).list().contains(endElement)) {
                        curNode = curNode.getChild(0)
                        continue
                    }
                    addNodeToClipboard(curNode, startNode, endNode, sel)
                    curNode = XMLHandler.followingNode(curNode)
                }

                // Add the final selected block
                addNodeToClipboard(endElement!!, startNode, endNode, sel)
            }
        }
        // Tests will fail when attempting to use the system clipboard
        if (!debugging) {
            lastCopiedString = convertBBXClipsToSystemClipboard(clips)
        }
    }

    fun convertSystemClipboardToBBXClips() {
        val cb = Clipboard(Display.getCurrent())
        val tt = TextTransfer.getInstance()
        val cbString = cb.getContents(tt) as String?
        if (cbString != null) {
            // If it was copied from outside of BrailleBlaster, or BB's
            // clipboard is empty,
            // paste that instead of what's inside BrailleBlaster's clipboard
            if (lastCopiedString == null || lastCopiedString != cbString) {
                // Convert it to BBX
                clips.clear()
                val blocks =
                    cbString.split(System.lineSeparator().toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (block in blocks) {
                    var block = block
                    val newBlock = BBX.BLOCK.DEFAULT.create()

                    block = block.replace("\t".toRegex(), " ")
                    val text = getUsableText(block)
                    newBlock.appendChild(text)

                    clips.add(Clip(newBlock))
                }
            }
        }
    }

    fun cut(manager: Manager) {
        copy(manager)
        if (manager.textView.selectionText.isEmpty()) return
        if (selectionContainsMath(manager)) {
            log.error("Cutting math")
        }

        // TODO: Should be false? See UndoEditTest.fullCut
        deleteTextViewSelection(manager, false)
    }

    private fun logPasteVariables(
        isStartText: Boolean, isStartMath: Boolean, inBlock: Boolean,
        inSection: Boolean, inList: Boolean, multipleClips: Boolean
    ) {
        log.debug(
            """
                        
                         isStartText {}
                         isStartMath {}
                         inBlock {}
                         inSection {}
                         inList {}
                         multipleClips {}
                         first clip {}
                         """.trimIndent(),
            isStartText, isStartMath, inBlock,
            inSection, inList, multipleClips, clips.firstOrNull()?.toString() ?: ""
        )
    }

    fun pasteAsMath(manager: Manager) {
        paste(manager, true)
    }

    @JvmOverloads
    fun paste(manager: Manager, isMath: Boolean = false) {
        manager.text.update(false)
        manager.stopFormatting()
        var sel = manager.simpleManager.currentSelection
        // Make sure we're not in a read only element
        val tme = manager.mapList.findNode(sel.start.node)
        if (tme is Uneditable && sel.start.cursorPosition == XMLNodeCaret.CursorPosition.ALL) {
            (tme as Uneditable).blockEdit(manager)
            return
        }
        //also check for cursor being in an uneditable TME that has no corresponding node
        if (manager.mapList.current is Uneditable) {
            (manager.mapList.current as Uneditable).blockEdit(manager)
            return
        }
        // Wrap to prevent 2 undo events
        cannotUndoNextEvent()
        // Overwrite
        deleteTextViewSelection(manager, true)
        resetUndoable()
        // references changed
        sel = manager.simpleManager.currentSelection
        val currentElement = manager.mapList.current
        // Is there something in the system clipboard?
        if (!debugging) { // Tests will fail when attempting to access
            // the clipboard
            convertSystemClipboardToBBXClips()
        }
        if (clips.isEmpty()) {
            return
        }

        if (isMath) {
            //I still don't believe it's this simple...there has to be a catch
            insertNew(MathSubject(clips.first().node))
            return
        }

        var startNode = sel.start.node
        val changedNodes: MutableList<Node> = mutableListOf()

        // Is the cursor in the middle of a block?
        val isStartText = sel.start is XMLTextCaret
        val isStartMath = isMath(sel.start.node)
        val cursorInText = isStartText || isStartMath
        val inBlock = sel.start.cursorPosition == XMLNodeCaret.CursorPosition.ALL
        val inSection = BBX.SECTION.isA(sel.start.node)
        var inList = BBX.CONTAINER.LIST.isA(sel.start.node) || BBX.BLOCK.LIST_ITEM.isA(sel.start.node)
        val multipleClips = clips.size > 1
        logPasteVariables(isStartText, isStartMath, inBlock, inSection, inList, multipleClips)

        if (inSection) {
            // TODO
            return
        }
        if (BBX.BLOCK.SPATIAL_MATH.isA(clips.first().node)) {
            return
            //Spatial math blocks should always be attached to a container
        }
        if (cursorInText && inBlock && clips.size == 1 && BBX.CONTAINER.LIST.isA(clips.first().node)
            && BBX.BLOCK.LIST_ITEM.isA(clips.first().node.getChild(0))
            && !BBX.BLOCK.LIST_ITEM.isA(findBlock(sel.start.node))
        ) {
            changedNodes.addAll(insertListInMiddleBlockText(isStartText, startNode, isStartMath, changedNodes, sel))
        } else if (isStartText && inBlock && !multipleClips) {
            val newEndNode: Node?
            val text = startNode as Text
            val startText = text.value.substring(0, sel.start.offset)
            var pasteOffset = sel.start.offset
            val endText = text.value.substring(sel.start.offset)
            text.value = startText
            newEndNode = Text(endText)
            var parent = startNode.parent
            var index = parent.indexOf(startNode) + 1
            if (BBX.BLOCK.isA(clips.first().node)) {
                for (i in 0..<clips.first().node.childCount) {
                    val child = clips.first().node.getChild(i).copy()
                    pasteOffset += child.value.length
                    index = addNodeToParent(parent, child, index)
                    paste.recordPaste(parent.getChild(index - 1), pasteOffset)
                }
            } else {
                var newChild = clips.first().node.copy()
                if (BBX.CONTAINER.LIST.isA(clips.first().node)) {
                    if (newChild.childCount == 1) {
                        //Do this to preserve emphasis
                        newChild = findBlockChild(newChild as Element).getChild(0).copy()
                        //newChild = UTDHelper.getFirstTextDescendant((Element) newChild);
                        index = addNodeToParent(parent, newChild, index)
                        paste.recordPaste(parent.getChild(index - 1), 0)
                    } else if (newChild.childCount > 1) {
                        newChild = findBlockChild(newChild as Element).getChild(0).copy()
                        index = addNodeToParent(parent, newChild, index)
                        paste.recordPaste(parent.getChild(index - 1), 0)
                        val blockParent = parent.parent
                        index = blockParent.indexOf(parent) + 1
                        parent = blockParent
                        for (i in 1..<clips.first().node.childCount) {
                            val child = clips.first().node.getChild(i).copy()
                            index = addNodeToParent(parent, child, index)
                            paste.recordPaste(parent.getChild(index - 1), 0)
                        }
                    }
                }
            }
            if (!newEndNode.value.trim { it <= ' ' }.isEmpty()) addNodeToParent(parent, newEndNode, index)
            changedNodes.add(parent)
        } else if (isStartMath && inBlock && !multipleClips) {
            insertNew(MathSubject(clips.first().node))
        } else if (cursorInText && inBlock && multipleClips) {
            // Split the existing block into two
            val block = findBlock(startNode)
            val blockCopy = copyBlock(startNode, null, block, sel, true)
            // Put the text of the first block into the existing first block
            if (!clips.first().node.value.trim { it <= ' ' }.isEmpty()) {
                if (BBX.BLOCK.isA(clips.first().node)) {
                    for (i in 0..<clips.first().node.childCount) {
                        addNodeToParent(block, clips.first().node.getChild(i).copy(), block.getChildCount())
                        paste.recordPaste(getFinalTextChild(block, true), 0)
                    }
                } else {
                    block.appendChild(clips.first().node)
                }
            }
            // Put the remaining blocks in between the two blocks
            val blockParent = block.parent
            var blockIndex = blockParent.indexOf(block) + 1
            for (i in 1..<clips.size) {
                val newChild = clips[i].node.copy()
                if (newChild.value.trim { it <= ' ' }.isEmpty()) {
                    continue
                }
                blockParent.insertChild(newChild, blockIndex)
                blockIndex++
                paste.recordPaste(newChild, 0)
            }
            blockParent.insertChild(blockCopy, blockIndex)
            changedNodes.add(blockParent)
        } else if (!inBlock) {
            if (BBX.INLINE.isA(startNode) || BBX.SPAN.isA(startNode) || cursorInText) {
                startNode = findBlock(startNode)
            }
            // Loop through clipboard
            var startNodeParent = startNode.parent
            var index = (startNodeParent.indexOf(startNode)
                    + (if (sel.start.cursorPosition == XMLNodeCaret.CursorPosition.BEFORE) 0 else 1))
            if (index < startNodeParent.getChildCount() && BBX.CONTAINER.TABLE.isA(startNodeParent.getChild(index))
                && TableUtils.isTableCopy((startNodeParent.getChild(index) as Element?)!!)
            ) {
                index++
            }
            if (BBX.BLOCK.LIST_ITEM.isA(startNode)) inList = true
            if (BBX.CONTAINER.LIST.isA(startNodeParent) && !inList) {
                startNode = startNodeParent
                startNodeParent = startNode.parent
                index = (startNodeParent.indexOf(startNode)
                        + (if (sel.start.cursorPosition == XMLNodeCaret.CursorPosition.BEFORE) 0 else 1))
            }
            for (i in clips.indices) {
                val listParent =
                    XMLHandler.ancestorVisitor(startNode) { node: Node? -> BBX.CONTAINER.LIST.isA(node) }
                val clipboardNode = clips[i].node
                if (clipboardNode.value.trim { it <= ' ' }.isEmpty()) {
                    continue
                }
                // Is the cursor inside a list container? Is the current clip a
                // list container?
                if (inList && BBX.CONTAINER.LIST.isA(clipboardNode)) {
                    normalizeList(listParent, clipboardNode, currentElement, index, startNodeParent, i)
                } else if (currentElement is WhiteSpaceElement && i == 0 && !BBX.CONTAINER.LIST.isA(startNode)) {
                    val wt = WhitespaceTransformer(manager)
                    val copyNode = clipboardNode.copy()
                    wt.transformWhiteSpace(currentElement, copyNode)
                    index = startNodeParent.indexOf(copyNode)
                    if (index == -1) {
                        /*There is no guarantee that transform whitespace will insert the node as a child of the current element's parent*/
                        startNodeParent = copyNode.parent
                        index = startNodeParent.indexOf(copyNode)
                    }
                    paste.recordPaste(copyNode, 0)
                    index++
                } else {
                    val copyNode = clipboardNode.copy()
                    startNodeParent.insertChild(copyNode, index)
                    paste.recordPaste(copyNode, 0)
                    index++
                }
            }
            changedNodes.add(startNodeParent)
        } // end cursor is outside of block


        if (changedNodes.isEmpty()) return

        // Refer to ticket 6177
        // We have to stop formatting again here.
        // I don't understand why.
        // UTD throws a nonsensical exception if we don't, even though the
        // formatting thread has been stopped.
        // This code is a nightmare.
        manager.stopFormatting()

        // Normalize nested emphasis
        changedNodes.forEach { n: Node? ->
            if (n is ParentNode) stripUTDRecursive(n)
            normalizeEmphasis(n!!)
        }

        val changedNodesArray = changedNodes.toTypedArray()
        manager.stopFormatting()
        manager.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, true, *changedNodesArray))

        // Move cursor to last pasted text
        val pasteNode = paste.node
        if (pasteNode != null && pasteNode.document != null) {
            if (pasteNode is Text) {
                manager.simpleManager.dispatchEvent(
                    XMLCaretEvent(Sender.BRAILLE, XMLTextCaret(pasteNode, paste.offset))
                )
            } else {
                manager.simpleManager.dispatchEvent(XMLCaretEvent(Sender.BRAILLE, XMLNodeCaret(pasteNode)))
                //RT 7767
                val findNodeText = manager.findNodeText(pasteNode)
                val offset = findNodeText.first().getEnd(manager.mapList)
                manager.setTextCaret(offset)
            }
        }
    }

    private fun insertListInMiddleBlockText(
        isStartText: Boolean, startNode: Node, isStartMath: Boolean, changedNodes: MutableList<Node>, sel: XMLSelection
    ): MutableList<Node> {
        val listItem = clips.first().node as Element
        clips.removeAt(0)
        clips.add(Clip(listItem))
        var endText = ""

        if (isStartText) {
            val text = startNode as Text
            endText = text.value.substring((sel.start as XMLTextCaret).offset)
        } else if (isStartMath) {
            endText = getMathText(startNode)
                .substring(
                    min(
                        manager.manager.textView.caretOffset - manager.manager.mapList
                            .current.getStart(manager.manager.mapList), getMathText(startNode).length
                    )
                )
        }

        if (!endText.isEmpty() && !endText.contentEquals(startNode.value)) {
            manager.manager.splitElement()
        }

        val parent = findBlock(startNode).parent
        var index =
            if (!endText.isEmpty() && endText.contentEquals(startNode.value)) parent.indexOf(findBlock(startNode)) else parent.indexOf(
                findBlock(startNode)
            ) + 1

        if (BBX.CONTAINER.LIST.isA(clips.first().node)) {
            for (i in 0..<clips.first().node.childCount) {
                val newChild = clips.first().node.getChild(i).copy()
                index = addNodeToParent(parent, newChild, index)
                paste.recordPaste(parent.getChild(index - 1), 0)
            }
        } else {
            val newChild = clips.first().node.copy()
            index = addNodeToParent(parent, newChild, index)
            paste.recordPaste(parent.getChild(index - 1), 0)
        }

        changedNodes.add(parent)
        //clips.remove(0);
        return changedNodes
    }

    fun normalizeList(
        listParent: Node?, clipboardNode: Node, currentElement: TextMapElement?,
        index: Int, startNodeParent: ParentNode, i: Int
    ) {
        // If the parent list has a smaller list level, change its
        // list level to equal
        // the clipboard list's level
        var index = index
        val parentLevel = BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.get(listParent as Element?)
        val clipboardLevel = BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.get(clipboardNode as Element?)
        if (parentLevel < clipboardLevel) {
            BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.set(listParent, clipboardLevel)
        }

        // Throw out the list container in the clip and attach
        // children
        for (child in 0..<clipboardNode.getChildCount()) {
            if (clipboardNode.getChild(child).value.trim { it <= ' ' }.isEmpty()) {
                continue
            }
            if (currentElement is WhiteSpaceElement && i == 0 && child == 0) {
                // TODO: Copy/pasted from below
                val wt = WhitespaceTransformer(manager.manager)
                val copyNode = clipboardNode.getChild(child).copy()
                wt.transformWhiteSpace(currentElement, copyNode)
                index = startNodeParent.indexOf(copyNode)
                if (index == -1) {
                    // I'm not sure how this could come up
                    println("An error occurred when pasting")
                    continue
                }
                paste.recordPaste(copyNode, 0)
            } else {
                val newChild = clipboardNode.getChild(child).copy()
                startNodeParent.insertChild(newChild, index)
                paste.recordPaste(newChild, 0)
            }
            index++
        }
    }

    /**
     * If inserting a text node, normalizes text node with surrounding text
     * nodes and handle emphasis
     *
     * @return index + 1 if no normalization happened, index if new node was
     * normalized
     */
    private fun addNodeToParent(parent: ParentNode, node: Node?, index: Int): Int {
        var index = index
        if (node is Text && index > 0 && parent.getChild(index - 1) is Text) {
            val sibling = parent.getChild(index - 1) as Text
            sibling.value += node.value
        } else {
            parent.insertChild(node, index)
            index++
        }
        return index
    }

    private fun addNodeToClipboard(nodeToCopy: Node, startNode: Node?, endNode: Node?, selection: XMLSelection) {
        //Children of lists should be added to the list in the clipboard
        var nodeToCopy = nodeToCopy
        if (BBX.CONTAINER.LIST.isA(nodeToCopy.parent)) {
            addListItemToClipboard(nodeToCopy, startNode, endNode, selection)
        } else if (BBX.BLOCK.isA(nodeToCopy)) {
            val copy = copyBlock(startNode, endNode, nodeToCopy as Element, selection, false)
            stripUTDRecursive(copy)
            if (!blockCopy(
                    copy, selection,
                    FastXPath.descendant(nodeToCopy).list().contains(startNode),
                    FastXPath.descendant(nodeToCopy).list().contains(endNode)
                )
            ) clips.add(Clip(copy))
        } else {
            if (nodeToCopy is ParentNode) {
                nodeToCopy = nodeToCopy.copy()
                stripUTDRecursive((nodeToCopy as ParentNode?)!!)
                clips.add(Clip(nodeToCopy))
            } else {
                clips.add(Clip(nodeToCopy.copy()))
            }
        }
    }

    private fun copyBlock(
        startNode: Node?,
        endNode: Node?,
        block: Element,
        selection: XMLSelection,
        cut: Boolean
    ): Element {
        val children: List<Node> = FastXPath.descendant(block).list()
        if (!children.contains(startNode) && !children.contains(endNode)) {
            return block.copy()
        }
        val blockCopy = block.copy()
        val nodesToDetach: MutableList<Node?> = ArrayList()
        if (startNode in children) {
            // If the startNode is inside this block, remove every node that
            // comes before startNode
            val indexes = findNodeIndexes(block, startNode!!)
            var findStartNodeCopy: Node = blockCopy
            for (i in indexes.indices.reversed()) {
                findStartNodeCopy = findStartNodeCopy.getChild(indexes[i]!!)
            }
            val startNodeCopy = findStartNodeCopy // lambdas!

            val following = FastXPath.followingAndSelf(blockCopy).list()
            for (n in following) {
                if (n !== blockCopy && n !== startNodeCopy && !FastXPath.descendant(n).list().contains(startNodeCopy)) {
                    nodesToDetach.add(n)
                }
                if (n === startNodeCopy) break
            }
            if (selection.start is XMLTextCaret) {
                // If only part of the text node inside the block was selected,
                // split the text node
                val newValue = startNodeCopy.value.substring(selection.start.offset)
                if (newValue.isEmpty()) nodesToDetach.add(startNodeCopy)
                else (startNodeCopy as Text).value = newValue
                if (cut && startNode is Text) {
                    startNode.value = startNode.value.substring(0, selection.start.offset)
                }
            } else if (isMath(selection.start.node)) {
                val newMath = makeMathFromSelection(manager.manager)
                val parentNode = startNodeCopy.parent
                parentNode.replaceChild(startNodeCopy, newMath)
            }
            if (cut && (startNode is Text || isMath(startNode))) {
                for (i in children.indexOf(startNode) + 1..<children.size) {
                    nodesToDetach.add(children[i])
                }
            }
        }
        if (endNode in children) {
            // If the endNode is inside this block, remove every node that comes
            // after endNode
            val indexes = findNodeIndexes(block, endNode!!)
            var findEndNodeCopy: Node = blockCopy
            for (i in indexes.indices.reversed()) {
                findEndNodeCopy = findEndNodeCopy.getChild(indexes[i]!!)
            }
            val endNodeCopy = findEndNodeCopy
            //If they're page numbers, don't detach anything from the node
            if (!(BBX.SPAN.PAGE_NUM.isA(endNodeCopy) || BBX.BLOCK.PAGE_NUM.isA(endNodeCopy))) {
                FastXPath.followingAndSelf(endNodeCopy).forEach(Consumer { n: Node? ->
                    if (n !== endNodeCopy) {
                        nodesToDetach.add(n)
                    }
                })
            }

            if (selection.end is XMLTextCaret) {
                // If only part of the text node inside the block was selected,
                // split the text node
                val newValue = endNodeCopy.value.substring(0, selection.end.offset)
                if (newValue.isEmpty()) nodesToDetach.add(endNodeCopy)
                else (endNodeCopy as Text).value = newValue
                if (cut && endNode is Text)  //					((Text) endNode).setValue(endNode.getValue().substring(caretSel.offset));
                    endNode.value = endNode.value.substring(selection.end.offset)
            } else if (isMath(selection.end.node)) {
                val newMath = makeMathFromSelection(manager.manager)
                val parentNode = endNodeCopy.parent
                parentNode.replaceChild(endNodeCopy, newMath)
            }
            if (cut && (endNode is Text || isMath(endNode))) {
                for (i in 0..<children.indexOf(endNode)) {
                    if (children[i].childCount == 0
                        || !FastXPath.descendant(children[i]).list().contains(endNode)
                    ) nodesToDetach.add(children[i])
                }
            }
        }

        nodesToDetach.forEach(Consumer { obj: Node? -> obj!!.detach() })
        stripUTDRecursive(blockCopy)
        return blockCopy
    }

    private fun addListItemToClipboard(listItem: Node, startNode: Node?, endNode: Node?, selection: XMLSelection) {
        val listTag = listItem.parent as Element
        require(BBX.CONTAINER.LIST.isA(listTag)) { "Invalid BBX: List Item outside of List Tag" }
        val listLevel = BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.get(listTag)
        val listItemCopy = copyBlock(startNode, endNode, listItem as Element, selection, false)
        if (blockCopy(
                listItemCopy, selection, FastXPath.descendant(listItem).list().contains(startNode),
                FastXPath.descendant(listItem).list().contains(endNode)
            )
        ) {
            return
        }
        if (clips.isEmpty() // If there's nothing else in the clipboard
            //or the previous clipboard item wasn't a list
            || !BBX.CONTAINER.LIST.isA(clips[clips.size - 1].node) // the previous list was a different list level Just add the list
            // tag to the clipboard
            || listLevel != BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.get(clips[clips.size - 1].node as Element)
        ) {
            val copy = listTag.copy()
            copy.removeChildren()
            copy.appendChild(listItemCopy)
            clips.add(Clip(copy))
        } else { // Otherwise, add the list item to the previous list tag
            (clips[clips.size - 1].node as Element).appendChild(listItemCopy)
        }
    }

    private fun addSimpleListItemToClipboard(
        listItem: Node,
        startNode: Node?,
        endNode: Node?,
        selection: XMLSelection
    ) {
        val listTag = listItem.parent as Element
        require(BBX.CONTAINER.LIST.isA(listTag)) { "Invalid BBX: List Item outside of List Tag" }
        val listLevel = BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.get(listTag)
        //		Element listItemCopy = copyBlock(startNode, endNode, (Element) listItem, selection, false);
        var block = findBlock(startNode)
        var node: Node?
        if (selection.start is XMLTextCaret) {
            // Trim the text down to only be what is selected
            val textValue = selection.start.node.value.substring(
                selection.start.offset,
                (selection.end as XMLTextCaret).offset
            )
            val inlineParent =
                XMLHandler.ancestorVisitor(selection.start.node) { node: Node? -> BBX.INLINE.isA(node) }
            // If text is descended from an emphasis, take the
            // emphasis with it
            if (inlineParent != null) {
                node = inlineParent.copy()
                (node as Element).removeChildren()
                node.appendChild(textValue)
            } else {
                node = Text(textValue)
            }
        } else if (isMath(selection.start.node)) {
            // will get the inline.mathml element
            node = makeMathFromSelection(manager.manager)
        } else {
            node = selection.start.node.copy()
        }
        if (!BBX.BLOCK.isA(node)) {
            block = block.copy()
            block.removeChildren()
            block.appendChild(node)
            node = block
        }
        if (blockCopy(
                block, selection, FastXPath.descendant(listItem).list().contains(startNode),
                FastXPath.descendant(listItem).list().contains(endNode)
            )
        ) {
            return
        }
        if (!BBX.BLOCK.isA(node)) {
            block = block.copy()
            block.removeChildren()
            block.appendChild(node)
            node = block
        }
        if (clips.isEmpty() // If there's nothing else in the clipboard
            //or the previous clipboard item wasn't a list
            || !BBX.CONTAINER.LIST.isA(clips[clips.size - 1].node) // the previous list was a different list level Just add the list
            // tag to the clipboard
            || listLevel != BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.get(clips[clips.size - 1].node as Element)
        ) {
            val copy = listTag.copy()
            copy.removeChildren()
            copy.appendChild(node)
            clips.add(Clip(copy))
        } else { // Otherwise, add the list item to the previous list tag
            (clips[clips.size - 1].node as Element).appendChild(node)
        }
    }

    private fun findNodeIndexes(block: Element?, nodeToFind: Node): MutableList<Int?> {
        val indexes: MutableList<Int?> = ArrayList()
        var curNode = nodeToFind
        while (curNode !== block) {
            indexes.add(curNode.parent.indexOf(curNode))
            curNode = curNode.parent
        }
        return indexes
    }

    private fun getFinalTextChild(node: Node, lookForMath: Boolean): Text? {
        if (node is Text) return node
        var returnText: Text? = null
        val children = FastXPath.descendant(node).list()
        for (child in children) {
            if (child is Text && ((!lookForMath || isMath(child)))
                && !XMLHandler.ancestorElementIs(child) { node: Element? -> UTDElements.BRL.isA(node) }
            ) returnText = child
        }
        return returnText
    }

    private fun normalizeEmphasis(parent: Node) {
        if (parent.childCount == 0) return
        val children =
            FastXPath.descendant(parent).filter { node -> BBX.INLINE.EMPHASIS.isA(node) }.toList()
        for (child in children) {
            val emphasis = child as Element
            val empParent = emphasis.parent
            val index = empParent.indexOf(emphasis)
            val empType = BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS.get(emphasis)
            // Is there an identical emphasis before this emphasis?
            if (index > 0 && BBX.INLINE.EMPHASIS.isA(empParent.getChild(index - 1))) {
                val prev = empParent.getChild(index - 1) as Element
                if (BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS.get(prev) == empType) {
                    for (i in prev.getChildCount() - 1 downTo 0) {
                        val prevChild = prev.getChild(i)
                        prevChild.detach()
                        emphasis.insertChild(prevChild, 0)
                    }
                    prev.detach()
                    normalizeEmphasis(parent)
                    return
                }
            }
            // Is there an identical emphasis after this emphasis?
            if (index < empParent.getChildCount() - 1 && BBX.INLINE.EMPHASIS.isA(empParent.getChild(index + 1))) {
                val next = empParent.getChild(index + 1) as Element
                if (BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS.get(next) == empType) {
                    while (next.getChildCount() > 0) {
                        val nextChild = next.getChild(0)
                        nextChild.detach()
                        emphasis.appendChild(nextChild)
                    }
                    next.detach()
                    normalizeEmphasis(parent)
                    return
                }
            }

            // Is there an emphasis descended from this emphasis?
            for (i in 0..<emphasis.getChildCount()) {
                if (BBX.INLINE.EMPHASIS.isA(emphasis.getChild(i))) {
                    val nestedEmphasis = emphasis.getChild(i) as Element
                    val nestedParent = nestedEmphasis.parent
                    val nestedEmphasisIndex = nestedParent.indexOf(nestedEmphasis)
                    // Is it identical?
                    if (BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS.get(nestedEmphasis) == empType) {
                        // Just throw out the emphasis node and replace it with
                        // its children
                        for (j in 0..<nestedEmphasis.getChildCount()) {
                            val nestedChild = nestedEmphasis.getChild(j)
                            nestedChild.detach()
                            nestedParent.insertChild(nestedChild, nestedEmphasisIndex)
                        }
                        nestedEmphasis.detach()
                    } else {
                        // Split the parent emphasis into two and put the nested
                        // emphasis in between
                        split(nestedParent as Element, nestedEmphasis)
                    }
                    normalizeEmphasis(parent)
                    return
                }
            }
        }
        combineAdjacentTextNodes(parent as ParentNode)
    }

    /**
     * Returns true if copiedBlock should not be added to the clipboard
     */
    private fun blockCopy(copiedBlock: Element, selection: XMLSelection, atStart: Boolean, atEnd: Boolean): Boolean {
        if (copiedBlock.getChildCount() == 0) {
            // RT #4707: If the selection is being done through the text view,
            // and it has nothing actually selected (thus copiedBlock has no
            // children)
            // do not copy the block.
            return (atStart && selection.start is XMLTextCaret)
                    || (atEnd && selection.end is XMLTextCaret)
        }
        return false
    }

    private fun deleteTextViewSelection(manager: Manager, keepBlock: Boolean) {
        if (manager.textView.selectionText.isEmpty()) {
            return  // use the view here as
            // manager.getSelection.isTextNoSelection will ignore math
        }

        // Delete selected nodes
        val selection = manager.simpleManager.currentSelection
        var section = selection.start.node
        while (!BBX.SECTION.isA(section)) {
            section = section.parent
        }

        if (!(isMath(selection.start.node) && isMath(selection.end.node)) && keepBlock
            && isBeginningOfBlock(selection.start.node)
            && isEndOfBlock(selection.end.node)
        ) {
            /*
             * When the block is fully selected, pressing delete removes the
             * whole block, which causes the paste-over feature to always apply
             * to the next block
             *
             * Pilcrow hack to the rescue! Text is overwritten with the pilcrow
             * character, paste can now add to block like normal, then LiveFixer
             * removes it
             */
            val event = Event()
            event.character = LiveFixer.PILCROW.first()
            if (manager.text.view.isFocusControl) {
                manager.text.view.notifyListeners(SWT.KeyDown, event)
                manager.text.update(false)

                val parentBlock = manager.simpleManager.currentCaret.node.findBlockOrNull()
                parentBlock?.addAttribute(Attribute(LiveFixer.NEWPAGE_PLACEHOLDER_ATTRIB, "true"))

                log.error("updated")
            }
        } else {
            // I'm so sorry for what's about to happen
            val event = Event()
            event.keyCode = SWT.DEL.code
            if (manager.text.view.isFocusControl) {
                // Send a delete key event to the text view
                manager.text.view.notifyListeners(SWT.KeyDown, event)
                // trigger MapList rebuild
                manager.text.update(false)
            }
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ClipboardModule::class.java)
        fun convertBBXClipsToSystemClipboard(clips: MutableList<Clip>): String {
            val systemCB = StringBuilder()
            clips.forEach(Consumer { c: Clip? ->
                val copy = c!!.node.copy()
                if (copy is ParentNode) stripUTDRecursive(copy)
                if (copy is Element && (BBX.SECTION.isA(copy) || BBX.CONTAINER.isA(copy))) {
//				systemCB.append(getBlocksTextInSection((Element) copy).trim()); ticket 7627
                    systemCB.append(getBlocksTextInSection(copy))
                } else {
                    systemCB.append(copy.value)
                }
                if (clips.indexOf(c) != clips.size - 1) systemCB.append(System.lineSeparator())
            })
            val textTransfer = TextTransfer.getInstance()
            val clipboard = Clipboard(Display.getCurrent())
            clipboard.clearContents()
            if (!systemCB.isEmpty()) {
                clipboard.setContents(arrayOf(systemCB.toString()), arrayOf<Transfer?>(textTransfer))
            } else {
                // SWT cannot setContents to be an empty string, because you're
                // expected to use clearContents.
                // However, clearContents can only clear the contents of the
                // clipboard if it was the one who
                // put it there. If the user copied text from another application,
                // clearContents does nothing.
                // So to override the clipboard, first we override the system
                // clipboard, then clearContents
                // works, since we were the last ones to touch the clipboard.
                clipboard.setContents(arrayOf(" "), arrayOf<Transfer?>(textTransfer))
                clipboard.clearContents()
            }
            clipboard.dispose()
            return systemCB.toString()
        }

        private fun getBlocksTextInSection(section: Element): String {
            val sb = StringBuilder()
            for (i in 0..<section.getChildCount()) {
                if (section.getChild(i) is Element) {
                    if (BBX.BLOCK.isA(section.getChild(i))) sb.append(section.getChild(i).value)
                        .append(System.lineSeparator())
                    else {
                        sb.append(getBlocksTextInSection((section.getChild(i) as Element?)!!))
                    }
                }
            }
            return sb.toString()
        }
    }
}

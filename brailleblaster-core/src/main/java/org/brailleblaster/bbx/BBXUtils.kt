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
package org.brailleblaster.bbx

import nu.xom.Attribute
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.bbx.BBX.ListType
import org.brailleblaster.bbx.BBX.MarginType
import org.brailleblaster.bbx.BBXUtils.findBlock
import org.brailleblaster.bbx.fixers.to3.ImageBlockToContainerImportFixer.Companion.convertImageBlockToContainer
import org.brailleblaster.math.mathml.MathModule.Companion.isSpatialMath
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.searcher.Searcher
import org.brailleblaster.settings.UTDManager
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.properties.EmphasisType
import org.brailleblaster.utd.toc.TOCAttributes
import org.brailleblaster.utd.utils.UTDHelper.Companion.getDocumentHead
import org.brailleblaster.utd.utils.UTDHelper.Companion.stripUTDRecursive
import org.brailleblaster.utd.utils.dom.BoxUtils.stripBoxBrl
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Check if given node is a BLOCK or SPAN of type PAGE_NUM
 */
fun Node?.isPageNum(): Boolean = BBX.SPAN.PAGE_NUM.isA(this) || BBX.BLOCK.PAGE_NUM.isA(this)

fun Node?.isPageNumAncestor(): Boolean = XMLHandler.ancestorElementIs(this) { it.isPageNum() }

/**
 * Checks if only descendant is a page num in addition to [.isPageNum]
 * and [.isPageNumAncestor]
 */
fun Node?.isPageNumEffectively(): Boolean = if (this.isPageNum() || this.isPageNumAncestor()) {
    true
} else if (this !is Element) {
    false
} else {
    // Check if all text nodes are from a page num
    FastXPath.descendant(this)
        .filterIsInstance<Text>()
        .filter { Searcher.Filters.noUTDAncestor(it) }
        .all { it.isPageNumAncestor() }
}

fun Node?.isTOCText(): Boolean = this != null && (BBX.BLOCK.MARGIN.isA(findBlock(this)) ||
        BBX.BLOCK.TOC_VOLUME_SPLIT.isA(findBlock(this)))

fun Element?.findBlockChildOrNull(): Element? = XMLHandler.childrenRecursiveVisitor(
    this
) { BBX.BLOCK.isA(it) }

fun Element?.findBlockChild(): Element = this.findBlockChildOrNull() ?: throw RuntimeException("Node does not contain a block.")

fun Node?.findBlockOrNull(): Element? = XMLHandler.ancestorVisitor(
    this
) { BBX.BLOCK.isA(it) } as Element?

fun Node?.findBlock(): Element = this.findBlockOrNull() ?: throw RuntimeException("Node not inside a block")

object BBXUtils {
    private val log: Logger = LoggerFactory.getLogger(BBXUtils::class.java)

    @JvmStatic
    fun isPageNumAncestor(node: Node?): Boolean = node.isPageNumAncestor()

    @JvmStatic
    fun findBlockChild(node: Element?): Element = node.findBlockChild()

    @JvmStatic
    fun findBlock(node: Node?): Element = node.findBlock()

    fun getAncestorListLevel(node: Node?): Int = BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL[XMLHandler.ancestorVisitorElement(
        node
    ) { BBX.CONTAINER.LIST.isA(it) }]

    /**
     * @see .stripStyle
     */
    fun stripStyle(elementToStrip: Element, m: Manager): Element = stripStyle(elementToStrip, m.document.settingsManager)

    /**
     * Actually remove all BBX style information from an Element
     */
    fun stripStyle(elementToStrip: Element, utdMan: UTDManager): Element {
        stripStyleExceptOverrideStyle(elementToStrip)
        utdMan.removeOverrideStyle(elementToStrip)
        return elementToStrip
    }

    fun stripStyleExceptOverrideStyle(elementToStrip: Element): Element {
        //All known options that can change style
        detachAttribute(BBX.BLOCK.MARGIN.ATTRIB_INDENT.getAttribute(elementToStrip))
        detachAttribute(BBX.BLOCK.MARGIN.ATTRIB_RUNOVER.getAttribute(elementToStrip))
        detachAttribute(BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL.getAttribute(elementToStrip))
        detachAttribute(TOCAttributes.TYPE.getAttribute(elementToStrip))
        return elementToStrip
    }

    private fun detachAttribute(attrib: Attribute?) {
        attrib?.detach()
    }

    fun parseListStyle(styleName: String): ListStyleData? {
        val listType: ListType?
        val marginType: MarginType?
        when (styleName[0]) {
            'L' -> {
                listType = ListType.NORMAL
                marginType = null
            }

            'G' -> {
                listType = ListType.DEFINITION
                marginType = null
            }

            'P' -> {
                listType = ListType.POEM
                marginType = null
            }

            'I' -> {
                listType = null
                marginType = MarginType.INDEX
            }

            'E' -> {
                listType = null
                marginType = MarginType.EXERCISE
            }

            'T' -> {
                listType = null
                marginType = MarginType.TOC
            }

            else -> {
                //may be margin style
                var hasHyphen = false
                for (c in styleName.toCharArray()) {
                    if (c == '-') {
                        if (hasHyphen) {
                            return null
                        }
                        hasHyphen = true
                    } else {
                        if (!Character.isDigit(c)) {
                            return null
                        }
                    }
                }
                if (!hasHyphen) {
                    return null
                }
                listType = null
                marginType = MarginType.NUMERIC
            }
        }

        val args = styleName.split("-", limit=2)
        if (args.size != 2) {
            return null
        }

        val indent: Int
        val runover: Int
        try {
            //remove prefix
            val firstNumber = if ((marginType == MarginType.NUMERIC))
                args[0]
            else
                args[0].substring(1)
            indent = firstNumber.toInt()
            runover = args[1].toInt()
        } catch (e: Exception) {
            log.trace("Unable to parse style {}", styleName, e)
            return null
        }

        return ListStyleData(listType, marginType, indent, runover)
    }

    fun indentFromLevel(level: Int): Int {
        return ((level * 2) + 1)
    }

    fun indentToLevel(indent: Int): Int {
        return (indent - 1) / 2
    }

    fun runoverFromLevel(level: Int): Int {
        //Stored runover is maximum indent, incriment for extra braille indent
        //eg max indent is 1 cell, so must use 1-3 style
        return (((level + 1) * 2) + 1)
    }

    fun runoverToLevel(runover: Int): Int {
        return ((runover - 1) / 2) - 1
    }

    /**
     * Remove non-list item styles from list container and split list if necessary
     */
    fun checkListStyle(listContainer: Element?, startItem: Element, m: UTDManager) {
        if (listContainer == null || !BBX.CONTAINER.LIST.isA(listContainer)) {
            throw NodeException("ListContainer must be a list, given ", listContainer)
        } else if (BBX.CONTAINER.isA(startItem)) {
            // container is wrapping list items
            return
        } else if (!BBX.BLOCK.isA(startItem)) {
            throw NodeException("startItem must be a block, given ", startItem)
        }

        //TODO: This is an edge case that can happen, but due to time constraints we are
        //ignoring it for now
        if (startItem.parent !== listContainer) {
            return
        }

        for (node in FastXPath.followingAndSelf(startItem)) {
            if (!XMLHandler.ancestorElementIs(node) { e: Element -> e === listContainer }) {
                break
            }
            if (!BBX.BLOCK.isA(node)) {
                continue
            }

            val style = m.engine.getStyle(node)
            if (style != null) {
                style.name
                if (parseListStyle(style.name) == null) {
//not a list item
                    val listCopy = listContainer.copy()
                    listCopy.removeChildren()
                    val index = listContainer.indexOf(node)
                    for (i in index - 1 downTo 0) {
                        val listItem = listContainer.getChild(i)
                        listItem.detach()
                        listCopy.insertChild(listItem, 0)
                    }

                    node.detach()
                    listContainer.parent.insertChild(node, listContainer.parent.indexOf(listContainer))
                    if (listCopy.childCount > 0) {
                        node.parent.insertChild(listCopy, node.parent.indexOf(node))
                    }
                    if (listContainer.childCount == 0) {
                        listContainer.detach()
                    }
                }
            }
        }
    }

    /**
     * Removes blocks if they are empty and converts blocks containing
     * only images to image containers. Node is any child of
     * the block, or the block. Returns the parent of the removed element
     */
    @JvmStatic
    fun cleanupBlock(node: Node?): Node? {
        val block = node.findBlockOrNull() ?: return null
        stripUTDRecursive(block)
        if (block.value.isEmpty()) {
            var parent: Node? = block.parent
            val image = XMLHandler.childrenRecursiveVisitor(
                block
            ) { BBX.SPAN.IMAGE.isA(it) }
            if (image == null) {
                block.detach()
            } else {
                convertImageBlockToContainer(image)
            }
            while (parent != null) {
                if (BBX.CONTAINER.isA(parent)) {
                    //If the container is a box, do not look at the boxlines when seeing if the element is empty
                    if (BBX.CONTAINER.BOX.isA(parent)) {
                        val boxCopy = parent.copy() as Element
                        stripUTDRecursive(boxCopy)
                        if (boxCopy.value.isEmpty()) {
                            val temp: Node = parent.parent
                            stripBoxBrl(parent as Element)
                            parent.detach()
                            parent = temp
                        } else {
                            return parent
                        }
                    } else if (parent.value.isEmpty()
                        && XMLHandler.childrenRecursiveNodeVisitor(
                            parent
                        ) { BBX.SPAN.IMAGE.isA(it) } == null
                    ) {
                        val temp: Node = parent.parent
                        parent.detach()
                        parent = temp
                    } else {
                        return parent
                    }
                } else {
                    return parent
                }
            }
            //Only happens if given node is detached from the document
            return null
        } else {
            return block
        }
    }

    /*
     * Will return the index of this node's direct ancestor within its block
     */
    fun getIndexInBlock(n: Node): Int {
        var parent: Node? = n
        var block = n.parent
        while (block != null && !BBX.BLOCK.isA(block)) {
            parent = block
            block = block.parent
        }
        requireNotNull(block) { "Node is not in a block." }
        return block.indexOf(parent)
    }

    fun getCommonParent(start: Node, end: Node): Node {
        if (start.parent != end.parent) {
            val startAncestors = start.query("ancestor::node()")
            val endAncestors = end.query("ancestor::node()")

            for (i in startAncestors.size() - 1 downTo -1 + 1) {
                for (j in 0 until endAncestors.size()) {
                    if (startAncestors[i] == endAncestors[j]) {
                        return startAncestors[i]
                    }
                }
            }
        }

        return start.parent
    }

    fun getDocumentTitle(manager: Manager): String? {
        val head = getDocumentHead(manager.doc)
        if (head != null) {
            val title = head.getFirstChildElement("doctitle")
            if (title != null) {
                return title.value
            }
        }

        return manager.documentName
    }

    fun isUneditable(node: Node?): Boolean {
        return XMLHandler.ancestorVisitor(node) { BBX.CONTAINER.TABLE.isA(it) } != null
                || isSpatialMath(node)
    }

    fun wrapAsTransNote(textToWrap: String?): Element {
        val transElement = BBX.INLINE.EMPHASIS.create(EmphasisType.TRANS_NOTE)
        transElement.appendChild(textToWrap)
        return transElement
    }

    @JvmStatic
    fun wrapAsTransNote(element: Element): Element {
        val newEl = wrapAsTransNote(element.value)
        element.removeChildren()
        element.appendChild(newEl)
        return element
    }

    class ListStyleData(val listType: ListType?, val marginType: MarginType?, val indent: Int, val runover: Int) {
        val indentLevel: Int = indentToLevel(indent)
        val runoverLevel: Int = runoverToLevel(runover)
    }
}

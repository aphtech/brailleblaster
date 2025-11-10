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
package org.brailleblaster.perspectives.braille.views.tree

import nu.xom.Element
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.Range
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.events.XMLCaretEvent
import org.brailleblaster.utils.xml.UTD_NS
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.TreeItem
import java.util.*

class BookTree(dm: Manager?, sash: SashForm?) : TreeView(dm!!, sash!!) {
    private inner class TreeItemData(e: Element, val sectionIndex: Int, val startRange: Int) {
        val list: LinkedList<TextMapElement> = LinkedList()
        var endRange: Int = -1

        init {
            setDataList(this, e, sectionIndex, startRange)
        }

        private fun setDataList(data: TreeItemData, e: Element, section: Int, start: Int) {
            val list = data.list
            val count = e.childCount
            for (i in 0 until count) {
                if (e.getChild(i) is Text) {
                    val index = manager.findNodeIndex(e.getChild(i), section, start)
                    if (index != -1) list.add(manager.getTextMapElement(section, index))
                } else if (e.getChild(i) is Element && (e.getChild(i) as Element).localName != "brl" && !BBX.INLINE.isA(
                        e.getChild(i)
                    )
                ) setDataList(data, e.getChild(i) as Element, section, start)
            }
        }
    }

    private val defaultList = listOf("Centered Heading", "Cell 5 Heading", "Cell 7 Heading", "h4", "h5", "h6")
    private var headings: List<String>? = null
    override var root: TreeItem? = null
        private set
    private var previousItem: TreeItem? = null
    private var selectionListener: SelectionAdapter? = null

    init {
        tree.pack()
    }

    override fun resetView(parent: Composite) {
        root!!.expanded = false
        root!!.dispose()
        root = null
    }

    override fun initializeListeners() {
        tree.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                if (!lock && tree.selection[0] == e.item) {
                    if (e.item != root) {
                        if (manager.needsMapListUpdate()) manager.updateFormatting()
                        val data = getItemData(e.item as TreeItem)
                        manager.checkView(data.list.first())
                        //SetCurrentMessage m = new SetCurrentMessage(Sender.TREE, data.list.getFirst().start, false);
                        manager.simpleManager.dispatchEvent(
                            XMLCaretEvent(
                                Sender.TREE,
                                determineEvent(data.list.first(), 0)
                            )
                        )
                        cursorOffset = 0
                        //manager.dispatch(m);
                    }
                }
            }
        }.also { selectionListener = it })
    }

    override fun removeListeners() {
        tree.removeSelectionListener(selectionListener)
    }

    override fun setRoot(e: Element) {
        root = TreeItem(tree, SWT.LEFT or SWT.BORDER)
        //TODO: update for bbx
//		headings = manager.getArchiver() != null  ? manager.getArchiver().getLevelList() : defaultList;
        headings = defaultList

        root!!.text = manager.documentName

        //		setTree(e, root);
        previousItem = null
    }

    //Finds text for tree item and sets corresponding data
    private fun setNewItemData(item: TreeItem, e: Element) {
        val range = findIndex(item, e)

        if (range != null && range.start != -1) {
            item.data = TreeItemData(e, range.section, range.start)
            if (previousItem != null) {
                if (getItemData(previousItem!!).startRange >= range.start) (previousItem!!.data as TreeItemData).endRange =
                    manager.getSectionSize(
                        getItemData(
                            previousItem!!
                        ).sectionIndex
                    ) - 1
                else (previousItem!!.data as TreeItemData).endRange = range.start - 1
            }

            item.text = getText(e)

            previousItem = item
        } else if (item.itemCount == 0) {
            item.dispose()
        }
    }

    private fun getText(e: Element): String {
        val els = e.childElements
        val count = els.size()
        for (i in 0 until count) {
            if (BBX.BLOCK.isA(els[i]) && isHeading(els[i])) return getCenteredHeadingText(els[i])
        }


        return "SECTION"
    }

    private fun getCenteredHeadingText(e: Element): String {
        val copy = e.copy()
        val count = copy.childCount
        var i = 0
        while (i < count) {
            if ((copy.getChild(i) is Element) && (copy.getChild(i) as Element).localName == "brl") {
                copy.removeChild(copy.getChild(i))
                i--
            }
            i++
        }
        return copy.value
    }

    private fun isHeading(e: Element): Boolean {
        val atr = e.getAttribute("overrideStyle", UTD_NS)
        if (atr != null) {
            return headings!!.contains(atr.value)
        }

        return false
    }

    private fun findIndex(item: TreeItem, e: Element): Range? {
        val count = e.childCount
        for (i in 0 until count) {
            if (e.getChild(i) is Text) {
                val sectionIndex: Int
                val startIndex: Int

                if (previousItem == null) {
                    sectionIndex = 0
                    startIndex = 0
                } else {
                    sectionIndex = getItemData(previousItem!!).sectionIndex
                    startIndex = getItemData(previousItem!!).startRange + 1
                }

                //int section = manager.getSection(searchIndex, e.getChild(i));
                return manager.getRange(sectionIndex, startIndex, e.getChild(i))
                //return new Range(section, manager.findNodeIndex(e.getChild(i), section, 0));
            } else if (e.getChild(i) is Element && (e.getChild(i) as Element).localName != "brl" && (e.getChild(i) as Element).localName != "img" && !BBX.INLINE.isA(
                    e.getChild(i)
                ) && e.getChild(i).childCount != 0
            ) return findIndex(item, e.getChild(i) as Element)
        }

        return null
    }

    private fun isSection(e: Element): Boolean {
        return BBX.SECTION.isA(e)
    }

    override fun setSelection(t: TextMapElement) {
        val section = manager.getSection(t)
        val item = findRange(section, manager.indexOf(section, t))
        if (item != null) {
            tree.setSelection(item)
        } else tree.setSelection(root)
    }

    override fun getSelection(t: TextMapElement?): TextMapElement? {
        return t
    }

    override val selectionIndex: Int
        get() {
            if (tree.selection.isNotEmpty() && tree.selection[0] != root) {
                val parent = tree.selection[0].parentItem
                return parent.indexOf(tree.selection[0])
            } else return 0
        }

    override fun clearTree() {
        tree.removeAll()
    }

    private fun getItemData(item: TreeItem): TreeItemData {
        return item.data as TreeItemData
    }

    private fun findRange(section: Int, index: Int): TreeItem? {
        return searchTree(root, section, index)
    }

    private fun searchTree(item: TreeItem?, section: Int, index: Int): TreeItem? {
        val itemList = getAllItems(section)

        for (treeItem in itemList) {
            if (checkItem(treeItem, section, index)) return treeItem
        }

        return null
    }

    private fun getAllItems(section: Int): List<TreeItem> {
        val allItems: MutableList<TreeItem> = LinkedList()

        for (item in tree.items) getAllItems(item, allItems, section)

        return allItems
    }

    private fun getAllItems(currentItem: TreeItem, allItems: MutableList<TreeItem>, section: Int) {
        val children = currentItem.items

        for (child in children) {
            if (getSection(child) == section) allItems.add(child)
            getAllItems(child, allItems, section)
        }
    }

    private fun getSection(item: TreeItem): Int {
        return (item.data as TreeItemData).sectionIndex
    }

    private fun checkItem(item: TreeItem, section: Int, index: Int): Boolean {
        val data = getItemData(item)
        return (data.sectionIndex == section && index >= data.startRange && index <= data.endRange) || (data.sectionIndex == section && index >= data.startRange && data.endRange == -1)
    }
}

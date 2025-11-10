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
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.settings.UTDManager.Companion.getNextPageNum
import org.brailleblaster.utils.xml.UTD_NS
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.TreeItem

class DividerTree(dm: Manager?, sash: SashForm?) : TreeView(dm!!, sash!!) {
    private inner class TreeItemData(val e: Element) {
        val index = count

        init {
            count++
        }
    }

    private var headings: List<String>? = null
    override var root: TreeItem? = null
        private set
    private var selectionListener: SelectionAdapter? = null
    private var count = 0

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
            }
        }.also { selectionListener = it })
    }

    override fun removeListeners() {
        tree.removeSelectionListener(selectionListener)
    }

    override fun setRoot(e: Element) {
        root = TreeItem(tree, SWT.LEFT or SWT.BORDER)
        //headings = manager.getArchiver() != null  ? manager.getArchiver().getLevelList() : defaultList;
        headings = DEFAULT_HEADING_LIST

        root!!.text = manager.documentName

        setTree(e, root)
    }

    private fun setTree(e: Element, item: TreeItem?) {
        val els = e.childElements

        for (i in 0 until els.size()) {
            if (isSection(els[i])) {
                if (hasBlocks(e)) {
                    val childItem = TreeItem(item, SWT.LEFT or SWT.BORDER)

                    setNewItemData(childItem, els[i])
                    setTree(els[i], childItem)
                } else setTree(els[i], item)
            } else if (els[i].localName != "brl") setTree(els[i], item)
        }
    }

    //Finds text for tree item and sets corresponding data
    private fun setNewItemData(item: TreeItem, e: Element) {
        val t = getText(e)
        val pageNum = getNextPageNum(e)
        item.data = TreeItemData(e)
        if (t != null) {
            item.text = formatTreeString(t.value, pageNum)
        } else {
            item.text = formatTreeString("SECTION", pageNum)
        }
    }

    private fun formatTreeString(item: String, page: String): String {
        return "p. $page:   $item"
    }

    private fun getText(e: Element): Text? {
        //String text = null;
        val els = e.childElements
        val count = els.size()
        for (i in 0 until count) {
            if (BBX.BLOCK.isA(els[i]) && isHeading(els[i])) return getCenteredHeadingText(els[i])
        }

        return null
    }

    private fun getCenteredHeadingText(e: Element): Text? {
        val count = e.childCount
        for (i in 0 until count) {
            if (e.getChild(i) is Element && (e.getChild(i) as Element).localName != "brl") {
                val t = getCenteredHeadingText(e.getChild(i) as Element)
                if (t != null) return t
            } else if (e.getChild(i) is Text) return e.getChild(i) as Text
        }

        return null
    }

    override fun setSelection(t: TextMapElement) {
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

    fun getElement(item: TreeItem): Element {
        return getItemData(item).e
    }

    fun getTreeIndex(item: TreeItem): Int {
        return getItemData(item).index
    }

    private fun isSection(e: Element): Boolean {
        return BBX.SECTION.isA(e)
    }

    private fun hasBlocks(e: Element): Boolean {
        val els = e.childElements
        val count = els.size()
        for (i in 0 until count) {
            if (BBX.BLOCK.isA(els[i]) || BBX.CONTAINER.isA(els[i])) return true
        }

        return false
    }

    private fun isHeading(e: Element): Boolean {
        val atr = e.getAttribute("overrideStyle", UTD_NS)
        if (atr != null) {
            return headings!!.contains(atr.value)
        }

        return false
    }

    companion object {
        private val DEFAULT_HEADING_LIST =
            listOf("Centered Heading", "Cell 5 Heading", "Cell 7 Heading", "h4", "h5", "h6")
    }
}

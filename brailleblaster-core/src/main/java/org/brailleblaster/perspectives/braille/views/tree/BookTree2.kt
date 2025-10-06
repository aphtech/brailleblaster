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
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBXUtils
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.searcher.Searcher
import org.brailleblaster.perspectives.mvc.XMLTextCaret
import org.brailleblaster.perspectives.mvc.events.XMLCaretEvent
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.util.Notify.showMessage
import org.brailleblaster.utils.UTD_NS
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.widgets.Tree
import org.eclipse.swt.widgets.TreeItem

class BookTree2(val manager: Manager, val dialog: BookTreeDialog) {
    private var headings: List<String>? = null

    @JvmField
    val tree: Tree = Tree(dialog.shell, SWT.VIRTUAL or SWT.BORDER)
    var root: TreeItem? = null

    init {
        setRoot(manager.document.rootElement)
        setInitialLocation(manager.mapList.current.node)
        initializeListeners()
    }

    private fun initializeListeners() {
        tree.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == SWT.CR.code) {
                    val success = navigate()
                    if (success) dialog.close()
                }
            }
        })
    }

    private fun setRoot(e: Element) {
        val root = TreeItem(tree, SWT.LEFT or SWT.BORDER)
        headings = DEFAULT_HEADING_LIST

        root.text = BBXUtils.getDocumentTitle(manager)

        setTree(e, root)

        // User will expand first anyway
        root.expanded = true
    }

    private fun setTree(e: Element, item: TreeItem?) {
        val els = e.childElements

        for (i in 0 until els.size()) {
            if (isSection(els[i])) {
                if (hasBlocks(e)) {
                    val childItem = TreeItem(item, SWT.LEFT or SWT.BORDER)

                    setNewItemData(childItem, els[i])
                    setTree(els[i], childItem)
                } else  // TODO: Should we really display empty sections?
                    setTree(els[i], item)
            } else if (els[i].localName != "brl") setTree(els[i], item)
        }
    }

    private fun setInitialLocation(n: Node?) {
        if (n != null) {
            val section = getSection(n)
            val item = findElement(section, tree.getItem(0))
            if (item != null) tree.setSelection(item)
        }
    }

    private fun findElement(e: Element, item: TreeItem): TreeItem? {
        val count = item.itemCount
        for (i in 0 until count) {
            val t = getText(e)
            if (t != null && item.items[i].data == t) return item.getItem(i)
            else if (item.items[i].data == e) return item.getItem(i)
            else if (item.getItem(i).itemCount > 0) {
                val item2 = findElement(e, item.getItem(i))

                if (item2 != null) return item2
            }
        }

        return null
    }

    private fun getSection(n: Node): Element {
        var p = n.parent
        while (!BBX.SECTION.isA(p)) p = p.parent

        return p as Element
    }

    //Finds text for tree item and sets corresponding data
    private fun setNewItemData(item: TreeItem, e: Element) {
        val t = getText(e)
        if (t != null) {
            item.data = t
            item.text = t.value
        } else {
            item.data = e
            item.text = "SECTION"
        }
    }

    private fun getText(e: Element): Text? {
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
            val child = e.getChild(i)
            if (child is Element && child.localName != "brl") {
                val t = getCenteredHeadingText(child)

                if (t != null) return t
            } else if (child is Text) return child
        }
        return null
    }

    private fun isHeading(e: Element): Boolean {
        val atr = e.getAttribute("overrideStyle", UTD_NS)
        if (atr != null) {
            return headings!!.contains(atr.value)
        }

        return false
    }

    private fun hasBlocks(e: Element): Boolean {
        return FastXPath.descendant(e)
            .filterIsInstance<Element>()
            .any { node -> BBX.BLOCK.isA(node) || BBX.CONTAINER.isA(node) && firstTextChild(node) != null }
    }

    private fun firstTextChild(e: Element?): Text? {
        return FastXPath.descendant(e)
            .filterIsInstance<Text>().firstOrNull { node -> Searcher.Filters.noUTDAncestor(node) }
    }

    fun navigate(): Boolean {
        val item = tree.selection[0]
        if (item != null && item.data != null) {
            if (item.data is Text) {
                val t = item.data as Text
                manager.simpleManager.dispatchEvent(XMLCaretEvent(Sender.TREE, XMLTextCaret(t, 0)))
                return true
            } else {
                showMessage("Section has no text to navigate to, please select another")
                return false
            }
        }

        return false
    }

    fun dispose() {
        tree.removeAll()
        tree.dispose()
    }

    private fun isSection(e: Element): Boolean {
        return BBX.SECTION.isA(e)
    }

    companion object {
        private val DEFAULT_HEADING_LIST =
            listOf("Centered Heading", "Cell 5 Heading", "Cell 7 Heading", "h4", "h5", "h6")
    }
}

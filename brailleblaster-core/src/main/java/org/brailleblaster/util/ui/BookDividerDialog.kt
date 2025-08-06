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
package org.brailleblaster.util.ui

import nu.xom.Element
import org.brailleblaster.archiver2.ArchiverFactory
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.views.tree.DividerTree
import org.brailleblaster.util.BookSplitter
import org.brailleblaster.util.Notify.showMessage
import org.brailleblaster.util.YesNoChoice
import org.brailleblaster.wordprocessor.BBFileDialog
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.SashForm
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.layout.FormAttachment
import org.eclipse.swt.layout.FormData
import org.eclipse.swt.layout.FormLayout
import org.eclipse.swt.widgets.*
import java.nio.file.Paths

class BookDividerDialog(val manager: Manager) {
    private class DividerItem(var element: Element?, var start: Int) {
        fun addStart(e: Element?, index: Int) {
            element = e
            this.start = index
        }
    }

    var shell: Shell? = null
    private var sash: SashForm? = null
    private var buttonGroup: Group? = null
    private var volumeGroup: Group? = null
    var table: Table? = null

    private var addStartSection: Button? = null
    var delete: Button? = null
    var split: Button? = null
    var cancel: Button? = null
    var tree: DividerTree? = null

    fun warn(): Boolean {
        val warning = MessageBox(manager.wpManager.shell, SWT.YES or SWT.NO)
        warning.text = localeHandler["warning"]
        warning.message = localeHandler["fileWarning"]

        return warning.open() == SWT.YES
    }

    fun open() {
        shell = Shell(manager.wpManager.shell, SWT.APPLICATION_MODAL or SWT.CLOSE)
        shell!!.text = localeHandler["splitBook"]
        shell!!.layout = FormLayout()

        sash = SashForm(shell, SWT.NONE)
        setFormData(sash!!, 0, 50, 0, 90)
        sash!!.layout = FormLayout()

        tree = DividerTree(manager, sash)
        tree!!.setRoot(manager.document.rootElement)

        volumeGroup = Group(shell, SWT.BORDER)
        volumeGroup!!.text = localeHandler["parts"]
        setFormData(volumeGroup!!, 50, 100, 0, 100)
        volumeGroup!!.layout = FormLayout()

        table = Table(volumeGroup, SWT.BORDER or SWT.CHECK or SWT.FULL_SELECTION or SWT.V_SCROLL or SWT.H_SCROLL)
        setFormData(table!!, 0, 100, 0, 90)
        table!!.linesVisible = true
        table!!.headerVisible = true

        val titles = arrayOf(localeHandler["partNumber"], localeHandler["partStartsAt"])

        for (title in titles) {
            val column = TableColumn(table, SWT.NULL)
            column.text = title
        }

        buttonGroup = Group(shell, SWT.BORDER)
        buttonGroup!!.text = localeHandler["createNewPart"]
        setFormData(buttonGroup!!, 0, 50, 90, 100)
        buttonGroup!!.layout = FillLayout()

        addStartSection = Button(buttonGroup, SWT.BORDER)
        addStartSection!!.text = localeHandler["addStart"]

        cancel = Button(buttonGroup, SWT.BORDER)
        cancel!!.text = localeHandler["cancel"]

        split = Button(volumeGroup, SWT.BORDER)
        split!!.text = localeHandler["divideBook"]
        setFormData(split!!, 0, 50, 90, 100)

        delete = Button(volumeGroup, SWT.BORDER)
        delete!!.text = localeHandler["removeSection"]
        setFormData(delete!!, 50, 100, 90, 100)

        initializeButtons()
        shell!!.pack()
        shell!!.open()

        for (loopIndex in titles.indices) {
            table!!.getColumn(loopIndex).width = table!!.clientArea.width / 2
        }

        tree!!.tree.setFocus()

        val tablist = arrayOf<Control>(
            sash!!, buttonGroup!!, volumeGroup!!
        )
        shell!!.tabList = tablist

        setFirstItem()
    }

    private fun setFirstItem() {
        val root = tree!!.root
        if (root != null && root.itemCount > 0) {
            val item = root.getItem(0)
            val tItem = TableItem(table, SWT.NONE)
            tItem.setText(0, table!!.itemCount.toString())
            tItem.setText(1, item.text)
            addStartElement(item, tItem, tree!!.getElement(item))
        }
    }

    private fun initializeButtons() {
        addStartSection!!.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                val items = tree!!.tree.selection
                val selectedItems = table!!.selection
                if (items.isNotEmpty() && items[0] != tree!!.root && !inTable(tree!!.getElement(items[0]))) {
                    if (selectedItems.isNotEmpty() && !hasStartSection(selectedItems[0])) {
                        val item = items[0]
                        val tItem = selectedItems[0]
                        tItem.setText(1, item.text)
                        addStartElement(item, tItem, tree!!.getElement(item))
                    } else {
                        val item = items[0]
                        val tItem = TableItem(table, SWT.NONE)
                        tItem.setText(0, table!!.itemCount.toString())
                        tItem.setText(1, item.text)
                        addStartElement(item, tItem, tree!!.getElement(item))
                        table!!.setSelection(tItem)
                    }
                }
                sort()
            }
        })

        delete!!.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                val selected = ArrayList<Int>()
                var count = table!!.itemCount
                for (i in 1 until count) {
                    if (table!!.getItem(i).checked) selected.add(i)
                }

                for (i in selected.indices.reversed()) {
                    val item = table!!.getItem(selected[i])
                    table!!.remove(selected[i])
                    item.dispose()
                }

                count = table!!.itemCount
                for (i in 0 until count) {
                    table!!.getItem(i).setText(0, (i + 1).toString())
                }
            }
        })

        tree!!.tree.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == SWT.CR.code) {
                    val items = tree!!.tree.selection
                    if (items.isNotEmpty() && items[0] != tree!!.root) {
                        val item = items[0]
                        val tItem = TableItem(table, SWT.NONE)
                        tItem.setText(0, table!!.itemCount.toString())
                        tItem.setText(1, item.text)
                        addStartElement(item, tItem, tree!!.getElement(item))
                        table!!.setSelection(tItem)
                    }
                }
            }
        })

        split!!.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                split()
            }
        })

        cancel!!.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                shell!!.close()
            }
        })
    }

    private fun split() {
        val list = ArrayList<Element>()
        val count = table!!.itemCount
        if (count <= 1) {
            showMessage("There is nothing selected to be split. Please select multiple parts to complete a split.")
            return
        }
        for (i in 0 until count) {
            val item = table!!.getItem(i).data as DividerItem
            if (i < count - 1) {
                val nextItem = table!!.getItem(i + 1).data as DividerItem
                if (item.start >= nextItem.start) {
                    manager.notify(localeHandler["invalidSplit"])
                    return
                }
            }

            list.add(item.element!!)
        }

        val destDialog = DirectoryDialog(shell, SWT.APPLICATION_MODAL)
        destDialog.message = "Save split books to folder"
        destDialog.filterPath = manager.archiver.path.parent.toAbsolutePath().toString()

        val dest = destDialog.open() ?: return
        val destPath = Paths.get(dest)

        val splitter = BookSplitter(list, manager, destPath)
        splitter.split()

        shell!!.close()
        val openDialog = YesNoChoice(localeHandler["openMessage"], false)
        if (openDialog.result == SWT.YES) openDialogAfterSplit()
    }

    private fun openDialogAfterSplit() {
        val filePath = manager.archiver.path.parent.toString()

        val dialog = BBFileDialog(
            manager.wpManager.shell, SWT.OPEN, null, ArchiverFactory.supportedDescriptionsWithCombinedEntry.toTypedArray(),
            ArchiverFactory.supportedExtensionsWithCombinedEntry.toTypedArray(),
            filterPath = filePath
        )

        val result = dialog.open()
        if (result != null) manager.wpManager.addDocumentManager(Paths.get(result))
    }

    private fun inTable(e: Element): Boolean {
        for (i in 0 until table!!.itemCount) {
            val dItem = table!!.getItem(i).data as DividerItem
            if (dItem.element != null && dItem.element == e) return true
        }

        return false
    }

    private fun addStartElement(treeItem: TreeItem, tableItem: TableItem, e: Element) {
        if (tableItem.data == null) tableItem.data = DividerItem(e, tree!!.getTreeIndex(treeItem))
        else (tableItem.data as DividerItem).addStart(e, tree!!.getTreeIndex(treeItem))
    }

    private fun hasStartSection(item: TableItem): Boolean {
        return (item.data as DividerItem).element != null
    }

    private fun setFormData(c: Control, left: Int, right: Int, top: Int, bottom: Int) {
        val fd = FormData()
        fd.left = FormAttachment(left)
        fd.right = FormAttachment(right)
        fd.top = FormAttachment(top)
        fd.bottom = FormAttachment(bottom)
        c.layoutData = fd
    }

    private fun sort() {
        val array = arrayOfNulls<DividerItem>(table!!.itemCount)
        val string = arrayOfNulls<String>(table!!.itemCount)
        for (i in array.indices) {
            array[i] = table!!.getItem(i).data as DividerItem
            string[i] = table!!.getItem(i).getText(1)
        }
        var sorted = false
        while (!sorted) {
            sorted = true
            for (i in 1 until array.size) {
                val a = array[i - 1]!!.start
                val b = array[i]!!.start
                if (a > b) {
                    sorted = false
                    swap(array, string, i - 1, i)
                }
            }
        }
        for (i in array.indices) {
            val t = table!!.getItem(i)
            t.setText(0, (i + 1).toString())
            t.setText(1, string[i])
            t.data = array[i]
        }
        table!!.itemCount = array.size
    }

    private fun swap(array: Array<DividerItem?>, string: Array<String?>, a: Int, b: Int) {
        val temp = array[a]
        array[a] = array[b]
        array[b] = temp
        val s1 = string[a]
        string[a] = string[b]
        string[b] = s1
    }

    companion object {
        private val localeHandler = getDefault()
    }
}

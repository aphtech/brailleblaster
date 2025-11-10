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
package org.brailleblaster.perspectives.braille.toolbar

import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.MenuManager
import org.brailleblaster.perspectives.mvc.menu.MenuManager.getSharedSelection
import org.brailleblaster.perspectives.mvc.menu.SharedItem
import org.brailleblaster.wordprocessor.WPManager
import org.eclipse.swt.SWT
import org.eclipse.swt.events.PaintEvent
import org.eclipse.swt.events.PaintListener
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.graphics.Rectangle
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.ToolBar
import org.eclipse.swt.widgets.ToolItem
import java.util.function.Consumer
import kotlin.math.max

class ToolBarSection(val name: ToolBarSettings.Settings) : IToolBarElement {
    private val items: MutableList<TBItem> = ArrayList()
    lateinit var relocator: ToolItem
        private set
    lateinit var dockPoint: Rectangle
        private set
    private var widgets: MutableList<ToolItem> = mutableListOf()
    private var listener: PaintListener? = null
    private var relocListener: PaintListener? = null
    var parent: ToolBar? = null
        private set
    var width = 0
        private set
    var height = 0
        private set
    var x = 0
        private set
    var y = 0
        private set

    fun addItem(image: Image, toolTip: String?, onSelect: Consumer<BBSelectionData>) {
        items.add(TBItem(image, toolTip, onSelect, null))
    }

    fun addItem(image: Image, toolTip: String?, sharedItem: SharedItem?) {
        items.add(
            TBItem(
                image, toolTip, getSharedSelection(
                    sharedItem!!
                ), sharedItem
            )
        )
    }

    fun addCheckItem(image: Image, toolTip: String?, selected: Boolean, sharedItem: SharedItem?) {
        items.add(
            TBCheckItem(
                image, toolTip, getSharedSelection(
                    sharedItem!!
                ), sharedItem, selected
            )
        )
    }

    override fun createSection(parent: ToolBar, sharedToolBars: MutableMap<SharedItem, ToolItem>): ToolBar {
        this.parent = parent
        width = 0
        height = 0
        widgets = mutableListOf()
        relocator = addRelocator(parent)
        items.forEach(Consumer { i: TBItem ->
            val checkItem = i is TBCheckItem
            val newItem = ToolItem(parent, if (checkItem) SWT.CHECK else SWT.PUSH)
            i.sharedItem?.let { sharedToolBars[it] = newItem }
            newItem.image = i.image
            newItem.toolTipText = i.toolTip
            if (checkItem) newItem.selection = i.selected
            newItem.addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent) {
                    val data = BBSelectionData(newItem, WPManager.getInstance())
                    data.toolBarItem = newItem
                    if (i.sharedItem != null && MenuManager.sharedMenuItems.containsKey(i.sharedItem)) {
                        data.menuItem = MenuManager.sharedMenuItems[i.sharedItem]
                    }
                    i.onSelect.accept(data)
                }
            })
            widgets.add(newItem)
            height = max(height.toDouble(), newItem.bounds.height.toDouble()).toInt()
            width += newItem.bounds.width
            if (items.indexOf(i) == 0) {
                x = newItem.bounds.x
                y = newItem.bounds.y
            }
        })
        draw(parent)
        drawRelocator(parent)
        dockPoint = Rectangle.WithMonitor(
            relocator.bounds.x,
            relocator.bounds.y,
            relocator.width - RELOCATOR_WIDTH,
            relocator.bounds.height,
            Display.getCurrent().primaryMonitor
        )
        return parent
    }

    private fun addRelocator(parent: ToolBar): ToolItem {
        val relocator = ToolItem(parent, SWT.SEPARATOR)
        relocator.width = RELOCATOR_WIDTH + DOCK_POINT_WIDTH
        return relocator
    }

    fun draw(parent: ToolBar) {
        if (listener != null) {
            parent.removePaintListener(listener)
            listener = null
        }
        if (items.isNotEmpty()) {
            val lines = IntArray(widgets.size - 1)
            val firstBounds = widgets[0].bounds
            val x = firstBounds.x
            val y = firstBounds.y
            val height = firstBounds.height
            var tempWidth = 0
            for (i in widgets.indices) {
                tempWidth += widgets[i].bounds.width
                if (i != widgets.size - 1) lines[i] = tempWidth
            }
            val width = tempWidth
            listener = PaintListener { e: PaintEvent ->
                e.gc.foreground = Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW)
                e.gc.drawRectangle(x, y, width - 1, height - 1)
                for (line in lines) {
                    e.gc.drawLine(x + line, y, x + line, height)
                }
            }
            parent.addPaintListener(listener)
        }
    }

    fun drawRelocator(parent: ToolBar) {
        if (relocListener != null) {
            parent.removePaintListener(relocListener)
            relocListener = null
        }
        val rect = relocator.bounds
        relocListener = PaintListener { e: PaintEvent ->
            val padding = RELOCATOR_WIDTH
            val totalWidth =
                relocator.bounds.width - (RELOCATOR_WIDTH + DOCK_POINT_WIDTH) //Will be 0 unless the width has been changed by mouse drag behavior
            e.gc.foreground = Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW)
            e.gc.drawRectangle(
                rect.x + padding + totalWidth,
                rect.y,
                rect.width - padding - totalWidth,
                rect.height - 1
            )
        }
        parent.addPaintListener(relocListener)
    }

    fun getImageWidth(spacing: Int): Int {
        var width = 0
        for (item in items) {
            width += item.image.bounds.width + spacing
        }
        return width
    }

    private open class TBItem(
        var image: Image,
        var toolTip: String?,
        var onSelect: Consumer<BBSelectionData>,
        var sharedItem: SharedItem?
    )

    private class TBCheckItem(
        image: Image,
        toolTip: String?,
        onSelect: Consumer<BBSelectionData>,
        sharedItem: SharedItem?,
        var selected: Boolean
    ) : TBItem(image, toolTip, onSelect, sharedItem)

    companion object {
        private const val RELOCATOR_WIDTH = 5
        private const val DOCK_POINT_WIDTH = 6
        const val DEFAULT_RELOCATOR_WIDTH = RELOCATOR_WIDTH + DOCK_POINT_WIDTH
    }
}

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
package org.brailleblaster.utils.swt

import org.brailleblaster.utils.swt.EasySWT.error
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import java.util.function.Consumer

/**
 * CompositeBuilder provides an easy way to construct a Composite. Please note: <list>
 *  * A parent Composite must be set, either through setParent() or through the constructor. When
 * a child control is added to this composite, its parent is automatically set by
 * CompositeBuilder.
 *  * If a number of columns is not set, CompositeBuilder will automatically assign enough
 * columns to make its content stay on the same line
 *  * Although there are helper methods to add other controls to the composite, the add() method
 * can take any other ControlBuilder inside EasySWT.
 *  * The downside to using builder syntax is that you cannot keep any references to the controls
 * you add to the composite. This class is useful for making simple composites that do not
 * require any kind of dynamicness (ex. an Ok/Cancel button panel). If dynamicness is required
 * (ex. a text box that you need the text out of), other static methods inside EasySWT can
 * simplify that process </list>
 */
class CompositeBuilder(override var parent: Composite?) : ControlBuilder {
    override var columns: Int? = null
    private val children: MutableList<ControlBuilder> = ArrayList()
    private var width: Int? = null
    private var height: Int? = null
    override var swtOptions: Int = 0
    private var equalColumnWidth = false

    fun setEqualColumnWidth(equalColumnWidth: Boolean): CompositeBuilder {
        this.equalColumnWidth = equalColumnWidth
        return this
    }

    private fun add(control: ControlBuilder): CompositeBuilder {
        children.add(control)
        return this
    }

    fun addButton(text: String?, columns: Int, onClick: Consumer<SelectionEvent>?): CompositeBuilder {
        return add(ButtonBuilder1(text, columns, onClick))
    }

    fun addButton(
        text: String?, width: Int, columns: Int, onClick: Consumer<SelectionEvent>?
    ): CompositeBuilder {
        return add(ButtonBuilder1(text, columns, onClick).setWidth(width))
    }

    override fun build(): Composite {
        if (parent == null) error(EasySWT.NOPARENTSET)
        val newComp = Composite(parent, swtOptions)
        if (columns == null) {
            columns = calculateColumns()
        }
        if (width != null || height != null) {
            GridDataBuilder().setHint(width, height).applyTo(newComp)
        }
        newComp.layout = GridLayout(columns!!, equalColumnWidth)
        for (child in children) {
            child.parent = newComp
            child.build()
        }
        return newComp
    }

    private fun calculateColumns(): Int {
        return children.sumOf { child ->
            val childcols = child.columns
            when {
                childcols != null -> childcols
                child is CompositeBuilder -> child.calculateColumns()
                else -> 0
            }
        }
    }

    override fun setWidth(width: Int): CompositeBuilder {
        this.width = width
        return this
    }

    override fun setHeight(height: Int): CompositeBuilder {
        this.height = height
        return this
    }
}

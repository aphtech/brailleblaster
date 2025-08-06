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
package org.brailleblaster.util

import org.apache.commons.lang3.time.DurationFormatUtils
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.utils.swt.ButtonBuilder
import org.brailleblaster.utils.swt.ComboBuilder
import org.brailleblaster.utils.swt.NumberFilterKeyListener
import org.brailleblaster.utils.swt.TextBuilder
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.events.*
import org.eclipse.swt.graphics.GC
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * Useful utilities for creating the SWT UI
 */
object FormUIUtils {
    private val log: Logger = LoggerFactory.getLogger(FormUIUtils::class.java)

    @JvmStatic
	fun setGridData(c: Control) {
        val gridData = GridData()
        gridData.horizontalAlignment = GridData.FILL
        gridData.verticalAlignment = GridData.FILL
        gridData.grabExcessHorizontalSpace = true
        c.layoutData = gridData
    }

    fun setGridDataVertical(c: Control) {
        setGridData(c)
        (c.layoutData as GridData).grabExcessVerticalSpace = true
    }

    @JvmStatic
	fun setGridDataGroup(group: Group) {
        setGridData(group)
        (group.layoutData as GridData).grabExcessVerticalSpace = true
    }

    /**
     * If the value is different from the getter, update the object with the setter.
     */
	@JvmStatic
	fun <V> updateObject(getter: Supplier<V>, setter: Consumer<V>, value: V?, updateFlag: Boolean): Boolean {
        if (value == null) {
            throw RuntimeException("Value is null.")
        }
        val getterValue: V? = getter.get()
        // Value didn't need updating but still need to pass on flag
        if (getterValue == null || getterValue != value) {
            setter.accept(value)
            return true
        } else return updateFlag
    }

    /**
     * Make dialog that has application modal, meaning the main shell can NOT be
     * clicked on. Is resizable and has dialog trim
     */
    fun makeDialog(manager: Manager): Shell {
        return makeDialog(manager.wpManager.shell)
    }

    @JvmStatic
	fun makeDialog(parent: Shell?): Shell {
        return Shell(parent, SWT.APPLICATION_MODAL or SWT.DIALOG_TRIM or SWT.RESIZE)
    }

    @JvmOverloads
    fun makeButton(parent: Composite?, style: Int = SWT.PUSH): ButtonBuilder {
        return ButtonBuilder(parent!!, style)
    }

    @JvmStatic
	fun makeCheckbox(parent: Composite?): ButtonBuilder {
        return ButtonBuilder(parent!!, SWT.CHECK)
    }

    @JvmStatic
	fun makeComboDropdown(parent: Composite?): ComboBuilder {
        return ComboBuilder(parent!!, SWT.DROP_DOWN or SWT.READ_ONLY)
    }

    @JvmOverloads
    fun makeText(parent: Composite?, style: Int = SWT.BORDER or SWT.SINGLE): TextBuilder {
        return TextBuilder(parent!!, style)
    }

    /**
     * Make dialog that doesn't have any modality, meaning the main shell can be
     * clicked on. Is resizable and has dialog trim
     */
    fun makeDialogFloating(manager: Manager): Shell {
        return makeDialogFloating(manager.wpManager.shell)
    }

    @JvmStatic
	fun makeDialogFloating(parent: Shell?): Shell {
        return Shell(parent, SWT.DIALOG_TRIM or SWT.RESIZE)
    }

    fun addEscapeCloseListener(shell: Shell) {
        shell.addTraverseListener { e: TraverseEvent ->
            if (e.detail == SWT.TRAVERSE_ESCAPE) {
                shell.close()
                e.detail = SWT.TRAVERSE_NONE
                e.doit = false
            }
        }
    }

    /**
     * Calculate sane size for large windows that may run off the screen
     */
	@JvmStatic
	fun setLargeDialogSize(shell: Shell) {
        // Give the window a sane default size and location
        val start = System.currentTimeMillis()
        val size = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT)
        val clientArea = shell.monitor.clientArea
        log.debug(
            "Resizing dialog to {}x{} | Client area x {} y {} width {} height {} | took {}", size.x, size.y,
            clientArea.x, clientArea.y, clientArea.width, clientArea.height,
            DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start)
        )
        val bounds = shell.bounds
        if (size.y > clientArea.height) {
            size.y = clientArea.height
            // Also put window at the top of the screen
            bounds.y = 0
        }
        if (size.x > clientArea.width) {
            size.x = clientArea.width
            // Put the window at the left of the screen
            bounds.x = 0
        }
        if (bounds.x < clientArea.x) {
            bounds.x = clientArea.x
        }
        if (bounds.y < clientArea.y) {
            bounds.y = clientArea.y
        }
        shell.bounds = bounds
        shell.size = size
    }

    /**
     * Generate a standard label
     */
	@JvmStatic
	fun addLabel(parent: Composite?, text: String?): Label {
        val label = newLabel(parent, text)
        setGridData(label)
        return label
    }

    @JvmStatic
	fun newLabel(parent: Composite?, text: String?): Label {
        val label = Label(parent, 0)
        label.text = text
        return label
    }

    @JvmStatic
	fun makeSelectedListener(function: Consumer<SelectionEvent>): SelectionListener {
        return object : SelectionAdapter() {
            override fun widgetSelected(se: SelectionEvent) {
                function.accept(se)
            }
        }
    }

    fun makeModifyListener(function: Consumer<ModifyEvent>): ModifyListener {
        return ModifyListener { function.accept(it) }
    }

    fun makeKeyListener(swtmod: Int, swtchar: Int, function: Consumer<KeyEvent>): KeyAdapter {
        return object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.stateMask == swtmod && e.character.code == swtchar) {
                    function.accept(e)
                }
            }

            override fun keyReleased(e: KeyEvent) {
                // TODO Auto-generated method stub
            }
        }
    }

    fun addSelectionListener(item: MenuItem, function: Consumer<SelectionEvent>): SelectionListener {
        val listener = makeSelectedListener(function)
        item.addSelectionListener(listener)
        return listener
    }

    fun addSelectionListener(item: ToolItem, function: Consumer<SelectionEvent>): SelectionListener {
        val listener = makeSelectedListener(function)
        item.addSelectionListener(listener)
        return listener
    }

    @JvmStatic
	fun addSelectionListener(button: Button, function: Consumer<SelectionEvent>): SelectionListener {
        val listener = makeSelectedListener(function)
        button.addSelectionListener(listener)
        return listener
    }

    fun addSelectionListener(text: StyledText, function: Consumer<SelectionEvent>): SelectionListener {
        val listener = makeSelectedListener(function)
        text.addSelectionListener(listener)
        return listener
    }

    fun addSelectionListener(combo: Combo, function: Consumer<SelectionEvent>): SelectionListener {
        val listener = makeSelectedListener(function)
        combo.addSelectionListener(listener)
        return listener
    }

    fun addSelectionListener(combo: ScrollBar, function: Consumer<SelectionEvent>): SelectionListener {
        val listener = makeSelectedListener(function)
        combo.addSelectionListener(listener)
        return listener
    }

    fun addSelectionListener(spinner: Spinner, function: Consumer<SelectionEvent>): SelectionListener {
        val listener = makeSelectedListener(function)
        spinner.addSelectionListener(listener)
        return listener
    }

    fun addModifyListener(textBox: Text, function: Consumer<ModifyEvent>): ModifyListener {
        val listener = makeModifyListener(function)
        textBox.addModifyListener(listener)
        return listener
    }

    fun addKeyListener(shell: Shell, swtmod: Int, swtchar: Int, function: Consumer<KeyEvent>): KeyAdapter {
        val listener = makeKeyListener(swtmod, swtchar, function)
        shell.addKeyListener(listener)
        return listener
    }

    @JvmStatic
	fun addDoubleFilter(t: Text) {
        addNumberFilter(t, false)
    }

    @JvmStatic
	fun addIntegerFilter(t: Text) {
        addNumberFilter(t, true)
    }

    /**
     * Disallow non-number and non-navigation keys in the text field
     */
    private fun addNumberFilter(t: Text, noDecimal: Boolean) {
        t.addKeyListener(NumberFilterKeyListener(noDecimal))
    }

    fun calcAverageCharWidth(parent: Composite?): Int {
        val gc = GC(parent)
        val averageCharWidth = gc.fontMetrics.averageCharacterWidth.toInt()
        gc.dispose()
        return averageCharWidth
    }

    fun calcAverageCharHeight(parent: Composite?): Int {
        val gc = GC(parent)
        val averageCharHeight = gc.fontMetrics.height
        gc.dispose()
        return averageCharHeight
    }

    fun getBottomIndex(text: StyledText): Int {
        // From JFaceTextUtil.getPartialBottomIndex
        val caHeight = text.clientArea.height
        val lastPixel = caHeight - 1
        // XXX what if there is a margin? can't take trim as this includes the
        // scrollbars which are not part of the client area
        return text.getLineIndex(lastPixel)
    }

    fun scrollViewToCursor(view: StyledText) {
        val offsetLine = view.getLineAtOffset(view.caretOffset)
        val topIndex = view.topIndex
        val bottomIndex = getBottomIndex(view)
        log.debug("Offset at line {}, currently between line {} and {}", offsetLine, topIndex, bottomIndex)
        if (offsetLine !in (bottomIndex + 1)..<topIndex) {
            log.debug("Scrolling")
            view.topIndex = offsetLine - 10
        }
    }

    fun getCaretAfterLineBreaks(view: StyledText, startPos: Int): Int {
        var startPos = startPos
        if (SWT.getPlatform() == "win32") {
            if (startPos > 0) {
                if (view.getTextRange(startPos - 1, 1) == "\r") startPos++
            }
        }
        return startPos
    }

    fun setCaretAfterLineBreaks(view: StyledText, startPos: Int) {
        view.caretOffset = getCaretAfterLineBreaks(view, startPos)
    }

    fun getCaretAtTextNodeOffset(view: StyledText, tme: TextMapElement, offset: Int, manager: Manager): Int {
        var offset = offset
        var text = view.getTextRange(tme.getStart(manager.mapList), offset)
        var lineBreakIndex = text.indexOf(System.lineSeparator())
        while (lineBreakIndex >= 0) {
            offset += System.lineSeparator().length
            text = view.getTextRange(tme.getStart(manager.mapList), offset)
            lineBreakIndex = text.indexOf(System.lineSeparator(), lineBreakIndex + 1)
        }
        return offset + tme.getStart(manager.mapList)
    }

    fun setCaretAtTextNodeOffset(view: StyledText, tme: TextMapElement, offset: Int, manager: Manager) {
        setCaretAfterLineBreaks(view, getCaretAtTextNodeOffset(view, tme, offset, manager))
    }
}

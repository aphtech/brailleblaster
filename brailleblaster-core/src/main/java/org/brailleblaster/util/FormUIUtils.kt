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

import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.widgets.Shell
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Consumer
import java.util.function.Supplier

/**
 * Useful utilities for creating the SWT UI
 */
object FormUIUtils {
    private val log: Logger = LoggerFactory.getLogger(FormUIUtils::class.java)

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
        return EasySWT.makeDialog(manager.wpManager.shell)
    }

    /**
     * Make dialog that doesn't have any modality, meaning the main shell can be
     * clicked on. Is resizable and has dialog trim
     */
    fun makeDialogFloating(manager: Manager): Shell {
        return EasySWT.makeDialogFloating(manager.wpManager.shell)
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
        var lineBreakIndex = text.indexOf(LINE_BREAK)
        while (lineBreakIndex >= 0) {
            offset += LINE_BREAK.length
            text = view.getTextRange(tme.getStart(manager.mapList), offset)
            lineBreakIndex = text.indexOf(LINE_BREAK, lineBreakIndex + 1)
        }
        return offset + tme.getStart(manager.mapList)
    }

    fun setCaretAtTextNodeOffset(view: StyledText, tme: TextMapElement, offset: Int, manager: Manager) {
        setCaretAfterLineBreaks(view, getCaretAtTextNodeOffset(view, tme, offset, manager))
    }
}

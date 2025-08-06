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
package org.brailleblaster.perspectives.braille.views.wp

import nu.xom.Element
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.*
import org.brailleblaster.perspectives.braille.mapping.interfaces.Uneditable
import org.brailleblaster.perspectives.mvc.modules.misc.TableSelectionModule
import org.brailleblaster.utd.actions.GenericBlockAction
import org.eclipse.swt.custom.StyledText

class Validator(var manager: Manager, var view: StyledText) {

    fun isFirstElement(childElement: Element): Boolean {
        var child = childElement
        var parent = child.parent as Element
        while (manager.getAction(parent) !is GenericBlockAction) {
            if (parent.indexOf(child) != 0) return false
            child = parent
            parent = parent.parent as Element
        }
        return parent.indexOf(child) == 0
    }

    fun validCut(
        currentElement: TextMapElement?,
        stateObj: ViewStateObject,
        selectionStart: Int,
        selectionLength: Int
    ): Boolean {
        if (manager.isTableSelected) {
            TableSelectionModule.displayInvalidTableMessage(manager.wpManager.shell)
            return false
        }
        val currentStart = stateObj.currentStart
        val currentEnd = stateObj.currentEnd
        val nextStart = stateObj.nextStart
        if (currentElement is PageIndicatorTextMapElement || currentElement is BoxLineTextMapElement) {
            return if (selectionStart == currentStart && selectionLength == currentEnd - currentStart) false else if (selectionStart in currentStart until currentEnd && selectionLength <= currentEnd - selectionStart) false else selectionStart != currentEnd || selectionLength != 1 || selectionStart + selectionLength != nextStart
        } else if (selectionLength > 0) {
            val t = manager.mapList.getElementInRange(selectionStart)
            return t !is BoxLineTextMapElement && t !is PageIndicatorTextMapElement || selectionStart != t.getEnd(
                manager.mapList
            ) || selectionLength != 1
        }
        return true
    }

    fun validPaste(
        currentElement: TextMapElement?,
        stateObj: ViewStateObject,
        selectionStart: Int,
        selectionLength: Int
    ): Boolean {
        if (manager.isTableSelected) {
            TableSelectionModule.displayInvalidTableMessage(manager.wpManager.shell)
            return false
        }
        val currentStart = stateObj.currentStart
        val currentEnd = stateObj.currentEnd
        if (currentElement is PageIndicatorTextMapElement || currentElement is BoxLineTextMapElement) {
            return if (selectionStart == currentStart && selectionLength == currentEnd - currentStart) false else if (selectionStart in currentStart until currentEnd && selectionLength <= currentEnd - selectionStart) false else view.selectionRanges[1] != 0 && (selectionStart !in currentStart..currentEnd)
        } else if (selectionLength > 0) {
            val t = manager.mapList.getElementInRange(selectionStart)
            return t !is BoxLineTextMapElement && t !is PageIndicatorTextMapElement || selectionStart != t.getEnd(
                manager.mapList
            ) || selectionLength != 1
        }
        return true
    }

    fun validDelete(
        currentElement: TextMapElement?,
        stateObj: ViewStateObject,
        selectionStart: Int,
        selectionLength: Int
    ): Boolean {
        val currentStart = stateObj.currentStart
        val currentEnd = stateObj.currentEnd
        val nextStart = stateObj.nextStart
        if (currentElement is Uneditable) {
            return if (selectionStart >= currentStart && selectionStart + selectionLength <= currentEnd || selectionLength == 0) {
                false
            } else if (selectionLength <= 0 && view.caretOffset == currentEnd) {
                false
            } else selectionStart != currentEnd || selectionStart + selectionLength != nextStart || selectionLength != lineBreakLength
        } else if (selectionLength <= 0 && manager.mapList.inPrintPageRange(view.caretOffset + lineBreakLength) || selectionLength <= 0 && manager.mapList.getElementInRange(
                view.caretOffset + lineBreakLength
            ) is BoxLineTextMapElement
        ) {
            return false
        } else if (selectionLength > 0) {
            val t = manager.mapList.getElementInRange(selectionStart)
            return t !is Uneditable || selectionStart != t.getEnd(manager.mapList) || selectionLength != lineBreakLength
        }
        return true
    }

    fun validBackspace(
        currentElement: TextMapElement?,
        stateObj: ViewStateObject,
        selectionStart: Int,
        selectionLength: Int
    ): Boolean {
        val currentStart = stateObj.currentStart
        val currentEnd = stateObj.currentEnd
        val nextStart = stateObj.nextStart
        if (currentElement is Uneditable) {
            return if (selectionStart >= currentStart && selectionStart + selectionLength <= currentEnd) {
                false
            } else if (selectionLength <= 0) {
                false
            } else selectionStart != currentEnd || selectionStart + selectionLength != nextStart || selectionLength != 1
        } else if (selectionLength <= 0 && currentElement !is WhiteSpaceElement && (manager.mapList.inPrintPageRange(
                view.caretOffset - 1
            ) || manager.mapList.getElementInRange(view.caretOffset - 1) is Uneditable)
        ) {
            return false
        } else if (selectionLength > 0) {
            val t = manager.mapList.getElementInRange(selectionStart)
            return t !is Uneditable || selectionStart != t.getEnd(manager.mapList) || selectionLength != 1
        }
        return true
    }

    companion object {
        var lineBreak: String = System.lineSeparator()
        var lineBreakLength = lineBreak.length
    }
}

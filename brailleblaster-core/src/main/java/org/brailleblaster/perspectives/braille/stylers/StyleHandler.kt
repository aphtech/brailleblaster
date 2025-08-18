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
package org.brailleblaster.perspectives.braille.stylers

import nu.xom.Element
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.document.BrailleDocument
import org.brailleblaster.perspectives.braille.mapping.elements.BoxLineTextMapElement
import org.brailleblaster.perspectives.braille.mapping.elements.PageIndicatorTextMapElement
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.perspectives.braille.mapping.maps.MapList
import org.brailleblaster.perspectives.braille.messages.AdjustLocalStyleMessage
import org.brailleblaster.perspectives.braille.messages.AdjustLocalStyleMessage.*
import org.brailleblaster.perspectives.braille.messages.UpdateStyleMessage
import org.brailleblaster.perspectives.braille.viewInitializer.ViewInitializer
import org.brailleblaster.utd.Style

class StyleHandler(manager: Manager, vi: ViewInitializer?, list: MapList?) : Handler(manager, vi!!, list!!) {
    private val document: BrailleDocument = manager.document

    fun updateStyle(message: UpdateStyleMessage) {
        val e = if (!message.multiSelect()) handleStyleSingleSelected(message)
        else handleStyleMultiSelected(message)

        if (e != null) reformat(e, false)
    }

    /***
     * Handle style if user just move cursor
     * @param message: Message object passed containing information from style table manager
     */
    private fun handleStyleSingleSelected(message: UpdateStyleMessage): Element? {
        val parent = parentStyle(list.current)
        if (!readOnly(parent)) {
            val style = message.style
            document.changeStyle(style, parent)
            return parent
        }

        return null
    }

    /***
     * Apply styles to selected text for multiple elements
     * @param message: Message object passed containing information from style table manager
     */
    private fun handleStyleMultiSelected(message: UpdateStyleMessage): Element? {
        val start = text.selectedText[0]
        val end = text.selectedText[0] + text.selectedText[1]

        val itemSet = manager.mapList.getElementInSelectedRange(start, end)
        val itr: Iterator<TextMapElement> = itemSet.iterator()

        //returned to calling method to be passed to formatter
        var firstEl: Element? = null
        while (itr.hasNext()) {
            val tempElement = itr.next()
            if ((!((tempElement is BoxLineTextMapElement) || (tempElement is PageIndicatorTextMapElement)))) {
                val parent = parentStyle(tempElement)
                if (firstEl == null) firstEl = parent
                val style = message.style
                document.changeStyle(style, parent)
            }
        }

        //clear selection in a way that cursor remains in same position
        text.setCurrentSelection(text.view.caretOffset, text.view.caretOffset)

        return firstEl
    }

    private fun parentStyle(current: TextMapElement): Element {
        val parent = if (current is PageIndicatorTextMapElement || current is BoxLineTextMapElement) current.nodeParent
        else document.getParent(current.node)

        return parent
    }

    /*Local Config */
    fun createAndApplyStyle(t: TextMapElement?, message: AdjustLocalStyleMessage) {
        val e = requireNotNull(message.getElement(manager))
        var style = manager.document.engine.styleMap.findValueOrDefault(e) as Style

        if (message is AdjustIndentMessage) {
            style = manager.document.settingsManager.applyStyleWithOption(
                style,
                Style.StyleOption.FIRST_LINE_INDENT,
                message.indent,
                e
            )
        } else if (message is AdjustMarginMessage) {
            style = manager.document.settingsManager.applyStyleWithOption(
                style,
                Style.StyleOption.INDENT,
                message.margin,
                e
            )
        } else if (message is AdjustAlignmentMessage) {
            style = manager.document.settingsManager.applyStyleWithOption(
                style,
                Style.StyleOption.ALIGN,
                message.alignment,
                e
            )
        } else if (message is AdjustLinesMessage) {
            val lines = message.lines
            style =
                if (message.linesBefore) manager.document.settingsManager.applyStyleWithOption(
                    style,
                    Style.StyleOption.LINES_BEFORE,
                    lines,
                    e
                )
                else manager.document.settingsManager.applyStyleWithOption(
                    style,
                    Style.StyleOption.LINES_AFTER,
                    lines,
                    e
                )
        } else if (message is AdjustPageMessage) {
            val newPagesBefore = message.newPagesBefore
            if (style.newPagesBefore != newPagesBefore) {
                style = manager.document.settingsManager.applyStyleWithOption(
                    style,
                    Style.StyleOption.NEW_PAGES_BEFORE,
                    newPagesBefore,
                    e
                )
            }
            val newPagesAfter = message.newPagesAfter
            if (style.newPagesAfter != newPagesAfter) {
                style = manager.document.settingsManager.applyStyleWithOption(
                    style,
                    Style.StyleOption.NEW_PAGES_AFTER,
                    newPagesAfter,
                    e
                )
            }
        } else {
            throw UnsupportedOperationException("Not implemented: " + message.javaClass.name)
        }

        reformat(e, false)
    }

    companion object {
        @JvmStatic
        fun addStyle(element: Element, style: String?, man: Manager) {
            val replaceStyle = man.document.engine.styleDefinitions.getStyleByName(style)
            //System.out.println("StyleHandler.addStyle: " + style);
            if (style != null) {
                val utd = man.simpleManager.utdManager
                utd.applyStyle(replaceStyle, element, element.document)
            }
        }
    }
}

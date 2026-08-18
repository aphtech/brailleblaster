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
package org.brailleblaster.perspectives.braille.mapping.elements

import nu.xom.Element
import org.brailleblaster.utd.properties.UTDElements

object BrlOnlyBMEFactory {
    private const val TYPE_ATTR = "type"
    private const val LINE_NUMBER_TYPE = "lineNumber"
    private const val RUNNING_HEAD_TYPE = "runningHead"
    private const val PRONUNCIATION_TYPE = "pronunciation"
    private const val GUIDE_WORD_TYPE = "guideWord"
    private const val BOX_LINE_TYPE = "formatting"
    private const val PAGE_INDICATOR_ATTR_NAME = "printIndicator"

    @JvmStatic
    fun createBrlOnlyBME(element: Element): BrlOnlyBrlMapElement {
        require(UTDElements.BRLONLY.isA(element)) { "Element" }
        val attrValue = element.getAttributeValue(TYPE_ATTR)
        val returnElement = checkType(element, attrValue)
        return when {
                returnElement != null -> returnElement
                isBoxLine(element) -> {
                    BoxLineBrlMapElement(element)
                }
                isPageIndicator(element) -> {
                    PageIndicatorBrlMapElement(element)
                }
                else -> {
                    GuideDotsBrlMapElement(element)
                }
            }
    }

    private fun isBoxLine(element: Element): Boolean {
        return element.parent is Element && (element.parent as Element).getAttributeValue(TYPE_ATTR) == BOX_LINE_TYPE
    }

    private fun isPageIndicator(element: Element): Boolean {
        return element.getAttribute(PAGE_INDICATOR_ATTR_NAME) != null
    }

    private fun checkType(element: Element, type: String?): BrlOnlyBrlMapElement? {
        if (type == null) return null
        return when (type) {
            LINE_NUMBER_TYPE -> LineNumberBrlMapElement(element)
            RUNNING_HEAD_TYPE -> RunningHeadBrlMapElement(element)
            PRONUNCIATION_TYPE -> GlossaryPronunciationBrlMapElement(element)
            GUIDE_WORD_TYPE -> GuideWordBrlMapElement(element)
            else -> null
        }
    }
}

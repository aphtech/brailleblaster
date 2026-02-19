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
package org.brailleblaster.utd.internal


import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.utd.IStyle
import org.brailleblaster.utd.IStyleMap
import org.brailleblaster.utd.NamespaceMap
import org.brailleblaster.utd.OverrideMap.AbstractNonMatcherMap
import org.brailleblaster.utd.Style
import org.brailleblaster.utd.Style.StyleOption
import org.brailleblaster.utd.properties.Align
import org.brailleblaster.utd.properties.NumberLinePosition
import org.brailleblaster.utils.xom.attributes
import java.util.*
import kotlin.NoSuchElementException

/**
 * Dynamically create Styles (if needed) with specified overriden settings in attributes
 */
abstract class DynamicOptionStyleMap(
    optionAttribNamespacePrefix: String?,
    optionAttribNamespaceUri: String?,
    optionAttribPrefix: String,
    private val styleNamePrefix: String,
    private val styleCategoryPrefix: String
) : AbstractNonMatcherMap<IStyle>(), IStyleMap {
    override val defaultValue: IStyle
        get() = Style()
    /**
     * Navigate by: Style Name -> Map of Style Options and Values (order irrelevant) -> Style Object
     */
    protected val overrideOptionStyles: MutableMap<String, MutableMap<Map<StyleOption, String>, Style>> =
        HashMap()
    protected val optionAttribNames: Map<String, StyleOption> = buildMap {
        for (option in StyleOption.entries.toTypedArray()) {
            put(styleOptionName(optionAttribPrefix, option), option)
        }
    }

    // Default to blank as xom doesn't return null for the relevant methods
    private val optionAttribNamespacePrefix: String =
        optionAttribNamespacePrefix ?: ""
    private val optionAttribNamespaceUri: String =
        optionAttribNamespaceUri ?: ""

    @Throws(NoSuchElementException::class)
    override fun findValue(node: Node): IStyle {
        if (node !is Element) {
            throw NoSuchElementException("No value found")
        }

        val options = buildMap {
            for (attrib in node.attributes) {
                if (attrib.namespaceURI == optionAttribNamespaceUri && attrib.namespacePrefix == optionAttribNamespacePrefix) {
                    optionAttribNames[attrib.localName]?.let {
                        put(it, attrib.value)
                    }
                }
            }
        }


        if (options.isEmpty()) {
            throw NoSuchElementException("No value found")
        }

        val baseStyle = getBaseStyle(node)
            ?: // some elements don't have styles, shouldn't set options on DEFAULT style
            throw NoSuchElementException("No value found")

        return overrideOptionStyles.computeIfAbsent(
            baseStyle.name
        ) { HashMap() }
            .computeIfAbsent(
                options
            ) { k: Map<StyleOption, String> -> generateStyle(baseStyle, k) }
    }

    /**
     * Get the style of the element.
     *
     * Get the style of the element. If the element does not have a style set then the default style is in use and this should return null to indicate that style options should not be set on it.
     *
     * @param elem The element to get the style for.
     * @return The style of the element or null if it uses the default style.
     */
    protected abstract fun getBaseStyle(elem: Element): Style?

    private fun generateStyle(baseStyle: Style, options: Map<StyleOption, String>): Style {
        val styleName = styleNamePrefix + UUID.randomUUID()
        val newStyle = Style(baseStyle, "$styleCategoryPrefix/$styleName", styleName)
        for ((option, stringValue) in options) {
            when (option) {
                StyleOption.LINES_BEFORE -> {
                    val linesBefore = stringValue.toInt()
                    newStyle.setLinesBefore(linesBefore)
                }

                StyleOption.LINES_AFTER -> {
                    val linesAfter = stringValue.toInt()
                    newStyle.setLinesAfter(linesAfter)
                }

                StyleOption.KEEP_WITH_NEXT -> {
                    val keepWithNext = stringValue.toBoolean()
                    newStyle.isKeepWithNext = keepWithNext
                }

                StyleOption.LINE_NUMBER -> {
                    val lineNumber = stringValue.toBoolean()
                    newStyle.isLineNumber = lineNumber
                }

                StyleOption.SKIP_NUMBER_LINES -> {
                    val skipNumberLines = NumberLinePosition.valueOf(stringValue)
                    newStyle.skipNumberLines = skipNumberLines
                }

                StyleOption.NEW_PAGES_BEFORE -> {
                    val pagesBefore = stringValue.toInt()
                    newStyle.newPagesBefore = pagesBefore
                }

                StyleOption.NEW_PAGES_AFTER -> {
                    val pagesAfter = stringValue.toInt()
                    newStyle.newPagesAfter = pagesAfter
                }

                StyleOption.ALIGN -> newStyle.setAlign(Align.valueOf(stringValue))
                StyleOption.INDENT -> newStyle.setIndent(stringValue.toInt())
                StyleOption.FIRST_LINE_INDENT -> newStyle.setFirstLineIndent(stringValue.toInt())
                StyleOption.GUIDE_WORDS -> {
                    val guideWords = stringValue.toBoolean()
                    newStyle.setGuideWords(guideWords)
                }

                StyleOption.FORMAT -> newStyle.setFormat(IStyle.Format.valueOf(stringValue))
                StyleOption.DONT_SPLIT -> newStyle.isDontSplit = stringValue.toBoolean()
                StyleOption.LINE_LENGTH -> newStyle.lineLength = stringValue.toInt()
                StyleOption.START_SEPARATOR -> newStyle.startSeparator = stringValue
                StyleOption.END_SEPARATOR -> newStyle.endSeparator = stringValue
                StyleOption.COLOR -> newStyle.color = stringValue
                else -> throw UnsupportedOperationException("Not implemented: $option")
            }
        }

        onNewStyle(newStyle)
        return newStyle
    }

    protected abstract fun onNewStyle(newStyle: Style)

    override var namespaces: NamespaceMap
        get() = NAMESPACE_MAP_EMPTY
        set(value) { super.namespaces = value }

    companion object {
        fun styleOptionName(optionAttribPrefix: String, option: StyleOption): String {
            return optionAttribPrefix + option.optionName
        }
    }
}

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
package org.brailleblaster.bbx.utd

import nu.xom.Attribute
import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.bbx.BBX
import org.brailleblaster.perspectives.mvc.modules.misc.StylesBuilder
import org.brailleblaster.settings.UTDManager
import org.brailleblaster.utd.IStyle
import org.brailleblaster.utd.IStyleMap
import org.brailleblaster.utd.Style
import org.brailleblaster.utd.Style.StyleOption
import org.brailleblaster.utd.config.StyleDefinitions
import org.brailleblaster.utd.internal.DynamicOptionStyleMap
import org.brailleblaster.utils.BB_NS
import org.brailleblaster.utils.xom.attributes
import java.util.function.Supplier

/**
 * Dynamically override styles with bb:overrideOption-* attributes
 */
class BBXDynamicOptionStyleMap(
    private val styleDefs: StyleDefinitions,
    private val styleMap: Supplier<out IStyleMap?>
) : DynamicOptionStyleMap(
    BBX.BB_PREFIX,
    BB_NS,
    OPTION_ATTRIB_PREFIX,
    UTDManager.DOCUMENT_STYLE_NAME_PREFIX,
    StylesBuilder.OPTIONS_CATEGORY_NAME
) {
    private var antiRecursionLock = false

    @Throws(NoSuchElementException::class)
    override fun findValue(node: Node): IStyle {
        if (antiRecursionLock) {
            throw NoSuchElementException("No value found")
        }
        return super.findValue(node)
    }

    override fun getBaseStyle(elem: Element): Style? {
        if (BBX._ATTRIB_OVERRIDE_STYLE.has(elem)) {
            val styleName = BBX._ATTRIB_OVERRIDE_STYLE[elem]
            return styleDefs.getStyleByName(styleName)
        } else {
            try {
                antiRecursionLock = true
                val style = styleMap.get()!!.findValueWithDefault(elem, null)
                    ?: // some elements don't have styles, shouldn't set options on DEFAULT style
                    return null
                return style as Style
            } finally {
                antiRecursionLock = false
            }
        }
    }

    override fun onNewStyle(newStyle: Style) {
        styleDefs.addStyle(newStyle)
    }

    fun isStyleOptionAttrib(attrib: Attribute): Boolean {
        return attrib.namespaceURI == BB_NS && attrib.namespacePrefix == BBX.BB_PREFIX && optionAttribNames.containsKey(
            attrib.localName
        )
    }

    fun getStyleOptions(element: Element): List<Attribute> {
        return element.attributes
            .filter { attrib -> this.isStyleOptionAttrib(attrib) }
    }

    val generatedStyles: List<Style>
        get() = overrideOptionStyles.values.flatMap { it.values }

    companion object {
        const val OPTION_ATTRIB_PREFIX: String = "overrideOption-"

        fun setStyleOptionAttrib(element: Element, option: StyleOption, value: Any) {
            val attribName = styleOptionName(OPTION_ATTRIB_PREFIX, option)
            element.addAttribute(Attribute("bb:$attribName", BB_NS, value.toString()))
        }
    }
}

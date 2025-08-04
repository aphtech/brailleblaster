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

import jakarta.xml.bind.annotation.adapters.XmlAdapter
import org.brailleblaster.utd.IStyle
import org.brailleblaster.utd.NamespaceMap
import org.brailleblaster.utd.Style
import org.brailleblaster.utd.StyleMap
import org.brailleblaster.utd.config.StyleDefinitions

class StyleMapAdapter : XmlAdapter<AdaptedStyleMap?, StyleMap?> {
    private val styleDefs: StyleDefinitions?
    private val saveStyleObject: Boolean

    /** For loading  */
    constructor(styleDefs: StyleDefinitions?) {
        this.styleDefs = styleDefs
        saveStyleObject = false
    }

    /** For saving  */
    constructor(saveStyleObject: Boolean) {
        styleDefs = null
        this.saveStyleObject = saveStyleObject
    }

    override fun marshal(actions: StyleMap?): AdaptedStyleMap? = if (actions == null) {
        null
    } else {
        AdaptedStyleMap().apply {
            semanticEntries.addAll(actions.map { (matcher, style) ->
                if (saveStyleObject) {
                    AdaptedStyleMap.Entry(
                        matcher,
                        null,
                        style as Style
                    )
                } else {
                    AdaptedStyleMap.Entry(matcher, style.name, null)
                }
            })
            namespaces = actions.namespaces
        }
    }

    override fun unmarshal(adaptedStyleMap: AdaptedStyleMap?): StyleMap? {
        if (adaptedStyleMap == null) {
            return null
        }
        val styleMap = StyleMap(styleDefs!!.defaultStyle ?: Style())
        styleMap.namespaces = adaptedStyleMap.namespaces ?: NamespaceMap()
        for ((i, entry) in adaptedStyleMap.semanticEntries.withIndex()) {
            var styleResult: IStyle
            val matcher = entry.matcher ?: continue
            if (entry.style != null) {
                styleResult = entry.style
                val baseStyleName = entry.style.baseStyleName
                if (baseStyleName != null) {
                    entry.style.baseStyle = styleDefs.getStyleByName(baseStyleName)
                }
            } else {
                styleResult = styleDefs.getStyleByName(entry.styleName) ?: throw RuntimeException(
                    "Unknown style " + entry.styleName + " for matcher " + matcher
                )
            }
            if (styleMap.containsKey(matcher)) {
                // This will cause a cryptic "IndexOutOfBoundsException: Index: 2, Size: 1"
                // Give more information so someone knows what to fix
                throw RuntimeException(
                    "Detected duplicate matcher " + matcher + " entry " + entry.styleName
                )
            }
            styleMap.put(i, matcher, styleResult)
        }
        return styleMap
    }
}
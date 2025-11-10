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
package org.brailleblaster.bbx.fixers.to3

import jakarta.xml.bind.JAXBElement
import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBX.StringAttribute
import org.brailleblaster.bbx.fixers.AbstractFixer
import org.brailleblaster.bbx.utd.BBXDynamicOptionStyleMap
import org.brailleblaster.bbx.utd.BBXStyleMap
import org.brailleblaster.settings.UTDManager
import org.brailleblaster.utd.*
import org.brailleblaster.utd.Style.StyleOption
import org.brailleblaster.utd.config.DocumentUTDConfig
import org.brailleblaster.utd.config.UTDConfig
import org.brailleblaster.utd.matchers.INodeMatcher
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utils.xml.UTD_NS
import org.brailleblaster.utils.xom.childNodes
import java.util.*
import java.util.function.Consumer

@Suppress("UNUSED")
class DocumentMapImportFixer : AbstractFixer() {
    override fun fix(matchedNode: Node) {
        processStyleMap(matchedNode)
        processActionMap(matchedNode)
    }

    private fun processStyleMap(matchedNode: Node) {
        val documentStyleMap = DocumentUTDConfig.NIMAS.loadStyle(matchedNode.document, STYLE_DEFS)
            ?: return
        for ((curMatcher, value1) in documentStyleMap) {
            val curDocumentStyle = value1 as Style
            var curBaseStyle: Style? = curDocumentStyle
            val styleValues: MutableMap<StyleOption, Any> = EnumMap(StyleOption::class.java)
            while (curBaseStyle != null && curBaseStyle.name.startsWith(UTDManager.DOCUMENT_STYLE_NAME_PREFIX)) {
                for ((curOption, curOptionField) in UTDConfig.STYLE_OPTION_FIELDS) {
                    if (styleValues.containsKey(curOption)) {
                        // furthest style from base takes priority
                        continue
                    }
                    val curValue: JAXBElement<*>? = try {
                        curOptionField[curBaseStyle] as JAXBElement<*>?
                    } catch (e: Exception) {
                        throw RuntimeException("Unable to get field $curOption from $curBaseStyle", e)
                    }
                    if (curValue != null) {
                        styleValues[curOption] = curValue.value
                    }
                }
                curBaseStyle = curBaseStyle.baseStyle
            }
            val actualBaseStyle = curBaseStyle!!
            recursiveMatch(matchedNode.document, curMatcher) { matchedElement: Element ->
                ATTRIB_OVERRIDE_ID_STYLE.detach(matchedElement)
                BBX._ATTRIB_OVERRIDE_STYLE[matchedElement] = actualBaseStyle.name
                for ((curOption, value) in styleValues) {
                    BBXDynamicOptionStyleMap.setStyleOptionAttrib(matchedElement, curOption, value)
                }
            }
        }
        getStyleMapElement(matchedNode)!!.detach()
    }

    private fun processActionMap(matchedNode: Node) {
        val documentActionMap = DocumentUTDConfig.NIMAS.loadActions(matchedNode.document) ?: return
        for ((matcher, curAction) in documentActionMap) {
            recursiveMatch(matchedNode.document, matcher) { matchedElement: Element? ->
                ATTRIB_OVERRIDE_ID_ACTION.detach(matchedElement)
                BBX._ATTRIB_OVERRIDE_ACTION[matchedElement] = OverrideMap.getActionClassName(curAction)
            }
        }
    }

    private fun recursiveMatch(root: Node, matcher: INodeMatcher, onMatch: Consumer<Element>) {
        for (curNode in root.childNodes) {
            if (matcher.isMatch(curNode, BBXStyleMap.BASIC_NAMESPACE_MAP)) {
                onMatch.accept(curNode as Element)
            }
            recursiveMatch(curNode, matcher, onMatch)
        }
    }

    class Matcher : INodeMatcher {
        override fun isMatch(node: Node, namespaces: NamespaceMap): Boolean {
            return getStyleMapElement(node) != null || getActionMapElement(node) != null
        }
    }

    companion object {
        private val STYLE_DEFS = UTDManager.loadStyleDefinitions(UTDManager.preferredFormatStandard)
        val ATTRIB_OVERRIDE_ID_ACTION = StringAttribute(
            "overrideActionId",
            UTDElements.UTD_PREFIX,
            UTD_NS
        )
        val ATTRIB_OVERRIDE_ID_STYLE = StringAttribute(
            "overrideStyleId",
            UTDElements.UTD_PREFIX,
            UTD_NS
        )

        fun getStyleMapElement(node: Node): Element? {
            return DocumentUTDConfig.NIMAS.getConfigElement(node.document, StyleMap::class.java)
        }

        fun getActionMapElement(node: Node): Element? {
            return DocumentUTDConfig.NIMAS.getConfigElement(node.document, ActionMap::class.java)
        }
    }
}
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
package org.brailleblaster.math.mathml

import nu.xom.*
import org.apache.commons.text.StringEscapeUtils
import org.brailleblaster.bbx.BBX
import org.brailleblaster.math.ascii.ASCII2MathML.translate
import org.brailleblaster.math.mathml.MathUtils.removeMathInSelectedRange
import org.brailleblaster.math.numberLine.NumberLine
import org.brailleblaster.math.numberLine.NumberLine.Companion.currentIsNumberLine
import org.brailleblaster.math.spatial.ConnectingContainer
import org.brailleblaster.math.spatial.Grid
import org.brailleblaster.math.spatial.Matrix
import org.brailleblaster.math.spatial.Matrix.Companion.currentIsMatrix
import org.brailleblaster.math.spatial.SpatialMathUtils.currentIsSpatialGrid
import org.brailleblaster.math.spatial.UebTranslations
import org.brailleblaster.math.template.Template
import org.brailleblaster.math.template.TemplateConstants
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.document.BrailleDocument
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.perspectives.braille.mapping.maps.MapList
import org.brailleblaster.perspectives.braille.views.wp.MathEditHandler.makeMathFromTextViewSelection
import org.brailleblaster.utd.MathBraileCode
import org.brailleblaster.utd.asciimath.AsciiMathConverter.toAsciiMath
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.properties.BrailleTableType
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.TextTranslator.translateText
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.wordprocessor.WPManager.Companion.getInstance
import org.eclipse.swt.SWT
import org.mwhapples.jlouis.Louis

object MathModuleUtils {

    private val localeHandler = getDefault()
    const val MATH_HELP_KEY: String = "mathHelp"
    const val ASCII_EDITOR_KEY: String = "asciiEditor"
    const val MATH_TOGGLE_KEY: String = "math"
    const val NEMETH_BLOCK_KEY: String = "nemethBlock"
    const val NEMETH_INLINE_KEY: String = "nemethInline"
    const val NEMETH_TOGGLE_KEY: String = "nemethIndicators"
    const val MATH_TABLE_KEY: String = "mathTable"
    const val MATRIX_KEY: String = "matrix"
    const val NUMBER_LINE_KEY: String = "numberLine"
    const val NUMERIC_PASSAGE_INLINE_KEY: String = "numericInline"
    const val NUMERIC_PASSAGE_BLOCK_KEY: String = "numericBlock"
    const val NUMERIC_SERIES_KEY: String = "numericSeries"
    const val NOT_YET_IMPLEMENTED_KEY: String = "notYetImplemented"
    const val HELP_PREFIX_KEY: String = "mhelp."
    const val SPATIAL_COMBO_KEY: String = "spatialMath"
    const val TEMPLATES_KEY: String = "mathTemplate"

    @JvmField
    val MATH_HELP: String = localeHandler[MATH_HELP_KEY]

    @JvmField
    val ASCII_EDITOR: String = localeHandler[ASCII_EDITOR_KEY]

    @JvmField
    val MATH_TOGGLE: String = localeHandler[MATH_TOGGLE_KEY]

    @JvmField
    val NEMETH_BLOCK: String = localeHandler[NEMETH_BLOCK_KEY]

    @JvmField
    val NEMETH_INLINE: String = localeHandler[NEMETH_INLINE_KEY]

    @JvmField
    val NEMETH_TOGGLE: String = localeHandler[NEMETH_TOGGLE_KEY]
    val TEMPLATES: String = localeHandler[TEMPLATES_KEY]

    @JvmField
    val SPATIAL_COMBO: String = localeHandler[SPATIAL_COMBO_KEY]
    var MATHDEBUG: Boolean = false
    var BASIC_MATH_XML: String =
        StringEscapeUtils.escapeJava("<math xmlns=\"http://www.w3.org/1998/Math/MathML\"><mn>0</mn></math>")

    const val MATH_ATTRIBUTE: String = "alttext"
    const val MATHML_NAMESPACE: String = "http://www.w3.org/1998/Math/MathML"

    @JvmField
    val MATH_TABLE: String = localeHandler[MATH_TABLE_KEY]
    val MATRIX: String = localeHandler[MATRIX_KEY]
    val NUMBER_LINE: String = localeHandler[NUMBER_LINE_KEY]
    val NUMERIC_PASSAGE_INLINE: String = localeHandler[NUMERIC_PASSAGE_INLINE_KEY]
    val NUMERIC_PASSAGE_BLOCK: String = localeHandler[NUMERIC_PASSAGE_BLOCK_KEY]

    @JvmField
    val NUMERIC_SERIES: String = localeHandler[NUMERIC_SERIES_KEY]
    val NOT_YET_IMPLEMENTED: String = localeHandler[NOT_YET_IMPLEMENTED_KEY]
    const val IMAGE_DESCRIBER_KEY: String = "imageDescriber"
    val IMAGE_DESCRIBER: String = localeHandler[IMAGE_DESCRIBER_KEY]

    @JvmField
    val MATH_ACCELERATOR: Int = SWT.MOD1 + 'M'.code
    const val SPATIAL_MATH_KEY: String = "spatialMath"
    val SPATIAL_MATH: String = localeHandler[SPATIAL_MATH_KEY]

    @JvmField
    val SPATIAL_MATH_WARNING: String = localeHandler["spatialMathWarningUseEditor"]
    val END_NEMETH_INDICATOR: String = localeHandler["endNemethIndicator"]
    val BEGIN_NEMETH_INDICATOR: String = localeHandler["beginNemethIndicator"]
    const val STYLE_DEF_SPATIAL_GRID: String = "Spatial Grid"
    const val STYLE_DEF_SPATIAL_BLOCK_BLANK_AFTER: String = "Spatial Math Line Break"
    const val STYLE_DEF_OPTION_LINES_AFTER: String = "linesAfter"
    const val STYLE_DEF_TEMPLATE: String = "Template"
    const val STYLE_DEF_NUMBER_LINE: String = "Number Line"
    const val STYLE_DEF_MATRIX: String = "Matrix"
    const val NBS: String = "\u00a0"
    const val GRADE_1_PASSAGE_START: String = ";;;"
    const val GRADE_1_PASSAGE_END: String = ";'"

    @JvmField
    val LONG_LINE_WARNING: String = localeHandler["lineTooLongWarning"]

    @JvmField
    val NUMERIC_BLOCK: String = localeHandler["numericBlock"]

    @JvmField
    val NUMERIC_INLINE: String = localeHandler["numericInline"]
    const val SPATIAL_MATH_BBX_VERSION: String = "2"

    fun toggleMath(m: Manager) {
        if (m.textView.selectionCount < 1 || m.textView.selectionText.isBlank()) {
            return
        }
        if (selectionContainsMath(m)) {
            removeMathInSelectedRange(m)
        } else {
            makeMathFromTextViewSelection()
        }
    }

    @JvmStatic
    fun isMath(currentNode: Node?): Boolean {
        return (currentNode is Element && currentNode.localName == "math")
                || (currentNode != null && XMLHandler.ancestorElementIs(
            currentNode
        ) { n: Element? -> n != null && n.localName == "math" })
    }

    fun isMathParent(currentNode: Node?): Boolean {
        return currentNode is Element && currentNode.localName == "math"
    }

    @JvmStatic
    fun getMathText(node: Node?): String {
        val string = if (node is Element) node.getAttributeValue("alttext") else ""
        return string ?: ""
    }

    fun getBrl(node: Node): Node? {
        var n: Node? = null
        for (i in 0 until node.parent.childCount) {
            if (UTDElements.BRL.isA(node.parent.getChild(i))) {
                n = node.parent.getChild(i)
                break
            }
        }
        return n
    }

    @JvmStatic
    fun blockContainsMath(curBlock: Node): Boolean {
        if (curBlock.childCount > 0) {
            for (i in 0 until curBlock.childCount) {
                if (blockContainsMath(curBlock.getChild(i))) {
                    return true
                }
            }
            return false
        } else {
            return isMath(curBlock)
        }
    }

    @JvmStatic
    fun selectionContainsMath(m: Manager): Boolean {
        val tmes = m.mapList.getElementsOneByOne(
            m.textView.selection.x,
            m.textView.selection.y
        )
        for (tme in tmes) {
            if (isMath(tme.node)) {
                return true
            }
        }
        return false
    }

    fun getMathParent(node: Node?): Node {
        val node = getMathParentOrNull(node)
        return node ?: throw NoSuchElementException()
    }

    fun getMathParentOrNull(node: Node?): Node? {
        var node = node
        while (node != null && !isMathParent(node)) {
            node = node.parent
        }
        return node
    }

    fun setASCIIText(node: Element) {
        val att = Attribute("alttext", toAsciiMath(Nodes(node), true))
        node.addAttribute(att)
    }

    @JvmStatic
    fun makeMathFromSelection(m: Manager): Node? {
        val selectedText = m.textView.selectionText
        if (selectedText.isEmpty()) {
            return null
        }
        val n = translate(selectedText)
        val inline = BBX.INLINE.MATHML.create()
        inline.appendChild(n)
        return inline
    }

    @JvmStatic
    val isNemeth: Boolean
        get() = MathBraileCode.Nemeth == getInstance().controller.document
            .engine.brailleSettings.mathBrailleCode

    @JvmOverloads
    fun getBrailleText(node: Node, s: String = ""): String {
        var sb = StringBuilder(s)
        for (i in 0 until node.childCount) {
            if (UTDElements.BRL.isA(node.getChild(i))) {
                val brailleNode = node.getChild(i)
                for (j in 0 until brailleNode.childCount) {
                    val brailleNodeChild = brailleNode.getChild(j)
                    if (brailleNodeChild is Text) {
                        sb.append(brailleNodeChild.value)
                    }
                }
            }
            sb = StringBuilder(getBrailleText(node.getChild(i), sb.toString()))
        }
        return sb.toString()
    }

    fun translateMathPrint(s: String): String {
        var s = s
        val translated: String
        if (isNemeth) {
            //Dumb workaround for a translation issue with cross and dot symbols
            if (s == TemplateConstants.MULTIPLY_CROSS_SYMBOL) {
                s = TemplateConstants.MULTIPLY_DOT_SYMBOL
            }
            translated =
                translateText(
                    s, getInstance().controller.document.engine,
                    Louis.TypeForms.PLAIN_TEXT, BrailleTableType.MATH
                )
        } else {
            translated =
                translateText(s, getInstance().controller.document.engine)
                    .replace(UebTranslations.NUMBER_CHAR.toRegex(), "")
        }
        return translated
    }

    fun translateAsciiMath(s: String): String {
        val mathNode = translate(s)
        val ele = BBX.INLINE.MATHML.create()
        ele.appendChild(mathNode)
        getInstance().controller.document.settingsManager.engine.expectedTranslate = true
        val nodes = getInstance().controller.document.settingsManager.engine.translate(ele)
        getInstance().controller.document.settingsManager.engine.expectedTranslate = false

        return getBrailleText(nodes[0])
    }

    fun translateMainPrint(s: String): String {
        return translateText(s, getInstance().controller.document.engine)
    }

    fun translateUncontracted(s: String): String {
        return translateText(
            s,
            getInstance().controller.document.engine,
            Louis.TypeForms.PLAIN_TEXT,
            BrailleTableType.UNCONTRACTED
        )
    }

    @JvmStatic
    fun isSpatialMath(node: Node?): Boolean {
        return node != null && (XMLHandler.ancestorVisitorElement(
            node
        ) { node: Element? -> BBX.CONTAINER.MATRIX.isA(node) } != null || XMLHandler.ancestorVisitorElement(
            node
        ) { node: Element? -> BBX.CONTAINER.NUMBER_LINE.isA(node) } != null || XMLHandler.ancestorVisitorElement(
            node
        ) { node: Element? -> BBX.CONTAINER.CONNECTING_CONTAINER.isA(node) } != null || XMLHandler.ancestorVisitorElement(
            node
        ) { node: Element? -> BBX.CONTAINER.TEMPLATE.isA(node) } != null || XMLHandler.ancestorVisitorElement(
            node
        ) { node: Element? -> BBX.CONTAINER.SPATIAL_GRID.isA(node) } != null)
    }

    fun getSpatialMathParent(node: Node): Element {
        return XMLHandler.ancestorVisitorElement(node) { node: Element? -> BBX.CONTAINER.isA(node) }
            ?: throw NoSuchElementException()
    }

    @JvmStatic
    fun retranslateSpatial(document: BrailleDocument) {
        val nodes = XMLHandler.queryElements(
            document.doc,
            "//*[@bb:type='TEMPLATE']"
        )
        for (n in nodes) {
            Template.initialize(n)
        }
        val nodes2 = XMLHandler.queryElements(
            document.doc,
            "//*[@bb:type='MATRIX']"
        )
        for (n in nodes2) {
            Matrix.initialize(n)
        }
        val nodes3 = XMLHandler.queryElements(
            document.doc,
            "//*[@bb:type='NUMBER_LINE']"
        )
        for (n in nodes3) {
            NumberLine.initialize(n)
        }
        val nodes4 = XMLHandler.queryElements(
            document.doc,
            "//*[@bb:type='CONNECTING_CONTAINER']"
        )
        for (n in nodes4) {
            ConnectingContainer.initialize(n)
        }
        val nodes6 = XMLHandler.queryElements(
            document.doc,
            "//*[@bb:type='SPATIAL_GRID']"
        )
        for (n in nodes6) {
            Grid.initialize(n)
        }
    }

    fun currentIsSpatialMath(): Boolean {
        return currentIsSpatialGrid() || currentIsMatrix() || currentIsNumberLine()
    }

    fun atBeginningSpatialMath(curElement: TextMapElement, list: MapList): Boolean {
        if (!isSpatialMath(curElement.node)) {
            return false
        }
        var index = list.indexOf(curElement)
        if (index == 0) {
            return true
        } else {
            index--
        }
        val previous = list.findPreviousNonWhitespace(index)
        if (previous != null && isSpatialMath(previous.node)) {
            val previousParent = getSpatialMathParent(previous.node)
            val thisParent = getSpatialMathParent(curElement.node)
            return previousParent != thisParent
        } else {
            return true
        }
    }

    fun atEndSpatialMath(curElement: TextMapElement, list: MapList): Boolean {
        if (!isSpatialMath(curElement.node)) {
            return false
        }
        var index = list.indexOf(curElement)
        if (index == list.size - 1) {
            return true
        } else {
            index++
        }
        val nextElement = list.findNextNonWhitespace(index)
        if (isSpatialMath(nextElement.node)) {
            val previousParent = getSpatialMathParent(nextElement.node)
            val thisParent = getSpatialMathParent(curElement.node)
            return previousParent != thisParent
        } else {
            return true
        }
    }
}

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
package org.brailleblaster.utils

import nu.xom.Element
import nu.xom.Elements
import nu.xom.Node
import nu.xom.Text
import org.slf4j.LoggerFactory

object MathMLConverter {
    private val log = LoggerFactory.getLogger(MathMLConverter::class.java)
    const val MATH_BEGIN = "\uf32e"
    const val MATH_END = "\uf32f"
    const val EXPRESSION_BEGIN = "\uf330"
    const val EXPRESSION_SEPARATOR = "\uf331"
    const val EXPRESSION_END = "\uf332"
    const val FRACTION_BEGIN = "\uf500"
    const val FRACTION_SEPARATOR = "\uf501"
    const val FRACTION_SEPARATOR_BEVELLED = "\uf502"
    const val FRACTION_END = "\uf503"
    const val FRACTION_MIXED_BEGIN = "\uf504"
    const val FRACTION_MIXED_SEPARATOR = "\uf505"
    const val FRACTION_MIXED_SEPARATOR_BEVELLED = "\uf506"
    const val FRACTION_MIXED_END = "\uf507"
    const val FRACTION_COMPLEX_BEGIN = "\uf508"
    const val FRACTION_COMPLEX_SEPARATOR = "\uf509"
    const val FRACTION_COMPLEX_SEPARATOR_BEVELLED = "\uf50a"
    const val FRACTION_COMPLEX_END = "\uf50b"
    const val FRACTION_SIMPLE_BEGIN = "\uf50c"
    const val FRACTION_SIMPLE_SEPARATOR = "\uf50d"
    const val FRACTION_SIMPLE_SEPARATOR_BEVELLED = "\uf50e"
    const val FRACTION_SIMPLE_END = "\uf50f"
    const val SUBSCRIPT_BEGIN = "\uf580"
    const val SUBSCRIPT_SEPARATOR = "\uf581"
    const val SUBSCRIPT_END = "\uf582"
    const val SUPERSCRIPT_BEGIN = "\uf583"
    const val SUPERSCRIPT_SEPARATOR = "\uf584"
    const val SUPERSCRIPT_END = "\uf585"
    const val SUBSUPERSCRIPT_BEGIN = "\uf586"
    const val SUBSUPERSCRIPT_SEPARATOR = "\uf587"
    const val SUBSUPERSCRIPT_END = "\uf588"
    const val UNDERSCRIPT_BEGIN = "\uf5a0"
    const val UNDERSCRIPT_SEPARATOR = "\uf5a1"
    const val UNDERSCRIPT_END = "\uf5a2"
    const val OVERSCRIPT_BEGIN = "\uf5a3"
    const val OVERSCRIPT_SEPARATOR = "\uf5a4"
    const val OVERSCRIPT_END = "\uf5a5"
    const val UNDEROVERSCRIPT_BEGIN = "\uf5a6"
    const val UNDEROVERSCRIPT_SEPARATOR = "\uf5a7"
    const val UNDEROVERSCRIPT_END = "\uf5a8"
    const val ROOT_BEGIN = "\uf5b0"
    const val ROOT_SEPARATOR = "\uf5b1"
    const val ROOT_END = "\uf5b2"
    const val SQRT_BEGIN = "\uf5b3"
    const val SQRT_END = "\uf5b4"
    const val TABLE_BEGIN = "\uf600"
    const val TABLE_END = "\uf601"
    const val TABLE_ROW_BEGIN = "\uf602"
    const val TABLE_ROW_END = "\uf603"
    const val TABLE_CELL_BEGIN = "\uf604"
    const val TABLE_CELL_END = "\uf605"
    const val TABLE_ROUND_BEGIN = "\uf610"
    const val TABLE_ROUND_END = "\uf611"
    const val TABLE_SQUARE_BEGIN = "\uf612"
    const val TABLE_SQUARE_END = "\uf613"
    const val TABLE_CURLY_BEGIN = "\uf614"
    const val TABLE_CURLY_END = "\uf615"
    const val TABLE_ANGLED_BEGIN = "\uf616"
    const val TABLE_ANGLED_END = "\uf617"
    const val TABLE_BAR = "\uf618"
    private fun getSiblingsName(siblings: Elements, index: Int): String {
        return try {
            val element = siblings[index]
            element.localName
        } catch (exception: Exception) {
            "ERROR"
        }
    }

    private fun getSiblingsValue(siblings: Elements, index: Int): String? {
        return try {
            val element = siblings[index]
            val node = element.getChild(0)
            (node as? Text)?.value
        } catch (exception: Exception) {
            null
        }
    }

    private fun convert1WithIndicators(
        elements: Elements, begin: String, end: String, indicators: Array<String>?): String? {
        val string: String = convertElement(elements[0], elements, 0, indicators) ?: return null
        return "$begin$string$end"
    }

    private fun convert1WithIndicators(
        element: Element, begin: String, end: String, indicators: Array<String>?): String? {
        val elements = element.childElements
        return if (elements.size() < 1) null else convert1WithIndicators(elements, begin, end, indicators)
    }

    private fun convert2WithIndicators(
        elements: Elements, begin: String?, separator: String, end: String?, indicators: Array<String>?): String? {
        val stringBuilder = StringBuilder()
        stringBuilder.append(begin)

        val zero: Element
        val one: Element
        //For Root, reverse the element order (fix for bug 30891) - MNS
        if (begin.equals(ROOT_BEGIN) && end.equals(ROOT_END) && separator == ROOT_SEPARATOR){
            zero = elements[1]
            one = elements[0]
        }
        else{
            zero = elements[0]
            one = elements[1]
        }
        var string: String? = convertElement(zero, elements, 0, indicators) ?: return null
        stringBuilder.append(string).append(separator)
        string = convertElement(one, elements, 1, indicators)
        if (string == null) return null
        stringBuilder.append(string).append(end)
        return stringBuilder.toString()
    }

    private fun convert2WithIndicators(
        element: Element, begin: String, separator: String, end: String, indicators: Array<String>?): String? {
        val elements = element.childElements
        return if (elements.size() < 2) null else convert2WithIndicators(elements, begin, separator, end, indicators)
    }

    private fun convert3WithIndicators(
        elements: Elements, begin: String, separator: String, end: String, indicators: Array<String>?): String? {
        var string: String? = convertElement(elements[0], elements, 0, indicators) ?: return null
        val stringBuilder = StringBuilder()
        stringBuilder.append(begin).append(string).append(separator)
        string = convertElement(elements[1], elements, 1, indicators)
        if (string == null) return null
        stringBuilder.append(string).append(separator)
        string = convertElement(elements[2], elements, 1, indicators)
        if (string == null) return null
        stringBuilder.append(string).append(end)
        return stringBuilder.toString()
    }

    private fun convert3WithIndicators(
        element: Element, begin: String, separator: String, end: String, indicators: Array<String>?): String? {
        val elements = element.childElements
        return if (elements.size() < 3) null else convert3WithIndicators(elements, begin, separator, end, indicators)
    }

    fun convertElement(element: Element, siblings: Elements, index: Int, indicators: Array<String>?): String? {
        val elements: Elements
        val sibling: Element
        val node: Node
        var bevelled: String
        var begin: String?
        var separator: String
        var end: String?
        val string: String
        return when (element.localName) {
            "mrow" -> EXPRESSION_BEGIN + convertElementChildren(element, indicators) + EXPRESSION_END
            "mn", "mi" -> {
                node = element.getChild(0)
                (node as? Text)?.value
            }
            "mo" -> {
                node = element.getChild(0)
                if (node !is Text) return null

                //  deal with ASCIIMath failures
                if (node.value == "-") {
                    if (index > 0 && getSiblingsName(siblings, index - 1) != "mo") return "\u2212"
                }
                if (node.value == ":") "\u2236" else node.value
            }
            "msub" -> convert2WithIndicators(
                    element, SUBSCRIPT_BEGIN, SUBSCRIPT_SEPARATOR, SUBSCRIPT_END, indicators)
            "msup" -> convert2WithIndicators(
                    element, SUPERSCRIPT_BEGIN, SUPERSCRIPT_SEPARATOR, SUPERSCRIPT_END, indicators)
            "msubsup" -> convert3WithIndicators(
                    element,
                    SUBSUPERSCRIPT_BEGIN,
                    SUBSUPERSCRIPT_SEPARATOR,
                    SUBSUPERSCRIPT_END,
                    indicators)
            "munder" -> convert2WithIndicators(
                    element, UNDERSCRIPT_BEGIN, UNDERSCRIPT_SEPARATOR, UNDERSCRIPT_END, indicators)
            "mover" -> convert2WithIndicators(
                    element, OVERSCRIPT_BEGIN, OVERSCRIPT_SEPARATOR, OVERSCRIPT_END, indicators)
            "munderover" -> convert3WithIndicators(
                    element,
                    UNDEROVERSCRIPT_BEGIN,
                    UNDEROVERSCRIPT_SEPARATOR,
                    UNDEROVERSCRIPT_END,
                    indicators)
            "mroot" -> convert2WithIndicators(element, ROOT_BEGIN, ROOT_SEPARATOR, ROOT_END, indicators)
            "msqrt" -> convert1WithIndicators(element, SQRT_BEGIN, SQRT_END, indicators)
            "mfrac" -> {
                elements = element.childElements
                if (elements.size() < 2) return null
                begin = FRACTION_BEGIN
                separator = FRACTION_SEPARATOR
                bevelled = FRACTION_SEPARATOR_BEVELLED
                end = FRACTION_END
                if (elements[0].localName == "mn" && elements[1].localName == "mn") {
                    if (index > 0) {
                        sibling = siblings[index - 1]
                        if (sibling.localName == "mn") {
                            begin = FRACTION_MIXED_BEGIN
                            separator = FRACTION_MIXED_SEPARATOR
                            bevelled = FRACTION_MIXED_SEPARATOR_BEVELLED
                            end = FRACTION_MIXED_END
                        } else if (sibling.localName == "mo") {
                            node = sibling.getChild(0)
                            if (node !is Text) return null
                            if (node.value == "\u2064") {
                                begin = FRACTION_MIXED_BEGIN
                                separator = FRACTION_MIXED_SEPARATOR
                                bevelled = FRACTION_MIXED_SEPARATOR_BEVELLED
                                end = FRACTION_MIXED_END
                            }
                        } else {
                            begin = FRACTION_SIMPLE_BEGIN
                            separator = FRACTION_SIMPLE_SEPARATOR
                            bevelled = FRACTION_SIMPLE_SEPARATOR_BEVELLED
                            end = FRACTION_SIMPLE_END
                        }
                    } else {
                        begin = FRACTION_SIMPLE_BEGIN
                        separator = FRACTION_SIMPLE_SEPARATOR
                        bevelled = FRACTION_SIMPLE_SEPARATOR_BEVELLED
                        end = FRACTION_SIMPLE_END
                    }
                } else if (elements[0].localName == "mfrac" || elements[1].localName == "mfrac") {
                    begin = FRACTION_COMPLEX_BEGIN
                    separator = FRACTION_COMPLEX_SEPARATOR
                    bevelled = FRACTION_COMPLEX_SEPARATOR_BEVELLED
                    end = FRACTION_COMPLEX_END
                }
                val attribute = element.getAttribute("bevelled")
                if (attribute != null) {
                    string = attribute.value
                    if (string == "true") separator = bevelled
                }
                convert2WithIndicators(elements, begin, separator, end, indicators)
            }
            "mtable" -> {
                begin = getSiblingsValue(siblings, index - 1)
                if (begin == null) return null
                end = getSiblingsValue(siblings, index + 1)
                if (end == null) return null
                val tableOps = arrayOf(when (begin) {
                    "(" -> TABLE_ROUND_BEGIN
                    "[" -> TABLE_SQUARE_BEGIN
                    "{" -> TABLE_CURLY_BEGIN
                    "<" -> TABLE_ANGLED_BEGIN
                    "|" -> TABLE_BAR
                    else -> begin
                },
                when (end) {
                    "(" -> TABLE_ROUND_END
                    "[" -> TABLE_SQUARE_END
                    "{" -> TABLE_CURLY_END
                    "<" -> TABLE_ANGLED_END
                    "|" -> TABLE_BAR
                    else -> end
                })
                TABLE_BEGIN + convertElementChildren(element, tableOps) + TABLE_END
            }
            "mtr" -> {
                if (indicators == null) return null
                if (indicators.size < 2) null else {
                    TABLE_ROW_BEGIN + indicators[0] + convertElementChildren(element, indicators) + indicators[1] + TABLE_ROW_END
                }
            }
            "mtd" -> TABLE_CELL_BEGIN + convertElementChildren(element, indicators) + TABLE_CELL_END
            "mspace" -> " "
            "mfenced", "menclose", "mphantom", "merrer" -> {
                log.error("MathMLConverter:  " + element.localName + "?")
                null
            }
            else -> {
                log.error("MathMLConverter:  " + element.localName + "?")
                null
            }
        }
    }

    fun convertElementChildren(parent: Element, indicators: Array<String>?): String? {
        var string: String?
        val result = StringBuilder()
        val children = parent.childElements
        for (i in 0 until children.size()) {
            val element = children[i]
            string = convertElement(element, children, i, indicators)
            if (string == null) return null
            result.append(string)
        }
        return result.toString()
    }

    fun convertMathML(node: Node?): Text? {
        if (node !is Element) return null
        if (node.localName != "math") return null
        var string = convertElementChildren(node, null)
        if (string == null) {
            if (log.isErrorEnabled) {
                log.error("MathMLConverter:  invalid MathML - " + node.toXML())
            }
            return null
        }

        //  deal with ASCIIMath generating improper mathml
        if (string.contains("\u2236\u2236")) string = string.replace("\u2236\u2236", "\u2237")
        return Text(MATH_BEGIN + string + MATH_END)
    }
}
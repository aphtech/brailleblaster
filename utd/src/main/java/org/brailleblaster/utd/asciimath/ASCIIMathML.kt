/*
 * Copyright (C) 2026 Michael Whapples
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
package org.brailleblaster.utd.asciimath

import org.w3c.dom.Document
import org.w3c.dom.DocumentFragment
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.IOException
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

/** Kotlin implementation of the ASCIIMathML parser used by the UTD converter. */
object ASCIIMathML {
    private const val MATHML_NS = "http://www.w3.org/1998/Math/MathML"

    @JvmStatic
    @Throws(ParserConfigurationException::class, IOException::class)
    fun parseMath(math: String?, latex: Boolean): Element {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val document = factory.newDocumentBuilder().newDocument()
        return Parser(document).parse(math.orEmpty(), latex)
    }

    private enum class Type {
        CONST, UNARY, BINARY, INFIX, LEFTBRACKET, RIGHTBRACKET, SPACE, UNDEROVER,
        DEFINITION, LEFTRIGHT, TEXT, UNARYUNDEROVER, XMLELEMENT, ATTRCONST
    }

    private data class Symbol(
        val input: String,
        val tag: String,
        val output: String,
        val type: Type,
        val attributeName: String? = null,
        val attributeValue: String? = null,
        val function: Boolean = false,
        val accent: Boolean = false,
        val invisible: Boolean = false,
        val rewriteLeftRight: Pair<String, String>? = null
    )

    private data class Result(val node: Node?, val rest: String)

    private class Parser(private val document: Document) {
        private var nestingDepth = 0
        private var currentType: Type? = null
        private var previousType: Type? = null

        fun parse(source: String, @Suppress("UNUSED_PARAMETER") latex: Boolean): Element {
            var input = source
                .replace("&nbsp;", "")
                .replace("&gt;", ">")
                .replace("&lt;", "<")
            input = FUNCTION_CASES.replace(input) { it.value.lowercase() }
            val root = createNode("math")
            val contents = parseExpression(input.replaceFirst(LEADING_WHITESPACE, ""), false).node
            if (contents != null) root.appendChild(contents)
            return root
        }

        private fun createNode(tag: String, child: Node? = null): Element {
            val element = document.createElementNS(MATHML_NS, tag)
            if (child != null) element.appendChild(child)
            return element
        }

        private fun textNode(text: String): Node = document.createTextNode(text)

        private fun removeCharsAndBlanks(value: String, count: Int = 0): String {
            var start = count.coerceAtMost(value.length)
            if (start < value.length && value[start] == '\\' &&
                value.getOrNull(start + 1) != '\\' && value.getOrNull(start + 1) != ' '
            ) {
                start++
            }
            while (start < value.length && value[start].code <= 32) start++
            return value.substring(start)
        }

        private fun getSymbol(value: String): Symbol {
            val symbol = SYMBOLS.firstOrNull { value.startsWith(it.input) }
            previousType = currentType
            if (symbol != null) {
                currentType = symbol.type
                return symbol
            }

            currentType = Type.CONST
            if (value.isEmpty()) return EMPTY_SYMBOL

            var end = 0
            while (end < value.length && value[end] in '0'..'9') end++
            if (end > 0) {
                if (value.getOrNull(end) == '.' && value.getOrNull(end + 1)?.let { it in '0'..'9' } == true) {
                    end += 2
                    while (end < value.length && value[end] in '0'..'9') end++
                }
                val number = value.substring(0, end)
                return Symbol(number, "mn", number, Type.CONST)
            }

            if (value.startsWith(".") && value.getOrNull(1)?.let { it in '0'..'9' } == true) {
                end = 2
                while (end < value.length && value[end] in '0'..'9') end++
                val number = value.substring(0, end)
                return Symbol(number, "mn", number, Type.CONST)
            }

            val character = value.substring(0, 1)
            val tag = if (character[0] in 'A'..'Z' || character[0] in 'a'..'z') "mi" else "mo"
            if (character == "-" && previousType == Type.INFIX) {
                currentType = Type.INFIX
                return Symbol(character, tag, character, Type.UNARY, function = true)
            }
            return Symbol(character, tag, character, Type.CONST)
        }

        private fun parseSexpr(source: String): Result {
            var input = removeCharsAndBlanks(source)
            var symbol = getSymbol(input)
            if (symbol.type == Type.RIGHTBRACKET && nestingDepth > 0) {
                return Result(null, input)
            }
            if (symbol.type == Type.DEFINITION) {
                input = symbol.output + removeCharsAndBlanks(input, symbol.input.length)
                symbol = getSymbol(input)
            }

            var result: Result
            when (symbol.type) {
                Type.CONST, Type.UNDEROVER -> {
                    input = removeCharsAndBlanks(input, symbol.input.length)
                    return Result(createNode(symbol.tag, textNode(symbol.output)), input)
                }
                Type.ATTRCONST -> {
                    input = removeCharsAndBlanks(input, symbol.input.length)
                    return Result(createNode(symbol.tag).also {
                        it.setAttribute(symbol.attributeName!!, symbol.attributeValue!!)
                    }, input)
                }
                Type.LEFTBRACKET -> {
                    nestingDepth++
                    input = removeCharsAndBlanks(input, symbol.input.length)
                    result = parseExpression(input, true)
                    nestingDepth--
                    val row = if (symbol.invisible) {
                        createNode("mrow", result.node)
                    } else {
                        createNode("mrow", createNode("mo", textNode(symbol.output))).also {
                            if (result.node != null) it.appendChild(result.node)
                        }
                    }
                    return Result(row, result.rest)
                }
                Type.TEXT -> {
                    if (symbol.input != "\"") input = removeCharsAndBlanks(input, symbol.input.length)
                    val end = when {
                        input.startsWith("{") -> input.indexOf('}')
                        input.startsWith("(") -> input.indexOf(')')
                        input.startsWith("[") -> input.indexOf(']')
                        symbol.input == "\"" -> input.drop(1).indexOf('"') + 1
                        else -> 0
                    }.let { if (it < 0) input.length else it }
                    val content = input.substring(1.coerceAtMost(input.length), end.coerceAtLeast(1).coerceAtMost(input.length))
                    val fragment = document.createDocumentFragment()
                    if (content.startsWith(" ")) fragment.appendChild(createNode("mspace").also { it.setAttribute("width", "1ex") })
                    fragment.appendChild(createNode(symbol.tag, textNode(content)))
                    if (content.endsWith(" ")) fragment.appendChild(createNode("mspace").also { it.setAttribute("width", "1ex") })
                    return Result(createNode("mrow", fragment), removeCharsAndBlanks(input, end + 1))
                }
                Type.UNARY, Type.UNARYUNDEROVER -> {
                    input = removeCharsAndBlanks(input, symbol.input.length)
                    result = parseSexpr(input)
                    if (result.node == null) {
                        return Result(createNode(symbol.tag, textNode(symbol.output)), input)
                    }
                    if (symbol.function) {
                        val next = input.firstOrNull()
                        if (next == '^' || next == '_' || next == '/' || next == '|' || next == ',' ||
                            (symbol.input.length == 1 && symbol.input[0].isLetterOrDigit() && next != '(')
                        ) {
                            return Result(createNode(symbol.tag, textNode(symbol.output)), input)
                        }
                        return Result(createNode("mrow", createNode(symbol.tag, textNode(symbol.output))).also {
                            it.appendChild(result.node)
                        }, result.rest)
                    }
                    removeBrackets(result.node)
                    if (symbol.input == "sqrt") return Result(createNode(symbol.tag, result.node), result.rest)
                    symbol.rewriteLeftRight?.let { (left, right) ->
                        return Result(createNode("mrow", createNode("mo", textNode(left))).also {
                            it.appendChild(result.node)
                            it.appendChild(createNode("mo", textNode(right)))
                        }, result.rest)
                    }
                    if (symbol.input == "cancel") {
                        return Result(createNode(symbol.tag, result.node).also {
                            it.setAttribute("notation", "updiagonalstrike")
                        }, result.rest)
                    }
                    if (symbol.accent) {
                        return Result(createNode(symbol.tag, result.node).also {
                            it.appendChild(createNode("mo", textNode(symbol.output)))
                        }, result.rest)
                    }
                    return Result(createNode(symbol.tag, result.node).also {
                        if (symbol.attributeName != null && symbol.attributeValue != null) {
                            it.setAttribute(symbol.attributeName, symbol.attributeValue)
                        }
                    }, result.rest)
                }
                Type.BINARY -> {
                    input = removeCharsAndBlanks(input, symbol.input.length)
                    val first = parseSexpr(input)
                    if (first.node == null) return Result(createNode("mo", textNode(symbol.input)), input)
                    removeBrackets(first.node)
                    val second = parseSexpr(first.rest)
                    if (second.node == null) return Result(createNode("mo", textNode(symbol.input)), input)
                    removeBrackets(second.node)
                    if (symbol.input == "color") {
                        val end = when {
                            input.startsWith("{") -> input.indexOf('}')
                            input.startsWith("(") -> input.indexOf(')')
                            input.startsWith("[") -> input.indexOf(']')
                            else -> 0
                        }
                        val color = if (end > 0) input.substring(1, end) else ""
                        return Result(createNode("mstyle", second.node).also { it.setAttribute("mathcolor", color) }, second.rest)
                    }
                    val children = document.createDocumentFragment()
                    if (symbol.input == "root" || symbol.output == "stackrel") children.appendChild(second.node)
                    children.appendChild(first.node)
                    if (symbol.input == "frac" || symbol.input == "bfrac") children.appendChild(second.node)
                    return Result(createNode(symbol.tag, children).also {
                        if (symbol.attributeName != null && symbol.attributeValue != null) {
                            it.setAttribute(symbol.attributeName, symbol.attributeValue)
                        }
                    }, second.rest)
                }
                Type.INFIX -> {
                    input = removeCharsAndBlanks(input, symbol.input.length)
                    return Result(createNode("mo", textNode(symbol.output)), input)
                }
                Type.SPACE -> {
                    input = removeCharsAndBlanks(input, symbol.input.length)
                    val fragment = document.createDocumentFragment()
                    fragment.appendChild(createNode("mspace").also { it.setAttribute("width", "1ex") })
                    fragment.appendChild(createNode(symbol.tag, textNode(symbol.output)))
                    fragment.appendChild(createNode("mspace").also { it.setAttribute("width", "1ex") })
                    return Result(createNode("mrow", fragment), input)
                }
                Type.LEFTRIGHT -> {
                    nestingDepth++
                    input = removeCharsAndBlanks(input, symbol.input.length)
                    result = parseExpression(input, false)
                    nestingDepth--
                    val lastValue = result.node?.lastChild?.firstChild?.nodeValue?.toString().orEmpty()
                    if (lastValue == "|") {
                        return Result(createNode("mrow", createNode("mo", textNode(symbol.output))).also {
                            if (result.node != null) it.appendChild(result.node)
                        }, result.rest)
                    }
                    return Result(createNode("mrow", createNode("mo", textNode("∣"))), input)
                }
                Type.XMLELEMENT -> return parseXmlElement(input, symbol)
                else -> {
                    input = removeCharsAndBlanks(input, symbol.input.length)
                    return Result(createNode(symbol.tag, textNode(symbol.output)), input)
                }
            }
        }

        private fun parseXmlElement(source: String, symbol: Symbol): Result {
            var input = removeCharsAndBlanks(source, symbol.input.length)
            val end = when {
                input.startsWith("<") -> input.indexOf("/>")
                input.startsWith("(") -> input.indexOf(')')
                input.startsWith("[") -> input.indexOf(']')
                input.startsWith("{") -> input.indexOf('}')
                else -> 0
            }.let { if (it < 0) input.length else it }
            var elementInfo = input.substring(1.coerceAtMost(input.length), end.coerceAtLeast(1).coerceAtMost(input.length))
            var consumed = end
            if (input.startsWith("/>", consumed)) consumed += 2
            if (input.getOrNull(consumed) in listOf(')', ']', '}')) consumed++
            input = removeCharsAndBlanks(input.substring(consumed.coerceAtMost(input.length)))

            val tagMatch = XML_TAG.find(elementInfo)
            val tag = tagMatch?.value ?: symbol.tag
            if (tagMatch != null) elementInfo = elementInfo.substring(tagMatch.range.last + 1)
            val element = if (symbol.input == "mml") createNode(tag) else document.createElementNS(symbol.output, tag)
            XML_ATTRIBUTE.findAll(elementInfo).forEach { match ->
                element.setAttribute(match.groupValues[1], match.groupValues[2].drop(1).dropLast(1))
            }
            val result = if (input.startsWith("()")) Result(null, input.substring(2)) else parseSexpr(input)
            result.node?.let { child ->
                val childCount = child.childNodes.length
                removeBrackets(child)
                if (childCount == child.childNodes.length) {
                    element.appendChild(child)
                } else {
                    while (child.hasChildNodes()) element.appendChild(child.firstChild)
                }
            }
            return Result(element, result.rest)
        }

        private fun parseIexpr(source: String): Result {
            var input = removeCharsAndBlanks(source)
            val firstSymbol = getSymbol(input)
            var result = parseSexpr(input)
            var node = result.node
            input = result.rest
            var symbol = getSymbol(input)
            if (symbol.type == Type.INFIX && symbol.input != "/") {
                input = removeCharsAndBlanks(input, symbol.input.length)
                result = parseSexpr(input)
                if (result.node == null) result = Result(createNode("mo", textNode("□")), input)
                else removeBrackets(result.node)
                input = result.rest
                val underOver = firstSymbol.type == Type.UNDEROVER || firstSymbol.type == Type.UNARYUNDEROVER
                if (symbol.input == "_") {
                    val next = getSymbol(input)
                    if (next.input == "^") {
                        input = removeCharsAndBlanks(input, next.input.length)
                        val superscript = parseSexpr(input)
                        if (superscript.node != null) removeBrackets(superscript.node)
                        input = superscript.rest
                        val combined = createNode(if (underOver) "munderover" else "msubsup", node)
                        combined.appendChild(result.node)
                        combined.appendChild(superscript.node ?: createNode("mo", textNode("□")))
                        node = createNode("mrow", combined)
                    } else {
                        node = createNode(if (underOver) "munder" else "msub", node).also { it.appendChild(result.node) }
                    }
                } else if (symbol.input == "^" && underOver) {
                    node = createNode("mover", node).also { it.appendChild(result.node) }
                } else {
                    node = createNode(symbol.tag, node).also { it.appendChild(result.node) }
                }
                if (firstSymbol.function) {
                    val next = getSymbol(input)
                    if (next.type != Type.INFIX && next.type != Type.RIGHTBRACKET) {
                        result = parseIexpr(input)
                        if (result.node != null) {
                            node = createNode("mrow", node).also { it.appendChild(result.node) }
                        }
                        input = result.rest
                    }
                }
            }
            return Result(node, input)
        }

        private fun parseExpression(source: String, rightBracket: Boolean): Result {
            var input = source
            val fragment = document.createDocumentFragment()
            var symbol: Symbol
            do {
                input = removeCharsAndBlanks(input)
                var result = parseIexpr(input)
                var node = result.node
                input = result.rest
                symbol = getSymbol(input)
                if (symbol.type == Type.INFIX && symbol.input == "/") {
                    input = removeCharsAndBlanks(input, symbol.input.length)
                    result = parseIexpr(input)
                    val denominator = result.node ?: createNode("mo", textNode("□"))
                    removeBrackets(denominator)
                    input = result.rest
                    if (node != null) removeBrackets(node)
                    val fraction = createNode(symbol.tag, node)
                    fraction.appendChild(denominator)
                    fragment.appendChild(fraction)
                    symbol = getSymbol(input)
                } else if (node != null) {
                    fragment.appendChild(node)
                }
            } while (
                (symbol.type != Type.RIGHTBRACKET && (symbol.type != Type.LEFTRIGHT || rightBracket) || nestingDepth == 0) &&
                symbol.input.isNotEmpty()
            )

            if (symbol.type == Type.RIGHTBRACKET || symbol.type == Type.LEFTRIGHT) {
                convertMatrix(fragment, symbol)
                input = removeCharsAndBlanks(input, symbol.input.length)
                if (!symbol.invisible) fragment.appendChild(createNode("mo", textNode(symbol.output)))
            }
            return Result(fragment, input)
        }

        private fun convertMatrix(fragment: DocumentFragment, closing: Symbol) {
            val children = (0 until fragment.childNodes.length).map { fragment.childNodes.item(it) }
            val lastRow = children.lastOrNull() as? Element ?: return
            if (lastRow.tagName != "mrow" || lastRow.lastChild?.firstChild?.nodeValue == null) return
            val right = lastRow.lastChild.firstChild.nodeValue.toString()
            if (right != ")" && right != "]") return
            val left = lastRow.firstChild?.firstChild?.nodeValue?.toString() ?: return
            if (!((left == "(" && right == ")" && closing.output != "}") || (left == "[" && right == "]"))) return
            val rows = children.filterIndexed { index, _ -> index % 2 == 0 }
            if (rows.any { it !is Element || it.tagName != "mrow" ||
                    it.firstChild?.firstChild?.nodeValue?.toString() != left ||
                    it.lastChild?.firstChild?.nodeValue?.toString() != right }) return
            if (children.filterIndexed { index, _ -> index % 2 == 1 }.any {
                    it.nodeName != "mo" || it.firstChild?.nodeValue != ","
                }) return

            val commaPositions = rows.map { row ->
                val element = row as Element
                (0 until element.childNodes.length).filter { index ->
                    val child = element.childNodes.item(index)
                    child.nodeName == "mo" && child.firstChild?.nodeValue == ","
                }
            }
            if (commaPositions.any { it.size != commaPositions.first().size }) return
            if (rows.size < 2 && commaPositions.first().isEmpty()) return

            val tableRows = document.createDocumentFragment()
            rows.forEach { rowNode ->
                val row = rowNode as Element
                val tableCells = document.createDocumentFragment()
                var cellContents = document.createDocumentFragment()
                val rowChildren = (0 until row.childNodes.length).map { row.childNodes.item(it) }
                rowChildren.drop(1).dropLast(1).forEach { child ->
                    if (child.nodeName == "mo" && child.firstChild?.nodeValue == ",") {
                        tableCells.appendChild(createNode("mtd", cellContents))
                        cellContents = document.createDocumentFragment()
                    } else {
                        cellContents.appendChild(child)
                    }
                }
                tableCells.appendChild(createNode("mtd", cellContents))
                tableRows.appendChild(createNode("mtr", tableCells))
            }
            while (fragment.hasChildNodes()) fragment.removeChild(fragment.firstChild)
            val table = createNode("mtable", tableRows)
            if (closing.invisible) table.setAttribute("columnalign", "left")
            fragment.appendChild(table)
        }

        private fun removeBrackets(node: Node) {
            if (!node.hasChildNodes() || node.nodeName != "mrow") return
            val first = node.firstChild
            if (first.hasChildNodes() && first.firstChild.nodeValue in listOf("(", "[", "{")) {
                node.removeChild(first)
            }
            val last = node.lastChild
            if (last.hasChildNodes() && last.firstChild.nodeValue in listOf(")", "]", "}")) {
                node.removeChild(last)
            }
        }
    }

    private val LEADING_WHITESPACE = Regex("^[\\u0009-\\u000D\\u0020\\u00A0\\u1680\\u2000-\\u200A\\u2028\\u2029\\u202F\\u205F\\u3000\\uFEFF]+")
    private val FUNCTION_CASES = Regex("Sin|Cos|Tan|Arcsin|Arccos|Arctan|Sinh|Cosh|Tanh|Cot|Sec|Csc|Log|Ln|Abs")
    private val XML_TAG = Regex("^[a-zA-Z_][a-zA-Z0-9_\\-.]*")
    private val XML_ATTRIBUTE = Regex("([a-zA-Z_][a-zA-Z0-9_\\-.]*)=(\"[^\"]*\"|'[^']*')")
    private val EMPTY_SYMBOL = Symbol("", "mo", "", Type.CONST)

    private val SYMBOLS: List<Symbol> by lazy { createSymbols().sortedByDescending { it.input.length } }

    private fun createSymbols(): List<Symbol> {
        val symbols = mutableListOf<Symbol>()
        fun add(
            input: String,
            tag: String = "mo",
            output: String = input,
            type: Type = Type.CONST,
            aliases: List<String> = emptyList(),
            attributeName: String? = null,
            attributeValue: String? = null,
            function: Boolean = false,
            accent: Boolean = false,
            invisible: Boolean = false,
            rewrite: Pair<String, String>? = null
        ) {
            val symbol = Symbol(input, tag, output, type, attributeName, attributeValue, function, accent, invisible, rewrite)
            symbols.add(symbol)
            aliases.forEach { symbols.add(symbol.copy(input = it)) }
        }

        listOf(
            "alpha" to "α", "beta" to "β", "chi" to "χ", "delta" to "δ", "Delta" to "Δ",
            "epsi" to "ε", "varepsilon" to "ɛ", "eta" to "η", "gamma" to "γ", "Gamma" to "Γ",
            "iota" to "ι", "kappa" to "κ", "lambda" to "λ", "Lambda" to "Λ", "lamda" to "λ",
            "Lamda" to "Λ", "mu" to "μ", "nu" to "ν", "omega" to "ω", "Omega" to "Ω",
            "phi" to "ϕ", "varphi" to "φ", "Phi" to "Φ", "pi" to "π", "Pi" to "Π",
            "psi" to "ψ", "Psi" to "Ψ", "rho" to "ρ", "sigma" to "σ", "Sigma" to "Σ",
            "tau" to "τ", "theta" to "θ", "vartheta" to "ϑ", "Theta" to "Θ", "upsilon" to "υ",
            "xi" to "ξ", "Xi" to "Ξ", "zeta" to "ζ"
        ).forEach { (name, value) ->
            val tag = if (name.first().isUpperCase() && name != "Psi") "mo" else "mi"
            add(name, tag, value, aliases = if (name == "epsi") listOf("epsilon") else emptyList())
        }

        add("*", output = "⋅", aliases = listOf("cdot")); add("**", output = "∗", aliases = listOf("ast"))
        add("***", output = "⋆", aliases = listOf("star")); add("//", output = "/")
        add("\\\\", output = "\\", aliases = listOf("backslash")); add("setminus", output = "\\")
        add("xx", output = "×", aliases = listOf("times")); add("|><", output = "⋉", aliases = listOf("ltimes"))
        add("><|", output = "⋊", aliases = listOf("rtimes")); add("|><|", output = "⋈", aliases = listOf("bowtie"))
        add("-:", output = "÷", aliases = listOf("div")); add("divide", output = "-:", type = Type.DEFINITION)
        add("@", output = "∘", aliases = listOf("circ")); add("o+", output = "⊕", aliases = listOf("oplus"))
        add("ox", output = "⊗", aliases = listOf("otimes")); add("o.", output = "⊙", aliases = listOf("odot"))
        add("sum", output = "∑", type = Type.UNDEROVER); add("prod", output = "∏", type = Type.UNDEROVER)
        add("^^", output = "∧", aliases = listOf("wedge")); add("^^^", output = "⋀", type = Type.UNDEROVER, aliases = listOf("bigwedge"))
        add("vv", output = "∨", aliases = listOf("vee")); add("vvv", output = "⋁", type = Type.UNDEROVER, aliases = listOf("bigvee"))
        add("nn", output = "∩", aliases = listOf("cap")); add("nnn", output = "⋂", type = Type.UNDEROVER, aliases = listOf("bigcap"))
        add("uu", output = "∪", aliases = listOf("cup")); add("uuu", output = "⋃", type = Type.UNDEROVER, aliases = listOf("bigcup"))

        add("!=", output = "≠", aliases = listOf("ne")); add(":=", output = ":="); add("lt", output = "<")
        add("<=", output = "≤", aliases = listOf("le")); add("lt=", output = "≤", aliases = listOf("leq")); add("gt", output = ">")
        add(">=", output = "≥", aliases = listOf("ge")); add("gt=", output = "≥", aliases = listOf("geq"))
        add("-<", output = "≺", aliases = listOf("prec")); add("-lt", output = "≺"); add(">-", output = "≻", aliases = listOf("succ"))
        add("-<=", output = "⪯", aliases = listOf("preceq")); add(">-=", output = "⪰", aliases = listOf("succeq"))
        add("in", output = "∈"); add("!in", output = "∉", aliases = listOf("notin")); add("sub", output = "⊂", aliases = listOf("subset"))
        add("sup", output = "⊃", aliases = listOf("supset")); add("sube", output = "⊆", aliases = listOf("subseteq"))
        add("supe", output = "⊇", aliases = listOf("supseteq")); add("-=", output = "≡", aliases = listOf("equiv"))
        add("~=", output = "≅", aliases = listOf("cong")); add("~~", output = "≈", aliases = listOf("approx"))
        add("prop", output = "∝", aliases = listOf("propto"))

        add("and", "mtext", "and", Type.SPACE); add("or", "mtext", "or", Type.SPACE); add("not", output = "¬", aliases = listOf("neg"))
        add("=>", output = "⇒", aliases = listOf("implies")); add("if", output = "if", type = Type.SPACE)
        add("<=>", output = "⇔", aliases = listOf("iff")); add("AA", output = "∀", aliases = listOf("forall"))
        add("EE", output = "∃", aliases = listOf("exists")); add("_|_", output = "⊥", aliases = listOf("bot"))
        add("TT", output = "⊤", aliases = listOf("top")); add("|--", output = "⊢", aliases = listOf("vdash"))
        add("|==", output = "⊨", aliases = listOf("models"))

        add("(", output = "(", type = Type.LEFTBRACKET); add(")", output = ")", type = Type.RIGHTBRACKET)
        add("[", output = "[", type = Type.LEFTBRACKET); add("]", output = "]", type = Type.RIGHTBRACKET)
        add("{", output = "{", type = Type.LEFTBRACKET); add("}", output = "}", type = Type.RIGHTBRACKET)
        add("|", output = "|", type = Type.LEFTRIGHT); add("(:", output = "〈", type = Type.LEFTBRACKET, aliases = listOf("langle"))
        add(":)", output = "〉", type = Type.RIGHTBRACKET, aliases = listOf("rangle")); add("<<", output = "〈", type = Type.LEFTBRACKET)
        add(">>", output = "〉", type = Type.RIGHTBRACKET); add("{:", output = "{:", type = Type.LEFTBRACKET, invisible = true)
        add(":}", output = ":}", type = Type.RIGHTBRACKET, invisible = true)

        add("int", output = "∫"); add("dx", "mi", "{:d x:}", Type.DEFINITION); add("dy", "mi", "{:d y:}", Type.DEFINITION)
        add("dz", "mi", "{:d z:}", Type.DEFINITION); add("dt", "mi", "{:d t:}", Type.DEFINITION)
        add("oint", output = "∮"); add("del", output = "∂", aliases = listOf("partial")); add("grad", output = "∇", aliases = listOf("nabla"))
        add("+-", output = "±", aliases = listOf("pm")); add("O/", output = "∅", aliases = listOf("emptyset")); add("oo", output = "∞", aliases = listOf("infty"))
        add("aleph", output = "ℵ"); add("...", output = "...", aliases = listOf("ldots")); add(":.", output = "∴", aliases = listOf("therefore"))
        add(":'", output = "∵", aliases = listOf("because")); add("/_", output = "∠", aliases = listOf("angle")); add("/_\\", output = "△", aliases = listOf("triangle"))
        add("'", output = "′", aliases = listOf("prime")); add("tilde", "mover", "~", Type.UNARY, accent = true)
        add("newline", output = "newline", type = Type.ATTRCONST, attributeName = "linebreak", attributeValue = "newline")
        add("\\ ", output = " "); add("frown", output = "⌢"); add("quad", output = "  "); add("qquad", output = "    ")
        add("cdots", output = "⋯"); add("vdots", output = "⋮"); add("ddots", output = "⋱"); add("diamond", output = "⋄"); add("square", output = "□")
        add("|__", output = "⌊", aliases = listOf("lfloor")); add("__|", output = "⌋", aliases = listOf("rfloor"))
        add("|~", output = "⌈", aliases = listOf("lceiling")); add("~|", output = "⌉", aliases = listOf("rceiling"))
        add("CC", output = "ℂ"); add("NN", output = "ℕ"); add("QQ", output = "ℚ"); add("RR", output = "ℝ"); add("ZZ", output = "ℤ")
        add("f", "mi", "f", Type.UNARY, function = true); add("g", "mi", "g", Type.UNARY, function = true)

        add("lim", output = "lim", type = Type.UNDEROVER); add("Lim", output = "Lim", type = Type.UNDEROVER)
        listOf("sin", "cos", "tan", "sinh", "cosh", "tanh", "cot", "sec", "csc", "arcsin", "arccos", "arctan", "coth", "sech", "csch", "exp", "log", "ln", "det", "gcd", "lcm").forEach {
            add(it, output = it, type = Type.UNARY, function = true)
        }
        add("abs", output = "abs", type = Type.UNARY, rewrite = "|" to "|"); add("norm", output = "norm", type = Type.UNARY, rewrite = "∥" to "∥")
        add("floor", output = "floor", type = Type.UNARY, rewrite = "⌊" to "⌋"); add("ceil", output = "ceil", type = Type.UNARY, rewrite = "⌈" to "⌉")
        add("dim", output = "dim"); add("mod", output = "mod"); add("lub", output = "lub"); add("glb", output = "glb")
        add("min", output = "min", type = Type.UNDEROVER); add("max", output = "max", type = Type.UNDEROVER)

        add("uarr", output = "↑", aliases = listOf("uparrow")); add("darr", output = "↓", aliases = listOf("downarrow"))
        add("rarr", output = "→", aliases = listOf("rightarrow")); add("->", output = "→", aliases = listOf("to"))
        add(">->", output = "↣", aliases = listOf("rightarrowtail")); add("->>", output = "↠", aliases = listOf("twoheadrightarrow"))
        add(">->>", output = "⤖", aliases = listOf("twoheadrightarrowtail")); add("|->", output = "↦", aliases = listOf("mapsto"))
        add("larr", output = "←", aliases = listOf("leftarrow")); add("harr", output = "↔", aliases = listOf("leftrightarrow"))
        add("rArr", output = "⇒", aliases = listOf("Rightarrow")); add("lArr", output = "⇐", aliases = listOf("Leftarrow"))
        add("hArr", output = "⇔", aliases = listOf("Leftrightarrow"))

        add("sqrt", "msqrt", "sqrt", Type.UNARY); add("root", "mroot", "root", Type.BINARY); add("frac", "mfrac", "/", Type.BINARY)
        add("/", "mfrac", "/", Type.INFIX); add("bfrac", "mfrac", "/", Type.BINARY, attributeName = "bevelled", attributeValue = "true")
        add("stackrel", "mover", "stackrel", Type.BINARY); add("overset", "mover", "stackrel", Type.BINARY); add("underset", "munder", "stackrel", Type.BINARY)
        add("_", "msub", "_", Type.INFIX); add("^", "msup", "^", Type.INFIX); add("__", "munder", "__", Type.INFIX); add("_^", "mover", "_^", Type.INFIX)
        add("hat", "mover", "^", Type.UNARY, accent = true); add("bar", "mover", "¯", Type.UNARY, aliases = listOf("overline"), accent = true)
        add("vec", "mover", "→", Type.UNARY, accent = true); add("dot", "mover", ".", Type.UNARY, accent = true)
        add("ddot", "mover", "..", Type.UNARY, accent = true); add("ul", "munder", "̲", Type.UNARY, aliases = listOf("underline"), accent = true)
        add("ubrace", "munder", "⏟", Type.UNARYUNDEROVER, aliases = listOf("underbrace"), accent = true)
        add("obrace", "mover", "⏞", Type.UNARYUNDEROVER, aliases = listOf("overbrace"), accent = true)
        add("text", "mtext", "text", Type.TEXT); add("mbox", "mtext", "mbox", Type.TEXT)
        add("color", "mstyle", "", Type.BINARY); add("cancel", "menclose", "cancel", Type.UNARY)
        add("\"", "mtext", "mbox", Type.TEXT)
        add("bb", "mstyle", "bb", Type.UNARY, attributeName = "mathvariant", attributeValue = "bold")
        add("mathbf", "mstyle", "mathbf", Type.UNARY, attributeName = "mathvariant", attributeValue = "bold")
        add("sf", "mstyle", "sf", Type.UNARY, attributeName = "mathvariant", attributeValue = "sans-serif")
        add("mathsf", "mstyle", "mathsf", Type.UNARY, attributeName = "mathvariant", attributeValue = "sans-serif")
        add("bbb", "mstyle", "bbb", Type.UNARY, attributeName = "mathvariant", attributeValue = "double-struck")
        add("mathbb", "mstyle", "mathbb", Type.UNARY, attributeName = "mathvariant", attributeValue = "double-struck")
        add("cc", "mstyle", "cc", Type.UNARY, attributeName = "mathvariant", attributeValue = "script")
        add("mathcal", "mstyle", "mathcal", Type.UNARY, attributeName = "mathvariant", attributeValue = "script")
        add("tt", "mstyle", "tt", Type.UNARY, attributeName = "mathvariant", attributeValue = "monospace")
        add("mathtt", "mstyle", "mathtt", Type.UNARY, attributeName = "mathvariant", attributeValue = "monospace")
        add("fr", "mstyle", "fr", Type.UNARY, attributeName = "mathvariant", attributeValue = "fraktur")
        add("mathfrak", "mstyle", "mathfrak", Type.UNARY, attributeName = "mathvariant", attributeValue = "fraktur")
        add("mml", "mrow", "mml", Type.XMLELEMENT)

        return symbols
    }
}
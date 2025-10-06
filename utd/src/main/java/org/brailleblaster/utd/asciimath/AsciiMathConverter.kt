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
package org.brailleblaster.utd.asciimath

import com.oracle.truffle.js.scriptengine.GraalJSScriptEngine
import nu.xom.*
import nu.xom.converters.DOMConverter
import nu.xom.xslt.XSLException
import nu.xom.xslt.XSLTransform
import org.brailleblaster.utd.UTDTranslationEngine
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utils.xom.childNodes
import org.graalvm.polyglot.Context
import org.w3c.dom.Document
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.IOException
import java.io.InputStreamReader
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.function.Predicate
import java.util.stream.Collectors
import java.util.stream.IntStream
import javax.script.Invocable
import javax.script.ScriptException
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

object AsciiMathConverter : AutoCloseable {

    private const val ASCII_MATH_XSLT_PATH: String = "/org/brailleblaster/utd/xslt/MathML2AsciiMath.xsl"
    private const val ASCII_MATH_PARSER_JS: String = "/org/brailleblaster/utd/js/ASCIIMathML.js"
    private const val MATHML_NS = "http://www.w3.org/1998/Math/MathML"
    private val MATHML_ATTRS_LIST: List<String> = listOf(
        "accent",
        "accentunder",
        "actiontype",
        "align",
        "alignmentscope",
        "bevelled",
        "charalign",
        "close",
        "columnalign",
        "columnlines",
        "columnspacing",
        "columnspan",
        "columnwidth",
        "crossout",
        "decimalpoint",
        "denomalign",
        "depth",
        "dir",
        "display",
        "displaystyle",
        "edge",
        "equalcolumns",
        "equalrows",
        "fence",
        "form",
        "frame",
        "framespacing",
        "groupalign",
        "height",
        "href",
        "id",
        "indentalign",
        "indentalignfirst",
        "indentalignlast",
        "indentshift",
        "indentshiftfirst",
        "indentshiftlast",
        "indenttarget",
        "infixlinebreakstyle",
        "largeop",
        "length",
        "linebreak",
        "linebreakmultchar",
        "linebreakstyle",
        "lineleading",
        "linethickness",
        "location",
        "longdivstyle",
        "lspace",
        "lquote",
        "mathbackground",
        "mathcolor",
        "mathsize",
        "mathvariant",
        "maxsize",
        "minlabelspacing",
        "minsize",
        "movablelimits",
        "notation",
        "numalign",
        "open",
        "overflow",
        "position",
        "rowalign",
        "rowlines",
        "rowspacing",
        "rowspan",
        "rspace",
        "rquote",
        "scriptlevel",
        "scriptminsize",
        "scriptsizemultiplier",
        "selection",
        "separator",
        "separators",
        "shift",
        "side",
        "src",
        "stackalign",
        "stretchy",
        "subscriptshift",
        "supscriptshift",
        "symmetric",
        "voffset",
        "width"
    )

    fun getBrailleText(node: Node): String {
        return getBrailleTextFromNode(node, "")
    }

    private fun getBrailleTextFromNode(node: Node, s: String): String {
        var str = StringBuilder(s)
        for (childNode in node.childNodes) {
            if (UTDElements.BRL.isA(childNode)) {
                str.append(childNode.childNodes.filterIsInstance<Text>().joinToString(separator = "") { it.value })
            }
            str = StringBuilder(getBrailleTextFromNode(childNode, str.toString()))
        }
        return str.toString()
    }


    private val jsEngine: GraalJSScriptEngine

    override fun close() {
        jsEngine.close()
    }

    interface ASCIIMathParser {
        fun parseMath(math: String?, latex: Boolean): org.w3c.dom.Element
    }

    private var transformer: XSLTransform
    private val amParser: ASCIIMathParser

    fun toAsciiMath(mathml: Nodes, bbSpaces: Boolean): String {
        return toAsciiMath(mathml, bbSpaces, MathTextFinder.NONE)
    }

    fun toAsciiMath(mathml: Nodes, bbSpaces: Boolean, includeMathMarkers: Boolean): String {
        return toAsciiMath(mathml, bbSpaces, includeMathMarkers, MathTextFinder.NONE)
    }

    fun toAsciiMath(mathml: Nodes, bbSpaces: Boolean, vararg finders: MathTextFinder): String {
        return toAsciiMath(mathml, bbSpaces, false, *finders)
    }

    /**
     * Convert MathML to ASCIIMath.
     *
     *
     * The MathML contained in the Nodes of mathml are converted to ASCIIMath text. When
     * includeMathMarkers is true the accent symbols will be used to surround the ASCIIMath math
     * content. When includeMathMarkers is false these accent signs will be omitted. The finders
     * parameter provides a list of finders which will search the MathML for a cached ASCIIMath string
     * rather than just converting the MathML to ASCIIMath. In this way the MathML can store
     * information about how the ASCIIMath is to be spaced the particular functions used, etc. In the
     * event of a cached ASCIIMath string being found, the includeMathMarkers parameter will be
     * ignored as it is assumed the cached string is in the form the user wants.
     *
     * @param mathml             Nodes representing the MathML to be converted to ASCIIMath.
     * @param includeMathMarkers Should the ` accent marks be used to indicate start and end of math.
     * @param finders            The finders to locate any cached ASCIIMath string which may be contained in the
     * MathML.
     * @return The ASCIIMath string representing the MathML.
     */
    fun toAsciiMath(
        mathml: Nodes,
        bbSpaces: Boolean,
        includeMathMarkers: Boolean,
        vararg finders: MathTextFinder
    ): String {
        val resultText = mathml.joinToString(separator = "") { n ->
            finders.mapNotNull { it.findText(n) }
                .firstOrNull { compareMathML(Nodes(n), toMathML(it, false)) } ?: transformFromMathML(
                n,
                includeMathMarkers
            )
        }
        if (bbSpaces) {
            return resultText.split("\\\\ ".toRegex()).joinToString(separator = " ") { sub -> sub.replace(" ", "\\ ") }
        }
        return resultText
    }

    private fun transformFromMathML(n: Node, includeMathMarkers: Boolean): String {
        val resultNodes: Nodes
        val resultString = StringBuilder()
        val mathMarker = if (includeMathMarkers) "`" else ""
        transformer.setParameter("beginMathSymbol", mathMarker)
        transformer.setParameter("endMathSymbol", mathMarker)
        try {
            resultNodes = transformer.transform(Nodes(n))
        } catch (e: XSLException) {
            throw RuntimeException("Problem transforming document", e)
        }
        for (i in 0 until resultNodes.size()) {
            val r = resultNodes[i]
            if (r is Text) {
                resultString.append(r.value)
            } else {
                throw RuntimeException("The result from transforming is not pure ASCIIMath")
            }
        }
        return resultString.toString()
    }

    init {
        val navigator = JSNavigator("BrailleBlaster UTD")
        val window = JSWindow()
        val dbf = DocumentBuilderFactory.newInstance()
        val jsDoc: Document = try {
            val db = dbf.newDocumentBuilder()
            db.parse(InputSource(StringReader("<html><head/><body/></html>")))
        } catch (e1: ParserConfigurationException) {
            throw RuntimeException("Problem creating the DOM for the JavaScript library", e1)
        } catch (e1: SAXException) {
            throw RuntimeException("Problem creating the DOM for the JavaScript library", e1)
        } catch (e1: IOException) {
            throw RuntimeException("Problem creating the DOM for the JavaScript library", e1)
        }
        val builder = Builder()
        try {
            this.javaClass.getResourceAsStream(ASCII_MATH_XSLT_PATH).use { `in` ->
                val stylesheet = builder.build(`in`)
                transformer = XSLTransform(stylesheet)
            }
        } catch (e: XSLException) {
            throw RuntimeException("Problem creating the ASCIIMath XSL transformer", e)
        } catch (e: ParsingException) {
            throw RuntimeException("Problem parsing the ASCIIMath XSL", e)
        } catch (e: IOException) {
            throw RuntimeException("Problem accessing the ASCIIMath XSL", e)
        }

        jsEngine = GraalJSScriptEngine.create(
            null,
            Context.newBuilder("js")
                .allowAllAccess(true)
                .allowExperimentalOptions(true)
                .option("js.nashorn-compat", "true")
        )
        jsEngine.put("navigator", navigator)
        jsEngine.put("document", jsDoc)
        jsEngine.put("window", window)
        try {
            requireNotNull(javaClass.getResourceAsStream(ASCII_MATH_PARSER_JS)).use { `in` ->
                jsEngine.eval(
                    InputStreamReader(`in`, StandardCharsets.UTF_8)
                )
            }
        } catch (e: ScriptException) {
            throw RuntimeException("Problem running the ASCIIMath parser javascript", e)
        } catch (e: IOException) {
            throw RuntimeException("Problem reading the ASCIIMath parser javascript", e)
        }
        window.onload?.run()
        val o = jsEngine["asciimath"]
        amParser = (jsEngine as Invocable).getInterface(o, ASCIIMathParser::class.java)
    }

    private fun compareMathML(m1: Nodes, m2: Nodes): Boolean {
        val nodePath1 = Stack<AbstractNodeIterator>()
        val nodePath2 = Stack<AbstractNodeIterator>()
        nodePath1.push(NodesIterator(m1) { it is Element || it is Text })
        nodePath2.push(NodesIterator(m2) { it is Element || it is Text })
        while (!(nodePath1.isEmpty() || nodePath2.isEmpty())) {
            val i1 = nodePath1.peek()
            val i2 = nodePath2.peek()
            if ((!i1.hasNext()) && (!i2.hasNext())) {
                nodePath1.pop()
                nodePath2.pop()
                continue
            }
            if (i1.hasNext() != i2.hasNext()) {
                // As one node still has child nodes when the other does not MathML must not match
                return false
            }
            val n1 = i1.next()
            val n2 = i2.next()
            if (n1 is Element && n2 is Element) {
                val matches = compareMathMLElement(n1, n2)
                if (!matches) return false
                // Now process the child nodes
                nodePath1.push(ChildNodeIterator(n1) { it is Element || it is Text })
                nodePath2.push(ChildNodeIterator(n2) { it is Element || it is Text })
                continue
            } else if (n1 is Text && n2 is Text) {
                // For now just check if the text nodes match, we may need to do better in future to account
                // for normalisation
                if (n1.value != n2.value) return false
                continue
            } else {
                return false
            }
        }
        return true
    }

    private fun compareMathMLElement(e1: Element, e2: Element): Boolean {
        // Elements should have same localname and namespace URI.
        if (!(e1.localName == e2.localName && MATHML_NS == e1.namespaceURI && MATHML_NS == e2.namespaceURI)) return false
        // Now check the attributes
        // First filter only MathML attributes
        val attrs1 =
            filterAttributes(
                e1
            ) { a: Attribute -> "" == a.namespaceURI && MATHML_ATTRS_LIST.contains(a.localName) }
        val attrs2 =
            filterAttributes(
                e2
            ) { a: Attribute -> "" == a.namespaceURI && MATHML_ATTRS_LIST.contains(a.localName) }
        // Check the same number of attributes
        if (attrs1.size != attrs2.size) return false
        for (i in attrs1.indices) {
            val a1 = attrs1[i]
            val a2 = attrs2[i]
            if (a1.localName != a2.localName) return false
            if (a1.value != a2.value) return false
        }
        return true
    }

    private fun filterAttributes(elem: Element, p: Predicate<Attribute>): List<Attribute> {
        return IntStream.range(0, elem.attributeCount)
            .mapToObj { i: Int -> elem.getAttribute(i) }
            .filter(p)
            .sorted(Comparator.comparing { obj: Attribute -> obj.localName })
            .collect(Collectors.toList())
    }

    @JvmOverloads
    fun toMathML(
        asciiMath: String?,
        bbSpaces: Boolean,
        addAltText: Boolean = true,
        stripMathMarkers: Boolean = false
    ): Nodes {
        var amStr = asciiMath!!.trim { it <= ' ' }
        var altTextStr = amStr
        if (bbSpaces) {
            amStr = Arrays.stream(amStr.split("\\\\ ".toRegex()).toTypedArray())
                .map { sub: String -> sub.replace(" ", "\\ ") }
                .collect(Collectors.joining(" "))
        }
        if (stripMathMarkers) {
            // The ASCIIMath parser's parseMath method does not strip the ` character
            if (amStr.startsWith("`")) {
                amStr = amStr.take(1)
            } else {
                altTextStr = "`$altTextStr"
            }
            if (amStr.endsWith("`")) {
                amStr = amStr.dropLast(1)
            } else {
                altTextStr = "$altTextStr`"
            }
        }
        val n = amParser.parseMath(amStr, false)
        val e = DOMConverter.convert(n)
        if (addAltText) {
            e.addAttribute(Attribute("alttext", altTextStr))
        }
        return Nodes(e)
    }

    /**
     * For putting the MathML in the SWT Browser view to work with MathJax
     */
    fun toMathMLHTMLNodes(asciiMath: String): org.w3c.dom.Element {
        // The ASCIIMath parser's parseMath method does not strip the ` character
        val amStr = asciiMath.trim { it <= ' ' }.removeSurrounding("`", "`")
        return amParser.parseMath(amStr, false)
    }

    /*
     * For spatial math formatting in BBV2.  Eventually move to UTD and integrate to PB
     */
    fun asciiToBraille(ascii: String?): String {
        val nodes = toMathML(ascii, true)
        val engine = UTDTranslationEngine()
        engine.brailleSettings.isUseAsciiBraille = true
        val translated = engine.translate(nodes[0])
        return getBrailleText(translated[0])
    }
    }

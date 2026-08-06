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
package org.brailleblaster.utd.actions

import jakarta.xml.bind.annotation.XmlAttribute
import nu.xom.*
import nu.xom.xslt.XSLException
import nu.xom.xslt.XSLTransform
import org.brailleblaster.utd.ITranslationEngine
import org.brailleblaster.utd.TextSpan
import org.brailleblaster.utd.exceptions.UTDTranslateException
import org.brailleblaster.utd.properties.BrailleTableType
import org.brailleblaster.utils.MathMLConverter
import java.io.IOException
import java.io.InputStream
import java.net.URI

open class XsltAction @JvmOverloads constructor(@get:XmlAttribute var table: BrailleTableType = BrailleTableType.LITERARY) : GenericBlockAction() {
    protected lateinit var transform: XSLTransform
    protected var altTransform: XSLTransform? = null
    @get:XmlAttribute
    var xsltUri: String? = null
    set(value) = if (value == null) {
        field = null
    } else {
        try {
            val uri = URI.create(value)
            val url = uri.toURL()
            transform = loadXslt(url.openStream())
        } catch (e: IOException) {
            throw IllegalArgumentException("Problem loading specified XSLT", e)
        }
        field = value
        xsltResource = null
    }
    @get:XmlAttribute
    var xsltResource: String? = "/org/brailleblaster/utd/xslt/default.xsl"
    set(value) = if (value == null) {
        field = null
    } else {
        try {
            this.javaClass.getResourceAsStream(value).use { inStream -> transform = loadXslt(inStream) }
        } catch (e: IOException) {
            throw IllegalArgumentException("Problem loading specified XSLT", e)
        }
        field = value
        xsltUri = null
    }

    @get:XmlAttribute
    var altTagName: String? = null
    @get:XmlAttribute
    var altXsltUri: String? = null
    set(value) = if (value == null) {
        field = null
    } else {
        try {
            val uri = URI.create(value)
            val url = uri.toURL()
            altTransform = loadXslt(url.openStream())
            altTransform!!.setParameter("undefinedSymbol", "!UNDEFINED!")
        } catch (e: IOException) {
            throw IllegalArgumentException("Problem loading specified XSLT", e)
        }
        field = value
        altXsltResource = null
    }
    @get:XmlAttribute
    var altXsltResource: String? = null
    set(value) = if (value == null) {
        field = null
    } else {
        try {
            this.javaClass.getResourceAsStream(value).use { inStream ->
                altTransform = loadXslt(inStream)
                altTransform!!.setParameter("undefinedSymbol", "!UNDEFINED!")
            }
        } catch (e: IOException) {
            throw IllegalArgumentException("Problem loading specified XSLT", e)
        }
        field = value
        altXsltUri = null
    }

    @Throws(IOException::class)
    protected fun loadXslt(inStream: InputStream?): XSLTransform {
        return try {
            val builder = Builder()
            val xsltDoc = builder.build(inStream)
            XSLTransform(xsltDoc)
        } catch (e: ParsingException) {
            throw IllegalArgumentException("Problem parsing specified XSLT", e)
        } catch (e: XSLException) {
            throw IllegalArgumentException("Problem creating XSLTransform", e)
        }
    }

    override fun applyTo(node: Node, context: ITranslationEngine): List<TextSpan> {
        return translate(node, table, context)
    }

    override fun translate(
            node: Node,
            tableType: BrailleTableType,
            context: ITranslationEngine): List<TextSpan> {
        // Apply the XSLT
        val results: Nodes
        val text = MathMLConverter.convertMathML(node)
        results = if (text == null) {
            try {
                transform.transform(Nodes(node))
            } catch (e: XSLException) {
                throw RuntimeException("Problem applying the stylesheet", e)
            }
        } else {
            Nodes(text)
        }
        val textToTranslate = StringBuilder()
        for (i in 0 until results.size()) {
            val curNode = results[i]
            if (curNode is Text) {
                textToTranslate.append(curNode.value)
            } else if (curNode is Element) {
                val action = context.actionMap.findValueOrDefault(curNode)
                val spans = action.applyTo(curNode, context)
                // Gather up all the text for a single TextSpan
                for (span in spans) {
                    textToTranslate.append(span.text)
                }
            }
        }
        val thisSpan = TextSpan(node, textToTranslate.toString())
        val spans: List<TextSpan> = listOf(thisSpan)
        try {
            translateString(spans, tableType, context)
        } catch (e: UTDTranslateException) {
            // Don't keep wrapping the exception otherwise it will bubble up to the root level
            throw e
        } catch (e: Exception) {
            throw UTDTranslateException("Failed at processing node " + node.toXML(), e)
        }
        thisSpan.isTranslated = true
        return spans
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = super.hashCode()
        result = prime * result + table.hashCode()
        result = prime * result + xsltResource.hashCode()
        result = prime * result + xsltUri.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || !super.equals(other)) return false
        if (javaClass != other.javaClass) return false
        val o = other as XsltAction
        return table == o.table && xsltResource == o.xsltResource && xsltUri == o.xsltUri
    }

}
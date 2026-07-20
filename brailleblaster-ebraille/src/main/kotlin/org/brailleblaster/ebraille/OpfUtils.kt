/*
 * Copyright (C) 2026 American Printing House for the Blind
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
package org.brailleblaster.ebraille

import nu.xom.Attribute
import nu.xom.Document
import nu.xom.Element
import org.brailleblaster.utils.xml.DC_NS
import org.brailleblaster.utils.xml.OPF_NS
import java.io.OutputStream
import java.net.URL
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun createOpf(items: List<PackageItem>): Document = Document(Element("package", OPF_NS).apply {
    val itemMap = items.mapIndexed { i, item -> "file${i}" to item }.toMap()
    addNamespaceDeclaration("dc", DC_NS)
    addAttribute(Attribute("version", "3.0"))
    addAttribute(Attribute("unique-identifier", "bookid"))
    appendChild(Element("metadata", OPF_NS).apply {
        val nowDateTime = LocalDateTime.now(Clock.systemUTC()).truncatedTo(ChronoUnit.SECONDS)
        appendChild(Element("dc:format", DC_NS).apply {
            appendChild("eBraille 1.0")
        })
        appendChild(Element("dc:date", DC_NS).apply {
            appendChild(DateTimeFormatter.ISO_LOCAL_DATE.format(nowDateTime))
        })
        appendChild(createMetaProperty("dcterms:modified", DateTimeFormatter.ISO_INSTANT.format(nowDateTime.toInstant(ZoneOffset.UTC))))
        appendChild(createMetaProperty("a11y:tactileGraphics", "false"))
        appendChild(Element("dc:identifier", DC_NS).apply {
            addAttribute(Attribute("id", "bookid"))
            appendChild("urn:uuid:${Uuid.generateV4().toHexDashString()}")
        })
        appendChild(Element("dc:title", DC_NS).apply {
            appendChild("-")
        })
        appendChild(Element("dc:creator", DC_NS).apply {
            appendChild("-")
        })
        appendChild(createMetaProperty("a11y:producer", "-"))
        appendChild(Element("dc:language", DC_NS).apply {
            appendChild("en-Brai")
        })
        appendChild(createMetaProperty("a11y:brailleSystem", "UEB"))
        appendChild(createMetaProperty("a11y:cellType", "6"))
        appendChild(createMetaProperty("a11y:completeTranscription", "true"))
        val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val defaultInstant = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC).format(dtf)
        appendChild(createMetaProperty("dcterms:dateCopyrighted", defaultInstant))
        appendChild(createMetaProperty("a11y:dateTranscribed", defaultInstant))
    })
    appendChild(Element("manifest", OPF_NS).apply {
        for ((id, item) in itemMap) {
            appendChild(Element("item", OPF_NS).apply {
                addAttribute(Attribute("id", id))
                addAttribute(Attribute("href", item.path))
                addAttribute(Attribute("media-type", item.mediaType))
                item.properties?.let { addAttribute(Attribute("properties", it)) }
            })
        }
    })
    appendChild(Element("spine", OPF_NS).apply {
        for (id in itemMap.filterValues { it.includeInSpine }.keys) {
            appendChild(Element("itemref", OPF_NS).apply {
                addAttribute(Attribute("idref", id))
            })
        }
    })
})

interface PackageItem {
    val path: String
    val mediaType: String
    val includeInSpine: Boolean
    val properties: String?
    fun write(output: OutputStream)
}

data class XHtmlItem(override val path: String, val document: org.jsoup.nodes.Document, override val includeInSpine: Boolean = true,
                     override val properties: String? = null) : PackageItem {
    override val mediaType: String = "application/xhtml+xml"
    override fun write(output: OutputStream) {
        output.bufferedWriter(Charsets.UTF_8).also {
            document.outputSettings(document.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml)).html(it)
        }.flush()
    }
}

data class ResourceItem(override val path: String, val resourceUrl: URL, override val mediaType: String,
                        override val includeInSpine: Boolean = false, override val properties: String? = null) : PackageItem {
    override fun write(output: OutputStream) {
        resourceUrl.openStream().use { it.copyTo(output) }
    }
}

private fun createMetaProperty(name: String, value: String): Element = Element("meta", OPF_NS).apply {
    addAttribute(Attribute("property", name))
    appendChild(value)
}
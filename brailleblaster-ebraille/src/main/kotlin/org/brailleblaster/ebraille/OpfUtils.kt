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
import org.brailleblaster.archiver2.OpfMetadata
import org.brailleblaster.utils.xml.DC_NS
import org.brailleblaster.utils.xml.OPF_NS
import java.io.OutputStream
import java.net.URL

fun createOpf(items: List<PackageItem>, sourceMetadata: OpfMetadata, manifest: EBrailleManifest): Document = Document(Element("package", OPF_NS).apply {
    val itemMap = items.mapIndexed { i, item -> "file${i}" to item }.toMap()
    addNamespaceDeclaration("dc", DC_NS)
    addAttribute(Attribute("version", "3.0"))
    addAttribute(Attribute("unique-identifier", "bookid"))
    appendChild(createMetadataElement(sourceMetadata, manifest))
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

private fun createMetadataElement(sourceMetadata: OpfMetadata, manifest: EBrailleManifest): Element = Element("metadata", OPF_NS).apply {
    appendChild(createDcElement("title", sourceMetadata.title))
    sourceMetadata.creators.forEach { appendChild(createDcElement("creator", it)) }
    appendChild(createDcElement("format", manifest.format))
    appendChild(createIdentifierElement(sourceMetadata.identifier))
    manifest.languages.forEach { appendChild(createDcElement("language", it)) }
    appendChild(createDcElement("date", sourceMetadata.date))
    appendChild(createMetaProperty("dcterms:modified", manifest.modified))
    appendChild(createMetaProperty("dcterms:dateCopyrighted", manifest.dateCopyrighted))
    appendChild(createMetaProperty("a11y:brailleCellType", manifest.brailleCellType))
    appendChild(createMetaProperty("a11y:brailleSystem", manifest.brailleSystem))
    appendChild(createMetaProperty("a11y:completeTranscription", manifest.completeTranscription))
    manifest.producers.forEach { appendChild(createMetaProperty("a11y:producer", it)) }
    appendChild(createMetaProperty("a11y:tactileGraphics", manifest.tactileGraphics))
}

private fun createDcElement(localName: String, value: String): Element = Element("dc:$localName", DC_NS).apply {
    appendChild(value)
}

private fun createIdentifierElement(value: String): Element = Element("dc:identifier", DC_NS).apply {
    addAttribute(Attribute("id", "bookid"))
    appendChild(value)
}

private fun createMetaProperty(name: String, value: String): Element = Element("meta", OPF_NS).apply {
    addAttribute(Attribute("property", name))
    appendChild(value)
}

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
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
package org.brailleblaster.archiver2

import nu.xom.Document
import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.utd.config.DocumentUTDConfig
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utils.xml.DC_NS
import org.brailleblaster.utils.xml.OPF_NS
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

class RequiredList private constructor(private val values: List<String>) : AbstractList<String>() {
    override val size: Int
        get() = values.size

    override fun get(index: Int): String = values[index]

    companion object {
        fun of(values: List<String>, defaultValue: String = "-"): RequiredList =
            RequiredList(values.filter { it.isNotBlank() }.ifEmpty { listOf(defaultValue) })
    }
}

/**
 * Required bibliographic metadata for a book, read from its original source markup - a NIMAS/EPUB
 * OPF package's `dc-metadata`, or a NIMAS dtbook `<head>` - before BBX conversion, so it can be
 * reused later (e.g. by the eBraille exporter) without re-parsing the source.
 *
 * Every field is required, so any field the source doesn't provide is filled with a default
 * constant (see [defaults]) rather than left blank or absent - a BBX document always has a
 * complete, well-formed record of this metadata after import, regardless of what the source
 * format provided.
 *
 * Persisted into the BBX `<head>` as the same OPF-style `<metadata>` block (Dublin Core elements)
 * that a source OPF or an eBraille package uses, so [fromOpf] can read both without a separate
 * BBX-only parser.
 */
data class OpfMetadata(
    val title: String,
    val creators: RequiredList,
    val identifier: String,
    val date: String,
    val modified: String,
    val dateCopyrighted: String,
    val producers: RequiredList
) {
    constructor(
        title: String,
        creators: List<String>,
        identifier: String,
        date: String,
        modified: String,
        dateCopyrighted: String,
        producers: List<String>
    ) : this(
        title = title,
        creators = RequiredList.of(creators),
        identifier = identifier,
        date = date,
        modified = modified,
        dateCopyrighted = dateCopyrighted,
        producers = RequiredList.of(producers, DEFAULT_PRODUCER)
    )

    fun saveTo(doc: Document) {
        val headElem = DocumentUTDConfig.NIMAS.getOrCreateHeadElement(doc)
        for (existing in headElem.getChildElements(METADATA_ELEMENT, OPF_NS)) {
            existing.detach()
        }
        headElem.appendChild(Element(METADATA_ELEMENT, OPF_NS).apply {
            metadataToXom(this@OpfMetadata).forEach { appendChild(it) }
        })
    }

    companion object {
        private const val METADATA_ELEMENT = "metadata"
        private const val DEFAULT_TITLE = "-"
        private const val DEFAULT_CREATOR = "-"
        private const val DEFAULT_PRODUCER = "-"
        private val DATE_COPYRIGHTED_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        /** The default constants used to fill in any field the source doesn't provide. */
        fun defaults(clock: Clock = Clock.systemUTC(), uuidProvider: () -> String = { UUID.randomUUID().toString() }): OpfMetadata =
            OpfMetadata(
                title = DEFAULT_TITLE,
                creators = RequiredList.of(emptyList(), DEFAULT_CREATOR),
                identifier = "urn:uuid:${uuidProvider()}",
                date = LocalDate.now(clock).format(DateTimeFormatter.ISO_LOCAL_DATE),
                modified = DateTimeFormatter.ISO_INSTANT.format(Instant.now(clock).truncatedTo(ChronoUnit.SECONDS)),
                dateCopyrighted = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC).format(DATE_COPYRIGHTED_FORMATTER),
                producers = RequiredList.of(emptyList(), DEFAULT_PRODUCER)
            )

        fun load(doc: Document, clock: Clock = Clock.systemUTC(), uuidProvider: () -> String = { UUID.randomUUID().toString() }): OpfMetadata {
            val metadataElem = DocumentUTDConfig.NIMAS.getHeadElement(doc)
                ?.getFirstChildElement(METADATA_ELEMENT, OPF_NS)
                ?: return defaults(clock, uuidProvider)
            return xomToMetadata(metadataElem.childElements.asIterable(), clock, uuidProvider)
        }

        /** Extracts dc:title/creator/identifier/date from an OPF-style metadata block - shared by NIMAS/EPUB source OPFs and the BBX head. */
        fun fromOpf(
            opfSource: Node,
            clock: Clock = Clock.systemUTC(),
            uuidProvider: () -> String = { UUID.randomUUID().toString() }
        ): OpfMetadata {
            val defaults = defaults(clock, uuidProvider)
            return OpfMetadata(
                title = OPFUtils.getDCElementValuesCaseInsensitive(opfSource, "title").firstOrNull { it.isNotBlank() } ?: defaults.title,
                creators = RequiredList.of(OPFUtils.getDCElementValuesCaseInsensitive(opfSource, "creator"), DEFAULT_CREATOR),
                identifier = OPFUtils.getDCElementValuesCaseInsensitive(opfSource, "identifier").firstOrNull { it.isNotBlank() } ?: defaults.identifier,
                date = OPFUtils.getDCElementValuesCaseInsensitive(opfSource, "date").firstOrNull { it.isNotBlank() } ?: defaults.date,
                modified = defaults.modified,
                dateCopyrighted = defaults.dateCopyrighted,
                producers = defaults.producers
            )
        }

        /** Extracts dc:Title/Creator/Identifier/Date from a NIMAS dtbook's `<head><meta name="dc:X" content="Y"/></head>` block. */
        fun fromDtbookHead(
            dtbookDocument: Document,
            clock: Clock = Clock.systemUTC(),
            uuidProvider: () -> String = { UUID.randomUUID().toString() }
        ): OpfMetadata {
            fun metaValues(name: String): List<String> = FastXPath.descendant(dtbookDocument)
                .filterIsInstance<Element>()
                .filter { it.localName == "meta" && (it.parent as? Element)?.localName == "head" }
                .filter { it.getAttributeValue("name")?.equals(name, ignoreCase = true) == true }
                .mapNotNull { it.getAttributeValue("content") }
                .filter { it.isNotBlank() }
                .toList()

            val defaults = defaults(clock, uuidProvider)
            return OpfMetadata(
                title = metaValues("dc:Title").firstOrNull() ?: defaults.title,
                creators = RequiredList.of(metaValues("dc:Creator"), DEFAULT_CREATOR),
                identifier = metaValues("dc:Identifier").firstOrNull() ?: defaults.identifier,
                date = metaValues("dc:Date").firstOrNull() ?: defaults.date,
                modified = defaults.modified,
                dateCopyrighted = defaults.dateCopyrighted,
                producers = defaults.producers
            )
        }
    }
}

/**
 * Converts [metadata] into the flat sequence of OPF-style `dc:*`/`meta` child elements it is made
 * of - callers append these as children of whichever `<metadata>` container element their format
 * uses (the BBX head's `opf:metadata`, an eBraille package's `metadata`, etc).
 */
fun metadataToXom(metadata: OpfMetadata): Iterable<Element> = buildList {
    add(dcElement("title", metadata.title))
    metadata.creators.forEach { add(dcElement("creator", it)) }
    add(dcElement("identifier", metadata.identifier))
    add(dcElement("date", metadata.date))
    add(metaPropertyElement("dcterms:modified", metadata.modified))
    add(metaPropertyElement("dcterms:dateCopyrighted", metadata.dateCopyrighted))
    metadata.producers.forEach { add(metaPropertyElement("a11y:producer", it)) }
}

/**
 * Parses [elements] - the child elements of a `<metadata>` container previously produced by
 * [metadataToXom] - back into an [OpfMetadata], falling back to [OpfMetadata.defaults] field by
 * field for anything not present (e.g. a `.bbx` file saved before a field existed).
 */
fun xomToMetadata(
    elements: Iterable<Element>,
    clock: Clock = Clock.systemUTC(),
    uuidProvider: () -> String = { UUID.randomUUID().toString() }
): OpfMetadata {
    val defaults = OpfMetadata.defaults(clock, uuidProvider)
    fun dcValues(localName: String): List<String> = elements
        .filter { it.namespacePrefix == "dc" && it.localName.equals(localName, ignoreCase = true) }
        .map { it.value }
    fun metaValues(property: String): List<String> = elements
        .filter { it.localName == "meta" && it.getAttributeValue("property") == property }
        .map { it.value }

    return OpfMetadata(
        title = dcValues("title").firstOrNull { it.isNotBlank() } ?: defaults.title,
        creators = RequiredList.of(dcValues("creator"), "-"),
        identifier = dcValues("identifier").firstOrNull { it.isNotBlank() } ?: defaults.identifier,
        date = dcValues("date").firstOrNull { it.isNotBlank() } ?: defaults.date,
        modified = metaValues("dcterms:modified").firstOrNull { it.isNotBlank() } ?: defaults.modified,
        dateCopyrighted = metaValues("dcterms:dateCopyrighted").firstOrNull { it.isNotBlank() } ?: defaults.dateCopyrighted,
        producers = RequiredList.of(metaValues("a11y:producer"), "-")
    )
}

private fun dcElement(localName: String, value: String): Element = Element("dc:$localName", DC_NS).apply {
    appendChild(value)
}

private fun metaPropertyElement(property: String, value: String): Element = Element("meta", OPF_NS).apply {
    addAttribute(nu.xom.Attribute("property", property))
    appendChild(value)
}

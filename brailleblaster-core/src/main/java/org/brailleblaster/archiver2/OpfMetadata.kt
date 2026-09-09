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

/** The XML namespace of the reserved `xml:` prefix (e.g. for `xml:lang`), predeclared per the XML spec. */
private const val XML_NS = "http://www.w3.org/XML/1998/namespace"

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
 * One `dc:title` element's value together with the attributes/refinements EPUB and eBraille use to
 * distinguish repeated titles: a stable [id] for `refines` targeting, an [lang] (`xml:lang`) for a
 * title in a different language, and a [titleType] (the EPUB `title-type` property, e.g. "main",
 * "subtitle") read from a `meta refines="#id" property="title-type"` element.
 */
data class TitleEntry(
    val value: String,
    val id: String? = null,
    val lang: String? = null,
    val titleType: String? = null
)

/** An ordered, non-empty list of [TitleEntry] - like [RequiredList] but for repeated `dc:title` elements. */
class TitleList private constructor(private val values: List<TitleEntry>) : AbstractList<TitleEntry>() {
    override val size: Int
        get() = values.size

    override fun get(index: Int): TitleEntry = values[index]

    companion object {
        fun of(values: List<TitleEntry>, default: TitleEntry = TitleEntry("-")): TitleList =
            TitleList(values.filter { it.value.isNotBlank() }.ifEmpty { listOf(default) })
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
    val titles: TitleList,
    val creators: RequiredList,
    val identifiers: RequiredList,
    val date: String,
    val modified: String,
    val dateCopyrighted: String,
    val producers: RequiredList
) {
    /** Backward-compatible accessor: the primary title, i.e. the first `dc:title` in document order. */
    val title: String get() = titles.first().value

    /** Backward-compatible accessor: the primary identifier, i.e. the one a `unique-identifier` attribute targets. */
    val identifier: String get() = identifiers.first()

    constructor(
        title: String,
        creators: List<String>,
        identifier: String,
        date: String,
        modified: String,
        dateCopyrighted: String,
        producers: List<String>
    ) : this(
        titles = TitleList.of(listOf(TitleEntry(title))),
        creators = RequiredList.of(creators),
        identifiers = RequiredList.of(listOf(identifier)),
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
                titles = TitleList.of(emptyList(), TitleEntry(DEFAULT_TITLE)),
                creators = RequiredList.of(emptyList(), DEFAULT_CREATOR),
                identifiers = RequiredList.of(emptyList(), "urn:uuid:${uuidProvider()}"),
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

        /**
         * Extracts dc:title/creator/identifier/date from an OPF-style metadata block - shared by
         * NIMAS/EPUB source OPFs and the BBX head. `dc:title` and `dc:identifier` are repeatable
         * per the EPUB/eBraille specifications, so every occurrence found (in document order) is
         * retained, not just the first.
         */
        fun fromOpf(
            opfSource: Node,
            clock: Clock = Clock.systemUTC(),
            uuidProvider: () -> String = { UUID.randomUUID().toString() }
        ): OpfMetadata {
            val defaults = defaults(clock, uuidProvider)
            return OpfMetadata(
                titles = TitleList.of(titleEntriesFromOpf(opfSource), defaults.titles.first()),
                creators = RequiredList.of(OPFUtils.getDCElementValuesCaseInsensitive(opfSource, "creator"), DEFAULT_CREATOR),
                identifiers = RequiredList.of(OPFUtils.getDCElementValuesCaseInsensitive(opfSource, "identifier"), defaults.identifier),
                date = OPFUtils.getDCElementValuesCaseInsensitive(opfSource, "date").firstOrNull { it.isNotBlank() } ?: defaults.date,
                modified = defaults.modified,
                dateCopyrighted = defaults.dateCopyrighted,
                producers = defaults.producers
            )
        }

        /**
         * Extracts dc:Title/Creator/Identifier/Date from a NIMAS dtbook's `<head><meta name="dc:X"
         * content="Y"/></head>` block. Every repeatable field keeps all of its values, in document
         * order; dtbook `<meta>` has no `id`/`xml:lang`/`refines` mechanism, so titles read from it
         * never carry those refinements (consistent with the source format's own capabilities).
         */
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
                titles = TitleList.of(metaValues("dc:Title").map { TitleEntry(it) }, defaults.titles.first()),
                creators = RequiredList.of(metaValues("dc:Creator"), DEFAULT_CREATOR),
                identifiers = RequiredList.of(metaValues("dc:Identifier"), defaults.identifier),
                date = metaValues("dc:Date").firstOrNull() ?: defaults.date,
                modified = defaults.modified,
                dateCopyrighted = defaults.dateCopyrighted,
                producers = defaults.producers
            )
        }

        /** Reads every `dc:title` in [opfSource], in document order, with its `id`, `xml:lang`, and `title-type` refinement (if any). */
        private fun titleEntriesFromOpf(opfSource: Node): List<TitleEntry> =
            OPFUtils.getDCElementsCaseInsensitive(opfSource, "title")
                .filter { it.value.isNotBlank() }
                .map { elem ->
                    val id = elem.getAttributeValue("id")
                    val lang = elem.getAttributeValue("lang", XML_NS)
                    val titleType = id?.let { OPFUtils.getMetaRefinesProperty(opfSource, it, "title-type") }
                    TitleEntry(value = elem.value, id = id, lang = lang, titleType = titleType)
                }
    }
}

/**
 * Converts [metadata] into the flat sequence of OPF-style `dc:*`/`meta` child elements it is made
 * of - callers append these as children of whichever `<metadata>` container element their format
 * uses (the BBX head's `opf:metadata`, an eBraille package's `metadata`, etc).
 */
fun metadataToXom(metadata: OpfMetadata): Iterable<Element> = buildList {
    metadata.titles.forEachIndexed { index, entry ->
        // A title-type refinement must refer to its dc:title by id, so synthesize a stable one
        // when the source didn't already provide one.
        val id = entry.id ?: entry.titleType?.let { "title-$index" }
        add(dcElement("title", entry.value, id = id, lang = entry.lang))
        if (entry.titleType != null) {
            add(metaPropertyElement("title-type", entry.titleType, refines = id))
        }
    }
    metadata.creators.forEach { add(dcElement("creator", it)) }
    metadata.identifiers.forEach { add(dcElement("identifier", it)) }
    add(dcElement("date", metadata.date))
    add(metaPropertyElement("dcterms:modified", metadata.modified))
    add(metaPropertyElement("dcterms:dateCopyrighted", metadata.dateCopyrighted))
    metadata.producers.forEach { add(metaPropertyElement("a11y:producer", it)) }
}

/**
 * Parses [elements] - the child elements of a `<metadata>` container previously produced by
 * [metadataToXom] - back into an [OpfMetadata], falling back to [OpfMetadata.defaults] field by
 * field for anything not present (e.g. a `.bbx` file saved before a field existed). Every `dc:title`
 * and `dc:identifier` element present is retained, in document order, since both are repeatable per
 * the EPUB/eBraille specifications.
 */
fun xomToMetadata(
    elements: Iterable<Element>,
    clock: Clock = Clock.systemUTC(),
    uuidProvider: () -> String = { UUID.randomUUID().toString() }
): OpfMetadata {
    val defaults = OpfMetadata.defaults(clock, uuidProvider)
    val elementList = elements.toList()
    fun dcElements(localName: String): List<Element> = elementList
        .filter { it.namespacePrefix == "dc" && it.localName.equals(localName, ignoreCase = true) }
    fun dcValues(localName: String): List<String> = dcElements(localName).map { it.value }
    fun metaValues(property: String): List<String> = elementList
        .filter { it.localName == "meta" && it.getAttributeValue("property") == property }
        .map { it.value }
    fun metaRefinesValue(refId: String, property: String): String? = elementList
        .firstOrNull { it.localName == "meta" && it.getAttributeValue("refines") == "#$refId" && it.getAttributeValue("property") == property }
        ?.value

    val titleEntries = dcElements("title")
        .filter { it.value.isNotBlank() }
        .map { elem ->
            val id = elem.getAttributeValue("id")
            val lang = elem.getAttributeValue("lang", XML_NS)
            val titleType = id?.let { metaRefinesValue(it, "title-type") }
            TitleEntry(value = elem.value, id = id, lang = lang, titleType = titleType)
        }

    return OpfMetadata(
        titles = TitleList.of(titleEntries, defaults.titles.first()),
        creators = RequiredList.of(dcValues("creator"), "-"),
        identifiers = RequiredList.of(dcValues("identifier"), defaults.identifier),
        date = dcValues("date").firstOrNull { it.isNotBlank() } ?: defaults.date,
        modified = metaValues("dcterms:modified").firstOrNull { it.isNotBlank() } ?: defaults.modified,
        dateCopyrighted = metaValues("dcterms:dateCopyrighted").firstOrNull { it.isNotBlank() } ?: defaults.dateCopyrighted,
        producers = RequiredList.of(metaValues("a11y:producer"), "-")
    )
}

private fun dcElement(localName: String, value: String, id: String? = null, lang: String? = null): Element = Element("dc:$localName", DC_NS).apply {
    id?.let { addAttribute(nu.xom.Attribute("id", it)) }
    lang?.let { addAttribute(nu.xom.Attribute("xml:lang", XML_NS, it)) }
    appendChild(value)
}

private fun metaPropertyElement(property: String, value: String, refines: String? = null): Element = Element("meta", OPF_NS).apply {
    addAttribute(nu.xom.Attribute("property", property))
    refines?.let { addAttribute(nu.xom.Attribute("refines", "#$it")) }
    appendChild(value)
}

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
import org.brailleblaster.archiver2.ImportedSourceMetadata
import org.brailleblaster.utd.ITranslationEngine
import org.brailleblaster.utd.config.DocumentUTDConfig
import org.brailleblaster.utils.xml.DC_NS
import org.brailleblaster.utils.xml.OPF_NS
import org.jsoup.nodes.Document as JsoupDocument
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * The required eBraille manifest metadata - everything that ends up in `package.opf`. Every field
 * is required by the eBraille spec, so [defaults] fills each one with a sensible default constant
 * rather than leaving it null or blank.
 */
data class EBrailleManifest(
    val title: String,
    val creators: List<String>,
    val format: String,
    val identifier: String,
    val languages: List<String>,
    val date: String,
    val modified: String,
    val dateCopyrighted: String,
    val brailleCellType: String,
    val brailleSystem: String,
    val completeTranscription: String,
    val producers: List<String>,
    val tactileGraphics: String
) {
    fun validate(): List<String> = emptyList()

    fun withDefaults(): EBrailleManifest = normalize(defaults())

    fun toMetadataElement(): Element = normalize(defaults()).let { normalized ->
        Element("metadata", OPF_NS).apply {
            manifestFields.forEach { field -> field.write(this, normalized) }
        }
    }

    companion object {
        private val UTC_EPOCH_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        fun defaults(clock: Clock = Clock.systemUTC(), uuidProvider: () -> String = { UUID.randomUUID().toString() }): EBrailleManifest {
            val now: Instant = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS)
            return EBrailleManifest(
                title = "-",
                creators = listOf("-"),
                format = "eBraille 1.0",
                identifier = EBrailleManifestDefaults.identifier(null, uuidProvider),
                languages = listOf("en-Brai"),
                date = LocalDate.now(clock).format(DateTimeFormatter.ISO_LOCAL_DATE),
                modified = DateTimeFormatter.ISO_INSTANT.format(now),
                dateCopyrighted = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC).format(UTC_EPOCH_FORMATTER),
                brailleCellType = "6",
                brailleSystem = "UEB",
                completeTranscription = "true",
                producers = listOf("-"),
                tactileGraphics = "none"
            )
        }
    }
}

/**
 * Default/derivation calculators for required eBraille manifest metadata that can be computed
 * from the document, the export package, or ambient state, rather than authored by a user.
 */
object EBrailleManifestDefaults {
    private const val BRAILLE_PATTERN_BASE = 0x2800
    private const val DOT_7_8_MASK = 0xC0
    private val TABLE_LANGUAGE_PREFIX = Regex("^([a-z]{2,3})-")

    /** Derives cell type ("6", "8", or "6 8" for mixed content) from Unicode braille patterns actually present in the exported documents. */
    fun brailleCellType(docs: List<JsoupDocument>): String {
        val dotCounts = docs.asSequence()
            .flatMap { it.text().asSequence() }
            .mapNotNull { ch ->
                val bits = ch.code - BRAILLE_PATTERN_BASE
                when {
                    bits !in 0..0xFF -> null
                    bits and DOT_7_8_MASK != 0 -> "8"
                    else -> "6"
                }
            }
            .toSortedSet()

        return if (dotCounts.isEmpty()) "6" else dotCounts.joinToString(" ")
    }

    /** Derives the tactile-graphics format list from the image resources actually bundled in the export package, most-frequent first. */
    fun tactileGraphics(packageItems: List<PackageItem>): String {
        val formats = packageItems.asSequence()
            .map { it.mediaType.substringBefore(';').trim().lowercase() }
            .filter { it.startsWith("image/") }
            .map { it.substringAfter('/') }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .map { it.key }

        return if (formats.isEmpty()) "none" else formats.joinToString(" ")
    }

    /** The current UTC time, truncated to seconds, for the volatile dcterms:modified field. */
    fun modified(clock: Clock = Clock.systemUTC()): String {
        val now = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS)
        return DateTimeFormatter.ISO_INSTANT.format(now)
    }

    /** Keeps an existing identifier if present, otherwise generates a new UUID-based one. */
    fun identifier(existing: String?, uuidProvider: () -> String = { UUID.randomUUID().toString() }): String =
        existing?.takeIf { it.isNotBlank() } ?: "urn:uuid:${uuidProvider()}"

    /** Derives a language tag from the engine's configured literary translation table (e.g. "en-ueb-g2.ctb" -> "en-Brai"), falling back to "en-Brai". */
    fun language(engine: ITranslationEngine?): String {
        val languageCode = engine?.brailleSettings?.mainTranslationTable
            ?.let { TABLE_LANGUAGE_PREFIX.find(it.lowercase())?.groupValues?.get(1) }

        return if (languageCode != null) "$languageCode-Brai" else "en-Brai"
    }
}

internal object EBrailleManifestDocumentStore {
    private const val ROOT_KEY = "ebrailleManifest"
    private const val VERSION_KEY = "$ROOT_KEY.version"

    fun load(doc: Document, clock: Clock = Clock.systemUTC()): EBrailleManifest? {
        if (!hasAnyPersistedManifestData(doc)) {
            return null
        }

        val defaults = EBrailleManifest.defaults(clock)
        return defaults.copy(
            title = readSingleValue(doc, "title", defaults.title),
            creators = readListValues(doc, "creators", defaults.creators),
            identifier = readSingleValue(doc, "identifier", defaults.identifier),
            languages = readListValues(doc, "languages", defaults.languages),
            date = readSingleValue(doc, "date", defaults.date),
            dateCopyrighted = readSingleValue(doc, "dateCopyrighted", defaults.dateCopyrighted),
            brailleCellType = readSingleValue(doc, "brailleCellType", defaults.brailleCellType),
            brailleSystem = readSingleValue(doc, "brailleSystem", defaults.brailleSystem),
            completeTranscription = readSingleValue(doc, "completeTranscription", defaults.completeTranscription),
            producers = readListValues(doc, "producers", defaults.producers)
        )
    }

    fun save(doc: Document, manifest: EBrailleManifest) {
        writeSingleValue(doc, "title", manifest.title)
        writeListValues(doc, "creators", manifest.creators)
        writeSingleValue(doc, "identifier", manifest.identifier)
        writeListValues(doc, "languages", manifest.languages)
        writeSingleValue(doc, "date", manifest.date)
        writeSingleValue(doc, "dateCopyrighted", manifest.dateCopyrighted)
        writeSingleValue(doc, "brailleCellType", manifest.brailleCellType)
        writeSingleValue(doc, "brailleSystem", manifest.brailleSystem)
        writeSingleValue(doc, "completeTranscription", manifest.completeTranscription)
        writeListValues(doc, "producers", manifest.producers)
        DocumentUTDConfig.NIMAS.setSetting(doc, VERSION_KEY, "1")
    }

    private fun hasAnyPersistedManifestData(doc: Document): Boolean {
        val directKeys = listOf(
            VERSION_KEY,
            key("title"),
            key("identifier"),
            key("date"),
            key("dateCopyrighted"),
            key("brailleCellType"),
            key("brailleSystem"),
            key("completeTranscription"),
            countKey("creators"),
            countKey("languages"),
            countKey("producers")
        )
        return directKeys.any { DocumentUTDConfig.NIMAS.getSetting(doc, it) != null }
    }

    private fun writeSingleValue(doc: Document, field: String, value: String) {
        DocumentUTDConfig.NIMAS.setSetting(doc, key(field), value)
    }

    private fun writeListValues(doc: Document, field: String, values: List<String>) {
        DocumentUTDConfig.NIMAS.setSetting(doc, countKey(field), values.size.toString())
        values.forEachIndexed { i, value ->
            writeSingleValue(doc, "$field.$i", value)
        }
    }

    private fun readSingleValue(doc: Document, field: String, fallback: String): String =
        DocumentUTDConfig.NIMAS.getSetting(doc, key(field)) ?: fallback

    private fun readListValues(doc: Document, field: String, fallback: List<String>): List<String> {
        val count = DocumentUTDConfig.NIMAS.getSetting(doc, countKey(field))?.toIntOrNull() ?: return fallback
        val values = (0 until count)
            .map { i -> readSingleValue(doc, "$field.$i", "") }
            .filter { it.isNotBlank() }
        return if (values.isEmpty()) fallback else values
    }

    private fun key(field: String): String = "$ROOT_KEY.$field.value"
    private fun countKey(field: String): String = "$ROOT_KEY.$field.count"
}

/**
 * Overlays the book's required bibliographic metadata (see [ImportedSourceMetadata], captured
 * from the original NIMAS/EPUB source at import time and persisted in the BBX head) onto a
 * manifest. [ImportedSourceMetadata] is always fully populated - defaulted where the source had
 * nothing usable - so it's always the authoritative value for these fields, including keeping the
 * book's identifier stable across repeated exports.
 */
internal fun EBrailleManifest.withImportedSourceMetadata(doc: Document): EBrailleManifest {
    val imported = ImportedSourceMetadata.load(doc)

    return copy(
        title = imported.title,
        creators = imported.creators,
        identifier = imported.identifier,
        date = imported.date
    )
}

private fun EBrailleManifest.normalize(defaults: EBrailleManifest): EBrailleManifest = EBrailleManifest(
    title = title.orDefault(defaults.title),
    creators = creators.orDefault(defaults.creators),
    format = format.orDefault(defaults.format),
    identifier = identifier.orDefault(defaults.identifier),
    languages = languages.orDefault(defaults.languages),
    date = date.orDefault(defaults.date),
    modified = modified.orDefault(defaults.modified),
    dateCopyrighted = dateCopyrighted.orDefault(defaults.dateCopyrighted),
    brailleCellType = brailleCellType.orDefault(defaults.brailleCellType),
    brailleSystem = brailleSystem.orDefault(defaults.brailleSystem),
    completeTranscription = completeTranscription.orDefault(defaults.completeTranscription),
    producers = producers.orDefault(defaults.producers),
    tactileGraphics = tactileGraphics.orDefault(defaults.tactileGraphics)
)

private interface ManifestField {
    fun write(element: Element, manifest: EBrailleManifest)
}

private data class StringManifestField(
    val writeValue: Element.(String) -> Unit,
    val readValue: (EBrailleManifest) -> String
) : ManifestField {
    override fun write(element: Element, manifest: EBrailleManifest) {
        element.writeValue(readValue(manifest))
    }
}

private data class ListManifestField(
    val writeValues: Element.(List<String>) -> Unit,
    val readValues: (EBrailleManifest) -> List<String>
) : ManifestField {
    override fun write(element: Element, manifest: EBrailleManifest) {
        element.writeValues(readValues(manifest))
    }
}

private val manifestFields = listOf<ManifestField>(
    StringManifestField({ value -> appendChild(createDcElement("title", value)) }, { it.title }),
    ListManifestField({ values -> values.forEach { appendChild(createDcElement("creator", it)) } }, { it.creators }),
    StringManifestField({ value -> appendChild(createDcElement("format", value)) }, { it.format }),
    StringManifestField({ value -> appendChild(createIdentifierElement(value)) }, { it.identifier }),
    ListManifestField({ values -> values.forEach { appendChild(createDcElement("language", it)) } }, { it.languages }),
    StringManifestField({ value -> appendChild(createDcElement("date", value)) }, { it.date }),
    StringManifestField({ value -> appendChild(createMetaProperty("dcterms:modified", value)) }, { it.modified }),
    StringManifestField({ value -> appendChild(createMetaProperty("dcterms:dateCopyrighted", value)) }, { it.dateCopyrighted }),
    StringManifestField({ value -> appendChild(createMetaProperty("a11y:brailleCellType", value)) }, { it.brailleCellType }),
    StringManifestField({ value -> appendChild(createMetaProperty("a11y:brailleSystem", value)) }, { it.brailleSystem }),
    StringManifestField({ value -> appendChild(createMetaProperty("a11y:completeTranscription", value)) }, { it.completeTranscription }),
    ListManifestField({ values -> values.forEach { appendChild(createMetaProperty("a11y:producer", it)) } }, { it.producers }),
    StringManifestField({ value -> appendChild(createMetaProperty("a11y:tactileGraphics", value)) }, { it.tactileGraphics })
)

private fun String.orDefault(default: String): String = ifBlank { default }

private fun List<String>.orDefault(default: List<String>): List<String> =
    takeIf { it.isNotEmpty() && it.all { value -> value.isNotBlank() } } ?: default

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
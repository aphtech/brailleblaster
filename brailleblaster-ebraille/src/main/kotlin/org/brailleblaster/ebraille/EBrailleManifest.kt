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
import nu.xom.Element
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

enum class ManifestValueSource {
    AUTHORED,
    IMPORTED,
    DERIVED,
    DEFAULT
}

data class ManifestValue(
    val value: String,
    val source: ManifestValueSource,
    val defaulted: Boolean = false
)

data class EBrailleManifest(
    val title: ManifestValue,
    val creators: List<ManifestValue>,
    val format: ManifestValue,
    val identifier: ManifestValue,
    val languages: List<ManifestValue>,
    val date: ManifestValue,
    val modified: ManifestValue,
    val dateCopyrighted: ManifestValue,
    val brailleCellType: ManifestValue,
    val brailleSystem: ManifestValue,
    val completeTranscription: ManifestValue,
    val producers: List<ManifestValue>,
    val tactileGraphics: ManifestValue
) {
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        if (title.value.isBlank()) {
            errors.add("dc:title must not be blank")
        }
        if (format.value.isBlank()) {
            errors.add("dc:format must not be blank")
        }
        if (identifier.value.isBlank()) {
            errors.add("dc:identifier must not be blank")
        }
        if (date.value.isBlank()) {
            errors.add("dc:date must not be blank")
        }
        if (modified.value.isBlank()) {
            errors.add("dcterms:modified must not be blank")
        }
        if (dateCopyrighted.value.isBlank()) {
            errors.add("dcterms:dateCopyrighted must not be blank")
        }
        if (brailleCellType.value.isBlank()) {
            errors.add("a11y:brailleCellType must not be blank")
        }
        if (brailleSystem.value.isBlank()) {
            errors.add("a11y:brailleSystem must not be blank")
        }
        if (completeTranscription.value.isBlank()) {
            errors.add("a11y:completeTranscription must not be blank")
        }
        if (tactileGraphics.value.isBlank()) {
            errors.add("a11y:tactileGraphics must not be blank")
        }
        if (creators.isEmpty()) {
            errors.add("dc:creator must contain at least one value")
        }
        if (creators.any { it.value.isBlank() }) {
            errors.add("dc:creator values must not be blank")
        }
        if (languages.isEmpty()) {
            errors.add("dc:language must contain at least one value")
        }
        if (languages.any { it.value.isBlank() }) {
            errors.add("dc:language values must not be blank")
        }
        if (producers.isEmpty()) {
            errors.add("a11y:producer must contain at least one value")
        }
        if (producers.any { it.value.isBlank() }) {
            errors.add("a11y:producer values must not be blank")
        }
        return errors
    }

    fun toMetadataElement(): Element = Element("metadata", OPF_NS).apply {
        appendChild(createDcElement("title", title.value))
        creators.forEach { appendChild(createDcElement("creator", it.value)) }
        appendChild(createDcElement("format", format.value))
        appendChild(createIdentifierElement(identifier.value))
        languages.forEach { appendChild(createDcElement("language", it.value)) }
        appendChild(createDcElement("date", date.value))
        appendChild(createMetaProperty("dcterms:modified", modified.value))
        appendChild(createMetaProperty("dcterms:dateCopyrighted", dateCopyrighted.value))
        appendChild(createMetaProperty("a11y:brailleCellType", brailleCellType.value))
        appendChild(createMetaProperty("a11y:brailleSystem", brailleSystem.value))
        appendChild(createMetaProperty("a11y:completeTranscription", completeTranscription.value))
        producers.forEach { appendChild(createMetaProperty("a11y:producer", it.value)) }
        appendChild(createMetaProperty("a11y:tactileGraphics", tactileGraphics.value))
    }

    companion object {
        private val UTC_EPOCH_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        fun defaults(clock: Clock = Clock.systemUTC(), uuidProvider: () -> String = { UUID.randomUUID().toString() }): EBrailleManifest {
            val now: Instant = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS)
            return EBrailleManifest(
                title = defaultValue("-"),
                creators = listOf(defaultValue("-")),
                format = defaultValue("eBraille 1.0"),
                identifier = EBrailleManifestDefaults.identifier(null, uuidProvider),
                languages = listOf(defaultValue("en-Brai")),
                date = defaultValue(LocalDate.now(clock).format(DateTimeFormatter.ISO_LOCAL_DATE)),
                modified = defaultValue(DateTimeFormatter.ISO_INSTANT.format(now)),
                dateCopyrighted = defaultValue(LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC).format(UTC_EPOCH_FORMATTER)),
                brailleCellType = defaultValue("6"),
                brailleSystem = defaultValue("UEB"),
                completeTranscription = defaultValue("true"),
                producers = listOf(defaultValue("-")),
                tactileGraphics = defaultValue("none")
            )
        }

        private fun defaultValue(value: String): ManifestValue = ManifestValue(value = value, source = ManifestValueSource.DEFAULT, defaulted = true)
    }
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
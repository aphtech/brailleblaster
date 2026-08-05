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

import nu.xom.Element
import org.testng.Assert
import org.testng.annotations.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class EBrailleManifestTest {
    @Test
    fun defaultsUseExpectedValues() {
        val fixedClock = Clock.fixed(Instant.parse("2026-07-22T12:34:56Z"), ZoneOffset.UTC)
        val manifest = EBrailleManifest.defaults(fixedClock) { "11111111-1111-1111-1111-111111111111" }

        Assert.assertEquals(manifest.title, "-")
        Assert.assertEquals(manifest.format, "eBraille 1.0")
        Assert.assertEquals(manifest.date, "2026-07-22")
        Assert.assertEquals(manifest.modified, "2026-07-22T12:34:56Z")
        Assert.assertEquals(manifest.identifier, "urn:uuid:11111111-1111-1111-1111-111111111111")
        Assert.assertEquals(manifest.brailleCellType, "6")
        Assert.assertEquals(manifest.tactileGraphics, "none")
        Assert.assertEquals(manifest.languages, listOf("en-Brai"))
        Assert.assertEquals(manifest.creators, listOf("-"))
        Assert.assertEquals(manifest.producers, listOf("-"))
    }

    @Test
    fun withDefaultsAppliesFallbackValuesForBlankOrMissingFields() {
        val manifest = EBrailleManifest.defaults().copy(
            title = "",
            creators = emptyList(),
            format = "",
            identifier = "",
            languages = listOf(""),
            date = "",
            modified = "",
            dateCopyrighted = "",
            brailleCellType = "",
            brailleSystem = "",
            completeTranscription = "",
            producers = listOf(""),
            tactileGraphics = ""
        )

        val normalized = manifest.withDefaults()

        Assert.assertEquals(normalized.title, "-")
        Assert.assertEquals(normalized.creators, listOf("-"))
        Assert.assertEquals(normalized.format, "eBraille 1.0")
        Assert.assertTrue(normalized.identifier.startsWith("urn:uuid:"))
        Assert.assertEquals(normalized.languages, listOf("en-Brai"))
        Assert.assertEquals(normalized.date, java.time.LocalDate.now().toString())
        Assert.assertTrue(normalized.modified.isNotBlank())
        Assert.assertEquals(normalized.producers, listOf("-"))
        Assert.assertEquals(normalized.tactileGraphics, "none")
    }

    @Test
    fun serializationPreservesRepeatedFieldCardinality() {
        val manifest = EBrailleManifest.defaults().copy(
            creators = listOf("Creator One", "Creator Two"),
            languages = listOf("en-Brai", "fr-Brai"),
            producers = listOf("Producer A", "Producer B")
        )

        val metadata = manifest.toMetadataElement()
        Assert.assertEquals(countNamed(metadata, "dc:creator"), 2)
        Assert.assertEquals(countNamed(metadata, "dc:language"), 2)
        Assert.assertEquals(countMetaProperty(metadata, "a11y:producer"), 2)
    }

    @Test
    fun serializationUsesSpecCorrectPropertyNames() {
        val metadata = EBrailleManifest.defaults().toMetadataElement()

        Assert.assertEquals(countMetaProperty(metadata, "a11y:brailleCellType"), 1)
        Assert.assertEquals(countMetaProperty(metadata, "a11y:cellType"), 0)
    }

    @Test
    fun serializationOrderAndCardinalityAreStable() {
        val manifest = EBrailleManifest.defaults(Clock.fixed(Instant.parse("2026-07-22T00:00:00Z"), ZoneOffset.UTC)) {
            "22222222-2222-2222-2222-222222222222"
        }.copy(
            creators = listOf("Author"),
            languages = listOf("en-Brai"),
            producers = listOf("Producer")
        )

        val metadata = manifest.toMetadataElement()
        val orderedLabels = mutableListOf<String>()
        for (i in 0 until metadata.childElements.size()) {
            val child = metadata.childElements[i]
            orderedLabels.add(
                if (child.localName == "meta") {
                    "meta:${child.getAttributeValue("property")}"
                } else {
                    "dc:${child.localName}"
                }
            )
        }

        val expected = listOf(
            "dc:title",
            "dc:creator",
            "dc:format",
            "dc:identifier",
            "dc:language",
            "dc:date",
            "meta:dcterms:modified",
            "meta:dcterms:dateCopyrighted",
            "meta:a11y:brailleCellType",
            "meta:a11y:brailleSystem",
            "meta:a11y:completeTranscription",
            "meta:a11y:producer",
            "meta:a11y:tactileGraphics"
        )
        Assert.assertEquals(orderedLabels, expected)
        Assert.assertEquals(metadata.childElements.size(), expected.size)
    }

    private fun countNamed(parent: Element, qName: String): Int {
        var count = 0
        for (i in 0 until parent.childElements.size()) {
            val child = parent.childElements[i]
            if (child.qualifiedName == qName) {
                count += 1
            }
        }
        return count
    }

    private fun countMetaProperty(parent: Element, property: String): Int {
        var count = 0
        for (i in 0 until parent.childElements.size()) {
            val child = parent.childElements[i]
            if (child.localName == "meta" && child.getAttributeValue("property") == property) {
                count += 1
            }
        }
        return count
    }
}

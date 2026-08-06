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
import org.brailleblaster.archiver2.ImportedSourceMetadata
import org.jsoup.Jsoup
import org.testng.Assert
import org.testng.annotations.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.io.OutputStream

class EBrailleManifestTest {
    @Test
    fun defaultsUseExpectedValues() {
        val fixedClock = Clock.fixed(Instant.parse("2026-07-22T12:34:56Z"), ZoneOffset.UTC)
        val manifest = EBrailleManifest.defaults(clock = fixedClock)

        Assert.assertEquals(manifest.format, "eBraille 1.0")
        Assert.assertEquals(manifest.modified, "2026-07-22T12:34:56Z")
        Assert.assertEquals(manifest.brailleCellType, "6")
        Assert.assertEquals(manifest.tactileGraphics, "none")
        Assert.assertEquals(manifest.languages, listOf("en-Brai"))
        Assert.assertEquals(manifest.producers, listOf("-"))
    }

    @Test
    fun defaultsUseProvidedLanguageList() {
        val manifest = EBrailleManifest.defaults(languages = listOf("fr-Brai"))

        Assert.assertEquals(manifest.languages, listOf("fr-Brai"))
    }

    @Test
    fun volatileExportValuesAreRecomputedFromPackageContent() {
        val manifest = EBrailleManifest.defaults().copy(
            modified = "2000-01-01T00:00:00Z",
            brailleCellType = "8",
            tactileGraphics = "png"
        )
        val docs = listOf(Jsoup.parse("<html><body><p>${sixDotChar()}</p></body></html>"))
        val packageItems = listOf(fakeItem("doc.html", "application/xhtml+xml"))

        val refreshed = manifest.withVolatileExportValues(docs, packageItems, Clock.fixed(Instant.parse("2026-07-22T00:00:00Z"), ZoneOffset.UTC))

        Assert.assertEquals(refreshed.modified, "2026-07-22T00:00:00Z")
        Assert.assertEquals(refreshed.brailleCellType, "6")
        Assert.assertEquals(refreshed.tactileGraphics, "none")
    }

    @Test
    fun serializationPreservesRepeatedFieldCardinality() {
        val importedMetadata = ImportedSourceMetadata(
            title = "Title",
            creators = listOf("Creator One", "Creator Two"),
            identifier = "id",
            date = "2020-01-01"
        )
        val manifest = EBrailleManifest.defaults().copy(
            languages = listOf("en-Brai", "fr-Brai"),
            producers = listOf("Producer A", "Producer B")
        )

        val metadata = metadataElement(importedMetadata, manifest)
        Assert.assertEquals(countNamed(metadata, "dc:creator"), 2)
        Assert.assertEquals(countNamed(metadata, "dc:language"), 2)
        Assert.assertEquals(countMetaProperty(metadata, "a11y:producer"), 2)
    }

    @Test
    fun serializationUsesSpecCorrectPropertyNames() {
        val metadata = metadataElement(ImportedSourceMetadata.defaults(), EBrailleManifest.defaults())

        Assert.assertEquals(countMetaProperty(metadata, "a11y:brailleCellType"), 1)
        Assert.assertEquals(countMetaProperty(metadata, "a11y:cellType"), 0)
    }

    @Test
    fun serializationOrderAndCardinalityAreStable() {
        val importedMetadata = ImportedSourceMetadata(
            title = "Title",
            creators = listOf("Author"),
            identifier = "identifier",
            date = "2020-01-01"
        )
        val manifest = EBrailleManifest.defaults(clock = Clock.fixed(Instant.parse("2026-07-22T00:00:00Z"), ZoneOffset.UTC)).copy(
            languages = listOf("en-Brai"),
            producers = listOf("Producer")
        )

        val metadata = metadataElement(importedMetadata, manifest)
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

    private fun metadataElement(importedMetadata: ImportedSourceMetadata, manifest: EBrailleManifest): Element =
        createOpf(emptyList(), importedMetadata, manifest).rootElement.getFirstChildElement("metadata", org.brailleblaster.utils.xml.OPF_NS)

    private fun fakeItem(itemPath: String, itemMediaType: String): PackageItem = object : PackageItem {
        override val path = itemPath
        override val mediaType = itemMediaType
        override val includeInSpine = false
        override val properties: String? = null
        override fun write(output: OutputStream) {}
    }

    private fun sixDotChar(): String = 0x2803.toChar().toString()
}

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
import org.brailleblaster.archiver2.OpfMetadata
import org.brailleblaster.utils.xml.OPF_NS
import org.testng.Assert
import org.testng.annotations.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class EBrailleOpfMetadataTest {
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-07-22T00:00:00Z"), ZoneOffset.UTC)

    @Test
    fun serializationPreservesRepeatedFieldCardinality() {
        val importedMetadata = OpfMetadata(
            title = "Title",
            creators = listOf("Creator One", "Creator Two"),
            identifier = "id",
            date = "2020-01-01",
            modified = "2026-07-22T00:00:00Z",
            dateCopyrighted = "1970-01-01 00:00:00",
            producers = listOf("Producer A", "Producer B")
        )

        val metadata = metadataElement(importedMetadata, languages = listOf("en-Brai", "fr-Brai"))
        Assert.assertEquals(countNamed(metadata, "dc:creator"), 2)
        Assert.assertEquals(countNamed(metadata, "dc:language"), 2)
        Assert.assertEquals(countMetaProperty(metadata, "a11y:producer"), 2)
    }

    @Test
    fun serializationUsesSpecCorrectPropertyNames() {
        val metadata = metadataElement(OpfMetadata.defaults(fixedClock), languages = listOf("en-Brai"))

        Assert.assertEquals(countMetaProperty(metadata, "a11y:brailleCellType"), 1)
        Assert.assertEquals(countMetaProperty(metadata, "a11y:cellType"), 0)
    }

    @Test
    fun serializationOrderAndCardinalityAreStable() {
        val importedMetadata = OpfMetadata(
            title = "Title",
            creators = listOf("Author"),
            identifier = "identifier",
            date = "2020-01-01",
            modified = "2026-07-22T00:00:00Z",
            dateCopyrighted = "1970-01-01 00:00:00",
            producers = listOf("Producer")
        )

        val metadata = metadataElement(importedMetadata, languages = listOf("en-Brai"))
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

        // Core OpfMetadata-owned elements come first (shared with the BBX head's opf:metadata
        // block), followed by the eBraille-only elements.
        val expected = listOf(
            "dc:title",
            "dc:creator",
            "dc:identifier",
            "dc:date",
            "meta:dcterms:modified",
            "meta:dcterms:dateCopyrighted",
            "meta:a11y:producer",
            "dc:format",
            "dc:language",
            "meta:a11y:brailleCellType",
            "meta:a11y:brailleSystem",
            "meta:a11y:completeTranscription",
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

    private fun metadataElement(importedMetadata: OpfMetadata, languages: List<String>): Element =
        createOpf(emptyList(), importedMetadata, languages, brailleCellType = "6", tactileGraphics = "none")
            .rootElement.getFirstChildElement("metadata", OPF_NS)
}

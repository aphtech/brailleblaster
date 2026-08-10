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

import nu.xom.Builder
import nu.xom.Document
import nu.xom.Element
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.testng.Assert
import org.testng.annotations.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ImportedSourceMetadataTest {
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-07-22T00:00:00Z"), ZoneOffset.UTC)
    private val fixedUuid: () -> String = { "11111111-1111-1111-1111-111111111111" }

    // --- fromOpf: real NIMAS OPF fixture (multiple dc:Creator, one blank) ---

    @Test
    fun fromOpfReadsNimasMetadataAndDropsBlankCreator() {
        val opf = XMLHandler().load(Paths.get("src/test/resources/nimasbaseline/0132027798NIMAS-metadata.opf"))

        val result = ImportedSourceMetadata.fromOpf(opf)

        Assert.assertEquals(result.title, "United States History: Reconstruction to the Present, Kentucky")
        Assert.assertEquals(
            result.creators,
            listOf("Emma Lapsansky-Werner", "Randy Roberts", "Peter Levy", "Alan Taylor")
        )
        Assert.assertEquals(result.identifier, "0132027798NIMAS")
        Assert.assertEquals(result.date, "2007-04-23")
    }

    // --- fromOpf: real EPUB-adjacent OPF fixture (single dc:creator, lowercase elements) ---

    @Test
    fun fromOpfReadsEpubMetadata() {
        val opf = XMLHandler().load(Paths.get("src/test/resources/epubbaseline/9781593277956-metadata.opf"))

        val result = ImportedSourceMetadata.fromOpf(opf)

        Assert.assertEquals(result.title, "Invent Your Own Computer Games with Python")
        Assert.assertEquals(result.creators, listOf("Al Sweigart"))
        Assert.assertEquals(result.identifier, "9781593277956")
        Assert.assertEquals(result.date, "2017")
    }

    // --- fromOpf: source has no creators ---

    @Test
    fun fromOpfDefaultsCreatorsWhenSourceHasNone() {
        val opf = XMLHandler().load(Paths.get("src/test/resources/fdr/FDR Inagural Address.opf"))

        val result = ImportedSourceMetadata.fromOpf(opf, fixedClock, fixedUuid)

        Assert.assertEquals(result.title, "FDR's Inaugural Address for use as part of the following: NIMAS v1.0 Exemplar file: Social Studies")
        Assert.assertEquals(result.creators, listOf("-"))
        Assert.assertEquals(result.date, "2007-01-10")
    }

    // --- fromDtbookHead: real NIMAS dtbook <head> fixture ---

    @Test
    fun fromDtbookHeadReadsMetaElements() {
        val dtbook = XMLHandler().load(Paths.get("src/test/resources/nimasbaseline/NIMASXMLGtDepJan2009_valid3.xml"))

        val result = ImportedSourceMetadata.fromDtbookHead(dtbook)

        Assert.assertEquals(result.title, "Valentin Haüy - the father of the education for the blind")
        Assert.assertEquals(result.creators, listOf("Beatrice Christensen Sköld"))
        Assert.assertEquals(result.identifier, "C00000")
        Assert.assertEquals(result.date, "2006-03-23")
    }

    @Test
    fun fromDtbookHeadDefaultsAllFieldsWhenHeadHasNoDcMeta() {
        val dtbook = Document(Element("dtbook"))

        val result = ImportedSourceMetadata.fromDtbookHead(dtbook, fixedClock, fixedUuid)

        Assert.assertEquals(result, ImportedSourceMetadata.defaults(fixedClock, fixedUuid))
        Assert.assertEquals(result.title, "-")
        Assert.assertEquals(result.creators, listOf("-"))
        Assert.assertEquals(result.identifier, "urn:uuid:11111111-1111-1111-1111-111111111111")
        Assert.assertEquals(result.date, "2026-07-22")
    }

    // --- save/load round trip on a BBX document ---

    @Test
    fun saveThenLoadRoundTripsThroughDocumentHead() {
        val doc = Document(Element("bbx"))
        val metadata = ImportedSourceMetadata(
            title = "Round Trip Title",
            creators = listOf("Creator One", "Creator Two"),
            identifier = "urn:isbn:123",
            date = "2020-01-01"
        )

        metadata.saveTo(doc)
        val reopened = reopen(doc)
        val loaded = ImportedSourceMetadata.load(reopened)

        Assert.assertEquals(loaded, metadata)
    }

    @Test
    fun loadReturnsDefaultsWhenNothingWasSaved() {
        val doc = Document(Element("bbx"))

        val loaded = ImportedSourceMetadata.load(doc, fixedClock, fixedUuid)

        Assert.assertEquals(loaded, ImportedSourceMetadata.defaults(fixedClock, fixedUuid))
    }

    @Test
    fun saveAlwaysWritesMetadataEvenWhenSourceHadNothingUsable() {
        val doc = Document(Element("bbx"))
        ImportedSourceMetadata.defaults(fixedClock, fixedUuid).saveTo(doc)

        val reopened = reopen(doc)
        val loaded = ImportedSourceMetadata.load(reopened, fixedClock, fixedUuid)

        Assert.assertEquals(loaded, ImportedSourceMetadata.defaults(fixedClock, fixedUuid))
    }

    @Test
    fun saveBbxAddsDefaultMetadataWhenDocumentHadNone() {
        val doc = Document(Element("bbx"))
        val tempFile = Files.createTempFile("bbx-metadata-", ".bbx")

        try {
            BBZArchiver.saveBBX(tempFile, doc)

            val reopened = XMLHandler().load(tempFile)
            val loaded = ImportedSourceMetadata.load(reopened)

            Assert.assertEquals(loaded.title, "-")
            Assert.assertEquals(loaded.creators, listOf("-"))
            Assert.assertTrue(loaded.identifier.startsWith("urn:uuid:"))
            Assert.assertTrue(Regex("""\d{4}-\d{2}-\d{2}""").matches(loaded.date))
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    @Test
    fun saveBbxPreservesExplicitCanonicalMetadata() {
        val doc = Document(Element("bbx"))
        val expected = ImportedSourceMetadata(
            title = "Canonical Title",
            creators = listOf("Creator One", "Creator Two"),
            identifier = "urn:isbn:9781234567890",
            date = "2021-06-15"
        )
        val tempFile = Files.createTempFile("bbx-metadata-preserve-", ".bbx")

        try {
            expected.saveTo(doc)

            BBZArchiver.saveBBX(tempFile, doc)

            val reopened = XMLHandler().load(tempFile)
            val loaded = ImportedSourceMetadata.load(reopened)

            Assert.assertEquals(loaded, expected)
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    @Test
    fun constructorNormalizesCreatorsToANonEmptyTypedList() {
        val blankCreators = ImportedSourceMetadata(
            title = "Title",
            creators = listOf("", "   "),
            identifier = "identifier",
            date = "2021-06-15"
        )

        Assert.assertEquals(blankCreators.creators, listOf("-"))
        Assert.assertTrue(blankCreators.creators is RequiredMetadataValues)
    }

    private fun reopen(doc: Document): Document {
        val xml = doc.toXML().toByteArray(StandardCharsets.UTF_8)
        return Builder().build(xml.inputStream(), "")
    }
}

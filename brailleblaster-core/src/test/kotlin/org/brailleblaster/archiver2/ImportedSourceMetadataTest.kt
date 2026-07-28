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
import java.nio.file.Paths

class ImportedSourceMetadataTest {

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

    // --- fromOpf: no usable metadata present ---

    @Test
    fun fromOpfReturnsEmptyPlaceholdersWhenMetadataAbsent() {
        val opf = XMLHandler().load(Paths.get("src/test/resources/fdr/FDR Inagural Address.opf"))

        val result = ImportedSourceMetadata.fromOpf(opf)

        Assert.assertEquals(result.title, "FDR's Inaugural Address for use as part of the following: NIMAS v1.0 Exemplar file: Social Studies")
        Assert.assertTrue(result.creators.isEmpty())
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
    fun fromDtbookHeadReturnsEmptyWhenHeadHasNoDcMeta() {
        val dtbook = Document(Element("dtbook"))

        val result = ImportedSourceMetadata.fromDtbookHead(dtbook)

        Assert.assertTrue(result.isEmpty)
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
    fun loadReturnsEmptyPlaceholdersWhenNothingWasSaved() {
        val doc = Document(Element("bbx"))

        val loaded = ImportedSourceMetadata.load(doc)

        Assert.assertTrue(loaded.isEmpty)
    }

    private fun reopen(doc: Document): Document {
        val xml = doc.toXML().toByteArray(StandardCharsets.UTF_8)
        return Builder().build(xml.inputStream(), "")
    }
}

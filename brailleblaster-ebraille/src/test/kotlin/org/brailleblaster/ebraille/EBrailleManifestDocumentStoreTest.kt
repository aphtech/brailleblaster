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

import nu.xom.Builder
import nu.xom.Document
import nu.xom.Element
import org.brailleblaster.utd.config.DocumentUTDConfig
import org.testng.Assert
import org.testng.annotations.Test
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class EBrailleManifestDocumentStoreTest {
    private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-07-23T00:00:00Z"), ZoneOffset.UTC)

    @Test
    fun saveOpenSaveRoundTripCreatesAndPreservesManifestWhenMissingInitially() {
        val doc = newBookDocument()
        Assert.assertNull(EBrailleManifestDocumentStore.load(doc, fixedClock))

        val toPersist = EBrailleManifest.defaults(fixedClock) { "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa" }.copy(
            title = "Round Trip Title",
            creators = listOf("Creator A", "Creator B")
        )
        EBrailleManifestDocumentStore.save(doc, toPersist)

        val reopened = reopen(doc)
        val loaded = EBrailleManifestDocumentStore.load(reopened, fixedClock)
        Assert.assertNotNull(loaded)
        Assert.assertEquals(loaded!!.title, "Round Trip Title")
        Assert.assertEquals(loaded.creators, listOf("Creator A", "Creator B"))

        // Simulate a non-eBraille workflow mutating unrelated document settings.
        DocumentUTDConfig.NIMAS.setSetting(reopened, "someOtherSetting", "42")

        val reopenedAgain = reopen(reopened)
        val loadedAgain = EBrailleManifestDocumentStore.load(reopenedAgain, fixedClock)
        Assert.assertNotNull(loadedAgain)
        Assert.assertEquals(loadedAgain!!.title, "Round Trip Title")
        Assert.assertEquals(loadedAgain.creators, listOf("Creator A", "Creator B"))
    }

    @Test
    fun loadHandlesPartialPersistedManifestByFallingBackToDefaults() {
        val doc = newBookDocument()
        DocumentUTDConfig.NIMAS.setSetting(doc, "ebrailleManifest.title.value", "Only Title")

        val loaded = EBrailleManifestDocumentStore.load(doc, fixedClock)
        Assert.assertNotNull(loaded)
        Assert.assertEquals(loaded!!.title, "Only Title")
        Assert.assertEquals(loaded.format, "eBraille 1.0")
        Assert.assertEquals(loaded.languages, listOf("en-Brai"))
        Assert.assertEquals(loaded.producers, listOf("-"))
    }

    @Test
    fun roundTripPreservesMultiValueStableFields() {
        val doc = newBookDocument()
        val manifest = EBrailleManifest.defaults(fixedClock) { "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb" }.copy(
            creators = listOf("Creator One", "Creator Two"),
            languages = listOf("en-Brai", "fr-Brai"),
            producers = listOf("Producer A", "Producer B")
        )

        EBrailleManifestDocumentStore.save(doc, manifest)
        val reopened = reopen(doc)
        val loaded = EBrailleManifestDocumentStore.load(reopened, fixedClock)

        Assert.assertNotNull(loaded)
        Assert.assertEquals(loaded!!.creators, listOf("Creator One", "Creator Two"))
        Assert.assertEquals(loaded.languages, listOf("en-Brai", "fr-Brai"))
        Assert.assertEquals(loaded.producers, listOf("Producer A", "Producer B"))
    }

    private fun newBookDocument(): Document {
        val root = Element("dtbook", "http://www.daisy.org/z3986/2005/dtbook/")
        return Document(root)
    }

    private fun reopen(doc: Document): Document {
        val xml = doc.toXML().toByteArray(StandardCharsets.UTF_8)
        return Builder().build(xml.inputStream(), "")
    }
}

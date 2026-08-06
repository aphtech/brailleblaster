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

import nu.xom.Document
import nu.xom.Element
import org.brailleblaster.archiver2.ImportedSourceMetadata
import org.testng.Assert
import org.testng.annotations.Test

class EBrailleManifestImportTest {

    @Test
    fun fillsInFieldsFromImportedSourceMetadata() {
        val doc = newBookDocument()
        ImportedSourceMetadata(
            title = "Imported Title",
            creators = listOf("Imported Author One", "Imported Author Two"),
            identifier = "urn:isbn:9781234567890",
            date = "2007-04-23"
        ).saveTo(doc)

        val result = EBrailleManifest.defaults().withImportedSourceMetadata(doc)

        Assert.assertEquals(result.title, "Imported Title")
        Assert.assertEquals(result.creators, listOf("Imported Author One", "Imported Author Two"))
        Assert.assertEquals(result.identifier, "urn:isbn:9781234567890")
        Assert.assertEquals(result.date, "2007-04-23")
        // Untouched fields are left as-is.
        Assert.assertEquals(result.format, EBrailleManifest.defaults().format)
        Assert.assertEquals(result.languages, EBrailleManifest.defaults().languages)
    }

    @Test
    fun importedSourceMetadataIsTheCanonicalSourceForSharedFields() {
        val doc = newBookDocument()
        ImportedSourceMetadata.defaults().copy(title = "Imported Title", identifier = "imported-id").saveTo(doc)

        val manifestWithDifferentTitle = EBrailleManifest.defaults().copy(title = "Some Other Title")

        val result = manifestWithDifferentTitle.withImportedSourceMetadata(doc)

        // The BBX-persisted required metadata always wins, so the same identifier/title survive
        // every export rather than drifting between calls.
        Assert.assertEquals(result.title, "Imported Title")
        Assert.assertEquals(result.identifier, "imported-id")
    }

    @Test
    fun importedSourceMetadataOverridesAllConflictingSharedManifestFields() {
        val doc = newBookDocument()
        ImportedSourceMetadata(
            title = "Imported Title",
            creators = listOf("Imported Author One", "Imported Author Two"),
            identifier = "imported-id",
            date = "2019-08-07"
        ).saveTo(doc)

        val conflictingManifest = EBrailleManifest.defaults().copy(
            title = "Manifest Title",
            creators = listOf("Manifest Creator"),
            identifier = "manifest-id",
            date = "2024-01-31"
        )

        val result = conflictingManifest.withImportedSourceMetadata(doc)

        Assert.assertEquals(result.title, "Imported Title")
        Assert.assertEquals(result.creators, listOf("Imported Author One", "Imported Author Two"))
        Assert.assertEquals(result.identifier, "imported-id")
        Assert.assertEquals(result.date, "2019-08-07")
    }

    @Test
    fun appliesDefaultedRequiredMetadataWhenNoneWasEverSaved() {
        val doc = newBookDocument()
        val manifest = EBrailleManifest.defaults()

        val result = manifest.withImportedSourceMetadata(doc)

        Assert.assertEquals(result.title, "-")
        Assert.assertEquals(result.creators, listOf("-"))
        Assert.assertTrue(result.identifier.startsWith("urn:uuid:"))
        Assert.assertTrue(Regex("""\d{4}-\d{2}-\d{2}""").matches(result.date))
    }

    @Test
    fun keepsHyphenDefaultWhenImportedTitleIsMissing() {
        val doc = newBookDocument()
        ImportedSourceMetadata.defaults().saveTo(doc)

        val result = EBrailleManifest.defaults().withImportedSourceMetadata(doc)

        Assert.assertEquals(result.title, "-")
    }

    private fun newBookDocument(): Document = Document(Element("dtbook", "http://www.daisy.org/z3986/2005/dtbook/"))
}

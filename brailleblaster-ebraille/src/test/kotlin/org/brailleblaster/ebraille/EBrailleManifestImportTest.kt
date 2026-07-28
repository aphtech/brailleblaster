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
    fun fillsInDefaultedFieldsFromImportedSourceMetadata() {
        val doc = newBookDocument()
        ImportedSourceMetadata(
            title = "Imported Title",
            creators = listOf("Imported Author One", "Imported Author Two"),
            identifier = "urn:isbn:9781234567890",
            date = "2007-04-23"
        ).saveTo(doc)

        val result = EBrailleManifest.defaults().withImportedSourceMetadata(doc)

        Assert.assertEquals(result.title.value, "Imported Title")
        Assert.assertEquals(result.title.source, ManifestValueSource.IMPORTED)
        Assert.assertEquals(result.creators.map { it.value }, listOf("Imported Author One", "Imported Author Two"))
        Assert.assertEquals(result.identifier.value, "urn:isbn:9781234567890")
        Assert.assertEquals(result.date.value, "2007-04-23")
        // Untouched fields are left as-is.
        Assert.assertEquals(result.format, EBrailleManifest.defaults().format)
        Assert.assertEquals(result.languages, EBrailleManifest.defaults().languages)
    }

    @Test
    fun authoredValuesBeatImportedSourceMetadata() {
        val doc = newBookDocument()
        ImportedSourceMetadata(title = "Imported Title", identifier = "imported-id").saveTo(doc)

        val authoredManifest = EBrailleManifest.defaults().copy(
            title = ManifestValue("User Entered Title", ManifestValueSource.AUTHORED)
        )

        val result = authoredManifest.withImportedSourceMetadata(doc)

        Assert.assertEquals(result.title.value, "User Entered Title")
        Assert.assertEquals(result.title.source, ManifestValueSource.AUTHORED)
        // Identifier had no authored value, so the imported one still applies.
        Assert.assertEquals(result.identifier.value, "imported-id")
    }

    @Test
    fun leavesManifestUnchangedWhenNoImportedSourceMetadataIsPresent() {
        val doc = newBookDocument()
        val manifest = EBrailleManifest.defaults()

        val result = manifest.withImportedSourceMetadata(doc)

        Assert.assertEquals(result, manifest)
    }

    private fun newBookDocument(): Document = Document(Element("dtbook", "http://www.daisy.org/z3986/2005/dtbook/"))
}

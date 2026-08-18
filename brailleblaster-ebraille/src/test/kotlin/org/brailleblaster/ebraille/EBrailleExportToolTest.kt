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
import org.brailleblaster.archiver2.OpfMetadata
import org.brailleblaster.utd.config.DocumentUTDConfig
import org.testng.Assert
import org.testng.annotations.Test

class EBrailleExportToolTest {
    @Test
    fun buildExportDataUsesCanonicalMetadataWithoutWritingEbrailleSettings() {
        val doc = Document(Element("bbx"))
        val expected = OpfMetadata(
            title = "Canonical Title",
            creators = listOf("Author One", "Author Two"),
            identifier = "urn:isbn:9781234567890",
            date = "2021-06-15",
            modified = "2021-06-15T00:00:00Z",
            dateCopyrighted = "1970-01-01 00:00:00",
            producers = listOf("Producer")
        )
        expected.saveTo(doc)

        val sourceMetadata = buildExportData(doc)

        Assert.assertNull(DocumentUTDConfig.NIMAS.getSetting(doc, "ebrailleManifest.version"))
        Assert.assertNull(DocumentUTDConfig.NIMAS.getSetting(doc, "ebrailleManifest.title.value"))
        Assert.assertEquals(sourceMetadata, expected)
    }
}

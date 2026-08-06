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
import org.brailleblaster.utd.BrailleSettings
import org.brailleblaster.utd.ITranslationEngine
import org.brailleblaster.utd.config.DocumentUTDConfig
import org.mwhapples.jlouis.Louis
import org.mwhapples.jlouis.TranslationResult
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.testng.Assert
import org.testng.annotations.Test

class EBrailleExportToolTest {
    @Test
    fun buildExportDataUsesCanonicalMetadataWithoutWritingEbrailleSettings() {
        val doc = Document(Element("bbx"))
        val expected = ImportedSourceMetadata(
            title = "Canonical Title",
            creators = listOf("Author One", "Author Two"),
            identifier = "urn:isbn:9781234567890",
            date = "2021-06-15"
        )
        expected.saveTo(doc)

        val exportData = buildExportData(doc, mockTranslationEngine())

        Assert.assertNull(DocumentUTDConfig.NIMAS.getSetting(doc, "ebrailleManifest.version"))
        Assert.assertNull(DocumentUTDConfig.NIMAS.getSetting(doc, "ebrailleManifest.title.value"))
        Assert.assertEquals(exportData.sourceMetadata, expected)
        Assert.assertEquals(exportData.manifest.languages, listOf("en-Brai"))
    }

    private fun mockTranslationEngine(): ITranslationEngine {
        val translationResult = Mockito.mock(TranslationResult::class.java)
        Mockito.`when`(translationResult.translation).thenReturn("x")
        Mockito.`when`(translationResult.inputPos).thenReturn(intArrayOf(0))

        val louis = Mockito.mock(Louis::class.java)
        Mockito.`when`(louis.translate(anyString(), anyString(), any(), anyInt(), anyInt())).thenReturn(translationResult)

        val settings = BrailleSettings().apply {
            isUseAsciiBraille = true
            mainTranslationTable = "en-us-g2.ctb"
        }

        val engine = Mockito.mock(ITranslationEngine::class.java)
        Mockito.`when`(engine.brailleTranslator).thenReturn(louis)
        Mockito.`when`(engine.brailleSettings).thenReturn(settings)
        return engine
    }
}
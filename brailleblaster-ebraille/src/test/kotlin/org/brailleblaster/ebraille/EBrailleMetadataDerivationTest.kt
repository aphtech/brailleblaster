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

import org.brailleblaster.utd.BrailleSettings
import org.brailleblaster.utd.ITranslationEngine
import org.jsoup.Jsoup
import org.mockito.Mockito
import org.testng.Assert
import org.testng.annotations.Test
import java.io.OutputStream
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class EBrailleMetadataDerivationTest {

    // --- brailleCellType ---

    @Test
    fun brailleCellTypeDefaultsToSixWhenNoBrailleContentPresent() {
        val docs = listOf(Jsoup.parse("<html><body><p>Plain print text</p></body></html>"))
        val result = deriveBrailleCellType(docs)
        Assert.assertEquals(result, "6")
    }

    @Test
    fun brailleCellTypeDetectsSixDotOnlyContent() {
        val docs = listOf(Jsoup.parse("<html><body><p>${sixDotChar()}</p></body></html>"))
        val result = deriveBrailleCellType(docs)
        Assert.assertEquals(result, "6")
    }

    @Test
    fun brailleCellTypeDetectsEightDotContent() {
        val docs = listOf(Jsoup.parse("<html><body><p>${eightDotChar()}</p></body></html>"))
        val result = deriveBrailleCellType(docs)
        Assert.assertEquals(result, "8")
    }

    @Test
    fun brailleCellTypeReportsMixedCellTypesAcrossDocuments() {
        val docs = listOf(
            Jsoup.parse("<html><body><p>${sixDotChar()}</p></body></html>"),
            Jsoup.parse("<html><body><p>${eightDotChar()}</p></body></html>")
        )
        val result = deriveBrailleCellType(docs)
        Assert.assertEquals(result, "6 8")
    }

    private fun sixDotChar(): String = 0x2803.toChar().toString()
    private fun eightDotChar(): String = 0x28C0.toChar().toString()

    // --- tactileGraphics ---

    @Test
    fun tactileGraphicsDefaultsToNoneWhenNoImagesPresent() {
        val result = deriveTactileGraphics(listOf(fakeItem("doc.html", "application/xhtml+xml")))
        Assert.assertEquals(result, "none")
    }

    @Test
    fun tactileGraphicsUsesSingleFormatWhenOnlyOnePresent() {
        val result = deriveTactileGraphics(listOf(fakeItem("a.png", "image/png")))
        Assert.assertEquals(result, "png")
    }

    @Test
    fun tactileGraphicsOrdersByFrequencyThenAlphabetically() {
        val items = listOf(
            fakeItem("a.png", "image/png"), fakeItem("b.png", "image/png"), fakeItem("c.png", "image/png"),
            fakeItem("d.jpeg", "image/jpeg"), fakeItem("e.jpeg", "image/jpeg"),
            fakeItem("f.gif", "image/gif"), fakeItem("g.gif", "image/gif")
        )
        val result = deriveTactileGraphics(items)
        Assert.assertEquals(result, "png gif jpeg")
    }

    private fun fakeItem(itemPath: String, itemMediaType: String): PackageItem = object : PackageItem {
        override val path = itemPath
        override val mediaType = itemMediaType
        override val includeInSpine = false
        override val properties: String? = null
        override fun write(output: OutputStream) {}
    }

    // --- modified ---

    @Test
    fun modifiedReflectsCurrentUtcTimeTruncatedToSeconds() {
        val fixedClock = Clock.fixed(Instant.parse("2026-07-27T10:15:30.123Z"), ZoneOffset.UTC)
        val result = currentModifiedTime(fixedClock)
        Assert.assertEquals(result, "2026-07-27T10:15:30Z")
    }

    // --- language ---

    @Test
    fun languageDefaultsToEnBrailWhenEngineAbsent() {
        val result = defaultEbrailleLanguage(null)
        Assert.assertEquals(result, "en-Brai")
    }

    @Test
    fun languageDerivesFromMainTranslationTablePrefix() {
        Assert.assertEquals(languageFor("en-ueb-g2.ctb"), "en-Brai")
        Assert.assertEquals(languageFor("fr-bfu-g2.ctb"), "fr-Brai")
    }

    @Test
    fun languageFallsBackWhenTableNameHasNoLanguagePrefix() {
        val result = languageFor("nemeth.ctb")
        Assert.assertEquals(result, "en-Brai")
    }

    private fun languageFor(table: String): String {
        val settings = BrailleSettings().apply { mainTranslationTable = table }
        val engine = Mockito.mock(ITranslationEngine::class.java)
        Mockito.`when`(engine.brailleSettings).thenReturn(settings)
        return defaultEbrailleLanguage(engine)
    }
}

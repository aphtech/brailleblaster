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

class EBrailleManifestDefaultsTest {

    // --- brailleCellType ---

    @Test
    fun brailleCellTypeDefaultsToSixWhenNoBrailleContentPresent() {
        val docs = listOf(Jsoup.parse("<html><body><p>Plain print text</p></body></html>"))
        val result = EBrailleManifestDefaults.brailleCellType(docs)
        Assert.assertEquals(result.value, "6")
        Assert.assertEquals(result.source, ManifestValueSource.DEFAULT)
        Assert.assertTrue(result.defaulted)
    }

    @Test
    fun brailleCellTypeDetectsSixDotOnlyContent() {
        val docs = listOf(Jsoup.parse("<html><body><p>${sixDotChar()}</p></body></html>"))
        val result = EBrailleManifestDefaults.brailleCellType(docs)
        Assert.assertEquals(result.value, "6")
        Assert.assertEquals(result.source, ManifestValueSource.DERIVED)
    }

    @Test
    fun brailleCellTypeDetectsEightDotContent() {
        val docs = listOf(Jsoup.parse("<html><body><p>${eightDotChar()}</p></body></html>"))
        val result = EBrailleManifestDefaults.brailleCellType(docs)
        Assert.assertEquals(result.value, "8")
        Assert.assertEquals(result.source, ManifestValueSource.DERIVED)
    }

    @Test
    fun brailleCellTypeReportsMixedCellTypesAcrossDocuments() {
        val docs = listOf(
            Jsoup.parse("<html><body><p>${sixDotChar()}</p></body></html>"),
            Jsoup.parse("<html><body><p>${eightDotChar()}</p></body></html>")
        )
        val result = EBrailleManifestDefaults.brailleCellType(docs)
        Assert.assertEquals(result.value, "6 8")
        Assert.assertEquals(result.source, ManifestValueSource.DERIVED)
    }

    private fun sixDotChar(): String = 0x2803.toChar().toString()
    private fun eightDotChar(): String = 0x28C0.toChar().toString()

    // --- tactileGraphics ---

    @Test
    fun tactileGraphicsDefaultsToNoneWhenNoImagesPresent() {
        val result = EBrailleManifestDefaults.tactileGraphics(listOf(fakeItem("doc.html", "application/xhtml+xml")))
        Assert.assertEquals(result.value, "none")
        Assert.assertEquals(result.source, ManifestValueSource.DEFAULT)
        Assert.assertTrue(result.defaulted)
    }

    @Test
    fun tactileGraphicsUsesSingleFormatWhenOnlyOnePresent() {
        val result = EBrailleManifestDefaults.tactileGraphics(listOf(fakeItem("a.png", "image/png")))
        Assert.assertEquals(result.value, "png")
        Assert.assertEquals(result.source, ManifestValueSource.DERIVED)
    }

    @Test
    fun tactileGraphicsOrdersByFrequencyThenAlphabetically() {
        val items = listOf(
            fakeItem("a.png", "image/png"), fakeItem("b.png", "image/png"), fakeItem("c.png", "image/png"),
            fakeItem("d.jpeg", "image/jpeg"), fakeItem("e.jpeg", "image/jpeg"),
            fakeItem("f.gif", "image/gif"), fakeItem("g.gif", "image/gif")
        )
        val result = EBrailleManifestDefaults.tactileGraphics(items)
        Assert.assertEquals(result.value, "png gif jpeg")
        Assert.assertEquals(result.source, ManifestValueSource.DERIVED)
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
        val result = EBrailleManifestDefaults.modified(fixedClock)
        Assert.assertEquals(result.value, "2026-07-27T10:15:30Z")
        Assert.assertEquals(result.source, ManifestValueSource.DERIVED)
    }

    // --- identifier ---

    @Test
    fun identifierGeneratesUuidWhenAbsent() {
        val result = EBrailleManifestDefaults.identifier(null) { "fixed-uuid" }
        Assert.assertEquals(result.value, "urn:uuid:fixed-uuid")
        Assert.assertEquals(result.source, ManifestValueSource.DEFAULT)
        Assert.assertTrue(result.defaulted)
    }

    @Test
    fun identifierGeneratesUuidWhenExistingIsBlank() {
        val result = EBrailleManifestDefaults.identifier(ManifestValue("", ManifestValueSource.AUTHORED)) { "fixed-uuid" }
        Assert.assertEquals(result.value, "urn:uuid:fixed-uuid")
    }

    @Test
    fun identifierKeepsExistingValueWhenPresent() {
        val existing = ManifestValue("urn:uuid:already-set", ManifestValueSource.AUTHORED)
        val result = EBrailleManifestDefaults.identifier(existing) { "fixed-uuid" }
        Assert.assertEquals(result, existing)
    }

    // --- language ---

    @Test
    fun languageDefaultsToEnBrailWhenEngineAbsent() {
        val result = EBrailleManifestDefaults.language(null)
        Assert.assertEquals(result.value, "en-Brai")
        Assert.assertEquals(result.source, ManifestValueSource.DEFAULT)
        Assert.assertTrue(result.defaulted)
    }

    @Test
    fun languageDerivesFromMainTranslationTablePrefix() {
        Assert.assertEquals(languageFor("en-ueb-g2.ctb").value, "en-Brai")
        val frenchResult = languageFor("fr-bfu-g2.ctb")
        Assert.assertEquals(frenchResult.value, "fr-Brai")
        Assert.assertEquals(frenchResult.source, ManifestValueSource.DERIVED)
    }

    @Test
    fun languageFallsBackWhenTableNameHasNoLanguagePrefix() {
        val result = languageFor("nemeth.ctb")
        Assert.assertEquals(result.value, "en-Brai")
        Assert.assertEquals(result.source, ManifestValueSource.DEFAULT)
    }

    private fun languageFor(table: String): ManifestValue {
        val settings = BrailleSettings().apply { mainTranslationTable = table }
        val engine = Mockito.mock(ITranslationEngine::class.java)
        Mockito.`when`(engine.brailleSettings).thenReturn(settings)
        return EBrailleManifestDefaults.language(engine)
    }

    // --- ManifestValuePrecedence.choose ---

    @Test
    fun chooseAuthoredValueBeatsImportedAndCalculated() {
        val authored = ManifestValue("Authored", ManifestValueSource.AUTHORED)
        val imported = ManifestValue("Imported", ManifestValueSource.IMPORTED)
        val derived = ManifestValue("Derived", ManifestValueSource.DERIVED)
        Assert.assertEquals(ManifestValuePrecedence.choose(authored, imported, derived), authored)
        Assert.assertEquals(ManifestValuePrecedence.choose(derived, imported, authored), authored)
    }

    @Test
    fun chooseImportedValueBeatsCalculatedWhenNoAuthoredValue() {
        val imported = ManifestValue("Imported", ManifestValueSource.IMPORTED)
        val derived = ManifestValue("Derived", ManifestValueSource.DERIVED)
        val default = ManifestValue("Default", ManifestValueSource.DEFAULT)
        Assert.assertEquals(ManifestValuePrecedence.choose(imported, derived, default), imported)
    }

    @Test
    fun chooseSkipsBlankValuesRegardlessOfSource() {
        val blankAuthored = ManifestValue("", ManifestValueSource.AUTHORED)
        val imported = ManifestValue("Imported", ManifestValueSource.IMPORTED)
        Assert.assertEquals(ManifestValuePrecedence.choose(blankAuthored, imported), imported)
    }

    @Test
    fun chooseReturnsNullWhenNoUsableCandidates() {
        Assert.assertNull(ManifestValuePrecedence.choose(null, ManifestValue("", ManifestValueSource.AUTHORED)))
    }

    // --- ManifestValuePrecedence.chooseList ---

    @Test
    fun chooseListPrefersAuthoredCreatorsOverImportedAndCalculated() {
        val authoredCreators = listOf(
            ManifestValue("Author One", ManifestValueSource.AUTHORED),
            ManifestValue("Author Two", ManifestValueSource.AUTHORED)
        )
        val importedCreators = listOf(
            ManifestValue("Imported One", ManifestValueSource.IMPORTED),
            ManifestValue("Imported Two", ManifestValueSource.IMPORTED),
            ManifestValue("Imported Three", ManifestValueSource.IMPORTED)
        )
        val calculatedCreators = listOf(ManifestValue("-", ManifestValueSource.DEFAULT))

        val result = ManifestValuePrecedence.chooseList(authoredCreators, importedCreators, calculatedCreators)
        Assert.assertEquals(result, authoredCreators)
    }

    @Test
    fun chooseListFallsBackToImportedLanguagesWhenNoAuthoredValues() {
        val importedLanguages = listOf(
            ManifestValue("en-Brai", ManifestValueSource.IMPORTED),
            ManifestValue("fr-Brai", ManifestValueSource.IMPORTED)
        )
        val calculatedLanguages = listOf(ManifestValue("en-Brai", ManifestValueSource.DEFAULT))

        val result = ManifestValuePrecedence.chooseList(emptyList(), importedLanguages, calculatedLanguages)
        Assert.assertEquals(result, importedLanguages)
    }

    @Test
    fun chooseListFallsBackToCalculatedProducersWhenNothingElseAvailable() {
        val calculatedProducers = listOf(ManifestValue("-", ManifestValueSource.DEFAULT))
        val result = ManifestValuePrecedence.chooseList(emptyList(), null, calculatedProducers)
        Assert.assertEquals(result, calculatedProducers)
    }

    @Test
    fun chooseListReturnsEmptyWhenAllCandidatesAreBlankOrAbsent() {
        val blankOnly = listOf(ManifestValue("", ManifestValueSource.AUTHORED))
        Assert.assertEquals(ManifestValuePrecedence.chooseList(blankOnly, null, emptyList()), emptyList<ManifestValue>())
    }
}

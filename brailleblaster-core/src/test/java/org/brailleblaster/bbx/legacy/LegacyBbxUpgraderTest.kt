/*
 * Copyright (C) 2025 American Printing House for the Blind
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
package org.brailleblaster.bbx.legacy

import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.legacy.LegacyTestSupport.V1X
import org.brailleblaster.bbx.legacy.LegacyTestSupport.bodyTextBlocks
import org.brailleblaster.bbx.legacy.LegacyTestSupport.files
import org.brailleblaster.bbx.legacy.LegacyTestSupport.load
import org.brailleblaster.bbx.legacy.LegacyTestSupport.missingWords
import org.brailleblaster.bbx.legacy.LegacyTestSupport.structure
import org.brailleblaster.bbx.legacy.LegacyTestSupport.toBrf
import org.brailleblaster.bbx.legacy.LegacyTestSupport.words
import org.brailleblaster.utd.internal.xml.FastXPath
import org.testng.Assert.assertEquals
import org.testng.Assert.assertFalse
import org.testng.Assert.assertNull
import org.testng.Assert.assertTrue
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.nio.file.Path

class LegacyBbxUpgraderTest {
    @DataProvider
    fun legacyFiles(): Array<Array<Any>> = files(V1X)
        .filter { LegacyFormat.legacyBbxVersion(load(it)) != null }
        .map { arrayOf<Any>(it) }.toTypedArray()

    @Test(dataProvider = "legacyFiles")
    fun upgradesToCurrentFormat(file: Path) {
        val doc = load(file)
        val version = LegacyFormat.legacyBbxVersion(doc)!!
        LegacyBbxUpgrader.upgrade(doc, version)

        assertEquals(BBX.getFormatVersion(doc), BBX.FORMAT_VERSION)
        BBX.assertIsA(doc)
        assertNull(LegacyFormat.legacyBbxVersion(doc), "an upgraded document is no longer legacy")
    }

    @Test(dataProvider = "legacyFiles")
    fun keepsAllText(file: Path) {
        val doc = load(file)
        val sourceWords = words(BBX.getRoot(doc)).map { it.replace('\t', ' ').trim() }.filter { it.isNotEmpty() }
        LegacyBbxUpgrader.upgrade(doc, LegacyFormat.legacyBbxVersion(doc)!!)
        val missing = missingWords(sourceWords.flatMap { it.split(' ') }, words(BBX.getRoot(doc)))
        assertTrue(missing.isEmpty(), "Text lost while upgrading $file: $missing")
    }

    @Test(dataProvider = "legacyFiles")
    fun translatesAfterUpgrade(file: Path) {
        val doc = load(file)
        LegacyBbxUpgrader.upgrade(doc, LegacyFormat.legacyBbxVersion(doc)!!)
        toBrf(doc) // must not throw
    }

    /**
     * 1.x's own upgrade test data: a `*.source.bbx` upgraded to the current format must have the
     * same structure as its `*.expected.bbx` upgraded to the current format.
     */
    @DataProvider
    fun upgradePairs(): Array<Array<Any>> = files(V1X.resolve("upgrade-pairs"))
        .filter { it.fileName.toString().endsWith(".source.bbx") }
        .map { arrayOf<Any>(it, it.resolveSibling(it.fileName.toString().replace(".source.", ".expected."))) }
        .toTypedArray()

    @Test(dataProvider = "upgradePairs")
    fun sourceMatchesExpected(source: Path, expected: Path) {
        val sourceDoc = upgraded(source)
        val expectedDoc = upgraded(expected)
        if (source.fileName.toString() in TABLE_DETECTION_DIFFERS) {
            assertEquals(words(BBX.getRoot(sourceDoc)), words(BBX.getRoot(expectedDoc)), "$source text")
            assertEquals(images(sourceDoc), images(expectedDoc), "$source images")
            return
        }
        assertEquals(structure(sourceDoc), structure(expectedDoc), "$source vs $expected")
    }

    private fun images(doc: nu.xom.Document) = FastXPath.descendant(doc)
        .filterIsInstance<nu.xom.Element>().filter { BBX.SPAN.IMAGE.isA(it) }
        .map { it.getAttributeValue("source", org.brailleblaster.utils.xml.BB_NS) }.toList()

    @Test
    fun tabsBecomeSpaces() {
        val doc = upgraded(V1X.resolve("edge/v2_tabs_in_text.bbx"))
        val text = FastXPath.descendant(doc).filterIsInstance<nu.xom.Text>().joinToString("") { it.value }
        assertFalse(text.contains('\t'))
        assertTrue(text.contains("Column one Column two Column three"))
    }

    @Test
    fun stepFailureBecomesBodyText() {
        val doc = load(V1X.resolve("edge/v3_upgrade_breaker_pagenum_no_block.bbx"))
        val report = LegacyBbxUpgrader.upgrade(doc, 3)

        assertEquals(BBX.getFormatVersion(doc), BBX.FORMAT_VERSION)
        assertEquals(report.fallbacks.size, 1, report.fallbacks.toString())
        assertTrue(bodyTextBlocks(doc).any { it.value == "12" }, doc.toXML())
        val text = words(BBX.getRoot(doc))
        assertTrue(text.containsAll(listOf("Text", "before.", "after.", "12")), text.toString())
        toBrf(doc)
    }

    @Test
    fun unknownSubtypeBecomesBodyText() {
        val doc = load(V1X.resolve("edge/v4_unknown_type.bbx"))
        val report = LegacyBbxUpgrader.upgrade(doc, 4)

        assertEquals(report.fallbacks.size, 1, report.fallbacks.toString())
        assertTrue(bodyTextBlocks(doc).any { it.value == "Block with a subtype 3.x does not know." }, doc.toXML())
        assertTrue(words(BBX.getRoot(doc)).containsAll(listOf("Known", "block.")))
        toBrf(doc)
    }

    @Test
    fun noFallbacksForRealOneXFiles() {
        val withFallbacks = files(V1X.resolve("documents"))
            .filter { LegacyFormat.legacyBbxVersion(load(it)) != null }
            .associateWith { val doc = load(it); LegacyBbxUpgrader.upgrade(doc, LegacyFormat.legacyBbxVersion(doc)!!) }
            .filterValues { it.fallbacks.isNotEmpty() }
        assertTrue(withFallbacks.isEmpty(), "Real 1.x files should convert exactly: $withFallbacks")
    }

    companion object {
        /**
         * 1.x stripped this table (issue #6163) because its formatter rated it "nontable". The 4 to 5
         * step decides by formatting the table with the current UTD engine, which now lays it out as
         * a table, so the table is kept. Text and images are unchanged; table detection is shared
         * with current imports and deliberately not changed here.
         */
        val TABLE_DETECTION_DIFFERS = setOf("4to5_brokenTable6163.source.bbx")
    }

    private fun upgraded(file: Path) = load(file).also { doc ->
        LegacyFormat.legacyBbxVersion(doc)?.let { LegacyBbxUpgrader.upgrade(doc, it) }
    }
}

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

import nu.xom.Document
import nu.xom.Element
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.legacy.LegacyTestSupport.V1X
import org.brailleblaster.bbx.legacy.LegacyTestSupport.V2X
import org.brailleblaster.bbx.legacy.LegacyTestSupport.bodyTextBlocks
import org.brailleblaster.bbx.legacy.LegacyTestSupport.files
import org.brailleblaster.bbx.legacy.LegacyTestSupport.load
import org.brailleblaster.bbx.legacy.LegacyTestSupport.missingCharacters
import org.brailleblaster.bbx.legacy.LegacyTestSupport.toBrf
import org.brailleblaster.utd.config.DocumentUTDConfig
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.properties.EmphasisType
import org.brailleblaster.utils.xml.BB_NS
import org.brailleblaster.utils.xml.MATHML_NS
import org.brailleblaster.utils.xml.UTD_NS
import org.testng.Assert.assertEquals
import org.testng.Assert.assertFalse
import org.testng.Assert.assertNotNull
import org.testng.Assert.assertNull
import org.testng.Assert.assertTrue
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.nio.file.Path

class V2ToBBXConverterTest {
    // ---------------- detection ----------------

    @DataProvider
    fun v2Files(): Array<Array<Any>> = files(V2X).map { arrayOf<Any>(it) }.toTypedArray()

    @Test(dataProvider = "v2Files")
    fun detectsTwoPointX(file: Path) {
        val doc = load(file)
        val before = doc.toXML()
        assertTrue(V2ToBBXConverter.isV2Document(doc))
        assertNull(LegacyFormat.legacyBbxVersion(doc), "2.0 files are not 1.x files")
        assertEquals(doc.toXML(), before, "detection must not modify the document")
    }

    @Test
    fun oneXAndCurrentFilesAreNotTwoPointX() {
        val notV2 = files(V1X) + LegacyFormatDetectionTest.CURRENT_FIXTURES
        notV2.forEach { assertFalse(V2ToBBXConverter.isV2Document(load(it)), it.toString()) }
    }

    // ---------------- every file ----------------

    @Test(dataProvider = "v2Files")
    fun convertsToCurrentFormatWithoutLosingText(file: Path) {
        val source = load(file)
        val sourceXml = source.toXML()
        val (doc, report) = V2ToBBXConverter.convert(source)

        assertEquals(source.toXML(), sourceXml, "the source document is not modified")
        assertEquals(BBX.getFormatVersion(doc), BBX.FORMAT_VERSION)
        BBX.assertIsA(doc)
        assertEquals(report.release, "2.0")
        val missing = missingCharacters(tmeText(source), LegacyFallback.visibleText(BBX.getRoot(doc)))
        assertTrue(missing.isEmpty(), "Text lost converting $file: $missing\n${doc.toXML()}")
        toBrf(doc)
    }

    /** 2.0 stored translated braille inside each tme as utd:brl; it must not become print text. */
    @Test(dataProvider = "v2Files")
    fun storedBrailleIsNotCopiedAsText(file: Path) {
        val source = load(file)
        val braille = elements(source)
            .filter { it.namespaceURI == UTD_NS && it.localName == "brl" }
            .map { it.value.trim() }.filter { it.length > 3 }
        val output = text(V2ToBBXConverter.convert(source).first)
        braille.forEach { assertFalse(output.contains(it), "braille \"$it\" copied into the text of $file") }
    }

    @Test
    fun realTwoPointXFilesConvertExactly() {
        val withFallbacks = files(V2X.resolve("real"))
            .associateWith { V2ToBBXConverter.convert(load(it)).second.fallbacks }
            .filterValues { it.isNotEmpty() }
        assertTrue(withFallbacks.isEmpty(), "Real 2.0 files should convert exactly: $withFallbacks")
    }

    // ---------------- mapping ----------------

    @Test
    fun emphasis() {
        val doc = convert("synthetic/emphasis_all.bbx")
        val expected = mapOf(
            "bold" to setOf(EmphasisType.BOLD), "BOLD" to setOf(EmphasisType.BOLD),
            "italic" to setOf(EmphasisType.ITALICS), "italics" to setOf(EmphasisType.ITALICS),
            "underline" to setOf(EmphasisType.UNDERLINE), "script" to setOf(EmphasisType.SCRIPT),
            "trans_1" to setOf(EmphasisType.TRANS_1), "trans_5" to setOf(EmphasisType.TRANS_5),
            "trans_note" to setOf(EmphasisType.TRANS_NOTE), "no_contract" to setOf(EmphasisType.NO_CONTRACT),
            "no_translate" to setOf(EmphasisType.NO_TRANSLATE),
        )
        val emphasised = elements(doc).filter { BBX.INLINE.EMPHASIS.isA(it) }
            .associate { it.value to BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS.get(it).toSet() }
        expected.forEach { (text, bits) -> assertEquals(emphasised[text], bits, text) }
        assertFalse(emphasised.containsKey("plain"), "plain is no emphasis")
        assertTrue(text(doc).contains("( 7 − 3 ) -: 2"), "linear math is kept as text")
    }

    @Test
    fun stylesAndOptions() {
        val doc = convert("synthetic/headings_styles.bbx")
        assertEquals(styleOf(doc, "Centered Heading underscore"), "Centered Heading")
        assertEquals(styleOf(doc, "Centered Heading space"), "Centered Heading")
        assertEquals(styleOf(doc, "Cell 5 Heading"), "Cell 5 Heading")
        assertEquals(styleOf(doc, "Cell 7 Heading"), "Cell 7 Heading")
        assertTrue(BBX.BLOCK.DEFAULT.isA(block(doc, "Prose paragraph.")), "Body Text is a default block")
        assertTrue(BBX.BLOCK.DEFAULT.isA(block(doc, "Style block, no style attr.")))
        assertTrue(BBX.CONTAINER.PRODNOTE.isA(block(doc, "Producer's note text.").parent))

        val options = block(doc, "Options block.")
        assertEquals(options.getAttributeValue("overrideOption-dontSplit", BB_NS), "true")
        assertEquals(options.getAttributeValue("overrideOption-keepWithNext", BB_NS), "true")
        assertNull(options.getAttributeValue("overrideOption-lineSpacing", BB_NS),
            "lineSpacing is not implemented by the current formatter, so it is dropped")
        assertEquals(options.getAttributeValue("overrideOption-guideWords", BB_NS), "false")
        assertEquals(options.getAttributeValue("overrideOption-skipNumberLines", BB_NS), "BOTH")
    }

    @Test
    fun listsAndPoems() {
        val doc = convert("synthetic/lists_poetry.bbx")
        val lists = elements(doc).filter { BBX.CONTAINER.LIST.isA(it) }
        assertEquals(lists.size, 2)
        val (list, poem) = lists
        assertEquals(BBX.CONTAINER.LIST.ATTRIB_LIST_TYPE.get(list), BBX.ListType.NORMAL)
        assertEquals(items(list).map { it.value to BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL.get(it) },
            listOf("Level 1 item" to 0, "Level 2 item" to 1, "Back to level 1" to 0))
        assertEquals(BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.get(list), 1)
        assertEquals(BBX.CONTAINER.LIST.ATTRIB_LIST_TYPE.get(poem), BBX.ListType.POEM)
        assertEquals(items(poem).map { it.value },
            listOf("First line of the poem", "Second line of the poem", "Third line of the poem"))
    }

    @Test
    fun tables() {
        val doc = convert("synthetic/tables.bbx")
        val tables = elements(doc).filter { BBX.CONTAINER.TABLE.isA(it) }
        assertEquals(tables.size, 2)
        val (simple, stairstep) = tables
        assertEquals(simple.getAttributeValue("format"), "simple")
        assertEquals(rowsOf(simple), listOf(listOf("R0C0", "R0C1", "R0C2"), listOf("R1C0", "R1C1", "R1C2"),
            listOf("R2C0", "R2C1", "R2C2")))
        assertEquals(stairstep.getAttributeValue("format"), "stairstep")
        assertEquals(rowsOf(stairstep), listOf(listOf("S00", "S01"), listOf("S10", "S11")))
        assertTrue(BBX.CONTAINER.TABLETN.isA(block(doc, "Table transcriber's note.").parent))
    }

    @Test
    fun realListedTable() {
        val doc = convert("real/Linear_Table.bbx")
        val table = elements(doc).firstOrNull { BBX.CONTAINER.TABLE.isA(it) }
        assertNotNull(table)
        assertEquals(table!!.getAttributeValue("format"), "listed")
        assertTrue(rowsOf(table).size > 1)
    }

    @Test
    fun titlePageTocAndVolumes() {
        val (doc, report) = V2ToBBXConverter.convert(load(V2X.resolve("synthetic/tpage_toc_volumes.bbx")))
        val title = block(doc, "A Sample Book")
        assertEquals(BBX._ATTRIB_OVERRIDE_STYLE.get(title), "TPage Heading")
        val category = title.parent as Element
        assertEquals(BBX.CONTAINER.TPAGE_CATEGORY.ATTRIB_TYPE.get(category), BBX.TPageCategory.TITLE)
        val section = category.parent as Element
        assertEquals(BBX.CONTAINER.TPAGE_SECTION.ATTRIB_TYPE.get(section), BBX.TPageSection.TITLE_PAGE)
        assertTrue(BBX.CONTAINER.TPAGE.isA(section.parent))
        assertEquals(BBX.CONTAINER.TPAGE_CATEGORY.ATTRIB_TYPE.get(block(doc, "By An Author").parent as Element),
            BBX.TPageCategory.AUTHOR)
        assertEquals(BBX.CONTAINER.TPAGE_SECTION.ATTRIB_TYPE.get(block(doc, "Special Symbols Used in This Volume").parent as Element),
            BBX.TPageSection.SPECIAL_SYMBOLS)

        // TOC entries are kept as Body Text with their page numbers (TOC Builder can rebuild them)
        val bodyText = bodyTextBlocks(doc).map { it.value }
        assertTrue(bodyText.contains("Chapter One 1"), bodyText.toString())
        assertTrue(bodyText.contains("Chapter Two 15"), bodyText.toString())
        assertTrue(report.fallbacks.count { it.startsWith("TOC") } == 3, report.fallbacks.toString())

        val volumes = elements(doc).filter { BBX.CONTAINER.VOLUME.isA(it) }
        assertEquals(volumes.map { BBX.CONTAINER.VOLUME.ATTRIB_TYPE.get(it) },
            listOf(BBX.VolumeType.VOLUME, BBX.VolumeType.VOLUME_SUPPLEMENTAL))
        assertEquals(volumes.map { it.childElements.first { e -> BBX.BLOCK.VOLUME_END.isA(e) }.value },
            listOf("End of Volume 1", "End of Supplemental Volume"))
        assertTrue(elements(doc).any { BBX.SPAN.PAGE_NUM.isA(it) && it.value == "2" })
    }

    @Test
    fun imagesMathAndInlineMarkers() {
        // Structure only: MathCAT is not available to translate MathML in the unit test environment
        val (doc, report) = V2ToBBXConverter.convert(load(V2X.resolve("synthetic/images_math_misc.bbx")), checkTranslation = false)
        val image = elements(doc).single { BBX.SPAN.IMAGE.isA(it) }
        assertEquals(BBX.SPAN.IMAGE.ATTRIB_SOURCE.get(image), "images/fig1.jpg")
        assertEquals(image.getAttributeValue("alt"), "A red circle")
        val placeholder = elements(doc).single { BBX.BLOCK.IMAGE_PLACEHOLDER.isA(it) }
        assertEquals(placeholder.getAttributeValue("skipLines", UTD_NS), "5")
        assertTrue(bodyTextBlocks(doc).any { it.value == "Long description of figure 3." })

        val math = elements(doc).single { BBX.INLINE.MATHML.isA(it) }
        assertEquals(math.childElements[0].localName, "math")
        assertEquals(math.childElements[0].namespaceURI, MATHML_NS)

        assertTrue(bodyTextBlocks(doc).any { it.value == "12" } && bodyTextBlocks(doc).any { it.value == "+ 3" },
            "spatial math lines kept as Body Text")
        assertTrue(BBX.CONTAINER.BOX.isA(block(doc, "Inside a box.").parent))
        assertEquals(block(doc, "Tabstop.").getAttributeValue("overrideOption-newPagesBefore", BB_NS), "1",
            "a 2.0 page break becomes a new page before the next block")
        val tab = elements(doc).single { BBX.SPAN.TAB.isA(it) }
        assertEquals(BBX.SPAN.TAB.ATTRIB_VALUE.get(tab), 10)
        assertTrue(elements(doc).any { BBX.SPAN.PROSE_LINE_NUMBER.isA(it) && it.value == "5" })
        assertTrue(elements(doc).any { BBX.SPAN.GUIDEWORD.isA(it) && it.value == "guide" })
        assertTrue(text(doc).contains("tuh-MAY-toe"), "pronunciation text kept")
        assertTrue(report.fallbacks.any { it.startsWith("spatial math") }, report.fallbacks.toString())
    }

    @Test
    fun pageSettingsArePreserved() {
        val doc = convert("synthetic/page_settings_head.bbx")
        val settings = DocumentUTDConfig.NIMAS.loadPageSettings(doc)
        assertNotNull(settings)
        assertTrue(settings!!.interpoint)
        assertEquals(settings.paperWidth, 292.1, 0.001)
    }

    // ---------------- fallback ----------------

    @Test
    fun unknownTmeTypeKeepsText() {
        val (doc, report) = V2ToBBXConverter.convert(load(V2X.resolve("fallback/unknown_tme_type.bbx")))
        assertTrue(text(doc).contains("Mystery tme text"))
        assertEquals(report.fallbacks.size, 1, report.fallbacks.toString())
    }

    @Test
    fun unknownBlockTypeBecomesBodyText() {
        val (doc, report) = V2ToBBXConverter.convert(load(V2X.resolve("fallback/unknown_block_type.bbx")))
        assertTrue(bodyTextBlocks(doc).any { it.value == "Text in an unknown block type." })
        assertEquals(report.fallbacks.size, 1, report.fallbacks.toString())
    }

    @Test
    fun unknownStyleBecomesBodyText() {
        val (doc, report) = V2ToBBXConverter.convert(load(V2X.resolve("fallback/unknown_style.bbx")))
        assertTrue(bodyTextBlocks(doc).any { it.value == "Text with an unknown style." })
        assertEquals(report.fallbacks.size, 1, report.fallbacks.toString())
    }

    @Test
    fun brokenTableBecomesBodyText() {
        val (doc, report) = V2ToBBXConverter.convert(load(V2X.resolve("fallback/broken_table_grid.bbx")))
        assertTrue(elements(doc).none { BBX.CONTAINER.TABLE.isA(it) })
        assertEquals(bodyTextBlocks(doc).map { it.value },
            listOf("A", "A again (duplicate cell)", "Far away cell", "Cell without row/col"))
        assertEquals(report.fallbacks.size, 1, report.fallbacks.toString())
    }

    @Test
    fun badMathMlKeepsText() {
        val (doc, report) = V2ToBBXConverter.convert(load(V2X.resolve("fallback/bad_mathml.bbx")))
        assertTrue(elements(doc).none { BBX.INLINE.MATHML.isA(it) })
        assertTrue(text(doc).replace(" ", "").contains("31"), doc.toXML())
        assertEquals(report.fallbacks.size, 1, report.fallbacks.toString())
    }

    @Test
    fun badListLevelBecomesBodyText() {
        val (doc, report) = V2ToBBXConverter.convert(load(V2X.resolve("fallback/bad_list_level.bbx")))
        assertTrue(bodyTextBlocks(doc).any { it.value == "Item with junk level" })
        assertEquals(report.fallbacks.size, 1, report.fallbacks.toString())
    }

    @DataProvider
    fun exactFallbackFiles(): Array<Array<Any>> = listOf(
        "fallback/empty_blocklist.bbx", "fallback/version_2_1.bbx",
        "fallback/version_whitespace.bbx", "fallback/unknown_attributes.bbx",
    ).map { arrayOf<Any>(it) }.toTypedArray()

    @Test(dataProvider = "exactFallbackFiles")
    fun unusualButValidFilesConvertExactly(name: String) {
        val (_, report) = V2ToBBXConverter.convert(load(V2X.resolve(name)))
        assertTrue(report.fallbacks.isEmpty(), report.fallbacks.toString())
    }

    // ---------------- helpers ----------------

    private fun convert(name: String): Document = V2ToBBXConverter.convert(load(V2X.resolve(name))).first

    private fun elements(doc: Document): List<Element> =
        FastXPath.descendant(doc.rootElement).filterIsInstance<Element>().toList()

    private fun block(doc: Document, text: String): Element =
        elements(doc).firstOrNull { BBX.BLOCK.isA(it) && it.value == text }
            ?: throw AssertionError("No block \"$text\" in ${doc.toXML()}")

    private fun styleOf(doc: Document, text: String): String? = BBX._ATTRIB_OVERRIDE_STYLE.get(block(doc, text))

    private fun items(list: Element): List<Element> = list.childElements.filter { BBX.BLOCK.LIST_ITEM.isA(it) }

    private fun rowsOf(table: Element): List<List<String>> =
        table.childElements.filter { BBX.CONTAINER.TABLE_ROW.isA(it) }.map { row -> row.childElements.map { it.value } }

    private fun text(doc: Document): String = LegacyFallback.visibleText(BBX.getRoot(doc))

    /** Print text of all 2.0 tme elements; image paths become attributes, not text. */
    private fun tmeText(doc: Document): String = elements(doc)
        .filter { it.localName == "tme" && it.getAttributeValue("type") !in setOf("imgref", "imgplaceholder") }
        .joinToString(" ") { LegacyFallback.visibleText(it) }
}

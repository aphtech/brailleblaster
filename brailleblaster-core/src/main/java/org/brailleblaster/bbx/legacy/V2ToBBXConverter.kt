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

import nu.xom.Attribute
import nu.xom.Document
import nu.xom.Element
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBX.ListType
import org.brailleblaster.bbx.BBX.TPageCategory
import org.brailleblaster.bbx.BBX.TPageSection
import org.brailleblaster.bbx.BBX.TableRowType
import org.brailleblaster.bbx.BBX.VolumeType
import org.brailleblaster.bbx.fixers2.LiveFixer
import org.brailleblaster.bbx.utd.BBXDynamicOptionStyleMap
import org.brailleblaster.settings.UTDManager
import org.brailleblaster.settings.UTDManager.Companion.loadStyleDefinitions
import org.brailleblaster.settings.UTDManager.Companion.preferredFormatStandard
import org.brailleblaster.utd.config.DocumentUTDConfig
import org.brailleblaster.utd.config.StyleDefinitions
import org.brailleblaster.utd.properties.EmphasisType
import org.brailleblaster.utd.properties.NumberLinePosition
import org.brailleblaster.utils.xml.BB_NS
import org.brailleblaster.utils.xml.MATHML_NS
import org.brailleblaster.utils.xml.UTD_NS
import org.slf4j.LoggerFactory
import java.util.EnumSet

/**
 * Converts a document written by BrailleBlaster 2.0 into a new document in the current BBX format.
 *
 * 2.0 used its own flat format: `bbdoc/head` + `blocklist/block/tme` with `<version>2.3</version>`
 * (see 2.0's `bbx.xsd`). Each block becomes current BBX: paragraphs, headings, lists, poems,
 * tables, title pages, volumes, page numbers, images, emphasis, math and style options.
 *
 * Anything that cannot be converted exactly is put in as a Body Text block holding its text and
 * recorded in the report, so no text is lost and the conversion never fails.
 */
object V2ToBBXConverter {
    private val log = LoggerFactory.getLogger(V2ToBBXConverter::class.java)
    const val RELEASE = "2.0"
    private val V2_VERSION = Regex("^2\\.\\d+$")

    /** True for documents written by BrailleBlaster 2.0. Read-only. */
    fun isV2Document(doc: Document): Boolean {
        val root = doc.rootElement
        if (!LegacyFormat.isBbDoc(root)) return false
        if (root.getFirstChildElement("blocklist", BB_NS) != null) return true
        return LegacyFormat.versionText(root)?.matches(V2_VERSION) == true
    }

    /**
     * Convert [source] (not modified) into a new current format document.
     * @param checkTranslation translate the result once and turn any content that fails into Body
     * Text; only turned off by tests that check structure in environments without MathCAT
     */
    fun convert(source: Document, checkTranslation: Boolean = true): Pair<Document, LegacyImportReport> {
        val version = LegacyFormat.versionText(source.rootElement) ?: "unknown"
        log.info("Converting BrailleBlaster {} document (version {})", RELEASE, version)
        val conversion = Conversion(loadStyleDefinitions(preferredFormatStandard), checkTranslation)
        val doc = conversion.run(source)
        conversion.fallbacks.forEach { log.warn("Converted to Body Text while converting {} document: {}", RELEASE, it) }
        return doc to LegacyImportReport(RELEASE, version, conversion.fallbacks.toList())
    }

    private class Conversion(private val styleDefs: StyleDefinitions, private val checkTranslation: Boolean) {
        val fallbacks = mutableListOf<String>()
        private var pendingNewPages = 0
        private var openBox: Element? = null

        fun run(source: Document): Document {
            val doc = BBX.newDocument()
            copyHead(source, doc)
            val section = BBX.SECTION.OTHER.create()
            doc.rootElement.appendChild(BBX.SECTION.ROOT.create().apply { appendChild(section) })

            source.rootElement.childElements
                .filter { it.localName != "head" }
                .forEach { child ->
                    if (child.localName == "blocklist") convertBlocks(blockElements(child), section)
                    else bodyText(section, child, "unexpected element ${child.localName}")
                }
            if (pendingNewPages > 0) log.info("Dropped a page break at the end of the document")

            LegacyBbxUpgrader.runStep(doc, "live fixer", fallbacks) { LiveFixer.fix(BBX.getRoot(it)) }
            fallbacks += LegacyBbxUpgrader.replaceUnknownTypes(doc)
            if (checkTranslation) LegacyBbxUpgrader.runStep(doc, "translation check", fallbacks) { translate(it) }
            return doc
        }

        // ---------------- head ----------------

        /** Keep UTD settings (page settings, braille settings) written in the 2.0 head. */
        private fun copyHead(source: Document, doc: Document) {
            val head = BBX.getHead(doc)
            source.rootElement.getFirstChildElement("head", BB_NS)?.childElements
                ?.filter { it.localName != "version" }
                ?.forEach { setting ->
                    if (setting.namespaceURI == UTD_NS) head.appendChild(setting.copy())
                    else log.info("Not keeping 2.0 header entry {}", setting.qualifiedName)
                }
            listOf<(Document) -> Any?>(
                { DocumentUTDConfig.NIMAS.loadPageSettings(it) },
                { DocumentUTDConfig.NIMAS.loadBrailleSettings(it) },
            ).forEach { load ->
                runCatching { load(doc) }.onFailure { e ->
                    log.warn("Dropping unreadable settings from 2.0 header", e)
                    head.childElements.filter { it.namespaceURI == UTD_NS && it.localName.endsWith("Settings") }
                        .forEach { it.detach() }
                }
            }
        }

        // ---------------- blocks ----------------

        private fun blockElements(blocklist: Element): List<Element> = blocklist.childElements
            .filter { child ->
                when {
                    child.localName == "block" -> true
                    child.namespaceURI == UTD_NS -> false // utd:newLine etc. are regenerated by UTD
                    else -> {
                        if (LegacyFallback.visibleText(child).isNotBlank()) {
                            pendingFallbackElements += child
                            true
                        } else false
                    }
                }
            }

        private val pendingFallbackElements = mutableSetOf<Element>()

        private fun convertBlocks(blocks: List<Element>, section: Element) {
            // 2.0 tables are identified by gid; a cell can appear after other blocks
            val tableCells = blocks.filter { typeOf(it) in TABLE_TYPES && it.getAttributeValue("gid") != null }
                .groupBy { it.getAttributeValue("gid") }
            val done = mutableSetOf<Element>()
            var i = 0
            while (i < blocks.size) {
                val block = blocks[i]
                if (block in done) { i++; continue }
                val type = typeOf(block)
                val run = when {
                    block in pendingFallbackElements -> listOf(block)
                    type in TABLE_TYPES -> tableCells[block.getAttributeValue("gid")]
                        ?: runOf(blocks, i) { typeOf(it) in TABLE_TYPES && it.getAttributeValue("gid") == null }
                    type == "LIST" -> runOf(blocks, i) { typeOf(it) == "LIST" && sameGroup(it, block) }
                    type == "POETRY" -> runOf(blocks, i) { typeOf(it) == "POETRY" && sameGroup(it, block) }
                    type == "TPAGE" -> runOf(blocks, i) { typeOf(it) == "TPAGE" }
                    else -> listOf(block)
                }
                when {
                    block in pendingFallbackElements -> bodyTextElement(target(section), block)
                    type in TABLE_TYPES -> convertTable(run, target(section))
                    type == "LIST" -> convertList(run, ListType.NORMAL, target(section))
                    type == "POETRY" -> convertList(run, ListType.POEM, target(section))
                    type == "TPAGE" -> convertTPage(run, target(section))
                    else -> convertSingle(block, section)
                }
                done += run
                i++
            }
        }

        private fun runOf(blocks: List<Element>, start: Int, belongs: (Element) -> Boolean): List<Element> =
            blocks.drop(start).takeWhile(belongs)

        private fun sameGroup(a: Element, b: Element): Boolean {
            val gidA = a.getAttributeValue("gid")
            val gidB = b.getAttributeValue("gid")
            return gidA == null || gidB == null || gidA == gidB
        }

        private fun target(section: Element): Element = openBox ?: section

        private fun convertSingle(block: Element, section: Element) {
            val tmes = tmes(block)
            val kinds = tmes.map { tmeType(it) }.toSet()
            val onlyKind = kinds.singleOrNull()?.takeIf { textOf(block).isBlank() || it == "pageind" }
            when {
                onlyKind == "boxline" -> toggleBox(section)
                onlyKind == "newpage" -> pendingNewPages++
                onlyKind == "sepline" -> log.info("Dropped a 2.0 separation line (no text)")
                onlyKind == "pageind" -> emit(section, BBX.BLOCK.PAGE_NUM.create().apply { appendChild(textOf(block).trim()) })
                else -> convertBlockByType(block, typeOf(block), section)
            }
        }

        private fun convertBlockByType(block: Element, type: String?, section: Element) {
            when (type) {
                "DEFAULT", "PROSE", "STYLE", "EMPTY_DOC", "END_OF_DOC", "IMGDESC", null -> {
                    if (textOf(block).isBlank() && tmes(block).none { tmeType(it) in IMAGE_TMES }) return
                    convertParagraph(block, section)
                }
                "PROD_NOTE" -> {
                    val note = BBX.CONTAINER.PRODNOTE.create()
                    target(section).appendChild(note)
                    convertParagraph(block, note)
                }
                "TABLETN" -> {
                    val tn = BBX.CONTAINER.TABLETN.create()
                    target(section).appendChild(tn)
                    convertParagraph(block, tn)
                }
                "VOLUME" -> convertVolume(block, section)
                "TOC", "TOCVOL", "TOCVOLSPLIT", "PAGE_TYPE_INDICATOR" ->
                    bodyText(target(section), block, "${block.getAttributeValue("type")} block")
                "SPATIAL_MATH" -> convertSpatialMath(block, target(section))
                else -> bodyText(target(section), block, "unknown block type $type")
            }
        }

        /** A block of text: style, options and inline content. Image placeholders become their own blocks. */
        private fun convertParagraph(block: Element, parent: Element) {
            val paragraph = styledBlock(block.getAttributeValue("overrideStyle"), block)
            applyOptions(block, paragraph)
            val (placeholders, inline) = tmes(block).partition { tmeType(it) == "imgplaceholder" }
            val descriptions = inline.filter { tmeType(it) == "imgdesc" }
            fillInline(paragraph, inline - descriptions.toSet())
            if (paragraph.childCount > 0) emit(parent, paragraph)
            placeholders.forEach { emit(parent, imagePlaceholder(it)) }
            descriptions.forEach { bodyText(parent, it, "image description") }
        }

        /** Add [elem] to [parent], applying a page break that came before it. */
        private fun emit(parent: Element, elem: Element) {
            if (pendingNewPages > 0 && BBX.BLOCK.isA(elem)) {
                BBXDynamicOptionStyleMap.setStyleOptionAttrib(
                    elem, org.brailleblaster.utd.Style.StyleOption.NEW_PAGES_BEFORE, pendingNewPages
                )
                pendingNewPages = 0
            }
            if (parent === openBox || BBX.SECTION.isA(parent)) target(parent).appendChild(elem) else parent.appendChild(elem)
        }

        private fun toggleBox(section: Element) {
            openBox = if (openBox == null) {
                BBX.CONTAINER.BOX.create().also {
                    BBX._ATTRIB_OVERRIDE_STYLE.set(it, "Box")
                    section.appendChild(it)
                }
            } else null
        }

        private fun styledBlock(rawStyle: String?, source: Element): Element {
            val style = normaliseStyle(rawStyle)
            return when {
                style == null || style == LegacyFallback.BODY_TEXT -> BBX.BLOCK.DEFAULT.create()
                styleDefs.getStyleByName(style) != null -> BBX.BLOCK.STYLE.create(style)
                else -> {
                    fallbacks += "style \"$rawStyle\": " + LegacyFallback.describe(source)
                    BBX.BLOCK.STYLE.create(LegacyFallback.BODY_TEXT)
                }
            }
        }

        private fun applyOptions(source: Element, block: Element) {
            (0 until source.attributeCount).map { source.getAttribute(it) }
                .filter { it.localName.startsWith(BBXDynamicOptionStyleMap.OPTION_ATTRIB_PREFIX) }
                .forEach { attribute ->
                    val name = attribute.localName.removePrefix(BBXDynamicOptionStyleMap.OPTION_ATTRIB_PREFIX)
                    val option = org.brailleblaster.utd.Style.StyleOption.values().firstOrNull { it.optionName == name }
                    val value = attribute.value.trim()
                    // lineSpacing is left out: the current formatter does not implement it
                    val valid = when (name) {
                        "dontSplit", "keepWithNext", "guideWords" -> value == "true" || value == "false"
                        "skipNumberLines" -> NumberLinePosition.entries.any { it.name == value }
                        else -> false
                    }
                    if (option != null && valid) BBXDynamicOptionStyleMap.setStyleOptionAttrib(block, option, value)
                    else log.info("Dropped 2.0 style option {}=\"{}\"", attribute.localName, value)
                }
        }

        // ---------------- inline content ----------------

        private fun fillInline(parent: Element, tmes: List<Element>) {
            tmes.forEach { tme ->
                val text = tmeText(tme)
                when (tmeType(tme)) {
                    "text", "tablecell", "tabtext", "proseNumber".lowercase(), "spmathtme" -> appendText(parent, text, tme)
                    "pageind" -> parent.appendChild(BBX.SPAN.PAGE_NUM.create().apply { appendChild(text.trim()) })
                    "imgref" -> parent.appendChild(imageSpan(tme))
                    "math" -> appendMath(parent, tme)
                    "tab" -> parent.appendChild(BBX.SPAN.TAB.create().also { tab ->
                        tme.getAttributeValue("tabValue")?.toIntOrNull()?.let { BBX.SPAN.TAB.ATTRIB_VALUE.set(tab, it) }
                    })
                    "guideword" -> parent.appendChild(BBX.SPAN.GUIDEWORD.create().apply { appendChild(text) })
                    "linenumber" -> parent.appendChild(BBX.SPAN.PROSE_LINE_NUMBER.create().apply { appendChild(text) })
                    "tocpagenum" -> appendPlain(parent, " $text")
                    "newpage", "boxline", "sepline", "spmathlinebr", "spamathwhitesp", "tablecellsep", "tabletnsep",
                    "emptydoc", "endofdoc" -> if (text.isNotBlank()) appendPlain(parent, text)
                    else -> if (text.isNotBlank()) {
                        fallbacks += "tme type \"${tme.getAttributeValue("type")}\" \"${text.trim()}\""
                        appendPlain(parent, " $text")
                    }
                }
            }
        }

        private fun appendText(parent: Element, text: String, tme: Element) {
            if (text.isEmpty()) return
            val emphasis = emphasisOf(tme.getAttributeValue("emphasis"))
            if (emphasis.isEmpty()) appendPlain(parent, text)
            else parent.appendChild(BBX.INLINE.EMPHASIS.create(emphasis).apply { appendChild(text) })
        }

        private fun appendPlain(parent: Element, text: String) {
            val last = if (parent.childCount > 0) parent.getChild(parent.childCount - 1) else null
            if (last is Text) last.value += text else parent.appendChild(text)
        }

        private fun emphasisOf(raw: String?): EnumSet<EmphasisType> {
            val result = EnumSet.noneOf(EmphasisType::class.java)
            raw?.split(Regex("[\\s,]+"))?.filter { it.isNotBlank() }?.forEach { token ->
                val key = token.uppercase()
                val parts = EMPHASIS[key]?.let { listOf(key) } ?: key.split('_')
                parts.forEach { part ->
                    when {
                        EMPHASIS.containsKey(part) -> EMPHASIS[part]?.let { result.add(it) }
                        part in IGNORED_EMPHASIS -> {}
                        else -> log.info("Dropped unknown 2.0 emphasis \"{}\" (text kept)", token)
                    }
                }
            }
            return result
        }

        private fun appendMath(parent: Element, tme: Element) {
            val math = tme.childElements.firstOrNull { it.localName == "math" && it.namespaceURI == MATHML_NS }
            if (math != null && isValidMathMl(math)) {
                val copy = math.copy()
                // drop translated braille that 2.0 stored inside the math
                org.brailleblaster.utd.internal.xml.FastXPath.descendant(copy).filterIsInstance<Element>()
                    .filter { it.namespaceURI == UTD_NS }.toList().forEach { it.detach() }
                parent.appendChild(BBX.INLINE.MATHML.create().apply { appendChild(copy) })
            } else {
                fallbacks += "math \"${tmeText(tme).trim()}\""
                appendPlain(parent, tmeText(tme))
            }
        }

        private fun isValidMathMl(math: Element): Boolean =
            org.brailleblaster.utd.internal.xml.FastXPath.descendantOrSelf(math)
                .filterIsInstance<Element>()
                .filter { it.namespaceURI != UTD_NS }
                .all { it.namespaceURI == MATHML_NS && it.localName in MATHML_ELEMENTS }

        private fun imageSpan(tme: Element): Element = BBX.SPAN.IMAGE.create().also { span ->
            val source = tme.getAttributeValue("source") ?: tmeText(tme).trim()
            BBX.SPAN.IMAGE.ATTRIB_SOURCE.set(span, source)
            span.addAttribute(Attribute("alt", tme.getAttributeValue("alt") ?: ""))
        }

        private fun imagePlaceholder(tme: Element): Element = BBX.BLOCK.IMAGE_PLACEHOLDER.create().also { block ->
            val lines = tme.getAttributeValue("skipLines")?.toIntOrNull() ?: 1
            block.addAttribute(BBX.BLOCK.IMAGE_PLACEHOLDER.ATTRIB_SKIP_LINES.newAttribute(lines))
            (tme.getAttributeValue("source") ?: tme.getAttributeValue("imgID"))?.let {
                block.addAttribute(BBX.BLOCK.IMAGE_PLACEHOLDER.ATTRIB_IMG_PATH.newAttribute(it))
            }
            tme.getAttributeValue("alt")?.let {
                block.addAttribute(BBX.BLOCK.IMAGE_PLACEHOLDER.ATTRIB_ALT_TEXT.newAttribute(it))
            }
        }

        // ---------------- structures ----------------

        private fun convertList(run: List<Element>, listType: ListType, parent: Element) {
            val list = BBX.CONTAINER.LIST.create(listType)
            val prefix = if (listType == ListType.POEM) "P" else "L"
            val levels = run.map { block ->
                val levelAttr = block.getAttributeValue("level")
                val style = normaliseStyle(block.getAttributeValue("overrideStyle"))
                when {
                    levelAttr != null -> levelAttr.trim().toIntOrNull()
                    style != null -> LIST_STYLE.matchEntire(style)?.takeIf { it.groupValues[1] == prefix }
                        ?.groupValues?.get(2)?.toInt()?.let { (it - 1) / 2 } ?: 0
                    else -> 0
                }
            }
            run.zip(levels).forEach { (block, level) ->
                if (level == null || level < 0) {
                    bodyText(list, block, "list item level \"${block.getAttributeValue("level")}\"")
                    return@forEach
                }
                val item = BBX.BLOCK.LIST_ITEM.create()
                BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL.set(item, level)
                normaliseStyle(block.getAttributeValue("overrideStyle"))
                    ?.takeIf { styleDefs.getStyleByName(it) != null }
                    ?.let { BBX._ATTRIB_OVERRIDE_STYLE.set(item, it) }
                applyOptions(block, item)
                fillInline(item, tmes(block))
                list.appendChild(item)
            }
            BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.set(list, levels.filterNotNull().maxOrNull() ?: 0)
            if (list.childCount > 0) emit(parent, list)
        }

        private fun convertTable(run: List<Element>, parent: Element) {
            val cells = run.map { block ->
                Triple(block.getAttributeValue("row")?.trim()?.toIntOrNull(),
                    block.getAttributeValue("col")?.trim()?.toIntOrNull(), block)
            }
            val positions = cells.mapNotNull { (row, col, _) -> if (row != null && col != null) row to col else null }
            val rows = positions.map { it.first }.distinct().sorted()
            val cols = positions.map { it.second }.distinct().sorted()
            val rowCount = (rows.maxOrNull() ?: -1) + 1
            val colCount = (cols.maxOrNull() ?: -1) + 1
            // 2.0 left empty cells out, so missing positions are filled with empty cells;
            // several blocks at one position are the paragraphs of one cell
            val distinct = positions.toSet()
            val consistent = positions.size == cells.size &&
                distinct.all { (row, col) -> row >= 0 && col >= 0 } &&
                distinct.size * 2 >= rowCount * colCount
            if (!consistent) {
                fallbacks += "table with an incomplete row/column grid (${cells.size} cells)"
                run.forEach { bodyTextElement(parent, it, record = false) }
                return
            }
            val table = BBX.CONTAINER.TABLE.create()
            // same attribute AutoTableFormatter reads; NONTABLE and unknown types are left to auto detection
            TABLE_FORMATS[run.first().getAttributeValue("tableType")?.uppercase()]
                ?.let { table.addAttribute(Attribute("format", it)) }
            (0 until rowCount).forEach { row ->
                val rowCells = cells.filter { it.first == row }.groupBy { it.second!! }
                val isHead = rowCells.isNotEmpty() && rowCells.values.flatten().all { typeOf(it.third) == "TABLEHEAD" }
                val tableRow = BBX.CONTAINER.TABLE_ROW.create(if (isHead) TableRowType.HEAD else TableRowType.NORMAL)
                (0 until colCount).forEach { col ->
                    tableRow.appendChild(BBX.BLOCK.TABLE_CELL.create().also { cell ->
                        rowCells[col]?.forEachIndexed { index, (_, _, block) ->
                            if (index > 0) appendPlain(cell, " ")
                            fillInline(cell, tmes(block))
                        }
                    })
                }
                table.appendChild(tableRow)
            }
            emit(parent, table)
        }

        private fun convertTPage(run: List<Element>, parent: Element) {
            val tpage = BBX.CONTAINER.TPAGE.create()
            var currentSection: Pair<String?, Element>? = null
            var currentCategory: Pair<String?, Element>? = null
            run.forEach { block ->
                val sectionName = block.getAttributeValue("section")
                val categoryName = block.getAttributeValue("category")
                val section = sectionName?.let { TPAGE_SECTIONS[it] }
                val category = categoryName?.let { TPAGE_CATEGORIES[it] }
                if ((sectionName != null && section == null) || (categoryName != null && category == null)) {
                    bodyText(tpage, block, "title page section \"$sectionName\" category \"$categoryName\"")
                    return@forEach
                }
                var container = tpage
                if (section != null) {
                    if (currentSection?.first != sectionName) {
                        currentSection = sectionName to BBX.CONTAINER.TPAGE_SECTION.create(section).also { tpage.appendChild(it) }
                        currentCategory = null
                    }
                    container = currentSection!!.second
                }
                if (category != null) {
                    if (currentCategory?.first != categoryName || currentCategory?.second?.parent !== container) {
                        currentCategory = categoryName to BBX.CONTAINER.TPAGE_CATEGORY.create(category).also { container.appendChild(it) }
                    }
                    container = currentCategory!!.second
                }
                convertParagraph(block, container)
            }
            emit(parent, tpage)
        }

        private fun convertVolume(block: Element, section: Element) {
            val type = VOLUME_TYPES[block.getAttributeValue("volumeType")?.uppercase()] ?: VolumeType.VOLUME
            val volume = BBX.CONTAINER.VOLUME.create(type)
            val text = textOf(block).trim().ifEmpty { "End of Volume" }
            volume.appendChild(BBX.BLOCK.VOLUME_END.create().apply { appendChild(text) })
            section.appendChild(volume)
        }

        /** Spatial math layout cannot be rebuilt exactly; keep each line as a Body Text block. */
        private fun convertSpatialMath(block: Element, parent: Element) {
            fallbacks += "spatial math " + LegacyFallback.describe(block)
            val lines = mutableListOf(StringBuilder())
            tmes(block).forEach { tme ->
                when (tmeType(tme)) {
                    "spmathlinebr" -> lines += StringBuilder()
                    else -> lines.last().append(tmeText(tme))
                }
            }
            lines.map { it.toString().trim() }.filter { it.isNotEmpty() }
                .forEach { emit(parent, LegacyFallback.bodyTextBlock(it)) }
        }

        // ---------------- fallback ----------------

        /** Body Text for a 2.0 block, keeping its tme text in order with page numbers spaced out. */
        private fun bodyText(parent: Element, source: Element, reason: String) {
            fallbacks += "$reason: " + LegacyFallback.describe(source)
            bodyTextElement(parent, source, record = false)
        }

        private fun bodyTextElement(parent: Element, source: Element, record: Boolean = true) {
            if (record) fallbacks += LegacyFallback.describe(source)
            val text = tmes(source).takeIf { it.isNotEmpty() }
                ?.joinToString("") { tme -> if (tmeType(tme) in SPACED_TMES) " ${tmeText(tme)} " else tmeText(tme) }
                ?: LegacyFallback.visibleText(source)
            val clean = text.replace(Regex("[ \\t]+"), " ").trim()
            if (clean.isNotEmpty()) emit(parent, LegacyFallback.bodyTextBlock(clean))
        }

        // ---------------- helpers ----------------

        private fun tmes(block: Element): List<Element> = block.childElements.toList().filter { it.localName == "tme" }
        private fun tmeType(tme: Element): String = tme.getAttributeValue("type")?.lowercase() ?: "text"
        private fun typeOf(block: Element): String? = block.getAttributeValue("type")?.trim()?.uppercase()
        private fun textOf(block: Element): String = tmes(block).joinToString("") { tmeText(it) }

        /** Print text of a tme; 2.0 also stored the translated braille inside it as utd:brl. */
        private fun tmeText(tme: Element): String = LegacyFallback.visibleText(tme)

        private fun normaliseStyle(raw: String?): String? =
            raw?.replace('_', ' ')?.replace(Regex("\\s+"), " ")?.trim()?.takeIf { it.isNotEmpty() }

        private fun translate(doc: Document) {
            val utdManager = UTDManager()
            utdManager.loadEngineFromDoc(doc, "bbx")
            utdManager.engine.translateAndFormatDocument(doc.copy())
        }
    }

    private val TABLE_TYPES = setOf("TABLE", "TABLEHEAD", "TABLECELL")
    private val IMAGE_TMES = setOf("imgref", "imgplaceholder", "imgdesc")
    private val SPACED_TMES = setOf("tocpagenum", "pageind", "pronunciation", "linenumber")
    private val LIST_STYLE = Regex("^([LP])(\\d+)-(\\d+)$")
    private val IGNORED_EMPHASIS = setOf("PLAIN", "MATH")

    private val EMPHASIS: Map<String, EmphasisType> = mapOf(
        "BOLD" to EmphasisType.BOLD,
        "ITALIC" to EmphasisType.ITALICS,
        "ITALICS" to EmphasisType.ITALICS,
        "UNDERLINE" to EmphasisType.UNDERLINE,
        "SCRIPT" to EmphasisType.SCRIPT,
        "TRANS_1" to EmphasisType.TRANS_1,
        "TRANS_2" to EmphasisType.TRANS_2,
        "TRANS_3" to EmphasisType.TRANS_3,
        "TRANS_4" to EmphasisType.TRANS_4,
        "TRANS_5" to EmphasisType.TRANS_5,
        "TRANS_NOTE" to EmphasisType.TRANS_NOTE,
        "NO_CONTRACT" to EmphasisType.NO_CONTRACT,
        "NO_TRANSLATE" to EmphasisType.NO_TRANSLATE,
    )

    private val TABLE_FORMATS = mapOf(
        "SIMPLE" to "simple",
        "LISTED" to "listed",
        "STAIRSTEP" to "stairstep",
        "LINEAR" to "linear",
    )

    private val TPAGE_SECTIONS = mapOf(
        "title" to TPageSection.TITLE_PAGE,
        "secondTitle" to TPageSection.SECOND_TITLE_PAGE,
        "specialSymbols" to TPageSection.SPECIAL_SYMBOLS,
        "transcriberNotes" to TPageSection.TRANSCRIBER_NOTES,
    )

    private val TPAGE_CATEGORIES = mapOf(
        "title" to TPageCategory.TITLE,
        "author" to TPageCategory.AUTHOR,
        "publisher" to TPageCategory.PUBLISHER,
        "transcription" to TPageCategory.TRANSCRIPTION,
        "volume" to TPageCategory.VOLUMES,
    )

    private val VOLUME_TYPES = mapOf(
        "NORMAL" to VolumeType.VOLUME,
        "PRELIMINARY" to VolumeType.VOLUME_PRELIMINARY,
        "SUPPLEMENTAL" to VolumeType.VOLUME_SUPPLEMENTAL,
    )

    /** Presentation MathML element names; anything else makes the math fall back to text. */
    private val MATHML_ELEMENTS = setOf(
        "math", "mi", "mn", "mo", "mtext", "mspace", "ms", "mglyph", "mrow", "mfrac", "msqrt", "mroot",
        "mstyle", "merror", "mpadded", "mphantom", "mfenced", "menclose", "msub", "msup", "msubsup",
        "munder", "mover", "munderover", "mmultiscripts", "mprescripts", "none", "mtable", "mtr", "mlabeledtr",
        "mtd", "maligngroup", "malignmark", "mstack", "mlongdiv", "msgroup", "msrow", "mscarries", "mscarry",
        "msline", "maction", "semantics", "annotation", "annotation-xml",
    )
}

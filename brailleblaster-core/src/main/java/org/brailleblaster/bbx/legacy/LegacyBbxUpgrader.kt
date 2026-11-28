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
import org.brailleblaster.BBIni
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBXValidator
import org.brailleblaster.bbx.BookToBBXConverter
import org.brailleblaster.bbx.fixers.ImportFixerMap
import org.brailleblaster.bbx.fixers2.BBXTo4Upgrader
import org.brailleblaster.bbx.fixers2.BBXTo5Upgrader
import org.brailleblaster.bbx.fixers2.BBXTo6Upgrader
import org.brailleblaster.bbx.fixers2.LiveFixer
import org.brailleblaster.settings.UTDManager.Companion.loadStyleDefinitions
import org.brailleblaster.settings.UTDManager.Companion.preferredFormatStandard
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utils.xml.BB_NS
import org.slf4j.LoggerFactory

/**
 * Brings a BrailleBlaster 1.x document (BBX format 1 to 5) up to the current format.
 *
 * It runs the existing format upgrade steps in order. When a step fails because of some part of
 * the document, that part is turned into Body Text blocks (keeping its text) and the step is run
 * again, so opening the file never fails and no text is thrown away.
 *
 * This is only used for documents that [LegacyFormat.legacyBbxVersion] identifies as 1.x;
 * current documents never reach it.
 */
object LegacyBbxUpgrader {
    private val log = LoggerFactory.getLogger(LegacyBbxUpgrader::class.java)
    const val RELEASE = "1.x"

    /** Most parts of one document that will be turned into Body Text before giving up on a step. */
    private const val MAX_FALLBACKS_PER_STEP = 200

    private class Step(val toVersion: Int, val name: String, val run: (Document) -> Unit)

    private val steps: List<Step> = listOf(
        Step(3, "SPAN_WRAPPER blocks") { replaceSpanWrappers(it) },
        Step(3, "2 to 3") { doc ->
            BookToBBXConverter(
                null,
                ImportFixerMap.load(BBIni.loadAutoProgramDataFile("utd", "bbx2to3.fixerMap.xml")),
                loadStyleDefinitions(preferredFormatStandard)
            ).doUpgrade(doc, "Upgrade from 2 to 3", 3)
        },
        Step(4, "3 to 4") { BBXTo4Upgrader.upgrade(it) },
        Step(5, "4 to 5") { BBXTo5Upgrader.upgrade(it) },
        Step(6, "5 to 6") { BBXTo6Upgrader.upgrade(it) },
    )

    /**
     * Upgrade [doc] in place from [fromVersion] to [BBX.FORMAT_VERSION].
     */
    fun upgrade(doc: Document, fromVersion: Int): LegacyImportReport {
        log.info("Upgrading BrailleBlaster {} document from BBX format {}", RELEASE, fromVersion)
        val fallbacks = mutableListOf<String>()
        withoutNodeExceptionDumps {
            if (LegacyFormat.versionText(doc.rootElement) == null) {
                BBX.setFormatVersion(doc, fromVersion)
            }
            steps.filter { it.toVersion > fromVersion }.forEach { step ->
                runStep(doc, step.name, fallbacks) { step.run(it); BBX.setFormatVersion(it, step.toVersion) }
            }
            runStep(doc, "live fixer", fallbacks) { LiveFixer.fix(BBX.getRoot(it)) }
            fallbacks += replaceUnknownTypes(doc)
            BBX.setFormatVersion(doc, BBX.FORMAT_VERSION)
            reportValidation(doc)
        }
        fallbacks.forEach { log.warn("Converted to Body Text while upgrading {} document: {}", RELEASE, it) }
        return LegacyImportReport(RELEASE, fromVersion.toString(), fallbacks)
    }

    /**
     * Run [action] on [doc]. If it throws, restore [doc], find the content unit that makes it
     * fail, turn that unit into Body Text and try again.
     */
    internal fun runStep(doc: Document, name: String, fallbacks: MutableList<String>, action: (Document) -> Unit) {
        repeat(MAX_FALLBACKS_PER_STEP) {
            val before = doc.copy()
            val failure = runCatching { action(doc) }.exceptionOrNull() ?: return
            log.warn("Upgrade step '{}' failed, looking for the content that causes it", name, failure)
            doc.rootElement = before.rootElement.copy()
            val culprit = findFailingUnit(doc, action)
            if (culprit == null) {
                log.error("Upgrade step '{}' failed and could not be isolated; skipping it", name, failure)
                return
            }
            fallbacks += LegacyFallback.replaceWithBodyText(culprit)
        }
        log.error("Upgrade step '{}' still failing after {} fallbacks; skipping it", name, MAX_FALLBACKS_PER_STEP)
    }

    /**
     * Content units are the elements directly inside a `SECTION` (other than sections).
     * Binary search for the first unit that has to be removed for [action] to succeed.
     */
    private fun findFailingUnit(doc: Document, action: (Document) -> Unit): Element? {
        val unitCount = contentUnits(doc).size
        fun passesWithFirstUnitsReplaced(count: Int): Boolean {
            val trial = doc.copy()
            contentUnits(trial).take(count).forEach { LegacyFallback.replaceWithBodyText(it) }
            return runCatching { action(trial) }.isSuccess
        }
        if (unitCount == 0 || !passesWithFirstUnitsReplaced(unitCount)) {
            return null
        }
        var low = 1
        var high = unitCount
        while (low < high) {
            val mid = (low + high) / 2
            if (passesWithFirstUnitsReplaced(mid)) high = mid else low = mid + 1
        }
        return contentUnits(doc)[low - 1]
    }

    /**
     * Issue #4222 (August 2016) removed the `BLOCK SPAN_WRAPPER` type that wrapped a lone page number
     * or image span, without converting existing files. Make them plain blocks; the 3 to 4 step
     * then turns page number blocks into `BLOCK PAGE_NUM` as #4222 intended.
     */
    private fun replaceSpanWrappers(doc: Document) {
        FastXPath.descendant(doc.rootElement)
            .filterIsInstance<Element>()
            .filter { it.localName == BBX.BLOCK.name && it.getAttributeValue("type", BB_NS) == "SPAN_WRAPPER" }
            .toList()
            .forEach { block ->
                BBX._ATTRIB_TYPE.set(block, BBX.BLOCK.DEFAULT.name)
                if (BBX._ATTRIB_OVERRIDE_STYLE.has(block)) BBX._ATTRIB_OVERRIDE_STYLE.detach(block)
            }
    }

    internal fun contentUnits(doc: Document): List<Element> = FastXPath.descendant(doc.rootElement)
        .filterIsInstance<Element>()
        .filter { it.namespaceURI == BB_NS && !BBX.SECTION.isA(it) }
        .filter { BBX.SECTION.isA(it.parent) }
        .toList()

    /**
     * Elements whose type or subtype the current BBX format does not know would break translation.
     * Text containers become Body Text; wrappers are removed but keep their content.
     */
    internal fun replaceUnknownTypes(doc: Document): List<String> {
        val root = BBX.getRoot(doc)
        val unknown = FastXPath.descendant(root)
            .filterIsInstance<Element>()
            .filter { it.namespaceURI == BB_NS && !hasKnownType(it) }
            .toList()
            .asReversed() // children before parents
        return unknown.mapNotNull { elem ->
            if (elem.document == null) return@mapNotNull null // already removed with an ancestor
            when {
                BBX.SECTION.isA(elem) -> LegacyFallback.describe(elem).also { BBX._ATTRIB_TYPE.set(elem, "OTHER") }
                BBX.CONTAINER.isA(elem) || BBX.INLINE.isA(elem) || BBX.SPAN.isA(elem) || insideBlock(elem) ->
                    LegacyFallback.describe(elem).also { XMLHandler.unwrapElement(elem) }
                else -> LegacyFallback.replaceWithBodyText(elem)
            }
        }
    }

    private fun hasKnownType(elem: Element): Boolean {
        val coreType = BBX.getTypeOrNull(elem) ?: return false
        return runCatching { coreType.getSubType(elem) }.isSuccess
    }

    private fun insideBlock(elem: Element): Boolean =
        XMLHandler.ancestorVisitorElement(elem.parent ?: return false) { e: Element? -> BBX.BLOCK.isA(e) } != null

    /** The validator only runs in debug mode; problems are logged, never thrown. */
    private fun reportValidation(doc: Document) {
        runCatching { BBXValidator.validateDocument(doc, loadStyleDefinitions(preferredFormatStandard)) }
            .onFailure { log.warn("Upgraded {} document has validation warnings", RELEASE, it) }
    }

    /** Keep failed trial runs from writing node_exception files to disk. */
    private inline fun <T> withoutNodeExceptionDumps(block: () -> T): T {
        val previous = System.getProperty(NodeException.SAVE_TO_DISK_ENABLED_PROPERTY)
        System.setProperty(NodeException.SAVE_TO_DISK_ENABLED_PROPERTY, "false")
        try {
            return block()
        } finally {
            if (previous == null) System.clearProperty(NodeException.SAVE_TO_DISK_ENABLED_PROPERTY)
            else System.setProperty(NodeException.SAVE_TO_DISK_ENABLED_PROPERTY, previous)
        }
    }
}

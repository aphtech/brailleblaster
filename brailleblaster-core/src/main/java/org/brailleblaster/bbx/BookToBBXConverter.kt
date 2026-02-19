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
package org.brailleblaster.bbx

import nu.xom.*
import org.apache.commons.lang3.exception.ExceptionUtils
import org.brailleblaster.BBIni
import org.brailleblaster.Main.initBB
import org.brailleblaster.bbx.fixers.AbstractFixer
import org.brailleblaster.bbx.fixers.ImportFixerMap
import org.brailleblaster.bbx.fixers2.BBXTo4Upgrader
import org.brailleblaster.bbx.fixers2.BBXTo5Upgrader
import org.brailleblaster.bbx.fixers2.BBXTo6Upgrader
import org.brailleblaster.bbx.fixers2.LiveFixer.fix
import org.brailleblaster.bbx.parsers.ImportParser.OldDocumentAction
import org.brailleblaster.bbx.parsers.ImportParserMap
import org.brailleblaster.document.BBDocument
import org.brailleblaster.settings.UTDManager.Companion.loadStyleDefinitions
import org.brailleblaster.settings.UTDManager.Companion.preferredFormatStandard
import org.brailleblaster.utd.NamespaceMap
import org.brailleblaster.utd.config.DocumentUTDConfig
import org.brailleblaster.utd.config.StyleDefinitions
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.NormaliserFactory
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.matchers.INodeMatcher
import org.brailleblaster.utd.utils.stripUTDRecursive
import org.brailleblaster.util.LINE_BREAK
import org.brailleblaster.utils.xom.childNodes
import org.brailleblaster.util.Utils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import kotlin.system.exitProcess

/**
 * Converts books in Nimas, EPUB, etc to BBX in 4 stages:
 *
 *  1. Map elements to BBX via $sourceFormat.parserMap.xml
 *  1. Cleanup markup via bbx.fixerMap.xml
 *  1. Upgrade BBX format to newer version if needed
 *  1. Validate BBX format
 */
class BookToBBXConverter(
    private val parserMap: ImportParserMap?,
    private val fixerMap: ImportFixerMap,
    private val styleDefs: StyleDefinitions
) {
    @JvmOverloads
    fun convert(oldDoc: Document, debug: String? = ""): Document {
        val startTime = System.currentTimeMillis()
        val bbxDoc = toBBX(oldDoc)
        log.info("toBBX checkpoint {} {}", Utils.runtimeToString(startTime), debug)
        //		debugAndPause(bbxDoc, bbxDoc.getRootElement(), "test", "test");
        fix(bbxDoc)
        log.info("fix checkpoint {} {}", Utils.runtimeToString(startTime), debug)
        BBXValidator.validateDocument(bbxDoc, styleDefs)
        log.info("validate checkpoint {}", Utils.runtimeToString(startTime))
        return bbxDoc
    }

    fun doUpgrade(bbxDoc: Document, debug: String?, newVersion: Int) {
        val startTime = System.currentTimeMillis()
        fix(bbxDoc)
        log.info("fix checkpoint {} {}", Utils.runtimeToString(startTime), debug)
        BBX.setFormatVersion(bbxDoc, newVersion)
    }

    private fun toBBX(oldDoc: Document): Document {
        //Remove UTD which isn't handled in parserMap
        oldDoc.stripUTDRecursive()
        val bbxDoc = BBX.newDocument()
        toBBXRecursive(oldDoc.rootElement, bbxDoc.rootElement)
        return bbxDoc
    }

    private fun toBBXRecursive(oldCursor: Element, bbxCursor: Element) {
        for (curOldChild in oldCursor.childNodes) {
//			log.debug("Parsing old cursor: " + XMLHandler.toXMLSimple(curOldChild));
            val parser = parserMap!!.findValueOrDefault(curOldChild) ?: throw NodeException("Unhandled element", curOldChild)
            val oldAction: OldDocumentAction = try {
                parser.parseToBBX(curOldChild, bbxCursor)
            } catch (e: Exception) {
                log.error("Exception has been thrown!", e)
                if (ExceptionUtils.indexOfType(e, NodeException::class.java) == -1) {
                    throw NodeException("Builder at", bbxCursor, e)
                } else {
                    //no need to make another node exception
                    throw RuntimeException("Failed in parser $parser", e)
                }
            }
            if (oldAction === OldDocumentAction.DESCEND) {
                toBBXRecursive(
                    curOldChild as Element,
                    bbxCursor.getChild(bbxCursor.childCount - 1) as Element
                )
            }
        }
    }

    private fun fix(bbxDocument: Document) {
        /*
		TODO: This is by far the slowest stage. The cursor must be reset high 
		in the tree as some fixers handle arbitrary levels of nesting, but this
		means the same element can be checked again and again
		 */
        val documentRoot = BBX.getRoot(bbxDocument)
        for ((curMatcher, curFixer) in fixerMap) {
            if (curFixer == null) {
                continue
            }
            var lastMatchedNode: Node? = null
            val lastMatchedAncestors: MutableList<Element> = ArrayList()
            var cursor: Node? = documentRoot
            while (true) {
                val rootToSearch = cursor
                log.debug(
                    "fixer {}{} matcher {} searching from cursor {}",
                    curFixer,
                    if (curFixer is AbstractFixer) " (" + curFixer.comment + ")" else "",
                    curMatcher,
                    rootToSearch?.let { XMLHandler.toXMLSimple(it) }
                )
                log.trace("import fix.1")

                //attempt to increase performance by skipping unnesesary ForwardIterator calc in descendantAndFollowing
                var matchedNode: Node? = if (cursor != null && curMatcher.isMatch(cursor, fixerMap.namespaces)) {
                    rootToSearch
                } else {
                    matchDescendantRecursive(rootToSearch as Element, curMatcher, fixerMap.namespaces)
                }
                log.trace("Import fix.3")
                if (matchedNode == null) {
                    log.trace("Import fix.4 == null")
                    for (curNode in FastXPath.following(rootToSearch)) {
                        if (curNode !== rootToSearch && curMatcher.isMatch(curNode, fixerMap.namespaces)) {
                            matchedNode = curNode
                            if (log.isTraceEnabled) {
                                log.trace("Import fix.5 {}", matchedNode)
                            }
                            break
                        }
                    }
                }

                /*					//To debug each step of the fixers, uncomment this
					if (DEBUG_WALK_MODE) {
						debugAndPauseWithSource(XMLHandler.nodeToElementOrParentOrDocRoot(matchedNode), "cursor", "nodeToBeFixed", origDocumentRoot);
					}
					*/if (matchedNode == null) {
                    //finished, move to next fixer
                    //log.trace("no nodes matches, finished with fixer {}", curFixer);
                    break
                } else if (lastMatchedNode != null) {
                    if (matchedNode === lastMatchedNode
                        && XMLHandler
                            .ancestorOrSelf(
                                XMLHandler.nodeToElementOrParentOrDocRoot(
                                    matchedNode
                                )
                            )
                            .contains(matchedNode)
                    ) {
                        val debugFixer = if (curFixer is AbstractFixer) "\"" + curFixer.comment + "\" " else ""
                        throw NodeException(
                            "Detected infinite loop, "
                                    + "just fixed element is matched again on "
                                    + debugFixer + curFixer + " matcher " + curMatcher,
                            matchedNode
                        )
                    } else {
                        //log.trace("import fix.4: {}, {}", lastMatchedNode.toString(), matchedNode.toString());
                        lastMatchedNode = matchedNode
                        lastMatchedAncestors.clear()
                        lastMatchedAncestors.addAll(
                            XMLHandler.ancestorOrSelf(
                                XMLHandler.nodeToElementOrParentOrDocRoot(
                                    matchedNode
                                )
                            )
                        )
                    }
                }
                log.trace("fixing matched node {}",
                    XMLHandler.toXMLSimple(matchedNode)
                )

                //Note: Restart as high in the tree as possible as some matchers
                //(eg nested matchers) might need to run again on the cursors ancestor.
                //Note: Nothing should though go past the section.
                //Note: Save before fixing as matchedNode could be detached
                val cursorSection = XMLHandler.ancestorVisitorElement(
                    matchedNode
                ) { node: Element? -> BBX.SECTION.isA(node) }
                curFixer.fix(matchedNode)
                cursor = cursorSection
            }
        }

        // Remove all internal markers used for fixer communication
        FastXPath.descendant(documentRoot)
            .filterIsInstance<Element>()
            .filter { node -> BBX.FixerMarker.ATTRIB_FIXER_MARKER.has(node) }
            .forEach { curNode -> BBX.FixerMarker.ATTRIB_FIXER_MARKER.detach(curNode) }
    }

    companion object {

        /**
         * Debug Mode: Ask using console stdin (works everywhere except inside maven-surefire tests)
         * or with an SWT message box (workaround for above)
         */
        private val DEBUG_ASK_SWT_DIALOG: Boolean

        /**
         * Enable strict BBX validation, disables FallbackImportParser and may throw more exceptions
         */
        @JvmField
        val STRICT_MODE = System.getProperty("bbx.strict", "false") == "true"
        private val log = LoggerFactory.getLogger(BookToBBXConverter::class.java)
        private var TEST_CONFIG: String? = null
        private lateinit var TEST_CONVERTER: BookToBBXConverter

        init {
            //testng will set this property with the current unit test
            val unitTest = System.getProperty("test")
            DEBUG_ASK_SWT_DIALOG = !unitTest.isNullOrBlank() && unitTest.startsWith("org.brailleblaster")
        }

        @JvmStatic
        @JvmOverloads
        fun fromConfig(bookType: String = "nimas"): BookToBBXConverter {
            if (BBDocument.TEST_MODE && TEST_CONFIG == bookType) {
                return TEST_CONVERTER
            }
            val converter = BookToBBXConverter(
                ImportParserMap.load(BBIni.loadAutoProgramDataFile("utd", "$bookType.parserMap.xml")),
                ImportFixerMap.load(BBIni.loadAutoProgramDataFile("utd", "bbx.fixerMap.xml")),
                loadStyleDefinitions(preferredFormatStandard)
            )
            if (BBDocument.TEST_MODE) {
                TEST_CONFIG = bookType
                TEST_CONVERTER = converter
            }
            return converter
        }

        @JvmStatic
        fun upgradeFormat(doc: Document) {
            val formatVersion = BBX.getFormatVersion(doc)
            if (formatVersion >= BBX.FORMAT_VERSION) {
                log.info("BBX version {} is up to date", formatVersion)
            }
            val styleDefs = loadStyleDefinitions(preferredFormatStandard)
            when (formatVersion) {
                0, 1, 2 -> {
                    log.info("Upgrading BBX format from 2 to 3")
                    BookToBBXConverter(
                        null,
                        ImportFixerMap.load(BBIni.loadAutoProgramDataFile("utd", "bbx2to3.fixerMap.xml")),
                        styleDefs
                    ).doUpgrade(doc, "Upgrade from 2 to 3", 3)
                    log.info("Upgrading BBX format from 3 to 4")
                    BBXTo4Upgrader.upgrade(doc)
                    log.info("Upgrading BBX format from 4 to 5")
                    BBXTo5Upgrader.upgrade(doc)
                    log.info("Upgrading BBX format from 5 to 6")
                    BBXTo6Upgrader.upgrade(doc)
                    log.info("Running LiveFixer, final upgrade")
                    fix(BBX.getRoot(doc))
                }

                3 -> {
                    log.info("Upgrading BBX format from 3 to 4")
                    BBXTo4Upgrader.upgrade(doc)
                    log.info("Upgrading BBX format from 4 to 5")
                    BBXTo5Upgrader.upgrade(doc)
                    log.info("Upgrading BBX format from 5 to 6")
                    BBXTo6Upgrader.upgrade(doc)
                    log.info("Running LiveFixer, final upgrade")
                    fix(BBX.getRoot(doc))
                }

                4 -> {
                    log.info("Upgrading BBX format from 4 to 5")
                    BBXTo5Upgrader.upgrade(doc)
                    log.info("Upgrading BBX format from 5 to 6")
                    BBXTo6Upgrader.upgrade(doc)
                    log.info("Running LiveFixer, final upgrade")
                    fix(BBX.getRoot(doc))
                }

                5 -> {
                    log.info("Upgrading BBX format from 5 to 6")
                    BBXTo6Upgrader.upgrade(doc)
                    log.info("Running LiveFixer, final upgrade")
                    fix(BBX.getRoot(doc))
                }

                else -> {
                    log.info("Running LiveFixer, final upgrade")
                    fix(BBX.getRoot(doc))
                }
            }
            BBXValidator.validateDocument(doc, styleDefs)
        }

        private fun matchDescendantRecursive(root: Element, matcher: INodeMatcher, namespaceMap: NamespaceMap): Node? {
            log.trace("matchDescendantRecursive.1")
            for (curChild in root.childNodes) {
                if (log.isTraceEnabled) {
                    log.trace("matchDescendantRecursive.2 {}", curChild.toString())
                }
                if (matcher.isMatch(curChild, namespaceMap)) {
                    log.trace("matchDescendantRecursive.3")
                    return curChild
                } else if (curChild is Element) {
                    log.trace("matchDescendantRecursive.4")
                    val result = matchDescendantRecursive(curChild, matcher, namespaceMap)
                    if (result != null) {
                        log.trace("matchDescendantRecursive.5")
                        return result
                    }
                }
            }
            log.trace("matchDescendantRecursive.6")
            return null
        }

        @JvmStatic
        fun main(args: Array<String>) {
            val devSetupArgs = devSetup(args)
            if (devSetupArgs.size != 2) {
                System.err.println(BookToBBXConverter::class.java.getName() + " <input nimas book> <output bbx file>")
                exitProcess(1)
            }
            val doc = XMLHandler().load(File(devSetupArgs[0]))
            initBB(emptyList())
            Thread.setDefaultUncaughtExceptionHandler(null)
            val converter = fromConfig()
            val startTime = System.currentTimeMillis()
            try {
                val newDoc = converter.convert(doc)

//			XMLHandler.queryStream(newDoc, "descendant::*[@bb:origElement]").forEach(node -> ((Element) node).getAttribute("origElement", BBX.BB_NAMESPACE).detach());
                saveDocumentFormatted(newDoc, File(devSetupArgs[1]))
            } finally {
                log.error("Converted in {}", Utils.runtimeToString(startTime))
            }
        }

        private fun saveDocumentFormatted(doc: Document, out: File) {
            log.debug("Writing to " + out.absolutePath)
            try {
                //Needs to be re-normalised due to indents
                val configElement = DocumentUTDConfig.NIMAS.getConfigElement(doc, NormaliserFactory.IS_NORMALISED_KEY)
                configElement?.detach()
                val serializer = Serializer(FileOutputStream(out), "UTF-8")
                serializer.lineSeparator = LINE_BREAK
                serializer.indent = 2
                serializer.write(doc)
            } catch (e: Exception) {
                throw RuntimeException("Unable to save", e)
            }
        }

        @JvmStatic
        fun devSetup(args: Array<String>): Array<String> {
            return if (File("/home/leon/").exists()) {
                System.setProperty(NodeException.SAVE_TO_DISK_FOLDER_PROPERTY, "exception.xml")
                try {
                    File("exception.xml").createNewFile()
                } catch (e: Exception) {
                    throw RuntimeException("Could not create exception.xml file", e)
                }
                arrayOf(
                    "../linuxdev/9780133268195NIMAS_revised.xml",
                    "out-final.xml"
                )
            } else args
        }
    }
}

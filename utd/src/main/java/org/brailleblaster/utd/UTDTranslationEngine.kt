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
package org.brailleblaster.utd

import nu.xom.*
import org.brailleblaster.utd.BRFWriter.*
import org.brailleblaster.utd.actions.GenericBlockAction
import org.brailleblaster.utd.actions.IAction
import org.brailleblaster.utd.actions.IBlockAction
import org.brailleblaster.utd.config.ShortcutDefinitions
import org.brailleblaster.utd.config.StyleDefinitions
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.properties.PageNumberType.Companion.equivalentPage
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.UTDHelper
import org.brailleblaster.utils.UTD_NS
import org.brailleblaster.utils.xom.childNodes
import org.mwhapples.jlouis.Louis
import org.mwhapples.jlouis.TranslationException
import org.mwhapples.jlouis.TranslationResult
import org.slf4j.LoggerFactory
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * The default translation context implementation.
 */
open class UTDTranslationEngine(
    override val brailleTranslator: Louis,
    override var brailleSettings: BrailleSettings,
    override var pageSettings: PageSettings,
    override var actionMap: IActionMap,
    override var styleMap: IStyleMap,
    override var styleDefinitions: StyleDefinitions,
    override var shortcutDefinitions: ShortcutDefinitions
) : ITranslationEngine {


    private var _tableID = 0
    override val tableID: String
        get() {
            _tableID++
            return "utd_table_$_tableID"
        }
    override var callback: UTDTranslationEngineCallback = object : UTDTranslationEngineCallback() {
        override fun onUpdateNode(n: Node) {}
        override fun onFormatComplete(root: Node) {}
    }
    override var isTestMode = false

    constructor() : this(
        actionMap = ActionMap(),
        brailleSettings = BrailleSettings(),
        pageSettings = PageSettings(),
        brailleTranslator = LouisMetrics(),
        styleDefinitions = StyleDefinitions(),
        styleMap = StyleMap(),
        shortcutDefinitions = ShortcutDefinitions()
    )

    constructor(engine: ITranslationEngine) : this(
        actionMap = engine.actionMap,
        brailleSettings = engine.brailleSettings,
        pageSettings = engine.pageSettings,
        brailleTranslator = engine.brailleTranslator,
        styleDefinitions = engine.styleDefinitions,
        styleMap = engine.styleMap,
        shortcutDefinitions = engine.shortcutDefinitions
    )

    override fun translate(doc: Node): Nodes {
        return translate(doc, false)
    }

    fun translate(node: Node, atomic: Boolean): Nodes {
        val result = Nodes()
        if (node is Document) {
            result.append(translateDocument(node, atomic))
        } else if (node.parent == null || node.parent is Document) {
            if (node is Element) {
                log.debug("Adding utd prefix to root")
                node.addNamespaceDeclaration(UTDElements.UTD_PREFIX, UTD_NS)
            }
            val action: IAction = GenericBlockAction()
            var nodeCopy: Node? = node
            if (atomic) {
                nodeCopy = node.copy()
            }
            action.applyTo(nodeCopy!!, this)
            result.append(nodeCopy)
        } else {
            val action = actionMap.findValueOrDefault(node)
            if (action is IBlockAction) {
                var nodeCopy: Node? = node
                if (atomic) {
                    nodeCopy = node.copy()
                }
                action.applyTo(nodeCopy!!, this)
                result.append(nodeCopy)
            } else {
                throw IllegalArgumentException("Node should be a translation block. Use findTranslationBlock to find the nearest translation block.")
            }
        }
        return result
    }

    override fun format(nodes: Node): Document {
        requireNotNull(nodes.document) { "The node must be attached to a document" }
        val formatter = FormatSelector(styleMap, StyleStack(), this)
        val formattedDocument = nodes.document
        formatter.formatDocument(formattedDocument)
        callback.onFormatComplete(formattedDocument)
        return formattedDocument
    }

    @JvmOverloads
    fun partialFormat(startElement: Element?, printPageBrl: Element? = null) {
        require(!(startElement == null || !UTDElements.NEW_PAGE.isA(startElement))) { "Expected NewPage, received $startElement" }
        val formatter = FormatSelector(styleMap, StyleStack(), this)
        formatter.startPartialFormat(startElement, printPageBrl)
        var root: Node? = startElement.document
        if (root == null) {
            var parent: Node? = startElement
            while (parent != null) {
                root = parent
                parent = root.parent
            }
        }
        callback.onFormatComplete(root!!)
    }

    override fun translateDocument(doc: Document): Document {
        return translateDocument(doc, false)
    }

    fun translateDocument(doc: Document, atomic: Boolean): Document {
        var docCopy = doc
        if (atomic) {
            docCopy = doc.copy()
        }
        docCopy.rootElement.addNamespaceDeclaration(UTDElements.UTD_PREFIX, UTD_NS)
        val action: IAction = GenericBlockAction()
        action.applyTo(docCopy.rootElement, this)
        return docCopy
    }

    override fun translateAndFormatDocument(doc: Document): Document {
        return translateAndFormatDocument(doc, false)
    }

    fun translateAndFormatDocument(doc: Document, atomic: Boolean): Document {
        return this.format(this.translateDocument(doc, atomic))
    }

    /**
     * Utility to sanely translate and format an arbitrary number of elements
     *
     * @param inputElements A non empty list of elements
     * @return If given 1 element, the translation block. If many, the common parent
     */
    fun translateAndFormat(inputElements: List<Element>): Element {
        val translatedBlocks = translateAndReplace(inputElements)
        val formatRoot = XMLHandler.findCommonParent(translatedBlocks)
        try {
            format(formatRoot)
        } catch (e: Exception) {
            throw NodeException("Exception encountered during format", formatRoot, e)
        }
        return formatRoot
    }

    /**
     * Utility to sanely translate an arbitrary number of elements
     *
     * @param inputElements A non empty list of elements
     * @return The replaced translation blocks after translating
     */
    fun translateAndReplace(inputElements: List<Element>): List<Element> {
        require(inputElements.isNotEmpty()) { "Elements cannot be empty" }
        val toTranslateBlocks = findTranslationBlocks(inputElements)
        val translatedBlocks: MutableList<Element> = ArrayList()
        for (block in toTranslateBlocks) {
            //Strip out potentially old brl tags
            //Will cause bugs if, eg clients translate <p>test</p>,
            //  then add a <span> after the text node without removing the hidden <brl> tag
            //They are a UTD implementation detail and thus should be stripped out in UTD
            UTDHelper.stripUTDRecursive(block)
            val translatedBlock = translate(block)[0] as Element
            block.parent.replaceChild(block, translatedBlock)
            translatedBlocks.add(translatedBlock)
        }
        return translatedBlocks
    }

    override fun findTranslationBlock(inputNode: Node): Node {
        var node = inputNode
        var parentNode: Node? = node.parent
        while (parentNode != null && parentNode !is Document && actionMap.findValueOrDefault(node) !is IBlockAction) {
            node = node.parent
            parentNode = node.parent
        }
        return node
    }

    /**
     * Get translation blocks of input elements, removing nested translation blocks
     *
     * @param inputElements
     * @return
     */
    fun findTranslationBlocks(inputElements: List<Element>): List<Element> {
        //Clean up input which might have elements inside of elements
        val pendingElements = LinkedList(inputElements)
        val toTranslateBlocks: MutableList<Element> = ArrayList()
        while (!pendingElements.isEmpty()) {
            val curElement = pendingElements.removeFirst()
            log.trace("Current element: " + curElement.toXML())
            if (curElement.document == null) {
                throw NodeException(
                    "Node " + inputElements.indexOf(curElement) + " is not attached to document",
                    curElement
                )
            }
            val block = findTranslationBlock(curElement) as Element
            log.trace("Translation block: " + block.toXML())
            XMLHandler.childrenRecursiveVisitor(block) { curBlockChild: Element ->
                run {
                    val itr = pendingElements.iterator()
                    while (itr.hasNext()) {
                        val toTranslateElement = itr.next()
                        if (toTranslateElement === curBlockChild) {
                            log.warn(
                                "Removing already translated element {}/{} {}",
                                inputElements.indexOf(toTranslateElement),
                                inputElements.size,
                                toTranslateElement.toXML()
                            )
                            itr.remove()
                        }
                    }
                }

                //We might of already added a very nested element but now are on a higher parent
                val itr = toTranslateBlocks.iterator()
                while (itr.hasNext()) {
                    val toTranslateBlock = itr.next()
                    if (toTranslateBlock === curBlockChild) {
                        log.warn(
                            "Removing already translated element {}/{} {}",
                            inputElements.indexOf(toTranslateBlock),
                            inputElements.size,
                            toTranslateBlock.toXML()
                        )
                        itr.remove()
                    }
                }
                false
            }
            toTranslateBlocks.add(block)
        }
        return toTranslateBlocks
    }

    override fun getStyle(node: Node): IStyle? {
        val members = node.query("ancestor-or-self::*")
        var style: IStyle? = null
        for (i in members.size() - 1 downTo 0) {
            style = styleMap.findValueWithDefault(members[i], null)
            if (style != null) break
        }
        return style
    }

    /**
     * Write **translated** document to specified file
     *
     * @param utdDocument
     * @param brfOutputFile
     * @throws IOException
     */
    @JvmOverloads
    @Throws(IOException::class)
    fun toBRF(
        utdDocument: Document,
        brfOutputFile: File,
        opts: Int = OPTS_DEFAULT,
        outputPageListener: PageListener = EMPTY_PAGE_LISTENER,
        convertToBrfChars: Boolean = true
    ) {
        log.debug("Writing BRL output to " + brfOutputFile.absolutePath)
        //On windows the default encoding is windows-1252/Cp1252 which doesn't like braille
        val writer = OutputStreamWriter(
            FileOutputStream(brfOutputFile), StandardCharsets.UTF_8
        )
        BufferedWriter(writer).use { brlOutput -> toBRF(utdDocument, brlOutput, opts, outputPageListener, true, convertToBrfChars) }
    }

    @JvmOverloads
    @Throws(IOException::class)
    fun toBRF(
        utdDocument: Document,
        writer: Writer,
        opts: Int,
        outputPageListener: PageListener,
        convertLineEndings: Boolean = false,
        convertToBrfChars: Boolean = false
    ) {
        toBRF(
            utdDocument,
            if (convertLineEndings) lineEndingRewriter { c: Char -> writer.append(c) }
            else OutputCharStream { c: Char -> writer.append(c) },
            opts,
            outputPageListener,
            convertToBrfChars
        )
    }

    /**
     * Write **translated** document to arbitrary output, written to sequentially
     *
     * @param utdDocument
     * @param ocs
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun toBRF(utdDocument: Document, ocs: OutputCharStream, opts: Int, outputPageListener: PageListener, convertToBrfChars: Boolean) {
        val writer = if (convertToBrfChars) OutputCharStream { ocs.accept(it + if (it in '\u0060'..'\u007f') -0x20 else 0) } else ocs
        val grid = BRFWriter(this, writer, opts, outputPageListener)
        val cellType = brailleSettings.cellType
        UTDHelper.getDescendantBrlFast(utdDocument) { curBrl: Element ->
            log.trace("Begin brl")
            outputPageListener.onBeforeBrl(grid, curBrl)
            val printPageAttrib = curBrl.getAttribute("printPage")
            if (printPageAttrib != null) {
                //Starting a new print page
                val printPage = printPageAttrib.value
                val printPageBrl = curBrl.getAttributeValue("printPageBrl")
                //If there's no page given both the orig and brl is blank
                if (printPage.isNotBlank() && printPageBrl.isBlank()) {
                    grid.inputPageListener.onPrintPageNum(printPageBrl, printPage)
                }
            }
            for (innerNode in curBrl.childNodes) {
                try {
                    log.trace("Brl node {}", innerNode)
                    if (innerNode is Text) {
                        //If the node only contains a pilcrow, then it should not be printed on the page.
                        //This would only happen if you have an empty newPagePlaceholder on the page
                        //Which is not needed in the brf. Pilcrow translation is "~p"
                        if (!((innerNode.parent.parent as Element).getAttribute("newPagePlaceholder") != null && innerNode.value == "~p")) {
                            //If the pilcrow, for some reason, appears on the last part of the text, do not print
                            if (innerNode.value.contains("~p") && innerNode.value
                                    .lastIndexOf("~p") == innerNode.value.length - 2
                            ) {
                                grid.append(innerNode.value.split("~p".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()[0])
                            } else {
                                grid.append(innerNode.value)
                            }
                        }
                    } else if (innerNode is Element) {
                        when (innerNode.localName) {
                            "moveTo" -> {
                                val vPos = innerNode.getAttributeValue("vPos").toDouble()
                                val hPos = innerNode.getAttributeValue("hPos").toDouble()
                                grid.moveTo(
                                    cellType.getCellsForWidth(hPos.toBigDecimal()),
                                    cellType.getLinesForHeight(vPos.toBigDecimal())
                                )
                            }

                            "newPage" -> {
                                var brlnum = innerNode.getAttributeValue("brlnum")
                                grid.newPage(brlnum.toInt())
                                val pageType = equivalentPage(innerNode.getAttributeValue("brlnumtype"))
                                brlnum = pageType.getFormattedPageNumber(brlnum)
                                if ("true" == innerNode.getAttributeValue("nonsequential")) {
                                    grid.setNonsequential(true)
                                    grid.setNonsequentialBrlNum(innerNode.getAttributeValue("transbrlnum"), brlnum)
                                } else {
                                    grid.setNonsequential(false)
                                    grid.inputPageListener
                                        .onBrlPageNum(innerNode.getAttributeValue("transbrlnum"), brlnum)
                                }
                            }

                            "brlPageNum", "printPageNum" -> {
                                //Check child
                                val pageNum = innerNode.value
                                grid.append(pageNum)
                            }

                            "brlonly" -> grid.append(innerNode.getChild(0).value)
                            else -> throw RuntimeException("Unknown element in brl: $innerNode")
                        }
                    }
                } catch (cause: BRFOutputException) {
                    throw NodeException("Failed in toBRF, see cause exception", innerNode, cause)
                } catch (e: Exception) {
                    val message = "Failed in toBRF at " + innerNode.parent.toXML()
                    val cause: Exception = grid.BRFOutputException(message, e)
                    throw NodeException("Failed in toBRF, see cause exception", innerNode, cause)
                }
            }
        }
        grid.onEndOfFile()
    }

    private fun interface LouisCall<V> {
        @Throws(TranslationException::class)
        fun call(): V
    }

    class LouisMetrics : Louis() {
        var timer: Long = 0

        @Throws(TranslationException::class)
        override fun translate(
            trantab: String,
            inbuf: String,
            typeForms: ShortArray?,
            cursorPos: Int,
            mode: Int
        ): TranslationResult {
            return time { super.translate(trantab, inbuf, typeForms, cursorPos, mode) }
        }

        @Throws(TranslationException::class)
        override fun translate(trantab: String, inbuf: String, cursorPos: Int, mode: Int): TranslationResult {
            return time { super.translate(trantab, inbuf, cursorPos, mode) }
        }

        @Throws(TranslationException::class)
        override fun translateString(tablesList: String, inbuf: String, typeforms: ShortArray?, mode: Int): String {
            return time { super.translateString(tablesList, inbuf, typeforms, mode) }
        }

        @Throws(TranslationException::class)
        override fun translateString(tablesList: String, inbuf: String, mode: Int): String {
            return time { super.translateString(tablesList, inbuf, mode) }
        }

        @Throws(TranslationException::class)
        private fun <R> time(function: LouisCall<R>): R {
            val start = System.currentTimeMillis()
            val result = function.call()
            val end = System.currentTimeMillis()
            timer += end - start
            return result
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(UTDTranslationEngine::class.java)
    }
}

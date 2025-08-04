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
import org.brailleblaster.utd.exceptions.UTDInterruption
import org.brailleblaster.utd.formatters.LiteraryFormatter
import org.brailleblaster.utd.internal.PartialFormatNodeAncestor
import org.brailleblaster.utd.internal.elements.NewPage
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.tables.AutoTableFormatter
import org.brailleblaster.utd.utils.PageBuilderHelper
import org.brailleblaster.utd.utils.UTDHelper
import org.brailleblaster.utd.utils.dom.BoxUtils
import org.brailleblaster.utils.SetList
import org.brailleblaster.utils.UTD_NS
import org.brailleblaster.utils.toRepeatingLetters
import org.slf4j.LoggerFactory
import java.util.*

class FormatSelector(styleMap: IStyleMap?, styleStack: StyleStack?, engine: ITranslationEngine) {
    @JvmField
    val styleMap: IStyleMap

    @JvmField
    val styleStack: StyleStack

    @JvmField
    val engine: ITranslationEngine
    private var pageLimit: Int? = null
    private var totalPageCount: Int
    var startIndex = 0
    private var processedPages = 0

    constructor(engine: ITranslationEngine) : this(StyleMap(), StyleStack(), engine)

    /**
     * Begin the formatting process
     * @param doc
     */
    fun formatDocument(doc: Document?) {
        requireNotNull(doc) { "Expected Document, received null" }
        pageLimit = null
        val pbs = SetList<PageBuilder>()
        pbs.add(PageBuilder(engine, Cursor(), PageNumberTracker()))

        //Clean braille metadata
        cleanMetaData(doc)
        val pageBuilder = formatNode(doc, pbs)
        pageBuilder.writeUTD()
        val brls = pageBuilder.brlElementsOnPage
        for (brl in brls) {
            engine.callback.onUpdateNode(brl)
        }
    }

    private fun isNewLine(node: Node): Boolean {
        if (node is Element) {
            return UTD_NS == node.namespaceURI && "newLine" == node.localName
        }
        return false
    }

    private fun isIgnorableNode(node: Node): Boolean {
        if (pageLimit != null && processedPages > pageLimit!!) {
            return true
        }
        return if (node is Element) {
            if (!PageBuilderHelper.isSkipLinesNode(node)) {
                val action = node.getAttributeValue("utd-action")
                val action2 =
                    node.getAttributeValue("overrideAction", UTD_NS)
                if (action != null && action == "SkipAction" || action2 != null && action2 == "SkipAction") {
                    return true
                }
            }
            // NOPMD Readability
            node.getAttributeValue("class")?.contains("utd:table") == true
        } else node !is Text
    }

    /**
     * Recursively called to step through the document element by element
     * picking the correct formatter as it goes
     */
    private var currPage = 0

    init {
        require(!(styleMap == null || styleStack == null))
        this.styleMap = styleMap
        this.styleStack = styleStack
        this.engine = engine
        totalPageCount = 0
    }

    fun formatNode(node: Node?, pageBuilders: MutableSet<PageBuilder>): PageBuilder {
        var pageBuilders = pageBuilders
        if (Thread.currentThread().isInterrupted) {
            log.warn("Abort current thread")
            throw UTDInterruption()
        }
        log.debug(" formatNode called for {}", node)
        requireNotNull(node) { "Expected node, received null" }
        if (node is Document) {
            return formatNode(node.rootElement, pageBuilders)
        }
        if (isNewLine(node)) {
            return handleNewLine(pageBuilders)
        }
        if (isIgnorableNode(node)) {
            log.debug("Ignoring node")
            return writePagesUTD(pageBuilders)
        }
        var pageBuilder = pageBuilders.last()
        if (currPage != pageBuilder.braillePageNumber.pageNumber) {
            if (pageBuilder.braillePageNumber.pageNumber == 1) {
                totalPageCount++
            } else {
                totalPageCount += pageBuilder.braillePageNumber.pageNumber - currPage
            }
            currPage = pageBuilder.braillePageNumber.pageNumber
        }
        val nodeStyle = styleMap.findValueOrDefault(node)
        styleStack.push(nodeStyle)
        addStyleAttributes(node, nodeStyle)
        if (node is Element) {
            BoxUtils.stripBoxBrl(node as Element?)
        }
        if (enableWriteUTD) {
            // flush the pageBuilders
            val pb = writePagesUTD(pageBuilders)
            // do we need to explicitly clear pageBuilders, does writePagesUTD do this?
            pageBuilders.clear()
            pageBuilders.add(pb)
        }
        pageBuilders = if (PageBuilderHelper.isSkipLinesNode(node)) {
            PageBuilderHelper.applySkipLinesNode(node as Element?, styleStack, pageBuilders, this)
        } else if (styleStack.format != null) {
            if (log.isDebugEnabled) {
                log.debug("About to format node with {} formatter", styleStack.format.name)
            }
            if (styleStack.format == IStyle.Format.SIMPLE) {
                //Double check table formatting
                val atf = AutoTableFormatter()
                atf.format(node, styleStack, pageBuilders, this)
            }
            else {
                styleStack.format.formatter.format(node, styleStack, pageBuilders, this)
            }
        } else {
            log.debug("About to format node with default formatter")
            IStyle.Format.NORMAL.formatter.format(node, styleStack, pageBuilders, this)
        }
        if (enableWriteUTD) {
            pageBuilder = writePagesUTD(pageBuilders)
        }
        styleStack.pop()
        if (log.isDebugEnabled) {
            log.debug(
                "Finished Processing node {} which reaches to Braille page {} on print page {}",
                node,
                pageBuilder.braillePageNumber.pageNumber,
                pageBuilder.printPageNumber
            )
        }
        return pageBuilder
    }

    private fun handleNewLine(pageBuilders: Set<PageBuilder>): PageBuilder {
        var curPage = writePagesUTD(pageBuilders)
        val pages: Set<PageBuilder> = curPage.insertNewLineOverride()
        curPage = writePagesUTD(pages)
        return curPage
    }

    fun startPartialFormat(node: Node, printPageBrl: Element?) {
        // Find the nearest new page where partial formatting may begin.
        val earlierNewPages = node.query("preceding::utd:newPage", UTDElements.UTD_XPATH_CONTEXT)
        if (UTDElements.NEW_PAGE.isA(node)) {
            if (earlierNewPages.size() > 0) //An empty Nodes list is considered a Collections.emptyList and throws an UnsupportedOperationException when append is used
                earlierNewPages.append(node)
        } else {
            // If the first brl related to the node starts with a newPage then that is also a candidate.
            val relatedBrls = UTDHelper.getBrlElements(node)
            if (relatedBrls.size() > 0) {
                val firstBrl = relatedBrls[0]
                if (firstBrl.childCount > 0) {
                    val firstChild = firstBrl.getChild(0)
                    if (UTDElements.NEW_PAGE.isA(firstChild)) {
                        earlierNewPages.append(firstChild)
                    }
                }
            }
        }
        // Find the last newPage where we can start from the candidates.
        var startPoint: Node? = null
        val pathToStart: Deque<PartialFormatNodeAncestor> = LinkedList()
        var startOfLine = false
        var i = earlierNewPages.size()
        while (startPoint == null && i > 0) {
            i--
            pathToStart.clear()
            var startPointStartsBrl = false
            var tmpSP = earlierNewPages[i]
            val tmpBrl = tmpSP!!.parent as Element
            if (tmpBrl.indexOf(tmpSP) == 0) {
                startPointStartsBrl = true
            }
            var tmpNode: Node? = UTDHelper.getAssociatedNode(tmpBrl) ?: continue
            while (tmpNode != null) {
                val tmpStyle = styleMap.findValueOrDefault(tmpNode)
                val tmpFormat = tmpStyle.format
                //Styles here can be different formatting styles, not just NORMAL
                if (tmpFormat == null || tmpFormat == IStyle.Format.NORMAL) {
                    // When the newPage is not first Braille content of the child it will not be for the parent element either, so no need to check again.
                    if (startPointStartsBrl) {
                        // Check whether any other BRLs come before the start point.
                        val firstBrl = UTDHelper.getDescendantBrlFastFirst(tmpNode)
                        // firstBrl == null means that tmpBrl is the only Braille content
                        if (firstBrl != null && tmpBrl != firstBrl) {
                            startPointStartsBrl = false
                        } else if (!startOfLine && tmpStyle.linesBefore > 0 && firstBrl != null && tmpBrl == firstBrl) {
                            startOfLine = true
                        }
                    }
                    pathToStart.addFirst(
                        PartialFormatNodeAncestor(
                            tmpNode,
                            tmpStyle,
                            startOfLine,
                            startPointStartsBrl
                        )
                    )
                    tmpNode = tmpNode.parent
                } else {
                    tmpSP = null
                    tmpNode = null
                }
            }
            startPoint = tmpSP
        }
        // If there is no newPage elements which are candidates then format the whole document
        if (startPoint == null) {
            formatDocument(node.document)
            return
        }
        // Construct the PageNumberTracker with the page information
        // TODO: May be we could combine this with the searching for the start point to make it more efficient.
        var newPage = NewPage(earlierNewPages[0] as Element)
        totalPageCount = earlierNewPages.size()
        var pageNumber =
            PageNumberTracker(newPage.pageNumber, newPage.pageNumberType, engine.pageSettings.isContinuePages)
        var curType = newPage.pageNumberType
        i = 1
        while (i < earlierNewPages.size()) {
            val child = earlierNewPages[i]
            newPage = NewPage(child as Element)
            if (curType !== newPage.pageNumberType) {
                curType = newPage.pageNumberType
                pageNumber.setPageNumberType(curType, engine.pageSettings.isContinuePages)
            } else {
                if (newPage.pageNumber != pageNumber.pageNumber) { //Fix edge case where newPage elements are duplicated for blank page before page 1
                    //Page numbers don't always go forward even if they're the same number type
                    if (newPage.pageNumber == 1) {
                        pageNumber.resetNumberCounters(newPage.pageNumberType, engine.pageSettings.isContinuePages)
                    } else {
                        pageNumber = pageNumber.nextPage(newPage.pageNumberType, engine.pageSettings.isContinuePages)
                    }
                }
            }
            if (child == startPoint) {
                break
            }
            i++
        }

        var pageBuilder = PageBuilder(engine, Cursor(), pageNumber)
        val pageBuilders = SetList<PageBuilder>()
        pageBuilders.add(pageBuilder)
        if (printPageBrl != null) {
            val newContLetter = printPageBrl.value.replace(printPageBrl.getAttributeValue("printPageBrl"), "")
            if (newContLetter.isNotEmpty()){
                pageBuilder.setPrintPageBrl(printPageBrl.getAttributeValue("printPageBrl"), newContLetter.toRepeatingLetters())
            } else pageBuilder.setPrintPageBrl(printPageBrl.getAttributeValue("printPageBrl"))
            pageBuilder.printPageNumber = printPageBrl.getAttributeValue("printPage")
        }
        pageBuilder.setStartOfBlock(startOfLine)
        // Start formatting
        pageBuilder = partialFormat(pathToStart, startPoint, pageBuilders)
        if (!pageBuilder.hasWrittenUTD()) {
            pageBuilder.writeUTD()
            val brls = pageBuilder.brlElementsOnPage
            for (brl in brls) {
                engine.callback.onUpdateNode(brl)
            }
        }
    }

    /**
     * Partial format version of formatNode.
     *
     * It is not expected that this method will be called by clients, it should only be called by other methods within this class or formatters.
     * It is made public due to formatters residing in other packages.
     *
     * @param pathToStart A collection of the ancestors leading to the start node.
     * @param startPoint The Braille node (IE. the child node of the brl element) where formatting should begin.
     * @return The last page's PageBuilder.
     */
    fun partialFormat(
        pathToStart: Deque<PartialFormatNodeAncestor>,
        startPoint: Node,
        pageBuilders: MutableSet<PageBuilder>
    ): PageBuilder {
        val pathElement = pathToStart.first
        val node = pathElement.node
        val style = pathElement.style
        styleStack.push(style)
        addStyleAttributes(node, style)
        // When starting part way through the Braille content do not remove any start separator
        if (node is Element) {
            if (pathElement.isFirstBrailleAtStartPoint) {
                BoxUtils.stripBoxBrl(node)
            } else {
                val endSeparator = BoxUtils.getEndSeparator(node)
                endSeparator?.detach()
            }
        }
        // partial formatting always uses the literary formatter in partial format mode.
        // pageBuilders = ((LiteraryFormatter)Format.NORMAL.getFormatter()).partialFormat(pathToStart, startPoint, styleStack, pageBuilders, this);
        pageBuilders.addAll(
            (IStyle.Format.NORMAL.formatter as LiteraryFormatter).partialFormat(
                pathToStart,
                startPoint,
                styleStack,
                pageBuilders,
                this
            )
        )
        return writePagesUTD(pageBuilders).also {
            styleStack.pop()
        }
    }

    private fun writePagesUTD(results: Set<PageBuilder>): PageBuilder {
        var pageBuilder: PageBuilder? = null
        val iter = results.iterator()
        while (iter.hasNext()) {
            val iterPB = iter.next()
            if (!iter.hasNext()) {
                // Set current page as last and skip writeUTD on current page.
                pageBuilder = iterPB
                break
            }
            if (!iterPB.hasWrittenUTD()) {
                processedPages += 1
                iterPB.writeUTD()
            }
        }
        return pageBuilder!!
    }

    private fun addStyleAttributes(n: Node?, style: IStyle) {
        if (n is Element) {
            val styleAttribute = Attribute(
                UTDElements.UTD_STYLE_ATTRIB,
                style.name
            )
            n.addAttribute(styleAttribute)
        }
    }

    val currentStyle: IStyle
        get() = styleStack

    fun increaseTotalPageCount(count: Int) {
        totalPageCount += count
    }

    private fun cleanMetaData(doc: Document) {
        val metas = MetadataHelper.getUTDMeta(doc)
        for (meta in metas) {
            if (meta.getAttribute("used") != null) {
                meta.removeAttribute(meta.getAttribute("used"))
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(FormatSelector::class.java)
        private var enableWriteUTD = true

   /*
	 * Set whether UTD should "flush" the set of page builders in between brl additions.
	 * When a page needs to be thrown out and remade, set this as false to keep UTD from
	 * adding formatting to the xml.
	 */
		@JvmStatic
		fun setEnableWriteUTD(enableWriteUTD: Boolean) {
            Companion.enableWriteUTD = enableWriteUTD
        }
    }
}

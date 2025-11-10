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
package org.brailleblaster.utd.utils

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.utd.FormatSelector
import org.brailleblaster.utd.IStyle
import org.brailleblaster.utd.PageBuilder
import org.brailleblaster.utd.PageSettings
import org.brailleblaster.utd.formatters.SkipLinesFormatter
import org.brailleblaster.utd.properties.PageNumberPosition
import org.brailleblaster.utd.properties.PageNumberType
import org.brailleblaster.utd.properties.PageNumberType.Companion.equivalentPage
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utils.xml.UTD_NS
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object PageBuilderHelper {
    private val log: Logger = LoggerFactory.getLogger(PageBuilderHelper::class.java)

    /**
     * Checks the PageBuilder to see if there is content on its current y
     * position, and if so, adds another line
     */
    fun forceNewLineIfNotEmpty(pageBuilder: PageBuilder): Set<PageBuilder> {
        var pb = pageBuilder
        val pages: MutableSet<PageBuilder> = LinkedHashSet()
        pages.add(pb)
        while (if (pb.isNumberLine) !pb.isEmptyNumberLine else !pb.isEmptyLine) {
            pb.setForceSpacing(true)
            //This uses setLinesAfter because the new lines are not needed if you get pushed on to a new page
            pb.addAtLeastLinesAfter(1)
            pages.addAll(pb.processSpacing())
            pb = pages.last()
        }
        return pages
    }

    @JvmStatic
    fun isPageIndicator(brl: Element): Boolean {
        require(UTDElements.BRL.isA(brl)) { "Expected brl, received $brl" }

        return brl.getAttributeValue("printPageBrl") != null && brl.getAttributeValue("printPage") != null
    }

    /**
     * Checks to see if you have space to insert a print page indicator on the page.
     * If not, move on to the next page.
     */
    fun verifyPageIndicator(pb: PageBuilder): PageBuilder? {
        val curY = pb.y
        var pendingLines = pb.pendingLinesBefore
        if (curY >= pb.linesPerPage) {
            return pb
        }

        //If the line is not empty, a new line will be created
        if (!pb.isEmptyLine && pendingLines == 0) pendingLines++
        val pendingPages = pb.pendingPages
        val maxLines = pb.totalHeight - 1

        //Check if the current amount of pending lines is enough to make a new page
        if (curY + pendingLines >= maxLines) {
            //Add enough lines to create a new page
            pb.addAtLeastPages(1)
            return pb
        }
        return if (curY + pendingLines < maxLines
            && (pendingPages <= 0
                    || curY + pendingLines != pb.lineSpacing)
        ) null else pb
    }


    /**
     * Sets the page number type from the brl of a pagenum element
     */
    fun changePageNumberType(pageBuilder: PageBuilder, brl: Element, style: IStyle, formatSelector: FormatSelector) {
        // The default page to be considered is the value of the pageType attribute.
        // If there is a style applied to the element, use the style.
        if (!pageBuilder.isOverridePageType() || pageBuilder.pageNumberTypeFromMetaData != null) {
            var pageType = brl.getAttributeValue("pageType")
            if (pageBuilder.pageNumberTypeFromMetaData != null) {
                pageType = pageBuilder.pageNumberTypeFromMetaData!!.name
            }
            if (pageType == null) {
                pageType = "NORMAL"
            }

            if ((pageBuilder.pageNumberType != equivalentPage(pageType)
                        && style.braillePageNumberFormat == null) //you're just after a volume change
                || pageBuilder.isAfterVolume
            ) {
                setPageNumberType(pageBuilder, equivalentPage(pageType), formatSelector)
            } else if (pageBuilder.isAfterTPage()) {
                pageBuilder.pageNumberType = equivalentPage(pageType)
            }
        }
    }

    fun setPageNumberType(
        pageBuilder: PageBuilder,
        pageType: PageNumberType,
        isContinuePages: Boolean,
        isInterpoint: Boolean
    ) {
        val isNotBlank =
            !pageBuilder.isBlankPageWithPageNumbers || !pageBuilder.isBlankPage || !pageBuilder.isBlankPageWithRunningHead

        if (!isContinuePages) {
            if (pageBuilder.isAfterTPage()) {
                pageBuilder.braillePageNumber.resetNumberCounters(pageType, false)
            } else {
                pageBuilder.braillePageNumber.resetNextPageNumberCounters(pageType)
            }
        }

        if (isInterpoint && !pageBuilder.isFirstPage) {
            if (pageBuilder.isRightPage) {
                var toAdd = 2
                if (pageBuilder.isBlankPageWithPageNumbers || pageBuilder.isBlankPageWithSkipLine
                    || pageBuilder.isAfterTPage()
                ) {
                    /*
                     * If you only have page numbers and/or a running head on your current page,
                     * then it's a page that you don't need as you switch page number types.
                     * Remove the page numbers and reduce the number of pages that you need to add.
                     */
                    pageBuilder.removeExistingPageNumbers()
                    toAdd = 0
                } else {
                    //Decrement continuation letter because you've added an unnecessary page
                    pageBuilder.setDecrementCont(true)
                }

                pageBuilder.addAtLeastPages(toAdd)
            } else {
//					if (isNotBlank || !pageBuilder.isAfterTPage()) {
                pageBuilder.addAtLeastPages(1)
                //					}
//					else {
//						pageBuilder.setPageNumberType(pageType);
//						return;
//					}
            }
        } else {
            if (isNotBlank && !pageBuilder.isAfterTPage()) {
                pageBuilder.addAtLeastPages(1)
            } else {
                pageBuilder.pageNumberType = pageType
                return
            }
        }

        pageBuilder.setNextPageNumberType(pageType)
    }


    /**
     * Set the page number type directly. If you're using a pagenum element, use changePageNumberType instead
     */
    @JvmStatic
    fun setPageNumberType(pageBuilder: PageBuilder, pageType: PageNumberType, formatSelector: FormatSelector) {
        //You need to double check here if you are indeed using the correct braille page type for this brl
        if (pageBuilder.pageNumberTypeFromMetaData != null) {
            setPageNumberType(
                pageBuilder,
                pageBuilder.pageNumberTypeFromMetaData!!,
                formatSelector.engine.pageSettings.isContinuePages,
                formatSelector.engine.pageSettings.interpoint
            )
        } else {
            setPageNumberType(
                pageBuilder,
                pageType,
                formatSelector.engine.pageSettings.isContinuePages,
                formatSelector.engine.pageSettings.interpoint
            )
        }
    }

    /**
     * First verifies if a page indicator can be inserted, and if so, makes a new line if the current line is not
     * empty and inserts a page indicator
     */
    @JvmStatic
    fun handlePageIndicator(
        pb: PageBuilder,
        brl: Element,
        style: IStyle,
        formatSelector: FormatSelector
    ): Set<PageBuilder> {
        var pageBuilder = pb
        val results: MutableSet<PageBuilder> = LinkedHashSet()
        results.add(pageBuilder)

        var addIndicator = true
        //Update the page number only if it's not the same as the one you currently have
        if (pageBuilder.printPageNumber != brl.getAttributeValue("printPage")) {
            pageBuilder.printPageNumber = brl.getAttributeValue("printPage")
            pageBuilder.setPrintPageBrl(brl.getAttributeValue("printPageBrl"))
        } else if (brl.getAttributeValue("printPage").isNotEmpty()) {
            addIndicator = false
        }
        if (brl.getAttribute("printPageOverride") != null) {
            pageBuilder.printPageNumberOverride = brl.getAttributeValue("printPageOverride")
            pageBuilder.setPrintPageBrlOverride(brl.getAttributeValue("printPageOverrideBrl"))
        } else {
            pageBuilder.printPageNumberOverride = ""
            pageBuilder.setPrintPageBrlOverride("")
        }
        // The default page to be considered is the value of the pageType attribute.
        // If there is a style applied to the element, use the style.
        changePageNumberType(pageBuilder, brl, style, formatSelector)

        if (addIndicator) {
            val indicatorPB = verifyPageIndicator(pageBuilder)
            if (indicatorPB == null) {
                //Do this only if you don't have a blank print page
                if (!(pageBuilder.printPageNumber.isEmpty() || pageBuilder.printPageValue.isEmpty())) {
                    //You should not insert a print page indicator if it's the first thing on the page
                    var isEmpty = true
                    var i = 1
                    while (i <= pageBuilder.y) {
                        if (!pageBuilder.isEmptyLine(i)) {
                            isEmpty = false
                            break
                        }
                        i++
                    }
                    isEmpty = if (pageBuilder.hasRunningHead()) (isEmpty) else pageBuilder.isEmptyNumberLine(0)
                    if (isEmpty && pageBuilder.hasAddedPageNumbers()) {
                        //If it is the first thing on the page, you need to update your print page number
                        pageBuilder.decrementContinuationLetter()
                        pageBuilder.addPageNumbers()
                        addIndicator = false
                    }
                }

                var insertedElements = 0
                if (addIndicator) {
                    /*There is no need for pending lines before the indicator.
                     * This happens because the indicator was added as a span
                     * inside a block with a style that has pending lines.
                     * Save the style information it currently has, reset,
                     * and add them after handling the print page indicator.
                     * Is there another way to do it besides this? */

                    val linesBefore = pageBuilder.pendingLinesBefore
                    val linesAfter = pageBuilder.pendingLinesAfter
                    val indent = pageBuilder.leftIndent
                    val firstLineIndent = pageBuilder.firstLineIndent
                    pageBuilder.resetPendingLines()

                    results.addAll(forceNewLineIfNotEmpty(pageBuilder))
                    pageBuilder = results.last()
                    insertedElements = pageBuilder.insertPrintPageIndicator(brl)

                    pageBuilder.addAtLeastLinesBefore(linesBefore)
                    pageBuilder.addAtLeastLinesAfter(linesAfter)
                    pageBuilder.setLeftIndent(indent)
                    pageBuilder.setFirstLineIndent(firstLineIndent)
                }
                if (brl.childCount > insertedElements) {
                    results.addAll(pageBuilder.addBrlFromChild(brl, insertedElements))
                }
            } else {
                pageBuilder = indicatorPB
                results.addAll(pageBuilder.processSpacing())
                for (child in brl.childCount - 1 downTo 0) {
                    brl.getChild(child).detach()
                }
            }
        } else {
            for (child in brl.childCount - 1 downTo 0) {
                brl.getChild(child).detach()
            }
        }

        pageBuilder = results.last()
        pageBuilder.clearKeepWithNext() //The page indicator should be considered what the heading is keeping with
        return results
    }

    @JvmStatic
    fun verifyPageSide(pageBuilder: PageBuilder, side: String) {
        //Check what side of the document you're on
        if (side == "right") {
            if (!pageBuilder.isRightPage) {
                pageBuilder.addAtLeastPages(1)
            }
        } else if (side == "left" && pageBuilder.isRightPage) {
            pageBuilder.addAtLeastPages(1)
        }
    }

    @JvmStatic
    fun isSkipLinesNode(node: Node?): Boolean {
        return node is Element && node.getAttributeValue(
            UTDElements.UTD_SKIP_LINES_ATTRIB,
            UTD_NS
        ) != null
    }

    fun applySkipLinesNode(
        element: Element?,
        style: IStyle?,
        pageBuilders: MutableSet<PageBuilder>,
        formatter: FormatSelector?
    ): MutableSet<PageBuilder> {
        log.debug("About to format node with skipLines formatter")
        return SkipLinesFormatter().format(element!!, style!!, pageBuilders, formatter!!)
    }

    fun getPageProperty(braillePageNumber: Int): Property {
        if (braillePageNumber % 2 == 0) {
            return Property.EVEN
        }

        return Property.ODD
    }

    @JvmStatic
    fun getPrintPageNumberAt(pageSettings: PageSettings, braillePageNumber: Int): PageNumberPosition {
        if (getPageProperty(braillePageNumber) == Property.EVEN) {
            return pageSettings.evenPrintPageNumberAt
        }
        return pageSettings.oddPrintPageNumberAt
    }

    @JvmStatic
    fun getBraillePageNumberAt(pageSettings: PageSettings, braillePageNumber: Int): PageNumberPosition {
        if (getPageProperty(braillePageNumber) == Property.EVEN) {
            return pageSettings.evenBraillePageNumberAt
        }
        return pageSettings.oddBraillePageNumberAt
    }

    fun isDecimal(input: Char): Boolean {
        return when (input) {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', ';', '#' -> true
            else -> false
        }
    }

    enum class Property {
        EVEN, ODD
    }
}

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
package org.brailleblaster.utd.formatters

import nu.xom.Attribute
import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.libembosser.spi.BrlCell
import org.brailleblaster.utd.*
import org.brailleblaster.utd.FormatSelector.Companion.setEnableWriteUTD
import org.brailleblaster.utd.properties.Align
import org.brailleblaster.utd.properties.PageNumberPosition
import org.brailleblaster.utd.properties.PageNumberType
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.PageBuilderHelper.getBraillePageNumberAt
import org.brailleblaster.utd.utils.PageBuilderHelper.getPrintPageNumberAt
import org.brailleblaster.utd.utils.PageBuilderHelper.setPageNumberType
import org.brailleblaster.utd.utils.containsBrl
import org.brailleblaster.utd.utils.getDescendantBrlFast
import org.brailleblaster.utils.xml.UTD_NS
import java.util.*
import kotlin.math.max

class TPageFormatter : LiteraryFormatter() {
    private var engine: ITranslationEngine? = null
    private var cellType: BrlCell? = null
    private var formatSelector: FormatSelector? = null
    private var style: IStyle? = null
    override fun format(
        node: Node, style: IStyle, pageBuilders: Set<PageBuilder>,
        formatSelector: FormatSelector
    ): MutableSet<PageBuilder> {
        var mutPageBuilders = pageBuilders.toMutableSet()
        var pageBuilder = mutPageBuilders.last()

        if (node !is Element || node.childElements.size() == 0) return mutPageBuilders

        pageBuilder.setCurrBrl(node)

        val origPageType = pageBuilder.pageNumberType
        val newPageType =
            if (pageBuilder.pageNumberTypeFromMetaData != null) pageBuilder.pageNumberTypeFromMetaData else PageNumberType.T_PAGE
        if (pageBuilder.pageNumberType != newPageType) {
            setPageNumberType(pageBuilder, newPageType!!, formatSelector)
        }
        mutPageBuilders.addAll(pageBuilder.processSpacing())
        pageBuilder = mutPageBuilders.last()

        val startOfBlockState = pageBuilder.isStartOfBlock()
        pageBuilder.setStartOfBlock(true)
        engine = formatSelector.engine
        cellType = engine!!.brailleSettings.cellType
        this.formatSelector = formatSelector
        this.style = style

        val styleMap = formatSelector.styleMap
        mutPageBuilders.addAll(preFormat(node, pageBuilder, style, styleMap))
        val titlePage: Element? = node.childElements[0]
        val secondaryPages: MutableList<Element> = LinkedList()
        for (i in 1 until node.childElements.size()) {
            if (node.childElements[i].containsBrl()) secondaryPages.add(node.childElements[i])
        }

        pageBuilder = mutPageBuilders.last()
        pageBuilder.y = 0
        var insertPages = false
        if (titlePage != null && titlePage.childCount > 0) {
            insertPages = true
            val centered =
                CENTERED_ATTR.value == titlePage.getAttributeValue(CENTERED_ATTR.localName, UTD_NS)
            if (centered) {
                //LiteraryFormatter uses the StyleStack to set PageBuilder's alignment, so we can't just use pageBuilder.setAlign
                //Push a centered style onto the StyleStack instead
                val centeredStyle = Style()
                centeredStyle.setAlign(Align.CENTERED)
                if (style is StyleStack) {
                    style.push(centeredStyle)
                }
            }
            mutPageBuilders = processTitlePage(titlePage, mutPageBuilders, 0, 0, -1, false)
            pageBuilder = mutPageBuilders.last()
            if (centered && style is StyleStack) {
                style.pop()
            }
        }
        pageBuilder = mutPageBuilders.last()
        for (page in secondaryPages) {
            if (insertPages) {
                //Don't need to always add a page before secondary pages if you have a blank title page
                //(e.g. only adding special symbols page)
                pageBuilder.addAtLeastPages(1)
                mutPageBuilders.addAll(pageBuilder.processSpacing())
            }
            mutPageBuilders.addAll(super.format(page, style, mutPageBuilders, formatSelector))
            pageBuilder = mutPageBuilders.last()
        }
        pageBuilder.addAtLeastPages(1)
        mutPageBuilders.addAll(postFormat(node, pageBuilder, style, styleMap))
        mutPageBuilders.addAll(mutPageBuilders.last().processSpacing())
        val lastPB = mutPageBuilders.last()
        lastPB.setStartOfBlock(startOfBlockState)
        lastPB.setAddedPageNumbers(false)
        lastPB.setAfterTPage(true)
        //		lastPB.setPageNumberType(origPageType);
        setPageNumberType(lastPB, origPageType, formatSelector)
        //		lastPB.setPageNumberType(PageNumberType.UNSPECIFIED);
        return mutPageBuilders
    }

    /**
     * Spaces must be placed between the sections of the title page so that there is text on
     * the first and final lines of the page. We recursively add spaces one by one until
     * this requirement is met
     */
    private fun processTitlePage(
        element: Element, pageBuilders: MutableSet<PageBuilder>, extraLines: Int,
        remainder: Int, numOfSections: Int, splitLastLine: Boolean
    ): MutableSet<PageBuilder> {
        var pageBuilders = pageBuilders
        var extraLines = extraLines
        var remainder = remainder
        var pageBuilder = pageBuilders.last()
        val curPages = pageBuilders.size
        var divCount = numOfSections //Number of sections we need to account for
        var firstPass = false //If we're doing our first pass, we need to count the number of sections
        val startRemainder = numOfSections - remainder
        var curRemainder = remainder
        if (divCount == -1) {
            divCount = 0
            firstPass = true
        }

        //Iterate through the sections
        for (i in 0 until element.childElements.size()) {
            if (element.childElements[i].containsBrl()) {
                if (i != 0) {
                    if (firstPass) {
                        divCount++
                        pageBuilder.addAtLeastLinesBefore(1)
                    } else {
                        //Determine if a blank line needs to be added
                        //startRemainder ensures that remaining lines get added towards the
                        //bottom sections of the page
                        val applyRemainder = curRemainder > 0 && i >= startRemainder
                        //Add the amount of existing blank lines from previous runs and an additional
                        //line if there is a remainder
                        pageBuilder.addAtLeastLinesBefore(1 + extraLines + (if (applyRemainder) 1 else 0))
                        //Decrease the remainder
                        if (applyRemainder) curRemainder = max((curRemainder - 1).toDouble(), 0.0).toInt()
                    }
                }
                val curSection = element.childElements[i]
                for (block in 0 until curSection.childElements.size()) {
                    //Format the section using the LiteraryFormatter

                    //Keep UTD from writing formatting to the document because we may be throwing out a page

                    setEnableWriteUTD(false)
                    (style as StyleStack?)!!.push(formatSelector!!.styleMap.findValueOrDefault(curSection.childElements[block]))
                    val onLastBlock =
                        i == element.childElements.size() - 1 && block == curSection.childElements.size() - 1
                    if (splitLastLine && onLastBlock) {
                        //The last line needs to be line wrapped to satisfy Braille Formats, so change the right margin to be
                        //the length of the page number and its padding (see below edge case)
                        val braillePosition = getBraillePageNumberAt(
                            formatSelector!!.engine.pageSettings, pageBuilder.braillePageNumber.pageNumber
                        )
                        val printPosition = getPrintPageNumberAt(
                            formatSelector!!.engine.pageSettings, pageBuilder.braillePageNumber.pageNumber
                        )
                        var pageNumberSize = 0
                        if (pageBuilder.braillePageNum.isNotEmpty() && (braillePosition == PageNumberPosition.BOTTOM_LEFT || braillePosition == PageNumberPosition.BOTTOM_RIGHT)) {
                            pageNumberSize += pageBuilder.braillePageNum.length + pageBuilder.padding
                        }
                        if (pageBuilder.printPageNumber.isNotEmpty() && (printPosition == PageNumberPosition.BOTTOM_LEFT || printPosition == PageNumberPosition.BOTTOM_RIGHT)) {
                            pageNumberSize += pageBuilder.printPageNumber.length + pageBuilder.padding
                        }
                        val lineLengthStyle = Style()
                        lineLengthStyle.lineLength = -1 * pageNumberSize
                        (style as StyleStack?)!!.push(lineLengthStyle)
                    }

                    //Add block to page
                    pageBuilder.setStartOfBlock(true)
                    pageBuilders =
                        super.format(curSection.childElements[block], style!!, pageBuilders, formatSelector!!)
                    (style as StyleStack?)!!.pop()

                    if (splitLastLine && onLastBlock) {
                        (style as StyleStack?)!!.pop()
                    }

                    //Renable formatting to be written to document
                    setEnableWriteUTD(true)
                    pageBuilder = pageBuilders.last()
                }
            }
        }
        if (divCount == 0) {
            return pageBuilders
        }

        if (pageBuilders.size > curPages) {
            //Text has runover to another page
            if (extraLines == 0 && remainder == 0) {
                //There is too much text to fit on the page, so abort
                return pageBuilders
            }

            //The last line of the tpage was short enough to fit on a line but was forced to line-wrap due to the page number.
            //Re-add the tpage with one less extra line so that the final line lands on the second-to-last line of the page.
            if (remainder > 0) {
                remainder--
            } else {
                extraLines--
                remainder = divCount - 1
            }

            //Remove all added brl
            removeBrlFromPB(pageBuilders, element)

            //Remove the extra page from the pagebuilders set
            pageBuilders.remove(pageBuilders.last())
            pageBuilder = pageBuilders.last()
            pageBuilder.y = 0
            pageBuilder.x = 0

            //Recurse, setting the splitLastLine flag to true
            return processTitlePage(element, pageBuilders, extraLines, remainder, divCount, true)
        }

        val endingY = pageBuilder.y //Last line of text
        val finalLine = cellType!!.getLinesForHeight(engine!!.pageSettings.drawableHeight.toBigDecimal()) //Last line of page
        //If the last line of text is not on the last line of the page and we have expended all of our remainder
        if (firstPass && endingY != finalLine && extraLines == 0 && curRemainder == 0) {
            val diff = finalLine - endingY - 1
            if (diff == 0) return pageBuilders
            //Remove everything we have added
            removeBrlFromPB(pageBuilders, element)
            //Reset the cursor to the beginning
            pageBuilder.y = 0
            pageBuilder.x = 0
            //Restart the process, distributing one extraLine to each section and setting the remainder to be
            //the leftover extra lines
            pageBuilders = processTitlePage(element, pageBuilders, diff / divCount, diff % divCount, divCount, false)
        }
        return pageBuilders
    }

    private fun removeBrlFromPB(set: Set<PageBuilder>, parent: Element) {
        val brls = parent.getDescendantBrlFast()
        for (brl in brls) {
            for (pb in set) {
                pb.removeBrl(brl)
            }
        }
    }

    companion object {
        val CENTERED_ATTR: Attribute =
            Attribute(UTDElements.UTD_PREFIX + ":centered", UTD_NS, "true")
    }
}

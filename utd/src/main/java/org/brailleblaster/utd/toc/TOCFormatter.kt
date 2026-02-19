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
package org.brailleblaster.utd.toc

import nu.xom.Attribute
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.utd.FormatSelector
import org.brailleblaster.utd.IStyle
import org.brailleblaster.utd.PageBuilder
import org.brailleblaster.utd.formatters.Formatter
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.properties.Align
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.PageBuilderHelper.handlePageIndicator
import org.brailleblaster.utd.utils.PageBuilderHelper.isPageIndicator
import org.brailleblaster.utd.utils.getAssociatedBrlElement
import org.brailleblaster.utd.utils.getDescendantBrlFast
import org.brailleblaster.utd.utils.getFirstTextDescendant
import org.brailleblaster.utd.utils.stripBRLOnly
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * 	 This formatter handles the TOC
 */
class TOCFormatter : Formatter() {
    private var workingTitle: StringBuilder = StringBuilder()
    private var titleAdded = false

    override fun format(
        node: Node, style: IStyle,
        pageBuilders: Set<PageBuilder>, formatSelector: FormatSelector
    ): MutableSet<PageBuilder> {
        val mutPageBuilders = pageBuilders.toMutableSet()
        if (node !is Element) {
            return mutPageBuilders
        }

        var pageBuilder = mutPageBuilders.last()
        pageBuilder.setSkipNumberLines(style.skipNumberLines)
        pageBuilder.isTOC = true
        pageBuilder.setAfterTPage(false)
        val keepWithNext = style.isKeepWithNext
        if (keepWithNext && !pageBuilder.getKeepWithNext()) pageBuilder.setKeepWithNext(true)
        pageBuilder.addAtLeastLinesBefore(1)

        //This will receive the title
        //Find sequential TOC Page which presumably is associated with this
        var pageMatch: Element?
        run {
            pageMatch = XMLHandler.childrenRecursiveNodeVisitor(node) { curNode: Node -> isTocPage(curNode) } as Element?
            if (pageMatch == null) {
                //See if next non-blank text node is inside the page
                //Don't keep searching as at the end of the TOC it will search the entire remaining document
                var pageResult: Element? = null
                XMLHandler.followingVisitor(node) { curNode: Node ->
                    log.debug("Current node: {}", curNode)
                    if (curNode is Text) {
                        //Text's parent isn't a page, stop
                        return@followingVisitor true
                    }

                    if (isTocPage(curNode)) {
                        pageResult = curNode as Element
                        return@followingVisitor true
                    }
                    false
                }
                pageMatch = pageResult
            }
        }

        val sb = StringBuilder()
        // A page number can be formed of multiple brl elements, eg. if there is partial
        // emphasis of the page number.
        val pageCheck = pageMatch != null
        if (pageCheck) {
            val brlPages = pageMatch.getDescendantBrlFast()
            for (brlPage in brlPages) {
                //Clear out any possible existing guide dots within the page elements first
                brlPage.stripBRLOnly()
                sb.append(getFirstTextDescendant(brlPage).value)
            }
        }
        val page = sb.toString()
        //relativeStyle not needed
        val curPages = mutPageBuilders.size
        pageBuilder.setStartOfBlock(true)
        val namespaceMap = formatSelector.styleMap.namespaces
        mutPageBuilders.addAll(
            formatTitle(
                node,
                style,
                pageBuilder,
                formatSelector,
                page.length,
                style.linesBefore,
                style.getLinesBefore(node, namespaceMap),
                style.newPagesBefore
            )
        )
        //You don't have to readd headings
        if (mutPageBuilders.size > curPages && !isTocHeading(node)) {
            /*
             * If you have more than one page after adding the title
             * and you have keepWithNext then remove the title from the current page
             * and readd it on to the next page. Continue.
             */
            if (keepWithNext) {
                pageBuilder = removeBrl(pageBuilder, node)
                pageBuilder.clearKeepWithNext()
                pageBuilder.setKeepWithNext(false)
                pageBuilder = mutPageBuilders.last()
                pageBuilder = removeBrl(pageBuilder, node)
                //				pageBuilders.addAll(formatTitle(element, style, pageBuilder, formatSelector, page.length(), style.getLinesBefore(), style.getLinesBefore(element, namespaceMap)));
                //Don't readd any pending lines that you added before, it' will already be on the page???
                mutPageBuilders.addAll(formatTitle(node, style, pageBuilder, formatSelector, page.length, 0, 0, 0))
            } else {
                pageBuilder = mutPageBuilders.last()
            }
        }

        if (pageCheck) {
            mutPageBuilders.addAll(formatPage(pageMatch, pageBuilder, formatSelector, page.length))
        }

        if (keepWithNext) mutPageBuilders.forEach { pb: PageBuilder -> pb.setKeepWithNext(false) }

        workingTitle = StringBuilder()
        titleAdded = false
        return mutPageBuilders
    }

    private fun removeBrl(pageBuilder: PageBuilder, element: Element): PageBuilder {
        var curPageBuilder = pageBuilder
        if (UTDElements.BRL.isA(element)) {
            curPageBuilder.removeBrl(element)
        }

        for (child in element.childElements) {
            curPageBuilder = removeBrl(curPageBuilder, child)
        }

        return curPageBuilder
    }

    private fun formatTitle(
        element: Element, origStyle: IStyle,
        pageBuilder: PageBuilder, formatSelector: FormatSelector, pageLength: Int,
        pendingLines: Int, maxLines: Int?, pendingPages: Int
    ): Set<PageBuilder> {
        var curPageBuilder = pageBuilder
        val results: MutableSet<PageBuilder> = LinkedHashSet()
        if (isTocHeading(element)) {
            //Format separately
            results.addAll(formatHeading(element, origStyle, curPageBuilder, formatSelector, pendingLines, maxLines))
            return results
        }

        results.add(curPageBuilder)
        curPageBuilder.setFirstLineIndent(origStyle.firstLineIndent)
        curPageBuilder.alignment = origStyle.align
        curPageBuilder.setSkipNumberLines(origStyle.skipNumberLines)
        curPageBuilder.isTOC = true
        curPageBuilder.addAtLeastLinesBefore(pendingLines)
        if (maxLines != null) {
            curPageBuilder.setMaxLines(maxLines)
        }
        curPageBuilder.isCenteredWithDots = true

        for (i in 0 until element.childCount) {
            if (element.getChild(i) is Text) {
                //Calculate the remaining space for the title
                //2 cells for spaces, at least 2 for guide dots, and page number length
                val brl = element.getAssociatedBrlElement(i)!!
                if (isPageIndicator(brl)) {
                    results.addAll(handlePageIndicator(curPageBuilder, brl, origStyle, formatSelector))
                    curPageBuilder = results.last()
                } else {
                    workingTitle.append(retrieveFullTitle(brl))

                    //Dot space is 3 because you don't need to have guide dots if your title and page can fit on one line
                    val dotSpace = 3

                    //Have to modify to give space for aligned titles
                    var rightIndent = -6
                    if (pageLength + dotSpace > 6) {
                        rightIndent = (pageLength + dotSpace) * -1
                    }
                    val leftIndent = origStyle.indent
                    curPageBuilder.setLeftIndent(leftIndent ?: 0)
                    curPageBuilder.setRightIndent(rightIndent)
                    val size = results.size
                    results.addAll(curPageBuilder.addBrl(brl))
                    if (results.size > size) {
                        curPageBuilder = results.last()
                    }

                    titleAdded = true
                }
            } else if (element.getChild(i) is Element) {
                val nextChild = element.getChild(i) as Element
                val styleMap = formatSelector.styleMap
                val curStyle = styleMap.findValueOrDefault(element.getChild(i))

                if (!UTDElements.BRL.isA(nextChild)) {
                    val attribute = TOCAttributes.TYPE.getAttribute(nextChild)
                    if (attribute != null && attribute.value == "page") {
                        continue
                    }
                    val size = results.size
                    results.addAll(
                        formatTitle(
                            nextChild,
                            origStyle,
                            curPageBuilder,
                            formatSelector,
                            pageLength,
                            curStyle.linesBefore,
                            curStyle.getLinesBefore(nextChild, styleMap.namespaces),
                            curStyle.newPagesBefore
                        )
                    )
                    if (results.size > size) {
                        curPageBuilder = results.last()
                    }
                }
            }
        }

        curPageBuilder.setLeftIndent(0)
        curPageBuilder.setRightIndent(0)
        curPageBuilder.addAtLeastLinesAfter(origStyle.linesAfter)

        return results
    }

    private fun formatHeading(
        element: Element, style: IStyle,
        pageBuilder: PageBuilder, formatSelector: FormatSelector, pendingLines: Int, maxLines: Int?
    ): Set<PageBuilder> {
        var curPageBuilder = pageBuilder
        val results: MutableSet<PageBuilder> = LinkedHashSet()
        results.add(curPageBuilder)
        curPageBuilder.isTOC = true

        for (i in 0 until element.childCount) {
            if (element.getChild(i) is Text) {
                //Calculate the remaining space for the title
                //2 cells for spaces, at least 2 for guide dots, and page number length
                val brl = element.getAssociatedBrlElement(i)!!
                if (isPageIndicator(brl)) {
                    results.addAll(handlePageIndicator(curPageBuilder, brl, style, formatSelector))
                    curPageBuilder = results.last()
                } else {
                    curPageBuilder.addAtLeastLinesBefore(pendingLines)
                    if (maxLines != null) {
                        curPageBuilder.setMaxLines(maxLines)
                    }
                    if (style.newPagesBefore > 0) {
                        curPageBuilder.addAtLeastPages(style.newPagesBefore)
                        results.addAll(curPageBuilder.processSpacing())
                        curPageBuilder = results.last()
                    }

                    curPageBuilder.setFirstLineIndent(style.firstLineIndent)
                    curPageBuilder.alignment = style.align
                    curPageBuilder.setSkipNumberLines(style.skipNumberLines)
                    val leftIndent = style.indent
                    curPageBuilder.setLeftIndent(leftIndent ?: 0)
                    val size = results.size
                    results.addAll(curPageBuilder.addBrl(brl))
                    if (results.size > size) {
                        curPageBuilder = results.last()
                    }
                }
            } else if (element.getChild(i) is Element) {
                val nextChild = element.getChild(i) as Element
                val styleMap = formatSelector.styleMap
                //Emphasis does not contain the correct style
                val curStyle = if (nextChild.localName == "INLINE") style else styleMap.findValueOrDefault(nextChild)

                if (!UTDElements.BRL.isA(nextChild)) {
                    val attribute = TOCAttributes.TYPE.getAttribute(nextChild)
                    if (attribute != null && attribute.value == "page") {
                        continue
                    }
                    val size = results.size
                    results.addAll(
                        formatHeading(
                            nextChild,
                            curStyle,
                            curPageBuilder,
                            formatSelector,
                            curStyle.linesBefore,
                            curStyle.getLinesBefore(nextChild, styleMap.namespaces)
                        )
                    )
                    if (results.size > size) {
                        curPageBuilder = results.last()
                    }
                }
            }
        }

        curPageBuilder.addAtLeastLinesAfter(style.linesAfter)

        return results
    }

    private fun formatPage(
        element: Element?,
        pageBuilder: PageBuilder,
        formatSelector: FormatSelector,
        pageLength: Int
    ): Set<PageBuilder> {
        var curPageBuilder = pageBuilder
        val results: MutableSet<PageBuilder> = LinkedHashSet()
        results.add(curPageBuilder)
        curPageBuilder.finishSegment()
        curPageBuilder.alignment = Align.LEFT

        //Get the current x position
        val curX = curPageBuilder.x

        //Calculate the remaining space for the title
        //2 cells for spaces, at least 2 for guide dots, and page number length
        // A page number may be formed of multiple brl elements, such as in the case of
        // partial emphasis of the page number.
        val brls = element.getDescendantBrlFast()

        if (brls.isNotEmpty()) {
            //Do not add guide dots if there is no title present
            //Formats 2016: Guide dots are not needed if the page number and the title can fit on the same line
            val endX = curPageBuilder.cellsPerLine - pageLength
            if ((pageLength > 0 && titleAdded) && (endX - curX) != 1) {
                var guideDot = '"'
                if (!formatSelector.engine.brailleSettings.isUseAsciiBraille) {
                    guideDot = '\u2810'
                }
                val guideDots = curPageBuilder.fillSpace(guideDot, endX - curX, 1, 4)
                guideDots?.addAttribute(Attribute("type", "guideDots"))
            }

            curPageBuilder.x = curPageBuilder.cellsPerLine - pageLength
            curPageBuilder.setLeftIndent(0)
            val origMaxLines = curPageBuilder.getMaxLines()
            curPageBuilder.setMaxLines(-1)

            for (brl in brls) {
                val size = results.size
                results.addAll(curPageBuilder.addBrl(brl))
                if (results.size > size) {
                    curPageBuilder = results.last()
                }
            }
            curPageBuilder.setMaxLines(origMaxLines)
        }

        return results
    }

    private fun retrieveFullTitle(brl: Element): String {
        val fullTitle = StringBuilder()

        for (i in 0 until brl.childCount) {
            if (brl.getChild(i) is Text) {
                fullTitle.append(brl.getChild(i).value)
            } else if (brl.getChild(i) is Element) {
                fullTitle.append(retrieveFullTitle(brl.getChild(i) as Element))
            }
        }

        return fullTitle.toString()
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(TOCFormatter::class.java)
        var time: Long = 0
        private fun isTocPage(curNode: Node): Boolean {
            return isTocAttr(curNode, "page")
        }

        private fun isTocHeading(curNode: Node): Boolean {
            return isTocAttr(curNode, "heading")
        }

        private fun isTocAttr(curNode: Node, value: String): Boolean {
            if (curNode !is Element) {
                return false
            }

            val tocTypeAttrib = TOCAttributes.TYPE.getAttribute(curNode)
            return tocTypeAttrib != null && tocTypeAttrib.value == value
        }
    }
}
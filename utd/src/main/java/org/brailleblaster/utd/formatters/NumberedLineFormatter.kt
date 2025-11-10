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
import nu.xom.Text
import org.brailleblaster.utd.FormatSelector
import org.brailleblaster.utd.IStyle
import org.brailleblaster.utd.PageBuilder
import org.brailleblaster.utd.StyleStack
import org.brailleblaster.utd.exceptions.BadPoetryException
import org.brailleblaster.utd.properties.NumberLinePosition
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.PageBuilderHelper.applySkipLinesNode
import org.brailleblaster.utd.utils.PageBuilderHelper.handlePageIndicator
import org.brailleblaster.utd.utils.PageBuilderHelper.isPageIndicator
import org.brailleblaster.utd.utils.PageBuilderHelper.isSkipLinesNode
import org.brailleblaster.utd.utils.UTDHelper.getAssociatedBrlElement
import org.brailleblaster.utd.utils.UTDHelper.getDescendantBrlFast
import org.brailleblaster.utils.xml.UTD_NS
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.math.max

/*
 * 		This formatter handles line-numbered poetry.
 */
class NumberedLineFormatter : Formatter() {
    var lines: MutableList<Element>? = null
    var lineDetails: MutableList<IntArray>? = null
    var padding: Int = 0
    var yBegin: Int = 0
    var xBegin: Int = 0
    var pendingLinePos: Int = 0
    var isPending: Boolean = false
    var parentLinenum: Element? = null
    private val log: Logger = LoggerFactory.getLogger(NumberedLineFormatter::class.java)
    private var skipPoetry = false

    fun format(
        node: Node, style: IStyle, pageBuilders: MutableSet<PageBuilder>,
        formatSelector: FormatSelector, lineNumbers: MutableList<Element>?, lineDetails: MutableList<IntArray>?
    ): Set<PageBuilder> {
        this.lines = lineNumbers
        this.lineDetails = lineDetails
        pageBuilders.last().poemEnabled = true
        return formatImpl(node, style, pageBuilders, formatSelector, false)
    }

    override fun format(
        node: Node, style: IStyle, pageBuilders: Set<PageBuilder>,
        formatSelector: FormatSelector
    ): MutableSet<PageBuilder> {
        // To ensure it works as previously
        val pageBuilder = pageBuilders.last()
        val startOfBlockState = pageBuilder.isStartOfBlock()
        pageBuilder.setStartOfBlock(true)
        lines = ArrayList()
        lineDetails = ArrayList()
        parentLinenum = null
        yBegin = -1
        xBegin = -1
        padding = 2
        pendingLinePos = -1
        isPending = false
        val results = formatGroup(node, style, pageBuilders.toMutableSet(), formatSelector)
        val lastPB = results.last()
        lastPB.setStartOfBlock(startOfBlockState)
        lines?.clear()
        lines = null
        return results
    }

    fun formatGroup(
        node: Node, style: IStyle, pageBuilders: MutableSet<PageBuilder>,
        formatSelector: FormatSelector
    ): MutableSet<PageBuilder> {
        var results = pageBuilders

        if (node !is Element) {
            return results
        }

        var pageBuilder = results.last()
        var size = pageBuilders.size
        pageBuilders.add(checkImagePlaceholder(node, style, pageBuilders, formatSelector))
        if (pageBuilders.size > size) {
            pageBuilder = handleNewPage(pageBuilder, pageBuilders, style)
        }
        val origPad = pageBuilder.padding

        //		pageBuilder.setSkipNumberLines(style.getSkipNumberLines());
        pageBuilder.alignment = style.align
        val linesBefore = style.getLinesBefore(node, formatSelector.styleMap.namespaces)
        if (linesBefore != null) {
            pageBuilder.setMaxLines(linesBefore)
        } else {
            pageBuilder.addAtLeastLinesBefore(style.linesBefore)
        }
        pageBuilder.setStartOfBlock(true)

        pageBuilders.add(pageBuilder)

        // Format as a group if it's a poetic stanza
        val styleName = if (style is StyleStack) {
            Objects.requireNonNull(style.peek()).name
        } else {
            style.name
        }
        if (styleName != "Poetic Stanza") {
            results = formatImpl(node, style, pageBuilders, formatSelector, true)
            pageBuilder = results.last()
            pageBuilder.setStartOfBlock(true)
            return results
        }

        for (i in 0 until node.getChildCount()) {
            val styleMap = formatSelector.styleMap
            val curStyle = styleMap.findValueOrDefault(node.getChild(i))
            if (style is StyleStack) {
                style.push(curStyle)
            }

            if (node.getChild(i) is Element) {
                if ((node.getChild(i) as Element).getAttribute("class") != null
                    && ((node.getChild(i) as Element).getAttributeValue("class") == "dontsplit")
                ) {
                    formatGroup(node.getChild(i), style, pageBuilders, formatSelector)
                    continue
                }
                size = pageBuilders.size
                pageBuilders.add(
                    checkImagePlaceholder(
                        node.getChild(i) as Element,
                        style,
                        pageBuilders,
                        formatSelector
                    )
                )
                if (pageBuilders.size > size) {
                    pageBuilder = handleNewPage(pageBuilder, pageBuilders, style)
                }
            }

            handleWrappedLines(node.getChild(i))

            var projectedPosition = pageBuilder.y + pageBuilder.pendingLinesBefore

            size = pageBuilders.size
            results.addAll(formatImpl(node.getChild(i), style, pageBuilders, formatSelector, true))
            if (results.size > size) {
                pageBuilder = results.last()
                projectedPosition = pageBuilder.y
            }
            pageBuilder.setStartOfBlock(true)

            if (pageBuilder.getLineNumber() != null && projectedPosition < pageBuilder.linesPerPage) {
                pageBuilder.setLineNumberPos(projectedPosition)
                pageBuilder.setLineNumberLength(pageBuilder.getLineNumber()!!.getAttributeValue("lineNumber").length)
                pageBuilder.insertLineNumber()
            }

            if (style is StyleStack) {
                style.pop()
            }
        }

        pageBuilder.padding = origPad
        pageBuilder.setRightIndent(0)

        return results
    }

    private fun handleWrappedLines(element: Node) {
        val assBrls = getDescendantBrlFast(element)
        if (assBrls.size > 1) {
            for (j in 0 until assBrls.size - 1) {
                for (line in lines!!) {
                    if (line == assBrls[j]) {
                        line.addAttribute(Attribute("continued", "true"))
                    }

                    if (line == assBrls[assBrls.size - 1]) {
                        line.addAttribute(Attribute("continued", "last"))
                    }
                }

                assBrls[j].addAttribute(Attribute("continued", "true"))
            }

            assBrls[assBrls.size - 1].addAttribute(Attribute("continued", "last"))
        }
    }

    private fun formatImpl(
        node: Node, style: IStyle, pageBuilders: MutableSet<PageBuilder>,
        formatSelector: FormatSelector, isNewLine: Boolean
    ): MutableSet<PageBuilder> {
        var pageBuilder = pageBuilders.last()

        if (node !is Element) {
            return pageBuilders
        }

        val isLineNum = node.getAttribute("linenum") != null
        // It's under a poem so automatically assume it's a line
        // Not all styles have names including these ones
        node.addAttribute(Attribute(UTDElements.UTD_STYLE_ATTRIB, "Poetry Line"))

        pageBuilder.alignment = style.align
        // Only consider adding new lines if you're on new poetry lines
        val isEmphasis = ((node.getAttribute("type", UTD_NS) != null)
                && (node.getAttributeValue("type", UTD_NS) == "EMPHASIS"))
        if (isNewLine && !isEmphasis) {
            val linesBefore = style.getLinesBefore(node, formatSelector.styleMap.namespaces)
            if (linesBefore != null) {
                pageBuilder.setMaxLines(linesBefore)
            } else {
                pageBuilder.addAtLeastLinesBefore(style.linesBefore)
            }
        }

        pageBuilder.setStartOfBlock(true)

        if (yBegin == -1 || xBegin == -1) {
            setXY(pageBuilder.x, pageBuilder.y)
        }

        var i = 0
        while (i < node.getChildCount()) {
            if (skipPoetry) {
                return formatRemainingAsLiterary(node, i, style, pageBuilders, formatSelector)
            }

            if (node.getChild(i) is Text) {
                val brl = getAssociatedBrlElement(node, i)
                pageBuilder.poemEnabled = true

                if (brl == null) {
                    i++
                    continue
                }

                var firstLineIndent = style.firstLineIndent
                if (firstLineIndent == null) {
                    firstLineIndent = 0
                }
                pageBuilder.setFirstLineIndent(firstLineIndent)
                var leftIndent = style.indent
                if (leftIndent == null) {
                    leftIndent = 0
                }
                pageBuilder.setLeftIndent(leftIndent)
                //				pageBuilder.setSkipNumberLines(style.getSkipNumberLines());
                pageBuilder.setStartOfBlock(true)

                lines!!.add(brl)
                lineDetails!!.add(intArrayOf(pageBuilder.pendingLinesBefore, firstLineIndent, leftIndent))

                if (isLineNum || parentLinenum?.getAttribute("linenum") != null) {
                    pageBuilder.setSkipNumberLines(NumberLinePosition.BOTH)
                    pageBuilder.setContinueSkip(true)
                    pageBuilder.padding = padding

                    // make sure this element is to the right
                    var numLength: Int

                    if (parentLinenum?.getAttribute("linenum") != null) {
                        numLength = parentLinenum!!.getAttributeValue("linenum").length
                        brl.addAttribute(Attribute("lineNumber", parentLinenum!!.getAttributeValue("linenum")))
                        parentLinenum = null
                    } else {
                        numLength = node.getAttributeValue("linenum").length
                        brl.addAttribute(Attribute("lineNumber", node.getAttributeValue("linenum")))
                    }

                    if (numLength > pageBuilder.getLineNumberLength()
                        && numLength > ((pageBuilder.getPoemRightIndent() + padding) * -1)
                    ) {
                        pageBuilder.setSkipNumberLines(NumberLinePosition.BOTH)
                        pageBuilder.setContinueSkip(true)
                        pageBuilder.setPoemRightIndent((numLength + padding) * -1)
                        pageBuilder.setRightIndent(pageBuilder.getPoemRightIndent())

                        // Removing all the brls included in lineNumbers
                        for (line in lines!!) {
                            if (!isSkipLinesNode(line)) {
                                for (builder in pageBuilders) builder.removeBrl(line)
                            }
                        }

                        pageBuilder.x = xBegin
                        pageBuilder.y = yBegin

                        // pageBuilder.setPoemRightIndent((numLength + padding)
                        // * -1);

                        // Readd the brailles accordingly.
                        pageBuilder.setLineNumberLength(numLength)
                        pageBuilder = readdBrl(pageBuilders, pageBuilder, style, formatSelector)
                    } else {
                        if (brl.getAttribute("lineNumber") != null) {
                            pageBuilder.setLineNumber(brl)

                            if (brl.getAttribute("continued") != null) {
                                if (brl.getAttributeValue("continued") == "true" && pendingLinePos != -1) {
                                    pendingLinePos = pageBuilder.y
                                } else if (brl.getAttributeValue("continued") == "last" && pendingLinePos != -1) {
                                    pageBuilder.setLineNumberPos(pendingLinePos)
                                    pageBuilder.setLineNumberLength(numLength)
                                    pageBuilder.insertLineNumber()
                                    isPending = false
                                    pendingLinePos = -1
                                }
                            } else if (pageBuilder.y + pageBuilder.pendingLinesBefore < pageBuilder
                                    .linesPerPage
                            ) {
                                pageBuilder.setLineNumberPos(pageBuilder.y + pageBuilder.pendingLinesBefore)
                                pageBuilder.setLineNumberLength(numLength)
                                pageBuilder.insertLineNumber()
                            }
                        }

                        pageBuilder.setRightIndent(pageBuilder.getPoemRightIndent())
                        val size = pageBuilders.size
                        pageBuilders.addAll(pageBuilder.addBrl(brl))
                        if (pageBuilders.size > size) {
                            try {
                                handleBadPoetry(pageBuilders, brl, pageBuilder, style)
                            } catch (_: BadPoetryException) {
                                pageBuilder.removeBrl(brl)
                                pageBuilder = pageBuilders.last()
                                pageBuilder.removeBrl(brl)
                                // pageBuilder.setPoemRightIndent(0);
                                pageBuilder.setRightIndent(0)
                                skipPoetry = true
                                return formatRemainingAsLiterary(
                                    node, i - 1, style,
                                    pageBuilders, formatSelector
                                )
                            }
                            pageBuilder = handleNewPage(pageBuilder, brl, pageBuilders, style)
                            i--
                        }
                    }
                } else {
                    val isPageIndicator = isPageIndicator(brl)
                    if (isPageIndicator) {
                        val size = pageBuilders.size
                        pageBuilders.addAll(handlePageIndicator(pageBuilder, brl, style, formatSelector))
                        pageBuilder = getLatestPB(pageBuilders, size, pageBuilder)
                    }

                    pageBuilder.setRightIndent(pageBuilder.getPoemRightIndent())

                    val size = pageBuilders.size
                    val oldLinesBefore = pageBuilder.pendingLinesBefore
                    if (!isPageIndicator) {
                        pageBuilders.addAll(pageBuilder.addBrlFromChild(brl, 0))
                    }
                    if (pageBuilders.size > size) {
                        try {
                            handleBadPoetry(pageBuilders, brl, pageBuilder, style)
                        } catch (_: BadPoetryException) {
                            pageBuilder.removeBrl(brl)
                            pageBuilder = pageBuilders.last()
                            pageBuilder.removeBrl(brl)
                            // pageBuilder.setPoemRightIndent(0);
                            pageBuilder.setRightIndent(0)
                            skipPoetry = true
                            return formatRemainingAsLiterary(
                                node, i - 1, style,
                                pageBuilders, formatSelector
                            )
                        }
                        pageBuilder = handleNewPage(pageBuilder, brl, pageBuilders, style)
                        pageBuilder.addAtLeastLinesBefore(oldLinesBefore)
                        i--
                    }
                }
            } else if (node.getChild(i) is Element) {
                if ((node.getChild(i) as Element).getAttribute("class") != null
                    && ((node.getChild(i) as Element).getAttributeValue("class") == "dontsplit")
                ) {
                    formatGroup(node.getChild(i), style, pageBuilders, formatSelector)
                    i++
                    continue
                }

                val size = pageBuilders.size
                pageBuilders.add(
                    checkImagePlaceholder(
                        node.getChild(i) as Element,
                        style,
                        pageBuilders,
                        formatSelector
                    )
                )
                if (pageBuilders.size > size) {
                    // if (pageBuilders.size() > 1) {
                    pageBuilder = handleNewPage(pageBuilder, pageBuilders, style)
                }

                val styleMap = formatSelector.styleMap
                val curStyle = styleMap.findValueOrDefault(node.getChild(i))

                if (!UTDElements.BRL.isA(node.getChild(i))) {
                    if (isLineNum) {
                        parentLinenum = node
                    }

                    (style as StyleStack).push(curStyle)
                    pageBuilders.addAll(
                        format(node.getChild(i), style, pageBuilders, formatSelector, lines, lineDetails)
                    )
                    if (pageBuilders.size > 1) {
                        pageBuilder = pageBuilders.last()
                        pageBuilder.setStartOfBlock(true)
                    }
                    style.pop()

                    // Prevent parentLinenum from carrying over to the <line>
                    // sibling
                    parentLinenum = null
                }
            }
            i++
        }

        return pageBuilders
    }

    /**
     * Format child nodes as literary from a given child index.
     *
     *
     * In the context of this formatter this method is probably a last resort recovery. If child nodes have a brl and that brl does not get formatted then this can lead to issues with saving, creating BRF or PEF and other bugs which are not immediately obvious and can prevent users doing things.
     *
     * @param element        Thee parent element of the child nodes to be formatted as literary.
     * @param fromIndex      The index of the child node to start formatting as literary.
     * @param style          The current style.
     * @param pageBuilders   The current pages held by the formatter.
     * @param formatSelector The format selector.
     * @return All pages after formatting.
     */
    private fun formatRemainingAsLiterary(
        element: Element,
        fromIndex: Int,
        style: IStyle,
        pageBuilders: MutableSet<PageBuilder>,
        formatSelector: FormatSelector
    ): MutableSet<PageBuilder> {
        var pages = pageBuilders
        val formatter: Formatter = LiteraryFormatter()
        for (i in max(fromIndex, 0) until element.childCount) {
            pages = formatter.format(element.getChild(i), style, pages, formatSelector)
        }
        return pages
    }

    fun setXY(x: Int, y: Int) {
        xBegin = x
        yBegin = y
    }

    private fun getLatestPB(results: Set<PageBuilder>, origSize: Int, pageBuilder: PageBuilder): PageBuilder {
        if (results.size > origSize) {
            lines!!.clear()
            lineDetails!!.clear()
            return results.last()
        }
        return pageBuilder
    }

    private fun readdBrl(
        pageBuilders: MutableSet<PageBuilder>, pageBuilder: PageBuilder, style: IStyle,
        formatSelector: FormatSelector
    ): PageBuilder {
        var pageBuilder = pageBuilder
        var j = 0
        while (j < lines!!.size) {
            if (isSkipLinesNode(lines!![j])) {
                pageBuilders.add(checkImagePlaceholder(lines!![j], style, pageBuilders, formatSelector))
                if (pageBuilders.size > 1) {
                    pageBuilder = handleNewPage(pageBuilder, pageBuilders, style)
                }
                j++
                continue
            }

            var size = pageBuilders.size

            pageBuilder.addAtLeastLinesBefore(lineDetails!![j][0])
            pageBuilder.setFirstLineIndent(lineDetails!![j][1])
            pageBuilder.setLeftIndent(lineDetails!![j][2])
            pageBuilder.setStartOfBlock(true)

            pageBuilders.addAll(pageBuilder.processSpacing())
            if (pageBuilders.size > size) {
                val lineNums = pageBuilder.lineNums
                for (lineNum in lineNums) {
                    pageBuilder.removeFormattingElement(lineNum)
                }
                val oldRI = pageBuilder.getPoemRightIndent()
                pageBuilder = pageBuilders.last()
                pageBuilder.setStartOfBlock(true)
                pageBuilder.setPoemRightIndent(oldRI)
                pageBuilder.setFirstLineIndent(lineDetails!![j][1])
                pageBuilder.setLeftIndent(lineDetails!![j][2])
                if (lineDetails!![j][0] > 1) {
                    pageBuilder.addAtLeastLinesBefore(1)
                }
                setXY(0, pageBuilder.y)
            }

            var topOfPage = false
            if (pageBuilder.y == 0 && pageBuilder.isSkipTop()) {
                pageBuilder.moveY(pageBuilder.lineSpacing, true)
                topOfPage = true
            }
            pageBuilder.setRightIndent(pageBuilder.getPoemRightIndent())

            var lineNums = pageBuilder.lineNums

            if (lines!![j].getAttribute("lineNumber") != null) {
                for (lineNum in lineNums) {
                    if (lineNum.value == lines!![j].getAttributeValue("lineNumber")) {
                        pageBuilder.removeFormattingElement(lineNum)
                    }
                }

                if (lines!![j].getAttribute("continued") != null) {
                    pendingLinePos = pageBuilder.y + pageBuilder.pendingLinesBefore
                    isPending = true
                }
                pageBuilder.setLineNumber(lines!![j])
                pageBuilder.setLineNumberPos(pageBuilder.y + pageBuilder.pendingLinesBefore)
            }

            size = pageBuilders.size
            val isPageIndicator = isPageIndicator(lines!![j])
            if (isPageIndicator) {
                pageBuilders.addAll(
                    handlePageIndicator(pageBuilder, lines!![j], style, formatSelector)
                )
                pageBuilder = getLatestPB(pageBuilders, size, pageBuilder)
            } else {
                pageBuilders.addAll(pageBuilder.addBrl(lines!![j]))
            }

            if (lines!![j].getAttribute("continued") != null && lines!![j].getAttributeValue("continued") == "last" && pendingLinePos != -1) {
                pageBuilder.setLineNumberPos(pendingLinePos)
                isPending = false
                pendingLinePos = -1
            }

            if (!isPending) {
                pageBuilder.insertLineNumber()
            }

            if (pageBuilders.size > size) {
                if (topOfPage) {
                    // Will not be able to do better as it was inserted at the
                    // top of a page so just continue
                    log.warn("Potential formatting issue on print page {}", pageBuilder.printPageNumber)
                    j++
                    continue
                }
                val brlCopy = lines!![j]
                var lastBrlOnly: Element? = null

                lineNums = pageBuilder.lineNums

                if (lineNums.isNotEmpty()) {
                    lastBrlOnly = pageBuilder.lineNums.last()
                }
                while (j > 0) {
                    lines!!.removeAt(0)
                    lineDetails!!.removeAt(0)
                    j--
                }

                pageBuilder.removeBrl(brlCopy)
                if (lastBrlOnly != null) {
                    pageBuilder.removeFormattingElement(lastBrlOnly)
                }
                val oldRI = pageBuilder.getPoemRightIndent()
                pageBuilder = pageBuilders.last()
                pageBuilder.setStartOfBlock(true)
                pageBuilder.removeBrl(brlCopy)
                if (lastBrlOnly != null) {
                    pageBuilder.removeFormattingElement(lastBrlOnly)
                }
                pageBuilder.setPoemRightIndent(oldRI)
                pageBuilder.setRightIndent(pageBuilder.getPoemRightIndent())
                pageBuilder.setFirstLineIndent(lineDetails!![j][1])
                pageBuilder.setLeftIndent(lineDetails!![j][2])
                setXY(0, pageBuilder.y)

                // pageBuilder.setSkipNumberLines(style.getSkipNumberLines());
                if (lines!![j].getAttribute("continued") != null && lines!![j].getAttributeValue("continued") == "last" && pendingLinePos != -1) {
                    pageBuilder.setLineNumberPos(pendingLinePos)
                    isPending = false
                    pendingLinePos = -1
                }

                if (!isPending) {
                    pageBuilder.insertLineNumber()
                }

                j = -1
            }
            j++
        }

        return pageBuilder
    }

    private fun handleNewPage(pageBuilder: PageBuilder, pageBuilders: Set<PageBuilder>, style: IStyle): PageBuilder {
        return handleNewPage(pageBuilder, null, pageBuilders, style)
    }

    private fun handleNewPage(
        pageBuilder: PageBuilder, brl: Element?, pageBuilders: Set<PageBuilder>,
        style: IStyle
    ): PageBuilder {
        var pageBuilder = pageBuilder
        lines!!.clear()
        lineDetails!!.clear()
        if (brl != null) {
            pageBuilder.removeBrl(brl)
            // Remove any brlonly that is related to the brl that may have been
            // added on the previous page as well
            removeBrlOnlyFromPage(brl, pageBuilder)
        }

        val oldRI = pageBuilder.getPoemRightIndent()
        val oldSkip = pageBuilder.skipNumberLines
        pageBuilder = pageBuilders.last()
        // pageBuilder.setStartOfBlock(true);
        pageBuilder.setPoemRightIndent(oldRI)
        pageBuilder.setRightIndent(pageBuilder.getPoemRightIndent())
        pageBuilder.setFirstLineIndent(style.firstLineIndent)
        var leftIndent = style.indent
        if (leftIndent == null) {
            leftIndent = 0
        }
        pageBuilder.setLeftIndent(leftIndent)

        if (brl != null) pageBuilder.removeBrl(brl)
        setXY(0, pageBuilder.y)

        // Only set skip number lines when you have line numbers; base this
        // setting on the previous page
        // pageBuilder.setSkipNumberLines(style.getSkipNumberLines());
        pageBuilder.setSkipNumberLines(oldSkip)

        return pageBuilder
    }

    private fun checkImagePlaceholder(
        element: Element, style: IStyle, pageBuilders: MutableSet<PageBuilder>,
        formatter: FormatSelector
    ): PageBuilder {
        var pageBuilders = pageBuilders
        if (isSkipLinesNode(element)) {
            // If the element isn't in the list already, add.
            val add = lines!!.stream().noneMatch { line: Element -> line == element }

            if (add) {
                lines!!.add(element)
                lineDetails!!.add(intArrayOf(0, 0, 0))
            }

            pageBuilders = applySkipLinesNode(element, style, pageBuilders, formatter)
            val pageBuilder = pageBuilders.last()
            pageBuilders.addAll(pageBuilder.processSpacing())
        }

        return pageBuilders.last()
    }

    @Throws(BadPoetryException::class)
    private fun handleBadPoetry(pageBuilders: Set<PageBuilder>, brl: Element, pageBuilder: PageBuilder, style: IStyle) {
        var testPB = pageBuilder

        val oldRI = testPB.getPoemRightIndent()
        val lines = testPB.pendingLinesBefore
        val oldSkip = pageBuilder.skipNumberLines
        testPB = pageBuilders.last()
        testPB.removeBrl(brl)
        if (testPB.isBlankPageWithPageNumbers) {
            testPB.setPoemRightIndent(oldRI)
            testPB.setRightIndent(testPB.getPoemRightIndent())
            testPB.setFirstLineIndent(style.firstLineIndent)
            var leftIndent = style.indent
            if (leftIndent == null) {
                leftIndent = 0
            }
            testPB.setLeftIndent(leftIndent)
            testPB.y =
                if (testPB.findLastBlankLine() > 0) testPB.findLastBlankLine() - 1 else testPB.findLastBlankLine()
            setXY(0, testPB.y)

            // Only set skip number lines when you have line numbers; base this
            // setting on the previous page
            // testPB.setSkipNumberLines(style.getSkipNumberLines());
            testPB.setSkipNumberLines(oldSkip)
            testPB.addAtLeastLinesBefore(lines)

            if (testPB.addBrl(brl).size > 1) {
                throw BadPoetryException()
            }
        }
    }

    // Removes brlonly that is associated with the brl from the page
    private fun removeBrlOnlyFromPage(brl: Element, pageBuilder: PageBuilder) {
        val parent = brl.parent as Element
        // TODO: Need to do the same for line letter
        val lineNums = pageBuilder.lineNums
        for (lineNum in lineNums) {
            if (parent.getAttribute("linenum") != null && parent.getAttributeValue("linenum") == lineNum.value) {
                pageBuilder.removeFormattingElement(lineNum)
            }
        }
    }
}

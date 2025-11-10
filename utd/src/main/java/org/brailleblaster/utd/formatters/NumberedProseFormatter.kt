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

import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.utd.FormatSelector
import org.brailleblaster.utd.IStyle
import org.brailleblaster.utd.PageBuilder
import org.brailleblaster.utd.StyleStack
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.PageBuilderHelper.handlePageIndicator
import org.brailleblaster.utd.utils.PageBuilderHelper.isPageIndicator
import org.brailleblaster.utd.utils.PageBuilderHelper.isSkipLinesNode
import org.brailleblaster.utd.utils.UTDHelper.getAssociatedBrlElement
import org.brailleblaster.utd.utils.UTDHelper.getAssociatedNode
import java.util.*

/*
 * This formatter should receive each line numbered paragraph.
 */
class NumberedProseFormatter : Formatter() {
    var lines: MutableList<Element>? = null
    private var lineDetails: MutableList<IntArray>? = null
    var padding: Int = 0
    private var yBegin: Int = 0
    private var xBegin: Int = 0
    private var currLineNum: String? = null
    private var firstAdd: Int = 0
    var spaced: Boolean = false
    var spaces: Int = 0

    fun format(
        node: Node, style: IStyle, pageBuilders: MutableSet<PageBuilder>,
        formatSelector: FormatSelector, lineNumbers: MutableList<Element>?, lineDetails: MutableList<IntArray>?
    ): Set<PageBuilder> {
        this.lines = lineNumbers
        this.lineDetails = lineDetails
        return formatImpl(node, style, pageBuilders, formatSelector)
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
        yBegin = -1
        xBegin = -1
        padding = 2
        firstAdd = -1
        currLineNum = ""
        spaced = false
        spaces = 0
        val results = formatDiv(node, style, pageBuilders.toMutableSet(), formatSelector)
        val lastPB = results.last()
        lastPB.setStartOfBlock(startOfBlockState)
        return results
    }

    private fun formatDiv(
        node: Node, style: IStyle, pageBuilders: MutableSet<PageBuilder>,
        formatSelector: FormatSelector
    ): MutableSet<PageBuilder> {
        var results = pageBuilders

        if (node !is Element) {
            return results
        }

        for (i in 0 until node.getChildCount()) {
            val pageBuilder = pageBuilders.last()
            pageBuilder.setStartOfBlock(true)
            val styleMap = formatSelector.styleMap
            val curStyle = styleMap.findValueOrDefault(node.getChild(i))
            (style as StyleStack).push(curStyle)

            results = formatImpl(node.getChild(i), style, pageBuilders, formatSelector)

            style.pop()
        }

        return results
    }

    private fun formatImpl(
        node: Node, style: IStyle, pageBuilders: MutableSet<PageBuilder>,
        formatSelector: FormatSelector
    ): MutableSet<PageBuilder> {
        var pageBuilder = pageBuilders.last()

        if (node !is Element) {
            return pageBuilders
        }

        pageBuilder.setSkipNumberLines(style.skipNumberLines)
        pageBuilder.setContinueSkip(true)
        pageBuilder.alignment = style.align
        val linesBefore = style.getLinesBefore(node, formatSelector.styleMap.namespaces)

        if (linesBefore != null) {
            pageBuilder.setMaxLines(linesBefore)
        } else {
            pageBuilder.addAtLeastLinesBefore(style.linesBefore)
        }

        var size = pageBuilders.size


//		pageBuilder.setStartOfBlock(true);
        val firstLineIndent = safeUnbox(style.firstLineIndent)
        pageBuilder.setFirstLineIndent(firstLineIndent)
        val leftIndent = safeUnbox(style.indent, 0)

        pageBuilder.setLeftIndent(leftIndent)

        pageBuilders.addAll(pageBuilder.processSpacing())
        if (pageBuilders.size > 1) {
            pageBuilder = getLatestPB(pageBuilders, size, pageBuilder)
            pageBuilder.setSkipNumberLines(style.skipNumberLines)
            pageBuilder.setContinueSkip(true)
        }

        if (yBegin == -1 || xBegin == -1) {
            setXY(firstLineIndent, pageBuilder.y)
        }

        for (i in 0 until node.getChildCount()) {
            if (node.getChild(i) is Text) {
                val brl = getAssociatedBrlElement(node, i) ?: continue


                //If there's no more space left on the current line, move the y to the next line or next page
                if (pageBuilder.x == pageBuilder.cellsPerLine + pageBuilder.rightIndent) {
                    pageBuilder.pendingSpaces = 1
                    pageBuilders.addAll(pageBuilder.processSpacing())
                    if (pageBuilders.size > 1) {
                        lines!!.clear()
                        lineDetails!!.clear()
                        pageBuilder = pageBuilders.last()
                        pageBuilder.setSkipNumberLines(style.skipNumberLines)
                        pageBuilder.setContinueSkip(true)
                    }
                }

                val isPageIndicator = isPageIndicator(brl)
                if (isPageIndicator) {
                    size = pageBuilders.size
                    pageBuilders.addAll(handlePageIndicator(pageBuilder, brl, style, formatSelector))
                    pageBuilder = getLatestPB(pageBuilders, size, pageBuilder)
                    pageBuilder.setSkipNumberLines(style.skipNumberLines)
                    pageBuilder.setContinueSkip(true)
                    continue
                }

                var spaceCount = 0
                if (spaced && pageBuilder.pendingLinesBefore < 1 && !pageBuilder.checkBlankBefore()) {
                    val totalSpaces = 3 - spaces
                    pageBuilder.x += totalSpaces
                    spaced = false
                    spaceCount = spaces
                } else if (spaced) {
                    spaced = false
                    spaces = 0
                }

                lines!!.add(brl)
                lineDetails!!.add(
                    intArrayOf(
                        0, pageBuilder.pendingLinesBefore, spaceCount,
                        if (pageBuilder.isStartOfBlock()) 1 else 0
                    )
                )

                pageBuilders.addAll(pageBuilder.addBrl(brl))
                if (pageBuilders.size > 1) {
                    lines!!.clear()
                    lineDetails!!.clear()
                    pageBuilder = pageBuilders.last()
                    pageBuilder.setSkipNumberLines(style.skipNumberLines)
                    pageBuilder.setContinueSkip(true)
                    setXY(leftIndent, pageBuilder.y)
                }
            } else if (node.getChild(i) is Element) {
                val styleMap = formatSelector.styleMap
                val curStyle = styleMap.findValueOrDefault(node.getChild(i))
                (style as StyleStack).push(curStyle)

                if (isLineNum(node.getChild(i) as Element)) {
                    val lineNum = node.getChild(i) as Element


                    //Change the right indent, Add previous linenum, set current linenum
                    val newLineNum = lineNum.getAttributeValue("linenum")

                    var yDown = false

                    if (!pageBuilder.setLineNumberPos(pageBuilder.y)) {
                        //No more space, make new page
                        if (pageBuilder.y + 1 == pageBuilder.linesPerPage) {
                            pageBuilder.addAtLeastPages(1)
                            pageBuilder = pageBuilder.processSpacing().last()
                            pageBuilder.setLineNumberPos(1)
                        } else {
                            pageBuilder.y += 1
                            pageBuilder.setLineNumberPos(pageBuilder.lineNumberPos + 1)
                            yDown = true
                        }
                        pageBuilder.x = leftIndent
                    }

                    if (pageBuilder.x + 3 < pageBuilder.cellsPerLine + pageBuilder.rightIndent
                        && ((pageBuilder.x > pageBuilder.leftIndent) && firstAdd < 1)
                    ) {
                        if (lineNum.getAttribute("space") != null) {
                            spaces = lineNum.getAttributeValue("space").toInt()
                        }
                        spaced = true
                    } else if (firstAdd < 1) {
                        spaces = 0
                        spaced = false
                        if (!yDown) {
                            pageBuilder.setLineNumberPos(pageBuilder.y + pageBuilder.lineSpacing)
                        }
                    }

                    if (firstAdd == -1) {
                        firstAdd = 1
                    } else if (firstAdd == 1) {
                        firstAdd = 0
                    }

                    lines!!.add(pageBuilder.insertLineNumber(newLineNum))
                    lineDetails!!.add(
                        intArrayOf(
                            pageBuilder.lineNumberPos, pageBuilder.pendingLinesBefore,
                            lineNum.getAttributeValue("space").toInt(), if (pageBuilder.isStartOfBlock()) 1 else 0
                        )
                    )

                    pageBuilders.add(pageBuilder)


                    //If currLineNum is empty, set right indent with linenum length
                    //Else compare the two lengths. If new # is longer, readd
                    val rightIndent = (newLineNum.length + padding) * -1
                    if (newLineNum.length > currLineNum!!.length) {
                        pageBuilder = remove(pageBuilders)
                        pageBuilder = readd(pageBuilders, pageBuilder, style, formatSelector, rightIndent)
                        currLineNum = newLineNum
                    }
                } else if (!UTDElements.BRL.isA(node.getChild(i))) {
//					((StyleStack) style).push(curStyle);
                    size = pageBuilders.size
                    pageBuilders.addAll(
                        format(
                            node.getChild(i),
                            style,
                            pageBuilders,
                            formatSelector,
                            lines,
                            lineDetails
                        )
                    )
                    if (pageBuilders.size > size) {
                        pageBuilder = pageBuilders.last()
                        pageBuilder.setSkipNumberLines(style.getSkipNumberLines())
                        pageBuilder.setContinueSkip(true)
                        //						pageBuilder.setStartOfBlock(true);
                    }
                    //					((StyleStack) style).pop();
                }
                style.pop()
            }
        }

        return pageBuilders
    }

    private fun readd(
        pageBuilders: MutableSet<PageBuilder>, pb: PageBuilder, style: IStyle,
        formatSelector: FormatSelector, rightIndent: Int
    ): PageBuilder {
        var pageBuilder = pb
        pageBuilder.setRightIndent(rightIndent)
        pageBuilder.x = xBegin
        pageBuilder.y = yBegin

        for (i in lines!!.indices) {
            if (pageBuilder.x != pageBuilder.leftIndent && i < 1) {
                spaced = true
            }
            //If brl, readd line
            if (UTDElements.BRL.isA(lines!![i])) {
                //If the element before this one is not a line number, it shouldn't be spaced
                if ((i - 1) >= 0 && UTDElements.BRL.isA(lines!![i - 1])) {
                    spaced = false
                }

                val curStyle = findStyle(
                    formatSelector,
                    getAssociatedNode(
                        lines!![i]
                    )!!.parent
                )
                if (lineDetails!![i][3] == 1 && !pageBuilder.isEmptyFormattingLine) {
                    pageBuilder.addAtLeastLinesBefore(curStyle.linesBefore)
                    pageBuilder.setStartOfBlock(true)
                    pageBuilder.setFirstLineIndent(curStyle.firstLineIndent)
                    pageBuilder.setLeftIndent(safeUnbox(curStyle.indent, 0))
                    if (pageBuilders.size > 1) {
                        lines!!.clear()
                        lineDetails!!.clear()
                        pageBuilder = pageBuilders.last()
                        pageBuilder.setSkipNumberLines(style.skipNumberLines)
                        pageBuilder.setContinueSkip(true)
                    }
                }

                if (spaced && pageBuilder.pendingLinesBefore < 1 && i > 1 && !pageBuilder.checkBlankBefore()) {
                    val totalSpaces = 3 - spaces
                    pageBuilder.x += totalSpaces
                    spaced = false
                }

                pageBuilders.addAll(pageBuilder.addBrl(lines!![i]))
                if (pageBuilders.size > 1) {
                    pageBuilder = pageBuilders.last()
                    pageBuilder.setSkipNumberLines(style.skipNumberLines)
                    pageBuilder.setContinueSkip(true)
                }
            } else if (UTDElements.BRLONLY.isA(lines!![i])) {
                var yDown = false
                if (!pageBuilder.setLineNumberPos(pageBuilder.y)
                    && firstAdd > -1
                ) {
                    pageBuilder.y += 1
                    pageBuilder.x = safeUnbox(style.indent, 0)
                    pageBuilder.setLineNumberPos(pageBuilder.lineNumberPos + pageBuilder.lineSpacing)
                    yDown = true
                }
                if (pageBuilder.x + 3 < pageBuilder.cellsPerLine + pageBuilder.rightIndent) {
                    spaces = lineDetails!![i][2]
                    spaced = true
                } else if (i > 0) {
                    spaces = 0
                    //					spaced = false;
                    if (!yDown) {
                        pageBuilder.setLineNumberPos(pageBuilder.lineNumberPos + pageBuilder.lineSpacing)
                    }
                }
                pageBuilder.insertLineNumber(lines!![i].value)
            }
        }

        return pageBuilder
    }

    private fun remove(pageBuilders: Set<PageBuilder>): PageBuilder {
        for (line in lines!!) {
            if (!isSkipLinesNode(line)) {
                val iter = pageBuilders.iterator()
                while (iter.hasNext()) {
                    if (UTDElements.BRL.isA(line)) {
                        iter.next().removeBrl(line)
                    } else if (UTDElements.BRLONLY.isA(line)) {
                        val pb = iter.next()
                        pb.removeFormattingElement(line)
                        pb.setLineNumberPos(-1)
                    } else {
                        iter.next()
                    }
                }
            }
        }

        return pageBuilders.last()
    }

    private fun setXY(x: Int, y: Int) {
        xBegin = x
        yBegin = y
    }

    private fun isLineNum(element: Element): Boolean {
        return element.getAttribute("class") != null && element.getAttributeValue("class") == "linenum"
    }

    private fun getLatestPB(results: Set<PageBuilder>, origSize: Int, pageBuilder: PageBuilder): PageBuilder {
        if (results.size > origSize) {
            lines!!.clear()
            lineDetails!!.clear()
            return results.last()
        }
        return pageBuilder
    }

    private fun findStyle(formatSelector: FormatSelector, node: Node): IStyle {
        val styleMap = formatSelector.styleMap
        val curStyle = styleMap.findValueOrDefault(node)
        if (curStyle.name == "DEFAULT") {
            return findStyle(formatSelector, node.parent)
        }
        return curStyle
    }

    companion object {
        private fun safeUnbox(leftIndentBoxed: Int?, defaultValue: Int = 0): Int {
            return leftIndentBoxed ?: defaultValue
        }
    }
}

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
package org.brailleblaster.perspectives.braille.views.wp

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.abstractClasses.ViewUtils
import org.brailleblaster.bbx.BBX
import org.brailleblaster.libembosser.spi.BrlCell
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.*
import org.brailleblaster.perspectives.braille.mapping.maps.MapList
import org.brailleblaster.utd.properties.UTDElements

open class Renderer(@JvmField protected var manager: Manager, private val wpView: WPView) {
    protected var spaceBeforeText = 0
    protected var spaceAfterText = 0
    var insertPosition = 0
    protected var indicatorWidth: String
    protected var validator: Validator = Validator(manager, wpView.view)

    @JvmField
	var vPos: Double
    protected val finalLineOfPage: Int
    @JvmField
	protected var cell: BrlCell = manager.document.engine.brailleSettings.cellType
    protected var curBrlPage: Int
    protected var curBrlPageType: String? = null
    @JvmField
	protected var firstPage: NewPageBrlMapElement? = null
    @JvmField
	protected var lastPage: NewPageBrlMapElement? = null

    init {
        finalLineOfPage =
            cell.getLinesForHeight(manager.document.engine.pageSettings.drawableHeight.toBigDecimal() - cell.height)
        indicatorWidth = convertString()
        vPos = 0.0
        curBrlPage = 1
    }

    private fun convertString(): String {
        val line = StringBuilder()
        val width = wpView.view.bounds.width.toDouble()
        val cellsPerLine = cell.getCellsForWidth(width.toBigDecimal())
        line.append(" ".repeat(cellsPerLine.coerceAtLeast(0)))
        return line.toString()
    }

    /** checks if a moveTo follows a new page
     * @param n
     * @return
     */
    protected fun moveToAfterNewPage(n: Node): Boolean {
        val parent = n.parent as Element
        val index = parent.indexOf(n)
        return if (index > 1 && ViewUtils.isElement(parent.getChild(index - 2)) && ViewUtils.followsMoveTo(n)) {
            UTDElements.NEW_PAGE.isA(parent.getChild(index - 2))
        } else false
    }

    fun findVPos(list: MapList, index: Int): Double {
        var vPos = 0.0
        if (list.isNotEmpty()) {
            var i = index
            listLoop@ while (i + 1 < list.size) {
                if (list[i].brailleList.isNotEmpty()) break
                if (list[i] is TableTextMapElement) {
                    for (tcme in (list[i] as TableTextMapElement).tableElements) {
                        if (tcme.brailleList.isNotEmpty()) break@listLoop
                    }
                }
                i++
            }
            vPos = findVPos(list[i])
            if (i == 0 && manager.getSection(list[i].node) == 0) vPos = 0.0
            return vPos
        }
        return vPos
    }

    private fun findVPos(t: TextMapElement): Double {
        val vPos = 0.0
        for (i in t.brailleList.indices) {
            /*removed line to resolve ticket #4584,
			 * may be old outdated code due to old UTD formatting
			 * if other rendering issues occur,
			 * possibly re-insert and re-visit ticket 
			 * !isPageNum(t.brailleList.get(i).n) &&
			*/
            if (ViewUtils.followsMoveTo(t.brailleList[i].node) || ViewUtils.followsNewPage(t.brailleList[i].node)) return getVPos(
                getPrecedingElement(
                    t.brailleList[i].node
                )
            )
        }
        return vPos
    }

    protected fun getVPos(moveTo: Element?): Double {
        var vPos = 0.0
        val atr = moveTo!!.getAttribute("vPos")
        if (atr != null) vPos = atr.value.toDouble()
        return vPos
    }

    protected fun getHPos(moveTo: Element): Double {
        var hPos = 0.0
        val atr = moveTo.getAttribute("hPos")
        if (atr != null) hPos = atr.value.toDouble()
        return hPos
    }

    private fun getPrecedingElement(n: Node): Element? {
        val p = n.parent
        val index = p.indexOf(n)
        return if (index > 0 && p.getChild(index - 1) is Element) {
            p.getChild(index - 1) as Element
        } else null
    }

    fun setPageBounds(upper: NewPageBrlMapElement?, lower: NewPageBrlMapElement?) {
        firstPage = upper
        lastPage = lower
    }

    protected fun tabbed(t: TextMapElement): Boolean {
        return t.brailleList.isNotEmpty() && onSameLine(t) && followsTab(t.node)
    }

    protected fun isVolume(t: TextMapElement): Boolean {
        return t.nodeParent.localName.contains("Volume")
    }

    protected fun onSameLine(t: TextMapElement): Boolean {
        val b = t.brailleList.first()
        return if (b.vPos == vPos) {
            b.hPos != null && b.hPos > 0.0
        } else false
    }

    protected fun followsTab(n: Node): Boolean {
        val p = n.parent
        val index = p.indexOf(n)
        if (index > 0) {
            if (p.getChild(index - 1) is Element) {
                val e = p.getChild(index - 1) as Element
                return BBX.SPAN.TAB.isA(e)
            }
        }
        return false
    }

    protected fun addGuideDotsToTableRow(row: String, hPos: Double, dots: Node): String {
        val newRow = StringBuilder(row)
        val dotsLength = dots.value.length
        val hPosCells = cell.getCellsForWidth(hPos.toBigDecimal())
        val endPoint = hPosCells + dotsLength
        if (hPosCells > newRow.length - 1) {
            newRow.append(" ".repeat(hPosCells - newRow.length)).append("\"".repeat(dotsLength))
            return newRow.toString()
        }
        var lastSpace = -1
        var i = hPosCells
        while (i < newRow.length && i < endPoint) {
            if (newRow[i] == ' ') {
                if (i > 1 && newRow[i - 1] == ' ') {
                    lastSpace = i
                    break
                }
            }
            i++
        }
        if (lastSpace == -1) {
            return row
        }
        newRow.replace(lastSpace, endPoint, "\"".repeat(endPoint - lastSpace))
        return newRow.toString()
    }

    protected fun handleNewPageElement(offset: Int, newPage: Element?, view: WPView) {
        val line = view.view.getLineAtOffset(offset)
        val pageIndicator = PageIndicator(manager, line)
        if (view !is BrailleView) pageIndicator.findPageNums(newPage)
        view.paintedElements.add(pageIndicator)
        pageIndicator.startListener(view)
    }

    protected fun addLastPageIndicator(list: MapList, view: WPView) {
        val tme = getFinalNonWhiteSpaceElement(list)
        if (tme != null) {
            for (i in tme.brailleList.indices) {
                val bme = tme.brailleList[i]
                if (bme is BraillePageBrlMapElement) {
                    handleNewPageElement(view.view.charCount, bme.node as Element, view)
                }
            }
        }
    }

    protected fun renderLineIndents(lineIndents: List<Int>, view: WPView) {
        for ((j, lineIndent) in lineIndents.withIndex()) {
            if (lineIndent == 0) continue
            view.view.setLineIndent(j, 1, lineIndent * view.charWidth)
        }
    }

    fun addLineBreak(lb: LineBreakElement, state: RendererState?, list: MapList) {
        val index = list.indexOf(lb)
        if (index > 0 && list[index - 1] !is WhiteSpaceElement) {
            lb.isEndOfLine = true
        }
    }

    private fun getFinalNonWhiteSpaceElement(list: MapList): TextMapElement? {
        return list.lastOrNull { it !is FormattingWhiteSpaceElement }
    }
}

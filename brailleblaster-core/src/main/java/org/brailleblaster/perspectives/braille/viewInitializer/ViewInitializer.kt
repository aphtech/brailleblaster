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
package org.brailleblaster.perspectives.braille.viewInitializer

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.math.mathml.MathMLTableElement
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.document.BrailleDocument
import org.brailleblaster.perspectives.braille.mapping.elements.*
import org.brailleblaster.perspectives.braille.mapping.maps.MapList
import org.brailleblaster.perspectives.braille.views.wp.BrailleRenderer
import org.brailleblaster.perspectives.braille.views.wp.BrailleView
import org.brailleblaster.perspectives.braille.views.wp.TextRenderer
import org.brailleblaster.perspectives.braille.views.wp.TextView
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.util.WhitespaceUtils.countLineBreaks

abstract class ViewInitializer(val document: BrailleDocument, val text: TextView, val braille: BrailleView) :
    Initializer() {
    lateinit var viewList: MapList

    private var atEndOfDocument = false

    abstract fun initializeViews(m: Manager)

    protected fun makeList(m: Manager): MapList {
        viewList = MapList(m)

        for (sectionElement in sectionList) {
            if (sectionElement.isVisible) {
                viewList.addAll(sectionElement.list)
                viewList.setCurrent(viewList.indexOf(viewList.current))
            }
        }

        setWhiteSpace(m, viewList)

        return viewList
    }

    protected fun appendToViews(tr: TextRenderer, br: BrailleRenderer, list: MapList?, index: Int) {
        val firstNewPage = findFirstNewPage(list)
        val lastNewPage = findLastNewPage(list)
        tr.setPageBounds(firstNewPage, lastNewPage)
        br.setPageBounds(firstNewPage, lastNewPage)
        val count = list!!.size
        for (i in index until count) {
            tr.add(list[i], list)
            br.add(list[i])
        }
    }

    /**
     * Buffers views by including one section before or after the given index if greater than zero and list than
     * list size, method is used when either arrowing up or down, or scrolling up or down.  Including one section
     * before the desired index
     *
     * @param index : index to be included in new views
     * @return new maplist of views
     */
    fun bufferViews(manager: Manager, index: Int): MapList {
        var index = index
        braille.clearPageRange()
        if (sectionList.size > 1) {
            removeListeners()
            removeWhitespace(viewList)
            val t = sectionList[index].list.firstUsable


            if (index != 0) index--

            setViews(manager, t, index)
            initializeListeners()


            if (viewList.contains(t)) viewList.setCurrent(viewList.indexOf(t))
        }

        setWhiteSpace(manager, viewList)
        return viewList
    }

    //RT 6942
    fun bufferViews(manager: Manager, index: Int, forward: Boolean): MapList {
        var index = index
        braille.clearPageRange()
        if (sectionList.size > 1) {
            removeListeners()
            removeWhitespace(viewList)
            var t = sectionList[index].list.firstUsable
            if (forward) {
                val list = sectionList[index - 1].list
                for (tme in list) {
                    if (!tme.isFullyVisible) {
                        t = tme
                        break
                    }
                }
                if (index != 0) index--
            } else {
                val list = sectionList[index + 1].list
                for (tme in list) {
                    if (!tme.isFullyVisible) {
                        t = tme
                    }
                }
            }


            setViews(manager, t, index)
            initializeListeners()


            if (viewList.contains(t)) viewList.setCurrent(viewList.indexOf(t))
        }

        setWhiteSpace(manager, viewList)
        return viewList
    }

    /**
     * Buffers views by including one section before or after the given index if greter than zero and list than
     * list size, method is used when either arrowing up or down, or scrolling up or down.  Including one section
     * before the desired index
     *
     * @param index : index to be included in new views
     * @return new maplist of views
     */
    fun reformatViews(manager: Manager, index: Int): MapList {
        val textMapElements = sectionList[0].list
        if (sectionList.size > 1 || (sectionList.size == 1 && !textMapElements.isEmpty())) {
            val t = sectionList[index].list.first()
            removeWhitespace(viewList)
            setViews(manager, t, index)
            setWhiteSpace(manager, viewList)
        }
        return viewList
    }

    /**
     * Resets the views from the starting index, used to recreate a view when undoing events
     *
     * @param index: index of section to start from to recreate views
     * @return a new maplist
     */
    fun resetViews(manager: Manager, index: Int): MapList {
        if (sectionList.size > 1) {
            removeListeners()
            val t = sectionList[index].list.first()
            removeWhitespace(viewList)
            setViews(manager, t, index)
            setWhiteSpace(manager, viewList)
            initializeListeners()
        }

        return viewList
    }

    private fun findFirstNewPage(list: MapList?): NewPageBrlMapElement? {
        for (tme in list!!) {
            if (tme is TableTextMapElement) {
                for (tableElement in tme.tableElements) {
                    for (bme in tableElement.brailleList) {
                        if (bme is NewPageBrlMapElement) return bme
                    }
                }
            } else {
                for (bme in tme.brailleList) {
                    if (bme is NewPageBrlMapElement) return bme
                }
            }
        }
        return null
    }

    private fun findLastNewPage(list: MapList?): NewPageBrlMapElement? {
        if (atEndOfDocument) return null
        var prevElement: NewPageBrlMapElement? = null
        for (tme in list!!) {
            if (tme is TableTextMapElement) {
                for (tableElement in tme.tableElements) {
                    for (bme in tableElement.brailleList) {
                        if (bme is NewPageBrlMapElement) prevElement = bme
                    }
                }
            } else {
                for (bme in tme.brailleList) {
                    if (bme is NewPageBrlMapElement) prevElement = bme
                }
            }
        }
        return prevElement
    }

    private fun setViews(manager: Manager, t: TextMapElement, index: Int) {
        val startPos = findFirst()
        val endPos = findLast()

        if (!(startPos == -1 && endPos == -1)) clearViewList(startPos, endPos)

        clearViews()

        var i = index

        var totalChars = 0

        while (i < sectionList.size && (i < index + 2 || totalChars < CHAR_COUNT)) {
            viewList.addAll(sectionList[i].list)
            totalChars += sectionList[i].charCount
            sectionList[i].setInView(true)
            i++
        }

        resetMapListVisibility(viewList)
        atEndOfDocument = i == sectionList.size
        val tr = TextRenderer(manager, text)
        val br = BrailleRenderer(manager, braille)
        val vPos = tr.findVPos(viewList, 0)

        tr.vPos = vPos
        br.vPos = vPos
        appendToViews(tr, br, viewList, 0)

        tr.finishRendering(viewList)
        br.finishRendering(viewList)

        if (viewList.first() != t) {
            val textLine = text.view.getLineAtOffset(t.getStart(viewList))
            text.positionScrollbar(textLine)
            if (t is TableTextMapElement) {
                braille.positionScrollbar(
                    braille.view.getLineAtOffset(
                        t.tableElements[0].brailleList.first().getStart(
                            viewList
                        )
                    )
                )
            } else  /*
				Assuming this isn't needed as views should line up
				braille.positionScrollbar(braille.view.getLineAtOffset(t.brailleList.getFirst().getStart(viewList)));
				*/
                braille.positionScrollbar(textLine)
        }

        manager.onPostBuffer(viewList)
    }

    private fun resetMapListVisibility(viewList: MapList?) {
        for (tme in viewList!!) {
            tme.isFullyVisible = true
        }
    }

    private fun clearViewList(startPos: Int, endPos: Int) {
        for (i in startPos..endPos) {
            viewList.removeAll(sectionList[i].list)
            sectionList[i].resetList()
        }
    }

    private fun clearViews() {
        replaceTextRange(0, text.view.charCount, 0, braille.view.charCount)
    }

    private fun replaceTextRange(textStart: Int, textLength: Int, brailleStart: Int, brailleLength: Int) {
        text.view.replaceTextRange(textStart, textLength, "")
        braille.view.replaceTextRange(brailleStart, brailleLength, "")
    }

    private fun removeListeners() {
        text.removeListeners()
        braille.removeListeners()
        //	treeManager.removeListeners();
    }

    private fun initializeListeners() {
        text.initializeListeners()
        braille.initializeListeners()
        //		treeManager.initializeListeners();
    }

    val startIndex: Int
        get() = findFirst()

    fun findFirst(): Int {
        for (i in sectionList.indices) {
            if (sectionList[i].isVisible) return i
        }

        return -1
    }

    fun findLast(): Int {
        var position = -1
        for (i in sectionList.indices) {
            if (sectionList[i].isVisible) position = i
        }

        return position
    }

    fun remove(list: MapList, pos: Int) {
        val start = findFirst()
        val t = list[pos]
        for (i in start until sectionList.size) {
            if (sectionList[i].list.contains(t)) {
                sectionList[i].list.remove(t)
                break
            }
        }

        list.removeAt(pos)
    }

    private fun setWhiteSpace(m: Manager, list: MapList) {
        checkContractions(list)
        setImagePlaceholder(list)
        setBlankLines(list)
    }

    /**
     * Contractions in braille across inline elements result in empty brl nodes
     * THis methods checks list and sets braille list to reference the contraction
     * which occurs before it.
     *
     * @param list
     */
    private fun checkContractions(list: MapList) {
        for (i in list.indices) {
            val t = list[i]
            if (t.brailleList.isEmpty() && t.node != null && t.text.isNotEmpty()) {
                var index = i
                while (index > 0 && t.brailleList.isEmpty()) {
                    index--
                    val ref = list[index]
                    if (!ref.brailleList.isEmpty()) t.brailleList.add(ref.brailleList.last())
                }
            }
        }
    }

    private fun setImagePlaceholder(list: MapList) {
        for (i in list.indices) {
            val t = list[i]
            if (t is ImagePlaceholderTextMapElement) {
                var prev = list.getPrevious(i, false)
                while (prev != null && prev.getEnd(list) < 0) {
                    prev = list.getPrevious(list.indexOf(prev), false)
                }

                var next = list.getNext(i, true)
                while (next != null && next.getStart(list) < 0) {
                    next = list.getNext(list.indexOf(next), true)
                }

                var start = if (prev == null) 0 else prev.getEnd(list) + breakLength
                var end = if (next == null) text.view.charCount else next.getStart(list) - breakLength
                if (next != null) {
                    //Subtract the end of the image placeholder by the number of line breaks between
                    //it and the following non-whitespace TME
                    end -= (countLineBreaks(t, next, list, false) * breakLength)
                }
                //This can happen when two image placeholders are next to each other (the second one will get ignored)
                if (start > end) {
                    start = end
                }
                t.setOffsets(start, end)
            }
        }
    }

    private fun setBlankLines(list: MapList) {
        if (!list.isEmpty()) {
            //Is the first TME farther down the page?
            if (list[0].getStart(viewList) > 0) {
                val end = list[0].getStart(list)
                val brailleStart = 0
                addWhiteSpaceElements(0, 0, end, brailleStart)
            }
        }
        for (i in 0 until list.size - 1) {
            //Fill in the difference between this tme and the next with whitespace
            val start = list[i].getEnd(list)
            val end = list[i + 1].getStart(list)
            val brailleStart = getBrailleEnd(list, i) + breakLength
            if (end - start > 1) {
                //If the two TMEs are on the same line, make this a Horizontal WSE
                if (//Safety check
                    start < end && end > 0 && start > 0 && end < text.view.charCount && !text.view.getText(start, end)
                        .contains(
                            System.lineSeparator()
                        )
                ) { //Check for newline between TMEs
                    val hwse = HorizontalFormattingWhiteSpaceElement(start, end)
                    list.add(i + 1, hwse)
                } else {
                    addWhiteSpaceElements(i + 1, start + breakLength, end, brailleStart)
                }
            }
        }

        val t = list.last()
        var i = t.getEnd(list) + breakLength
        while (i <= text.view.charCount) {
            val wse = FormattingWhiteSpaceElement(i, i)
            wse.brailleList.add(BrailleWhiteSpaceElement(braille.view.charCount, braille.view.charCount))
            list.add(wse)
            i += breakLength
        }
    }

    private fun getBrailleEnd(list: MapList?, index: Int): Int {
        var index = index
        var t = list!![index]
        if (t is TableTextMapElement && t !is MathMLTableElement) {
            if (t.brailleList.isEmpty()) {
                if (index == 0) return 0
                return getBrailleEnd(list, index - 1)
            }
            return t.tableElements[t.tableElements.size - 1].brailleList.last().getEnd(list)
        } else {
            while (t.brailleList.isEmpty()) {
                index -= 1
                if (index < 0) return 0
                t = list[index]
            }
            try {
                return t.brailleList.last().getEnd(list)
            } catch (e: Exception) {
                throw NodeException("Failed to get braille from brailleList, tme node: ", t.node, e)
            }
        }
    }

    private fun removeWhitespace(list: MapList) {
        var i = 0
        while (i < list.size) {
            if (list[i] is WhiteSpaceElement) {
                list.removeAt(i)
                i--
            }
            i++
        }
    }

    /**
     * Add WhiteSpaceMapElements at index i, going from offsets start to end, at braille view
     * offset brailleStart
     */
    private fun addWhiteSpaceElements(index: Int, start: Int, end: Int, brailleStart: Int) {
        var index = index
        var start = start
        var brailleStart = brailleStart
        while (start < end) {
            val curElement = if (index < viewList.size) viewList[index] else null
            if (curElement !is LineBreakElement) {
                if (checkUneditableWhiteSpace(index)) {
                    val wse = ReadOnlyFormattingWhiteSpace(start, start)
                    wse.brailleList.add(BrailleWhiteSpaceElement(brailleStart, brailleStart))
                    viewList.add(index, wse)
                } else {
                    val wse = FormattingWhiteSpaceElement(start, start)
                    wse.brailleList.add(BrailleWhiteSpaceElement(brailleStart, brailleStart))
                    viewList.add(index, wse)
                }
            }
            start += breakLength
            index++
            brailleStart += breakLength
        }
    }

    private fun checkUneditableWhiteSpace(tmeIndex: Int): Boolean {
        if (tmeIndex >= viewList.size || viewList.getPrevious(tmeIndex, true) == null) {
            return false
        }
        val prevTME = viewList.getPrevious(tmeIndex, true)
        val curTME = viewList[tmeIndex]
        return (prevTME is ReadOnlyTableTNTextMapElement
                && curTME is ReadOnlyTableTextMapElement)
    }

    protected open fun findSections(m: Manager, e: Element) {
        val list = MapList(m)
        sectionList.add(SectionElement(list, 0))
        initializeViews(e, m, 0)
    }

    fun initializeMap(m: Manager) {
        findSections(m, document.rootElement)
        viewList = MapList(m)
    }

    /**
     * Searches section maplist for node
     *
     * @param n node to find
     * @return a pair containing section(left in pair) and index(right in pair)
     */
    fun findSection(n: Node?): Pair<Int, Int>? {
        val searcher = SectionMapSearcher(sectionList)
        return searcher.search(n)
    }

    companion object {
        private val lineBreak: String = System.lineSeparator()
        private val breakLength = lineBreak.length
    }
}

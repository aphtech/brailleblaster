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
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.math.mathml.MathMLElement
import org.brailleblaster.math.mathml.MathMLTableElement
import org.brailleblaster.math.mathml.MathModule
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.*
import org.brailleblaster.perspectives.braille.mapping.maps.MapList
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utils.xom.childNodes
import org.brailleblaster.util.Utils
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyleRange
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.widgets.Display
import java.util.function.Consumer

class TextRenderer(manager: Manager, private val textView: TextView) : Renderer(manager, textView) {
    var state: RendererState = RendererState(manager.simpleManager.utdManager.engine)
    var emphasisList: List<TextMapElement> = ArrayList()
    private val lineNumberList: MutableList<Pair<Int, LineNumberBrlMapElement>> = ArrayList()
    private var pastFirstNewPage: Boolean
    private var pastLastNewPage: Boolean
    private var lastTab: TabTextMapElement? = null
    private var lastPageBreak: PageBreakWhiteSpaceElement? = null

    init {
        pastFirstNewPage = false
        pastLastNewPage = false
    }

    fun add(t: TextMapElement, list: MapList) {
        when (t) {
            is TableTextMapElement -> {
                addTable(t, list)
            }

            is LineBreakElement -> {
                if (!pastFirstNewPage || pastLastNewPage) {
                    t.isFullyVisible = false
                } else {
                    addLineBreak(t, state, list)
                }
                return
            }

            is TabTextMapElement -> {
                addTab(t)
            }

            is PageBreakWhiteSpaceElement -> {
                addPageBreak(t, list)
            }

            is ImagePlaceholderTextMapElement, is WhiteSpaceElement -> {
                //Do not set their offsets yet
                return
            }

            else -> {
                appendToView(t, list)
            }
        }

        //Now that we're done processing the node, set the end offset of the textmapelement
        t.setEnd(state.charCount)
        //Edge case where TME had no braillelist
        if (t.getStart(list) == TextMapElement.NOT_SET) {
            t.setStart(t.getEnd(list))
        }
    }

    fun appendToView(t: TextMapElement, list: MapList) {
        val n = t.node
        var brl = getBrlNode(n)
        if (brl == null) {
            brl = when (t) {
                is BoxLineTextMapElement if UTDElements.BRL.isA(t.node) -> {
                    t.node as Element
                }

                is GuideDotsTextMapElement if UTDElements.BRLONLY.isA(t.node) -> {
                    t.getNodeParent()
                }

                is MathMLElement -> {
                    MathModule.getBrl(n) as Element
                }

                is PageIndicatorTextMapElement -> {
                    t.node.childNodes.filterIsInstance<Element>().first { UTDElements.BRL.isA(it) }
                }

                is UncontractedWordTextMapElement if UTDElements.BRLONLY.isA(t.node) -> {
                    t.getNodeParent()
                }

                else -> {
                    throw NullPointerException("No brl found")
                }
            }
        }
        brl?.let { renderBrailleList(t, it, list) }
    }

    fun addTable(t: TableTextMapElement, list: MapList) {
        state.setTableMode(true)
        t.tableElements.forEach(Consumer { tcme: TableCellTextMapElement -> appendToView(tcme, list) })
        state.setTableMode(false)
    }

    fun addTab(tab: TabTextMapElement) {
        lastTab = tab
        tab.setStart(state.charCount)
    }

    fun addPageBreak(pb: PageBreakWhiteSpaceElement, list: MapList) {
        if (list.indexOf(pb) == 0) {
            pb.setStart(0)
            lastPageBreak = pb
            return
        }
        if (lastTab != null) {
            //If a tab preceded this page break, abandon it here
            lastTab!!.setOffsets(state.charCount, state.charCount)
            lastTab = null
        }
        var index = list.indexOf(pb) - 1
        while (index > 0 && list[index] is PaintedWhiteSpaceElement) {
            index--
        }
        pb.setStart(list[index].getEnd(list) + System.lineSeparator().length)
        lastPageBreak = pb
    }

    /**
     * Set StyledText widget's text and wrap up rendering code that
     * must run after the StyledText content has been set
     */
    fun finishRendering(list: MapList?) {
        state.finishPage()
        textView.view.text = state.text
        renderLineIndents(state.lineIndents, textView)
        renderEmphasis()
        renderNewPages()
        renderLineNumbers()
        addLastPageIndicator(list!!, textView)
    }

    private fun renderBrailleList(t: TextMapElement, brl: Element, list: MapList) {
        var start = 0
        var end = 0
        var totalLength = 0
        var firstText = true //Catch edge case where text node begins with spaces
        //Get the index attribute value out of the brl node. This allows us to line up brl characters
        //with print characters
        val indexes = getIndexArray(brl)
        val emphasisNode = if (UTDElements.BRL.isA(t.node)) null else getEmphasisNode(t.node)
        val brlTextLength = brl.childNodes.filterIsInstance<Text>().sumOf { it.value.length }
        var mathAdded = false
        if (t.brailleList.isEmpty()) {
            state.addToLine(t.text)
        } else {
            for (bme in t.brailleList) {
                when (bme) {
                    is NewPageBrlMapElement -> {
                        if (bme === firstPage) pastFirstNewPage = true
                        if (bme === lastPage) pastLastNewPage = true
                        //At a newPage element, so adjust the state to create a new page and blank lines
                        state.newPage(bme.node as Element)
                    }

                    is PrintPageBrlMapElement, is BraillePageBrlMapElement, is RunningHeadBrlMapElement, is GuideWordBrlMapElement -> {
                        //These don't get rendered into the view, they are painted
                        //Issue #4950: RunningHead and GuideWord ensure next TME's start offset is correct
                        continue
                    }

                    is LineNumberBrlMapElement -> {
                        //Line numbers can stop here because they don't need their moveTo's to be checked
                        handleBrlOnlyMapElement(t, bme)
                        continue
                    }

                    else -> {
                        if (!pastFirstNewPage) {
                            if (t.node is Text
                                && bme !is BrlOnlyBrlMapElement
                            ) { //BrlOnly doesn't count towards indexes
                                totalLength += bme.text.length
                                if (indexes != null) {
                                    //Do not render anything that comes before this, but update
                                    //the indexes appropriately
                                    end = indexes[totalLength - 1] + 1
                                    //Add nonrendered portion to the invisible text of the TME
                                    if (totalLength == indexes.size) {
                                        t.appendInvisibleText(t.node.value.substring(start))
                                    } else {
                                        t.appendInvisibleText(t.node.value.substring(start, end))
                                    }
                                    start = end
                                }
                            }
                            t.isFullyVisible = false
                            continue
                        }
                        if (pastLastNewPage) {
                            t.isFullyVisible = false
                            if (lastPageBreak != null) {
                                lastPageBreak!!.setEnd(state.charCount)
                                lastPageBreak = null
                            }
                            if (indexes != null) {
                                //Add the rest of the text to the TME's invisible text
                                if (totalLength != indexes.size) {
                                    t.appendInvisibleText(t.node.value.substring(end))
                                }
                                //Only do the above line once
                                totalLength = indexes.size
                            }
                            continue
                        }
                        checkForMoveTo(t, brl, bme.node, list)
                        if (t.getStart(list) == TextMapElement.NOT_SET) {
                            //Commenting this out for now. There doesn't seem to be a reason to not include
                            //the pending spaces here but making a note in case an issue arises. Fixes RT 6134
                            //					t.setStart(state.getCharCount());
                            t.setStart(state.charCountWithPendingSpaces)
                        }
                        if (emphasisNode != null) {
                            //Add emphasis to a list to be iterated through after the text has been set in the view
                            state.applyEmphasis(getEmphasisNode(t.node))
                        }
                        //Check if the brl needs special handling
                        if (bme is BrlOnlyBrlMapElement) {
                            handleBrlOnlyMapElement(t, bme)
                        } else {
                            //This is a normal brl node that needs no special handling
                            totalLength += bme.text.length
                            if (t is MathMLElement || t is MathMLTableElement) {
                                if (!mathAdded) {
                                    state.addToLine(t.text)
                                    mathAdded = true
                                } else {
                                    state.applyEmphasis(null)
                                }
                            } else if (indexes != null) {
                                if (firstText) {
                                    //Account for spaces at beginning of text node that was stripped from brl
                                    var i = 0
                                    while (i < t.text.length && bme.text.isNotEmpty()) {
                                        val brlMatchesIndexesLength = indexes.size > brlTextLength
                                        if (t.text[i] == ' ' && bme.text[0] != ' ' && brlMatchesIndexesLength) {
                                            totalLength++
                                        } else {
                                            break
                                        }
                                        i++
                                    }
                                    firstText = false
                                }
                                //Find what this brl text node is in the print text node
                                end = indexes[totalLength - 1] + 1
                                val lineContent = if (totalLength == indexes.size) {
                                    t.node.value.substring(start)
                                } else {
                                    t.node.value.substring(start, end)
                                }
                                state.addToLine(lineContent)
                            }
                            start = end
                        }
                    }
                }
            }
        }
    }

    private fun checkForMoveTo(t: TextMapElement, brl: Element, brlText: Node, list: MapList) {
        val brlIndex = brl.indexOf(brlText)
        //Check if the text node is preceded by a MoveTo
        if (brlIndex > 0 && UTDElements.MOVE_TO.isA(brl.getChild(brlIndex - 1))) {
            val moveTo = brl.getChild(brlIndex - 1) as Element
            if (t is TableCellTextMapElement) {
                //We need to adjust the spacing of the table to look correct in print
                adjustTableSpacing(t, moveTo, list)
            } else {
                //!!Special case where if it's a line number, you shouldn't bother moving the hpos.
                val index = brl.parent.indexOf(brl)
                if (index - 2 > 0 && BBX.SPAN.PROSE_LINE_NUMBER.isA(brl.parent.getChild(index - 2))) {
                    state.setLineNumber(true)
                }
                if (Utils.vPosToLines(manager, getVPos(moveTo)) != state.y) {
                    onNewLine(moveTo, list)
                }
                state.moveTo(getHPos(moveTo), getVPos(moveTo))
                if (lastTab != null && lastPageBreak != null) {
                    //If a page break preceded this tab, place the Tab at the beginning
                    //of the page
                    lastTab!!.setOffsets(state.charCount, state.charCount)
                    lastTab = null
                }
            }
            if (lastTab != null) {
                //Tab is in the middle of an element
                lastTab!!.setEnd(state.charCountWithPendingSpaces)
                if (t.getStart(list) < lastTab!!.getEnd(list)) {
                    t.setStart(lastTab!!.getEnd(list))
                }
                lastTab = null
            }
            if (lastPageBreak != null) {
                setLastPageBreakPos(list)
            }
        }
    }

    /**
     * Set the end offset of the previous PageBreakWhiteSpaceElement.
     */
    private fun setLastPageBreakPos(list: MapList) {
        val index = list.indexOf(requireNotNull(lastPageBreak) { "No last page break." })
        require(index != -1) { "PageBreak not in MapList" }
        //Count the number of line breaks that follow this page break
        val followingLineBreaks: MutableList<LineBreakElement> = ArrayList()
        var lineBreakIndex = index
        while (lineBreakIndex + 1 < list.size) {
            if (list[lineBreakIndex + 1] is LineBreakElement) {
                followingLineBreaks.add(list[lineBreakIndex + 1] as LineBreakElement)
                lineBreakIndex++
            } else {
                break
            }
        }
        //Reduce the end of the page break by the number of line breaks that follow it
        val pageBreakEnd =
            state.charCount - System.lineSeparator().length - followingLineBreaks.size * System.lineSeparator().length
        lastPageBreak!!.setEnd(pageBreakEnd)
        if (lastPageBreak!!.getEnd(list) <= lastPageBreak!!.getStart(list)) {
            lastPageBreak!!.setEnd(lastPageBreak!!.getStart(list))
        }
        lastPageBreak = null
    }

    //If we're inside a table, we need to adjust the place we're moving to
    //so that we leave two blank spaces between each column
    private fun adjustTableSpacing(t: TableCellTextMapElement, moveTo: Element, list: MapList) {
        val column = t.col
        val parent = t.parentTableMapElement
        checkTableStartOffsets(t, moveTo, list)
        var colWidth = 0
        for (col in 0 until column) {
            colWidth += parent.columns[col].printWidth
            colWidth += 2
        }
        state.moveTo(cell.getWidthForCells(colWidth).toDouble(), getVPos(moveTo))
    }

    private fun checkTableStartOffsets(t: TableCellTextMapElement, moveTo: Element, list: MapList) {
        state.moveTo(0.0, getVPos(moveTo))
        val charCountAtOffset = state.getCharCountToVPos(cell.getLinesForHeight(getVPos(moveTo).toBigDecimal()))
        if (t.parentTableMapElement.getStart(list) == TextMapElement.NOT_SET || t.parentTableMapElement.getStart(list) > charCountAtOffset) {
            t.parentTableMapElement.setStart(charCountAtOffset)
        }
    }

    private fun handleBrlOnlyMapElement(tme: TextMapElement, bme: BrailleMapElement) {
        if (bme is LineNumberBrlMapElement) {
            //This is a line number that should be painted onto the view
            lineNumberList.add((state.charCount) to bme)
        } else if (bme is GuideWordBrlMapElement || bme is RunningHeadBrlMapElement) {
            //Skip guide words and running heads, they are painted on by PageIndicator
        } else if (bme is BoxLineBrlMapElement || bme is PageIndicatorBrlMapElement) {
            state.addToLine(tme.text)
        } else if (tme is TableCellTextMapElement) {
            //This is the guide dots in simple tables.
            //TODO: Handle this
        } else {
            state.addToLine(bme.text)
        }
    }

    /**
     * Change any state that needs to be changed before moving on to a new line. Does not trigger on new pages.
     * For right now, this only concerns TabTextMapElements
     * @param moveTo
     */
    private fun onNewLine(moveTo: Element, list: MapList) {
        if (lastTab != null) {
            lastTab!!.setStart(state.charCount)
            if (Utils.vPosToLines(manager, getVPos(moveTo)) == state.y + 1) {
                //If moving to the following line, set the end to be after the line break
                lastTab!!.setEnd(state.charCount + System.lineSeparator().length)
            } else {
                lastTab!!.setEnd(lastTab!!.getStart(list))
            }
            lastTab = null
        }
    }

    private fun getEmphasisNode(node: Node): Element? {
        var parent = node.parent
        while (!BBX.BLOCK.isA(parent) && !BBX.SECTION.isA(parent)) {
            if (BBX.INLINE.EMPHASIS.isA(parent) || BBX.INLINE.MATHML.isA(parent)) {
                return parent as Element
            }
            parent = parent.parent
        }
        return null
    }

    /**
     * Gets index attribute from brl element and converts to integer array
     *
     * @param e
     * : Brl element to retrieve indexes
     * @return : Integer Array containing brl indexes
     */
    private fun getIndexArray(e: Element): IntArray? {
        val atr = e.getAttribute("index")
        if (atr != null) {
            val arr = atr.value
            if (arr.isNotEmpty()) {
                val tokens = arr.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val indexArray = IntArray(tokens.size)
                for (i in tokens.indices) indexArray[i] = tokens[i].toInt()
                return indexArray
            }
        }
        return null
    }

    /**
     * Finds the corresponding braille node of a standard text node
     *
     * @param n
     * : node to check
     * @return braille element if markup is correct, null if braille cannot be
     * found
     */
    private fun getBrlNode(n: Node): Element? {
        val index: Int
        //		if (MathModule.isMath(n)){
//			e = (Element) n.getParent();
//			index = e.indexOf(n);
//			if (index != e.getChildCount() - 1) {
//				if(UTDElements.BRL.isA(e.getChild(index + 1)))
//					return (Element) e.getChild(index + 1);
//			}
//		}
//		else {
        val e: Element = n.parent as Element
        index = e.indexOf(n)
        if (index != e.childCount - 1) {
            if (UTDElements.BRL.isA(e.getChild(index + 1))) return e.getChild(index + 1) as Element
        }
        //		}
        return null
    }

    private fun renderEmphasis() {
        textView.ranges.clear()
        val emphasisList = state.emphasis
        for (e in emphasisList) {
            if (BBX.INLINE.MATHML.isA(e.inlineNode)) {
                textView.addMathHighlights(e.start, e.end - e.start, e.inlineNode)
            } else {
                textView.setFontStyleRange(e.start, e.end - e.start, manager.getAction(e.inlineNode), e.inlineNode)
            }
        }
        //Convert list to array
        val rangeArray = arrayOfNulls<StyleRange>(textView.ranges.size)
        for (i in textView.ranges.indices) {
            rangeArray[i] = textView.ranges[i]
        }
        textView.view.styleRanges = rangeArray

        // Highlight nbsp. Loop to prevent out of order errors or conflicts with existing style rnages
        for (curStyleRange in setNonBreakingSpaceEmphasis(
            textView.view, 0, textView.view.charCount
        )) {
            textView.view.setStyleRange(curStyleRange)
        }
    }

    private fun renderNewPages() {
        val newPages = state.newPages
        for (pair in newPages) {
            handleNewPageElement(pair.first, pair.second, textView)
        }
    }

    private fun renderLineNumbers() {
        for (lineNumber in lineNumberList) {
            handleLineNumber(lineNumber.second, lineNumber.first)
        }
    }

    private fun handleLineNumber(g: LineNumberBrlMapElement, insertPos: Int) {
        textView.paintedElements.add(g)
        g.startListener(manager, textView, insertPos)
    }

    companion object {
        const val NON_BREAKING_SPACE = '\u00A0'
        const val NON_BREAKING_SPACE_COLOR = SWT.COLOR_BLACK

        /**
         * Handle highlighting non breaking spaces \u0080
         *
         * @param view
         * @param viewTextStart
         * @param viewTextLength
         */
        fun setNonBreakingSpaceEmphasis(view: StyledText, viewTextStart: Int, viewTextLength: Int): Array<StyleRange> {
            val results: MutableList<StyleRange> = ArrayList()

            // Don't use t.getText() as it won't contain line wrapping newlines
            val text = view.getTextRange(viewTextStart, viewTextLength)
            // logger.trace("text: '{}'", text);
            var start = 0
            while (text.indexOf('\u00A0', start).also { start = it } != -1) {
                var len = 1
                while (text.length > start + len && text[start + len] == NON_BREAKING_SPACE) len++
                val nbspRange = StyleRange()
                nbspRange.start = viewTextStart + start
                nbspRange.length = len
                setNonBreakingSpaceStyleRange(nbspRange)
                results.add(nbspRange)
                // logger.trace("Added nbsp range from {}/{} len {}",
                // nbspRange.start, start, nbspRange.length);
                start += len
            }
            return results.toTypedArray<StyleRange>()
        }

        @JvmStatic
		fun setNonBreakingSpaceStyleRange(sr: StyleRange) {
            //nbspRange.background = Display.getCurrent().getSystemColor(NON_BREAKING_SPACE_COLOR);
            sr.borderColor = Display.getCurrent().getSystemColor(NON_BREAKING_SPACE_COLOR)
            sr.borderStyle = SWT.BORDER_SOLID
        }
    }
}

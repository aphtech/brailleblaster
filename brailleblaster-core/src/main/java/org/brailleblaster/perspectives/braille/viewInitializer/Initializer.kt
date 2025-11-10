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
import nu.xom.Text
import org.brailleblaster.abstractClasses.ViewUtils.followsMoveTo
import org.brailleblaster.abstractClasses.ViewUtils.followsNewPage
import org.brailleblaster.abstractClasses.ViewUtils.isElement
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.findBlockOrNull
import org.brailleblaster.bbx.isPageNum
import org.brailleblaster.math.mathml.MathMLElement
import org.brailleblaster.math.mathml.MathMLTableElement
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.*
import org.brailleblaster.perspectives.braille.mapping.elements.BrlOnlyBMEFactory.createBrlOnlyBME
import org.brailleblaster.perspectives.braille.mapping.maps.MapList
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.UTDHelper.getAssociatedBrlElement
import org.brailleblaster.utils.xml.UTD_NS
import org.brailleblaster.utils.xom.childNodes
import org.slf4j.Logger
import org.slf4j.LoggerFactory

open class Initializer {

    var sectionList: ArrayList<SectionElement> = ArrayList()
        protected set
    private var chars = 0
    private var block: Node? = null
    private var lastGuideWord: GuideWordBrlMapElement? = null
    private val log: Logger = LoggerFactory.getLogger(Initializer::class.java)

    private var inTable = false
    private var readOnly = false
    private var inSpatialMathGrid = false
    private var readOnlyTableParent: Element? = null

    protected fun initializeViews(current: Node, m: Manager, index: Int) {
        if (current is Text && !UTDElements.BRL.isA(current.parent)
            && vaildTextElement(current, current.value)
        ) {
            if (inTable) {
                addToTable(m, TableCellTextMapElement(current))
            } else if (readOnlyTableParent != null) {
                if (BBX.CONTAINER.TABLETN.isA(readOnlyTableParent)) {
                    add(m, ReadOnlyTableTNTextMapElement(current))
                } else {
                    add(m, ReadOnlyTableTextMapElement(current))
                }
            } else if (readOnly) {
                add(m, ReadOnlyTextMapElement(current))
            } else {
                add(m, TextMapElement(current))
            }
        }

        var i = index
        while (i < current.childCount) {
            if (BBX.CONTAINER.SPATIAL_GRID.isA(current.getChild(i))) {
                readOnly = true
                inSpatialMathGrid = true //TODO why doesn't this render when true
                initializeViews(current.getChild(i), m, 0)
                inSpatialMathGrid = false
                readOnly = false
            } else if (BBX.CONTAINER.NUMBER_LINE.isA(current.getChild(i))) {
                if (inSpatialMathGrid) {
                    i++
                    continue
                }
                readOnly = true
                initializeViews(current.getChild(i), m, 0)
                readOnly = false
            } else if (BBX.CONTAINER.MATRIX.isA(current.getChild(i))) {
                if (inSpatialMathGrid) {
                    i++
                    continue
                }
                readOnly = true
                initializeViews(current.getChild(i), m, 0)
                readOnly = false
            } else if (BBX.CONTAINER.TEMPLATE.isA(current.getChild(i))) {
                if (inSpatialMathGrid) {
                    i++
                    continue
                }
                readOnly = true
                initializeViews(current.getChild(i), m, 0)
                readOnly = false
            } else if (current.getChild(i) is Element) {
                if (BBX.SPAN.TAB.isA(current.getChild(i))) {
                    val tab = if (readOnly) {
                        ReadOnlyTabTextMapElement(current.getChild(i))
                    } else {
                        TabTextMapElement(current.getChild(i))
                    }
                    add(m, tab)
                    if (current.getChild(i).childCount > 0 && UTDElements.BRL.isA(current.getChild(i).getChild(0))) {
                        tab.brailleList.add(BrailleMapElement(current.getChild(i).getChild(0)))
                    }
                    //The following code fixes an issue with two adjacent Tab elements. If it adds a tab immediately after having already
                    //added a tab (not counting whitespace), delete the previous tab.
                    val curList = sectionList[sectionList.size - 1].list
                    if (curList.getPrevious(curList.size - 1, true) != null && curList.getPrevious(
                            curList.size - 1,
                            true
                        ) is TabTextMapElement
                    ) {
                        val prevTab = curList.getPrevious(curList.size - 1, true) as TabTextMapElement
                        if (current === prevTab.nodeParent) {
                            i--
                        }
                        prevTab.node.detach()
                        curList.remove(prevTab)
                    }
                } else if ((BBX.CONTAINER.TABLE.isA(current.getChild(i)) && i != current.childCount - 1 && current.getChild(
                        i + 1
                    ) is Element
                            && BBX.CONTAINER.TABLE.isA(current.getChild(i + 1))) && (current.getChild(i + 1) as Element).getAttribute(
                        "class"
                    ) != null && (current.getChild(i + 1) as Element).getAttributeValue("class").contains("utd:table")
                ) {
                    i++
                    continue  //Skip original table
                } else if (BBX.CONTAINER.TABLE.isA(current.getChild(i)) && (current.getChild(i) as Element).getAttribute(
                        "class"
                    ) != null && (current.getChild(i) as Element).getAttributeValue("class").contains("utd:tableSimple")
                ) {
                    inTable = true
                    val newTableMap = TableTextMapElement(current.getChild(i))
                    add(m, newTableMap)
                    initializeViews(current.getChild(i), m, 0)
                    newTableMap.setLines(m)
                    inTable = false
                } else if ((BBX.CONTAINER.TABLE.isA(current.getChild(i)) && isNotSimpleTable(current.getChild(i) as Element)) || BBX.CONTAINER.TABLETN.isA(
                        current.getChild(i)
                    )
                ) {
                    readOnlyTableParent = current.getChild(i) as Element
                    initializeViews(current.getChild(i), m, 0)
                    readOnlyTableParent = null
                } else if (UTDElements.BRL.isA(current.getChild(i))) {
                    try {
                        if (isBoxLine((current.getChild(i) as Element))) initializeBoxline(
                            m,
                            current.getChild(i) as Element
                        )
                        else {
                            if (sectionList[sectionList.size - 1].list.isEmpty()) {
                                i++
                                continue
                            }
                            if (!inTable) initializeBraille(
                                m,
                                sectionList[sectionList.size - 1].list.last(),
                                current.getChild(i) as Element
                            )
                            else {
                                val lastElement = sectionList[sectionList.size - 1].list.last() as TableTextMapElement
                                if (lastElement.tableElements.isNotEmpty()) {
                                    initializeBraille(
                                        m,
                                        lastElement.tableElements[lastElement.tableElements.size - 1],
                                        current.getChild(i) as Element
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        throw NodeException("Failed to init braille", current.getChild(i), e)
                    }
                } else if (BBX.INLINE.MATHML.isA(current)) {
                    if ((current.getChild(i) as Element).localName == "math") {
                        initializeMathML(m, current.getChild(i) as Element, true)
                    } else {
                        initializeViews(current.getChild(i), m, 0)
                    }
                } else if (isBoxLine((current.getChild(i) as Element))) {
                    initializeViews(current.getChild(i), m, 0)
                } else if (current.getChild(i).isPageNum()) {
                    initializePrintPage(m, current.getChild(i) as Element)
                } else if (inTable && lastTable.tableElements.isEmpty() && BBX.CONTAINER.CAPTION.isA(current.getChild(i))) {
                    val lastTable = sectionList[sectionList.size - 1].list.removeLast() as TableTextMapElement
                    inTable = false
                    initializeViews(current.getChild(i), m, 0)
                    inTable = true
                    sectionList[sectionList.size - 1].list.add(lastTable)
                } else if (isPlaceholder(current.getChild(i) as Element)) {
                    addPlaceholder(current.getChild(i) as Element)
                } else if (UTDElements.NEW_LINE.isA(current.getChild(i))) {
                    addLineBreak(current.getChild(i) as Element)
                } else {
                    val currentChild = current.getChild(i) as Element
                    if (!UTDElements.META.isA(currentChild)) initializeViews(currentChild, m, 0)
                }
            } else {
                initializeViews(current.getChild(i), m, 0)
            }
            i++
        }
    }

    private fun initializeBraille(m: Manager, tme: TextMapElement, brl: Element) {
        var t = tme
        var tempGuideWord: GuideWordBrlMapElement? = null
        for (brlChild in brl.childNodes) {
            if (brlChild is Text) {
                val b = BrailleMapElement(brlChild)
                setBraillePosition(b, brlChild)
                t.brailleList.add(b)
            } else if (isElement(brlChild)) {
                if (UTDElements.PRINT_PAGE_NUM.isA(brlChild)) {
                    val p = PrintPageBrlMapElement(brlChild)
                    setBraillePosition(p, brlChild)
                    t.brailleList.add(p)
                } else if (UTDElements.BRL_PAGE_NUM.isA(brlChild)) {
                    val b = BraillePageBrlMapElement(brlChild)
                    setBraillePosition(b, brlChild)
                    t.brailleList.add(b)
                } else if (UTDElements.BRLONLY.isA(brlChild)) {
                    if (!inTable && isGuideDots(brlChild as Element)) {
                        //If the guide dots occur at the beginning of an element,
                        //initializeGuideDots will take care of it. Otherwise,
                        //treat it as normal
                        if (initializeGuideDots(m, t, brlChild)) {
                            continue
                        }
                    }

                    if (isUncontractedWord(brlChild as Element)) {
                        val newTME = initializeUncontractedWord(m, t, brlChild)
                        if (newTME != null) {
                            t = newTME
                        }
                        continue
                    }

                    val g = createBrlOnlyBME(brlChild)
                    setBraillePosition(g, brlChild)
                    val lastList = sectionList[sectionList.size - 1].list

                    if (g is RunningHeadBrlMapElement) {
                        //Do not add the painted white space if the running head is in the middle of the element
                        if (!hasBraille(t)) {
                            if (lastList.contains(t)) {
                                lastList.add(lastList.indexOf(t), PaintedWhiteSpaceElement())
                            } else if ((t is TableCellTextMapElement
                                        && lastList.contains(t.parentTableMapElement)) && t.parentTableMapElement.tableElements.indexOf(
                                    t
                                ) == 0
                            ) {
                                //Check if the TableCellTME is the first table element of the TableTME
                                //(so we don't add a PaintedWSE in the middle of a table)
                                lastList.add(lastList.indexOf(t.parentTableMapElement), PaintedWhiteSpaceElement())
                            }
                        }
                        g.setPrintTextFromUtdEngine(m.document.engine)
                    } else if (g is GuideWordBrlMapElement) {
                        tempGuideWord = g
                    }

                    t.brailleList.add(g)
                } else if (UTDElements.NEW_PAGE.isA(brlChild)) {
                    val n = NewPageBrlMapElement(brlChild)
                    setBraillePosition(n, brlChild)
                    t.brailleList.add(n)
                    sectionList[sectionList.size - 1].incrementPages()
                    if (n.isPageBreak) {
                        val lastList = sectionList[sectionList.size - 1].list
                        if (lastList.contains(t) || (t is TableCellTextMapElement && lastList.contains(t.parentTableMapElement))) {
                            val insertIndex = if (t is TableCellTextMapElement) findIndexForPageBreak(
                                lastList.indexOf(t.parentTableMapElement),
                                lastList,
                                brl
                            )
                            else findIndexForPageBreak(lastList.indexOf(t), lastList, brl)
                            //Make sure that the list contains the TME and prevent adjacent page breaks
                            if ((insertIndex == 0
                                        || lastList.getPrevious(insertIndex, false) !is PageBreakWhiteSpaceElement)
                                && lastList.getPrevious(insertIndex, true) !is ImagePlaceholderTextMapElement
                            ) { //RT #5538 PageBreakWSE is unnecessary here
                                lastList.add(insertIndex, PageBreakWhiteSpaceElement())
                            }
                        }
                    }
                }
            }
        }
        if (tempGuideWord != null) {
            if (isLastBraille(t, tempGuideWord) && sectionList[sectionList.size - 1].list.contains(t)) {
                lastGuideWord = tempGuideWord
            }
        }
        if (!inTable) t.sort()
    }

    private fun addGuideWordWhiteSpace() {
        if (lastGuideWord != null) {
            sectionList[sectionList.size - 1].list.add(PaintedWhiteSpaceElement())
            lastGuideWord = null
        }
    }

    private fun findIndexForPageBreak(index: Int, list: MapList, brl: Node): Int {
        var i = index
        while (i > 0) {
            val prev = list[i - 1]
            if (prev.node != null && prev.node.findBlockOrNull() === brl.findBlockOrNull()) {
                i--
            } else if (prev is LineBreakElement
                || prev is PaintedWhiteSpaceElement
            ) {
                i--
            } else {
                break
            }
        }
        return i
    }

    private fun initializeBoxline(m: Manager, brl: Element) {
        val b: BoxLineTextMapElement
        val p = brl.parent

        //check whether parent is the wrapper boxline element based on seperator attribute
        if (p is Element && attributeEquals(brl, "separator", "start")) {
            b = BoxLineTextMapElement(brl, p, true)
        } else {
            val index = p.indexOf(brl)
            b = BoxLineTextMapElement(brl, p.getChild(index - 1) as Element, false)
        }

        add(m, b)
        initializeBraille(m, b, brl)
    }

    /**
     * Takes care of special handling when guide dots occur at the beginning of an element.
     * If the guide dots are not present at the beginning of an element, returns false
     */
    private fun initializeGuideDots(m: Manager, t: TextMapElement, brlonly: Element): Boolean {
        val brl = brlonly.parent as Element
        val isFirstText = t.brailleList.none { bme -> bme !is PageNumberBrlMapElement && bme.node != null && bme.node.value.trim { it <= ' ' }.isNotEmpty() }

        if (getAssociatedBrlElement(t.node) === brl && isFirstText) {
            val tme = GuideDotsTextMapElement(brlonly)
            tme.brailleList.add(GuideDotsBrlMapElement(brlonly))
            setBraillePosition(tme.brailleList[0], brlonly)
            val curList = sectionList[sectionList.size - 1].list
            curList.add(curList.size - 1, tme)
            return true
        }
        return false
    }

    private fun initializeUncontractedWord(m: Manager, t: TextMapElement, brlonly: Element): TextMapElement? {
        val brl = brlonly.parent as Element

        if (getAssociatedBrlElement(t.node) === brl
        ) {
            //Make a tme for uncontracted word
            val tme = UncontractedWordTextMapElement(brlonly)
            tme.brailleList.add(GlossaryPronunciationBrlMapElement(brlonly))
            //Append it as the last brl
            setBraillePosition(tme.brailleList[0], brlonly)
            val curList = sectionList[sectionList.size - 1].list
            curList.add(tme)

            return tme
        }
        return null
    }

    private fun setBraillePosition(b: BrailleMapElement, n: Node?) {
        requireNotNull(n) { "Expected Node, received null" }
        if (followsMoveTo(n)) {
            val parent = n.parent
            val index = parent.indexOf(n)
            val moveTo = parent.getChild(index - 1) as Element
            b.setPosition(moveTo)
        } else if (followsNewPage(n)) {
            b.setPosition(0.0, 0.0)
        }
    }

    private fun initializePrintPage(m: Manager, page: Element) {
        val p = PageIndicatorTextMapElement(page)
        val brl = p.findBraillePageNode(page)
        if (brl != null) initializeBraille(m, p, brl)

        if (!p.brailleList.isEmpty()) add(m, p)
    }

    private fun initializeMathML(m: Manager, math: Element, mathRoot: Boolean?) {
        log.debug("Initializer processing node: {}", math.toXML())
        val mathElement = MathMLElement(math)
        val nodeList = getBrailleChildren(math, ArrayList())
        for (node in nodeList) {
            initializeBraille(m, mathElement, node as Element)
        }
        if (inTable) {
            val mathTableElement = MathMLTableElement(mathElement, math)
            addToTable(m, mathTableElement)
        } else {
            add(m, mathElement)
        }
    }

    private fun getBrailleChildren(node: Node, nodeList: ArrayList<Node>): ArrayList<Node> {
        for (i in 0 until node.childCount) {
            if (node.getChild(i) !is Element) {
                continue
            }
            if (UTDElements.BRL.isA(node.getChild(i))) {
                nodeList.add(node.getChild(i))
            }
            getBrailleChildren(node.getChild(i), nodeList)
        }
        return nodeList
    }

    private fun add(m: Manager, t: TextMapElement) {
        if (t !is LineBreakElement) addGuideWordWhiteSpace()
        var localBlock = t.node
        if (!inTable && readOnlyTableParent == null) {
            while (!BBX.BLOCK.isA(localBlock)) {
                localBlock = localBlock.parent
                if (BBX.SECTION.isA(localBlock)) {
                    localBlock = t.node
                    break
                }
            }
        }
        val newChars = if (t is BoxLineTextMapElement) 0 else t.text.length
        if (chars > SECTION_COUNT) {
            if (localBlock !== block) {
                sectionList[sectionList.size - 1].setChars(chars)
                sectionList.add(SectionElement(MapList(m)))
                chars = 0
            }
        } else {
            sectionList[sectionList.size - 1].setChars(chars)
            block = localBlock
        }
        sectionList[sectionList.size - 1].list.add(t)
        chars += newChars
        if (readOnlyTableParent != null) {
            t.isReadOnly = true
        }
    }

    private fun addPlaceholder(e: Element) {
        val p = ImagePlaceholderTextMapElement(e)
        p.brailleList.add(BrailleMapElement(e))
        sectionList[sectionList.size - 1].list.add(p)
    }

    private fun addLineBreak(lineBreak: Element) {
        val lb = LineBreakElement(lineBreak)
        sectionList[sectionList.size - 1].list.add(lb)
    }

    private fun addToTable(m: Manager, tableCell: TableCellTextMapElement) {
        val lastTable = lastTable
        if (lastTable.tableElements.isNotEmpty()) tableCell.prev =
            lastTable.tableElements[lastTable.tableElements.size - 1]
        tableCell.isReadOnly = true
        tableCell.parentTableMapElement = lastTable
        lastTable.tableElements.add(tableCell)
    }

    private fun vaildTextElement(n: Node, text: String): Boolean {
        val e = n.parent as Element
        val index = e.indexOf(n)
        val length = text.length

        if (index == e.childCount - 1 || !(UTDElements.BRL.isA(e.getChild(index + 1)))) return false

        for (i in 0 until length) {
            if (text[i] != '\n' && text[i] != '\t') return true
        }

        //if empty node of length zero, but followed by brl, then part of template document
        return length == 0 && index < e.childCount - 1 && UTDElements.BRL.isA(e.getChild(index + 1))
    }

    protected fun utdStyleEquals(e: Element, value: String): Boolean {
        val key = "utd-style"
        return attributeEquals(e, key, value)
    }

    protected fun utdActionEquals(e: Element, value: String): Boolean {
        val key = "utd-action"
        return attributeEquals(e, key, value)
    }

    private fun attributeEquals(e: Element, key: String, value: String): Boolean {
        val atr = e.getAttribute(key)
        if (atr != null) {
            return atr.value == value
        }

        return false
    }

    /**
     * Used to check whether a BRL node has the specified attribute that specifies boxline
     *
     * @param e
     * @return
     */
    private fun isBoxLine(e: Element): Boolean {
        val atr = e.getAttribute("type")
        if (atr != null) return atr.value == "formatting"

        return false
    }

    private fun isGuideDots(e: Element): Boolean {
        return "guideDots" == e.getAttributeValue("type")
    }

    private fun isUncontractedWord(e: Element): Boolean {
        return "pronunciation" == e.getAttributeValue("type")
    }

    private fun isPlaceholder(e: Element): Boolean {
        return e.getAttribute("skipLines", UTD_NS) != null
    }

    private fun isNotSimpleTable(e: Element): Boolean {
        val format = e.getAttributeValue("class")
        if (format != null) return format == "utd:tableListed" || format == "utd:tableStairstep" || format == "utd:tableLinear"
        return false
    }

    private fun hasBraille(parent: TextMapElement): Boolean {
        for (i in parent.brailleList.indices) {
            if (parent.brailleList[i].node != null && parent.brailleList[i].text.isNotEmpty()) {
                return true
            }
        }
        return false
    }

    private fun isLastBraille(parent: TextMapElement, target: BrailleMapElement): Boolean {
        for (i in parent.brailleList.indices.reversed()) {
            if (parent.brailleList[i] === target) {
                return true
            }
            if (parent.brailleList[i] !is PageNumberBrlMapElement && parent.brailleList[i].node != null && parent.brailleList[i].text.isNotEmpty()) {
                return false
            }
        }
        throw IllegalArgumentException("BrailleMapElement target must be child of parent")
    }

    private val lastTable: TableTextMapElement
        /**
         * Convenience method to get the table most recently added to the maplist
         *
         * @return
         */
        get() = sectionList[sectionList.size - 1].list.last() as TableTextMapElement

    companion object {
        const val SECTION_COUNT: Int = 10000

        const val CHAR_COUNT: Int = 20000
    }
}

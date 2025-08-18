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
import org.brailleblaster.libembosser.utils.BrailleMapper
import org.brailleblaster.utd.LineWrapper.InsertionResult
import org.brailleblaster.utd.LineWrapper.LineBreakResult
import org.brailleblaster.utd.actions.TransNoteAction.Companion.getEnd
import org.brailleblaster.utd.actions.TransNoteAction.Companion.getStart
import org.brailleblaster.utd.exceptions.CellOccupiedException
import org.brailleblaster.utd.exceptions.NoLineBreakPointException
import org.brailleblaster.utd.internal.DocumentOrderComparator
import org.brailleblaster.utd.internal.elements.*
import org.brailleblaster.utd.pagelayout.*
import org.brailleblaster.utd.properties.*
import org.brailleblaster.utd.utils.PageBuilderHelper
import org.brailleblaster.utd.utils.TextTranslator
import org.brailleblaster.utd.utils.UTDHelper
import org.brailleblaster.utils.Counter
import org.brailleblaster.utils.RepeatingLetters
import org.brailleblaster.utils.SetList
import org.brailleblaster.utils.toRepeatingLetters
import org.brailleblaster.utils.xom.NodeSorter.sortByDocumentOrder
import org.brailleblaster.utils.xom.childNodes
import org.mwhapples.jlouis.TranslationException
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.math.max

/**
 * Create a UTD page.
 *
 * This class is used to create and place UTD elements on a Braille page. You
 * create the page by creating an instance, setting the properties relating to
 * how the Braille should be placed on the page and then add brl elements to the
 * object.
 *
 *
 * This class will handle wrapping the lines, aligning the Braille and when the
 * page is full starting a new page. Also it will place the page numbers and any
 * headers/footers for you.
 *
 */
class PageBuilder {
    /**
     * Used for KeepWithNext and DontSplit for taking a "snapshot" of PageBuilder's
     * current style options so that nodes can be re-added to a new page, keeping
     * their original appearance intact.
     *
     */
    internal class PBStyle {
        var leftIndent = 0
        var firstLineIndent: Int? = null
        var rightIndent = 0
        var pendingLines = -1
        var overrideLines = 0
            private set

        /**
         * maxLines represents the restriction imposed by a blank line exception.
         */
        var maxLines = 0
        var alignment: Align = Align.LEFT
        var brl: Element
        var isEndOfChain = false
        var dontSplit = false
        var pageBuilder: PageBuilder? = null

        constructor(pageBuilder: PageBuilder, brl: Element) {
            leftIndent = pageBuilder._alignment.leftIndent
            firstLineIndent = pageBuilder._alignment.firstLineIndent
            rightIndent = pageBuilder._alignment.rightIndent
            pendingLines = max(pageBuilder.pendingLinesBefore, pageBuilder.pendingLinesAfter)
            overrideLines = pageBuilder.newLinesOverride
            maxLines = pageBuilder.getMaxLines()
            alignment = pageBuilder._alignment.align
            isEndOfChain = false
            dontSplit = pageBuilder.dontSplit
            this.pageBuilder = pageBuilder
            this.brl = brl
        }

        constructor(brl: Element) {
            this.brl = brl
        }
    }

    /**
     * Get the Braille page number of this page.
     *
     * @return The Braille page number.
     */
    var braillePageNumber: PageNumberTracker
        private set
    var engine: ITranslationEngine? = null
        private set
    private var segment: SegmentInfo? = null

    /**
     * Get the number of lines to progress when starting a new line.
     *
     * @return The number of lines to progress when starting a new line.
     */
    var lineSpacing = 1
        private set
    private val log = LoggerFactory.getLogger(this.javaClass)
    private var pages = PageList()
    private var leftoverFormattingNodes: MutableList<Node> = ArrayList()
    private var pageGrid: PageGrid
    private var pendingSpacing = PendingSpacing()
    private var nextPageNumberType: PageNumberType? = null
    var previousPageNumberType: PageNumberType? = null
    private var keepWithNext = false
    private val keepWithNextList: MutableList<PBStyle> = LinkedList()
    private var _x = 0
    private var _y = 0
    private var addedPageNumbers = false
    private var pageNumbers = PageNumbering()
    private var _alignment = Alignment()
    var alignment: Align
        get() = _alignment.align
        set(value) {
            _alignment.align = value
        }
    private var skipTop = false
    private var nextSkipTop = false
    private var skipBottom = false
    private var writtenUTD = false
    private var forceSpacing = false
    private var yPPI = -1
    var cellsPerLineWithLeftIndent = 0
    var runningHead: String = ""
    private var startOfBlock = false
    private var startGuideWord: Element? = null
    private var endGuideWord: Element? = null
    var altEndGuideWord: Element? = null
    private var endGuideWordChanged = false
    private var guideWordEnabled = false
    var titlePage = false
        get() = braillePageNumber.pageNumber == 1 || field
    var isTOC = false
    var poemEnabled = false
    var dontSplit = false
        private set
    private var lockSplit = false
    private var lineNumber: Element? = null
    var lineNumberPos = -1
        private set
    private var usedLineNums: MutableList<Element> = ArrayList()
    var blankPageAdded = 0
    private var volumeEnd = false
    var volumeEndLength = 0
    private var tabbed = false
    private var overridePageType = false
    private var lineNumberLength = 0
    private var poemRightIndent = 0
    var currentBrl: Element? = null
        private set
    private var addingSkipLines = false
    private var disablePPIChecks = -1
    var isRightPage = true
        private set
    private var continueSkip = false
    private var nonsequentialPages = false
    private var isReset = false
    private var isContinuePages = false
    private var isSpecialReset = false
    private var runHeadAdded = false
    private var afterTPage = false
    private var singleGuideWord: Element? = null
    private var isDecrementCont = false
    var isAfterVolume = false
        private set
    private var afterBlankPageNumber = false
    private var currBlankPageNumber = false
    var isFirstPage = true
        private set
    var skipNumberLines: NumberLinePosition? = null
        private set
    private var contLetter: String? = null
    private var metaPageType: PageNumberType? = null

    /*
	 * True if page was not naturally created by overflowing text 
	 * (a newPageBefore/After style or a Don't Split/Keep With 
	 * Next operation)
	 */
    private var forcedPageBreak = false

    /**
     * Used to catch edge cases involving newLines. When a formatter
     * directly calls processSpacing and override lines exist, this
     * gets set to true so that the next time text is added, new
     * lines will not be added
     */
    private var ignoreSpacing = false

    /**
     * Create a PageBuilder.
     *
     * @param cursor
     * The cursor for the new page.
     * @param braillePageNumber
     * The Braille page number this page is to represent.
     */
    @JvmOverloads
    constructor(
        engine: ITranslationEngine?,
        cursor: Cursor = Cursor(),
        braillePageNumber: PageNumberTracker = PageNumberTracker()
    ) {
        this.engine = engine
        _x = brailleSettings.cellType.getCellsForWidth(cursor.x.toBigDecimal())
        _y = brailleSettings.cellType.getLinesForHeight(cursor.y.toBigDecimal())
        this.braillePageNumber = braillePageNumber
        runningHead = engine!!.pageSettings.getRunningHead(engine)
        initNewSegment()
        val cell = brailleSettings.cellType
        val lineLength = cell.getCellsForWidth(pageSettings.drawableWidth.toBigDecimal())
        val numOfLines = cell.getLinesForHeight(pageSettings.drawableHeight.toBigDecimal())
        pageGrid = PageGrid(lineLength, numOfLines)
        pages.add(pageGrid)
        cellsPerLineWithLeftIndent = cellsPerLine
    }

    /**
     * Create a copy of the PageBuilder for backup/restore purposes.
     *
     * @param other The PageBuilder to be copied.
     */
    constructor(other: PageBuilder) : this(
        other.engine,
        braillePageNumber = PageNumberTracker(other.braillePageNumber)
    ) {
        require(!other.hasWrittenUTD()) { "Cannot copy a PageBuilder which has already been written to UTD" }
        this.pageGrid = PageGrid(other.pageGrid)
        this.pages = PageList(other.pages)
        this.pages.replace(other.pageGrid, this.pageGrid)
        // Be careful, mutable object but current use suggests copy not needed.
        this.segment = other.segment
        this.addedPageNumbers = other.addedPageNumbers
        this.addingSkipLines = other.addingSkipLines
        this.afterTPage = other.afterTPage
        this._alignment = other._alignment.copy()
        // Be careful, mutable object but current use suggests copy not needed.
        this.altEndGuideWord = other.altEndGuideWord
        this.pendingSpacing = other.pendingSpacing.copy()
        this.blankPageAdded = other.blankPageAdded
        this.cellsPerLineWithLeftIndent = other.cellsPerLineWithLeftIndent
        this.continueSkip = other.continueSkip
        // Be careful, mutable object but current use suggests copy not needed.
        this.currentBrl = other.currentBrl
        this.disablePPIChecks = other.disablePPIChecks
        this.dontSplit = other.dontSplit
        this.endGuideWord = other.endGuideWord
        this.endGuideWordChanged = other.endGuideWordChanged
        this.forceSpacing = other.forceSpacing
        this.guideWordEnabled = other.guideWordEnabled
        this.isReset = other.isReset
        this.isContinuePages = other.isContinuePages
        this.keepWithNext = other.keepWithNext
        this.keepWithNextList.addAll(other.keepWithNextList)
        this.leftoverFormattingNodes = ArrayList(other.leftoverFormattingNodes)
        // Be careful, mutable object but current use suggests copy not needed.
        this.lineNumber = other.lineNumber
        this.lineNumberLength = other.lineNumberLength
        this.lineNumberPos = other.lineNumberPos
        this.lineSpacing = other.lineSpacing
        this.lockSplit = other.lockSplit
        this.maxLines = other.maxLines
        this.nextPageNumberType = other.nextPageNumberType
        this.previousPageNumberType = other.previousPageNumberType
        this.nonsequentialPages = other.nonsequentialPages
        this.overridePageType = other.overridePageType
        this.pageNumbers = PageNumbering()
        this.poemEnabled = other.poemEnabled
        this.poemRightIndent = other.poemRightIndent
        this.isRightPage = other.isRightPage
        this.runHeadAdded = other.runHeadAdded
        this.runningHead = other.runningHead
        this.skipBottom = other.skipBottom
        this.skipTop = other.skipTop
        this.nextSkipTop = other.nextSkipTop
        // Be careful, mutable object but current use suggests copy not needed.
        this.startGuideWord = other.startGuideWord
        this.startOfBlock = other.startOfBlock
        this.tabbed = other.tabbed
        this.titlePage = other.titlePage
        this.isTOC = other.isTOC
        this.usedLineNums.addAll(other.usedLineNums)
        this.volumeEnd = other.volumeEnd
        this.volumeEndLength = other.volumeEndLength
        this.writtenUTD = other.writtenUTD
        this._x = other._x
        this._y = other._y
        this.yPPI = other.yPPI
        this.singleGuideWord = other.singleGuideWord
        this.isDecrementCont = other.isDecrementCont
        this.isAfterVolume = other.isAfterVolume
        this.afterBlankPageNumber = other.afterBlankPageNumber
        this.currBlankPageNumber = other.currBlankPageNumber
        this.isFirstPage = other.isFirstPage
        this.skipNumberLines = other.skipNumberLines
        this.contLetter = other.contLetter
        this.metaPageType = other.metaPageType
    }

    private val brailleSettings: BrailleSettings
        get() = engine!!.brailleSettings
    val brlElementsOnPage: Set<Element>
        get() {
            val brls: MutableSet<Element> = TreeSet(DocumentOrderComparator())
            val nodes: MutableList<Node> = ArrayList()
            for (i in 0 until pageGrid.height) {
                for (j in 0 until pageGrid.width) {
                    val node = pageGrid.getCell(j, i)?.node
                    if (node != null && !nodes.contains(node)) {
                        nodes.add(node)
                        val parent = node.parent
                        if (parent != null && UTDElements.BRL.isA(parent)) {
                            brls.add(parent as Element)
                        }
                    }
                }
            }
            return brls
        }

    /**
     * Request that at least the specified number of new lines appear after the
     * last inserted Braille.
     *
     * This will ensure that there is at least the specified number of new lines
     * after the last inserted Braille. This does not mean that the request will
     * actually lead to new lines being inserted, as previous calls to this
     * method may have already lead to enough new lines already being inserted.
     * The guarantee of this method is that the current insertion point will be
     * separated from the last Braille by the specified number of lines.
     *
     * @param numOfNewLines
     * The number of new lines you want after the last Braille
     * inserted.
     */
    fun addAtLeastLinesBefore(numOfNewLines: Int) {
        pendingSpacing.addAtLeastLinesBefore(numOfNewLines)
    }

    fun addAtLeastLinesAfter(numOfNewLines: Int) {
        pendingSpacing.addAtLeastLinesAfter(numOfNewLines)
    }

    fun resetPendingLines() {
        pendingSpacing.linesAfter = 0
        pendingSpacing.linesBefore = 0
    }

    /**
     * Request that at least the specified number of new pages appear after the
     * last inserted Braille.
     *
     * This will ensure that there is at least the specified number of new pages
     * after the last inserted Braille. This does not mean that the request will
     * actually lead to new pages being inserted, as previous calls to this
     * method may have already lead to enough new pages already being inserted.
     * The guarantee of this method is that the current insertion point will be
     * separated from the last Braille by the specified number of pages.
     *
     * @param numOfNewPages
     * The number of new pages you want after the last Braille
     * inserted.
     */
    fun addAtLeastPages(numOfNewPages: Int) {
        pendingSpacing.addAtLeastPages(numOfNewPages)
    }

    fun addExplicitPages(pages: Int) {
        pendingSpacing.addExplicitPages(pages)
    }

    fun resetExplicitPages() {
        pendingSpacing.explicitPages = 0
    }

    fun setSkipNumberLineTop(skipTop: Boolean): PageBuilder {
        val printPos = PageBuilderHelper.getPrintPageNumberAt(pageSettings, braillePageNumber.pageNumber)
        val brlPos = PageBuilderHelper.getBraillePageNumberAt(pageSettings, braillePageNumber.pageNumber)
        if (brlPos.isTop || printPos.isTop && printPageNumber.isNotEmpty()) {
            if (this.skipTop) {
                this.skipTop = this.skipTop && skipTop
            } else {
                this.skipTop = this.skipTop || skipTop
            }
        } else {
            this.skipTop = false
        }
        nextSkipTop = skipTop
        return this
    }

    private val pageSettings: PageSettings
        get() = engine!!.pageSettings

    fun setSkipNumberLineBottom(skipBottom: Boolean): PageBuilder {
        val printPos = PageBuilderHelper.getPrintPageNumberAt(pageSettings, braillePageNumber.pageNumber)
        val brlPos = PageBuilderHelper.getBraillePageNumberAt(pageSettings, braillePageNumber.pageNumber)
        if (brlPos.isBottom || printPos.isBottom) {
            if (this.skipBottom) {
                this.skipBottom = this.skipBottom && skipBottom
            } else {
                this.skipBottom = this.skipBottom || skipBottom
            }
        } else {
            this.skipBottom = false
        }
        return this
    }

    fun setForceSpacing(forceSpacing: Boolean) {
        this.forceSpacing = forceSpacing
    }

    //This is a linesBefore because you need to put the skip lines before the next element
    fun addSkipLines(lines: Int) {
        setForceSpacing(true)
        addAtLeastLinesBefore(lines)
        addingSkipLines = true
    }

    fun addBrl(brl: Element, lineWrapper: LineWrapper): Set<PageBuilder> {
        return addBrl(brl, pageNumberType, lineWrapper)
    }

    private fun addBrlChild(
        brlChild: Text, startingIndex: Int,
        numberType: PageNumberType, lineWrapper: LineWrapper
    ): Set<PageBuilder> {
        if (segment == null) {
            initNewSegment()
        }
        segment!!.rightIndent = rightIndent
        if (!addedPageNumbers || afterTPage) {
            addPageNumbers()
            //			setAfterTPage(false);
        }
        if (tabbed) {
            setTabbed(false)
        }
        var result: MutableSet<PageBuilder> = LinkedHashSet()
        result.add(this)
        if (isSkipTop() && _y == 0) {
            moveY(lineSpacing, true)
        }

        // Place Braille into grid
        // LineWrapPoint is the (1-based) point that should be line wrapped
        // pageGrid[y][lineWrapPoint - 1] is the last character of a line
        var lineWrapPoint = cellsPerLine
        if (rightIndent > 0) {
            // Remember cannot be greater than the number of cells on the page.
            lineWrapPoint = lineWrapPoint.coerceAtMost(leftIndent + rightIndent)
        } else lineWrapPoint += rightIndent
        val brlText = brlChild.value
        var word: String
        var startOfWord: Int
        val spaceDelimiter: Char = if (!brailleSettings.isUseAsciiBraille) '\u2800' else ' '
        var i = startingIndex
        while (i < brlText.length) {
            startOfWord = i
            if (_y >= linesPerPage) {
                // Should we try and add it to this page or just start a new one.
                log.debug("moving to next page for text \"{}\"", brlText)
                return lineWrapToNextPage(result, brlChild, startOfWord, numberType, lineWrapper)
            }
            var brlChr = brlText[i]
            if (brlChr == spaceDelimiter) {
                if (startOfBlock
                    && _x == firstLineIndent
                ) {
                    log.debug("Stripping space at {} from text \"{}\"", i, brlText)
                    i++
                    if (i == brlText.length && rightIndent == 0 && segment!!.isBeginning) {
                        break
                    }
                    continue
                } else if (segment!!.isBeginning && !startOfBlock) {
                    log.debug("Stripping space at {} from text \"{}\"", i, brlText)
                    i++
                    continue
                }
            }
            var lineWrapCheck = getMaxLineRemaining(lineWrapPoint)
            var startLineInsertion: InsertionResult? = null
            var insertionLength = 0
            if (segment!!.isBeginning) {
                // check about number signs
                startLineInsertion = lineWrapper.checkStartLineInsertion(brlText, i)
                val insertionDots = startLineInsertion.insertionDots
                val insertionPos = startLineInsertion.insertionPosition
                if (!insertionDots.isNullOrEmpty() && insertionPos >= i && insertionPos - i < lineWrapCheck) {
                    lineWrapCheck -= insertionDots.length
                    insertionLength = insertionDots.length
                } else {
                    startLineInsertion = null
                }
            }
            log.debug("lineWrapPoint: {} lineWrapCheck: {}", lineWrapPoint, lineWrapCheck)
            var nextBreak: LineBreakResult
            var fitsOnLine = true
            var contDotsPos: Int
            try {
                nextBreak = lineWrapper.findNextBreakPoint(brlText, i, lineWrapCheck)
                contDotsPos = nextBreak.insertionPosition
            } catch (e: NoLineBreakPointException) {
                log.debug("Line break within line length not found")
                nextBreak = e.getnextBreak()
                contDotsPos = nextBreak.insertionPosition
                fitsOnLine = false
            }
            log.debug(
                "i={}, startOfWord={}, nextBreak={}, contDotsPos={}, x={}, y={}, page={}",
                i,
                startOfWord,
                nextBreak.lineBreakPosition,
                contDotsPos,
                _x,
                _y,
                braillePageNumber.pageNumber
            )
            val nextBreakPos = nextBreak.lineBreakPosition
            brlChr = brlText[nextBreakPos - 1]
            val onSpace = brlChr == spaceDelimiter
            // Add the current text to the StringBuilder
            word = brlText.substring(i, nextBreakPos)
            i = nextBreakPos - 1
            segment!!.isBeginning = false
            if (word.isEmpty()) {
                break
            }
            return if (fitsOnLine) {
                if (startLineInsertion != null) {
                    if (startLineInsertion.insertionPosition > startOfWord) {
                        addBrlChildFragment(brlChild, startOfWord, startLineInsertion.insertionPosition)
                    }
                    val insertionBrl = UTDElements.BRLONLY.create()
                    insertionBrl.appendChild(startLineInsertion.insertionDots)
                    addBrlChildFragment(insertionBrl, 0, startLineInsertion.insertionDots?.length ?: 0)
                    word = word.substring(startLineInsertion.insertionPosition - startOfWord)
                    startOfWord = startLineInsertion.insertionPosition
                }
                if (contDotsPos >= 0) {
                    val contDots = nextBreak.insertionDots
                    addBrlChildFragment(brlChild, startOfWord, contDotsPos)
                    val contDotsBrl = UTDElements.BRLONLY.create()
                    contDotsBrl.appendChild(contDots)
                    addBrlChildFragment(contDotsBrl, 0, contDots?.length ?: 0)
                    i = startOfWord + word.length
                    addBrlChildFragment(brlChild, contDotsPos, i)
                    setStartOfBlock(false)
                    nextLine(true)
                    if (_y >= linesPerPage) {
                        log.debug("starting new page")
                        return lineWrapToNextPage(result, brlChild, i, numberType, lineWrapper)
                    }
                    initNewSegment()
                } else {
                    i = word.length + startOfWord
                    addBrlChildFragment(brlChild, startOfWord, i)
                }
                continue
            } else {
                // Word won't fit on the current line or overlaps a page
                // number
                if (wordHasTrailingSpacesAtEndOfLine(onSpace, word, lineWrapPoint - insertionLength)) {
                    if (startLineInsertion != null) {
                        if (startLineInsertion.insertionPosition > startOfWord) {
                            addBrlChildFragment(brlChild, startOfWord, startLineInsertion.insertionPosition)
                        }
                        val insertionBrl = UTDElements.BRLONLY.create()
                        insertionBrl.appendChild(startLineInsertion.insertionDots)
                        addBrlChildFragment(insertionBrl, 0, startLineInsertion.insertionDots?.length ?: 0)
                        word = word.substring(startLineInsertion.insertionPosition - startOfWord)
                        startOfWord = startLineInsertion.insertionPosition
                    }
                    addBrlChildFragment(brlChild, startOfWord, word.length - 1 + startOfWord)
                    nextLine(false)
                    startOfWord += word.length
                    if (_y >= linesPerPage) {
                        log.debug("moving to next page for text \"{}\"", brlText)
                        return lineWrapToNextPage(result, brlChild, startOfWord, numberType, lineWrapper)
                    }
                    initNewSegment()
                    i = startOfWord
                    continue
                } else if (wordIsLongerThanLine(
                        word,
                        lineWrapPoint - insertionLength,
                        startOfBlock && startOfWord == 0
                    )
                ) {
                    //Word is longer than a single line. Add as much that will fit on the current line.
                    // Check that we have not already reached the end of the line
                    if (lineWrapPoint <= _x) {
                        nextLine(true, startOfBlock && startOfWord == 0)
                        if (_y >= linesPerPage) {
                            log.debug("moving to next page for text \"{}\"", brlText)
                            return lineWrapToNextPage(result, brlChild, startOfWord, numberType, lineWrapper)
                        }
                        initNewSegment()
                        i = startOfWord
                        continue
                    }
                    if (startLineInsertion != null) {
                        if (startLineInsertion.insertionPosition > startOfWord) {
                            addBrlChildFragment(brlChild, startOfWord, startLineInsertion.insertionPosition)
                        }
                        val insertionBrl = UTDElements.BRLONLY.create()
                        insertionBrl.appendChild(startLineInsertion.insertionDots)
                        addBrlChildFragment(insertionBrl, 0, startLineInsertion.insertionDots?.length ?: 0)
                        word = word.substring(startLineInsertion.insertionPosition - startOfWord)
                        startOfWord = startLineInsertion.insertionPosition
                    }
                    var endOfLine = lineWrapPoint - _x
                    if (isNumberLine && pageNumberWidth > 0) {
                        endOfLine = (endOfLine - pageNumberWidth).coerceAtLeast(0)
                    }
                    addBrlChildFragment(brlChild, startOfWord, startOfWord + endOfLine)
                    startOfWord += endOfLine
                    nextLine(true)
                    if (_y >= linesPerPage) {
                        log.debug("moving to next page for text \"{}\"", brlText)
                        return lineWrapToNextPage(result, brlChild, startOfWord, numberType, lineWrapper)
                    }
                    initNewSegment()
                    i = startOfWord
                    continue
                } else if (wordIsOnlySpaces(onSpace, i == brlText.length - 1, word, spaceDelimiter)) {
                    //Word consists only of spaces, due to either bad normalization or unicode.
                    // MWhapples: Should not need number sign insertion as why would spaces need a numbersign?
                    nextLine(false)
                    if (_y >= linesPerPage) {
                        log.debug("moving to next page for text \"{}\"", brlText)
                        result.addAll(startNewPage(numberType))
                        result.addAll(
                            result.last().addBrlChild(brlChild, startOfWord + 1, numberType, lineWrapper)
                        )
                        return result
                    }
                    initNewSegment()
                    i = startOfWord + word.length
                    continue
                } else {
                    //Word can fit on next line
                    //Note: This case of line wrapping is unique in that it "undoes"
                    //previous line wrapping and moves it to the next line and then 
                    //restarts the loop on the current word, thus breaking
                    //the recursive pattern of the rest of the line wrapping code.
                    // MWhapples: Should not need number sign insertion as either dealing with part word so not start of a line or does not actually insert as moving to next line.
                    if (wordIsNotComplete(spaceDelimiter, word)) {
                        //This brl child only represents part of a word
                        val newCells = splitLineAtChar(spaceDelimiter)
                        nextLine(false)
                        if (_y >= linesPerPage) {
                            log.debug("moving to next page for text \"{}\"", brlText)
                            if (verifySplit()) {
                                result.addAll(startNewPage(numberType))
                                return result
                            }
                            //Moving down created a new page, so go ahead and put it all on that page 
                            //before passing it off to that pagebuilder 
                            val newPB = insertCellsOnNewPage(newCells, numberType)
                            result.addAll(newPB.addBrlChild(brlChild, startOfWord, numberType, lineWrapper))
                            result
                        } else {
                            //New line fits on this page
                            initNewSegment()
                            var curPB = this
                            for (cell in newCells) {
                                cell.segmentInfo = curPB.segment
                                if (curPB.pageGrid.getCell(curPB._x, curPB._y) != null) {
                                    curPB.moveY(lineSpacing, false)
                                    if (curPB.hasLeftPage()) {
                                        curPB._x = curPB.cornerPageLength
                                    } else {
                                        curPB._x = curPB._alignment.leftIndent
                                    }
                                    if (curPB._y >= linesPerPage) {
                                        if (verifySplit()) {
                                            result.addAll(startNewPage(numberType))
                                            return result
                                        }
                                        result.addAll(startNewPage(numberType))
                                        curPB = result.last()
                                        if (curPB.hasLeftPage()) {
                                            curPB._x = curPB.cornerPageLength
                                        } else {
                                            curPB._x = curPB._alignment.leftIndent
                                        }
                                        curPB.initNewSegment()
                                    }
                                }
                                curPB.addCell(curPB._x, curPB._y, cell)
                                curPB._x++
                                curPB.setStartOfBlock(false)
                                curPB.segment!!.isBeginning = false
                            }
                            if (curPB !== this) {
                                result.addAll(curPB.addBrlChild(brlChild, startOfWord, numberType, lineWrapper))
                                return result
                            }
                            i = startOfWord
                            continue
                        }
                    } else {
                        //This brl child represents a whole word
                        val firstLine = startOfBlock && startOfWord == 0
                        nextLine(true, firstLine)
                        if (_y >= linesPerPage) {
                            log.debug("moving to next page for text \"{}\"", brlText)
                            return lineWrapToNextPage(result, brlChild, startOfWord, numberType, lineWrapper)
                        }
                        initNewSegment()
                        //New line fits on current page
                        i = startOfWord
                        continue
                    }
                }
            }
        }


        //Uncontracted words and extra spacing for glossary 
        val brl = brlChild.parent as Element?
        if (brl != null) {
            if (brl.getAttribute("type") != null && brl.getAttributeValue("type") == "pronunciation" && brl.value != " ") {
                result = insertUncontractedGW(brl, brl.indexOf(brlChild) + 1)
            }
            if (brl.getAttribute("spacing") != null) {
                val spacing = brl.getAttributeValue("spacing").toInt()
                if (spacing > 0 && x + spacing < cellsPerLine) {
                    x += spacing
                }
                brl.removeAttribute(brl.getAttribute("spacing"))
            }
        }
        return result
    }

    /**
     * Add document content from a BRL to the page.
     *
     * This method is very similar to addStringToPageGrid but will set startOfBlock to false. This should be used when the content being added is actually part of the document content and will change whether the next Braille to be added is at the start of a block.
     *
     * @param insertionBrl The child of the brl element to insert.
     * @param startIndex The index where to start inserting from.
     * @param endIndex The index of the end of the substring.
     */
    private fun addBrlChildFragment(insertionBrl: Node, startIndex: Int, endIndex: Int) {
        addStringToPageGrid(insertionBrl, startIndex, endIndex)
        setStartOfBlock(false)
    }

    @JvmOverloads
    fun nextLine(forceNewLine: Boolean, firstLine: Boolean = false, lineSize: Int = lineSpacing) {
        moveY(lineSize, forceNewLine)
        // If the Braille starts a block remember first line indent
        _x = if (firstLine) {
            // Might this cause problems for left pages with page numbers?
            firstLineIndent
        } else if (hasLeftPage()) {
            cornerPageLength
        } else {
            _alignment.leftIndent
        }
    }

    private fun getMaxLineRemaining(lineWrapPoint: Int): Int {
        var lineWrapCheck = _x
        while (lineWrapCheck < lineWrapPoint && pageGrid.getCell(lineWrapCheck, _y) == null) {
            lineWrapCheck++
        }
        lineWrapCheck -= _x
        return lineWrapCheck
    }

    /**
     * Line-wrapping helper. Returns true if word is too long to fit on one line.
     */
    private fun wordIsLongerThanLine(word: String, lineWrapPoint: Int, startOfLine: Boolean): Boolean {
        return if (startOfLine) word.length > lineWrapPoint - 1 - firstLineIndent && _x == _alignment.firstLineIndent else word.length > lineWrapPoint - 1 - _alignment.leftIndent && _x == _alignment.leftIndent
    }

    /**
     * Line-wrapping helper. Returns true if word ends with one or more spaces that run off the end of the page
     */
    private fun wordHasTrailingSpacesAtEndOfLine(endsWithSpace: Boolean, word: String, lineWrapPoint: Int): Boolean {
        return if (endsWithSpace) {
            if (_x + word.length > lineWrapPoint && _x + word.length == lineWrapPoint + 1 && pageGrid.getCell(
                    lineWrapPoint - 1,
                    _y
                ) == null
            ) true else _x + word.length in 2..lineWrapPoint && pageGrid.getCell(
                _x + word.length - 1,
                _y
            ) != null && pageGrid.getCell(_x + word.length - 2, _y) == null
        } else false
    }

    /**
     * Line-wrapping helper. Returns true if word consists only of spaces.
     */
    private fun wordIsOnlySpaces(
        endsWithSpace: Boolean,
        endOfWord: Boolean,
        word: String,
        spaceDelimiter: Char
    ): Boolean {
        return endsWithSpace && endOfWord && word.replace(spaceDelimiter.toString().toRegex(), "").isEmpty()
    }

    /**
     * Line-wrapping helper. Returns true if text does not represent an entire word
     */
    private fun wordIsNotComplete(spaceDelimiter: Char, word: String): Boolean {
        return _x > 0 && _x > _alignment.leftIndent && pageGrid.getCell(_x - 1, _y)?.char !in listOf(null, spaceDelimiter) && word.replace(
            spaceDelimiter.toString().toRegex(), ""
        ).isNotEmpty()
    }

    /**
     * Line-wrapping helper. Takes a list of Cells and insert them on a new page.
     */
    private fun insertCellsOnNewPage(newCells: List<Cell>, numberType: PageNumberType): PageBuilder {
        // MWhapples: This method probably needs to be tightened up to check that the Braille fits on the line it is inserted on.
        val keepWithNext = keepWithNextList.isNotEmpty()
        val newPB = startNewPage(numberType).last()
        if (keepWithNext && (newPB.isNumberLine && newPB.isEmptyNumberLine || !newPB.isEmptyLine)) { // NOPMD
            return newPB
        }
        if (newPB.hasTopLeftPage(newPB.braillePageNumber)) {
            newPB._x = newPB.cornerPageLength
        } else {
            newPB._x = newPB._alignment.leftIndent
        }
        newPB.initNewSegment()
        // MWhapples: quick fix for specific issue, may be we should reuse addBrl and related methods.
        var remainingBlankCells = 0
        for (c in newPB._x until cellsPerLine) {
            if (pageGrid.getCell(c, newPB._y) == null) remainingBlankCells++ else break
        }
        if (newCells.size > remainingBlankCells) {
            newPB.nextLine(true)
        }
        for (cell in newCells) {
            cell.segmentInfo = newPB.segment
            newPB.addCell(newPB._x, newPB._y, cell)
            newPB._x++
        }
        return newPB
    }

    /**
     * Extracts all cells backwards from the current position until the given character is hit
     * @param splitChar
     * @return
     */
    private fun splitLineAtChar(splitChar: Char): List<Cell> {
        val newCells: MutableList<Cell> = ArrayList()
        var spaceIndex = findPrevChar(_x - 1, splitChar)
        if (spaceIndex > _alignment.leftIndent) { //If entire line gets linewrapped, enters an infinite loop
            while (spaceIndex < cellsPerLine) {

                //For every cell after the last space, make sure it isn't a page number, and transfer it down to the next line
                val cell = pageGrid.getCell(spaceIndex, _y)
                val node = cell?.node
                if (node != null && node !is PageNumber
                ) {
                    val copiedCell = cell.copy()
                    removeCell(spaceIndex, _y)
                    newCells.add(copiedCell)
                }
                spaceIndex++
            }
        }
        return newCells
    }

    val cornerPageLength: Int
        get() {
            val printPageLength = printPageValue.length + 3
            if (_y == 0 || _y > linesPerPage) {
                if (PageBuilderHelper.getPrintPageNumberAt(
                        pageSettings,
                        braillePageNumber.pageNumber
                    ) == PageNumberPosition.TOP_LEFT
                ) {
                    return printPageLength
                } else if (PageBuilderHelper.getBraillePageNumberAt(
                        pageSettings,
                        braillePageNumber.pageNumber
                    ) == PageNumberPosition.TOP_LEFT
                ) {
                    return braillePageNum.length + 3
                }
            } else if (_y == linesPerPage || _y + lineSpacing == linesPerPage) {
                if (PageBuilderHelper.getPrintPageNumberAt(
                        pageSettings,
                        braillePageNumber.pageNumber
                    ) == PageNumberPosition.BOTTOM_LEFT
                ) {
                    return printPageLength
                } else if (PageBuilderHelper.getBraillePageNumberAt(
                        pageSettings,
                        braillePageNumber.pageNumber
                    ) == PageNumberPosition.BOTTOM_LEFT
                ) {
                    return braillePageNum.length + 3
                }
            }
            return 0
        }

    /**
     * After checking Keep With Next and Don't Split properties, creates a new page and begins addBrlChild recursion
     */
    private fun lineWrapToNextPage(
        curResult: MutableSet<PageBuilder>,
        brlChild: Text,
        startOfWord: Int,
        numberType: PageNumberType,
        lineWrapper: LineWrapper
    ): Set<PageBuilder> {
        if (verifySplit()) {
            var curPb: PageBuilder
            do {
                curResult.addAll(startNewPage(numberType))
                curPb = curResult.last()
            } while (curPb._y >= linesPerPage)
            return curResult
        }
        curResult.addAll(startNewPage(numberType))
        curResult.addAll(curResult.last().addBrlChild(brlChild, startOfWord, numberType, lineWrapper))
        return curResult
    }

    /**
     * Returns true if element should not be split across pages
     * @return
     */
    private fun verifySplit(): Boolean {
        return dontSplit || keepWithNext
    }

    /**
     * From the page grid's current position, find the last use of the given char on this line
     * @param start
     * @param prevChar
     * @return
     */
    private fun findPrevChar(start: Int, prevChar: Char): Int {
        var startIndex = start
        while (startIndex >= 0) {
            if (pageGrid.getCell(startIndex, _y)?.char == prevChar) break
            startIndex--
        }
        return startIndex + 1 //Skip the given character
    }

    /**
     * Add Braille to a page.
     *
     * @param brl
     * The Braille to be placed on the page.
     * @return A list of all the PageBuilder objects representing the pages the
     * Braille spans onto.
     */
    @JvmOverloads
    fun addBrl(brl: Element, numberType: PageNumberType = pageNumberType): Set<PageBuilder> {
        return addBrlFromChild(brl, 0, numberType)
    }

    fun addBrl(brl: Element, numberType: PageNumberType, lineWrapper: LineWrapper): Set<PageBuilder> {
        return addBrlFromChild(brl, 0, numberType, lineWrapper)
    }
    // * @param startChild The child node where to start adding content to the page.
    // * @param numberType The page number type to be used should a new page need to be started.
    /**
     * Add the Braille of a brl from a given child node to the page.
     *
     * This method behaves in a similar way to the addBrl methods except it will not affect any of the formatting before the child specified in startChild. Calling this method with startChild=0 is the same as calling addBrl.
     *
     *
     * This method is used for partial reformatting and so one needs to be careful when using this method to ensure that the startChild is where you want to start reformatting from and that the PageBuilder is set with the correct x and y position to start inserting this text on the page. It almost certainly would be wrong to use this method at any time other than the start of a reformat.
     *
     * @param brl The brl element containing the Braille to be added to the page.
     * @return A set of all PageBuilders representing pages this Braille content spans onto.
     */
    fun addBrlFromChild(brl: Element, start: Int): Set<PageBuilder> {
        return addBrlFromChild(brl, start, pageNumberType)
    }

    @JvmOverloads
    fun addBrlFromChild(
        brl: Element,
        startChild: Int,
        numberType: PageNumberType,
        lineWrapper: LineWrapper = RegexLineWrapper()
    ): Set<PageBuilder> {
        require(UTDElements.BRL.isA(brl)) { "Expected brl, received $brl" }
        if (startChild < 0 || startChild != 0 && startChild >= brl.childCount) { // NOPMD
            throw IndexOutOfBoundsException(String.format("startChild %d is not a valid child index", startChild))
        }
        setCurrBrl(brl)

        // Strip old formatting and normalise the text nodes.
        if ("formatting" == brl.getAttributeValue("type")) {
            brl.detach()
            val result: MutableSet<PageBuilder> = HashSet()
            result.add(this)
            return result
        }
        stripFormattingAndNormalise(brl, startChild)

        // TODO: Add handling for the continuation pages
        var added = false
        if (keepWithNext || dontSplit) {
            keepWithNextList.add(PBStyle(this, brl))
            added = true
        } else if (!keepWithNext && keepWithNextList.isNotEmpty() && !keepWithNextList.last().dontSplit) {
            keepWithNextList.add(PBStyle(this, brl))
            keepWithNextList.last().isEndOfChain = true
            added = true
        }
        val pbs: MutableSet<PageBuilder> = LinkedHashSet()
        if (startChild == 0) {
            val curSize = pbs.size.coerceAtLeast(1)
            // If the brl starts spaces then deduct this from the padding spaces
            // as processSpacing does not take the brl so cannot do this automatically.
            if (pendingSpacing.spaces > 0) {
                val leadingSpaces = getLeadingSpaces(brl, startChild)
                pendingSpacing.removeSpace(leadingSpaces)
                log.debug("Spaces pending insertion for padding is {}", pendingSpacing.spaces)
            }
            // Only process spacing when doing entire brl element
            pbs.addAll(processSpacing(numberType))
            pbs.last().ignoreSpacing = false
            if ((keepWithNext || dontSplit) && pbs.size > curSize && pbs.last().containsBrl(brl)) {
                return pbs
            }
        } else {
            pbs.add(this)
        }

        // As we have started processing the element we now disregard the padding
        var curPage = pbs.last()
        curPage.pendingSpaces = 0
        // To allow line wrapping to add child nodes with continuation dots, number signs and other insertions, we need to make a copy of the nodes to be processed so that those insertions do not mess up the looping.
        val childNodes: MutableList<Node> = mutableListOf()
        for (i in startChild until brl.childCount) {
            val child = brl.getChild(i)
            // At the moment only text nodes get added to the page grid, so may as well filter them
            // In the future may be processing instructions and/or elements may be processed as well.
            if (child is Text) {
                childNodes.add(child)
            }
        }
        for (brlChild in childNodes) {
            // Only add Braille from text node children
            if (brlChild is Text) {
                pbs.addAll(curPage.addBrlChild(brlChild, 0, numberType, lineWrapper))
            }
            curPage = pbs.last()
        }
        if (added && keepWithNextList.isNotEmpty()) {
            //Update the pageBuilder for the most recent item in the keepWithNext list
            keepWithNextList[keepWithNextList.size - 1].pageBuilder = pbs.last()
        }
        if (!keepWithNext && !dontSplit) {
            keepWithNextList.clear()
        }

        // If a brl node is empty, make sure the return list contains this
        // PageBuilder
        if (pbs.isEmpty()) pbs.add(this)
        return pbs
    }

    private fun getLeadingSpaces(brl: Element, startChild: Int): Int {
        var focusParent: Node? = brl
        var focusIndex = startChild
        if (focusParent == null) {
            throw NullPointerException("The brl node cannot be null")
        }
        if (focusParent.childCount == 0) {
            return 0
        }
        require(focusParent.childCount > focusIndex) { "The start child index is greater than the index of the last child" }
        val nodeStack: Deque<Node> = LinkedList()
        val indexStack: Deque<Int> = LinkedList()
        // To prevent a NPE on unboxing of the integer, make the indexStack at least 1 entry longer than nodeStack.
        indexStack.push(0)
        var numOfLeadingSpaces = 0
        while (focusParent != null) {
            val focus = focusParent.getChild(focusIndex)
            if (focus.childCount > 0) {
                nodeStack.push(focusParent)
                indexStack.push(focusIndex)
                focusParent = focus
                focusIndex = 0
                continue
            }
            if (focus is Text) {
                // Count up the spaces before any Braille dots
                val strBrl = focus.value
                for (element in strBrl) {
                    if (Character.isWhitespace(element) && element == '\u2800') {
                        numOfLeadingSpaces++
                    } else {
                        return numOfLeadingSpaces
                    }
                }
            }
            focusIndex++
            if (focusIndex == focusParent.childCount) {
                focusParent = nodeStack.pop()
                focusIndex = indexStack.pop()
            }
        }
        return numOfLeadingSpaces
    }

    private fun stripFormattingAndNormalise(brl: Element, startChild: Int) {
        var childNum = startChild
        var textBuffer = StringBuilder()
        while (childNum < brl.childCount) {
            val child = brl.getChild(childNum)
            (child as? Element)?.detach()
                ?: if (child is Text) {
                    textBuffer.append(child.value)
                    child.detach()
                } else {
                    if (textBuffer.isNotEmpty()) {
                        brl.insertChild(textBuffer.toString(), childNum)
                        textBuffer = StringBuilder()
                        childNum++
                    }
                    childNum++
                }
        }
        if (textBuffer.isNotEmpty()) {
            brl.appendChild(textBuffer.toString())
        }
    }

    /**
     * Force PageBuilder to add its current pending lines and pages
     *
     * @return The set of all pages the spacing affects
     */
    fun processSpacing(): MutableSet<PageBuilder> {
        return processSpacing(pageNumberType)
    }

    private fun processSpacing(numberType: PageNumberType): MutableSet<PageBuilder> {
        var numberType = numberType
        val result: MutableSet<PageBuilder> = LinkedHashSet()
        result.add(this)
        var newPageCreated = false
        var ignorePendingLine = false

        //Account for the pending lines that didn't go through the catch in startOfPage()
        if (isBlankPageWithSkipLine && hasRunningHead() && !addingSkipLines && pendingSpacing.linesBefore > 1) {
            //This is only concerned with the beginning of the page so only do linesBefore
            ignorePendingLine = true
            pendingSpacing.linesBefore = 1
        }
        val pendingLinesBefore = pendingSpacing.linesBefore
        var pendingLinesAfter = pendingSpacing.linesAfter
        var pendingLines = max(pendingLinesBefore, pendingLinesAfter)
        if (maxLines >= 0) {
            pendingLines = maxLines
        }
        maxLines = -1
        // Make sure pendingLines is at least the lineSpacing
        // Remember if pendingLines == 0 then it is not requesting insertion of new lines so should be exempt from increase to lineSpacing.
        if (pendingLines in 1 until lineSpacing) {
            pendingLines = lineSpacing
        }
        var pendingPages = pendingSpacing.pages
        if (pendingPages > 0 || pendingLines > 0 || newLinesOverride > 0) {
            var pb = this
            if (pendingPages > 0) {
                // explicit new pages by definition must break keepWithNext
                keepWithNextList.clear()
                keepWithNext = false
                //Zero out the pending lines after but carry over any potential pending lines before for the next pages
                pendingLinesAfter = 0
                pendingSpacing.linesAfter = pendingLinesAfter
                //#7243: Page breaks should ignore line breaks 
                newLinesOverride = 0
                while (pendingPages > 0) {
                    pendingPages--
                    pb.pendingSpacing.pages = pendingPages
                    result.addAll(pb.startNewPage(numberType))
                    pb = result.last()
                    pendingLines = pb.pendingLinesBefore
                    if (isDecrementCont) {
                        pb.decrementContinuationLetter()
                        pb.setDecrementCont(false)
                    }
                    pb.forcedPageBreak = true
                    numberType = pb.pageNumberType
                    newPageCreated = true
                }
                if (forceSpacing) forceSpacing = false
            }
            if (ignoreSpacing) {
                pendingLines = 0
            }
            resetExplicitPages()
            log.trace(
                "pendingPages={} pendingLines={} newLinesOverride={}",
                pendingPages,
                pendingLines,
                newLinesOverride
            )
            if (newLinesOverride > 0) {
                //!!Removing this because it's preventing the user from doing a simple override.
                //They should be able to do any kind of lines override on their end
//				//If current line is empty, count it towards line insertions unless at the top of the page
//				if (y != 0 && y < getLinesPerPage() && isEmptyLine() && hasRunningHead()) {
//					setNewLinesOverride(getNewLinesOverride() - 1);
//				}

                //TODO: Keep With Next and overriding new lines have complicated interactions that require lots of special treatment
                //inside BrailleBlaster. For now, just turn off keepWithNext when new lines are being overridden.
                keepWithNextList.clear()
                keepWithNext = false
                while (newLinesOverride > 0) {
                    pb.nextLine(true, lineSize = 1)
                    newLinesOverride -= 1
                    if (pb.y >= pb.linesPerPage) {
                        result.addAll(pb.startNewPage(numberType))
                        pb = result.last()
                        numberType = pb.pageNumberType
                        newPageCreated = true
                    }
                }
                pb.newLinesOverride = 0
                addingSkipLines = true
                pb.ignoreSpacing = true
            } else {
                /* If current line is blank then count it towards line insertions.
				*  ignorePendingLine - check for when you have carried over more than 2 pending lines
				*  from the previous page with a running head.
				*  Keep the check for running head here to make sure it doesn't delete a required pending line
				*  just because you have your cursor moved to a blank line after the running head (case for heading).
				*/
                if (pb._y < linesPerPage && pb.isEmptyLine && pb.hasRunningHead()
                    && !ignorePendingLine
                    && !pb.forceSpacing
                ) {
                    pendingLines--
                }
                while (pb._y < linesPerPage && pendingLines > 0 //						&& !hasRunningHead()
                ) {
                    // Previously we moved Y by linespacing, but this was a wrong assumption for double line spacing.
                    // We set pendingLines as we need for double line spacing earlier on.
                    pb.moveY(1, false)
                    pendingLines--
                }
            }
            pendingLines = 0
            pb.pendingSpacing.linesBefore = pendingLines
            if (pb._y >= linesPerPage) {
                result.addAll(pb.startNewPage(numberType))
                pb = result.last()
                newPageCreated = true
            }
            setTabbed(false)
            if (addingSkipLines) {
                //Forcing lines on a page may push a braille page number off a page when you're adding it at the end of a page.
                //Readd the page number to prevent this.
                readdBrlPageNumbers()
                pb.disablePPIChecks = pb._y
                addingSkipLines = false
            }
            //When Keep With Next pushes a centered heading to a new page, it takes care of the indent and segment inside startNewPage()
            if ((!keepWithNext || !newPageCreated) && (!dontSplit || !newPageCreated)) {
                if (startOfBlock) {
                    if (pb.hasTopLeftPage(pb.braillePageNumber)) {
                        pb.x = pb.cornerPageLength
                        pb.cellsPerLineWithLeftIndent = cellsPerLine - pb.cornerPageLength
                    } else {
                        pb.x = firstLineIndent
                        pb.cellsPerLineWithLeftIndent = cellsPerLine - firstLineIndent
                    }
                } else {
                    if (pb.hasTopLeftPage(pb.braillePageNumber)) {
                        pb.x = pb.cornerPageLength
                        pb.cellsPerLineWithLeftIndent = cellsPerLine - pb.cornerPageLength
                    } else {
                        pb.x = leftIndent
                        pb.cellsPerLineWithLeftIndent = cellsPerLine - leftIndent
                    }
                }
                initNewSegment()
            }
        } else {
            // Do horizontal padding
            var checkX = x
            val checkY = y
            for (i in checkX - 1 downTo 0) {
                if (pendingSpacing.spaces <= 0) {
                    // When pendingSpaces is not greater than zero then there must by default be sufficient padding
                    break
                }
                val cell = pageGrid.getCell(i, checkY)
                if (cell == null || Character.isWhitespace(cell.char)) {
                    pendingSpacing.removeSpace(1)
                } else {
                    // Move X right by the number of remaining pending spaces
                    log.debug("About to move the X position from {} right by {} cells", checkX, pendingSpacing.spaces)
                    checkX += pendingSpacing.spaces
                    if (checkX > cellsPerLine) {
                        checkX = cellsPerLine
                    }
                    x = checkX
                    break
                }
            }
        }
        //Resetting all the lines and spacing because they should all have been processed
        pendingSpacing.spaces = 0
        pendingSpacing.linesBefore = 0
        pendingSpacing.linesAfter = 0
        newLinesOverride = 0
        return result
    }

    private fun initNewSegment() {
        if (segment != null && isCenteredWithDots) {
            segment!!.rightIndent = 0
            segment!!.leftIndent = 0
        }
        finishSegment()
        segment = SegmentInfo()
        segment!!.leftIndent = leftIndent
        segment!!.rightIndent = rightIndent
        segment!!.alignment = _alignment.align
    }

    fun finishSegment() {
        if (segment != null && !tabbed) {
            alignSegment(segment)
            segment = null
        }
    }

    private fun moveToBlankCell() {
        for (j in x until cellsPerLine - 1) {
            if (pageGrid.getCell(j + 1, y)?.node != null) {
                _x = j + 2
            }
        }
    }

    private fun addCell(cellNum: Int, lineNum: Int, cell: Cell) {
        pageGrid.setCell(cellNum, lineNum, cell, true)
    }

    private fun removeCell(cellNum: Int, lineNum: Int) {
        if (pageGrid.getCell(cellNum, lineNum) != null) {
            pageGrid.setCell(cellNum, lineNum, null, true)
        }
    }

    /**
     * Will add the given string between startIndex and endIndex to the current
     * x and y position of the page grid. Note: Does not check for needed line
     * wrapping. This should be done before calling this method
     *
     * @param brl
     * The brl node to be associated with the cell
     * @param startIndex
     * The index in the brl that the given string occurs
     * @param endIndex
     * The index to stop at
     */
    private fun addStringToPageGrid(
        brl: Node, startIndex: Int,
        endIndex: Int
    ) {
        if (log.isDebugEnabled) {
            log.debug("Adding string: \"{}\" to pageGrid", brl.value.substring(startIndex, endIndex))
        }
        for (i in startIndex until endIndex) {
            addCell(_x, _y, getCellInstance(brl, i, segment))
            _x++
        }
        // As Braille is inserted we know that newLinesOverride is complete
        newLinesOverride = 0
    }

    private fun createPageNumberCells(pageNumber: PageNumber): Array<Cell?> {
        val pageNumberStr = pageNumber.pageNumber
        val result = arrayOfNulls<Cell>(pageNumberStr.length)
        for (i in pageNumberStr.indices) {
            result[i] = getCellInstance(pageNumber, i)
        }
        return result
    }

    /**
     * The maximum number of cells which can be fitted on a line.
     *
     * @return The maximum cells which can be placed on a line.
     */
    val cellsPerLine: Int
        get() = pageGrid.width

    /**
     * Get the cursor for this page.
     *
     * The cursor should always relate to the position of the end of the last
     * inserted text.
     *
     * @return The cursor.
     */
    /**
     * Set the cursor for this page.
     *
     * The cursor to be set on this page. This cursor should have the
     * same page values as the cursor this will be replacing.
     */
    var cursor: Cursor
        get() = Cursor(
            brailleSettings.cellType.getWidthForCells(_x).toDouble(),
            brailleSettings.cellType.getHeightForLines(_y).toDouble()
        )
        set(cursor) {
            val cellType = brailleSettings.cellType
            x = cellType.getCellsForWidth(cursor.x.toBigDecimal())
            y = cellType.getLinesForHeight(cursor.y.toBigDecimal())
        }

    /**
     * Get the left indent of the first line after an explicit new line.
     *
     * The indent is measured from the left page margin to the left edge of the
     * first line of the zone where this PageBuilder will insert Braille. The
     * units for this indent value is in Braille cells.
     *
     * @return The left indent of the first line after an explicit new line, in
     * Braille cells.
     */
    val firstLineIndent: Int
        get() = _alignment.firstLineIndent ?: _alignment.leftIndent

    /**
     * Get the left indent of the area where Braille can be inserted.
     *
     * The left indent is measured from the left page margin to the left edge of
     * the zone where this PageBuilder will insert Braille. The units for this
     * indent value is in Braille cells.
     *
     * @return The left indent in Braille cells.
     */
    val leftIndent: Int
        get() = _alignment.leftIndent

    /**
     * Get the number of lines which can appear on a page.
     *
     * @return The maximum number of lines which can appear on the page.
     */
    val linesPerPage: Int
        get() = if (skipBottom || guideWordEnabled) {
            pageGrid.height - 1
        } else pageGrid.height

    /**
     * Get the total height of the page grid without accounting for the skipBottom style
     * @return
     */
    val totalHeight: Int
        get() = pageGrid.height

    /**
     * Unknown how it differs from the page type in the cursor.
     *
     * @return Some page type value, unknown what it represents.
     *///			setAfterTPage(false);
    var pageNumberType: PageNumberType
        get() = braillePageNumber.pageNumberType
        set(pageNumberType) {
            previousPageNumberType = this.pageNumberType
            braillePageNumber.setPageNumberType(pageNumberType, isContinuePages)
            setNextPageNumberType(pageNumberType)
            if (pageNumberType == PageNumberType.T_PAGE) {
                if (braillePageNumber.pageNumber == 1) {
                    removeRunningHead()
                }
                addPageNumbers()
            } else if (!addedPageNumbers || volumeEnd || afterTPage) {
                addPageNumbers()
                //			setAfterTPage(false);
            }
        }

    fun setNextPageNumberType(pageNumberType: PageNumberType?) {
        nextPageNumberType = pageNumberType
    }

    /**
     * Get the Braille representation of the print page number.
     * useMetaData dictates if it should affect the meta data or not
     *
     * @return The Braille translation of the print page number.
     */
    fun getPrintPageBrl(useMetadata: Boolean): String {
        val isContinueLetter = pageSettings.isPrintPageNumberRange
        var newPrintPage = pageNumbers.printPageBrl
        var continuationLetter =
            if (!isContinueLetter || isAfterTPage()) RepeatingLetters(0) else pageNumbers.continuationLetter

        if (pageNumbers.printPageBrlOverride.isNotEmpty() && continuationLetter.index != 0) {
            newPrintPage = pageNumbers.printPageBrlOverride
        }
        var nextContLetter = RepeatingLetters(0)
        var useNext = false
        if (continuationLetter.index != 0 && pageNumbers.printPageBrl.isNotEmpty()) {
            //Check new print page for number
            val letterIndicator =
                if (engine!!.pageSettings.isPrintPageLetterIndicator && !PageBuilderHelper.isDecimal(newPrintPage[0]))
                    ";"
                else ""
            newPrintPage = continuationLetter.toString() + letterIndicator + newPrintPage
        }

        //Make sure to check the metadata to 
        //print out the correct number value
        if (currentBrl != null) {
            val head = UTDHelper.getDocumentHead(currentBrl!!.document)
            var skipCont = false
            if (head != null) {
                val meta = MetadataHelper.findPrintPageChange(head.document, newPrintPage)
                if (meta != null) {
                    //Display it on the next page but not this page
                    if ((meta.getAttribute("blank") != null && meta.getAttribute("used") == null) || currBlankPageNumber) {
                        if (useMetadata) {
                            MetadataHelper.markUsed(head.document, newPrintPage, "printPage")
                        }
                        useNext = true
                        nextContLetter = continuationLetter.inc().toString().toRepeatingLetters()
                        continuationLetter = RepeatingLetters(0)
                        newPrintPage = ""
                        currBlankPageNumber = true
                    }
                    if (meta.getAttributeValue("new") != null) {
                        newPrintPage = meta.getAttributeValue("new")
                        //Ensure that you've matched the metadata print page to the page in PageNumberTracker
                        pageNumbers.printPageBrl = newPrintPage
                    }
                    //possible change with the continuation letter
                    if (meta.getAttribute("cl") != null && isContinueLetter) {
                        if (meta.getAttributeValue("cl").isNotEmpty()) {
                            continuationLetter = meta.getAttributeValue("cl").toRepeatingLetters()
                        }
                    } else {
                        skipCont = true
                    }
                    if (!skipCont && continuationLetter.index != 0 && pageNumbers.printPageBrl.isNotEmpty()) {
                        newPrintPage = continuationLetter.toString() + newPrintPage
                    }
                    if (meta.getAttribute("pageType") != null) {
                        val metaPageType = PageNumberType.equivalentPage(meta.getAttributeValue("pageType"))
                        if (metaPageType != pageNumberType) {
                            //Allows for interpoint to be handled correctly
                            PageBuilderHelper.setPageNumberType(
                                this,
                                metaPageType,
                                pageSettings.isContinuePages,
                                pageSettings.interpoint
                            )
                            overridePageType = true
                        }
                    }
                }
                val combinedMeta = MetadataHelper.findPrintPageCombiner(head.document, pageNumbers.printPageBrl)
                if (combinedMeta != null) {
                    newPrintPage = combinedMeta.getAttributeValue("new")
                    if (continuationLetter.index != 0 && pageNumbers.printPageBrl.isNotEmpty()) {
                        newPrintPage = continuationLetter.toString() + newPrintPage
                    }
                }
            }
        }
        if (useNext) {
            pageNumbers.continuationLetter = nextContLetter
        } else if (pageNumberType != PageNumberType.T_PAGE) {
            pageNumbers.continuationLetter = continuationLetter
            incrementContinuationLetter()
        }
        contLetter = if (continuationLetter.index != 0) "" else continuationLetter.toString()
        return newPrintPage
    }


    val pageNumberTypeFromMetaData: PageNumberType?
        get() {
            if (currentBrl != null) {
                val head = UTDHelper.getDocumentHead(currentBrl!!.document)
                if (head != null) {
                    val meta = MetadataHelper.findPrintPageChange(head.document, printPageValue)
                    if (meta?.getAttribute("pageType") != null //						&& !getPageNumberType().equals(PageNumberType.T_PAGE)
                    ) {
                        overridePageType = true
                        return PageNumberType.equivalentPage(meta.getAttributeValue("pageType"))
                    }
                }
            }
            return metaPageType
        }

    private fun incrementContinuationLetter() {
        pageNumbers.incrementContinuationLetter()
    }

    fun decrementContinuationLetter() {
        pageNumbers.decrementContinuationLetter()
    }

    fun setPrintPageBrlOverride(printPageBrlOverride: String) {
        pageNumbers.printPageBrlOverride = printPageBrlOverride
    }

    //Need both versions of this function for Java compatibility
    fun setPrintPageBrl(printPageBrl: String){
        pageNumbers.continuationLetter = RepeatingLetters(0)
        pageNumbers.printPageBrl = printPageBrl
    }

    fun setPrintPageBrl(printPageBrl: String, continuationLetter: Counter = RepeatingLetters(0)) {
        pageNumbers.continuationLetter = continuationLetter
        pageNumbers.printPageBrl = printPageBrl
    }
    //Or if the only change is a running head then no need to update anything else

    //The new brl page needs to match the count of the braillePageNumber
    //Check if they want to turn off running head for this page

    //If you don't have any other attributes in the meta but "new" and it's the same as the current page number, return
    //This probably happened because the user turned the running head back on.
    val braillePage: String
        get() {
            var brlPage = braillePageNumber.braillePageNumber
            if (currentBrl != null) {
                val head = UTDHelper.getDocumentHead(currentBrl!!.document)
                if (head != null) {
                    val meta = MetadataHelper.findBraillePageChange(head.document, brlPage)
                    if (meta?.getAttribute("new") != null && meta.getAttributeValue("new")
                            .isNotEmpty() && meta.getAttribute("used") == null
                    ) {
                        var volumeMetaLoc = -1
                        if (meta.getAttribute("pageVolume") != null) {
                            val volumeLoc = meta.getAttributeValue("pageVolume")
                            volumeMetaLoc =
                                volumeLoc.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1].toInt()
                        }
                        val currVolume = UTDHelper.findCurrentVolumeNumber(currentBrl)
                        if (volumeMetaLoc == currVolume || volumeMetaLoc == -1) {
                            MetadataHelper.markUsed(head.document, brlPage, "braillePage")

                            //Check if they want to turn off running head for this page
                            if (meta.getAttribute("runHead") != null && hasRunningHead() && runHeadAdded) {
                                runningHead = ""
                                setSkipNumberLineTop(false)
                                clearRunningHead()
                            }

                            //If you don't have any other attributes in the meta but "new" and it's the same as the current page number, return
                            //This probably happened because the user turned the running head back on.
                            if (meta.attributeCount == 3 && brlPage == meta.getAttributeValue("new") //Or if the only change is a running head then no need to update anything else
                                || meta.attributeCount == 4 && brlPage == meta.getAttributeValue("new") && meta.getAttribute(
                                    "runHead"
                                ) != null
                            ) {
                                return brlPage
                            }

                            //The new brl page needs to match the count of the braillePageNumber
                            brlPage = meta.getAttributeValue("new")
                            updateBraillePageNumber(brlPage)
                        }
                    }
                }
            }
            return brlPage
        }

    //Convert before returning
    val braillePageNum: String
        get() {
            var brlPage = braillePage

            //Convert before returning
            brlPage = TextTranslator.translateText(brlPage, engine!!)
            return brlPage
        }

    /**
     * Get the print page number of this page.
     *
     * @return The print page number for this Braille page.
     */
    var printPageNumber: String
        get() = pageNumbers.printPageNumber
        set(printPageNumber) {
            pageNumbers.printPageNumber = printPageNumber
        }
    var printPageNumberOverride: String
        get() = pageNumbers.printPageNumberOverride
        set(printPageNumberOverride) {
            pageNumbers.printPageNumberOverride = printPageNumberOverride
        }

    /**
     * Get the right indent of the area where Braille will be inserted.
     *
     * When this value is positive it represents the length of the line. When
     * this value is zero or negative it measures the distance from the right
     * margin of the page to the right edge of the area where Braille will be
     * inserted. The units used for this value is Braille cells.
     *
     * @return The right indent of where Braille will be inserted.
     */
    val rightIndent: Int
        get() = _alignment.rightIndent

    /**
     * Clears out any node that is being tracked for keep with next or don't split
     */
    fun clearKeepWithNext() {
        keepWithNext = false
        dontSplit = false
        keepWithNextList.clear()
    }

    fun setKeepWithNext(keepWithNext: Boolean) {
        if (!keepWithNext) {
            lockSplit = false
        }
        if (!lockSplit) {
            this.keepWithNext = keepWithNext
        }
    }

    fun getKeepWithNext(): Boolean {
        return keepWithNext
    }

    /**
     * Determine if Keep With Next will move any text to the next page
     */
    fun hasKeepWithNextQueued(): Boolean {
        return keepWithNextList.isNotEmpty()
    }
    /**
     * Gets the x position of the PageBuilder's internal cursor
     *
     * @return x The horizontal cell number where the next insertion will occur
     */// Changing to a new segment.
    /**
     * Sets the x position of the PageBuilder's internal cursor.
     *
     * Note that this insertion point is not guaranteed. Line wrapping and other
     * factors may move the cursor before insertion occurs
     *
     */
    var x: Int
        get() = _x
        set(x) {
            if (tabbed && x == 0) {
                log.debug("tabbed=true and x=0")
                setTabbed(false)
                return
            }
            if (_x != x) {
                // Changing to a new segment.
                finishSegment()
                log.debug("Setting x to: {}", x)
                _x = if (hasLeftPage()) {
                    cornerPageLength
                } else {
                    x
                }
            }
        }
    /**
     * Gets the x position of the PageBuilder's internal cursor
     *
     * @return x The vertical cell number where the next insertion will occur
     */// Not the same line
    /**
     * Sets the y position of the PageBuilder's internal cursor.
     *
     * Note that this insertion point is not guaranteed. Line wrapping and other
     * factors may move the cursor before insertion occurs
     *
     */
    var y: Int
        get() = _y
        set(y) {
            if (_y != y) {
                // Not the same line
                finishSegment()
                _y = y
            }
        }

    /**
     * Used by partial formatting to ensure page is ready for content.
     * Not intended for general use.
     */
    fun preparePage() {
        if (!addedPageNumbers) {
            addPageNumbers()
        }
    }

    /**
     * This is made public only for certain complex formatters. This does not need to be called outside of PageBuilder
     * in almost all cases.
     */
    fun addPageNumbers() {
        removeExistingPageNumbers()
        setAddedPageNumbers(true)

        //Perform sanity check. Make sure PB pagenum type is the same as BN pagenum type
        if (pageNumberType != braillePageNumber.pageNumberType) {
            braillePageNumber.setPageNumberType(pageNumberType, isContinuePages)
        }

        //Adding the braille page numbers first now because running heads are dependent on them.
        //PPNs can disappear if you remove running heads after already adding PPNs on the page.
        addBrlPageNumbers()
        if (pageNumberType == PageNumberType.T_PAGE && braillePageNumber.pageNumber == 1) {
            removeRunningHead()
        } else if (hasRunningHead()) {
            updateRunningHead()
        }
        var pageNum = getPrintPageBrl(true)
        //pageNumber is correct, but doesn't actually show on the print page. It shows in the braille though!
        val pageNumber: PageNumber
        val position = PageBuilderHelper.getPrintPageNumberAt(pageSettings, braillePageNumber.pageNumber)

        // Protect against long page numbers, probably wrong mark up
        if (pageNum.length > cellsPerLine) {
            pageNum = pageNum.take(cellsPerLine)
        }

        if (pageNum.isNotEmpty() && pageNumberType != PageNumberType.T_PAGE) {
            pageNumber = PrintPageNumber(pageNum)
            val curContLetter = pageNumbers.previousContinuationLetter()
            val pageNumNoCont =
                if (pageNum.length > 1 && pageNum.startsWith(curContLetter.toString()))
                    pageNum.substring(curContLetter.toString().length)
                else
                    pageNum

            pageNumber.addAttribute(Attribute("printPage", printPageNumber))
            pageNumber.addAttribute(Attribute("printPageBrl", pageNumNoCont))
            pageNumber.addAttribute(Attribute("contLetter", curContLetter.toString()))
            pageNumber.addAttribute(Attribute("pageType", pageNumberType.toString()))

            if (verifyPageNumberProperty()) {
                var padding = padding
                if (pageNum.length + this.padding >= cellsPerLine) {
                    padding = 0
                }
                placePageNumber(pageNumber, position, padding)
            }
        }
    }

    fun addBrlPageNumbers() {
        val pageNumber: PageNumber = BrlPageNumber(braillePageNum)
        val untranslated = StringBuilder()
        if (PageNumberType.P_PAGE == pageNumberType) {
            untranslated.append("p")
        } else if (PageNumberType.T_PAGE == pageNumberType) {
            untranslated.append("t")
        }
        untranslated.append(braillePageNumber.pageNumber)
        pageNumber.addAttribute(Attribute("untranslated", untranslated.toString()))
        val position =
            PageBuilderHelper.getBraillePageNumberAt(pageSettings, braillePageNumber.pageNumber)
        if (verifyPageNumberProperty()) {
            placePageNumber(pageNumber, position, padding)
        }
    }

    fun readdBrlPageNumbers() {
        for (y in 0 until pageGrid.height) {
            for (x in 0 until pageGrid.width) {
                val curCell = pageGrid.getCell(x, y)
                if (curCell != null) {
                    if (curCell.node == null || UTDElements.BRL_PAGE_NUM.isA(
                            curCell.node
                        )
                    ) {
                        pageGrid.setCell(x, y, null, true)
                    }
                }
            }
        }
        addBrlPageNumbers()
    }

    fun setAddedPageNumbers(isAdded: Boolean) {
        addedPageNumbers = isAdded
    }

    fun hasAddedPageNumbers(): Boolean {
        return addedPageNumbers
    }

    private fun verifyPageNumberProperty(): Boolean {
        return if (PageBuilderHelper.getPageProperty(braillePageNumber.pageNumber) == PageBuilderHelper.Property.ODD
            && braillePageNumber.pageNumber % 2 == 0) {
            false
        } else PageBuilderHelper.getPageProperty(braillePageNumber.pageNumber) != PageBuilderHelper.Property.EVEN
            || braillePageNumber.pageNumber % 2 == 0
    }

    fun removeExistingPageNumbers() {
        for (y in 0 until pageGrid.height) {
            for (x in 0 until pageGrid.width) {
                val curCell = pageGrid.getCell(x, y)
                if (curCell != null) {
                    if (curCell.node == null || curCell.node is PageNumber) {
                        pageGrid.setCell(x, y, null, true)
                    }
                }
            }
        }
    }

    /**
     * If either is satisfied, do not add a print page indicator:
     * 1. If currently on a number line.
     * 2. If the page is empty.
     * 3. If the page you are on is already the same number as the one you're trying to add.
     *
     * Exception: Blank print page indicators.
     */
    private fun validateInsertPPI(brl: Element?): Boolean {
        var pageNumber = printPageValue
        var printPageNumber = printPageNumber
        if (brl != null && printPageNumber.isEmpty()) {
            pageNumber = brl.getAttributeValue("printPageBrl")
            printPageNumber = brl.getAttributeValue("printPage")
        }

        // If on a number line, no print page indicator should be added except when you have
        // a blank print page number. Blank print page number indicators can be added on line 1.
        if (isNumberLine && _y == 0 && !(pageNumber.isEmpty() || printPageNumber.isEmpty())) {
            //Readd page numbers when the location of the new print page indicator is on the first line
            if (_y == 0 && addedPageNumbers) {
                //Since you're readding it on the same location, roll back the continuation letter once
                decrementContinuationLetter()
                addPageNumbers()
            }
            return false
        }
        var empty = true
        var lineCheck = if (hasRunningHead()) 1 else 0
        while (lineCheck < _y) {
            empty =
                if (lineCheck == 0 || lineCheck == pageGrid.height - 1) pageGrid.isEmptyNumberLine(lineCheck) else pageGrid.isEmptyLine(
                    lineCheck
                )
            if (!empty) break
            lineCheck++
        }
        return !empty || pageNumber.isEmpty() || printPageNumber.isEmpty()
    }

    /**
     * Inserts the print page indicator whenever a new page number has been
     * detected. This has to rest on a completely new line and must contain the
     * print page number at the end of the line.
     */
    fun insertPrintPageIndicator(brl: Element?): Int {
        //Always validate in case another method decides to call insert directly
        if (!validateInsertPPI(brl)) {
            return 0
        }
        var pageNumber = printPageValue
        var printPageNumber = printPageNumber
        //Below is mostly just needed for testing purposes. Should we keep the brl parameter?
        if (brl != null && printPageNumber.isEmpty()) {
            pageNumber = brl.getAttributeValue("printPageBrl")
            printPageNumber = brl.getAttributeValue("printPage")
        }

        var insertedElements = 0

        // Create the brlonly, append, put the text in the page
        pageNumber = pageNumber.take(cellsPerLine)
        val remainingWidth = cellsPerLine - pageNumber.length
        val brlOnly = UTDElements.BRLONLY.create()
        // Add the brlonly to the brl
        if (brl != null && UTDElements.BRL.isA(brl)) {
            insertedElements = 1
            brl.insertChild(brlOnly, 0)
            brl.addAttribute(Attribute("brlonly", "true"))
        }
        val str = StringBuilder()
        val indicator = pageSettings.indicatorChar
        str.append(indicator.repeat(remainingWidth.coerceAtLeast(0)))
        val printStr = str.toString()
        str.append(pageNumber)
        brlOnly.appendChild(str.toString())
        _x = 0
        addStringToPageGrid(brlOnly, 0, str.length)
        ignoreSpacing = false
        //You need to add a line after the page indicator
        addAtLeastLinesAfter(1)
        _x = leftIndent
        brlOnly.addAttribute(Attribute("printIndicator", printStr + printPageNumber))

        //If brl was null, getPrintPageBrl already incremented
        if (brl != null) incrementContinuationLetter()
        yPPI = _y
        return insertedElements
    }

    /**
     * If a print page indicator has no text after it, delete it from the page grid
     */
    fun deletePrintPageIndicator(forceDelete: Boolean) {
        if (yPPI == -1) {
            return
        }
        if (!forceDelete) {
            var i = yPPI + lineSpacing
            while (i < linesPerPage) {
                if (!pageGrid.isEmptyNumberLine(i)) {
                    return
                }
                i += lineSpacing
            }
        }
        pageGrid.getCell(0, yPPI)?.node?.detach()
        pageGrid.clearLine(yPPI)
        pageNumbers.decrementContinuationLetter()
        yPPI = -1
    }

    /**
     * Check if the most recently placed print page indicator is 2 or more spaces from
     * the previous text, and if so move it up
     */
    fun checkPrintPageIndicator() {
        //No need to revisit the line before the print page indicator if it's placed on the first line
        if (yPPI == 0) {
            return
        }
        //Check the line before yPPI
        val emptyAbove = pageGrid.isEmptyLine(yPPI - lineSpacing)
        if (!emptyAbove || disablePPIChecks >= yPPI) {
            return
        }

        /* Check if current y is at least 2 spaces away from the yPPI.
		 * If so, check if the line after yPPI is blank. If it is,
		 * adjust the print page 
		 */if (_y > yPPI + lineSpacing && _y < linesPerPage && emptyAbove && pageGrid.isEmptyLine(yPPI + lineSpacing)) {
            for (i in yPPI.._y) {
                val lineMoved: MutableList<Cell> = ArrayList()
                for (j in 0 until cellsPerLine) {
                    val cell = pageGrid.getCell(j, i)
                    if (cell?.node != null && cell.node !is PageNumber
                    ) {
                        val cellInfo = cell.copy()
                        pageGrid.setCell(j, i, null, true)
                        lineMoved.add(cellInfo)
                    }
                }
                pageGrid.setCells(0, i - lineSpacing, lineMoved)
            }
            yPPI--
            _y -= lineSpacing
        }
    }

    /**
     * Adds the top and bottom box lines. This should add the line on a
     * completely new line if there are other text on the current cursor line.
     * Exception: page numbers. If it is on a number line, keep the box line on
     * the same line, but shorten to accommodate the correct spacing.
     *
     * @param sepString
     * -- The correct separator string as defined in
     * style.getStartSeparator() or style.getEndSeparator()
     */
    fun insertSeparatorLine(sepString: String, color: String?): Element {
        var color = color
        require(sepString.length <= 1) { "The separator string is too long." }
        // We cannot insert a separator line on a non-empty line (page numbers are permitted)
        // Unfortunately we cannot automatically move to the next line as we do not have the capability in this method to start a new page in the event of being on the last line of the page.
        // Therefore we will throw an exception and the caller should handle what to do in the event of a full line.
        if (!isEmptyNumberLine) {
            throw CellOccupiedException("Unable to insert a separator line on non-empty line")
        }
        val brailleStandard = engine!!.brailleSettings.mainTranslationTable
        if (!color.isNullOrEmpty()) {
            // add transcriber note symbols
            try {
                color = engine!!.brailleTranslator.translateString(
                    engine!!.brailleSettings.mainTranslationTable,
                    color, 0
                )
            } catch (e: TranslationException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
            color = getStart(brailleStandard) + color + getEnd(brailleStandard) + " "
        }
        val pageNumber = printPageValue
        val maxLines = linesPerPage
        val brlPagePos = PageBuilderHelper.getBraillePageNumberAt(pageSettings, braillePageNumber.pageNumber)
        val printPagePos = PageBuilderHelper.getPrintPageNumberAt(pageSettings, braillePageNumber.pageNumber)
        var separatorLength = cellsPerLine

        // Special handling if the line contains page numbers
        val str = StringBuilder()
        var pageLength = 0
        val newY = _y + lineSpacing
        var xShift = 0
        if (_y == 0 && (brlPagePos.isTop || printPagePos.isTop)) {
            var numLength: Int
            // Remember both print and Braille page numbers may be on the same line
            // Therefore we must allow accounting for both.
            if (brlPagePos.isTop) {
                numLength = braillePageNum.length
                pageLength += numLength + padding
            }
            if (printPagePos.isTop && pageNumber.isNotEmpty()) {
                numLength = pageNumber.length
                pageLength += numLength + padding
            }
            separatorLength -= pageLength
        } else if (newY >= maxLines
            && (brlPagePos.isBottom || printPagePos.isBottom)
        ) { // NOPMD
            var printNumLength = 0
            var brlNumLength = 0
            var moveXTo = 0
            // Remember about both page numbers being on the same line
            // and the need to account for this.
            if (brlPagePos.isBottom) {
                brlNumLength = braillePageNum.length
                // Check that there is actually a page number
                if (brlNumLength > 0) {
                    brlNumLength += padding
                    if (brlPagePos.isLeft) {
                        moveXTo += brlNumLength
                    }
                }
            }
            if (printPagePos.isBottom) {
                printNumLength = pageNumber.length
                // Check that there is a page number.
                if (printNumLength > 0) {
                    printNumLength += padding
                    if (printPagePos.isLeft) {
                        moveXTo += printNumLength
                    }
                }
            }
            if (!(pageSettings.interpoint && braillePageNumber.pageNumber % 2 == 0)) {
                pageLength = brlNumLength + printNumLength
                separatorLength -= pageLength
                xShift = moveXTo
            }
        }
        val brlOnly = UTDElements.BRLONLY.create()
        val brl = UTDElements.BRL.create()
        brl.addAttribute(Attribute("type", "formatting"))
        brl.appendChild(brlOnly)
        val isColor = !color.isNullOrEmpty()
        for (i in 0 until separatorLength) {
            if (i == separatorLength - 1 && isColor
                && !str.toString().contains(getEnd(brailleStandard))
            ) {
                str.append(getEnd(brailleStandard))
                //for some reason the user has added 
                //a really long color, truncate the color 
                //and insert the transcriber end symbol
                //at the end of the line
            } else if (isColor && color.length > i) {
                str.append(color[i])
            } else {
                str.append(sepString)
            }
        }
        brlOnly.appendChild(str.toString())

        // Use xShift to account for anything at left of page
        // For example page numbers at the left.
        _x = xShift
        addStringToPageGrid(brlOnly, 0, str.length)

        //Add a line after the separator line
        addAtLeastLinesAfter(1)
        ignoreSpacing = false
        _x = leftIndent
        return brl
    }

    /**
     * Insert the top box line and give it the keep with next style option
     * @param sepString Character to be inserted into boxline
     * @param parent Element that will have boxline inserted into
     * @return All pagebuilders created from processSpacing()
     */
    fun insertStartSeparatorLine(sepString: String, color: String?, parent: ParentNode): Set<PageBuilder> {
        val lines = linesToBeInserted
        val isOverrideLines = newLinesOverride > 0

        //#4915: Extra line needed after the running head is already taken care of
//		boolean padded = false;
//		if (isOverrideLines && hasRunningHead()) {
//			//You need an additional line between the running head and the start separator line
//			setNewLinesOverride(getNewLinesOverride() + 1);
//			padded = true;
//		}
        val startOfKWN = keepWithNextList.isEmpty()
        val results = processSpacing()
        var newPB = results.last()

//		if (padded && newPB.isTitlePage()) {
//			//Additional line is not necessary when you end up on a page that don't actually have a running head displayed on it
//			newPB.y -= lineSpacing;
//		}

        //If you're on the last line of the page, move on to a new page
        if (newPB._y == pageGrid.height - 1) {
            newPB.addAtLeastLinesBefore(1)
            newPB.addAtLeastLinesAfter(1)
            results.addAll(newPB.processSpacing())
            newPB = results.last()
        }
        if (newPB !== this && lines + newPB._y < newPB.pageGrid.height && !startOfKWN) {
            //If a new page was made and this box line is preceded by another KWN item,
            //re-add the proper spacing
            newPB.addAtLeastLinesBefore(lines)
            newPB = newPB.processSpacing().last()
        } else if (newPB !== this && startOfKWN
            && !isOverrideLines
            && newPB.hasRunningHead()
        ) {
            //Ensure that box lines are never on line 1 when there is a running head
            //Do this ONLY if you have not forcibly added new lines before the box
            newPB._y = 2
        }
        val sepBrl = newPB.insertSeparatorLine(sepString, color ?: "")
        sepBrl.addAttribute(Attribute("separator", "start"))
        if (color != null) {
            sepBrl.addAttribute(Attribute("color", color))
        }
        val newStyle = PBStyle(sepBrl)
        newStyle.firstLineIndent = 0
        newStyle.leftIndent = 0
        newStyle.pendingLines = lines
        newStyle.pageBuilder = this
        keepWithNextList.add(newStyle)
        parent.insertChild(sepBrl, 0)
        return results
    }

    /**
     * Insert the bottom box line and take away all applied keep with next styles
     * @param sepString
     * @return
     */
    fun insertEndSeparatorLine(sepString: String): Element {
        if (keepWithNextList.isNotEmpty() && "formatting" == keepWithNextList.last().brl.getAttributeValue("type")) keepWithNextList.clear()
        val sepBrl = insertSeparatorLine(sepString, "")
        if (keepWithNext || dontSplit) {
            val newStyle = PBStyle(sepBrl)
            newStyle.firstLineIndent = 0
            newStyle.leftIndent = 0
            newStyle.pendingLines = 1
            newStyle.pageBuilder = this
            keepWithNextList.add(newStyle)
        }
        return sepBrl
    }

    /**
     * Check if the page builder's current line is empty
     * @return
     */
    val isEmptyLine: Boolean
        get() = pageGrid.isEmptyLine(_y)

    fun isEmptyLine(y: Int): Boolean {
        return pageGrid.isEmptyLine(y)
    }

    /**
     * Check if the page builder's current line is empty except for page numbers
     * @return
     */
    val isEmptyNumberLine: Boolean
        get() = pageGrid.isEmptyNumberLine(_y)

    fun isEmptyNumberLine(y: Int): Boolean {
        return pageGrid.isEmptyNumberLine(y)
    }

    private fun isNumberLine(y: Int): Boolean {
        val brlPagePos = PageBuilderHelper.getBraillePageNumberAt(pageSettings, braillePageNumber.pageNumber)
        val printPagePos = PageBuilderHelper.getPrintPageNumberAt(pageSettings, braillePageNumber.pageNumber)

        //for some cases where y is 25 like moving on to new pages
        if ((y == 0 || y == 25) && (brlPagePos.isTop || printPagePos.isTop)) {
            return true
        }
        val maxLines = linesPerPage
        //getLinesPerPage is 1-based, y is 0-based
        val newY = y + lineSpacing
        return (newY == maxLines || y == 24) && (brlPagePos.isBottom || printPagePos.isBottom)
    }

    val isNumberLine: Boolean
        get() = isNumberLine(_y)
    val isBlank: Boolean
        get() = isBlankPage || isBlankPageWithSkipLine || isBlankPageWithPageNumbers || isBlankPageWithRunningHead
    val isBlankPage: Boolean
        get() = pageGrid.isBlankPage
    val isBlankPageWithSkipLine: Boolean
        get() {
            val top = pageGrid.isEmptyNumberLine(0)
            val bottom = pageGrid.isEmptyNumberLine(pageGrid.height - 1)
            val middle = pageGrid.isEmptyLines(1, pageGrid.height - 2)
            var condition = middle && top && bottom
            if (isSkipTop()) {
                condition = middle && bottom
            }
            if (skipBottom) {
                condition = middle && top
            }
            if (isSkipTop() && skipBottom) {
                condition = middle
            }
            if (!isSkipTop() && !skipBottom) {
                condition = false
            }
            return condition
        }
    val isBlankPageWithRunningHead: Boolean
        get() {
            val top = pageGrid.isEmptyNumberLine(0)
            val bottom = pageGrid.isEmptyNumberLine(pageGrid.height - 1)
            val middle = pageGrid.isEmptyLines(1, pageGrid.height - 2)
            var condition = middle && top && bottom
            if (hasRunningHead()) {
                condition = middle && bottom
            }
            return condition
        }
    val isBlankPageWithPageNumbers: Boolean
        get() {
            val top = pageGrid.isEmptyNumberLine(0)
            val bottom = pageGrid.isEmptyNumberLine(pageGrid.height - 1)
            val middle = pageGrid.isEmptyLines(1, pageGrid.height - 2)
            return middle && top && bottom
        }

    /*
	 * Check if the spaces on and before the current x is blank
	 * To use with "3-blank spacing"
	 */
    fun checkBlankBefore(): Boolean {
        return (_x <= 0
                || pageGrid.getCell(_x - 1, _y) == null)
    }// MWhapples: Similar to above, only consider page numbers when there is actually text in the page number.// The continuation letter is included in the string from getPrintPageBrl(true) so we need not consider it in this calculation.// MWhapples: When there is no text for the page number, it does not actually exist and so we must ignore it.//Retrieve the print page brl once or else your continuation letter will keep incrementing
    //Since you're just looking for the length, you don't really want the continuation letter to increment
    /**
     * Get the width of the page number and its padding on the current line
     * @return Length of the page number plus its padding, or -1 if not on a number line
     */
    private val pageNumberWidth: Int
        get() {
            val brlPagePos = PageBuilderHelper.getBraillePageNumberAt(pageSettings, braillePageNumber.pageNumber)
            val printPagePos = PageBuilderHelper.getPrintPageNumberAt(pageSettings, braillePageNumber.pageNumber)
            //Retrieve the print page brl once or else your continuation letter will keep incrementing
            //Since you're just looking for the length, you don't really want the continuation letter to increment
            val printPageBrl = printPageValue
            if (_y == 0) {
                // MWhapples: When there is no text for the page number, it does not actually exist and so we must ignore it.
                if (brlPagePos.isTop && braillePageNum.isNotEmpty()) {
                    return padding + braillePageNum.length
                } else if (printPagePos.isTop && printPageBrl.isNotEmpty()) {
                    // The continuation letter is included in the string from getPrintPageBrl(true) so we need not consider it in this calculation.
                    return printPageBrl.length + padding
                }
            } else if (_y + 1 == linesPerPage) {
                // MWhapples: Similar to above, only consider page numbers when there is actually text in the page number.
                if (brlPagePos.isBottom && braillePageNum.isNotEmpty()) {
                    return padding + braillePageNum.length
                } else if (printPagePos.isBottom && printPageBrl.isNotEmpty()) {
                    return printPageBrl.length + padding
                }
            }
            return -1
        }

    private fun placePageNumber(pageNumber: PageNumber, position: PageNumberPosition, padding: Int) {
        val lastLine = pageGrid.height - 1
        when (position) {
            PageNumberPosition.TOP_LEFT -> placePageNumberLeft(pageNumber, padding, 0)
            PageNumberPosition.BOTTOM_LEFT -> placePageNumberLeft(pageNumber, padding, lastLine)
            PageNumberPosition.TOP_RIGHT -> placePageNumberRight(pageNumber, padding, 0)
            PageNumberPosition.BOTTOM_RIGHT -> placePageNumberRight(pageNumber, padding, lastLine)
            PageNumberPosition.NONE -> {}
        }
    }

    private fun placePageNumberLeft(pageNumber: PageNumber, paddingCells: Int, lineNumber: Int) {
        val pageNumberStr = pageNumber.pageNumber
        val pageNumLength = pageNumberStr.length
        val pageNumCells = createPageNumberCells(pageNumber)
        pageGrid.setCells(0, lineNumber, pageNumCells)
        val paddingCellsArray = arrayOfNulls<Cell>(paddingCells)
        Arrays.fill(paddingCellsArray, 0, paddingCells, getCellInstance(null, -1))
        pageGrid.setCells(pageNumLength, lineNumber, paddingCellsArray)
    }

    private fun placePageNumberRight(pageNumber: PageNumber, paddingCells: Int, lineNumber: Int) {
        val pageNumberStr = pageNumber.pageNumber
        val pageNumLength = pageNumberStr.length
        val pageNumStart = pageGrid.width - pageNumLength
        val pageNumCells = createPageNumberCells(pageNumber)
        pageGrid.setCells(pageNumStart, lineNumber, pageNumCells)
        val paddingCellsArray = arrayOfNulls<Cell>(paddingCells)
        Arrays.fill(paddingCellsArray, 0, paddingCells, getCellInstance(null, -1))
        pageGrid.setCells(pageNumStart - paddingCells, lineNumber, paddingCellsArray)
    }

    @JvmOverloads
    fun hasLeftPage(braillePageNumber: PageNumberTracker? = this.braillePageNumber): Boolean {
        var result = false
        if ((_y == 0 || _y > linesPerPage) && (PageBuilderHelper.getPrintPageNumberAt(
                pageSettings,
                braillePageNumber!!.pageNumber
            ) == PageNumberPosition.TOP_LEFT || PageBuilderHelper.getBraillePageNumberAt(
                pageSettings, braillePageNumber.pageNumber
            ) == PageNumberPosition.TOP_LEFT)
        ) {
            result = true
        } else if ((_y == linesPerPage || _y + lineSpacing == linesPerPage) && (PageBuilderHelper.getPrintPageNumberAt(
                pageSettings, braillePageNumber!!.pageNumber
            ) == PageNumberPosition.BOTTOM_LEFT || PageBuilderHelper.getBraillePageNumberAt(
                pageSettings, braillePageNumber.pageNumber
            ) == PageNumberPosition.BOTTOM_LEFT)
        ) {
            result = true
        }
        return result
    }

    @JvmOverloads
    fun hasTopLeftPage(braillePageNumber: PageNumberTracker? = this.braillePageNumber): Boolean {
        return (_y == 0 || _y > linesPerPage) && (PageBuilderHelper.getPrintPageNumberAt(
            pageSettings,
            braillePageNumber!!.pageNumber
        ) == PageNumberPosition.TOP_LEFT || PageBuilderHelper.getBraillePageNumberAt(
            pageSettings, braillePageNumber.pageNumber
        ) == PageNumberPosition.TOP_LEFT)
    }

    /**
     * Remove the Braille of the brl element from the page.
     *
     * @param brl
     * The brl element to be removed.
     * @return True if the element was found and removed, false in all other
     * cases.
     */
    fun removeBrl(brl: Element): Boolean {
        require(UTDElements.BRL.isA(brl)) { "Expected brl, received $brl" }
        // May be additionally we will want to check if brl is null. Not
        // checking brl == null would offer a way to remove just reserved space
        // not associated to any brl.
        var removedFlag = false
        val childNodes: MutableList<Node> = mutableListOf()
        for (i in 0 until brl.childCount) {
            childNodes.add(brl.getChild(i))
        }
        var move = true
        for (i in 0 until pageGrid.height) {
            for (j in 0 until pageGrid.width) {
                if (pageGrid.getCell(j, i)?.node in childNodes
                ) {
                    //Move x and y back to the first instance of this node
                    if (move) {
                        x = j
                        y = i
                        move = false
                    }
                    removeCell(j, i)
                    removedFlag = true
                }
            }
        }
        return removedFlag
    }

    fun removeFormattingElement(element: Element): Boolean {
        return if (UTDElements.BRLONLY.isA(element)) {
            var removedFlag = false
            var move = true
            for (i in 0 until pageGrid.height) {
                for (j in 0 until pageGrid.width) {
                    // For some reason, this doesn't always work: pageGrid.getCell(j, i).getNode() == element
                    if (pageGrid.getCell(
                            j,
                            i
                        )?.node?.toXML() == element.toXML()
                    ) {
                        if (move) {
                            x = j
                            y = i
                            move = false
                        }
                        removeCell(j, i)
                        removedFlag = true
                    }
                }
            }
            removedFlag
        } else {
            throw IllegalArgumentException("Expected BRLONLY or PAGEBREAK, received $element")
        }
    }

    val isEmptyFormattingLine: Boolean
        //V2 code
        get() = pageGrid.getLine(y).map { it?.node }.noneMatch { it != null && UTDElements.BRLONLY.isA(it) }
        /*
        get() {
            var result = true
            for (x in 0 until pageGrid.width) {
                val node = pageGrid.getCell(x, _y)?.node
                if (node != null && !UTDElements.BRLONLY.isA(
                        node
                    )
                ) {
                    result = false
                    break
                }
            }
            return result
        }

         */

    fun containsBrl(brl: Element): Boolean {
        require(UTDElements.BRL.isA(brl)) { "Expected brl, received $brl" }
        //V2 code
        return pageGrid.cells.anyMatch {
            val node = it?.node
            node != null && brl.childNodes.contains(node)
        }
        /*
        val childNodes: MutableList<Node> = mutableListOf()
        for (i in 0 until brl.childCount) {
            childNodes.add(brl.getChild(i))
        }
        return (0 until pageGrid.height).flatMap { i -> (0 until pageGrid.width).map { j -> i to j } }.any { (i, j) -> pageGrid.getCell(j, i)?.node in childNodes }

         */
    }

    /**
     * Set the left indent of the first line after an explicit new line.
     *
     * The indent is measured from the left page margin to the left edge of the
     * first line of the zone where this PageBuilder will insert Braille. The
     * units for this indent value is in Braille cells.
     *
     * @param firstLineIndent
     * The indent of the first line after an explicit new line, in
     * Braille cells.. Use the null value if you want this to
     * automatically be the same as the left indent value.
     */
    fun setFirstLineIndent(firstLineIndent: Int?): PageBuilder {
        log.debug("Setting firstLineIndent to: {}", firstLineIndent)
        _alignment.firstLineIndent = firstLineIndent
        return this
    }

    /**
     * Set the left indent of where Braille can be inserted on the page.
     *
     * The left indent is measured from the left page margin to the left edge of
     * the zone where this PageBuilder will insert Braille. The units for this
     * indent value is in Braille cells.
     *
     * @param indent
     * The left indent in Braille cells.
     */
    fun setLeftIndent(indent: Int): PageBuilder {
        log.debug("Setting left indent to: {}", indent)
        _alignment.leftIndent = indent
        cellsPerLineWithLeftIndent = cellsPerLine - indent
        return this
    }

    fun isSkipTop(): Boolean {
        val printPos = PageBuilderHelper.getPrintPageNumberAt(pageSettings, braillePageNumber.pageNumber)
        val brlPos = PageBuilderHelper.getBraillePageNumberAt(pageSettings, braillePageNumber.pageNumber)
        return if (hasRunningHead()
            && (brlPos.isTop || printPos.isTop && (printPageNumber.isNotEmpty() || pageNumberType == PageNumberType.T_PAGE))
        ) {
            true
        } else skipTop
    }

    fun setSkipNumberLines(position: NumberLinePosition?): PageBuilder {
        skipNumberLines = position
        if (position == null) {
            setSkipNumberLineBottom(false)
            setSkipNumberLineTop(false)
            return this
        }
        when (position) {
            NumberLinePosition.BOTH -> {
                setSkipNumberLineBottom(true)
                setSkipNumberLineTop(true)
            }

            NumberLinePosition.TOP -> {
                setSkipNumberLineTop(true)
                setSkipNumberLineBottom(false)
            }

            NumberLinePosition.BOTTOM -> {
                setSkipNumberLineBottom(true)
                setSkipNumberLineTop(false)
            }

            NumberLinePosition.NONE -> {
                setSkipNumberLineBottom(false)
                setSkipNumberLineTop(false)
            }

        }
        return this
    }

    /**
     * Set the number of lines to progress when starting a new line.
     *
     * @param lineSpacing
     * The number of lines to progress when starting a new line.
     */
    fun setLineSpacing(lineSpacing: Int): PageBuilder {
        require(lineSpacing >= 1) { "Line spacing must be a positive integer of at least 1." }
        this.lineSpacing = lineSpacing
        return this
    }

    /**
     * Set the right indent of where Braille can be inserted.
     *
     * When this value is positive it represents the length of the line. When
     * this value is zero or negative it measures the distance from the right
     * margin of the page to the right edge of the area where Braille will be
     * inserted. The units used for this value is Braille cells.
     *
     * @param rightIndent
     * The right indent of where Braille can be inserted.
     */
    fun setRightIndent(rightIndent: Int): PageBuilder {
        _alignment.rightIndent = rightIndent
        return this
    }

    fun moveY(lineSpacing: Int, force: Boolean) {
        if (_y == 0 && pageGrid.isEmptyNumberLine(0)
            && !hasRunningHead() // NOPMD 
            && !force && !forceSpacing
        ) {
            return
        }
        _y += lineSpacing

        //Verify if new line has a page. If it does, move x accordingly
        if (hasLeftPage()) {
            _x = cornerPageLength
        }

        //Every time y is moved, check for the space before and after the PPI
        if (yPPI != -1) {
            checkPrintPageIndicator()
        }
        if (forceSpacing) forceSpacing = false
    }

    fun setXY(x: Int, y: Int) {
        if (_x != x || _y != y) {
            finishSegment()
            _x = x
            _y = y
        }
    }

    fun findFirstBlankLine(): Int {
        return findFirstBlankLineAfter(0)
    }

    fun findFirstBlankLineAfter(lineNumber: Int): Int {
        for (i in lineNumber until pageGrid.height) {
            val blank: Boolean = if (isNumberLine(i)) pageGrid.isEmptyNumberLine(i) else pageGrid.isEmptyLine(i)
            if (blank) return i
        }
        return -1
    }

    fun findLastBlankLine(): Int {
        var returnInt = 0
        for (i in 0 until pageGrid.height) {
            val blank: Boolean = if (isNumberLine(i)) pageGrid.isEmptyNumberLine(i) else pageGrid.isEmptyLine(i)
            if (!blank) returnInt = i + 1
        }
        return returnInt
    }

    var pendingSpaces: Int
        get() = pendingSpacing.spaces
        set(spaces) {
            pendingSpacing.spaces = spaces
        }

    fun updatePendingSpaces(requestedSpaces: Int) {
        pendingSpacing.updateSpaces(requestedSpaces)
    }

    /**
     * The number of lines the PageBuilder will add before processing the next
     * brl node
     */
    val pendingLinesBefore: Int
        get() = pendingSpacing.linesBefore
    val pendingLinesAfter: Int
        get() = pendingSpacing.linesAfter
    var newLinesOverride: Int
        get() = pendingSpacing.newLinesOverride
        set(newLinesOverride) {
            pendingSpacing.newLinesOverride = newLinesOverride
        }

    // They will have already been inserted.
    //Need to see if you add linesBefore and linesAfter or you need to pick between the two
    //		return pendingSpacing.getLines();
    val linesToBeInserted: Int
        get() {
            if (pendingSpacing.newLinesOverride > 0) {
                return 0 // They will have already been inserted.
            }
            return if (maxLines >= 0) maxLines else max(pendingSpacing.linesBefore, pendingSpacing.linesAfter)
            //Need to see if you add linesBefore and linesAfter or you need to pick between the two
            //		return pendingSpacing.getLines();
        }

    /**
     * The number of pages the PAgeBuilder will add before processing the next
     * brl node
     */
    val pendingPages: Int
        get() = pendingSpacing.pages

    /**
     * Resets the count for all page numbers
     */
    fun resetPageNumbers() {
        isReset = true
        //		setContinuePages(true);
    }

    /**
     * Resets the count for all page numbers but normal page numbers
     */
    fun resetSpecialPageNumbers() {
        isSpecialReset = true
        isContinuePages = true
    }

    //Move the normal page forward but reset the other two
    fun resetSpecialBrlPageNumbers() {
        braillePageNumber = braillePageNumber.nextPage(PageNumberType.NORMAL, false).apply {
            resetNumberCounters(PageNumberType.T_PAGE, isContinuePages)
            resetNumberCounters(PageNumberType.P_PAGE, isContinuePages)
        }
    }

    /**
     * Starts a new page
     * @param pageNumberStyle
     * @return The new PageBuilder representing the new page
     */
    private fun startNewPage(pageNumberStyle: PageNumberType): Set<PageBuilder> {
        finishSegment()
        if (pageGrid.isBlankPage && !addedPageNumbers) {
            addPageNumbers()
        }
        var noIncrement = false
        if (isBlankPageWithRunningHead && braillePageNumber.pageNumber == 1 && pageSettings.interpoint) {
            //An interpoint blank page should not have a running head
            clearRunningHead()
            removeExistingPageNumbers()
            noIncrement = true
        }
        if (isBlankPage) {
            //If you have an interpoint and you're about to remove an odd braille page number, 
            //roll back the count for the braille page number so the next page gets the correct page number.
            //Don't do this if you're on the first page.
            if (pageSettings.interpoint && braillePageNumber.pageNumber % 2 != 0 && braillePageNumber.pageNumber != 1) {
                braillePageNumber.pageNumber -= 1
            }
            removeExistingPageNumbers()
        }

        //Make sure the last line is empty so that you're right to put guide words there. Avoid overwriting.
        val lastLineIndex = pageGrid.height - 1
        //If your last line had guide words only, then it'll be free. If not, then you still shouldn't touch that line.
        clearGuideWord()
        if (pageGrid.isEmptyLine(lastLineIndex) || pageGrid.isEmptyNumberLine(lastLineIndex)) {
            insertGuideWords(true)
        }
        deletePrintPageIndicator(false)
        var nextPageNumber: PageNumberTracker?
        //v2 code
        when {
            nextPageNumberType != null -> {
                nextPageNumber = braillePageNumber.nextPage(pageNumberStyle, isContinuePages)
                nextPageNumber.setPageNumberTypeContinue(nextPageNumberType!!)
            }
            isOverridePageType() -> {
                nextPageNumber = braillePageNumber.nextPage(pageNumberType, isContinuePages)
                nextPageNumber.setPageNumberTypeContinue(pageNumberType)
            }
            else -> {
                nextPageNumber = braillePageNumber.nextPage(pageNumberStyle, isContinuePages)
            }
        }
        /*
        var nextPageNumber = if (noIncrement) {
            PageNumberTracker(braillePageNumber!!)
        } else if (nextPageNumberType != null) {
            braillePageNumber!!.nextPage(pageNumberStyle, isContinuePages).apply {
                setPageNumberTypeContinue(nextPageNumberType!!)
            }
        } else if (overridePageType) {
            braillePageNumber!!.nextPage(pageNumberType, isContinuePages).apply {
                setPageNumberTypeContinue(pageNumberType)
            }
        } else {
            braillePageNumber!!.nextPage(pageNumberStyle, isContinuePages)
        }
         */
        log.debug("nextPageNumber={}", nextPageNumber.pageNumber)
        if (isReset) {
            braillePageNumber.resetNumberCounters(pageNumberType, isContinuePages)
            resetSpecialBrlPageNumbers()
            nextPageNumber = braillePageNumber
        } else if (isSpecialReset) {
            resetSpecialBrlPageNumbers()
            nextPageNumber = braillePageNumber
        }
        val results: MutableSet<PageBuilder> = LinkedHashSet()
        var nextPage = PageBuilder(engine, braillePageNumber = nextPageNumber)
        results.add(nextPage)

        //Handler for interpoint
        if (isBlankPageWithSkipLine || isBlankPage || isBlankPageWithRunningHead) {
            if (currBlankPageNumber) {
                nextPage.currBlankPageNumber = true
            }
            if (afterBlankPageNumber) {
                nextPage.afterBlankPageNumber = true
            }

            //Move the numbering back
            //if it's not a current blank page or if it's not after a blank page
            if (!currBlankPageNumber) {
                pageNumbers.decrementContinuationLetter()
            }
            for (i in 0 until pageGrid.width) {
                //First line
                val curCell = pageGrid.getCell(i, 0)
                if (curCell?.node is PageNumber) {
                    curCell.stripNode()
                }
            }
            blankPageAdded++
        }
        if (isAfterVolume) {
            nextPage.isAfterVolume = false
        }
        if (volumeEnd) {
            keepWithNextList.clear()
            nextPage.titlePage = true
            setVolumeEnd(false)
            //The only text on the page is "End of Volume"
            if (y == 2) {
                nextPage.isAfterVolume = true
            }
        }
        nextPage.pullStyleOptions(this)
        nextPage.metaPageType = metaPageType
        nextPage.pages = pages
        nextPage.pages.add(nextPage.pageGrid)
        nextPage.pageNumbers = pageNumbers.copy()
        if (nextPage.hasTopLeftPage(nextPage.braillePageNumber)) {
            nextPage._x = nextPage.cornerPageLength
        } else {
            nextPage._x = nextPage._alignment.leftIndent
        }
        //!!NOTE!! removed nextPage.x = x;
        nextPage.engine = engine
        nextPage.leftoverFormattingNodes = leftoverFormattingNodes
        nextPage.initNewSegment()
        nextPage.previousPageNumberType = previousPageNumberType
        nextPage.braillePageNumber.setPageNumberTypeContinue(nextPageNumberType ?: pageNumberStyle)
        nextPage.currentBrl = currentBrl
        nextPage.blankPageAdded = blankPageAdded
        nextPage.overridePageType = overridePageType
        if (currBlankPageNumber && !nextPage.currBlankPageNumber) {
            nextPage.afterBlankPageNumber = true
        }
        nextPage.isRightPage = !isRightPage
        nextPage.isContinuePages = isContinuePages
        nextPage.addPageNumbers()
        //You need to add the running head only after page numbers have already been updated
        //because the running head is based on the page number as well.
        if (nextPage.hasRunningHead()) {
            nextPage.setSkipNumberLineTop(true)
        }
        nextPage.singleGuideWord = endGuideWord
        nextPage.isDecrementCont = isDecrementCont
        nextPage.isFirstPage = false
        if (afterTPage && (!isBlankPage || !isBlankPageWithPageNumbers || !isBlankPageWithRunningHead)) {
            nextPage.afterTPage = false
        }
        if (isTOC || continueSkip) {
            nextPage.setSkipNumberLineTop(skipTop)
            nextPage.setSkipNumberLineBottom(skipBottom)
        }
        if (poemEnabled) {
            nextPage.startOfBlock = startOfBlock
        }
        nextPage.addAtLeastLinesBefore(pendingLinesBefore)
        //		nextPage.addAtLeastLinesAfter(getPendingLinesAfter());
        //If you have more than one pending lines and you have a running head on the new page, add one less pending line
//		if (getPendingLinesBefore() > 1 && nextPage.hasRunningHead()) {
//			nextPage.addAtLeastLinesBefore(getPendingLinesBefore());
//		} else {
//			nextPage.addAtLeastLinesBefore(getPendingLinesBefore());
//		}

        //GW: If the end was changed, make sure the end is the start of the next page
        if (endGuideWordChanged) {
            if (isBlank) {
                nextPage.startGuideWord = endGuideWord
            } else {
                nextPage.startGuideWord = altEndGuideWord
            }
        }

        // If the first and last brl of the list is in the same pb, then assume they can fit on one page
        // If you need more than one page for the entirety of the list, abandon Keep With Next
        var keepWithNextForNextPage = false
        if (pendingSpacing.pages > 0) {
            if (keepWithNextList.isNotEmpty()
                && keepWithNextList[0].pageBuilder != keepWithNextList[keepWithNextList.size - 1].pageBuilder
            ) {
                keepWithNextList.clear()
            } else {
                keepWithNextForNextPage = true
            }
        }
        if (keepWithNextList.isNotEmpty()) {
            if (keepWithNextList.last().isEndOfChain
                && containsBrl(keepWithNextList.last().brl)
                && !keepWithNextList.last().dontSplit
            ) {
                keepWithNextList.clear()
            }
            for (i in keepWithNextList.indices) {
                val style = keepWithNextList[i]
                nextPage.setLeftIndent(style.leftIndent)
                nextPage.setRightIndent(style.rightIndent)
                nextPage.alignment = style.alignment
                nextPage.setFirstLineIndent(style.firstLineIndent)
                if (!style.isEndOfChain) {
                    removeBrl(style.brl)
                    nextPage.keepWithNext = false
                    nextPage.dontSplit = false
                    if (newLinesOverride > 0) {
                        nextPage.newLinesOverride = newLinesOverride
                        newLinesOverride = 0
                    } else {
                        nextPage.addAtLeastLinesBefore(style.pendingLines)
                        nextPage.setMaxLines(style.maxLines)
                    }

                    //Start of page has linesBefore
                    if (nextPage.pendingSpacing.linesBefore > 0) {
                        nextPage.startOfBlock = true
                    }
                    if (i == 0 && nextPage.hasRunningHead()) {
                        //If the page has a running head, it needs to compensate for the fact that y = 1 
                        if (nextPage.pendingSpacing.linesBefore == 1) {
                            nextPage.pendingSpacing.linesBefore = 0
                            nextPage._x = nextPage.firstLineIndent
                        } else if (nextPage.pendingSpacing.linesBefore > 1) nextPage.pendingSpacing.linesBefore = 1
                    }
                    if ("formatting" == style.brl.getAttributeValue("type")) {
                        // Separator lines must always start a new line
                        if (!nextPage.isEmptyNumberLine) {
                            nextPage.moveY(1, true)
                        }
                        results.addAll(nextPage.processSpacing())
                        nextPage = results.last()
                        if (nextPage._y < 2 && nextPage.hasRunningHead()) nextPage._y = 2
                        val newBrl = nextPage.insertSeparatorLine("" + style.brl.value[0], "")
                        val sepAttr = style.brl.getAttributeValue("separator")
                        if (sepAttr != null) newBrl.addAttribute(Attribute("separator", sepAttr))
                        style.brl.parent.replaceChild(style.brl, newBrl)
                        //You need to add the separator in the next page's keepWithNextList if you're planning to add more than 1 page
                        if (keepWithNextForNextPage) {
                            val newStyle = PBStyle(newBrl)
                            newStyle.firstLineIndent = 0
                            newStyle.leftIndent = 0
                            newStyle.pageBuilder = this
                            nextPage.keepWithNextList.add(newStyle)
                        }
                    } else {
                        results.addAll(nextPage.addBrl(style.brl))
                    }
                    nextPage = results.last()
                } else {
                    val lines =
                        if (style.overrideLines > 0) style.overrideLines else if (style.maxLines >= 0) style.maxLines else style.pendingLines
                    nextPage.moveY(lineSpacing * lines, false)
                }
            }
            deletePrintPageIndicator(false)
            nextPage.pageNumbers.continuationLetter = pageNumbers.continuationLetter
            nextPage.addPageNumbers()
            keepWithNextList.clear()
            lockSplit =
                true //We've split once. Don't do it again for this segment of KeepWithNext or DontSplit elements.
        }
        if (!nextPage.startOfBlock) {
            nextPage.setSkipNumberLineTop(nextSkipTop)
        }
        return results
    }

    /**
     * When creating a new page, use this method to copy style options from a previous page builder
     */
    private fun pullStyleOptions(oldBuilder: PageBuilder) {
        _alignment = oldBuilder._alignment.copy()
        setLineSpacing(oldBuilder.lineSpacing)
        setKeepWithNext(oldBuilder.getKeepWithNext())
    }

    /**
     * Add given character to the page grid repeatedly until a non-null cell is hit
     * @param filler Character to fill space with
     * @param padding Number of blank cells to leave before and after characters
     * @param minimum Minimum number of spaces required before adding character (including padding)
     * @return Created brlonly element, or null if no element was created
     */
    fun fillSpace(filler: Char, padding: Int, minimum: Int): Element? {
        var calculatedSpace = 0
        while (_x < pageGrid.width && pageGrid.getCell(_x, _y) != null) {
            _x++ //Find the first null space to start the dots
            if (_x == pageGrid.width) {
                return null
            }
        }
        var curX = _x
        while (curX < pageGrid.width && pageGrid.getCell(curX, _y) == null) {
            curX++
            calculatedSpace++
        }
        return fillSpace(filler, calculatedSpace, padding, minimum)
    }

    /**
     * Add given character to the page grid repeatedly for the given length
     * @param filler Character to fill space with
     * @param length 1-based number of cells to fill space
     * @param padding Number of blank cells to leave before and after characters
     * @param minimum Minimum number of spaces required before adding character (including padding)
     * @return Created brlonly element, or null if no element was created
     */
    fun fillSpace(filler: Char, length: Int, padding: Int, minimum: Int): Element? {
        var length = length
        while (_x < pageGrid.width && pageGrid.getCell(_x, _y) != null) {
            _x++ //Find the first null space to start the dots
            length--
            if (_x == pageGrid.width) {
                return null
            }
        }
        if (length + _x > pageGrid.width) length = pageGrid.width - _x
        return if (length > padding * 2 && length + _x <= pageGrid.width) {
            val onEdgeOfPage = length + _x == pageGrid.width
            val dots = UTDElements.BRLONLY.create()
            val sb = StringBuilder()
            sb.append(filler.toString().repeat(length - if (onEdgeOfPage) padding else padding * 2))
            dots.appendChild(sb.toString())
            _x += padding
            for (i in sb.indices) {
                val newCell = getCellInstance(dots, i)
                addCell(_x, _y, newCell)
                _x++
            }
            if (!onEdgeOfPage) _x += padding
            dots
        } else {
            null
        }
    }

    fun toBRF(): String {
        finishSegment()
        val maxPageLength = (1 + cellsPerLine) * linesPerPage
        val sb = StringBuilder(maxPageLength)
        val lastLineIndex = linesPerPage - 1
        for (i in lastLineIndex downTo 0) {
            if (i == lastLineIndex) {
                sb.insert(0, '\u000c')
            } else {
                sb.insert(0, System.lineSeparator())
            }
            var afterLine = true
            for (j in cellsPerLine - 1 downTo 0) {
                val cell = pageGrid.getCell(j, i)
                if (cell == null) {
                    if (afterLine) {
                        continue
                    } else {
                        sb.insert(0, ' ')
                    }
                } else {
                    sb.insert(0, cell.char)
                    afterLine = false
                }
            }
        }
        return BrailleMapper.UNICODE_TO_ASCII_FAST.map(sb.toString())
    }

    override fun toString(): String {
        val returnString = StringBuilder()
        for (i in 0 until pageGrid.height) {
            var number = "" + i
            if (_y == i) number = ">"
            if (number.length < 2) returnString.append(" ").append(number).append(": ") else returnString.append(number)
                .append(": ")
            for (j in 0 until pageGrid.width) {
                    returnString.append(pageGrid.getCell(j, i)?.char ?: '.')
            }
            returnString.append("\n")
        }
        return returnString.toString()
    }

    /**
     * Write the formatting UTD into the document.
     *
     * This method will write the formatting UTD of this page into the document.
     * It uses the existing document structure writing in place. It is for the
     * caller to decide to save or do anything with the modified document.
     */
    fun writeUTD() {
        if (log.isDebugEnabled) {
            val pageBrf = toBRF()
            log.debug("BRF version of page is: {}", pageBrf)
        }
        finishSegment()
        val cellType = brailleSettings.cellType
        var prevBrl: Element? = null
        var prevBrlChild: Node? = null
        var lastBrlCell: Cell? = null
        val formattingNodes: MutableList<Node> = ArrayList()
        // Below code is disabled because we find the first brl node by XML order and place the left over formatting nodes there instead.
        /*if(leftoverFormattingNodes.size() > 0){
			log.debug("Writing leftoverFormattingNodes {} to formattingNodes", leftoverFormattingNodes);
			formattingNodes.addAll(leftoverFormattingNodes);
			leftoverFormattingNodes.clear();
		}*/
        val blankPage = pageGrid.isBlankPage
        // Every page starts with a newPage
        val newPage = NewPage(
            braillePageNumber.pageNumber,
            braillePage,
            braillePageNum,
            pageNumberType
        )
        if (nonsequentialPages) {
            newPage.addAttribute(Attribute("nonsequential", "true"))
        }
        if (forcedPageBreak) {
            newPage.addAttribute(Attribute(NewPage.PAGE_BREAK_ATTR_NAME, NewPage.PAGE_BREAK_ATTR_VALUE))
        }
        leftoverFormattingNodes.add(newPage)
        // The newPage and left over formatting nodes must appear before the first moveTo in the XML document order, not in page order (top left to bottom right)
        // Therefore we will need to collect the moveTo elements and then find the first in XML order
        var moveTosOnPage: MutableList<Node> = mutableListOf()
        val ncm: MutableMap<Node, List<Cell>> = HashMap()
        for (i in 0 until pageGrid.height) {
            for (j in 0 until pageGrid.width) {
                val curCell = pageGrid.getCell(j, i)
                // Check if the cell has any content
                if (curCell?.node == null) {
                    prevBrl = null
                    prevBrlChild = null
                    continue
                }
                var curBrlChild = curCell.node
                val curBrl = curBrlChild?.parent as Element?
                // Add moveTo when brl changes or follows empty cells
                if (/* On a blank page moveTos should not be inserted for the position between cells of a page number, it would not work anyway */prevBrl == null && prevBrlChild == null || curBrl != null && prevBrl != null && curBrl !== prevBrl || blankPage && !(prevBrlChild === curBrlChild && curBrlChild is PageNumber)) {
                    //Don't add a moveTo if the previous brl node runs directly into this brl node on the same line
                    if (!(// NOPMD
                                j > 0 && pageGrid.getCell(j - 1, i)?.let { c -> c.segmentInfo == curCell.segmentInfo } == true && curCell.node !is PageNumber)
                    ) {
                        val hPos = cellType.getWidthForCells(j)
                        val vPos = cellType.getHeightForLines(i)
                        val moveTo = MoveTo(hPos, vPos)
                        formattingNodes.add(moveTo)
                        moveTosOnPage.add(moveTo)
                    }
                }
                // If node is not attached to any brl add it to queue for adding
                // to brl element
                if (!formattingNodes.contains(curBrlChild) && (curBrl == null && curBrlChild !is Text || blankPage)) {
                    formattingNodes.add(curBrlChild!!)
                }
                // Add any queued formatting nodes to the brl
                if (curBrl != null && formattingNodes.isNotEmpty() && !blankPage) {
                    var position = curBrl.indexOf(curBrlChild)
                    // TODO: Split a text node if appropriate
                    if (curBrlChild is Text && curCell.index > 0) {
                        position++
                        val nodeStr = curBrlChild.value
                        val splitPoint = curCell.index
                        if (splitPoint < nodeStr.length) {
                            var cells: Iterable<Cell>
                            if (ncm.containsKey(curBrlChild)) {
                                cells = ncm[curBrlChild]!!
                                ncm.remove(curBrlChild)
                            } else {
                                cells = pages.getCellsForNode(curBrlChild)
                            }
                            curBrlChild.value = nodeStr.take(splitPoint)
                            val endText: Node = Text(nodeStr.drop(splitPoint))
                            curBrl.insertChild(endText, position)
                            val endCells: MutableList<Cell> = mutableListOf()
                            for (tmpCell in cells) {
                                val newIndex = tmpCell.index
                                if (newIndex >= splitPoint) {
                                    tmpCell.index = newIndex - splitPoint
                                    tmpCell.node = endText
                                    endCells.add(tmpCell)
                                }
                            }
                            ncm[endText] = endCells
                        }
                    }
                    var tablePosition = 0
                    for (formattingNode in formattingNodes) {
                        if (isPageFormattingNode(
                                formattingNodes,
                                formattingNodes.indexOf(formattingNode)
                            ) && "false" == curBrl.getAttributeValue("tableHeading")
                        ) {
                            var findY = 0
                            while (findY < pageGrid.height) {
                                for (findX in 0 until pageGrid.width) {
                                    val node = pageGrid.getCell(
                                        findX,
                                        findY
                                    )?.node
                                    if (node != null && node.parent != null && "true" == (node.parent as Element).getAttributeValue("tableHeading")
                                    ) {
                                        node.parent.insertChild(
                                            formattingNode,
                                            tablePosition
                                        )
                                        tablePosition++
                                        findY = pageGrid.height
                                        break
                                    }
                                }
                                findY++
                            }
                        } else {
                            curBrl.insertChild(formattingNode, position)
                            position++
                        }
                    }
                    formattingNodes.clear()
                    curBrlChild = curBrl.getChild(position)
                }
                prevBrlChild = curBrlChild
                prevBrl = curBrl
                if (curBrl != null) {
                    lastBrlCell = pageGrid.getCell(j, i)
                }
            }
            // A new line always requires a moveTo, this can be achieved by
            // setting the prevBrl to null
            prevBrl = null
            prevBrlChild = null
        }
        log.debug("Formatting nodes remaining: {}", formattingNodes)
        var lastBrlChild: Text? = null
        val lastCellNode = lastBrlCell?.node
        if (lastBrlCell != null && lastCellNode != null) {
            val lastBrl = lastCellNode.parent as Element
            var insertIndex: Int
            if (UTDElements.BRLONLY.isA(lastCellNode)) {
                lastBrlChild = lastCellNode.getChild(0) as Text
                insertIndex = lastBrl.indexOf(lastCellNode) + 1
            } else if (lastCellNode is Text) {
                lastBrlChild = lastCellNode
                insertIndex = lastBrl.indexOf(lastBrlChild)
            } else {
                throw RuntimeException(
                    "PageGrid Cell Node: $lastCellNode, expected Text or BrlOnly"
                )
            }
            if (formattingNodes.isNotEmpty()) {
                // Any outstanding formatting nodes need adding to the last brl for
                // this page.
                var splitIndex = lastBrlCell.index
                val brlStr = lastBrlChild.value
                splitIndex++
                val nodeCells = pages.getCellsForNode(lastBrlChild)
                lastBrlChild.value = brlStr.take(splitIndex)
                val endText = Text(brlStr.drop(splitIndex))
                for (cell in nodeCells) {
                    val newIndex = cell.index
                    if (newIndex >= splitIndex) {
                        cell.index = newIndex - splitIndex
                        cell.node = endText
                    }
                }
                insertIndex++
                if (endText.value.isNotEmpty()) {
                    if (insertIndex > lastBrl.childCount) lastBrl.appendChild(endText) else lastBrl.insertChild(
                        endText,
                        insertIndex
                    )
                }
                log.debug("insertIndex={}", insertIndex)
                for (node in formattingNodes) {
                    if (insertIndex > lastBrl.childCount) lastBrl.appendChild(node) else lastBrl.insertChild(
                        node,
                        insertIndex
                    )
                    insertIndex++
                }
                log.debug("Last brl of the page is: {}", lastBrl.toXML())
                formattingNodes.clear()
            }
        }
        moveTosOnPage = moveTosOnPage.filter { x: Node -> x.parent != null }.toMutableList()
        if (moveTosOnPage.isNotEmpty()) {
            val sortedMoveTos = sortByDocumentOrder(moveTosOnPage)
            val firstMoveTo = sortedMoveTos[0]
            val fp = firstMoveTo.parent
            val insertIndex = fp.indexOf(firstMoveTo)
            // If we iterate in reverse order then we need not alter the index for insertion
            val li = leftoverFormattingNodes.listIterator(leftoverFormattingNodes.size)
            while (li.hasPrevious()) {
                val node = li.previous()
                fp.insertChild(node, insertIndex)
                li.remove()
            }
        }
        if (formattingNodes.isNotEmpty()) {
            leftoverFormattingNodes.addAll(formattingNodes)
        }
        // Now do the adding of the newPage before the first moveTo
        pages.remove(pageGrid)
        writtenUTD = true
        if (lastBrlChild != null) {
            engine!!.callback.onUpdateNode(lastBrlChild)
        }
    }

    private fun isPageFormattingNode(formattingNodes: List<Node>, index: Int): Boolean {
        val node = formattingNodes[index]
        return if (node is NewPage || node is PageNumber || node is BrlOnly && "runningHead" == node.getAttributeValue("type")
        ) true else node is MoveTo && index < formattingNodes.size - 1 && (formattingNodes[index + 1] is PageNumber || formattingNodes[index + 1] is BrlOnly)
    }

    fun hasWrittenUTD(): Boolean {
        return writtenUTD
    }

    private fun alignSegment(info: SegmentInfo?) {
        if (info == null) {
            return
        }
        log.debug("Aligning using info: {}", info)
        val align = info.alignment
        if (align == null) {
            log.debug("Align was null")
            // As we don't do anything for left alignment may as well return.
            return
        }
        val leftBound = info.leftIndent
        var rightBound = info.rightIndent
        rightBound += if (rightBound > 0) {
            leftBound
        } else {
            cellsPerLine
        }
        when (align) {
            Align.RIGHT -> alignRight(info, 0, rightBound)
            Align.CENTERED -> {
                alignCenter(info, leftBound, rightBound)
                if (y < linesPerPage && isTOC) {
                    moveToBlankCell()
                }
            }

            else -> {}
        }
    }

    private fun alignRight(info: SegmentInfo, leftBound: Int, rightBound: Int) {
        log.debug("Aligning segment right {}", info)
        for (lineNum in 0 until pageGrid.height) {
            val segmentStart = calculateSegmentStart(leftBound, rightBound, lineNum, info)
            val segmentEnd = calculateSegmentEnd(leftBound, rightBound, lineNum, info)
            val segmentLineLength = segmentEnd + 1 - segmentStart
            if (segmentLineLength > 0) {
                var moveToIndex = -1
                for (cellNum in rightBound - 1 downTo leftBound) {
                    val cell = pageGrid.getCell(cellNum, lineNum)
                    if (moveToIndex < 0 && cell != null) {
                        if (cell.segmentInfo != info) {
                            // Some reserved cell like page number
                            // Therefore we need to ensure that we do not overwrite it.
                            moveToIndex--
                        } else {
                            // Where to move the last cell of the segment's line to.
                            moveToIndex += rightBound
                        }
                    }
                    if (moveToIndex >= 0) {
                        pageGrid.setCell(moveToIndex, lineNum, cell, true)
                        pageGrid.setCell(cellNum, lineNum, null, true)
                        moveToIndex--
                    }
                }
            }
        }
    }

    private fun alignCenter(info: SegmentInfo, leftBound: Int, rightBound: Int) {
        log.debug("Aligning segment center {}, leftBound={}, rightBound={}", info, leftBound, rightBound)
        val availableWidth = rightBound - leftBound
        for (lineNum in 0 until pageGrid.height) {
            val segmentStart = calculateSegmentStart(0, rightBound, lineNum, info)
            if (segmentStart in 0 until rightBound) {
                val segmentEnd = calculateSegmentEnd(leftBound, rightBound, lineNum, info)
                if (segmentEnd in 0 until rightBound) {
                    val segmentLineLength = segmentEnd + 1 - segmentStart
                    var contentLength = segmentLineLength
                    val trailingCell = pageGrid.getCell(segmentEnd, lineNum)
                    // When the line has a trailing space we should not include it in the calculation of the position of the centering
                    if (trailingCell != null && (trailingCell.char == ' ' || trailingCell.char == '\u2800')) {
                        contentLength--
                    }
                    val preferredStartIndex = (availableWidth - contentLength) / 2 + leftBound
                    if (segmentStart > preferredStartIndex) {
                        // moving text left
                        // Check whether reserved cells affect the preferred start
                        var moveToIndex = segmentStart
                        do {
                            moveToIndex--
                        } while (moveToIndex >= preferredStartIndex && pageGrid.getCell(moveToIndex, lineNum) == null)
                        for (cellNum in segmentStart until segmentEnd + 1) {
                            moveToIndex++
                            val tmpCell = pageGrid.getCell(cellNum, lineNum)
                            pageGrid.setCell(cellNum, lineNum, null, true)
                            pageGrid.setCell(moveToIndex, lineNum, tmpCell, true)
                        }
                        log.debug("Moved content left on line {} to cell {}", lineNum, preferredStartIndex)
                    } else if (segmentStart < preferredStartIndex) {
                        // Moving text right
                        val preferredEnd = preferredStartIndex + segmentLineLength
                        var moveToIndex = segmentEnd + 1
                        while (moveToIndex < preferredEnd && pageGrid.getCell(moveToIndex, lineNum) == null) {
                            moveToIndex++
                        }
                        for (cellNum in segmentEnd downTo segmentStart) {
                            moveToIndex--
                            val tmpCell = pageGrid.getCell(cellNum, lineNum)
                            pageGrid.setCell(cellNum, lineNum, null, true)
                            pageGrid.setCell(moveToIndex, lineNum, tmpCell, true)
                        }
                        log.debug("Moved content right on line {} to cell {}", lineNum, moveToIndex)
                    }
                }
            }
        }
    }

    private fun calculateSegmentEnd(
        leftBound: Int, rightBound: Int, lineNum: Int,
        info: SegmentInfo
    ): Int = (leftBound until rightBound).map { pageGrid.getCell(it, lineNum) }.indexOfLast { it != null && it.segmentInfo == info } + leftBound

    private fun calculateSegmentStart(
        leftBound: Int, rightBound: Int,
        lineNum: Int, info: SegmentInfo
    ): Int = (leftBound until rightBound).map { pageGrid.getCell(it, lineNum) }.indexOfFirst { it != null && it.segmentInfo == info }

    fun setDontSplit(dontSplit: Boolean): PageBuilder {
        if (!dontSplit) {
            lockSplit = false
        }
        if (!lockSplit) {
            this.dontSplit = dontSplit
            if (!dontSplit) keepWithNextList.clear()
        }
        return this
    }

    var padding: Int
        get() = braillePageNumber.padding
        set(padding) {
            braillePageNumber = braillePageNumber.setPadding(padding)
        }

    private fun clearRunningHead() {
        pageGrid.getCell(0, 0)?.node?.detach()
        pageGrid.clearLine(0)
        _y = 0
        runHeadAdded = false
    }

    fun updateRunningHead() {
        for (i in 0 until cellsPerLine) {
            pageGrid.setCell(i, 0, null, true)
        }
        insertRunningHead()
    }

    fun insertRunningHead() {
        if (runningHead.isEmpty() || isFirstNonPPage) {
            return
        }

        //Make sure y is moved on to the next line
        if (_y < 1) _y = 1

        //Count the remaining space based on the placement and length of the page numbers
        val printPos = PageBuilderHelper.getPrintPageNumberAt(pageSettings, braillePageNumber.pageNumber)
        val brlPos = PageBuilderHelper.getBraillePageNumberAt(pageSettings, braillePageNumber.pageNumber)
        val cellsPerLine = cellsPerLine
        val printPage = printPageValue
        var runHead = runningHead
        var runHeadStart = (cellsPerLine - runHead.length) / 2
        var runHeadEnd = runHeadStart + runHead.length

        // Protect against very long page numbers
        if (printPos == PageNumberPosition.TOP_RIGHT || brlPos == PageNumberPosition.TOP_RIGHT) {
            val pageNumStart: Int = if (printPos == PageNumberPosition.TOP_RIGHT) {
                cellsPerLine - (printPage.length + padding)
            } else {
                cellsPerLine - (braillePageNum.length + padding)
            }
            while (runHeadEnd > pageNumStart && runHead.isNotEmpty()) {
                runHead = if (runHead.isNotEmpty()) runHead.dropLast(1) else ""
                runHeadStart = (cellsPerLine - runHead.length) / 2
                runHeadEnd = runHeadStart + runHead.length
            }
        } else if (printPos == PageNumberPosition.TOP_LEFT || brlPos == PageNumberPosition.TOP_LEFT) {
            val pageNumEnd: Int = if (printPos == PageNumberPosition.TOP_LEFT) {
                printPage.length + padding
            } else {
                braillePageNum.length + padding
            }
            if (pageNumEnd > runHeadStart) {
                runHead = runHead.substring(pageNumEnd - runHeadStart)
                runHeadStart = pageNumEnd
            }
        }
        if (runHead.isNotEmpty()) {
            val runningHead = UTDElements.BRLONLY.create()
            runningHead.addAttribute(Attribute("type", "runningHead"))
            runningHead.appendChild(runHead)
            val runningHeadCells = createRunningHeadCells(runningHead)
            pageGrid.setCells(runHeadStart, 0, runningHeadCells)
            runHeadAdded = true
        }
    }

    private fun removeRunningHead() {
        for (i in 0 until pageGrid.width) {
            val curCell = pageGrid.getCell(i, 0)
            if (curCell != null && UTDElements.BRLONLY.isA(curCell.node) && "runningHead" == (curCell.node as Element).getAttributeValue(
                    "type"
                )
            ) {
                removeCell(i, 0)
            }
        }
    }

    private fun createRunningHeadCells(runningHead: Element): Array<Cell?> {
        val runningHeadStr = runningHead.value
        val result = arrayOfNulls<Cell>(runningHeadStr.length)
        for (i in runningHeadStr.indices) {
            result[i] = getCellInstance(runningHead, i)
        }
        return result
    }

    fun setStartGuideWord(startGuideWord: Element?) {
        val lastLineIndex = pageGrid.height - 1
        if (startGuideWord == null) {
            this.startGuideWord = null
            return
        }
        this.startGuideWord = startGuideWord
        clearGuideWord()
        if (pageGrid.isEmptyLine(lastLineIndex) || pageGrid.isEmptyNumberLine(lastLineIndex)) {
            insertGuideWords(false)
        }
    }

    fun getStartGuideWord(): Element? {
        return startGuideWord
    }

    fun setEndGuideWord(endGuideWord: Element?) {
        val lastLineIndex = pageGrid.height - 1
        if (endGuideWord == null) {
            this.endGuideWord = null
            return
        }
        this.endGuideWord = endGuideWord
        clearGuideWord()
        if (pageGrid.isEmptyLine(lastLineIndex) || pageGrid.isEmptyNumberLine(lastLineIndex)) {
            insertGuideWords(false)
        }
    }

    fun getEndGuideWord(): Element? {
        return endGuideWord
    }

    fun isGuideWordEnabled(): Boolean {
        return guideWordEnabled
    }

    fun setGuideWordEnabled(guideWordEnabled: Boolean) {
        this.guideWordEnabled = guideWordEnabled
    }

    private fun clearGuideWord() {
        for (i in 0 until pageGrid.width) {
            val curCell = pageGrid.getCell(i, pageGrid.height - 1)
            if (curCell != null && UTDElements.BRLONLY.isA(curCell.node) && "guideWord" == (curCell.node as Element).getAttributeValue(
                    "type"
                )
            ) {
                removeCell(i, pageGrid.height - 1)
            }
        }
    }

    /**
     * Append the first and last <dt>
    </dt> */
    fun insertGuideWords(isStartPage: Boolean) {
        //Make sure to clear bottom line first before inserting guide words
        clearGuideWord()
        if (endGuideWord == null) {
            endGuideWord = startGuideWord
        }
        if (!isGuideWordEnabled() && singleGuideWord == null || !engine!!.pageSettings.isGuideWords || startGuideWord == null || isBlank) {
            //Push your current endGuideWord as your startGuideWord on the next page
            if (isBlank) {
                endGuideWordChanged = true
            }
            return
        }
        if (isStartPage) {
            //TODO: Double check if your end guide word is correct
            val brlEnd = UTDHelper.getDescendantBrlFastFirst(endGuideWord)
            val brlAltEnd = UTDHelper.getDescendantBrlFastFirst(altEndGuideWord)
            if (brlEnd != null && brlAltEnd != null && !containsBrl(brlEnd) && containsBrl(brlAltEnd)) {
                val temp = endGuideWord
                endGuideWord = altEndGuideWord
                altEndGuideWord = temp
                endGuideWordChanged = true
            }
        }
        var isSingle = false
        if (isNull(startGuideWord) && isNull(endGuideWord) && singleGuideWord != null) {
            startGuideWord = singleGuideWord
            endGuideWord = singleGuideWord
            isSingle = true
        }

        //Get the node value or whatever is written in the attribute
        var startGuideWord = UTDHelper.getTextChild(startGuideWord).value
        var endGuideWord = UTDHelper.getTextChild(endGuideWord).value

        //Don't forget to translate the attribute if the braille is needed.
        if (this.startGuideWord!!.getAttribute("contraction") != null) {
            startGuideWord = this.startGuideWord!!.getAttributeValue("contraction")
        }
        if (this.endGuideWord!!.getAttribute("contraction") != null) {
            endGuideWord = this.endGuideWord!!.getAttributeValue("contraction")
        }
        val guideWords = UTDElements.BRLONLY.create()
        guideWords.addAttribute(Attribute("type", "guideWord"))
        var printGW = startGuideWord.trim { it <= ' ' }
        if (endGuideWord != startGuideWord) {
            printGW += "-" + endGuideWord.trim { it <= ' ' }
        }
        val brlPageLength = braillePageNum.length
        if (printGW.length > cellsPerLine - 6) {
            printGW = printGW.take(cellsPerLine - brlPageLength - 6)
        }
        if (isSingle) {
            printGW += " (cont.)"
        }
        guideWords.addAttribute(Attribute("printIndicator", printGW))
        startGuideWord = TextTranslator.translateText(startGuideWord.trim { it <= ' ' }, engine!!)
        endGuideWord = TextTranslator.translateText(endGuideWord.trim { it <= ' ' }, engine!!)
        val printPos = PageBuilderHelper.getPrintPageNumberAt(pageSettings, braillePageNumber.pageNumber)
        val brlPos = PageBuilderHelper.getBraillePageNumberAt(pageSettings, braillePageNumber.pageNumber)
        var length = cellsPerLine - padding * 2
        val printPage = printPageValue

        //Change the running head's length as the space decreases
        if (printPos.isBottom) {
            length -= printPage.length
        }
        if (brlPos.isBottom) {
            length -= braillePageNum.length
        }
        val guideWord = StringBuilder()
        guideWord.append(startGuideWord)
        var dash = StringBuilder()
        val table = engine!!.brailleSettings.mainTranslationTable.split("-".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()[1]
        if (table == "ueb") {
            dash.append(",-")
        } else if (table == "us") {
            dash.append("--")
        }
        if (!engine!!.brailleSettings.isUseAsciiBraille) {
            val newDash = TextTranslator.translateText(dash.toString(), engine!!)
            dash = StringBuilder()
            dash.append(newDash)
        }

        //If the end is similar to the start guide word
        if (endGuideWord == startGuideWord) {
            dash.delete(0, dash.length)
            endGuideWord = ""
        }
        guideWord.append(dash)
        guideWord.append(endGuideWord)
        // When adding (cont.) this needs to be considered if trimming to fit to the page.
        val contStr = " (cont.)"
        val addLength = if (isSingle) contStr.length else 0
        if (guideWord.length + addLength > length) {
            val newGW = guideWord.substring(0, length - addLength)
            guideWord.delete(0, guideWord.length)
            guideWord.append(newGW)
        }
        if (isSingle) {
            guideWord.append(contStr)
        }
        guideWords.appendChild(guideWord.toString())
        val guideWordCells = arrayOfNulls<Cell>(guideWord.length)
        for (i in guideWord.indices) {
            guideWordCells[i] = getCellInstance(guideWords, i)
        }
        var startingCell = padding
        val centerPad = (length - guideWord.length) / 2
        startingCell += centerPad
        if (printPos == PageNumberPosition.BOTTOM_LEFT) {
            startingCell += printPage.length
        } else if (brlPos == PageNumberPosition.BOTTOM_LEFT) {
            startingCell += braillePageNum.length
        }
        pageGrid.setCells(startingCell, pageGrid.height - 1, guideWordCells)
    }

    private fun isNull(node: Node?): Boolean {
        return node == null
    }

    fun isStartOfBlock(): Boolean {
        return startOfBlock
    }

    fun setStartOfBlock(startOfBlock: Boolean) {
        this.startOfBlock = startOfBlock
    }

    fun setContinueSkip(continueSkip: Boolean) {
        this.continueSkip = continueSkip
    }

    private var maxLines = -1 // Negative number means no limit
    fun setMaxLines(maxLines: Int) {
        this.maxLines = maxLines
    }

    fun getMaxLines(): Int {
        return maxLines
    }

    fun insertLineNumber() {
        if (lineNumber != null) {
            insertLineNumber(lineNumber!!.getAttributeValue("lineNumber"))
        }
    }

    fun insertLineNumber(num: String): Element {
        val lineNumber = UTDElements.BRLONLY.create()
        lineNumber.addAttribute(Attribute("type", "lineNumber"))
        lineNumber.appendChild(num)
        if (lineNumberPos == 0 && isSkipTop()) {
            lineNumberPos += lineSpacing
        }
        val lineNumCells = arrayOfNulls<Cell>(num.length)
        for (i in num.indices) {
            lineNumCells[i] = getCellInstance(lineNumber, i)
        }

        //starting cell is cellsperline - num length
        val startingCell = cellsPerLine - num.length
        pageGrid.setCells(startingCell, lineNumberPos, lineNumCells)
        usedLineNums.add(lineNumber)
        this.lineNumber = null
        return lineNumber
    }

    fun setLineNumberPos(lineNumberPos: Int): Boolean {
        if (this.lineNumberPos != lineNumberPos && lineNumberPos < linesPerPage) {
            this.lineNumberPos = lineNumberPos
            return true
        }
        return false
    }

    fun setLineNumber(lineNumber: Element?) {
        this.lineNumber = lineNumber
    }

    fun getLineNumber(): Element? {
        return lineNumber
    }

    val lineNums: List<Element>
        get() = usedLineNums

    fun setVolumeEnd(volumeEnd: Boolean) {
        this.volumeEnd = volumeEnd
    }

    fun setAfterTPage(afterTPage: Boolean) {
        this.afterTPage = afterTPage
    }

    fun setTabbed(tabbed: Boolean) {
        this.tabbed = tabbed
    }

    fun isOverridePageType(): Boolean {
        return overridePageType
    }

    fun setOverridePageType(overridePageType: Boolean) {
        this.overridePageType = overridePageType
    }

    fun addVolumeBlankLines(): Set<PageBuilder> {
        val result: MutableSet<PageBuilder> = LinkedHashSet()
        result.add(this)

        //Clear any remaining pending lines.
        pendingSpacing.linesBefore = 0
        pendingSpacing.linesAfter = 0
        if (y == yPPI) {
            deletePrintPageIndicator(true)
        }
        val totalLines = linesPerPage
        val nextY = y + 1

        //If you're on a line with text, check following lines
        if (!pageGrid.isEmptyNumberLine(y)) {
            //If you have at least 2 spaces til the end of the page,
            //move y 2 spaces down.
            if (totalLines - nextY > lineSpacing) {
                moveY(lineSpacing * 2, false)
                x = 0
            } else if (totalLines - nextY == 1) {
                moveY(lineSpacing, false)
                x = 0
            } else {
                var pageLength = 0
                if (PageBuilderHelper.getPrintPageNumberAt(
                        pageSettings,
                        braillePageNumber.pageNumber
                    ) == PageNumberPosition.BOTTOM_LEFT || PageBuilderHelper.getPrintPageNumberAt(
                        pageSettings, braillePageNumber.pageNumber
                    ) == PageNumberPosition.BOTTOM_RIGHT
                ) {
                    pageLength = printPageValue.length
                } else if (PageBuilderHelper.getBraillePageNumberAt(
                        pageSettings,
                        braillePageNumber.pageNumber
                    ) == PageNumberPosition.BOTTOM_LEFT || PageBuilderHelper.getBraillePageNumberAt(
                        pageSettings, braillePageNumber.pageNumber
                    ) == PageNumberPosition.BOTTOM_RIGHT
                ) {
                    pageLength = braillePageNum.length
                }
                val remX = cellsPerLine - x - 6 - pageLength
                //If there's space, move x 3 spaces to the right
                if (remX > 0 && remX >= volumeEndLength) {
                    x += 3
                } else {
                    result.addAll(startNewPage(pageNumberType))
                    val newPB = result.last()
                    //Start of a new page so it's a linesBefore
                    newPB.addAtLeastLinesBefore(1)
                }
            }
        } else if (totalLines - y > lineSpacing && y != 0 && !pageGrid.isEmptyNumberLine(y - lineSpacing)) {
            moveY(lineSpacing, false)
        }
        segment = null
        return result
    }

    fun setLineNumberLength(lineNumberLength: Int) {
        this.lineNumberLength = lineNumberLength
    }

    fun getLineNumberLength(): Int {
        return lineNumberLength
    }

    var isCenteredWithDots: Boolean
        get() = _alignment.isCenteredWithDots
        set(centeredWithDots) {
            _alignment.isCenteredWithDots = centeredWithDots
        }

    fun setPoemRightIndent(poemRightIndent: Int) {
        this.poemRightIndent = poemRightIndent
    }

    fun getPoemRightIndent(): Int {
        return poemRightIndent
    }

    fun setIgnoreSpacing(ignoreSpacing: Boolean) {
        this.ignoreSpacing = ignoreSpacing
    }

    fun insertUncontractedGW(uncontractedE: Element, childIndex: Int): MutableSet<PageBuilder> {
        val results: MutableSet<PageBuilder> = LinkedHashSet()
        val pb = this
        results.add(pb)
        var cellsPerLine = cellsPerLine

        //Check for a space before the element and space after
        val textBefore = uncontractedE.query("preceding::text()[1]")[0].value
        val hasSpaceBefore = ' ' == textBefore[textBefore.length - 1] || uncontractedE.getAttribute("spaced") != null

//		String textAfter = uncontractedE.query("following::text()[1]").get(0).getValue();
//		boolean hasSpaceAfter = ' ' == textAfter.charAt(0);

        //If uncontracted text is the same as the contracted version then return
        val contracted = TextTranslator.translateText(uncontractedE.getAttributeValue("term"), engine!!)
        val uncontracted =
            TextTranslator.translateText(uncontractedE.getAttributeValue("term"), engine!!, tableType = BrailleTableType.UNCONTRACTED)
        if (contracted == uncontracted) {
            return results
        }
        if (!hasSpaceBefore && x + 1 < cellsPerLine) {
            if (x + 1 > this.cellsPerLine) {
                //No more space, go to the next line
                moveY(1, false)
                x = leftIndent
            } else {
                x += 1
            }
        }
        val uncontractedGW = UTDElements.BRLONLY.create()
        uncontractedGW.addAttribute(Attribute("type", "pronunciation"))
        uncontractedGW.appendChild(uncontracted)
        // Attach the brlonly to the brl we want it to appear in
        // Add it after the requested index, or at the end if childIndex is not a valid index
        if (childIndex >= 0 && childIndex <= uncontractedE.childCount) {
            uncontractedE.insertChild(uncontractedGW, childIndex)
        } else {
            uncontractedE.appendChild(uncontractedGW)
        }
        val uncontractedGWCells = createUncontractedGWCells(uncontractedGW)
        if (isNumberLine) {
            cellsPerLine -= pageNumberWidth
        }
        if (uncontractedGWCells.size > cellsPerLine - _x) {
            if (_y + lineSpacing < linesPerPage) {
                moveY(lineSpacing, false)
                x = leftIndent
            } else {
                results.addAll(startNewPage(pageNumberType))
            }
        }
        results.addAll(addBrlOnlyGW(results, uncontractedGWCells, uncontracted))

        //All your line wrapping should be done at this point.
        //The brl line wrapping should take care of moving the cursor to the right position.
//		if (getX() + uncontracted.length() > getCellsPerLine()) {
//			//No more space, go to the next line
//			moveY(1, false);
//			setX(getLeftIndent());
//		}
//		else {
//			pb.setX(getX() + uncontracted.length());
//		}
        uncontractedE.removeAttribute(uncontractedE.getAttribute("type"))
        return results
    }

    private fun createUncontractedGWCells(uncontractedGW: Element): Array<Cell?> {
        val uncontractedGWStr = uncontractedGW.value
        val result = arrayOfNulls<Cell>(uncontractedGWStr.length)
        for (i in uncontractedGWStr.indices) {
            result[i] = getCellInstance(uncontractedGW, i)
        }
        return result
    }

    fun setNonsequentialPages(nonsequential: Boolean) {
        nonsequentialPages = nonsequential
    }

    fun insertNewLineOverride(): SetList<PageBuilder> {
        if (addingSkipLines) {
            processSpacing()
        }
        val overrideLines = newLinesOverride + 1
        newLinesOverride = overrideLines
        val pages: SetList<PageBuilder> = SetList()
        pages.add(this)
        return pages
    }

    fun setCurrBrl(currBrl: Element?) {
        currentBrl = currBrl
    }

    fun isAfterTPage(): Boolean {
        return afterTPage
    }

    fun setDecrementCont(isDecrementCont: Boolean) {
        this.isDecrementCont = isDecrementCont
    }

    /*
	 * Add brlonly elements to the page.
	 * Handles any kind of line-wrapping that might be needed
	 * in order to fit the text on the page.
	 */
    fun addBrlOnlyGW(results: MutableSet<PageBuilder>, brlOnly: Array<Cell?>, brlOnlyStr: String): Set<PageBuilder> {
        val pageBuilder = results.last()
        val cellsPerLine = cellsPerLine

        //Linewrap
        if (brlOnly.size > cellsPerLine - pageBuilder.x) {
            //Get the remaining space
            results.addAll(splitBrlOnlyString(brlOnlyStr, results))
        } else {
            pageBuilder.pageGrid.setCells(pageBuilder.x, pageBuilder.y, brlOnly)
        }
        return results
    }

    /*
	 * This needs to consider the possibility of having a really long brlonly string
	 * such that it doesn't fit on just two lines of the page grid.
	 * You will then have to keep on line wrapping onto the next lines and possibly the next page.
	 * 
	 * This should take care of the line wrapping. 
	 * Call another method that would add the brl to the grid itself.
	 */
    private fun splitBrlOnlyString(brlOnly: String, results: MutableSet<PageBuilder>): Set<PageBuilder> {
        val pageBuilder = results.last()
        //Split your brlonly using spaces 
        var brlOnlyArr: Array<String>? = brlOnly.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val maximumLength = pageBuilder.cellsPerLine - pageBuilder.x

        //Gather up strings from the start until it covers the maximum length
        val maxStr = StringBuilder()
        var go = true
        while (go) {
            if (brlOnlyArr != null && maxStr.length + brlOnlyArr[0].length < maximumLength) {
                maxStr.append(brlOnlyArr[0])
                maxStr.append(" ")
                brlOnlyArr = if (brlOnlyArr.size > 1) {
                    brlOnlyArr.drop(1).toTypedArray()
                } else {
                    null
                }
            } else {
                go = false
            }
        }

        //Append the string to the current line in the document
        val brlOnlyTemp = UTDElements.BRLONLY.create()
        brlOnlyTemp.addAttribute(Attribute("type", "pronunciation"))
        brlOnlyTemp.appendChild(maxStr.toString())
        val brlOnlyTempCells = createUncontractedGWCells(brlOnlyTemp)
        //Append the substring to the line
        pageBuilder.pageGrid.setCells(pageBuilder.x, pageBuilder.y, brlOnlyTempCells)
        if (brlOnlyArr != null) {
            //Move it to the next line
            pageBuilder.moveY(lineSpacing, true)
            val newStr = StringBuilder()
            for (s in brlOnlyArr) {
                newStr.append(s)
                newStr.append(" ")
            }
            results.addAll(splitBrlOnlyString(newStr.toString(), results))
        } else {
            //You're at the end of the linewrapping so set your x one more space to the right?
            pageBuilder.x += 1
        }
        return results
    }

    private fun updateBraillePageNumber(pageNumber: String) {
        var pageNumber = pageNumber
        var pageType = PageNumberType.NORMAL

        /*
		 * For older versions of the dialog, we used ASCII for braille page editing.
		 * That means older files will have this format: p#a.
		 * Some users might still edit files like that for some reason so we need
		 * to take into account this possibility.
		 */
        var page = pageNumber.split("#".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        //To the left of the split is the page type
        if (page.size > 1) {
            pageType = getPageTypeFromKey(page[0])
            pageNumber = page[1]
        } else {
            //Separate the number from the letter: p1
            page = pageNumber.split("(?<=\\D)(?=\\d)".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            //Repeat check for second instance
            if (page.size > 1) {
                pageType = getPageTypeFromKey(page[0])
                pageNumber = page[1]
            } else {
                pageNumber = page[0]
            }
        }
        metaPageType = pageType
        if (pageType != pageNumberType) {
            //Handles the interpoint correctly
            PageBuilderHelper.setPageNumberType(this, pageType, pageSettings.isContinuePages, pageSettings.interpoint)
            pageNumberType = pageType
        }
        braillePageNumber.pageNumber = getIntegerPageNumberFromString(pageNumber)
        //		braillePageNumber.setPageNumber(pageType, getIntegerPageNumberFromString(pageNumber));
    }

    private fun getIntegerPageNumberFromString(pageNumber: String): Int {
        var pageNumber = pageNumber
        try {
            pageNumber.toInt()
        } catch (_: NumberFormatException) {
            //Take each character from the string and assign a number to it
            val number = StringBuilder()
            var i = 0
            while (i < pageNumber.length) {
                val equivalent = pageNumber[i].code % 96
                number.append(equivalent)
                i++
            }
            pageNumber = number.toString()
        }
        return pageNumber.toInt()
    }

    private fun getPageTypeFromKey(key: String): PageNumberType {
        when (key) {
            "t" -> return PageNumberType.T_PAGE
            "p" -> return PageNumberType.P_PAGE
        }
        return PageNumberType.NORMAL
    }//Changes here should, in no way, affect the metadata BUT should make use of it

    //Only addPageNumbers() should set currBlankPageNumber to true
    //Retrieve only the print page string but make sure all the indicators stay the same as before
    val printPageValue: String
        get() {
            val origCount = pageNumbers.continuationLetter
            val origBlank = currBlankPageNumber
            //Changes here should, in no way, affect the metadata BUT should make use of it
            val pageNumber = getPrintPageBrl(false)
            pageNumbers.continuationLetter = origCount
            //Only addPageNumbers() should set currBlankPageNumber to true
            currBlankPageNumber = origBlank
            return pageNumber
        }

    fun hasRunningHead(): Boolean {
        //If on the first line of the page or just starting, you can only assume the ff:
        var value = runningHead.isNotEmpty() && !isFirstNonPPage

        //But if inquiring from the later part of the page
        //assume it has already been added so check for the first line if it's empty
        if (y > 0) {
            value = value && !isEmptyNumberLine(0)
        }
        return value
    }

    val isFirstNonPPage: Boolean
        get() = (braillePageNumber.pageNumberType != PageNumberType.P_PAGE
                && braillePageNumber.pageNumber == 1)

    fun updatePageNumberType(pageType: PageNumberType) {
        //If the page type is already what it's supposed to be, return
        if (pageType == pageNumberType) {
            return
        }
        if (PageNumberType.T_PAGE != pageNumberType) {
            previousPageNumberType = pageNumberType
        }
        //For t-pages, it only gets set in this instance
        overridePageType = true
        //Allows for interpoint to be handled correctly
        PageBuilderHelper.setPageNumberType(this, pageType, pageSettings.isContinuePages, pageSettings.interpoint)
    }

    companion object {
        /**
         * Factory method for creating Cell objects.
         *
         * This method will generate a Cell instance suitable for the node being
         * passed in. Most nodes will be able to use the general Cell type but some
         * may need alternative implementations for finding the correct character at
         * the index specified.
         *
         * @param node
         * The node this Cell should point to.
         * @param index
         * The index of the character referenced by this Cell.
         * @return The Cell which can be inserted into the page grid.
         */
        private fun getCellInstance(node: Node?, index: Int): Cell {
            return getCellInstance(node, index, null)
        }

        /**
         * Factory method for creating Cell objects.
         *
         * This method will generate a Cell instance suitable for the node being
         * passed in. Most nodes will be able to use the general Cell type but some
         * may need alternative implementations for finding the correct character at
         * the index specified.
         *
         * @param node
         * The node this Cell should point to.
         * @param index
         * The index of the character referenced by this Cell.
         * @param lineInfo
         * The line information of the line segment the cell should
         * belong to.
         * @return The Cell which can be inserted into the page grid.
         */
        private fun getCellInstance(
            node: Node?, index: Int,
            lineInfo: SegmentInfo?
        ): Cell {
            return Cell(node, index, lineInfo)
        }

    }
}

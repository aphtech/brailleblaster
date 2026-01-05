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
import org.brailleblaster.bbx.BBX
import org.brailleblaster.exceptions.CursorMovementException
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.*
import org.brailleblaster.perspectives.braille.mapping.interfaces.Uneditable
import org.brailleblaster.perspectives.braille.messages.*
import org.brailleblaster.perspectives.braille.views.style.BreadcrumbsToolbar
import org.brailleblaster.perspectives.braille.views.wp.formatters.EditRecorder
import org.brailleblaster.perspectives.braille.views.wp.listeners.ImageDisposeListener
import org.brailleblaster.perspectives.braille.views.wp.listeners.ImagePaintListener
import org.brailleblaster.perspectives.braille.views.wp.listeners.WPPaintListener
import org.brailleblaster.perspectives.braille.views.wp.listeners.WPScrollListener
import org.brailleblaster.perspectives.mvc.XMLNodeCaret
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.events.XMLCaretEvent
import org.brailleblaster.utd.actions.GenericBlockAction
import org.brailleblaster.utd.properties.Align
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.util.FormUIUtils
import org.brailleblaster.util.LINE_BREAK
import org.brailleblaster.utils.swt.AccessibilityUtils.setName
import org.brailleblaster.utils.swt.EasySWT
import org.brailleblaster.utils.xml.BB_NS
import org.brailleblaster.utils.xml.UTD_NS
import org.eclipse.swt.SWT
import org.eclipse.swt.accessibility.AccessibleAdapter
import org.eclipse.swt.accessibility.AccessibleEvent
import org.eclipse.swt.accessibility.AccessibleTextEvent
import org.eclipse.swt.accessibility.AccessibleTextExtendedAdapter
import org.eclipse.swt.custom.*
import org.eclipse.swt.dnd.Clipboard
import org.eclipse.swt.dnd.TextTransfer
import org.eclipse.swt.dnd.Transfer
import org.eclipse.swt.events.*
import org.eclipse.swt.graphics.Point
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Listener
import org.slf4j.LoggerFactory
import java.awt.Desktop
import java.net.URI

class TextView(manager: Manager, sash: Composite) : WPView(manager, sash) {
    val state: ViewStateObject = ViewStateObject()
    private val selection: Selection = Selection()
    var currentChanges = 0
        private set

    @JvmField
    var textChanged = false
    private var range: StyleRange? = null
    private var selectionListener: SelectionAdapter? = null
    private var scrollbarListener: WPScrollListener? = null
    private var verifyKeyListener: VerifyKeyListener? = null

    //	private VerifyListener verifyListener;
    private var modListener: ExtendedModifyListener? = null
    private var focusListener: FocusAdapter? = null
    private var caretListener: CaretListener? = null
    private var mouseListener: MouseAdapter? = null
    private var paintObjListener: PaintObjectListener? = null
    private var paintListener: PaintListener? = null
    private var menuKeyListener: Listener? = null
    var currentElement: TextMapElement? = null
        private set
    private var currentIndex = 0
    var isMultiSelected: Boolean
        private set
    private var focusListenerLock: Boolean
    private var editRecorder: EditRecorder
    private var keyHandler: SixKeyHandler? = null

    init {
        editRecorder = EditRecorder(manager, this)
        isMultiSelected = false
        focusListenerLock = false
        addAccessibilityListener()
    }

    override fun initializeListeners() {
        super.initializeListeners()
        view.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                if (e.x != e.y) { //only fire if something's actually being selected (prevents weird edge cases)
                    setSelection()
                } else {
                    setSelection()
                }
            }
        }.also { selectionListener = it })
        val vkl = TextVerifyKeyListener(manager, this, state, selection, validator, editRecorder)
        keyHandler = SixKeyHandler(null, vkl, java.lang.Boolean.getBoolean("sixKey"))
        view.addVerifyKeyListener(keyHandler.also { verifyKeyListener = it })
        view.addKeyListener(keyHandler)
        view.addExtendedModifyListener(ExtendedModifyListener { e: ExtendedModifyEvent ->
            if (!lock) {
                if (e.length > 0) handleTextEdit(e) else handleTextDeletion(e)
            }
        }.also { modListener = it })
        addFocusListener()

        //SWT needs a specific listener to detect the menu button, updates context menu before displaying
        view.addListener(SWT.MenuDetect, Listener { updateContextMenu() }
            .also { menuKeyListener = it })

        view.addCaretListener(CaretListener { e: CaretEvent ->
            if (!lock) {
                if (view.selectionCount == 0) isMultiSelected = false
                if (manager.stylePane.updateStylePane && !manager.textView.isDisposed) manager.stylePane.updateCursor(
                    manager.textView.getLineAtOffset(manager.textView.caretOffset)
                )
                logger.debug(
                    "Current char '{}' event {} stateObj {}",
                    view.caretOffset,
                    e.caretOffset,
                    state.currentChar,
                    RuntimeException("CaretEVent")
                )
                if (atRunningHeadLine() || atGuideWordLine()) {
                    val line = view.getLineAtOffset(e.caretOffset)
                    val oldLine =
                        if (state.oldCursorPosition < 0 || state.oldCursorPosition > view.charCount) 0 else view.getLineAtOffset(
                            state.oldCursorPosition
                        )
                    val newOffset: Int = if (oldLine < line) {
                        view.getOffsetAtLine(if (line == view.lineCount - 1) line - 1 else line + 1)
                    } else {
                        val expectedLine = if (line == 0) 1 else line - 1
                        view.getOffsetAtLine(expectedLine) + view.getLine(expectedLine).length
                    }

                    //A selection is not being made and shift is not being held down
                    if (view.selectionRanges[1] == 0 && state.currentStateMask and SWT.SHIFT == 0) {
                        view.caretOffset = newOffset
                    }
                }
                val currentStart = state.currentStart
                val currentEnd = state.currentEnd
                if (isArrowKey(state.currentChar) || isNavKey(state.currentChar)) {
                    if (e.caretOffset !in (currentStart + 1)..<currentEnd) {
                        if (textChanged) sendUpdate()
                        if (view.selectionRanges[1] > 0) setSelectionElements(
                            selection.selectionStart,
                            selection.selectionEnd
                        ) else setCurrent(view.caretOffset)
                        state.currentChar = ' '.code
                        state.currentStateMask = -1
                    } else if (!textChanged && e.caretOffset >= state.originalStart && e.caretOffset < state.originalEnd) {
                        if (view.selectionRanges[1] > 0) setSelectionElements(
                            selection.selectionStart,
                            selection.selectionEnd
                        ) else setCurrent(view.caretOffset)
                    }
                    sendStatusBarUpdate(view.getLineAtOffset(view.caretOffset))
                }
                //setCurrent(view.getCaretOffset());
                if (view.getLineAtOffset(view.caretOffset) != currentLine) sendStatusBarUpdate(view.getLineAtOffset(view.caretOffset))

                //Change context menu if cursor is in a table
                updateContextMenu()
            }
        }.also { caretListener = it })
        view.addMouseListener(object : MouseAdapter() {
            override fun mouseDown(e: MouseEvent) {
                if (e.button == 1 && e.stateMask == SWT.MOD1){
                  //Basically, check if selection is a link, get the href attribute, open it in browser
                  //If it's an internal link, find whatever node it points to and move the cursor there
                  val current = manager.mapList.current
                  if (BBX.INLINE.LINK.isA(current.nodeParent)){
                    val el = current.node.parent as Element
                    val isExternal = el.getAttributeValue("external", BB_NS).toBoolean()
                    val href = el.getAttributeValue("href", BB_NS)
                    //Get the href attribute and open it in a browser
                    try {
                      if (!href.isNullOrBlank()) {
                        if (isExternal) {
                          //Need a method for URL checking, for now just assume it's valid - browser can handle it.
                          Desktop.getDesktop().browse(URI(href))
                        }
                        else {
                          //Find block with matching linkID and move the navigation pane to it.
                          val xpath = """//*[@*[local-name() = 'linkID']]"""
                          val internalLinkNodes = manager.simpleManager.doc.query(xpath).toList()
                          //println("Found ${internalLinkNodes.size} bookmarks in doc")
                          for (node in internalLinkNodes){
                            val np = node as Element
                            val linkID = np.getAttributeValue("linkID", BB_NS).toString()
                            //println("Found internal link candidate with linkID: $linkID")
                            if (linkID == href){
                              //Found the target node, move the cursor there
                              //println("Moving to internal link with linkID: $linkID\n node: ${np.toXML()}")
                              manager.simpleManager.dispatchEvent(XMLCaretEvent(Sender.GO_TO_PAGE, XMLNodeCaret(np)))
                              break
                            }
                          }
                        }
                      }
                      else {
                        logger.warn("Tried to open a link with no href attribute")
                      }
                    }
                    catch (e: Exception){
                      //println("Error opening link: " + e.message)
                    }
                  }
                  else if (BBX.BLOCK.IMAGE_PLACEHOLDER.isA(current.nodeParent)){
                      //Open the image via the system default
                      val src = current.nodeParent.getAttributeValue("src", UTD_NS)
                      try {
                          if (!src.isNullOrEmpty()) {
                              println("Opening image placeholder with src: $src")
                              Desktop.getDesktop().open(java.io.File(src))
                          }
                      } catch (ex: Exception) {
                          logger.warn("Error opening image placeholder: " + ex.message)
                      }
                  }
                  else{
                    //Do nothing
                    //println("Clicked element is not a link")
                  }
                }
                if (e.button == 2) return
                if (e.button == 3 && !view.isTextSelected) {
                    //This fixes the bug where right-clicking in the indent margin would send the caret to the doc start.
                    try {
                        val offsetPoint = view.getOffsetAtPoint(Point.WithMonitor(e.x, e.y, Display.getCurrent().primaryMonitor))
                        view.caretOffset = offsetPoint
                        if (offsetPoint < 0) {
                            val line = view.getLineIndex(e.y)
                            val offsetStart = view.getOffsetAtLine(line)
                            val offsetAmount = view.getLine(line).length
                            val offsetEnd = offsetStart + offsetAmount
                            //Move caret to start of line
                            view.caretOffset = offsetStart
                            val myCaret = view.caret.location
                            //Estimated position of caret at end of line
                            val estEndPoint = myCaret.x + offsetAmount * 10
                            if (e.x > myCaret.x && e.x > estEndPoint) {
                                view.caretOffset = offsetEnd
                            }
                        }
                    } catch (ex: IllegalArgumentException) {
                        //code above sets to exact location on a line of text
                        //will set caret to start of line, used for blank lines due to SWT Bug
                        val line = view.getLineIndex(e.y)
                        view.caretOffset = view.getOffsetAtLine(line)
                    }
                }

//				if (manager.getCurrent().n == null){
//					new Notify("Current is null");
//					return;
//				}
                val currentStart = state.currentStart
                val currentEnd = state.currentEnd
                if (view.caretOffset !in currentStart..currentEnd) {
                    if (textChanged) sendUpdate()
                    if (view.isTextSelected && view.selectionRanges[1] > 0) setSelectionElements(
                        selection.selectionStart,
                        selection.selectionEnd
                    ) else setCurrent(view.caretOffset)
                    sendStatusBarUpdate(view.getLineAtOffset(view.caretOffset))
                } else if (!textChanged && view.caretOffset >= state.originalStart && view.caretOffset < state.originalEnd) {
                    if (view.isTextSelected && selection.getSelectionLength() > 0) setSelectionElements(
                        selection.selectionStart,
                        selection.selectionEnd
                    ) else setCurrent(view.caretOffset)
                    sendStatusBarUpdate(view.getLineAtOffset(view.caretOffset))
                }
                if (atRunningHeadLine() || atGuideWordLine()) {
                    val line = view.getLineAtOffset(view.caretOffset)
                    view.caretOffset = when {
                        line < view.lineCount -> {
                            view.getOffsetAtLine(line + 1)
                        }

                        line == 0 -> {
                            view.getLineAtOffset(1)
                        }

                        else -> {
                            view.getOffsetAtLine(line - 1)
                        }
                    }
                    return
                }

                //Change context menu if cursor is in a table
                updateContextMenu()
                if (view.selectionRange.y == 0) {
                    selection.setSelectionLength(0)
                }
            }
        }.also { mouseListener = it })
        view.verticalBar.addSelectionListener(WPScrollListener(manager, this, Sender.TEXT).also {
            scrollbarListener = it
        })
        view.horizontalBar.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {}
        })
        view.addPaintObjectListener(ImagePaintListener().also { paintObjListener = it })
        view.addListener(SWT.Dispose, ImageDisposeListener(view))
        view.addPaintListener(WPPaintListener(this, Sender.TEXT).also { paintListener = it })
        view.addModifyListener(viewMod)
        setName(view, NAME)
    }

    override fun removeListeners() {
        if (selectionListener != null) {
            super.removeListeners()
            view.removeModifyListener(viewMod)
            view.removeSelectionListener(selectionListener)
            view.removeExtendedModifyListener(modListener)
            view.removeFocusListener(focusListener)
            view.removeKeyListener(keyHandler)
            view.removeVerifyKeyListener(verifyKeyListener)
            view.removeMouseListener(mouseListener)
            view.removeCaretListener(caretListener)
            view.verticalBar.removeSelectionListener(scrollbarListener)
            view.removePaintObjectListener(paintObjListener)
            view.removePaintListener(paintListener)
            view.removeListener(SWT.MenuDetect, menuKeyListener)
            removeAllPaintedElements(this)
        }
    }

    private fun addAccessibilityListener() {
        // added to speak breadcrumbs
        view.accessible.addAccessibleListener(object : AccessibleAdapter() {
            override fun getDescription(e: AccessibleEvent) {
                e.result = if (BreadcrumbsToolbar.enabled) BreadcrumbsToolbar.crumb_string else ""
            }
        })

        //This will read the painted page numbers when the screen reader requests
        //a line that has a page number
        view.accessible.addAccessibleTextListener(object : AccessibleTextExtendedAdapter() {
            override fun getText(e: AccessibleTextEvent) {
                //Since for some reason this is entirely undocumented in SWT:
                //	e.start - Styled text offset of the beginning of the requested text
                // 	e.end - Styled text offset of the end of the requested text (after the line break)
                //	e.result - Text given to the screen reader to read
                val line = view.getLineAtOffset(e.start)

                // If the line consists of multiple emphasis, for some (completely undocumented!)
                // reason this listener fires multiple times, adding the page number each time.
                // Only add the page number if we're looking at the end of the line.
                if (e.end == view.getOffsetAtLine(line) + view.getLine(line).length
                    || e.end == view.getOffsetAtLine(line) + view.getLine(line).length + LINE_BREAK.length
                ) {
                    if (offsetIsPrintPageNumberLine(e.start)) {
                        val pageNum = getPreviousPageIndicator(e.start)!!.printPageNum
                        if (pageNum != null) {
                            e.result = e.result + " " + pageNum
                        }
                    } else if (offsetIsBraillePageNumberLine(e.start)) {
                        val pageNum = getNextPageIndicator(e.start)!!.braillePageNum
                        if (pageNum != null) {
                            e.result = e.result + " " + pageNum
                        }
                    }
                }
            }
        })
    }

    //public method to check if an update should be made before exiting or saving
    fun update(forceUpdate: Boolean) {
        if (textChanged || forceUpdate) sendUpdate()
    }

    private fun sendUpdate() {
        val currentStart = state.currentStart
        val currentEnd = state.currentEnd

        //If cursor was moved to a TME after the current one, save the TME that follows the current one.
        var nextTMEFromCurrent: TextMapElement? = null
        if (view.caretOffset > state.currentEnd) {
            nextTMEFromCurrent = manager.mapList.getNext(true)
            //Uneditable TME's (Box lines, tables) usually have nodes that are ripped out on reformat
            while (nextTMEFromCurrent is Uneditable) {
                nextTMEFromCurrent = manager.mapList.getNext(manager.mapList.indexOf(nextTMEFromCurrent), true)
            }
        }
        //Calculate how much text was added
        val changedTextLength = state.originalEnd - state.originalStart - (currentEnd - currentStart)
        val updateMessage: Message = if (currentElement is WhiteSpaceElement) WhitespaceMessage(
            currentIndex, view.caretOffset, getString(currentStart, currentEnd - currentStart).replace(
                LINE_BREAK, ""
            ), state.originalEnd - state.originalStart
        ) else UpdateMessage(
            view.caretOffset,
            getString(currentStart, currentEnd - currentStart),
            state.originalEnd - state.originalStart
        )
        try {
            manager.dispatch(updateMessage)
        } catch (e: Exception) {
            manager.handleEditingException(e)
        }
        currentChanges = 0
        textChanged = false
        try {
            if (nextTMEFromCurrent != null) {
                //Find the next TME from the old map list in the newly updated map list
                val oldTMEInNewMapList = manager.mapList.findNode(nextTMEFromCurrent.node)
                if (oldTMEInNewMapList != null) {
                    //Calculate how much it moved from before
                    val newOffset =
                        view.caretOffset + (oldTMEInNewMapList.getStart(manager.mapList) - (nextTMEFromCurrent.getStart(
                            manager.mapList
                        ) - changedTextLength))
                    if (newOffset > 0 && newOffset < view.charCount) {
                        //Move cursor by how much it moved
                        EasySWT.setCaretAfterLineBreaks(view, newOffset)
                    }
                }
            }
        } catch (e: RuntimeException) {
            throw CursorMovementException("Attempted to move cursor after an update", e)
        }
    }

    fun scrollToCursor() {
        EasySWT.scrollViewToCursor(view)
    }

    fun setCurrentElement(pos: Int) {
        val newPos = setCursor(pos)
        setCurrent(newPos)
    }

    fun setCurrent(pos: Int) {
        val tmeIndex = manager.mapList.findClosest(
            pos,
            manager.mapList.current,
            0,
            manager.mapList.size - 1
        )
        var tme = manager.mapList[tmeIndex]
        if (isLastInView(tmeIndex)) {
            manager.bufferForward()
            if (selection.getSelectionLength() > 0) setSelection(-1, -1)
            var nTme: TextMapElement? = manager.mapList.current
            while (nTme is TableTextMapElement || nTme is ReadOnlyTableTextMapElement || nTme is FormattingWhiteSpaceElement) {
                nTme = manager.mapList.getNext(manager.mapList.indexOf(nTme), true)
            }
            while (nTme is PaintedWhiteSpaceElement || nTme is WhiteSpaceElement || nTme is PageIndicatorTextMapElement) {
                nTme = manager.mapList.getNext(manager.mapList.indexOf(nTme), true)
            }
            tme = requireNotNull(nTme) { "Problem finding tme" }
            val insertPos = calculateTextOffset(tme.getStart(manager.mapList), tme)
            manager.simpleManager.dispatchEvent(
                XMLCaretEvent(
                    Sender.TREE, createNodeCaret(tme, insertPos)
                )
            )
            setLocalState(view.caretOffset)
        } else if (isFirstInView(tmeIndex)) {
            //int insertPos = calculateTextOffset(pos, tme);
            manager.decrementView()
            if (selection.getSelectionLength() > 0) setSelection(-1, -1)
            tme = manager.mapList.current
            while (tme is TableTextMapElement || tme is ReadOnlyTableTextMapElement || tme is FormattingWhiteSpaceElement) {
                tme = manager.mapList.getPrevious(manager.mapList.indexOf(tme), true)
            }
            val insertPos = calculateTextOffset(tme.getStart(manager.mapList), tme)
            manager.simpleManager.dispatchEvent(
                XMLCaretEvent(Sender.TREE, createNodeCaret(tme, insertPos))
            )
            setLocalState(view.caretOffset)
        } else {
            var insertPos = pos - tme.getStart(manager.mapList)
            logger.trace("textView setCurrent tme.start " + tme.getStart(manager.mapList) + " pos " + pos + " viewLength " + view.charCount + " insertPos " + insertPos)
            if (!(tme.getStart(manager.mapList) == pos && pos == view.charCount) //					&&
            //					!(tme instanceof MathMLElement)
            ) {
                insertPos = calculateTextOffset(pos, tme)
            }
            manager.simpleManager.dispatchEvent(
                XMLCaretEvent(
                    Sender.TEXT, createNodeCaret(tme, insertPos)
                )
            )
            setLocalState(pos)
        }
    }

    /**
     * Find the offset of the cursor relative to the text node
     */
    private fun calculateTextOffset(offset: Int, tme: TextMapElement): Int {
        val noLineBreakOffset: Int
        if (!(tme.getStart(manager.mapList) == offset && offset == view.charCount)) {
            //bug occurs at end of view selecting text, so -1
            var textPos = if (offset == view.charCount) offset - 1 else offset
            /* Workaround for offset including NewLine causing an
             * invalid XMLTextCaret offset when caret is at the end of a block.
             * On windows it just grabs \r so this bug would be hidden
             */
            if (textPos - 1 >= tme.getStart(manager.mapList)) {
                textPos -= 1
            }
            val text = view.getText(tme.getStart(manager.mapList), textPos)
            val textWithoutLineBreaks = text.replace(LINE_BREAK, "")
            noLineBreakOffset = text.length - textWithoutLineBreaks.length
            return offset - tme.getStart(manager.mapList) - noLineBreakOffset
        }
        return 0
    }

    private fun setSelectionElements(startPos: Int, endPos: Int) {
        val list = manager.mapList
        val tmeIndex1 = list.findClosest(
            startPos, list.current,
            0, list.size - 1
        )
        val tmeIndex2 = list.findClosest(
            endPos, list.current,
            0, list.size - 1
        )
        var noLineBreakOffset = 0
        val tme1 = list[tmeIndex1]
        val tme2 = list[tmeIndex2]

        //good ole SWT bug when tme.start and pos are equal and at end of widget
        if (!(tme2.getStart(list) == endPos && endPos == view.charCount)) {
            val text = view.getTextRange(tme1.getStart(list), startPos - tme1.getStart(list))
            noLineBreakOffset = text.length - text.replace(LINE_BREAK, "").length
        }
        var startOffset = selection.selectionStart - tme1.getStart(list) - noLineBreakOffset
        if (lengthOfGuideDotsInTME(tme1) > 0) {
            startOffset = selection.selectionStart - (tme1.getEnd(list) - tme1.node.value.length)
        }
        val start = createNodeCaret(tme1, startOffset)
        noLineBreakOffset = 0
        if (!(tme2.getStart(list) == endPos && endPos == view.charCount)) {
            val text = view.getTextRange(tme2.getStart(list), endPos - tme2.getStart(list))
            noLineBreakOffset = text.length - text.replace(LINE_BREAK, "").length
        }
        val endOffset = endPos - tme2.getStart(list) - noLineBreakOffset
        if (lengthOfGuideDotsInTME(tme2) > 0) {
            startOffset = selection.selectionStart - (tme2.getEnd(list) - tme2.node.value.length)
        }
        val end = createNodeCaret(tme2, endOffset)
        manager.simpleManager.dispatchEvent(XMLCaretEvent(Sender.TEXT, start, end))
    }

    private fun lengthOfGuideDotsInTME(tme: TextMapElement): Int {
        var guideDotLength = 0
        for (bme in tme.brailleList) {
            if (bme is GuideDotsBrlMapElement) {
                guideDotLength += bme.getText().length
            }
        }
        return guideDotLength
    }

    //checks if index last in maplist and not last section loaded in view
    private fun isLastInView(index: Int): Boolean {
        return (index == manager.mapList.size - 1
                && !manager.viewInitializer.sectionList[manager.viewInitializer.sectionList.size - 1].isVisible)
    }

    private fun isFirstInView(index: Int): Boolean {
        return index == 0 &&
                !manager.viewInitializer.sectionList[0].isVisible
    }

    private fun sendAdjustRangeMessage(type: String, position: Int) {
        val adjustmentMessage = AdjustRangeMessage(type, position)
        manager.dispatch(adjustmentMessage)
        if (type == "start") {
            state.adjustStart(-position)
            state.originalStart -= position
        } else {
            state.adjustEnd(position)
            state.adjustNextStart(position)
        }
    }

    override fun setViewData() {}
    private fun makeTextChange(offset: Int) {
        state.adjustEnd(offset)
        incrementNext(offset)
        currentChanges += offset
        textChanged = true
    }

    private fun incrementNext(offset: Int) {
        if (state.nextStart != -1) state.adjustNextStart(offset)
    }

    private fun shiftLeft(offset: Int) {
        state.adjustStart(offset)
        state.adjustEnd(offset)
        state.adjustNextStart(offset)
    }

    fun setCursor(offset: Int): Int {
        var offset = offset
        if (LINE_BREAK.length == 2 && offset + 1 < view.charCount && view.getTextRange(
                offset,
                1
            ) == LINE_BREAK.substring(1)
        ) {
            offset++
        }
        view.caretOffset = offset
        return offset
    }

    private fun handleTextEdit(e: ExtendedModifyEvent) {
        //initially set changes to length of new text, can be greater than 1 for paste
        var changes = e.length

        //if replacedText length is greater than 0 then selection occured, check range selected
        if (e.replacedText.isNotEmpty()) {
            if (e.start < state.currentStart) {
                setCurrent(e.start)
            }

            //if selection(i.e. replaced text in view) extends beyond bounds, delegate work to selection handler
            if (e.start + e.replacedText.length > state.currentEnd) {
                deleteSelection(e)
                //view.setCaretOffset(e.start + e.length);
            } else {
                if (selection.selectionStart < state.currentStart) {
                    sendAdjustRangeMessage("start", state.currentStart - selection.selectionStart)
                    changes -= e.replacedText.length
                    makeTextChange(changes)
                    sendUpdate()
                    setCurrent(view.caretOffset)
                } else if (e.start + e.replacedText.length == state.currentEnd) {
                    changes -= e.replacedText.length
                    makeTextChange(changes)
                    recordEvent(e, true)
                } else {
                    //current range is changed by the difference between the text enter subtracted by the length replaced
                    //example if the word 'the' is replaced with the letter 't' changes=-2
                    //TODO: possibly refactor, same result as changes -= e.replacedText.length()
                    changes = e.length - selection.getSelectionLength()
                    makeTextChange(changes)
                    recordEvent(e, true)
                }
            }
        } else {
            //due to changes in whitespace, these 2 if statements may no longer be necessary, needs testing
            if (state.oldCursorPosition > state.currentEnd) sendAdjustRangeMessage(
                "end",
                state.oldCursorPosition - state.currentEnd
            )
            if (state.oldCursorPosition < state.currentStart) sendAdjustRangeMessage(
                "start",
                state.currentStart - state.oldCursorPosition
            )

            //basic typing will fall through to here
            //event passed to edit recorder for undo/redo of typing
            makeTextChange(changes)
            recordEvent(e, true)
        }

        //style range must be updated programmatically for emphasis because SWT
        //clear selection object following edit event
        checkStyleRange(range)
        setSelection(-1, -1)
    }

    private fun handleTextDeletion(e: ExtendedModifyEvent) {
        var offset = view.caretOffset - state.oldCursorPosition

        //deletions in a selection go to different method
        //replaced text of length 2 with a selection of 0 is a \r\n, windows line break
        if (e.replacedText.length > 1 && !(e.replacedText.length == 2 && selection.getSelectionLength() <= 0)) {
            deleteSelection(e)
        } else if (state.currentChar == SWT.BS.code) {
            //for inline elements, backspace into next node, old cursor pos is at start and caret is now crossed into previous element boundary
            if (state.oldCursorPosition == state.currentStart && view.caretOffset < state.previousEnd) {
                //if necessary, update current before shifting to previous node
                if (textChanged) {
                    shiftLeft(offset)
                    sendUpdate()
                }

                //set current to previous node, make changes, update
                setCurrent(view.caretOffset)
                makeTextChange(offset)
                sendUpdate()
            } else {
                //basic deletion inside a text node
                makeTextChange(offset)
                recordEvent(e, false)
            }
        } else if (state.currentChar == SWT.DEL.code) {
            //since delete does not change caret pos, calculate length of change
            offset = if (e.replacedText.isNotEmpty()) -e.replacedText.length else -1

            //checks if delete occurs between inline elements, current end will be next start since boundaries meet
            if (state.oldCursorPosition == state.currentEnd && view.caretOffset == state.nextStart) {
                //if current node has change update
                if (textChanged) {
                    sendUpdate()

                    //after update view is rebuilt, so to correctly obtain edited text
                    //shift by amount deleted. capture changes and update
                    shiftLeft(-offset)
                    makeTextChange(offset)
                    sendUpdate()
                } else {
                    //update current node, then make changes
                    manager.incrementCurrent()
                    makeTextChange(offset)

                    //if node is now empty, update
                    if (state.currentStart == state.currentEnd) sendUpdate()
                }
            } else {
                //basic backspace edit
                makeTextChange(offset)
                recordEvent(e, false)
            }
        }
    }

    private fun deleteSelection(e: ExtendedModifyEvent) {
        val curElement = currentElement
        if (curElement is PageIndicatorTextMapElement //				&& view.getSelectionRanges().length == 1
        ) {
            val parent: Node = curElement.nodeParent.parent
            curElement.nodeParent.detach()
            try {
                manager.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, true, parent))
            } catch (ex: Exception) {
                manager.handleEditingException(ex)
            }
            return
        }
        if (selection.selectionStart >= state.currentStart && selection.selectionEnd <= state.currentEnd) {
            makeTextChange(-selection.getSelectionLength())
            recordEvent(e, false)
        } else {
            try {
                manager.dispatch(
                    SelectionMessage(
                        e.replacedText,
                        view.getTextRange(e.start, e.length),
                        selection.selectionStart,
                        selection.selectionEnd
                    )
                )
            } catch (ex: Exception) {
                manager.handleEditingException(ex)
            }
        }
        setSelection(-1, -1)
    }

    fun adjustCurrentElementValues(changes: Int) {
        currentChanges = changes
        state.adjustEnd(changes)
        state.adjustNextStart(changes)
        textChanged = currentChanges != 0
    }

    private fun recordEvent(e: ExtendedModifyEvent, edit: Boolean) {
        editRecorder.recordEvent(e)
    }

    fun getString(start: Int, length: Int): String {
        return view.getTextRange(start, length)
    }

    /**
     * DO NOT USE. These are left here until SpellCheckManager is refactored
     * Use ClipboardModule instead
     */
    fun cut() {
        if (selection.getSelectionLength() > 0) editRecorder.recordLine(
            selection.selectionStart,
            selection.selectionEnd
        ) else editRecorder.recordLine(
            view.getLine(view.getLineAtOffset(view.caretOffset)),
            view.getLineAtOffset(view.caretOffset)
        )
        if (validator.validCut(
                currentElement,
                state,
                selection.selectionStart,
                selection.getSelectionLength()
            )
        ) view.cut()
    }

    /**
     * DO NOT USE. These are left here until SpellCheckManager is refactored
     * Use ClipboardModule instead
     */
    fun copyAndPaste(text: String, start: Int, end: Int) {
        view.caretOffset = start
        setCurrent(view.caretOffset)
        view.setSelection(start, end)
        setSelection(start, end - start)
        val cb = Clipboard(view.display)
        val textTransfer = TextTransfer.getInstance()
        if (text.isNotEmpty()) {
            cb.setContents(arrayOf<Any>(text), arrayOf<Transfer>(textTransfer))
            view.paste()
        } else {
            cut()
        }
        cb.dispose()
        sendUpdate()
    }

    //	public void updateCursorPosition(UpdateCursorsMessage message){
    ////		setListenerLock(true);
    //		setViewData(message);
    //		setCursorPosition(message);
    //		setPositionFromStart();
    ////		setListenerLock(false);
    //	}
    fun updateCursor(offsetFromTMEStart: Int) {
        setListenerLock(true)
        //setViewData
        run {
            setStateObject()
            if (state.currentStart < view.charCount) range = this.styleRange
            currentElement = manager.mapList.current
        }
        run { FormUIUtils.setCaretAtTextNodeOffset(view, currentElement!!, offsetFromTMEStart, manager) }
        setListenerLock(false)
        sendStatusBarUpdate(view.getLineAtOffset(view.caretOffset))
    }

    private val styleRange: StyleRange?
        get() {
            val currentStart = state.currentStart
            return if (currentStart < view.charCount) {
                view.getStyleRangeAtOffset(currentStart)
            } else null
        }

    private fun checkStyleRange(range: StyleRange?) {
        val currentStart = state.currentStart
        val currentEnd = state.currentEnd
        if (range != null) setFontStyleRange(currentStart, currentEnd - currentStart, range)
    }

    /**
     * Resets the selection object, but does not affect the view
     * since manipulation to the swt widget can result in the screen to
     * jump to a new position.
     * This method is currently used by the textviewmodule to
     * ensure the selection is cleared after a modify event.
     */
    fun resetSelectionObject() {
        selection.selectionStart = -1
        selection.setSelectionLength(-1)
    }

    private fun setSelection(start: Int, length: Int) {
        selection.selectionStart = start
        selection.setSelectionLength(length)
    }

    fun highlight(start: Int, end: Int) {
        view.setSelection(start, end)
        setSelection()
    }

    //Calls manager to insert a node or element in the DOM and updates the view
    //Used when inserting new paragraphs or transcriber notes
    fun insertNewNode(m: InsertNodeMessage?, pos: Int?) {
        manager.dispatch(m)
        if (pos != null) {
            view.caretOffset = pos
            setCurrent(pos)
        }
    }

    override fun resetView(parent: Composite) {
        recreateView(parent)
        editRecorder = EditRecorder(manager, this)
        state.oldCursorPosition = -1
        currentChanges = 0
        textChanged = false
    }

    fun resetOffsets() {
//		GetCurrentMessage m = new GetCurrentMessage(Sender.TEXT, view.getCaretOffset());
//		manager.dispatch(m);
//		setViewData(m);
        setViewData()
    }

    val selectedText: IntArray
        get() {
            val temp = IntArray(2)
            if (selection.getSelectionLength() > 0) {
                temp[0] = selection.selectionStart
                temp[1] = selection.getSelectionLength()
            }
            return temp
        }

    fun setCurrentSelection(start: Int, end: Int) {
        view.setSelection(start, end)
        setSelection(start, end - start)
    }

    fun clearSelection() {
        view.setSelection(-1, -1)
        setSelection(-1, -1)
    }

    private fun setSelection() {
        val selectionArray = view.selectionRanges
        if (selectionArray[1] > 0) {
            setSelection(selectionArray[0], selectionArray[1])
            isMultiSelected = true
            state.currentChar = ' '.code
            state.currentStateMask = -1
            if (textChanged) {
                sendUpdate()
            }
            setSelectionElements(selectionArray[0], selectionArray[0] + selectionArray[1])
        } else {
            isMultiSelected = false
            selection.selectionElement = null
            //!!!!!
//			selection.setSelectionLength(0);
            setCurrent(view.caretOffset)
        }
    }

    fun undoEdit(start: Int, length: Int, text: String) {
        val changes = text.length - length
        //	StyleRange [] ranges = view.getStyleRanges(start, length);
        replaceTextRange(start, length, text)
        makeTextChange(changes)

        //	for(int i = 0; i < ranges.length; i++)
        //		view.setStyleRange(ranges[i]);
        if (currentChanges == 0) textChanged = false
        reapplyStyle()
    }

    private fun reapplyStyle() {
        val curElement = currentElement
        if (curElement != null && curElement !is WhiteSpaceElement) {
            val style = manager.getStyle(curElement.node)!!
            val startLine = view.getLineAtOffset(curElement.getStart(manager.mapList))
            val endLine: Int =
                if (curElement.getEnd(manager.mapList) + currentChanges > view.charCount) view.getLineAtOffset(
                    view.charCount
                ) else view.getLineAtOffset(
                    curElement.getEnd(manager.mapList) + currentChanges
                )

            //resets indent in view
            val firstLineIndent = style.firstLineIndent
            val indent = style.indent
            if (isFirstInBlock(curElement) && style.align != null && style.align == Align.LEFT) {
                var first = true
                for (i in startLine..endLine) {
                    if (first) {
                        if (firstLineIndent != null) view.setLineIndent(
                            i,
                            1,
                            firstLineIndent * charWidth
                        ) else if (indent != null) view.setLineIndent(i, 1, indent * charWidth)
                        first = false
                    } else {
                        if (indent != null) view.setLineIndent(i, 1, indent * charWidth)
                    }
                }
            } else if (style.align != null && style.align == Align.LEFT) {
                for (i in startLine..endLine) {
                    if (i == startLine && firstLineIndent != null) view.setLineIndent(
                        i,
                        1,
                        firstLineIndent * charWidth
                    ) else if (indent != null) view.setLineIndent(i, 1, indent * charWidth)
                }
            }
            if (manager.getAction(curElement.nodeParent) !is GenericBlockAction) {
                this.setFontStyleRange(
                    curElement.getStart(manager.mapList),
                    curElement.getEnd(manager.mapList) + currentChanges - curElement.getStart(manager.mapList),
                    manager.getAction(curElement.nodeParent),
                    curElement.nodeParent
                )
            }
        }
    }

    fun updateContextMenu() {}
    private fun isArrowKey(keyCode: Int): Boolean {
        return keyCode == SWT.ARROW_DOWN || keyCode == SWT.ARROW_LEFT || keyCode == SWT.ARROW_RIGHT || keyCode == SWT.ARROW_UP
    }

    private fun isNavKey(keyCode: Int): Boolean {
        return keyCode == SWT.HOME || keyCode == SWT.END
    }

    private fun atRunningHeadLine(): Boolean {
        return atPrintPageLine() && getPreviousPageIndicator(view.caretOffset) != null &&
                getPreviousPageIndicator(view.caretOffset)!!.hasRunningHead()
    }

    private fun atGuideWordLine(): Boolean {
        return atBraillePageLine() && getNextPageIndicator(view.caretOffset) != null &&
                getNextPageIndicator(view.caretOffset)!!.hasGuideWord()
    }

    private fun atPrintPageLine(): Boolean {
        return offsetIsPrintPageNumberLine(view.caretOffset)
    }

    private fun offsetIsPrintPageNumberLine(offset: Int): Boolean {
        return if (getPreviousPageIndicator(offset) != null) {
            //Usually the page indicators are considered to be on the line before the print page number,
            //however the very first page indicator is on the same line as the print page number
            (getPreviousPageIndicator(offset)!!.line == (view.getLineAtOffset(offset) - 1).coerceAtLeast(0)
                    && view.getLineAtOffset(offset) != 1 //Don't also count the second line as the print page number
                    )
        } else false
    }

    private fun atBraillePageLine(): Boolean {
        return offsetIsBraillePageNumberLine(view.caretOffset)
    }

    private fun offsetIsBraillePageNumberLine(offset: Int): Boolean {
        return if (getNextPageIndicator(offset) != null) {
            getNextPageIndicator(offset)!!.line == view.getLineAtOffset(offset)
        } else false
    }

    private fun getPreviousPageIndicator(caretOffset: Int): PageIndicator? {
        val line = view.getLineAtOffset(caretOffset)
        var prevIndicator =
            if (manager.text.paintedElements.pageIndicators.isNotEmpty()) manager.text.paintedElements.pageIndicators[0] else null
        for (pi in manager.text.paintedElements.pageIndicators) {
            if (pi.line >= line) {
                return prevIndicator
            }
            prevIndicator = pi
        }
        return prevIndicator
    }

    private fun getNextPageIndicator(caretOffset: Int): PageIndicator? {
        val line = view.getLineAtOffset(caretOffset)
        for (pi in manager.text.paintedElements.pageIndicators) {
            if (pi.line >= line) return pi
        }
        return null
    }

    private fun setStateObject() {
        state.currentStart = manager.mapList.current.getStart(manager.mapList)
        state.currentEnd = manager.mapList.current.getEnd(manager.mapList)
        state.previousEnd = manager.mapList.prevEnd
        state.nextStart = manager.mapList.nextStart
        state.setOriginalPositions(state.currentStart, state.currentEnd)
    }

    private fun setLocalState(pos: Int) {
        val list = manager.mapList
        currentIndex = list.findClosest(pos, manager.mapList.current, 0, list.size - 1)
        val currentElement = list[currentIndex]
        this.currentElement = currentElement
        list.setCurrent(currentIndex)

        //find prevEnd
        val prevEnd: Int =
            if (currentIndex > 0 && list.getPrevious(currentIndex, false) != null) list.getPrevious(currentIndex, false)
                .getEnd(list) else -1

        //find nextStart
        val nextStart: Int = list.getNext(currentIndex, false)?.getStart(list) ?: -1
        state.currentStart = currentElement.getStart(list)
        state.currentEnd = currentElement.getEnd(list)
        state.previousEnd = prevEnd
        state.nextStart = nextStart
        state.setOriginalPositions(state.currentStart, state.currentEnd)
        range = styleRange
    }

    val currentStart: Int
        get() = state.currentStart
    val currentEnd: Int
        get() = state.currentEnd
    var selectionElement: Element?
        get() = selection.selectionElement
        set(e) {
            selection.selectionElement = e
        }
    val previousSelection: Element?
        get() = selection.previousElement()

    fun addFocusListener() {
        removeFocusListener()
        if (!focusListenerLock) {
            focusListener = object : FocusAdapter() {
                override fun focusGained(e: FocusEvent) {
                    sendStatusBarUpdate(view.getLineAtOffset(view.caretOffset))
                }

                override fun focusLost(e: FocusEvent) {
                    if (textChanged) {
                        sendUpdate()
                        setCurrent(view.caretOffset)
                    }
                }
            }
            view.addFocusListener(focusListener)
        }
    }

    fun removeFocusListener() {
        if (focusListener != null) {
            view.removeFocusListener(focusListener)
        }
    }

    fun setFocusListenerLock(focusListenerLock: Boolean) {
        if (focusListenerLock) {
            removeFocusListener()
        }
        this.focusListenerLock = focusListenerLock
        if (!focusListenerLock) {
            addFocusListener()
        }
    }

    var sixKeyMode: Boolean
        get() = keyHandler != null && keyHandler!!.sixKeyMode
        set(sixKeyMode) {
            if (keyHandler != null) {
                keyHandler!!.sixKeyMode = sixKeyMode
            }
        }

    companion object {
        private val logger = LoggerFactory.getLogger(TextView::class.java)
        private const val NAME = "Text View" //Name read by accessibility clients
    }
}

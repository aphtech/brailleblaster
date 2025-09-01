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

import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.*
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.views.style.BreadcrumbsToolbar
import org.brailleblaster.perspectives.braille.views.wp.listeners.WPPaintListener
import org.brailleblaster.perspectives.braille.views.wp.listeners.WPScrollListener
import org.brailleblaster.perspectives.mvc.events.XMLCaretEvent
import org.brailleblaster.utils.swt.AccessibilityUtils.setName
import org.eclipse.swt.SWT
import org.eclipse.swt.accessibility.AccessibleAdapter
import org.eclipse.swt.accessibility.AccessibleEvent
import org.eclipse.swt.custom.CaretEvent
import org.eclipse.swt.custom.CaretListener
import org.eclipse.swt.custom.VerifyKeyListener
import org.eclipse.swt.events.*
import org.eclipse.swt.widgets.Composite

class BrailleView(manager: Manager, sash: Composite) : WPView(manager, sash) {
  private val stateObj: ViewStateObject = ViewStateObject()
  private val pageRanges: ArrayList<PageRange> = ArrayList()
  private var verifyListener: VerifyKeyListener? = null

  //	private FocusListener focusListener;
  private var mouseListener: MouseAdapter? = null
  private var caretListener: CaretListener? = null
  private var selectionListener: SelectionAdapter? = null
  private var paintListener: PaintListener? = null

  init {
    addAccessibilityListener()
  }

  private fun addAccessibilityListener() {
    view.accessible.addAccessibleListener(object : AccessibleAdapter() {
      override fun getDescription(e: AccessibleEvent) {
        e.result = if (BreadcrumbsToolbar.enabled) BreadcrumbsToolbar.crumb_string else ""
      }
    })
  }

  override fun initializeListeners() {
    super.initializeListeners()
    view.addVerifyKeyListener(VerifyKeyListener { e: VerifyEvent ->
      stateObj.currentChar = e.keyCode
      stateObj.currentStateMask = e.stateMask

      //Handles single case where page is on last line and text is selected to last line and arrow down is pressed which does not move cursor
      if (manager.mapList.inBraillePageRange(view.caretOffset) && e.keyCode == SWT.ARROW_DOWN && view.getLineAtOffset(
          view.caretOffset
        ) == view.lineCount - 1
      ) view.caretOffset = stateObj.nextStart
      stateObj.oldCursorPosition = view.caretOffset
    }.also { verifyListener = it })
    view.addMouseListener(object : MouseAdapter() {
      override fun mouseDown(e: MouseEvent) {
        if (!lock) {
          if (e.button == 2) return
          if (view.caretOffset > stateObj.currentEnd || view.caretOffset < stateObj.currentStart) {
            setCurrent()
          }
          sendStatusBarUpdate(view.getLineAtOffset(view.caretOffset))
        }
      }
    }.also { mouseListener = it })
    view.addCaretListener(CaretListener { e: CaretEvent ->
      if (!lock) {
        if (manager.stylePane.updateStylePane && !manager.textView.isDisposed) manager.stylePane.updateCursor(
          manager.brailleView.getLineAtOffset(manager.brailleView.caretOffset)
        )
        if (stateObj.currentChar == SWT.ARROW_DOWN || stateObj.currentChar == SWT.ARROW_LEFT || stateObj.currentChar == SWT.ARROW_RIGHT || stateObj.currentChar == SWT.ARROW_UP || stateObj.currentChar == SWT.PAGE_DOWN || stateObj.currentChar == SWT.PAGE_UP) {
          if (e.caretOffset >= stateObj.currentEnd || e.caretOffset < stateObj.currentStart) {
            setCurrent()
            stateObj.currentChar = ' '.code
            stateObj.currentStateMask = -1
          }
          sendStatusBarUpdate(view.getLineAtOffset(view.caretOffset))
        }
      }
    }.also { caretListener = it })
    view.verticalBar.addSelectionListener(WPScrollListener(manager, this, Sender.BRAILLE).also {
      selectionListener = it
    })
    view.addPaintListener(WPPaintListener(this, Sender.BRAILLE).also { paintListener = it })
    view.addModifyListener(viewMod)
    setName(view, NAME)
  }

  override fun removeListeners() {
    super.removeListeners()
    view.removeModifyListener(viewMod)
    view.removeVerifyKeyListener(verifyListener)
    view.removeMouseListener(mouseListener)
    view.removeCaretListener(caretListener)
    view.verticalBar.removeSelectionListener(selectionListener)
    view.removePaintListener(paintListener)
    removeAllPaintedElements(this)
  }

  private fun setCurrent() {
    val pos = view.caretOffset
    var forward = false
    var specialTme = false
    var tmeIndex = manager.mapList.findClosestBraille(
      pos,
      manager.mapList.current
    )
    if (stateObj.currentChar == SWT.ARROW_DOWN || stateObj.currentChar == SWT.ARROW_RIGHT) forward = true
    var lastNNTme = if (tmeIndex == 0) manager.mapList.current else manager.mapList[tmeIndex]
    var tme: TextMapElement? = lastNNTme
    while (tme is PaintedWhiteSpaceElement || tme is WhiteSpaceElement || tme is PageIndicatorTextMapElement || tme is ReadOnlyTableTextMapElement) {
      checkTme(tme, forward)
      tme = if (forward) manager.mapList.getNext(
        manager.mapList.indexOf(tme),
        true
      ) else manager.mapList.getPrevious(manager.mapList.indexOf(tme), true)
      checkTme(tme, forward)
      if (tme != null) lastNNTme = tme
      specialTme = true
    }
    if (specialTme) {
      if (tme == null) tme = lastNNTme
      manager.simpleManager.dispatchEvent(
        XMLCaretEvent(
          Sender.TEXT, createNodeCaret(tme, tme.getStart(manager.mapList))
        )
      )
      return
    }
    tmeIndex = manager.mapList.getNodeIndex(tme)
    val firstRow = view.getLineAtOffset(pos) == 0
    if ((isFirstInView(tmeIndex) || (tme != null && !tme.isFullyVisible) || firstRow) && !forward) {
      manager.decrementView()
      //FirstUsable gets us in the first TME, just not at the right cursor position.
      //Use pos instead, though this is not without its faults - text/braille doesn't quite line up.
      tme = manager.mapList.firstUsable
      manager.simpleManager.dispatchEvent(XMLCaretEvent(Sender.TEXT, createNodeCaret(tme, pos)))
    } else if ((isLastInView(tmeIndex) || (tme != null && !tme.isFullyVisible)) && forward) {
      manager.incrementView()
      tme = manager.mapList.lastUsable
      manager.simpleManager.dispatchEvent(
        XMLCaretEvent(
          Sender.TEXT, createNodeCaret(tme, tme.getStart(manager.mapList))
        )
      )
    } else {
      val start =
        if (tme!!.brailleList.isNotEmpty()) tme.brailleList.first().getStart(manager.mapList) else 0 //Tables have no braille list
      manager.simpleManager.dispatchEvent(
        XMLCaretEvent(
          Sender.BRAILLE, createNodeCaret(tme, pos - start)
        )
      )
    }
  }

  //checking if in while is last or first
  private fun checkTme(tme: TextMapElement?, foward: Boolean) {
    val tmeIndex = manager.mapList.getNodeIndex(tme)
    if (tmeIndex == -1) {
      return
    }
    if (foward) {
      if (isLastInView(tmeIndex) || !tme!!.isFullyVisible) {
        manager.incrementView()
      }
    } else {
      if (isFirstInView(tmeIndex) || !tme!!.isFullyVisible || tme.getStart(manager.mapList) == 0) {
        manager.decrementView()
      }
    }
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

  override fun setViewData() {
    stateObj.currentStart = manager.mapList.currentBrailleOffset
    stateObj.currentEnd = manager.mapList.currentBrailleEnd
    stateObj.nextStart = manager.mapList.nextBraille
  }

  fun setPageRange(start: Int, end: Int) {
    pageRanges.add(PageRange(start, end))
  }

  fun clearPageRange() {
    pageRanges.clear()
  }

  fun updateCursor(tme: TextMapElement, offsetFromTMEStart: Int) {
    setListenerLock(true)

    //setViewData
    run {
      stateObj.currentStart = manager.mapList.currentBrailleOffset
      stateObj.currentEnd = manager.mapList.currentBrailleEnd
      stateObj.nextStart = manager.mapList.nextBraille
    }
    run { updateCursorFromTME(tme, offsetFromTMEStart) }
    setListenerLock(false)
  }

  fun setPositionFromStart() {
    val lineBreak = System.lineSeparator()
    positionFromStart = view.caretOffset - stateObj.currentStart
    if (positionFromStart > 0 && stateObj.currentStart + positionFromStart <= stateObj.currentEnd) {
      val text = view.getTextRange(stateObj.currentStart, positionFromStart)
      val count = text.length - text.replace(lineBreak.toRegex(), "").length
      positionFromStart -= count
      positionFromStart -= checkPageRange(stateObj.currentStart, stateObj.currentStart + positionFromStart)
      cursorOffset = count
    } else if (positionFromStart > 0 && stateObj.currentStart + positionFromStart > stateObj.currentEnd) {
      cursorOffset = stateObj.currentStart + positionFromStart - stateObj.currentEnd
      positionFromStart = 99999
    } else {
      cursorOffset = 0
    }
  }

  fun updateCursorFromTME(tme: TextMapElement, offsetFromTMEStart: Int) {
    val lastPositon = manager.text.positionFromStart
    val e = getBrlNode(tme.node)
    var pos: Int
    if (e != null) {
      val arr = getIndexArray(e)
      if (arr == null) {
        pos = if (lastPositon == 0) stateObj.currentStart else stateObj.currentEnd
      } else {
        if (lastPositon < 0 && stateObj.currentStart > 0)
          pos = stateObj.currentStart + lastPositon
        else if (lastPositon == 99999)
          pos = stateObj.currentEnd + offsetFromTMEStart
        else {
          pos = stateObj.currentStart + findCurrentPosition(arr, offsetFromTMEStart)
          pos += plusLineBreaks(stateObj.currentStart, pos)
          pos += checkPageRange(stateObj.currentStart, pos)
        }
      }
      if (pos <= view.charCount) {
        /*
   * RT4647
   * check line break on windows gets the line from offset from the view.
   * this can be called with a position > the charcount due to a blank page
   * with a pagenum at the bottom at the end of the document and throw an exception
   */
        if (checkLineBreakOnWindows(pos)) pos++
      }
      view.caretOffset = pos
    }
  }

  /** Checks for swt bug on widows when exception is thrown by setting caret between \r\n
   * @param pos to move caret
   * @return true if will cause exception, false if okay
   */
  private fun checkLineBreakOnWindows(pos: Int): Boolean {
    if (SWT.getPlatform() == "win32") {
      val line = view.getLineAtOffset(pos)
      if (pos < view.charCount && line < view.lineCount - 1) {
        return pos + 1 == view.getOffsetAtLine(line + 1)
      }
    }
    return false
  }

  private fun plusLineBreaks(start: Int, pos: Int): Int {
    val startLine = view.getLineAtOffset(start)
    val endLine = view.getLineAtOffset(pos)
    val diff = endLine - startLine
    if (diff > 0) return diff * LINE_BREAK.length else if (diff < 0) println("Line cannot be below zero")
    return 0
  }

  private fun checkPageRange(elementStart: Int, position: Int): Int {
    var offset = 0
    for (pageRange in pageRanges) {
      if (pageRange.start >= elementStart && position + offset > pageRange.start) offset += pageRange.end - pageRange.start
    }
    return offset
  }

  private fun findCurrentPosition(indexes: IntArray, textPos: Int): Int {
    for (i in indexes.indices) {
      if (textPos == indexes[i]) return i else if (textPos < indexes[i]) return i - 1
    }
    return indexes.size
  }

  override fun resetView(parent: Composite) {
    recreateView(parent)
    stateObj.oldCursorPosition = -1
    clearPageRange()
  }

  private class PageRange(var start: Int, var end: Int)
  companion object {
    private const val NAME = "Braille View" //Name read by accessibility clients
  }
}

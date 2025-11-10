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
import nu.xom.Text
import org.brailleblaster.BBIni
import org.brailleblaster.abstractClasses.AbstractView
import org.brailleblaster.abstractClasses.BBEditorView
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.utd.BBXEmphasisAction
import org.brailleblaster.exceptions.OutdatedMapListException
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getBanaStyles
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.*
import org.brailleblaster.perspectives.braille.mapping.maps.PaintedElementsList
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.messages.UpdateScrollbarMessage
import org.brailleblaster.perspectives.braille.messages.UpdateStatusbarMessage
import org.brailleblaster.perspectives.mvc.XMLNodeCaret
import org.brailleblaster.perspectives.mvc.XMLNodeCaret.CursorPosition
import org.brailleblaster.perspectives.mvc.XMLTextCaret
import org.brailleblaster.utd.actions.IAction
import org.brailleblaster.utd.properties.Align
import org.brailleblaster.utd.properties.EmphasisType
import org.brailleblaster.util.ColorManager
import org.brailleblaster.util.LINE_BREAK
import org.brailleblaster.utils.swt.DebugStyledText
import org.brailleblaster.util.Utils
import org.brailleblaster.utils.xml.BB_NS
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyleRange
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.events.ModifyListener
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.graphics.GC
import org.eclipse.swt.graphics.GlyphMetrics
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.widgets.Composite

private data class ViewAndValidator(var view: StyledText, var validator: Validator)
abstract class WPView(manager: Manager, parent: Composite) : AbstractView(manager, parent), BBEditorView {
    private val viewAndValidator = newView()
    override var view: StyledText by viewAndValidator::view
    protected var validator: Validator by viewAndValidator::validator
    override var charWidth = 0
        protected set
    protected var viewMod: ModifyListener = ModifyListener { hasChanged = true }

    @JvmField
    var paintedElements: PaintedElementsList
    var ranges: MutableList<StyleRange>

    init {

        // Better use a ModifyListener to set the change flag.
        paintedElements = PaintedElementsList()
        ranges = ArrayList()
    }

    abstract override fun setViewData()
    protected val fontWidth: Int
        /**
         * finds Char width used to determine indent value
         *
         * @return width of braille cell
         */
        get() {
            val gc = GC(view)
            val fm = gc.fontMetrics
            gc.dispose()
            return fm.averageCharacterWidth.toInt()
        }

    /**
     * Public method for calculating char width, used when font changes, or font size changes
     */
    fun setCharWidth() {
        charWidth = fontWidth
    }

    /**
     * Positions the scrollbar based off the top-most visible line
     *
     * @param topIndex : top-most visible line in view
     */
    fun positionScrollbar(topIndex: Int) {
        parent.setRedraw(false)
        view.topIndex = topIndex
        parent.setRedraw(true)
        parent.display.update()
    }

    /**
     * Disables event listeners, inserts text into view at the point specified
     *
     * @param start : start offset in view
     * @param text  : text to insert
     */
    fun insertText(start: Int, text: String?) {
        val originalPosition = view.caretOffset
        view.caretOffset = start
        view.insert(text)
        view.caretOffset = originalPosition
    }

    fun replaceTextRange(start: Int, length: Int, text: String?) {
        val originalPosition = view.caretOffset
        view.caretOffset = start
        view.replaceTextRange(start, length, text)
        view.caretOffset = originalPosition
    }

    protected fun setImageStyleRange(image: Image, offset: Int, length: Int) {
        val style = StyleRange()
        style.start = offset
        style.length = length
        style.data = image
        val rect = image.bounds
        style.metrics = GlyphMetrics(rect.height, 0, rect.width)
        view.setStyleRange(style)
    }

    /**
     * sets range and applies given form of emphasis
     *
     * @param start  : position at which to insert style range
     * @param length : length style range covers
     * @param action :action to set
     */
    fun setFontStyleRange(start: Int, length: Int, action: IAction?, inlineElement: Element?) {
        val ranges = view.getStyleRanges(start, length)
        val range = if (ranges.isNotEmpty()) ranges[0] else StyleRange()
        range.start = start
        range.length = length
        if (setStyleRangeForAction(range, view, action, inlineElement)) {
            this.ranges.add(range)
        }
    }

    fun addMathHighlights(start: Int, length: Int, inlineNode: Element?) {
        val ranges = view.getStyleRanges(start, length)
        val range = if (ranges.isNotEmpty()) ranges[0] else StyleRange()
        range.start = start
        range.length = length
        range.background = ColorManager.getColor(ColorManager.Colors.LIGHT_PURPLE, view)
        this.ranges.add(range)
    }

    fun addLinkStyleRange(start: Int, length: Int, inlineNode: Element?) {
        val ranges = view.getStyleRanges(start, length)
        val range = if (ranges.isNotEmpty()) ranges[0] else StyleRange()
        //Not sure that the data field is actually useful here, but it might be?
        range.data = inlineNode?.getAttributeValue("href", BB_NS)
        //println("Adding link style range for href: ${range.data}")
        range.start = start
        range.length = length
        range.underline = true //Have to set this to true to see underline
        range.underlineStyle = SWT.UNDERLINE_SINGLE
        range.foreground = ColorManager.getColor(ColorManager.Colors.DARK_BLUE, view)
        this.ranges.add(range)
    }

    /**
     * sets range and applies given form of emphasis
     *
     * @param start      : position at which to insert style range
     * @param length     : length style range covers
     * @param styleRange : SWT StyleRange object
     */
    protected fun setFontStyleRange(start: Int, length: Int, styleRange: StyleRange) {
        styleRange.start = start
        styleRange.length = length
        view.setStyleRange(styleRange)
    }

    /**
     * reverts range to plain text
     *
     * @param range : Style Range to reset
     */
    protected fun resetStyleRange(range: StyleRange) {
        range.fontStyle = SWT.NORMAL
        range.underline = false
        view.setStyleRange(range)
    }

    /**
     * Gets index attribute from brl element and converts to integer array
     *
     * @param e : Brl element to retrieve indexes
     * @return : Integer Array containing brl indexes
     */
    protected fun getIndexArray(e: Element): IntArray? {
        val arr = e.getAttributeValue("index") ?: return null
        return arr.trim().split(" ").filter { it.isNotEmpty() }.map { it.toInt() }.toIntArray()
    }

    /**
     * Used to set LibLouisUTDML LeftMargin style attribute, in SWT API uses a line wrap function to achieve equivalent
     *
     * @param pos           : starting offset
     * @param text          : text inserted in view
     * @param indent        : amount of left margin specified in number of braille cells
     * @param skipFirstLine : true if first line should not have style applied
     */
    protected fun handleLineWrap(pos: Int, text: String, indent: Int, skipFirstLine: Boolean) {
        var newPos: Int
        var i = 0
        while (i < text.length && (text[i] == '\n' || text[i] == '\r')) i++
        if (!skipFirstLine) view.setLineIndent(view.getLineAtOffset(pos + i), 1, indent * charWidth)
        while (i < text.length) {
            if ((text[i] == '\n' || text[i] == '\r') && i != text.length - 1) {
                i++
                newPos = pos + i
                view.setLineIndent(view.getLineAtOffset(newPos), 1, indent * charWidth)
            }
            i++
        }
    }

    /**
     * Counts total lines a string covers within a view
     *
     * @param startOffset : starting position of text
     * @param text        : text placed in view
     * @return lines count covered by text
     */
    protected fun getLineNumber(startOffset: Int, text: String): Int {
        val startLine = view.getLineAtOffset(startOffset)
        val endLine = view.getLineAtOffset(startOffset + text.length)
        return endLine - startLine + 1
    }

    /**
     * Sets the first line indent specified by liblouisutdml style
     *
     * @param start    : start position
     */
    protected fun setFirstLineIndent(start: Int, indent: Int, margin: Int) {
        val indentSpaces = indent + margin
        val startLine = view.getLineAtOffset(start)
        view.setLineIndent(startLine, 1, indentSpaces * charWidth)
    }

    /**
     * Sets alignment using a Styles object
     *
     * @param start    : start offset
     * @param end      : end offset
     */
    protected fun setAlignment(start: Int, end: Int, alignment: Align?) {
        val startLine = view.getLineAtOffset(start)
        when (alignment) {
            Align.RIGHT -> view.setLineAlignment(
                startLine,
                getLineNumber(start, view.getTextRange(start, end - start)),
                SWT.RIGHT
            )

            Align.CENTERED -> view.setLineAlignment(
                startLine,
                getLineNumber(start, view.getTextRange(start, end - start)),
                SWT.CENTER
            )

            else -> view.setLineAlignment(
                startLine,
                getLineNumber(start, view.getTextRange(start, end - start)),
                SWT.LEFT
            )
        }
    }

    /**
     * Pauses event listeners and clears a range of text
     *
     * @param start  : starting offset
     * @param length : length of text
     */
    fun clearTextRange(start: Int, length: Int) {
        view.replaceTextRange(start, length, "")
    }

    /**
     * Creates message and text to display status in the status bar.
     * The message typically covers current style, line number and word count
     *
     * @param line
     */
    protected fun sendStatusBarUpdate(line: Int) {
        var statusBarText = ""
        val printPage = manager.getCurrentPrintPage(view.getLineAtOffset(view.caretOffset))
        val braillePage = manager.getCurrentBraillePage(view.getLineAtOffset(view.caretOffset))
        val estimatedPage = (view.getLineAtOffset(view.caretOffset) / manager.document.linesPerPage) + 1

        val fileMgr = BBIni.propertyFileManager
        val showPrintPage = fileMgr.getPropertyAsBoolean("StatusBar.printPage", true)
        val showBraillePage = fileMgr.getPropertyAsBoolean("StatusBar.braillePage", true)
        val showLineNumber = fileMgr.getPropertyAsBoolean("StatusBar.line", true)
        val showCellNumber = fileMgr.getPropertyAsBoolean("StatusBar.cell", true)
        val showIndent = fileMgr.getPropertyAsBoolean("StatusBar.indents", true)
        val showAlignment = fileMgr.getPropertyAsBoolean("StatusBar.alignment", true)
        val showStyle = fileMgr.getPropertyAsBoolean("StatusBar.styles", true)

        //TODO: Localize strings
        if (showPrintPage) {
            if (!printPage.isNullOrBlank()) {
                statusBarText += "Print Page: $printPage | "
            } else if (view.caretOffset >= 0) {
                //If it's null or blank, estimate the page count based on line length instead.
                statusBarText += "Print Page: $estimatedPage | "
            }
        }

        if (showBraillePage) {
            if (!braillePage.isNullOrBlank()) {
                statusBarText += "Braille Page: $braillePage | "
            } else if (view.caretOffset >= 0) {
                // Estimate the braille page number for the last page in the doc, as it otherwise just shows as null
                statusBarText += "Braille Page: $estimatedPage | "
            }
        }

        if (showLineNumber) {
            //Every 25th line start counting line again
            statusBarText += "Line: " + (line % manager.document.linesPerPage + 1) + " | "
        }

        if (showCellNumber) {
            //Added this line for cursor position
            if (this is BrailleView){
                statusBarText += "Braille "
            }
            else if (this is TextView){
                statusBarText += "Text "
            }

            statusBarText +=
                "Cell Number: " +
                (view.caretOffset - view.getOffsetAtLine(line) + (view.getLineIndent(line) / charWidth + 1)) +
                " | "
        }

        if (view.getLineIndent(line) > 0 && showIndent) {
            statusBarText += " Indent: Cell " + (view.getLineIndent(line) / charWidth + 1) + " | "
        }

        if (showAlignment) {
            if (view.getLineAlignment(line) == SWT.LEFT) {
                statusBarText += " Alignment: Left" + " | "
            } else if (view.getLineAlignment(line) == SWT.CENTER) {
                statusBarText += " Alignment: Center" + " | "
            } else if (view.getLineAlignment(line) == SWT.RIGHT) {
                statusBarText += " Alignment: Right" + " | "
            }
        }

        if (manager.mapList.current.nodeParent != null && showStyle) {
            var styleName: String
            if (manager.mapList.current is PageIndicatorTextMapElement) {
                styleName = "Print Page"
            } else {
                if (manager.mapList.current.node.document == null) {
                    throw OutdatedMapListException("Node " + manager.mapList.current.node.toXML())
                }
                val e = manager.document.getParent(manager.mapList.current.node)
                val style = manager.document.engine.styleMap.findValueOrDefault(e)
                styleName = getBanaStyles()[style.name]
                if (styleName.contains("local_")) {
                    val tokens = styleName.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    styleName = tokens[1]
                }
            }
            statusBarText += "Style: $styleName | "
        }

      if (manager.mapList.current.nodeParent != null) {
        if (BBX.INLINE.LINK.isA(manager.mapList.current.nodeParent)) {
          val href = manager.mapList.current.nodeParent.getAttributeValue("href", BB_NS)
          if (!href.isNullOrEmpty()) {
            statusBarText += "Link: $href | "
          }
        }
      }

        val statusMessage = UpdateStatusbarMessage(statusBarText)
        manager.dispatch(statusMessage)
        currentLine = view.getLineAtOffset(view.caretOffset)
    }

    /**
     * Public method for moving the scrollbar by resetting the top index.
     * Event listeners are paused and index is changed.
     *
     * @param line
     */
    fun setTopIndex(line: Int) {
        view.topIndex = line
        topIndex = line
    }

    /**
     * Checks if status bar should be updated
     *
     * @param sender : Enumeration signify which view, text, braille or tree, is sending the message
     */
    fun checkStatusBar(sender: Sender) {
        if (!lock) {
            if (topPixel != view.topPixel) {
                topPixel = view.topPixel
                val scrollMessage = UpdateScrollbarMessage(sender,  /*this value isn't used*/0)
                manager.dispatch(scrollMessage)
            }
        }
    }

    /**
     * Redraws the views when a refresh occurs
     */
    protected fun recreateView(parent: Composite?) {
        this.parent = parent!!
        view.dispose()
        newView().also {
            view = it.view
            validator = it.validator
        }
        view.addModifyListener(viewMod)
        view.parent.layout()
    }

    private fun newView(): ViewAndValidator {
        return DebugStyledText(parent, SWT.BORDER or SWT.H_SCROLL or SWT.V_SCROLL).let {
            ViewAndValidator(it, Validator(manager, it))
        }
    }

    protected fun isFirstInBlock(t: TextMapElement): Boolean {
        if (t.nodeParent.indexOf(t.node) == 0) {
            val block = manager.document.engine.findTranslationBlock(t.node) as Element
            return if (block == t.nodeParent) true else {
                block.indexOf(t.nodeParent) == 0
            }
        }
        return false
    }

    fun removeAllPaintedElements(view: WPView) {
        for (paintedElement in paintedElements) paintedElement.removeListener(view)
        paintedElements = PaintedElementsList()
    }

    fun inLineBreak(pos: Int): Boolean {
        if (Utils.isWindows) {
            //if not last char
            if (pos < view.charCount - 1) {
                val line = view.getLineAtOffset(pos)
                if (pos == view.getOffsetAtLine(line) + 1) {
                    val text = view.getText(view.getOffsetAtLine(line), pos)
                    return text == LINE_BREAK
                }
            }
        }
        return false
    }

    fun clearText() {
        view.text = ""
    }

    fun createNodeCaret(t: TextMapElement, pos: Int): XMLNodeCaret {
        return determineCaretType(t, pos, -1)
    }

    private fun determineCaretType(t: TextMapElement, pos: Int, prev: Int): XMLNodeCaret {
        var cursorPos = CursorPosition.ALL
        if (prev != -1) {
            cursorPos = if (prev > manager.mapList.indexOf(t)) CursorPosition.AFTER else CursorPosition.BEFORE
        }
        if (t is BoxLineTextMapElement || t is PageIndicatorTextMapElement) {
            return XMLNodeCaret(t.nodeParent, cursorPos)
        } else if (t is GuideDotsTextMapElement || t is UncontractedWordTextMapElement) {
            return XMLNodeCaret(t.nodeParent.parent, cursorPos)
        } else if (t is ImagePlaceholderTextMapElement) {
            return XMLNodeCaret(t.node, cursorPos)
        } else if (t is WhiteSpaceElement) {
            val newTME = determineWhitespace(t.getEnd(manager.mapList))
            //If new tme came before whitespace, set pos to 0, otherwise set pos to end of the text
            val newPos = if (manager.mapList.indexOf(newTME) > manager.mapList.indexOf(t)) 0 else newTME.text.length
            return determineCaretType(newTME, newPos, manager.mapList.indexOf(t))
        } else if (t is TableTextMapElement || t is ReadOnlyTableTextMapElement) {
            return XMLNodeCaret(Manager.getTableParent(t.node), cursorPos)
        } else {
            if (t.node is Element) {
                return XMLNodeCaret(t.node, cursorPos)
            }
        }
        require(t.node is Text) { "XMLTextCaret not found for " + t.javaClass + " TME. Node: " + t.node.toXML() }
        return XMLTextCaret(t.node as Text, pos, cursorPos)
    }

    private fun determineWhitespace(pos: Int): TextMapElement {
        val newTME = manager.mapList.getClosest(pos, true)

        //If after the beginning box line, see if there's a TME after it instead
        //(better for cut/paste)
        if (newTME is BoxLineTextMapElement && newTME.isStartSeparator) {
            val tempTME = manager.mapList.getNext(manager.mapList.indexOf(newTME), true)
            if (tempTME != null && tempTME !is BoxLineTextMapElement) {
                return tempTME
            }
        }
        return newTME
    }

    override fun initializeListeners() {
        manager.viewManager.initializeModuleListeners(this)
    }

    override fun removeListeners() {
        manager.viewManager.removeModuleListeners(this)
    }

    companion object {
        @JvmField
        var currentLine = 0

        protected var topIndex = 0
        private var topPixel = 0

        /**
         * Style the given StyleRange based on the action
         *
         * @param range
         * @param view
         * @param action
         * @param inlineElement
         * @return
         */
        fun setStyleRangeForAction(
            range: StyleRange,
            view: StyledText,
            action: IAction?,
            inlineElement: Element?
        ): Boolean {
            if (action is BBXEmphasisAction) {
                //Toggle bits as multiple emphasis can be applied
                for (curEmphasis in BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS[inlineElement]) {
                    when (curEmphasis) {
                        EmphasisType.BOLD -> range.fontStyle = range.fontStyle or SWT.BOLD
                        EmphasisType.ITALICS -> range.fontStyle = range.fontStyle or SWT.ITALIC
                        EmphasisType.UNDERLINE -> {
                            range.fontStyle = range.fontStyle or SWT.UNDERLINE_SINGLE
                            range.underline = true
                        }

                        EmphasisType.TRANS_1 -> range.background =
                            ColorManager.getColor(ColorManager.Colors.YELLOW, view)

                        EmphasisType.TRANS_2 -> range.background =
                            ColorManager.getColor(ColorManager.Colors.ORANGE, view)

                        EmphasisType.TRANS_3 -> range.background =
                            ColorManager.getColor(ColorManager.Colors.GREEN, view)

                        EmphasisType.TRANS_4 -> range.background = ColorManager.getColor(ColorManager.Colors.BLUE, view)
                        EmphasisType.TRANS_5 -> range.background =
                            ColorManager.getColor(ColorManager.Colors.LIGHT_PURPLE, view)

                        EmphasisType.NO_TRANSLATE ->             //214, 173, 218
                            range.background = Color(view.display, 225, 197, 228)

                        EmphasisType.NO_CONTRACT ->             //171, 233, 203
                            range.background = Color(view.display, 198, 240, 220)

                        EmphasisType.SCRIPT -> range.background = Color(view.display, 241, 190, 191)
                        EmphasisType.TRANS_NOTE -> {}
                        else -> throw UnsupportedOperationException("Unhandled emphasis $curEmphasis")
                    }
                }
            } else {
                //TODO: Are there any other actions that need colors?
                val color = ColorManager.getColorFromAction(action, view)
                if (color != null) {
                    range.background = color
                } else {
                    return false
                }
            }
            return true
        }
    }
}

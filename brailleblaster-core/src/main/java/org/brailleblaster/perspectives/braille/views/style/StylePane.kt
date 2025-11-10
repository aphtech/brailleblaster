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
package org.brailleblaster.perspectives.braille.views.style

import nu.xom.Element
import org.brailleblaster.abstractClasses.BBEditorView
import org.brailleblaster.bbx.BBX
import org.brailleblaster.easierxml.ImageUtils.getImageNavigateBlock
import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.BraillePageBrlMapElement
import org.brailleblaster.perspectives.braille.mapping.elements.PageIndicator
import org.brailleblaster.perspectives.braille.mapping.maps.MapList
import org.brailleblaster.perspectives.braille.mapping.maps.PaintedElementsList
import org.brailleblaster.perspectives.braille.views.wp.WPView
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.util.LINE_BREAK
import org.brailleblaster.util.Utils.runtimeToString
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getBanaStyles
import org.brailleblaster.utils.swt.AccessibilityUtils.setName
import org.brailleblaster.utils.swt.DebugStyledText
import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.events.*
import org.eclipse.swt.graphics.GC
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.widgets.Composite
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.math.max

class StylePane(parent: Composite, private val m: Manager) : BBEditorView {
    private val paintedElements = PaintedElementsList()
    private val pageIndicatorLines: MutableSet<Int> = HashSet()
    lateinit var widget: StyledText
    private var currentLine = 0
    var updateStylePane: Boolean = true
        private set

    init {
        resetView(parent)

        setName(widget, NAME)
    }

    fun generate(mapList: MapList) {
        val startTime = System.currentTimeMillis()
        removeAllPaintedElements()

        val textViewWidget = m.textView

        val sb = StringBuilder()
        var blockStart: Element? = null
        var blockEnd: Element? = null
        val processedBlocks = HashMap<Element?, Int>()
        val lines = TreeMap<Int?, String?>()
        for (textMapElement in mapList) {
            for (curBraille in textMapElement.brailleList) {
                if (curBraille !is BraillePageBrlMapElement) {
                    continue
                }

                val pageLine = m.brailleView.getLineAtOffset(curBraille.getStart(mapList))

                if (pageIndicatorLines.contains(pageLine)) {
                    continue
                }
                pageIndicatorLines.add(pageLine)

                val newPage = PageIndicator(m)
                newPage.line = pageLine
                newPage.startListener(this)
            }
            //			log.debug("Counter {}", counter);
            log.debug(
                "tme start {} end {} text {}",
                textMapElement.getStart(mapList),
                textMapElement.getEnd(mapList),
                if (textMapElement.node == null) null else XMLHandler.toXMLSimple(
                    textMapElement.node
                )
            )
            if (textMapElement.node == null) {
                log.trace("skipping null node")
                continue
            }

            // Issue #6276: Set Cell position doesn't show style here
            // Problem: Tab element is before MoveTo, so were still on the previous line
            // Solution: Just ignore tab characters and hope text isn't inside them
            if (XMLHandler.ancestorElementIs(textMapElement.node) { node: Element? -> BBX.SPAN.TAB.isA(node) }) {
                log.trace("skipping tab")
                continue
            }

            //Issue #4961: Some elements may not be set yet
            //Corey: Not all TMEs will have start/end values within the text view, sometimes
            //we have to "hide" parts of TMEs
            if (textMapElement.getStart(mapList) < 0 || textMapElement.getEnd(mapList) > textViewWidget.charCount) {
                log.trace("skipping negative start")
                continue
            }
            val lineAtOffset = textViewWidget.getLineAtOffset(textMapElement.getStart(mapList))

            var styledElement: Element?
            if (BBX.CONTAINER.isA(textMapElement.node)) {
                styledElement = textMapElement.node as Element
            } else if (XMLHandler.ancestorElementIs(
                    textMapElement.node
                ) { node: Element? -> BBX.CONTAINER.NUMBER_LINE.isA(node) }
            ) {
                styledElement = XMLHandler.ancestorVisitorElement(
                    textMapElement.node
                ) { node: Element? -> BBX.CONTAINER.NUMBER_LINE.isA(node) }
            } else if (XMLHandler.ancestorElementIs(
                    textMapElement.node
                ) { node: Element? -> BBX.CONTAINER.MATRIX.isA(node) }
            ) {
                styledElement = XMLHandler.ancestorVisitorElement(
                    textMapElement.node
                ) { node: Element? -> BBX.CONTAINER.MATRIX.isA(node) }
            } else if (XMLHandler.ancestorElementIs(
                    textMapElement.node
                ) { node: Element? -> BBX.CONTAINER.SPATIAL_GRID.isA(node) }
            ) {
                styledElement = XMLHandler.ancestorVisitorElement(
                    textMapElement.node
                ) { node: Element? -> BBX.CONTAINER.SPATIAL_GRID.isA(node) }
            } else if (XMLHandler.ancestorElementIs(
                    textMapElement.node
                ) { node: Element? -> BBX.CONTAINER.TEMPLATE.isA(node) }
            ) {
                styledElement = XMLHandler.ancestorVisitorElement(
                    textMapElement.node
                ) { node: Element? -> BBX.CONTAINER.TEMPLATE.isA(node) }
            } else if ((Manager.getTableParent(textMapElement.node).also { styledElement = it }) != null) {
                //use that table
            } else {
                val ancestorBlock = XMLHandler.ancestorVisitorElement(
                    textMapElement.node
                ) { node: Element? -> BBX.BLOCK.isA(node) }
                if (ancestorBlock == null) {
                    //Probably a floating UTD brl tag like sidebar boxline
                    //Regardless we can't actually do anything
                    log.trace("skipping no ancestor block")
                    continue
                }
                styledElement = ancestorBlock
            }
            if (processedBlocks.containsKey(styledElement) // block's first TME may not have usable content
            // (eg Set Cell Position tab is before MoveTo to next line)
            //					&& lines.containsKey(lineAtOffset) 
            //					&& !lines.get(lineAtOffset).isEmpty()
            ) {
                log.trace("skipping already processed element")
                continue
            }
            processedBlocks[Objects.requireNonNull(styledElement)] = lineAtOffset

            val styleName = findStyle(styledElement)
            if (styleName == null) {
                processedBlocks.remove(styledElement)
                log.trace("skipping null style")
                continue
            }

            if (lines.containsKey(lineAtOffset)) {
                log.trace("skipping already used line {}", lineAtOffset)
                continue
            }
            log.debug("line {} style {}", lineAtOffset, styleName)
            lines[lineAtOffset] = styleName

            //At end so only valid blocks are used
            if (blockStart == null) {
                blockStart = styledElement
            }
            blockEnd = styledElement
        }

        //Process images
        val blockEndActual = blockEnd
        var blockStartReached = false
        var blockEndReached = false
        for (imageNode in FastXPath.descendant(BBX.getRoot(m.doc))) {
            if (!blockStartReached) {
                if (imageNode === blockStart) {
                    blockStartReached = true
                } else {
                    continue
                }
            }

            if (!blockEndReached && imageNode === blockEnd) {
                blockEndReached = true
            } else if (blockEndReached && XMLHandler.ancestorElementNot(imageNode) { ancestor: Element -> ancestor === blockEndActual }) {
                break
            }

            val block: Element = getImageNavigateBlock(imageNode)?.let { block ->
                if (!processedBlocks.containsKey(block) || lines[processedBlocks[block]] == null) {
                    FastXPath.preceding(block)
                        .filterIsInstance<Element>()
                        .firstOrNull { node -> BBX.BLOCK.isA(node) && processedBlocks.containsKey(node) && lines[processedBlocks[node]] != null }
                } else {
                    block
                }
            } ?: continue



            val lineOfBlock = processedBlocks[block]
            val blockStylePaneText = lines[lineOfBlock]
            if (!blockStylePaneText!!.endsWith("*")) {
                lines[lineOfBlock] = "$blockStylePaneText*"
            }
        }

        var curLine = 0
        for ((key, value) in lines) {
            sb.append(
                LINE_BREAK.repeat(
                    max(0.0, (key!! - curLine).toDouble()).toInt()
                )
            ).append(value).append(LINE_BREAK)
            curLine = key + 1
        }

        while (curLine - 1 != textViewWidget.lineCount) {
            sb.append(LINE_BREAK)
            curLine++
        }

        widget.text = sb.toString()
        log.info("Building stylePane took {}", runtimeToString(startTime))
    }

    private fun findStyle(ancestorBlock: Element?): String? {
        var styleName = ancestorBlock!!.getAttributeValue(UTDElements.UTD_STYLE_ATTRIB)
        if (styleName == null) {
            //For things that get wrapped in prose, you can find the style in the container that wraps the blocks
            if ((ancestorBlock.parent != null && BBX.CONTAINER.PROSE.isA(ancestorBlock.parent)) && (ancestorBlock.parent as Element).getAttribute(
                    UTDElements.UTD_STYLE_ATTRIB
                ) != null
            ) {
                styleName = (ancestorBlock.parent as Element).getAttributeValue(UTDElements.UTD_STYLE_ATTRIB)
            } else {
                return null
            }
        }

        styleName = getBanaStyles()[m.document.settingsManager.getBaseStyle(styleName!!, ancestorBlock)]
        // Workaround for BLOCK.PAGE_NUM having a weird style
        if (BBX.BLOCK.PAGE_NUM.isA(ancestorBlock)) {
            styleName = "Print Page"
        }
        if (BBX.BLOCK.IMAGE_PLACEHOLDER.isA(ancestorBlock)) {
            styleName += " " + (BBX.BLOCK.IMAGE_PLACEHOLDER.ATTRIB_SKIP_LINES[ancestorBlock])
        }
        if (BBX.BLOCK.SPATIAL_MATH.isA(ancestorBlock)) {
            styleName = MathModuleUtils.SPATIAL_MATH
        }
        return styleName
    }

    override val view: StyledText
        get() = widget

    override fun resetView(parent: Composite) {
        widget = DebugStyledText(parent, SWT.BORDER or SWT.H_SCROLL).apply {
            alignment = SWT.RIGHT
            editable = false
            EasySWT.setGridData(this)
            (layoutData as GridData).grabExcessVerticalSpace = true
        }
    }

    override fun initializeListeners() {
        widget.addFocusListener(object : FocusListener {
            override fun focusLost(e: FocusEvent) {
                // TODO Auto-generated method stub
                setCurrent()
            }

            override fun focusGained(e: FocusEvent) {
                // TODO Auto-generated method stub
                setCurrent()
            }
        })
        widget.addKeyListener(object : KeyListener {
            override fun keyReleased(e: KeyEvent) {
                // TODO Auto-generated method stub
                updateTextView()
            }

            override fun keyPressed(e: KeyEvent) {
                // TODO Auto-generated method stub
                m.viewManager.textView.view.topPixel = widget.topPixel
            }
        })

        widget.addMouseListener(object : MouseListener {
            override fun mouseUp(e: MouseEvent) {
                // TODO Auto-generated method stub
                updateTextView()
            }

            override fun mouseDown(e: MouseEvent) {
                // TODO Auto-generated method stub
            }

            override fun mouseDoubleClick(e: MouseEvent) {
                // TODO Auto-generated method stub
            }
        })
    }

    override fun removeListeners() {
        removeAllPaintedElements()
    }

    private fun removeAllPaintedElements() {
        for (paintedElement in paintedElements) paintedElement.removeListener(this)
        paintedElements.clear()
    }

    override val charWidth: Int
        get() {
            val gc = GC(widget)
            val fm = gc.fontMetrics
            gc.dispose()
            return fm.averageCharacterWidth.toInt()
        }

    fun updateCursor() {
        view.caretOffset = view.getOffsetAtLine(WPView.currentLine)
    }

    fun updateCursor(line: Int) {
        m.stylePane.view.caretOffset = view.getOffsetAtLine(line)
    }

    private fun setCurrent() {
        val pos = widget.caretOffset
        val line = widget.getLineAtOffset(pos)
        val textView = m.textView
        if (line < textView.content.lineCount) {
            textView.caretOffset = textView.getOffsetAtLine(line)
        }
        val brailleView = m.brailleView
        if (line < brailleView.content.lineCount) {
            brailleView.caretOffset = brailleView.getOffsetAtLine(line)
        }
    }

    private fun updateTextView() {
        val pos = widget.caretOffset
        val line = widget.getLineAtOffset(pos)
        if (line < m.textView.content.lineCount) {
            if (currentLine != widget.getLineAtOffset(pos)) {
                updateStylePane = false
                m.viewManager.textView.setCurrentElement(m.textView.getOffsetAtLine(line))
                updateStylePane = true
                currentLine = widget.getLineAtOffset(pos)
            }
        }
    }


    companion object {
        private val log: Logger = LoggerFactory.getLogger(StylePane::class.java)
        private const val NAME = "Style Pane" //Name read by accessibility clients
    }
}


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
package org.brailleblaster.perspectives.braille.ui

import nu.xom.Element
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.utd.BBXDynamicOptionStyleMap
import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.math.mathml.MathUtils.wrapInMath
import org.brailleblaster.perspectives.braille.views.wp.TextRenderer
import org.brailleblaster.perspectives.mvc.modules.misc.ChangeTranslationModule
import org.brailleblaster.utd.IStyle
import org.brailleblaster.utd.Style.StyleOption
import org.brailleblaster.utd.internal.DynamicOptionStyleMap
import org.brailleblaster.utd.properties.EmphasisType
import org.brailleblaster.utd.utils.TableUtils
import org.brailleblaster.utd.utils.getDescendantBrlFast
import org.brailleblaster.util.ColorManager
import org.brailleblaster.util.LINE_BREAK
import org.brailleblaster.util.Notify.showMessage
import org.brailleblaster.utils.swt.EasySWT
import org.brailleblaster.utils.xml.BB_NS
import org.brailleblaster.utils.xom.detachAll
import org.brailleblaster.wordprocessor.WPManager
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.ExtendedModifyEvent
import org.eclipse.swt.custom.StyleRange
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.custom.VerifyKeyListener
import org.eclipse.swt.events.*
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.graphics.Resource
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Consumer

/**
 * Wrapper class for a StyledText that can have styles like Bold or Italic and can convert to XML
 */
class BBStyleableText(parent: Composite, buttonPanel: Composite?, buttons: Int, style: Int) {
    enum class TextStyle {
        BOLD, ITALIC, BOLDANDITALIC, UNDERLINE, COLOR_YELLOW, COLOR_ORANGE, COLOR_GREEN, COLOR_BLUE, COLOR_PURPLE, COLOR_LIGHT_PURPLE, COLOR_BROWN, COLOR_KHAKI, LARGE_FONT, COLOR_LAVENDER, COLOR_BLUSH, COLOR_SEAGLASS
    }

    abstract class Tag {
        var textStyle: TextStyle? = null
        abstract fun test(e: Element): Boolean
        override fun toString(): String {
            return "Tag{textStyle=$textStyle}"
        }
    }

    private class MathTag(textStyle: TextStyle?) : Tag() {
        init {
            this.textStyle = textStyle
        }

        override fun test(e: Element): Boolean {
            return BBX.INLINE.MATHML.isA(e)
        }

        override fun toString(): String {
            return "MathTag{" + "super=" + super.toString() + '}'
        }
    }

    private class StyleTag(var style: IStyle, textStyle: TextStyle?) : Tag() {
        init {
            this.textStyle = textStyle
        }

        override fun test(e: Element): Boolean {
            if (!BBX.BLOCK.STYLE.isA(e)) {
                return false
            }
            val elemStyle =
                WPManager.getInstance().controller.document.engine.styleMap.findValueOrDefault(e)
            return elemStyle.name != null && elemStyle.name == style.name
        }

        override fun toString(): String {
            return "StyleTag{" + "style=" + style.name + ", super=" + super.toString() + '}'
        }
    }

    private class EmphasisTag(var types: EnumSet<EmphasisType>, textStyle: TextStyle?) : Tag() {
        constructor(type: EmphasisType, textStyle: TextStyle?) : this(EnumSet.of<EmphasisType>(type), textStyle)

        init {
            this.textStyle = textStyle
        }

        override fun test(e: Element): Boolean {
            return BBX.INLINE.EMPHASIS.isA(e) && BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS[e] == types
        }
    }

    private class Range(var start: Int, var end: Int, var tag: Tag?) {
        val length: Int
            get() = end - start

        override fun toString(): String {
            return "Range{start=$start, end=$end, tag=$tag}"
        }
    }

    enum class EmphasisTags(val tagName: String, val defaultAccelerator: Int) {
        BOLD("Bold", SWT.MOD1 or 'B'.code), ITALIC("Italic", SWT.MOD1 or 'I'.code), BOLDITALIC(
            "Bold + Italic",
            SWT.MOD1 or SWT.MOD2 or 'B'.code
        ),
        ITALICBOLD("Italic + Bold", SWT.MOD1 or SWT.MOD2 or 'B'.code), SCRIPT(
            "Script",
            SWT.MOD1 or 'S'.code
        ),
        UNDERLINE("Underline", SWT.MOD1 or 'U'.code), TE1(
            "Transcriber Emphasis 1",
            SWT.MOD1 or '1'.code
        ),
        TE2("Transcriber Emphasis 2", SWT.MOD1 or '2'.code), TE3(
            "Transcriber Emphasis 3",
            SWT.MOD1 or '3'.code
        ),
        TE4("Transcriber Emphasis 4", SWT.MOD1 or '4'.code), TE5(
            "Transcriber Emphasis 5",
            SWT.MOD1 or '5'.code
        ),
        CTEXACT("Direct Translation", ChangeTranslationModule.DIRECT_HOTKEY), CTUNCONTRACTED(
            "Uncontracted Translation",
            ChangeTranslationModule.UNCONTRACTED_HOTKEY
        );
    }

    enum class StyleTags {
        CENTERED_HEADING, TPAGE_HEADING
    }

    enum class MathTags(val tagName: String, val acc: Int) {
        MATH("Math", 0);
    }

    private data class Margin(var firstLineIndent: Int, var indent: Int) {
        val displayName: String
            get() = "[" + (firstLineIndent + 1) + "-" + (indent + 1) + "]"
    }


    private var buttonPanel: Composite?
    private var newLineWrap = true

    @JvmField
    var text: StyledText
    private var styles: MutableList<Range>
    private val margins: MutableList<Margin>
    private var fontSize = DEFAULT_FONT_SIZE
    var display: Display
    private var buttonStyle: Int
    var parent: Composite
    private var disposeables: MutableList<Resource> = ArrayList()
    private var allTags: MutableList<Tag> = ArrayList()
    private var newLineStyle = "Body Text"
    private var curMarginPaintListener: Listener? = null

    init {
        checkParams(parent, buttonPanel, buttons)
        this.parent = parent
        this.buttonPanel = buttonPanel
        buttonStyle = buttons
        text = StyledText(parent, style)
        if (hasCustomMargins()) {
            changeMarginLineIndent(true)
        }
        disableTab(text)
        if (buttonPanel != null) createButtons()
        initTags()
        styles = ArrayList()
        margins = ArrayList()
        margins.add(DEFAULT_MARGIN)
        if (buttonStyle and NONEWLINES != 0) {
            newLineWrap = false
        }
        display = parent.display
        text.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                updateRanges()
                updateMargins(e.keyCode)
            }
        })

        // Issue #5744: Non breaking space support
        text.addVerifyKeyListener(object : VerifyKeyListener {
            private val log = LoggerFactory.getLogger(javaClass)
            override fun verifyKey(e: VerifyEvent) {
                if (e.character == SWT.SPACE && e.stateMask == SWT.MOD1) {
                    e.doit = false
                    log.info("adding nbps")
                    text.insert("" + TextRenderer.NON_BREAKING_SPACE)
                    val sr = StyleRange()
                    sr.start = text.caretOffset
                    sr.length = 1
                    sr.background = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK)
                    text.setStyleRange(sr)
                    text.caretOffset += 1
                    updateRanges()
                }
            }
        })
        text.addExtendedModifyListener { event: ExtendedModifyEvent ->
            if (event.replacedText.contains(LINE_BREAK)) {
                val startLine = text.getLineAtOffset(event.start) + 1
                var index = 0
                while (event.replacedText.indexOf(LINE_BREAK, index) != -1) {
                    if (startLine >= margins.size) {
                        log.error("Tried to delete a margin that did not exist. StartLine: " + startLine + " margins size:" + margins.size)
                        break
                    }
                    margins.removeAt(startLine)
                    index = event.replacedText.indexOf(LINE_BREAK, index) + 1
                }
            }
        }
        text.addDisposeListener(disposeListener())
        if (hasCustomMargins()) {
            curMarginPaintListener = newMarginPaintListener()
            text.addListener(SWT.Paint, curMarginPaintListener)
        }
    }

    /**
     * Converts all BBMarkdown elements into xml. All lines will be placed between p tags. Note: Always
     * check for null after using this method in case user incorrectly marks up text
     * @param root The root element
     * @return Root element of the converted xml, or null if an error occurs
     */
    fun getXML(root: Element): Element {
        return extractXML(root, allTags)
    }

    fun setXML(parent: Element) {
        val xmlString = xmlToString(parent, allTags)
        text.text = xmlString
        repaint()
    }

    fun clear() {
        text.text = ""
        styles = ArrayList()
        repaint()
    }

    private fun xmlToString(parent: Element, tags: List<Tag>): String {
        var parent = parent
        parent = parent.copy()

        //Detach all brls
        val brls = parent.getDescendantBrlFast()
        for (brl in brls) {
            brl.detach()
        }
        // Also handle tablebrl
        TableUtils.findTableBrls(parent).detachAll()
        return extractTextFromXML(parent, tags, 0)
    }

    private fun extractTextFromXML(parent: Element, tags: List<Tag>, startOffset: Int): String {
        val sb = StringBuilder()
        for (i in 0 until parent.childCount) {
            val child = parent.getChild(i)
            if (child is Text) {
                var relevantTag: Tag? = null
                for (tag in tags) {
                    if (tag.test(parent)) {
                        relevantTag = tag
                        break
                    }
                }
                if (relevantTag != null) {
                    val start = startOffset + sb.length
                    sb.append(child.value)
                    val end = startOffset + sb.length
                    styles.add(Range(start, end, relevantTag))
                } else sb.append(child.value)
            } else if (MathModuleUtils.isMathParent(child)) {
                val start = startOffset + sb.length
                sb.append(MathModuleUtils.getMathText(child))
                val end = startOffset + sb.length
                styles.add(Range(start, end, tags.first { n: Tag? -> n is MathTag }))
            } else if (child is Element) {
                if (newLineWrap && BBX.BLOCK.isA(child) && sb.isNotEmpty()) {
                    sb.append(LINE_BREAK)
                    computeMargin(child)
                }
                sb.append(extractTextFromXML(child, tags, startOffset + sb.length))
            }
        }
        return sb.toString()
    }

    private fun extractXML(root: Element, tags: List<Tag>): Element {
        var marginIndex = 0
        var parent = root
        if (newLineWrap) {
            parent = BBX.BLOCK.STYLE.create(newLineStyle)
            if (hasCustomMargins()) {
                if (margins[marginIndex] != DEFAULT_MARGIN) {
                    BBXDynamicOptionStyleMap.setStyleOptionAttrib(
                        parent,
                        StyleOption.FIRST_LINE_INDENT,
                        margins[marginIndex].firstLineIndent
                    )
                    BBXDynamicOptionStyleMap.setStyleOptionAttrib(
                        parent,
                        StyleOption.INDENT,
                        margins[marginIndex].indent
                    )
                }
                marginIndex++
            }
        }
        val allText = text.text
        val lines = allText.split(LINE_BREAK.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var startOffset = 0
        for (line in lines) {
            val ranges = getRangesInRange(startOffset, startOffset + line.length)
            if (ranges.isEmpty()) {
                parent.appendChild(line)
            } else {
                var curOffset = startOffset
                for (range in ranges) {
                    log.error("range $range")
                    if (range.start > curOffset) {
                        parent.appendChild(allText.substring(curOffset, range.start))
                    }
                    when (val tag = range.tag) {
                        is EmphasisTag -> {
                            val newElement = BBX.INLINE.EMPHASIS.create(tag.types)
                            newElement.appendChild(allText.substring(range.start, range.end))
                            parent.appendChild(newElement)
                        }

                        is StyleTag -> {
                            val newElement = BBX.BLOCK.STYLE.create(tag.style.name)
                            val text = allText.substring(range.start, range.end)
                            log.error(
                                "appending text '{}' {}", text, Character.getName(
                                    text[0].code
                                )
                            )
                            newElement.appendChild(text)
                            root.appendChild(parent)
                            parent = newElement
                        }

                        is MathTag -> {
                            wrapInMath(allText.substring(range.start, range.end), parent, parent.childCount)
                            log.error("math in table {}", allText)
                        }
                    }
                    curOffset = range.end
                }
                if (curOffset < line.length + startOffset) {
                    parent.appendChild(allText.substring(curOffset, line.length + startOffset))
                }
            }
            startOffset += line.length + LINE_BREAK.length
            if (newLineWrap) {
                root.appendChild(parent)
                parent = BBX.BLOCK.STYLE.create(newLineStyle)
                if (hasCustomMargins() && marginIndex < margins.size) {
                    if (margins[marginIndex] != DEFAULT_MARGIN) {
                        BBXDynamicOptionStyleMap.setStyleOptionAttrib(
                            parent,
                            StyleOption.FIRST_LINE_INDENT,
                            margins[marginIndex].firstLineIndent
                        )
                        BBXDynamicOptionStyleMap.setStyleOptionAttrib(
                            parent,
                            StyleOption.INDENT,
                            margins[marginIndex].indent
                        )
                    }
                    marginIndex++
                }
            }
        }
        return root
    }

    /*
	 * Use if the custom margins feature needs to be disabled (i.e. another feature
	 * overrides the margins like the TPages Dialog's centered style)
	 */
    fun setCustomMargins(enabled: Boolean) {
        buttonStyle = buttonStyle xor CUSTOM_MARGINS
        if (!hasCustomMargins()) {
            if (curMarginPaintListener != null) {
                text.removeListener(SWT.Paint, curMarginPaintListener)
            }
            changeMarginLineIndent(false)
        } else {
            curMarginPaintListener = newMarginPaintListener()
            text.addListener(SWT.Paint, curMarginPaintListener)
            changeMarginLineIndent(true)
        }
        text.redraw()
    }

    private fun updateMargins(keyCode: Int) {
        if (hasCustomMargins()) {
            if (keyCode == SWT.CR.code || keyCode == SWT.KEYPAD_CR) {
                val curLine = text.getLineAtOffset(text.caretOffset)
                margins.add(curLine, DEFAULT_MARGIN)
                text.redraw()
            }
        }
    }

    private fun computeMargin(element: Element) {
        if (element.getAttribute(FIRST_LINE_INDENT_ATTRIB, BB_NS) != null
            && element.getAttribute(INDENT_ATTRIB, BB_NS) != null
        ) {
            margins.add(
                Margin(
                    element.getAttributeValue(FIRST_LINE_INDENT_ATTRIB, BB_NS).toInt(),
                    element.getAttributeValue(INDENT_ATTRIB, BB_NS).toInt()
                )
            )
        } else {
            margins.add(DEFAULT_MARGIN.copy())
        }
    }

    fun removeAllEmphasis() {
        styles.clear()
        text.setStyleRange(null)
        repaint()
    }

    fun toggleTag(tag: EmphasisTags) {
        toggleTag(getEquivalent(tag))
    }

    fun toggleTag(tag: MathTags?) {
        toggleTag(getEquivalent(MathTags.MATH))
    }

    fun toggleTag(tag: StyleTags) {
        toggleTag(getEquivalent(tag))
    }

    fun applyEmphasisToAll(tag: EmphasisTags) {
        applyEmphasisToAll(getEquivalent(tag))
    }

    fun applyEmphasisToAll(tag: MathTags) {
        applyEmphasisToAll(getEquivalent(tag))
    }

    fun applyMargin(firstLineIndent: Int, indent: Int) {
        if (!hasCustomMargins()) {
            return
        }
        val curLine = text.getLineAtOffset(text.caretOffset)
        if (curLine > margins.size) {
            log.error("curLine: " + curLine + " margin size: " + margins.size)
            return
        }
        margins.removeAt(curLine)
        margins.add(curLine, Margin(firstLineIndent, indent))
        text.redraw()
    }

    private fun getEquivalent(tag: EmphasisTags): Tag {
        return when (tag) {
            EmphasisTags.BOLD -> BOLDTAG
            EmphasisTags.BOLDITALIC -> BOLDITALICTAG
            EmphasisTags.CTEXACT -> CTEXACTTAG
            EmphasisTags.CTUNCONTRACTED -> CTUNCONTRACTEDTAG
            EmphasisTags.ITALIC -> ITALICTAG
            EmphasisTags.ITALICBOLD -> ITALICBOLDTAG
            EmphasisTags.SCRIPT -> SCRIPTTAG
            EmphasisTags.TE1 -> TE1
            EmphasisTags.TE2 -> TE2
            EmphasisTags.TE3 -> TE3
            EmphasisTags.TE4 -> TE4
            EmphasisTags.TE5 -> TE5
            EmphasisTags.UNDERLINE -> UNDERLINETAG
        }
    }

    private fun getEquivalent(tag: MathTags): Tag {
        return when (tag) {
            MathTags.MATH -> MATHTAG
        }
    }

    private fun getEquivalent(tag: StyleTags): Tag {
        return when (tag) {
            StyleTags.CENTERED_HEADING -> CENTEREDHEADING
            StyleTags.TPAGE_HEADING -> TPAGEHEADING
        }
    }

    private fun toggleTag(tag: Tag) {
        if (text.selectionCount != 0) {
            val split = splitSelection(LINE_BREAK)
            var cont = true
            var i = 0
            while (i < split.size) {
                val start = split[i]
                val end = split[i + 1]
                if (verifyToggle(start, end, tag) && cont) {
                    val index = findIndex(start, end)
                    if (index == -1) {
                        styles.add(Range(start, end, tag))
                    } else {
                        styles.add(index, Range(start, end, tag))
                    }
                } else cont = false
                i += 2
            }
            repaint()
        }
        text.setSelection(text.caretOffset)
    }

    private fun applyEmphasisToAll(tag: Tag) {
        if (text.text.isEmpty()) return
        styles.clear()
        text.setStyleRange(null)
        text.setSelection(0, text.text.length)
        toggleTag(tag)
    }

    private fun createButtons() {
        val includeBold = buttonStyle and BOLD != 0
        val includeItalic = buttonStyle and ITALIC != 0
        val includeCT = buttonStyle and CHANGETRANSLATION != 0
        val includeTE = buttonStyle and TRANSCRIBEREMPHASIS != 0
        val includeScript = buttonStyle and SCRIPT != 0
        val includeUnderline = buttonStyle and UNDERLINE != 0
        val includeHeading = buttonStyle and CREATEHEADING != 0 || buttonStyle and CREATETPAGEHEADING != 0
        if (includeBold || includeItalic || includeTE || includeScript || includeUnderline) {
            val styleCombo = Combo(buttonPanel, SWT.DROP_DOWN or SWT.READ_ONLY)
            if (includeBold) {
                styleCombo.add("Bold")
                styleCombo.setData("Bold", BOLDTAG)
            }
            if (includeItalic) {
                styleCombo.add("Italic")
                styleCombo.setData("Italic", ITALICTAG)
            }
            if (includeBold && includeItalic) {
                styleCombo.add("Bold+Italic")
                styleCombo.setData("Bold+Italic", BOLDITALICTAG)
            }
            if (includeScript) {
                styleCombo.add("Script")
                styleCombo.setData("Script", SCRIPTTAG)
            }
            if (includeUnderline) {
                styleCombo.add("Underline")
                styleCombo.setData("Underline", UNDERLINETAG)
            }
            if (includeTE) {
                styleCombo.add("TE 1")
                styleCombo.setData("TE 1", TE1)
                styleCombo.add("TE 2")
                styleCombo.setData("TE 2", TE2)
                styleCombo.add("TE 3")
                styleCombo.setData("TE 3", TE3)
                styleCombo.add("TE 4")
                styleCombo.setData("TE 4", TE4)
                styleCombo.add("TE 5")
                styleCombo.setData("TE 5", TE5)
            }
            styleCombo.select(0)
            EasySWT.makePushButton(buttonPanel, "Apply", 1) {
                toggleTag(
                    styleCombo.getData(
                        styleCombo.getItem(styleCombo.selectionIndex)
                    ) as Tag
                )
            }
        }
        if (hasCustomMargins()) {
            EasySWT.makePushButton(
                buttonPanel,
                if (buttonStyle and SMALL_BUTTONS != 0) "Runover" else "Change Runover",
                1
            ) { openChangeMarginDialog() }
        }
        if (includeCT) {
            EasySWT.makePushButton(
                buttonPanel,
                if (buttonStyle and SMALL_BUTTONS != 0) "Translation" else "Change Translation",
                1
            ) { openChangeTranslationDialog() }
        }
        if (includeHeading) {
            EasySWT.makePushButton(
                buttonPanel,
                if (buttonStyle and SMALL_BUTTONS != 0) "Heading" else "Create Heading",
                1
            ) { openCreateHeadingDialog() }
        }
    }

    private fun initTags() {
        allTags.add(BOLDTAG)
        allTags.add(ITALICTAG)
        allTags.add(0, BOLDITALICTAG)
        allTags.add(0, ITALICBOLDTAG)
        allTags.add(SCRIPTTAG)
        allTags.add(UNDERLINETAG)
        allTags.add(TE1)
        allTags.add(TE2)
        allTags.add(TE3)
        allTags.add(TE4)
        allTags.add(TE5)
        allTags.add(CTEXACTTAG)
        allTags.add(CTUNCONTRACTEDTAG)
        allTags.add(CENTEREDHEADING)
        allTags.add(TPAGEHEADING)
        allTags.add(MATHTAG)
        //		allTags.add(CELL5HEADING);
    }

    private fun openChangeTranslationDialog() {
        val ctDialog = Shell(parent.shell, SWT.DIALOG_TRIM or SWT.APPLICATION_MODAL)
        ctDialog.layout = RowLayout(SWT.VERTICAL)
        val newLabel = Label(ctDialog, SWT.NONE)
        newLabel.text = "Change translation to:"
        val dropDown = Combo(ctDialog, SWT.DROP_DOWN or SWT.BORDER)
        dropDown.setData("0", CTUNCONTRACTEDTAG)
        dropDown.add("Uncontracted")
        dropDown.setData("1", CTEXACTTAG)
        dropDown.add("Direct")
        dropDown.setData("2", MATHTAG)
        dropDown.add("Math")
        dropDown.select(0)
        val okButton = Button(ctDialog, SWT.PUSH)
        okButton.text = "Ok"
        okButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                toggleTag(dropDown.getData(dropDown.selectionIndex.toString()) as Tag)
                ctDialog.close()
            }
        })
        ctDialog.pack(true)
        ctDialog.open()
    }

    private fun openCreateHeadingDialog() {
        val headingDialog = Shell(parent.shell, SWT.DIALOG_TRIM or SWT.APPLICATION_MODAL)
        headingDialog.layout = RowLayout(SWT.VERTICAL)
        val newLabel = Label(headingDialog, SWT.NONE)
        newLabel.text = "Heading type:"
        val dropDown = Combo(headingDialog, SWT.DROP_DOWN or SWT.BORDER)
        if (buttonStyle and CREATEHEADING != 0) {
            dropDown.setData("0", CENTEREDHEADING)
            dropDown.add("Centered")
        } else if (buttonStyle and CREATETPAGEHEADING != 0) {
            dropDown.setData("0", TPAGEHEADING)
            dropDown.add("Centered (TPage)")
        }
        //TODO: Figure out how to make this less nimas-specific
        //and also figure out a way to differentiate headings
//		dropDown.setData("1", CELL5HEADING);
//		dropDown.add("Cell 5");
        dropDown.select(0)
        val okButton = Button(headingDialog, SWT.PUSH)
        okButton.text = "Ok"
        okButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                toggleTag(dropDown.getData(dropDown.selectionIndex.toString()) as Tag)
                headingDialog.close()
            }
        })
        headingDialog.pack(true)
        headingDialog.open()
    }

    private fun openChangeMarginDialog() {
        //TODO
        throw UnsupportedOperationException()
    }

    private fun splitSelection(match: String): IntArray {
        val selection = text.selectionText
        if (!selection.contains(match)) return intArrayOf(text.selection.x, text.selection.y)
        var curOffset = text.selection.x
        val split = selection.split(match.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val returnInts = IntArray(split.size * 2)
        for (i in split.indices) {
            returnInts[i * 2] = curOffset
            curOffset += split[i].length
            returnInts[i * 2 + 1] = curOffset
            curOffset += match.length
        }
        return returnInts
    }

    private fun findIndex(start: Int, end: Int): Int {
        if (styles.isEmpty()) return 0
        if (styles.size == 1) {
            return if (start < styles[0].start) 0 else 1
        }
        for ((i, curRange) in styles.withIndex()) {
            if (i != 0) {
                val prevRange = styles[i - 1]
                if (start >= prevRange.end && end <= curRange.start) return i
            } else if (end <= curRange.start) return 0
        }
        return -1
    }

    private fun getRangesInRange(start: Int, end: Int): List<Range> {
        val returnList: MutableList<Range> = ArrayList()
        for (point in styles) {
            if (start > point.start && start < point.end) {
                returnList.add(point)
                if (end < point.end) return returnList
            } else if (end > point.start && end < point.end) {
                returnList.add(point)
                return returnList
            } else if (point.start >= start && point.end <= end) {
                returnList.add(point)
            }
        }
        return returnList
    }

    private fun verifyToggle(start: Int, end: Int, tag: Tag): Boolean {
        var returnBool = true
        val points = getRangesInRange(start, end)
        for (point in points) {
            var replace1: Range? = null
            var replace2: Range? = null
            if (start > point.start) {
                replace1 = Range(point.start, start, point.tag)
            }
            if (end < point.end) {
                replace2 = Range(end, point.end, point.tag)
            }
            if (point.tag!!.textStyle == tag.textStyle) returnBool = false
            var index = styles.indexOf(point)
            styles.removeAt(index)
            if (replace1 != null) {
                styles.add(index, replace1)
                index++
            }
            if (replace2 != null) {
                styles.add(index, replace2)
            }
        }
        return returnBool
    }

    private fun updateRanges() {
        val styleRanges = text.styleRanges
        styles = ArrayList()
        for (sr in styleRanges) {
            if (sr.background != null && ColorManager.equals(sr.background, ColorManager.Colors.BLACK)) {
                // skip non-breaking spaces, they should be treated as part of the s text
                continue
            }
            val tagFromStyle = getTagFromStyle(sr)
            styles.add(Range(sr.start, sr.start + sr.length, tagFromStyle))
        }
    }

    private fun getTagFromStyle(sr: StyleRange): Tag? {
        var curTag: Tag? = null
        for (allTag in allTags) {
            curTag = allTag
            when (curTag.textStyle) {
                TextStyle.BOLD -> if (sr.fontStyle == SWT.BOLD) return curTag
                TextStyle.ITALIC -> if (sr.fontStyle == SWT.ITALIC) return curTag
                TextStyle.BOLDANDITALIC -> if (sr.fontStyle == SWT.BOLD or SWT.ITALIC) return curTag
                TextStyle.UNDERLINE -> if (sr.underline) return curTag
                TextStyle.COLOR_LAVENDER -> if (sr.background != null && ColorManager.equals(
                        sr.background,
                        ColorManager.Colors.LAVENDER
                    )
                ) return curTag

                TextStyle.COLOR_SEAGLASS -> if (sr.background != null && ColorManager.equals(
                        sr.background,
                        ColorManager.Colors.SEAGLASS
                    )
                ) return curTag

                TextStyle.COLOR_BLUSH -> if (sr.background != null && ColorManager.equals(
                        sr.background,
                        ColorManager.Colors.BLUSH
                    )
                ) return curTag

                TextStyle.COLOR_BLUE -> if (sr.background != null && ColorManager.equals(
                        sr.background,
                        ColorManager.Colors.BLUE
                    )
                ) return curTag

                TextStyle.COLOR_BROWN -> if (sr.background != null && ColorManager.equals(
                        sr.background,
                        ColorManager.Colors.BROWN
                    )
                ) {
                    return curTag
                }

                TextStyle.COLOR_GREEN -> if (sr.background != null && ColorManager.equals(
                        sr.background,
                        ColorManager.Colors.GREEN
                    )
                ) return curTag

                TextStyle.COLOR_KHAKI -> if (sr.background != null && ColorManager.equals(
                        sr.background,
                        ColorManager.Colors.KHAKI
                    )
                ) return curTag

                TextStyle.COLOR_ORANGE -> if (sr.background != null && ColorManager.equals(
                        sr.background,
                        ColorManager.Colors.ORANGE
                    )
                ) return curTag

                TextStyle.COLOR_PURPLE -> if (sr.background != null && ColorManager.equals(
                        sr.background,
                        ColorManager.Colors.PURPLE
                    )
                ) return curTag

                TextStyle.COLOR_LIGHT_PURPLE -> if (sr.background != null && ColorManager.equals(
                        sr.background,
                        ColorManager.Colors.LIGHT_PURPLE
                    )
                ) return curTag

                TextStyle.COLOR_YELLOW -> if (sr.background != null && ColorManager.equals(
                        sr.background,
                        ColorManager.Colors.YELLOW
                    )
                ) return curTag

                TextStyle.LARGE_FONT -> if (sr.font != null && sr.font.fontData[0].height > fontSize) {
                    return curTag
                }

                else -> return null
            }
        }
        return curTag
    }

    private fun repaint() {
        text.setStyleRange(null)

        // Issue #5744: Highlight nbsp
        for (range in TextRenderer.setNonBreakingSpaceEmphasis(text, 0, text.charCount)) {
            text.setStyleRange(range)
        }
        if (styles.isEmpty() || text.text.isEmpty()) return
        val ranges = arrayOfNulls<StyleRange>(styles.size)
        for (i in styles.indices) {
            val range = styles[i]
            val styleRange = getSRForTextStyle(range.start, range.length, range.tag!!.textStyle)
            ranges[i] = styleRange
        }
        try {
            text.styleRanges = ranges
        } catch (_: IllegalArgumentException) {
            if (logger.isErrorEnabled) {
                logger.error("Invalid StyleRange for BBMarkdownText. Style ranges:")
                for (range in ranges) {
                    logger.error("Start: {} Length: {}", range!!.start, range.length)
                }
                logger.error("Text content: {}", text.text)
            }
            showMessage("An error occurred while opening the table.")
        }
    }

    private fun hasCustomMargins(): Boolean {
        return buttonStyle and CUSTOM_MARGINS != 0
    }

    private fun checkParams(parent: Composite?, buttonPanel: Composite?, buttons: Int) {
        requireNotNull(parent) { "Parent cannot be null" }
        if (buttonPanel == null) {
            val errorType = StringBuilder()
            if (buttons and BOLD != 0) errorType.append("BOLD,")
            if (buttons and ITALIC != 0) errorType.append("ITALIC,")
            if (buttons and SCRIPT != 0) errorType.append("SCRIPT,")
            if (buttons and UNDERLINE != 0) errorType.append("UNDERLINE,")
            if (buttons and CREATEHEADING != 0) errorType.append("CREATEHEADING,")
            if (buttons and CHANGETRANSLATION != 0) errorType.append("CHANGETRANSLATION,")
            if (errorType.isNotEmpty()) {
                errorType.deleteCharAt(errorType.length - 1)
                throw IllegalArgumentException("ButtonPanel cannot be null with buttons: $errorType")
            }
        }
    }

    private fun getSRForTextStyle(start: Int, length: Int, style: TextStyle?): StyleRange {
        val styleRange = StyleRange()
        styleRange.start = start
        styleRange.length = length
        when (style) {
            TextStyle.BOLD -> styleRange.fontStyle = SWT.BOLD
            TextStyle.ITALIC -> styleRange.fontStyle = SWT.ITALIC
            TextStyle.BOLDANDITALIC -> styleRange.fontStyle = SWT.BOLD or SWT.ITALIC
            TextStyle.UNDERLINE -> styleRange.underline = true
            TextStyle.COLOR_LAVENDER -> styleRange.background =
                ColorManager.getColor(ColorManager.Colors.LAVENDER, text)

            TextStyle.COLOR_SEAGLASS -> styleRange.background =
                ColorManager.getColor(ColorManager.Colors.SEAGLASS, text)

            TextStyle.COLOR_BLUSH -> styleRange.background = ColorManager.getColor(ColorManager.Colors.BLUSH, text)
            TextStyle.COLOR_BLUE -> styleRange.background = ColorManager.getColor(ColorManager.Colors.BLUE, text)
            TextStyle.COLOR_BROWN -> styleRange.background = ColorManager.getColor(ColorManager.Colors.BROWN, text)
            TextStyle.COLOR_GREEN -> styleRange.background = ColorManager.getColor(ColorManager.Colors.GREEN, text)
            TextStyle.COLOR_KHAKI -> styleRange.background = ColorManager.getColor(ColorManager.Colors.KHAKI, text)
            TextStyle.COLOR_ORANGE -> styleRange.background = ColorManager.getColor(ColorManager.Colors.ORANGE, text)
            TextStyle.COLOR_PURPLE -> styleRange.background = ColorManager.getColor(ColorManager.Colors.PURPLE, text)
            TextStyle.COLOR_LIGHT_PURPLE -> styleRange.background =
                ColorManager.getColor(ColorManager.Colors.LIGHT_PURPLE, text)

            TextStyle.COLOR_YELLOW -> styleRange.background = ColorManager.getColor(ColorManager.Colors.YELLOW, text)
            TextStyle.LARGE_FONT -> {
                val font = text.font.fontData[0]
                font.setHeight(fontSize + 5)
                val newFont = Font(display, font)
                styleRange.font = newFont
                disposeables.add(newFont)
            }

            else -> {}
        }
        return styleRange
    }

    fun setFontSize(fontSize: Int) {
        this.fontSize = fontSize
        val data = text.font.fontData[0]
        data.setHeight(fontSize)
        val newFont = Font(display, data)
        text.font = newFont
        disposeables.add(newFont)
    }

    fun setNewLineWrapStyle(newStyleName: String) {
        newLineStyle = newStyleName
    }

    private fun disableTab(text: StyledText) {
        text.addTraverseListener { e: TraverseEvent ->
            if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) e.doit = true
        }
    }

    fun dispose() {
        text.dispose()
        if (buttonPanel != null) {
            for (control in buttonPanel!!.children) {
                control.dispose()
            }
        }
    }

    private fun disposeListener(): DisposeListener {
        return DisposeListener { disposeables.forEach(Consumer { obj: Resource -> obj.dispose() }) }
    }

    /*
	 * When margins are painted onto the text box, the indent of the text
	 * box must be moved to the right to keep the text from intersecting with
	 * the painted text
	 */
    private fun changeMarginLineIndent(indented: Boolean) {
        if (indented) {
            text.indent = EasySWT.getWidthOfText("[1-3] ")
        } else {
            text.indent = 0
        }
    }

    /*
	 * This listener allows margins to be painted onto the text box
	 */
    private fun newMarginPaintListener(): Listener {
        return Listener { event: Event ->
            var curLine = 0
            while (curLine < text.lineCount && curLine < margins.size) {
                val x = text.bounds.x - 7
                val y = text.getLinePixel(curLine)
                event.gc.drawText(margins[curLine].displayName, x, y)
                curLine++
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(BBStyleableText::class.java)

        /**
         * Constant to include Transcriber Emphasis options
         */
        const val TRANSCRIBEREMPHASIS = 1 shl 1

        /**
         * Constant to include the Bold style
         */
        const val BOLD = 1 shl 2

        /**
         * Constant to include the Italic style
         */
        const val ITALIC = 1 shl 3

        /**
         * Constant to include the Script style
         */
        const val SCRIPT = 1 shl 4

        /**
         * Constant to include the Underline style
         */
        const val UNDERLINE = 1 shl 5

        /**
         * Constant to include Create Heading button
         */
        const val CREATEHEADING = 1 shl 6

        /**
         * Constant to include Change Translation button
         */
        const val CHANGETRANSLATION = 1 shl 7

        /**
         * Constant to include Create TPage Heading for the TPage dialog
         */
        const val CREATETPAGEHEADING = 1 shl 8

        /**
         * Style constant to minimize button size
         */
        const val SMALL_BUTTONS = 1 shl 10

        /**
         * Constant to allow user to change identation of created {@code
         *
         *} tags
         */
        const val CUSTOM_MARGINS = 1 shl 11

        /**
         * Constant to prevent wrapping lines in {@code
         *
         *} tags
         */
        const val NONEWLINES = 1 shl 15
        val logger: Logger = LoggerFactory.getLogger(BBStyleableText::class.java)

        /**
         * Creates a BBStyleableText with:
         * <list>
         *  * All emphasis options
         *  * No new lines
         *  * SWT.BORDER
         *  * SWT.V_SCROLL
         *  * SWT.WRAP
        </list> *
         */
        fun createDefaultText(parent: Composite, buttonPanel: Composite?): BBStyleableText {
            return BBStyleableText(
                parent,
                buttonPanel,
                BOLD or ITALIC or SCRIPT or UNDERLINE or TRANSCRIBEREMPHASIS or CHANGETRANSLATION or SMALL_BUTTONS or NONEWLINES,
                SWT.BORDER or SWT.V_SCROLL or SWT.WRAP
            )
        }

        private val BOLDTAG: Tag = EmphasisTag(EmphasisType.BOLD, TextStyle.BOLD)
        private val ITALICTAG: Tag = EmphasisTag(EmphasisType.ITALICS, TextStyle.ITALIC)
        private val SCRIPTTAG: Tag = EmphasisTag(EmphasisType.SCRIPT, TextStyle.COLOR_KHAKI)
        private val UNDERLINETAG: Tag = EmphasisTag(EmphasisType.UNDERLINE, TextStyle.UNDERLINE)
        private val CTEXACTTAG: Tag = EmphasisTag(EmphasisType.NO_TRANSLATE, TextStyle.COLOR_LAVENDER)
        private val CTUNCONTRACTEDTAG: Tag = EmphasisTag(EmphasisType.NO_CONTRACT, TextStyle.COLOR_SEAGLASS)
        private val MATHTAG: Tag = MathTag(TextStyle.COLOR_LIGHT_PURPLE)
        private val TE1: Tag = EmphasisTag(EmphasisType.TRANS_1, TextStyle.COLOR_YELLOW)
        private val TE2: Tag = EmphasisTag(EmphasisType.TRANS_2, TextStyle.COLOR_ORANGE)
        private val TE3: Tag = EmphasisTag(EmphasisType.TRANS_3, TextStyle.COLOR_GREEN)
        private val TE4: Tag = EmphasisTag(EmphasisType.TRANS_4, TextStyle.COLOR_BLUE)
        private val TE5: Tag = EmphasisTag(EmphasisType.TRANS_5, TextStyle.COLOR_LIGHT_PURPLE)
        private val BOLDITALICTAG: Tag =
            EmphasisTag(EnumSet.of(EmphasisType.BOLD, EmphasisType.ITALICS), TextStyle.BOLDANDITALIC)
        private val ITALICBOLDTAG: Tag =
            EmphasisTag(EnumSet.of(EmphasisType.BOLD, EmphasisType.ITALICS), TextStyle.BOLDANDITALIC)

        //Not yet supported
        private val CENTEREDHEADING: Tag = StyleTag(
            WPManager.getInstance().list[0].document.engine.styleDefinitions.getStyleByName("Centered Heading")!!,
            TextStyle.LARGE_FONT
        )
        private val TPAGEHEADING: Tag = StyleTag(
            WPManager.getInstance().list[0].document.engine.styleDefinitions.getStyleByName("TPage Heading")!!,
            TextStyle.LARGE_FONT
        )
        private const val DEFAULT_FONT_SIZE = 10
        private val INDENT_ATTRIB =
            DynamicOptionStyleMap.styleOptionName(BBXDynamicOptionStyleMap.OPTION_ATTRIB_PREFIX, StyleOption.INDENT)
        private val DEFAULT_MARGIN = Margin(0, 2)
        private val FIRST_LINE_INDENT_ATTRIB = DynamicOptionStyleMap.styleOptionName(
            BBXDynamicOptionStyleMap.OPTION_ATTRIB_PREFIX,
            StyleOption.FIRST_LINE_INDENT
        )
    }
}
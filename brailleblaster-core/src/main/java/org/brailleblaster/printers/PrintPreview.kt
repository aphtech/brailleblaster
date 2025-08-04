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
package org.brailleblaster.printers

import com.google.common.collect.Range
import nu.xom.Document
import org.brailleblaster.BBIni
import org.brailleblaster.embossers.EmbossingUtils.emboss
import org.brailleblaster.embossers.EmbossingUtils.embossBrf
import org.brailleblaster.exceptions.BBNotifyException
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.views.wp.SixKeyHandler
import org.brailleblaster.perspectives.mvc.menu.MenuManager.menuItemAcceleratorSuffix
import org.brailleblaster.perspectives.mvc.modules.misc.FontSizeModule
import org.brailleblaster.printers.DocumentData.BBDocumentData
import org.brailleblaster.printers.DocumentData.BrfDocumentData
import org.brailleblaster.settings.UTDManager
import org.brailleblaster.settings.ui.BrailleSettingsDialog
import org.brailleblaster.settings.ui.EmbosserSettingsTab
import org.brailleblaster.utd.BRFWriter
import org.brailleblaster.utd.BRFWriter.PageListener
import org.brailleblaster.util.FormUIUtils
import org.brailleblaster.util.Notify.showMessage
import org.brailleblaster.util.WorkingDialog
import org.brailleblaster.utils.braille.BrailleUnicodeConverter
import org.brailleblaster.utils.braille.BrailleUnicodeConverter.unicodeToAsciiLouis
import org.brailleblaster.utils.braille.BrailleUnicodeConverter.unicodeToAsciiUppercase
import org.brailleblaster.utils.swt.AccessibilityUtils.setName
import org.brailleblaster.wordprocessor.FontManager
import org.brailleblaster.wordprocessor.FontManager.Companion.decreaseFontSetting
import org.brailleblaster.wordprocessor.FontManager.Companion.increaseFontSetting
import org.brailleblaster.wordprocessor.FontManager.Companion.isShowBraille
import org.brailleblaster.wordprocessor.FontManager.Companion.newBrailleFont
import org.brailleblaster.wordprocessor.FontManager.Companion.newTextFont
import org.brailleblaster.wordprocessor.WPManager
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.events.*
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.Path
import java.util.function.BiFunction
import kotlin.io.path.readText
import kotlin.math.max

class PrintPreview private constructor(
    parent: Shell, private val fontManager: FontManager?, private val docData: DocumentData,
    private val m: Manager
) {
    private val viewColorDefault: Color
    private val viewColorDisabled: Color
    private val shell: Shell
    private val pageStartOffsets: MutableList<Int> = ArrayList()
    private val brlPageOffsets: MutableMap<String, Int> = LinkedHashMap()

    /**
     * Contains both braille and orig versions of page number
     */
    private val printPageOffsets: MutableMap<String, Int> = LinkedHashMap()
    private val viewLeft: StyledText
    private val viewRight: StyledText
    private var lastFocusedView: StyledText? = null
    private val nextPageButton: Button
    private val previousPageButton: Button
    private val brlPageButton: Button
    private val rawPageButton: Button
    private val searchSixKey: Button
    private val searchSixKeyUppercase: Button
    private val dualViewMenuItem: MenuItem
    private val unicodeMenuItem: MenuItem
    private val curPage: Text
    private val searchText: Text
    private lateinit var brfOutput: String
    private val pageRanges: MutableList<Range<Int>> = ArrayList()
    private var currentViewMode: ViewMode
    private val dualViewMode: ViewMode
    private val singleViewMode: ViewMode
    private val ppStatusBar: PPStatusBar

    constructor(parent: Shell, brfFile: Path?, wpManager: WPManager) : this(
        parent, null, BrfDocumentData(
            brfFile!!
        ), wpManager.controller
    )

    constructor(parent: Shell, fontManager: FontManager, utdManager: UTDManager, doc: Document, m: Manager) : this(
        parent,
        fontManager,
        BBDocumentData(utdManager, doc),
        m
    )

    init {
        WorkingDialog("Generating BRF").use { wait ->

            // Load settings
            val userSettings = BBIni.propertyFileManager
            val pageType = userSettings.getProperty(SETTINGS_KEY_PAGE_TYPE, "rawPage")

            this.shell = Shell(parent, SWT.TITLE or SWT.BORDER or SWT.APPLICATION_MODAL or SWT.CENTER or SWT.CLOSE)
            shell.layout = GridLayout(2, true)
            shell.text = "Braille Preview"

            // Menu
            val topMenu = Menu(shell, SWT.BAR)
            shell.menuBar = topMenu
            // file
            val fileMenuItem = MenuItem(topMenu, SWT.CASCADE)
            fileMenuItem.text = "File"

            val fileMenu = Menu(fileMenuItem)
            fileMenuItem.menu = fileMenu

            val embossMenuItem = MenuItem(fileMenu, SWT.CASCADE)
            embossMenuItem.text = "Emboss" + menuItemAcceleratorSuffix(SWT.MOD1 or 'E'.code)
            embossMenuItem.accelerator = SWT.MOD1 or 'E'.code

            // view
            val viewMenuItem = MenuItem(topMenu, SWT.CASCADE)
            viewMenuItem.text = "View"

            val viewMenu = Menu(viewMenuItem)
            viewMenuItem.menu = viewMenu

            dualViewMenuItem = MenuItem(viewMenu, SWT.CHECK)
            dualViewMenuItem.text = "2 Page View"

            unicodeMenuItem = MenuItem(viewMenu, SWT.CHECK)
            unicodeMenuItem.text = "Display as Unicode"

            val increaseFontMenuItem = MenuItem(viewMenu, SWT.PUSH)
            increaseFontMenuItem.text =
                "Increase Font Size" + menuItemAcceleratorSuffix(FontSizeModule.HOTKEY_INCREASE_FONT)
            increaseFontMenuItem.accelerator = FontSizeModule.HOTKEY_INCREASE_FONT

            val decreaseFontMenuItem = MenuItem(viewMenu, SWT.PUSH)
            decreaseFontMenuItem.text =
                "Decrease Font Size" + menuItemAcceleratorSuffix(FontSizeModule.HOTKEY_DECREASE_FONT)
            decreaseFontMenuItem.accelerator = FontSizeModule.HOTKEY_DECREASE_FONT

            // Settings
            val settingsWrapper = Composite(shell, SWT.NONE)
            settingsWrapper.layoutData = GridData(SWT.FILL, SWT.FILL, true, false, 2, 1)
            settingsWrapper.layout = GridLayout(1, false)

            // Page navigataton panel
            val pageNavPanel = Composite(settingsWrapper, SWT.NONE)
            pageNavPanel.layoutData = GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1)
            var rowLayout = RowLayout()
            rowLayout.center = true
            pageNavPanel.layout = rowLayout

            curPage =
                FormUIUtils.makeText(pageNavPanel).rowDataWidth(FormUIUtils.calcAverageCharWidth(shell) * 5).get()

            val pageOf = Label(pageNavPanel, SWT.NONE)
            pageOf.text = "of"

            val pageTotal = Label(pageNavPanel, SWT.NONE)

            previousPageButton = Button(pageNavPanel, SWT.NONE)
            previousPageButton.text = "Previous Page"

            nextPageButton = Button(pageNavPanel, SWT.NONE)
            nextPageButton.text = "Next Page"

            val printPageButton = Button(pageNavPanel, SWT.RADIO)
            printPageButton.text = "Print Page Number"
            if (pageType == "printPage") printPageButton.selection = true

            brlPageButton = Button(pageNavPanel, SWT.RADIO)
            brlPageButton.text = "Braille Page Number"
            if (pageType == "braillePage") brlPageButton.selection = true

            rawPageButton = Button(pageNavPanel, SWT.RADIO)
            rawPageButton.text = "Ordinal Page Number"
            if (pageType == "rawPage") rawPageButton.selection = true

            // Find panel
            val findPanel = Composite(settingsWrapper, SWT.NONE)
            findPanel.layoutData = GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1)
            rowLayout = RowLayout()
            rowLayout.center = true
            findPanel.layout = rowLayout

            FormUIUtils.newLabel(findPanel, "Find:")

            searchText =
                FormUIUtils.makeText(findPanel).rowDataWidth(FormUIUtils.calcAverageCharWidth(shell) * 20).get()

            val searchPrev = Button(findPanel, SWT.NONE)
            searchPrev.text = "Previous"
            searchPrev.toolTipText = "Search Previous"

            val searchNext = Button(findPanel, SWT.NONE)
            searchNext.text = "Next"
            searchNext.toolTipText = "Search Next"

            searchSixKey = Button(findPanel, SWT.CHECK)
            searchSixKey.text = "Six Key"
            searchSixKey.selection =
                BBIni.propertyFileManager.getPropertyAsBoolean(SETTINGS_KEY_SEARCH_SIX_KEY, true)

            searchSixKeyUppercase = Button(findPanel, SWT.CHECK)
            searchSixKeyUppercase.text = "Uppercase"

            viewLeft = newView(shell)
            viewRight = newView(shell)
            viewColorDefault = viewLeft.background
            viewColorDisabled = Display.getCurrent().getSystemColor(SWT.COLOR_GRAY)


            //status bar
            ppStatusBar = PPStatusBar(shell)


            // ------------------ Listeners -------------------
            dualViewMenuItem.selection = userSettings.getProperty(SETTINGS_KEY_DUAL_VIEW, "true").toBoolean()
            singleViewMode = SingleViewMode()
            dualViewMode = DualViewMode()
            currentViewMode = if (dualViewMenuItem.selection) dualViewMode else singleViewMode

            shell.addTraverseListener { e: TraverseEvent ->
                if (e.keyCode == SWT.ESC.code) close()
            }
            shell.addListener(SWT.Close) { close() }

            curPage.addTraverseListener { e: TraverseEvent ->
                // Only trigger on enter key
                if (e.detail != SWT.TRAVERSE_RETURN) return@addTraverseListener

                val raw = curPage.text
                val offset: Int
                if (rawPageButton.selection) {
                    val requestedPage: Int
                    try {
                        requestedPage = raw.toInt()
                    } catch (_: NumberFormatException) {
                        showMessage("$raw is not a number")
                        return@addTraverseListener
                    }
                    if (requestedPage > pageStartOffsets.size) {
                        showMessage("Page $requestedPage is too big")
                        return@addTraverseListener
                    }

                    // Get the line and scroll to it (requested page is 1 based)
                    offset = pageStartOffsets[requestedPage - 1]
                } else if (brlPageButton.selection) {
                    val result =
                        brlPageOffsets.filterKeys { key -> key.equals(raw, ignoreCase = true) }.values.firstOrNull()
                    if (result == null) {
                        showMessage("Braille page {} does not exist", raw)
                        return@addTraverseListener
                    }
                    offset = result
                } else {
                    // Print page
                    val result =
                        printPageOffsets.filterKeys { key -> key.equals(raw, ignoreCase = true) }.values.firstOrNull()
                    if (result == null) {
                        showMessage("Print page {} does not exist", raw)
                        return@addTraverseListener
                    }
                    offset = result
                }
                log.info("current view {}", currentViewMode.javaClass)
                currentViewMode.goToOffset(offset)
            }
            // emboss
            FormUIUtils.addSelectionListener(embossMenuItem) { fileEmbossNow() }

            FormUIUtils.addSelectionListener(nextPageButton) { jumpToAdjacentPage(1) }
            FormUIUtils.addSelectionListener(previousPageButton) { jumpToAdjacentPage(-1) }
            FormUIUtils.addSelectionListener(brlPageButton) { updateCurrentPage() }
            FormUIUtils.addSelectionListener(printPageButton) { updateCurrentPage() }
            FormUIUtils.addSelectionListener(rawPageButton) { updateCurrentPage() }

            FormUIUtils.addSelectionListener(
                brlPageButton
            ) { BBIni.propertyFileManager.save(SETTINGS_KEY_PAGE_TYPE, "braillePage") }
            FormUIUtils.addSelectionListener(
                printPageButton
            ) { BBIni.propertyFileManager.save(SETTINGS_KEY_PAGE_TYPE, "printPage") }
            FormUIUtils.addSelectionListener(
                rawPageButton
            ) { BBIni.propertyFileManager.save(SETTINGS_KEY_PAGE_TYPE, "rawPage") }

            FormUIUtils.addSelectionListener(dualViewMenuItem) {
                BBIni.propertyFileManager.save(
                    SETTINGS_KEY_DUAL_VIEW,
                    dualViewMenuItem.selection.toString()
                )
                currentViewMode = if (dualViewMenuItem.selection) {
                    dualViewMode
                } else {
                    singleViewMode
                }
                currentViewMode.onModeChange()

                currentViewMode.onGenerate()

                shell.size = shell.computeSize(SWT.DEFAULT, SWT.DEFAULT)
                shell.pack(true)
            }

            FormUIUtils.addSelectionListener(searchPrev) { searchPrev() }
            FormUIUtils.addSelectionListener(searchNext) { searchNext() }
            val skh = SixKeyHandler(null, null, searchSixKey.selection)
            searchText.addKeyListener(skh)
            searchText.addVerifyListener { event: VerifyEvent? -> skh.verifyKey(event!!) }
            FormUIUtils.addSelectionListener(searchSixKey) {
                val sixKeyMode = searchSixKey.selection
                BBIni.propertyFileManager.saveAsBoolean(SETTINGS_KEY_SEARCH_SIX_KEY, sixKeyMode)
                skh.sixKeyMode = sixKeyMode
                searchText.text = ""
            }

            FormUIUtils.addSelectionListener(unicodeMenuItem) {
                val isUnicodeSelected = unicodeMenuItem.selection
                BBIni.propertyFileManager.save(SETTINGS_KEY_UNICODE, isUnicodeSelected.toString())
                try {
                    WorkingDialog(
                        "Re-generating BRF with unicode " + (if (isUnicodeSelected) "enabled" else "disabled")
                    ).use {
                        generateOutput()
                        shell.setActive()
                    }
                } catch (ex: Exception) {
                    throw RuntimeException("Failed to refresh view, unicode $isUnicodeSelected", ex)
                }
            }

            viewLeft.addCaretListener { updateCurrentPage() }
            FormUIUtils.addSelectionListener(viewLeft.verticalBar) { updateCurrentPage() }

            // Font
            FormUIUtils.addSelectionListener(increaseFontMenuItem) { updateFont(true) }
            FormUIUtils.addSelectionListener(decreaseFontMenuItem) { updateFont(false) }

            // ------------------ Data ------------------------
            unicodeMenuItem.selection = userSettings.getProperty(SETTINGS_KEY_UNICODE, "false").toBoolean()

            // Dump BRF into string
            try {
                generateOutput()
            } catch (e: Exception) {
                wait.finished()
                throw RuntimeException("Failed to write brf", e)
            }

            // Update page nav
            curPage.text = "1"
            pageTotal.text = "" + pageStartOffsets.size

            if (docData is BrfDocumentData) {
                rawPageButton.selection = true
                brlPageButton.isEnabled = false
                printPageButton.isEnabled = false
                unicodeMenuItem.isEnabled = false
            }

            currentViewMode.onModeChange()
            if (viewRight.isVisible || viewLeft.background === viewColorDisabled) {
                viewRight.setFocus()
            } else {
                viewLeft.setFocus()
            }

            FormUIUtils.setLargeDialogSize(shell)
            shell.open()
        }
    }

    private fun updateFont(increase: Boolean) {
        if (increase) {
            if (fontManager != null) {
                fontManager.increaseFont()
            } else {
                increaseFontSetting()
            }
        } else {
            if (fontManager != null) {
                fontManager.decreaseFont()
            } else {
                decreaseFontSetting()
            }
        }
        setFontsInView()

        FormUIUtils.setLargeDialogSize(shell)
    }

    private fun setFontsInView() {
        val curFont = if (fontManager != null) {
            if (isShowBraille) fontManager.m.brailleView.font
            else fontManager.m.textView.font
        } else {
            if (isShowBraille) newBrailleFont() else newTextFont()
        }
        viewLeft.font = curFont
        viewRight.font = curFont
    }

    private fun newView(shell: Shell?): StyledText {
        val view = StyledText(shell, SWT.BORDER or SWT.H_SCROLL or SWT.V_SCROLL or SWT.READ_ONLY)
        view.alwaysShowScrollBars = false
        view.alwaysShowScrollBars = false
        view.setWordWrap(false)
        view.editable = false
        val viewData = GridData()
        viewData.horizontalAlignment = GridData.FILL
        viewData.verticalAlignment = GridData.FILL
        viewData.grabExcessHorizontalSpace = true
        viewData.grabExcessVerticalSpace = true
        view.layoutData = viewData

        val marginH = FormUIUtils.calcAverageCharHeight(shell) * MARGIN_LINES
        val marginW = FormUIUtils.calcAverageCharWidth(shell) * MARGIN_CELLS
        view.setMargins(marginW, marginH, marginW, marginH)

        view.addVerifyKeyListener { e: VerifyEvent ->
            if (e.stateMask == SWT.MOD1 && e.keyCode == 'q'.code) {
                e.doit = false
                close()
            } else if (e.keyCode == SWT.PAGE_UP) {
                e.doit = false
                log.debug("Triggered on page up")
                jumpToAdjacentPage(-1)
            } else if (e.keyCode == SWT.PAGE_DOWN) {
                e.doit = false
                log.debug("Triggered on page down")
                jumpToAdjacentPage(1)
            }
        }

        view.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent) {
                if (view.background !== viewColorDisabled) {
                    log.debug("Switching view")
                    lastFocusedView = view
                }
            }
        })

        view.addCaretListener {
            // TODO Auto-generated method stub
            updatePPStatusBar()
        }

        // Map scrolling to page up/down
        view.addMouseWheelListener { e: MouseEvent ->
            /*
			Absolutely no idea why we get a MouseWheel Event when nothing happened, but...
			Exception #97 reported 2017-08-09 04:48:16
			*/
            if (e.count == 0) {
                return@addMouseWheelListener
            }
            jumpToAdjacentPage(e.count * -1)
        }

        return view
    }

    abstract inner class ViewMode {
        protected fun getTotalOffset(pageIndex: Int): Int {
            val currentPageIndex = max(pageIndex, 0) // handle -1 page

            return pageRanges[currentPageIndex].lowerEndpoint()
        }

        abstract val totalOffset: Int

        abstract fun searchGetStart(): Int

        abstract fun seachSetCaret(foundOffset: Int, needle: String)

        abstract fun onGenerate()

        abstract fun onModeChange()

        abstract fun goToOffset(totalOffset: Int)

        protected fun getPageRangeIndexFromOffset(totalOffset: Int): Int {
            val index = pageRanges.indexOfFirst { totalOffset < it.upperEndpoint() }
            if (index >= 0) {
                return index
            } else {
                throw RuntimeException("Offset " + totalOffset + " not found, total: " + brfOutput.length)
            }
        }

        abstract fun doJumpToAdjacentPage(direction: Int)
    }

    private inner class DualViewMode : ViewMode() {
        private var pageRangeIndex = 0
        private var leftPageIndex = 0
        var leftPageRange: Range<Int>? = null
        var rightPageRange: Range<Int>? = null

        override val totalOffset: Int
            get() = getTotalOffset(leftPageIndex)

        override fun searchGetStart(): Int {
            val startIndex: Int
            if (lastFocusedView === viewLeft) {
                startIndex = leftPageRange!!.lowerEndpoint() + viewLeft.caretOffset
                log.trace("getting left caret {}", startIndex)
            } else if (lastFocusedView === viewRight) {
                startIndex = rightPageRange!!.lowerEndpoint() + viewRight.caretOffset
                log.trace("getting right caret {}", startIndex)
            } else if (leftPageRange != null) {
                // Need to check for null as could be first page which is on right
                startIndex = leftPageRange!!.lowerEndpoint()
                log.trace("getting start of left {}", startIndex)
            } else {
                startIndex = rightPageRange!!.lowerEndpoint()
                log.trace("getting start of right {}", startIndex)
            }
            return startIndex
        }

        override fun seachSetCaret(foundOffset: Int, needle: String) {
            if (leftPageRange != null && leftPageRange!!.contains(foundOffset)) {
                viewLeft.setSelectionRange(foundOffset - leftPageRange!!.lowerEndpoint(), needle.length)
                viewLeft.setFocus()
            } else if (rightPageRange != null && rightPageRange!!.contains(foundOffset)) {
                viewRight.setSelectionRange(foundOffset - rightPageRange!!.lowerEndpoint(), needle.length)
                viewRight.setFocus()
            } else {
                throw RuntimeException(
                    "Unable to set selection to " + foundOffset + " with " + leftPageRange
                            + " to " + rightPageRange
                )
            }
        }

        fun onDualViewPageChange() {
            log.debug("Page index updated to {}", pageRangeIndex)
            leftPageIndex = pageRangeIndex

            if (pageRangeIndex % 2 == 0) {
                pageRangeIndex--
                log.warn("Increasing even page range")
            }

            if (pageRangeIndex != -1) {
                leftPageRange = pageRanges[pageRangeIndex]
                viewLeft.text =
                    brfOutput.substring(leftPageRange!!.lowerEndpoint(), leftPageRange!!.upperEndpoint())
                viewLeft.background = viewColorDefault
            } else {
                leftPageRange = null
                viewLeft.text = ""
                viewLeft.background = viewColorDisabled
            }

            if (pageRangeIndex + 1 <= pageRanges.size - 1) {
                rightPageRange = pageRanges[pageRangeIndex + 1]
                viewRight.text =
                    brfOutput.substring(rightPageRange!!.lowerEndpoint(), rightPageRange!!.upperEndpoint())
                viewRight.background = viewColorDefault
            } else {
                rightPageRange = null
                viewRight.text = ""
                viewRight.background = viewColorDisabled
            }

            // Keep cursor following logical page order
            if (viewLeft.background !== viewColorDisabled) {
                viewLeft.setFocus()
            } else {
                viewRight.setFocus()
            }

            nextPageButton.isEnabled =
                pageRangeIndex < pageRanges.size -  /* to 0-index */1 -  /* ignore second panel */1
            previousPageButton.isEnabled = pageRangeIndex > 0

            updateCurrentPage()
        }

        override fun onGenerate() {
            onDualViewPageChange()
        }

        override fun onModeChange() {
            viewLeft.styleRanges = arrayOfNulls(0)
            viewRight.styleRanges = arrayOfNulls(0)

            (viewRight.layoutData as GridData).exclude = false
            viewRight.isVisible = true
            (viewLeft.layoutData as GridData).horizontalSpan = 1
        }

        override fun goToOffset(totalOffset: Int) {
            pageRangeIndex = getPageRangeIndexFromOffset(totalOffset)
            onDualViewPageChange()
        }

        override fun doJumpToAdjacentPage(direction: Int) {
            if (pageRangeIndex <= 0 && direction < 0) {
                // On first page and trying to go to beginning
                viewRight.caretOffset = 0
                viewRight.forceFocus()
            } else if (pageRangeIndex >= pageRanges.size -  /* to 0-index */1 -  /* ignore second panel */1
                && direction > 0
            ) {
                if (viewRight.background === viewColorDisabled) {
                    viewLeft.caretOffset = 0
                    viewLeft.forceFocus()
                } else {
                    viewRight.caretOffset = 0
                    viewRight.forceFocus()
                }
            } else if (direction < 0) {
                pageRangeIndex -= 2
                onDualViewPageChange()
            } else if (direction > 0) {
                pageRangeIndex += 2
                onDualViewPageChange()
            }
        }
    }

    inner class SingleViewMode : ViewMode() {
        private var pageRangeIndex = 0
        private var pageRange: Range<Int>? = null

        override val totalOffset: Int
            get() = getTotalOffset(pageRangeIndex)

        override fun searchGetStart(): Int {
            return pageRange!!.lowerEndpoint() + viewLeft.caretOffset
        }

        override fun seachSetCaret(foundOffset: Int, needle: String) {
            if (!pageRange!!.contains(foundOffset)) {
                throw RuntimeException("Unable to set selection to $foundOffset with $pageRange")
            }
            viewLeft.setSelectionRange(foundOffset - pageRange!!.lowerEndpoint(), needle.length)
            viewLeft.setFocus()
        }

        private fun onSingleViewPageChange() {
            log.debug("Page range index set to {}", pageRangeIndex)

            pageRange = pageRanges[pageRangeIndex]
            viewLeft.text = brfOutput.substring(pageRange!!.lowerEndpoint(), pageRange!!.upperEndpoint())

            nextPageButton.isEnabled = pageRangeIndex < pageRanges.size -  /* to 0-index */1
            previousPageButton.isEnabled = pageRangeIndex > 0

            viewLeft.setFocus()

            updateCurrentPage()
        }

        override fun onGenerate() {
            onSingleViewPageChange()
        }

        override fun onModeChange() {
            viewLeft.styleRanges = arrayOfNulls(0)
            viewRight.styleRanges = arrayOfNulls(0)

            (viewRight.layoutData as GridData).exclude = true
            viewRight.isVisible = false
            (viewLeft.layoutData as GridData).horizontalSpan = 2

            // Reset
            viewLeft.background = viewColorDefault
        }

        override fun goToOffset(totalOffset: Int) {
            pageRangeIndex = getPageRangeIndexFromOffset(totalOffset)
            onSingleViewPageChange()
        }

        override fun doJumpToAdjacentPage(direction: Int) {
            if (pageRangeIndex <= 0 && direction < 0) {
                // On first page and trying to go to beginning
                viewLeft.caretOffset = 0
                viewLeft.forceFocus()
            } else if (pageRangeIndex >= pageRanges.size -  /* to 0-index */1 && direction > 0) {
                // on last page
                viewLeft.caretOffset = 0
                viewLeft.forceFocus()
            } else if (direction < 0) {
                pageRangeIndex -= 1
                onSingleViewPageChange()
            } else if (direction > 0) {
                pageRangeIndex += 1
                onSingleViewPageChange()
            }
        }
    }

    private fun jumpToAdjacentPage(direction: Int) {
        require(direction != 0) { "direction == 0" }

        // Find current page
        currentViewMode.doJumpToAdjacentPage(direction)
        updateCurrentPage()
    }

    fun updateCurrentPage() {
        val offset: Int = currentViewMode.totalOffset

        // When we pass the offset we just entered the current page
        if (rawPageButton.selection) {
            for (curPageRange in pageRanges) {
                if (offset < curPageRange.upperEndpoint()) {
                    val pageNum = pageRanges.indexOf(curPageRange)
                    curPage.text = "" + (pageNum + 1)
                    break
                }
            }
        } else {
            // Find the offset relating to the end of the current page.
            val pageEnd = pageRanges.stream().filter { e: Range<Int> -> e.contains(offset) }.findFirst()
                .map { obj: Range<Int> -> obj.upperEndpoint() }
                .orElse(offset)
            val lookup: Map<String, Int> = if (brlPageButton.selection) brlPageOffsets else printPageOffsets
            // Find the last page before the end of the current page.
            lookup.entries.stream().filter { e: Map.Entry<String, Int> -> e.value <= pageEnd }
                .reduce { _: Map.Entry<String, Int>?, b: Map.Entry<String, Int>? -> b }
                .ifPresent { e: Map.Entry<String, Int> -> curPage.text = e.key }
        }

        updatePPStatusBar()
    }

    @Throws(IOException::class)
    private fun generateOutput() {
        setFontsInView()

        if (docData is BrfDocumentData) {
            generateOutputBrf(docData)
        } else {
            generateOutputDoc(docData as BBDocumentData)
        }
    }

    @Throws(IOException::class)
    private fun generateOutputDoc(docData: BBDocumentData) {
        log.info("Dumping dom to BRF")
        pageStartOffsets.clear()
        brlPageOffsets.clear()
        printPageOffsets.clear()
        pageRanges.clear()
        // First page starts at offset 0
        pageStartOffsets.add(0)

        // Load settings

        // Lambdas...
        val output = StringBuilder()
        //		uncomment the lines below to add margin to printpreview on bbx file
        docData.utdManager.engine.toBRF(
            docData.document, { onChar: Char ->
                if (onChar == BRFWriter.PAGE_SEPARATOR) {
                    val lastPageStart = pageStartOffsets[pageStartOffsets.size - 1]
                    // Don't include page seperator text in ranges so dual view doesn't have to
                    // strip them out
                    val lastPageEnd = output.length
                    pageRanges.add(Range.closed(lastPageStart, lastPageEnd))
                    pageStartOffsets.add(output.length)
                    //				setMarginTopLines(output, addMargin);
//				setMarginLeftCells(output, addMargin);
//			} else if (onChar == BRFWriter.NEWLINE) {
//				output.append(BRFWriter.NEWLINE);
//				setMarginLeftCells(output, addMargin);
                } else {
                    output.append(onChar)
                }
            }, if (unicodeMenuItem.selection) BRFWriter.OPTS_OUTPUT_UNICODE else BRFWriter.OPTS_DEFAULT,
            object : PageListener {
                override fun onBrlPageNum(brlPageBraille: String, brlPageOrig: String) {
                    log.trace("At length {} braille page {}", output.length, brlPageOrig)
                    brlPageOffsets[brlPageOrig] = output.length
                }

                override fun onPrintPageNum(printPageBraille: String, printPageOrig: String) {
                    log.trace(
                        "At length {} print braille {} orig {}", output.length, printPageBraille,
                        printPageOrig
                    )
                    printPageOffsets[printPageOrig] = output.length
                }
            })

        // Remove EOF page sep as its confusing to the user and logic
        pageStartOffsets.removeAt(pageStartOffsets.size - 1)

        brfOutput = output.toString()

        currentViewMode.onGenerate()

        searchSixKeyUppercase.selection = false
        searchSixKeyUppercase.isEnabled = false
    }

    @Throws(IOException::class)
    private fun generateOutputBrf(docData: BrfDocumentData) {
        log.info("Parsing brf file")
        pageStartOffsets.clear()
        brlPageOffsets.clear()
        printPageOffsets.clear()
        pageRanges.clear()

        brfOutput = docData.brfFile.readText(Charsets.UTF_8)
        //		uncomment the two lines below to add margin to printpreview on brf file
        var pageStart = 0
        var pageEnd: Int
        while ((brfOutput.indexOf(BRFWriter.PAGE_SEPARATOR, pageStart).also { pageEnd = it }) != -1) {
            pageRanges.add(Range.closed(pageStart, pageEnd))
            pageStart = pageEnd + 1
            pageStartOffsets.add(pageStart)
        }


        // Issue #6469: Single page files may not have any at all
        if (pageStartOffsets.isEmpty()) {
            pageStartOffsets.add(0)
            pageRanges.add(Range.closed(0, brfOutput.length))
        }


        // Add start at 0, liblouis files have this
        if (pageStartOffsets[0] != 0) {
            pageStartOffsets.add(0, 0)
        }

        currentViewMode.onGenerate()

        // Not 100% reliable but better than nothing
        var uppercaseMode: Boolean? = null
        for (element in brfOutput) {
            when (element) {
                '\r', '\n' -> continue
            }
            if (BrailleUnicodeConverter.UPPERCASE_ASCII_BRAILLE.indexOf(element) == -1) {
                log.info("Uppercase mode failed on character '{}' {}", element, Character.getName(element.code))
                uppercaseMode = false
                break
            } else if (BrailleUnicodeConverter.LOWERCASE_ASCII_BRAILLE.indexOf(element) == -1) {
                log.info("Lowercase mode failed on character '{}' {}", element, Character.getName(element.code))
                uppercaseMode = true
                break
            }
        }
        if (uppercaseMode == null) {
            // matches both?! Default to liblouis
            log.error("No mode failed, defaulting to liblouis")
            uppercaseMode = false
        }
        searchSixKeyUppercase.selection = uppercaseMode
    }

    private fun searchNext() {
        search(
            searchNext = true, nested = false, startIndex = currentViewMode.searchGetStart()
        ) { needle: String?, startIndex: Int ->
            brfOutput.indexOf(
                needle!!,
                startIndex +  /* don't match selection again */1
            )
        }
    }

    private fun searchPrev() {
        search(
            searchNext = false, nested = false, startIndex = currentViewMode.searchGetStart()
        ) { needle: String?, startIndex: Int ->
            brfOutput
                .substring(0, if (startIndex <= 0) 0 else (startIndex -  /* don't match selection again */1))
                .lastIndexOf(needle!!)
        }
    }

    private fun search(
        searchNext: Boolean, nested: Boolean, startIndex: Int,
        startIndexToFoundOffset: BiFunction<String, Int, Int>
    ) {
        var needle = searchText.text
        if (needle.isEmpty()) {
            throw BBNotifyException("No search text entered")
        }
        if (searchSixKey.selection) {
            // re-encode to ascii range
            val oldNeedle = needle
            needle = if (searchSixKeyUppercase.selection) {
                unicodeToAsciiUppercase(needle)
            } else {
                unicodeToAsciiLouis(needle)
            }
            log.info("Mapped six key input '{}' to '{}'", oldNeedle, needle)
        } else {
            val charMap = needle.toCharArray()
            for (i in charMap.indices) {
                val c = charMap[i]
                var index = 0
                if (searchSixKeyUppercase.selection
                    && (BrailleUnicodeConverter.LOWERCASE_ASCII_BRAILLE.indexOf(c).also { index = it }) != -1
                ) {
                    charMap[i] = BrailleUnicodeConverter.UPPERCASE_ASCII_BRAILLE[index]
                } else if (!searchSixKeyUppercase.selection
                    && (BrailleUnicodeConverter.UPPERCASE_ASCII_BRAILLE.indexOf(c).also { index = it }) != -1
                ) {
                    charMap[i] = BrailleUnicodeConverter.LOWERCASE_ASCII_BRAILLE[index]
                }
            }
            needle = String(charMap)
        }

        val foundOffset = startIndexToFoundOffset.apply(needle, startIndex)
        if (foundOffset == -1) {
            log.info("Text '{}' not found from startIndex {}", needle, startIndex)
            val newStartIndex = if (nested) {
                throw BBNotifyException("Not found")
            } else if (searchNext) {
                0
            } else {
                brfOutput.length
            }
            search(searchNext, true, newStartIndex, startIndexToFoundOffset)
            return
        }

        currentViewMode.goToOffset(foundOffset)
        currentViewMode.seachSetCaret(foundOffset, needle)
    }

    private fun fileEmbossNow() {
        if (docData is BBDocumentData) {
            emboss(m.document, m.document.engine, shell) { s: Shell? ->
                BrailleSettingsDialog(
                    s,
                    m,
                    EmbosserSettingsTab::class.java
                )
            }
        } else {
            // Compute the number of lines and cells per page.
            // This is reliant on the first page of the brf being representative of the rest of the document.
            val newline = System.lineSeparator()
            //Split the first page of text into lines, then find the longest line.
            val firstPage = brfOutput.substring(0, pageStartOffsets[1])
            val lines = firstPage.split(newline)
            val longestLine = lines.maxByOrNull { it.length }
            //I'm assuming there's a check somewhere along the way to prevent embossing a blank brf.
            val cellsPerLine = longestLine!!.length
            val linesPerPage = lines.size - 1

            //Now what multipliers do we need to get the paper size based on char sizes?
            //Now use the current braille cell size to get the paper size - have to rely on user settings here.
            val cellType = m.document.engine.brailleSettings.cellType
            val minPaperHeight =
                (BigDecimal.valueOf(linesPerPage.toDouble()) * cellType.height).setScale(2, RoundingMode.HALF_UP)
            val minPaperWidth =
                (BigDecimal.valueOf(cellsPerLine.toDouble()) * cellType.width).setScale(2, RoundingMode.HALF_UP)
            //Ta-da, we have the paper size for the document, though without any margins factored in.
            //Really should have additional embossing options for those.
            log.info("BRF minimum paper size (mm): $minPaperWidth x $minPaperHeight")

            embossBrf(
                minPaperHeight,
                minPaperWidth,
                (docData as BrfDocumentData).brfFile,
                m.document.engine,
                shell
            ) { s: Shell? -> BrailleSettingsDialog(s, m, EmbosserSettingsTab::class.java) }
        }
    }

    private fun close() {
        shell.dispose()
    }

    private fun updatePPStatusBar() {
        if (ppStatusBar.statusBar.text != getPPStatusBarMessage(false)) {
            ppStatusBar.setText(getPPStatusBarMessage(false))
            if (lastFocusedView != null) setName(lastFocusedView!!, getPPStatusBarMessage(true))
        }
    }

    private fun getPPStatusBarMessage(onlyPage: Boolean): String {
        var message = ""
        val page = curPage.text
        message += "Page $page"
        if (lastFocusedView != null && !onlyPage) {
            val line = lastFocusedView!!.getLineAtOffset(lastFocusedView!!.caretOffset) + 1
            val cellNumber = lastFocusedView!!.caretOffset - lastFocusedView!!.getOffsetAtLine(line - 1) + 1
            message += " | Line $line | Cell Number $cellNumber"
        }
        return message
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(PrintPreview::class.java)
        private const val SETTINGS_KEY_DUAL_VIEW = "printpreview.dualView"
        private const val SETTINGS_KEY_PAGE_TYPE = "printpreview.pageType"
        private const val SETTINGS_KEY_UNICODE = "printpreview.unicode"
        private const val SETTINGS_KEY_SEARCH_SIX_KEY = "printpreview.searchSixKey"
        private const val MARGIN_LINES = 2
        private const val MARGIN_CELLS = 3
    }
}

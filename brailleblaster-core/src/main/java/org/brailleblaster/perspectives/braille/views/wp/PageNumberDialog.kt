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

import nu.xom.Attribute
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.BBIni
import org.brailleblaster.bbx.BBX
import org.brailleblaster.frontmatter.VolumeUtils.getVolumeElements
import org.brailleblaster.frontmatter.VolumeUtils.getVolumeNames
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.ui.BBStyleableText
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.SharedItem
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.search.GoToPageDialog
import org.brailleblaster.tools.MenuToolModule
import org.brailleblaster.utd.MetadataHelper.changeBraillePageNumber
import org.brailleblaster.utd.MetadataHelper.changePrintPageNumber
import org.brailleblaster.utd.MetadataHelper.getUTDMeta
import org.brailleblaster.utd.MetadataHelper.markBlankPrintPageNumber
import org.brailleblaster.utd.properties.EmphasisType
import org.brailleblaster.utd.properties.PageNumberType
import org.brailleblaster.utd.utils.TextTranslator
import org.brailleblaster.exceptions.BBNotifyException
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.utils.findCurrentVolumeNumber
import org.brailleblaster.utd.utils.getDocumentHead
import org.brailleblaster.utils.swt.EasySWT
import org.brailleblaster.utils.swt.EasyListeners
import org.brailleblaster.wordprocessor.WPManager
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.ScrolledComposite
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.custom.TableEditor
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.layout.RowData
import org.eclipse.swt.layout.RowLayout
import org.eclipse.swt.widgets.*

class PageNumberDialog(parent: Shell?) : Dialog(parent, SWT.NONE), MenuToolModule {
    private var pageChangeListTab: PageChangeListTab? = null
    private var shell: Shell? = null
    private var folder: TabFolder? = null
    private var manager: Manager? = null
    private var startingBPN: String? = null
    private var startingContLetter: String? = null
    private var startingPPN: String? = null
    private var ppIndicator: Element? = null
    private var ppnContinuationText: org.eclipse.swt.widgets.Text? = null
    private var ppnText: org.eclipse.swt.widgets.Text? = null
    private var bpnText: org.eclipse.swt.widgets.Text? = null
    private var ppiText: BBStyleableText? = null
    private var pageType: String? = null
    private var volumeLocation: String? = null

    // private Button ppnCombine;
    private var ppiCombine: Button? = null
    private var ppiImply: Button? = null
    private var okButton: Button? = null
    private var deleteButton: Button? = null
    private var yesHead: Button? = null
    private var noHead: Button? = null
    var pageChangeComposite: Composite? = null
    var metaCounter = 0
    var deletedMeta: MutableList<Element> = ArrayList()
    override val title = MENU_NAME

    /*
     * This tab should contain all the information needed for adding a new
     * page change
     */
    private inner class PageChangeTab(folder: TabFolder?) {
        init {
            val tab = TabItem(folder, 0)
            // New page number changes?
            tab.text = "Edit Page Number"
            pageChangeComposite = Composite(folder, 0)
            pageChangeComposite!!.layout = GridLayout(1, true)
            tab.control = pageChangeComposite
            setGlobalValues()
            createTabsInPageChangeTab()
            addVolumeLocation(pageChangeComposite!!)
            addApplyOKAndCancelButton(pageChangeComposite!!)
            if (startingContLetter != null) ppnContinuationText!!.text = startingContLetter
            if (startingPPN != null) ppnText!!.text = startingPPN
            if (startingBPN != null) bpnText!!.text = startingBPN
            shell!!.layout()
        }

        private fun setGlobalValues() {
            val curText = manager!!.text.view
            val curLine = curText.getLineAtOffset(curText.caretOffset)
            setStartingBPN(manager!!.getCurrentBraillePageMapElement(curLine))
            setStartingPPN(manager!!.getCurrentPrintPageMapElement(curLine))
            ppIndicator = manager!!.findPreviousPrintPageIndicator()
        }

        private fun createTabsInPageChangeTab() {
            val folder = TabFolder(pageChangeComposite, SWT.NONE)
            PPIndicatorTab(folder)
            PPNumberTab(folder)
            PNTypeTab(folder)
            BPNumberTab(folder)
            RunningHeadTab(folder)
        }

        private inner class PPIndicatorTab(folder: TabFolder?) {
            init {
                val tab = TabItem(folder, 0)
                tab.text = "Print Page Indicator"
                val ppIndicatorTabComposite = Composite(folder, 0)
                ppIndicatorTabComposite.layout = GridLayout(1, true)
                tab.control = ppIndicatorTabComposite
                createContents(ppIndicatorTabComposite)
            }

            private fun createContents(composite: Composite) {
                addPrintPageIndicatorContainer(composite)
            }

            private fun addPrintPageIndicatorContainer(container: Composite) {
                if (ppIndicator != null) {
                    val ppiContainer = makeGroup(
                        container,
                        "Change Previous Print Page Indicator from " + ppIndicator!!.getChild(0).value, MAX_COLUMNS
                    )
                    insertTextBoxWithPrintPage(ppiContainer)
                    EasySWT.makePushButton(ppiContainer, "Direct", BUTTON_WIDTH, 1) {
                        // Add attribute to the block that indicates it needs to be
                        // direct translated
                        wrapTextInEmphasis(BBX.INLINE.EMPHASIS.create(EmphasisType.NO_TRANSLATE), ppiText!!.text)
                        manager!!.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, true, ppIndicator!!.parent))
                    }
                    EasySWT.makePushButton(ppiContainer, "Uncontracted", BUTTON_WIDTH, 1) {
                        wrapTextInEmphasis(BBX.INLINE.EMPHASIS.create(EmphasisType.NO_CONTRACT), ppiText!!.text)
                        manager!!.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, true, ppIndicator!!.parent))
                    }
                    EasyListeners.keyReleased(ppiText!!.text) { populateCombinedAndImpliedText() }
                    val radioGroup = makeGroup(ppiContainer, "Runover Page Numbers", MAX_COLUMNS)
                    EasySWT.buildGridData().setColumns(MAX_COLUMNS).applyTo(radioGroup)
                    createPushButtonForCombined(radioGroup)
                    createPushButtonForImplied(radioGroup)
                    if (ppIndicator!!.getAttribute("utd-pnOverride") != null) {
                        ppiImply!!.selection = true
                    } else {
                        ppiCombine!!.selection = true
                    }
                    if (ppiCombine!!.enabled) {
                        populateCombinedAndImpliedText()
                    }
                    val deletePageButton = EasySWT.makePushButton(
                        ppiContainer,
                        "Delete Indicator",
                        BUTTON_WIDTH,
                        1
                    ) {
                        ppiText!!.text.text = ""
                        changeNumbers()
                    }
                    EasySWT.addSwtBotKey(deletePageButton, SWTBOT_DELETE_PRINT_INDICATOR)
                }
            }

            private fun insertTextBoxWithPrintPage(ppiContainer: Group) {
                ppiText = BBStyleableText(ppiContainer, ppiContainer, 0, SWT.BORDER)
                ppiText!!.text.text = ppIndicator!!.getChild(0).value
                ppiText!!.text.data = ppIndicator
                ppiText!!.text.layoutData = GridData(SWT.FILL, SWT.FILL, true, false, 1, 1)
                EasySWT.addSwtBotKey(ppiText!!.text, SWTBOT_PRINT_PAGE_NUMBER_TEXT)
            }

            private fun createPushButtonForCombined(radioGroup: Group) {
                ppiCombine = EasySWT.buildPushButton(radioGroup).apply {
                    this.swtOptions = SWT.RADIO
                    this.text = "Combined"
                }.build().apply {
                    EasySWT.buildGridData().setColumns(MAX_COLUMNS).setAlign(SWT.FILL, SWT.CENTER).setGrabSpace(
                        horizontally = true,
                        vertically = false
                    )
                        .applyTo(this)
                    isEnabled = ppiText!!.text.text.contains("-")
                    pack()
                }
            }

            private fun createPushButtonForImplied(radioGroup: Group) {
                ppiImply = EasySWT.buildPushButton(radioGroup).apply {
                    this.swtOptions = SWT.RADIO
                    this.text = "Implied"
                }.build().apply {
                    EasySWT.buildGridData().setColumns(MAX_COLUMNS).setAlign(SWT.FILL, SWT.CENTER).setGrabSpace(
                        horizontally = true,
                        vertically = false
                    )
                        .applyTo(this)
                    isEnabled = ppiText!!.text.text.contains("-")
                    pack()
                }
            }

            private fun populateCombinedAndImpliedText() {
                val runover = findRunover(ppiText!!.text.text)
                if (runover.isNotEmpty()) {
                    ppiCombine!!.isEnabled = true
                    ppiImply!!.isEnabled = true
                    ppiCombine!!.text = "Combined: a" + ppiText!!.text.text + ", b" + ppiText!!.text.text + "..."
                    ppiImply!!.text = "Implied: a$runover, b$runover..."
                } else {
                    ppiCombine!!.isEnabled = false
                    ppiImply!!.isEnabled = false
                    ppiCombine!!.text = "Combined"
                    ppiImply!!.text = "Implied"
                }
                ppiCombine!!.pack(true)
                ppiImply!!.pack(true)
            }

            private fun wrapTextInEmphasis(emphasis: Element, text: StyledText) {
                var start = text.selectionRange.x
                var end = start + text.selectionRange.y
                val origText = text.text
                if (start == end) {
                    start = 0
                    end = origText.length
                }
                emphasis.appendChild(origText.substring(start, end))

                // Remove all the children of the current ppi and replace with these
                // new ones
                ppIndicator!!.removeChild(0)
                val startText = origText.take(start)
                if (startText.isNotEmpty()) {
                    ppIndicator!!.appendChild(startText)
                }
                ppIndicator!!.appendChild(emphasis)
                val endText = origText.substring(end)
                if (endText.isNotEmpty()) {
                    ppIndicator!!.appendChild(endText)
                }
            }
        }

        private inner class PPNumberTab(folder: TabFolder?) {
            init {
                val tab = TabItem(folder, 0)
                tab.text = "Print Page Number"
                val ppNumberTabComposite = Composite(folder, 0)
                ppNumberTabComposite.layout = GridLayout(1, true)
                tab.control = ppNumberTabComposite
                createContents(ppNumberTabComposite)
            }

            private fun createContents(composite: Composite) {
                addPrintPageNumberContainer(composite)
            }

            private fun addPrintPageNumberContainer(container: Composite) {
                val printPageContainer = makeGroup(
                    container, "Change Print Page Number from "
                            + if (startingContLetter == null) startingPPN else startingContLetter + startingPPN,
                    MAX_COLUMNS
                )
                // ppnCombine =
                // EasySWT.buildPushButton(printPageContainer).setSWTOptions(SWT.CHECK).setText("Combine
                // Pages").build();
                // EasySWT.buildGridData().setColumns(MAX_COLUMNS).setAlign(SWT.FILL,
                // SWT.BEGINNING).setGrabSpace(true, false).applyTo(ppnCombine);
                // EasySWT.EasyListeners.selection(ppnCombine, (e) -> {
                // ppnContinuationText.setEnabled(!ppnCombine.getSelection());
                // });
                EasySWT.makeLabel(printPageContainer, "Continuation Letter:", 1)
                ppnContinuationText =
                    EasySWT.makeText(printPageContainer, SMALL_BOX_WIDTH, 1) { changeNumbers() }
                EasySWT.makeLabel(printPageContainer, "Page Number:", 1)
                ppnText = EasySWT.makeText(printPageContainer, LARGE_BOX_WIDTH, 1) { changeNumbers() }
                EasySWT.makePushButton(printPageContainer, "Delete Page", BUTTON_WIDTH, 1) {
                    markBlankPrintPageNumber(
                        manager!!.document.doc,
                        startingContLetter + startingPPN,
                        volumeLocation,
                        false
                    )
                    manager!!.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, true, ppIndicator!!.parent))
                    metaCounter++
                }
            }
        }

        private inner class PNTypeTab(folder: TabFolder?) {
            init {
                val tab = TabItem(folder, 0)
                tab.text = "Page Number Type"
                val pnTypeTabComposite = Composite(folder, 0).apply {
                    layout = GridLayout(1, true)
                }
                tab.control = pnTypeTabComposite
                createContents(pnTypeTabComposite)
            }

            private fun createContents(composite: Composite) {
                EasySWT.makeLabel(composite, "Page Number Type:", 1)
                makePageTypeCombo(composite)
            }

            private fun makePageTypeCombo(parent: Composite) {
                val combo = Combo(parent, SWT.READ_ONLY)
                for (type in PageNumberType.entries) {
                    when (type) {
                        PageNumberType.P_PAGE -> combo.add("P-page")
                        PageNumberType.T_PAGE -> combo.add("T-page")
                        PageNumberType.NORMAL -> combo.add("Normal")
                    }
                }
                combo.addSelectionListener(object : SelectionAdapter() {
                    override fun widgetSelected(e: SelectionEvent) {
                        pageType = when (combo.text) {
                            "P-page" -> "P_PAGE"
                            "T-page" -> "T_PAGE"
                            "Normal" -> "NORMAL"
                            else -> combo.text
                        }
                    }
                })
            }
        }

        private inner class BPNumberTab(folder: TabFolder?) {
            init {
                val tab = TabItem(folder, 0)
                tab.text = "Braille Page Number"
                val bpNumberTab = Composite(folder, 0).apply {
                    layout = GridLayout(1, true)
                }
                tab.control = bpNumberTab
                createContents(bpNumberTab)
            }

            private fun createContents(composite: Composite) {
                addBraillePageContainer(composite)
                toggleRunningHeadContainer(composite)
            }

            private fun addBraillePageContainer(container: Composite) {
                val braillePageContainer = makeGroup(
                    container, "Change Braille Page Number from $startingBPN",
                    MAX_COLUMNS - 1
                )
                EasySWT.makeLabel(braillePageContainer, "Page Number:", 1)
                bpnText = EasySWT.makeText(
                    braillePageContainer,
                    LARGE_BOX_WIDTH,
                    MAX_COLUMNS - 2
                ) { changeNumbers() }
            }

            //The default is yes
            private fun toggleRunningHeadContainer(container: Composite) {
                val runHeadContainer = makeGroup(
                    container, "Running Head on page $startingBPN",
                    MAX_COLUMNS - 1
                )
                yesHead = Button(runHeadContainer, SWT.RADIO)
                yesHead!!.text = "Yes"
                yesHead!!.selection = true
                noHead = Button(runHeadContainer, SWT.RADIO)
                noHead!!.text = "No"
            }
        }

        private inner class RunningHeadTab(folder: TabFolder?) {
            init {
                val tab = TabItem(folder, 0)
                tab.text = "Running Head"
                val runningHeadTab = Composite(folder, 0).apply {
                    layout = GridLayout(1, true)
                }
                tab.control = runningHeadTab
                createContents(runningHeadTab)
            }

            private fun createContents(composite: Composite) {
                toggleRunningHeadContainer(composite)
            }

            //The default is yes
            private fun toggleRunningHeadContainer(container: Composite) {
                val runHeadContainer = makeGroup(
                    container, "Running Head on page $startingBPN",
                    MAX_COLUMNS - 1
                )
                yesHead = Button(runHeadContainer, SWT.RADIO)
                yesHead!!.text = "Yes"
                yesHead!!.selection = true
                noHead = Button(runHeadContainer, SWT.RADIO)
                noHead!!.text = "No"
            }
        }

        private fun setStartingBPN(braillePageMapElement: Node?) {
            if (braillePageMapElement != null) {
                if ((braillePageMapElement as Element).getAttribute("untranslated") != null) {
                    startingBPN = braillePageMapElement.getAttributeValue("untranslated")
                }
            } else {
                startingBPN = ""
            }
        }

        private fun setStartingPPN(printPageMapElement: Node?) {
            if (printPageMapElement != null) {
                val ppnElement = printPageMapElement as Element
                val ppn = ppnElement.getAttributeValue("printPageBrl")
                val continuationLetter =
                    if (ppnElement.getAttribute("contLetter") != null) ppnElement.getAttributeValue("contLetter") else ""
                startingPPN = ppn
                startingContLetter = continuationLetter
            } else {
                startingPPN = ""
            }
        }

        private fun addApplyOKAndCancelButton(container: Composite) {
            val buttonComp = EasySWT.makeComposite(container, 4)
            makeSpace(buttonComp, 265, 0)

            val applyButton =
                EasySWT.makePushButton(buttonComp, "Apply", BUTTON_WIDTH, 1)
                { changeNumbers() }

            EasySWT.addSwtBotKey(applyButton, SWTBOT_APPLY_BUTTON)

            okButton = EasySWT.makePushButton(buttonComp, "Ok", BUTTON_WIDTH, 1)
            { changeAndClose() }.also {
                EasySWT.addSwtBotKey(it, SWTBOT_OK_BUTTON)
            }

            EasySWT.makePushButton(buttonComp, "Cancel", BUTTON_WIDTH, 1)
            { cancelPageChange() }
        }

        private fun changeNumbers() {
            handlePrintPageChanges()
            handleBraillePageChanges()
            val refresh = handlePrintPageIndicatorChanges()
            // Page type change needs to come after ppi change
            handlePageTypeChanges()
            if (refresh) {
                manager!!.simpleManager.dispatchEvent(
                    ModifyEvent(
                        Sender.NO_SENDER,
                        false,
                        manager!!.document.rootElement
                    )
                )
            }
            metaCounter++
        }

        private fun changeAndClose() {
            changeNumbers()
            metaCounter = 0
            deletedMeta = ArrayList()
            if (shell != null && !shell!!.isDisposed) {
                shell!!.close()
            }
        }

        private fun handlePrintPageChanges() {
            var newPPN = ppnText!!.text
            val newContLetter = if (ppnContinuationText!!.enabled) ppnContinuationText!!.text else ""
            if (startingPPN == null && startingBPN == null) {
                notifyNoExistingPrintPage()
            } else if (startingPPN != null && startingPPN != newPPN) {
                try {
                    // Is it a number?
                    newPPN.toInt()
                    newPPN = TextTranslator.translateText(newPPN, manager!!.document.engine)
                } catch (_: NumberFormatException) {
                }
                changePrintPageNumber(
                    manager!!.document.doc,
                    (if (startingContLetter == null) startingPPN else startingContLetter + startingPPN)!!, newPPN,
                    newContLetter, pageType, volumeLocation, false
                )
            } else if (startingPPN != null && (startingContLetter == null && newContLetter.isNotEmpty() || startingContLetter != null && startingContLetter != newContLetter)) {
                changePrintPageNumber(
                    manager!!.document.doc,
                    (if (startingContLetter == null) startingPPN else startingContLetter + startingPPN)!!, startingPPN,
                    newContLetter, pageType, volumeLocation, false
                )
            }
        }

        private fun notifyNoExistingPrintPage() {
            val messageText =
                "No existing print page. Insert a print page number using the Style->Miscellaneous->Page option."
            if (BBIni.debugging) {
                throw RuntimeException(messageText)
            }
            val message = MessageBox(WPManager.getInstance().shell)
            message.message = messageText
            message.open()
        }

        private fun handleBraillePageChanges() {
            val newBPN = bpnText!!.text
            if (startingBPN == null && newBPN.isNotEmpty() || startingBPN != null && startingBPN != newBPN || yesHead!!.selection || noHead!!.selection) {
                // Find the first element on the current page in the selection
                changeBraillePageNumber(
                    manager!!.document.doc, startingBPN!!, newBPN,
                    volumeLocation, false, !noHead!!.selection
                )
                volumeLocation = null
            }
        }

        private fun handlePrintPageIndicatorChanges(): Boolean {
            if (ppIndicator != null) {
                val ppiParent = ppIndicator!!.parent as Element?
                if (ppiParent != null && ppiText != null &&
                    (ppiText!!.text.text != ppIndicator!!.getChild(0).value
                            || ppiImply!!.selection && ppIndicator!!.getAttribute("utd-pnOverride") == null
                            || ppiCombine!!.selection && ppIndicator!!.getAttribute("utd-pnOverride") != null)
                ) {
                    //Deleted the print page indicator
                    if (ppiText!!.text.text.isEmpty()) {
                        ppiParent.removeChild(ppIndicator)
                    }
                    ppIndicator!!.getChild(0).detach()
                    ppIndicator!!.insertChild(ppiText!!.text.text, 0)
                    if (ppiImply!!.selection && ppiImply!!.enabled) {
                        ppIndicator!!.addAttribute(Attribute("utd-pnOverride", findRunover(ppiText!!.text.text)))
                    } else if (ppIndicator!!.getAttribute("utd-pnOverride") != null) {
                        ppIndicator!!.removeAttribute(ppIndicator!!.getAttribute("utd-pnOverride"))
                    }
                    if (pageType != null) {
                        ppIndicator!!.addAttribute(Attribute("pageType", pageType))
                        pageType = null
                    }
                    manager!!.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, true, ppiParent))
                    return false
                }
            }
            return true
        }

        private fun handlePageTypeChanges() {
            if (pageType != null) {
                if (pageType == "T_PAGE") {
                    //Find the selected part of the document

                    //In v2, all these nodes should block or a child of one
                    var startNode = manager!!.simpleManager.currentSelection.start.node
                    var endNode = manager!!.simpleManager.currentSelection.end.node
                    if (startNode is Text) {
                        startNode = getBlockParent(startNode)
                    }
                    if (endNode is Text) {
                        endNode = getBlockParent(endNode)
                    }

                    //Find all of the blocks that are between start and endNode, inclusive
                    val blocksBetween = getIntersectionInclusive(startNode, endNode)
                    //Add a new attribute that tells UTD what the page type should be when these nodes are on the page
                    for (block in blocksBetween) {
                        val blockE = block as Element
                        blockE.addAttribute(Attribute("pageType", "T_PAGE"))
                    }
                } else {
                    changePrintPageNumber(
                        manager!!.document.doc,
                        (if (startingContLetter == null) startingPPN else startingContLetter + startingPPN)!!,
                        null,
                        null,
                        pageType,
                        volumeLocation,
                        false
                    )
                }
                pageType = null
                volumeLocation = null
            }
        }

        //Delete the most recent page changes added to the document
        private fun cancelPageChange() {
            val head = getDocumentHead(manager!!.doc)
            if (head != null) {
                repeat(metaCounter) {
                    head.removeChild(head.childCount - 1)
                }
                refreshDocumentAndClose()
            }
        }

        private fun addVolumeLocation(parent: Composite) {
            val volumeGroup = makeGroup(parent, "", MAX_COLUMNS - 2)
            EasySWT.makeLabel(volumeGroup, "Volume Location:", 1)
            val volumeList = getVolumeElements(manager!!.doc)
            //Find a way to set the current volume based on current brl
            val currNode = manager!!.simpleManager.currentSelection.start.node
            val currVolNum =
                if (findCurrentVolumeNumber(currNode) > 0) findCurrentVolumeNumber(currNode) - 1 else 0
            if (volumeList.isNotEmpty()) {
                val volumeNames = getVolumeNames(volumeList)
                val location = EasySWT.makeLabel(
                    volumeGroup,
                    if (volumeNames.isNotEmpty()) volumeNames[currVolNum].nameLong else "",
                    1
                )
                volumeLocation = location.text
            }
        }
    }

    /*
     * This tab shows the list of page changes that have been made for the entire book
     */
    private inner class PageChangeListTab(folder: TabFolder?) {
        var metaDataTable: Table? = null
        var dialog: Composite
        var scrolledContainer: ScrolledComposite

        init {
            val tab = TabItem(folder, 0)
            // New page number changes?
            tab.text = "Page Change List"
            dialog = Composite(folder, 0)
            dialog.layout = GridLayout(1, true)
            tab.control = dialog
            scrolledContainer = ScrolledComposite(dialog, SWT.V_SCROLL or SWT.H_SCROLL or SWT.BORDER)
            scrolledContainer.layout = GridLayout(1, false)
            scrolledContainer.setSize(500, pageChangeComposite!!.bounds.height - 90)
            scrolledContainer.expandHorizontal = true
            scrolledContainer.showFocusedControl = true
            EasySWT.buildGridData().setAlign(SWT.FILL, SWT.FILL).setGrabSpace(horizontally = true, vertically = true)
                .applyTo(scrolledContainer)
            createContentsInPageChangeListTab()
        }

        fun createContentsInPageChangeListTab() {
            // Make a table for all the available metadata entries
            metaDataTable = Table(scrolledContainer, SWT.CHECK)
            metaDataTable!!.headerVisible = true
            createTableColumns()
            populateTable()
            packColumns()
            metaDataTable!!.size = scrolledContainer.size
            scrolledContainer.content = metaDataTable
            EasySWT.makePushButton(dialog, "Select All Pages", 1) {
                for (i in metaDataTable!!.items) {
                    i.checked = true
                }
            }
            makeSpace(dialog, 0, 15)
            addDeleteOKAndCancelButton()
        }

        private fun createTableColumns() {
            val tableHeaders = arrayOf("Type", "Original", "New", "Page Type", "Volume", "Go To")
            for (tableHeader in tableHeaders) {
                val tableColumn = TableColumn(metaDataTable, SWT.NULL)
                tableColumn.text = tableHeader
            }
        }

        private fun repopulateTable() {
            metaDataTable!!.removeAll()
            val children = metaDataTable!!.children
            for (child in children) {
                child.dispose()
            }
            populateTable()
        }

        private fun populateTable() {
            val metaDataList: List<Element> = metaDataList
            for (meta in metaDataList) {
                // Create new table item for every metadata available
                val item = TableItem(metaDataTable, SWT.NULL)

                // Metadata will always have type and original
                item.setText(0, getType(meta.getAttributeValue("type")))
                item.setText(1, meta.getAttributeValue("original"))
                if (meta.getAttribute("new") != null) {
                    var newPage = meta.getAttributeValue("new")
                    if (meta.getAttribute("cl") != null) {
                        newPage = meta.getAttributeValue("cl") + newPage
                    }
                    item.setText(2, newPage)
                }
                if (meta.getAttribute("pageType") != null) {
                    item.setText(3, meta.getAttributeValue("pageType"))
                }
                if (meta.getAttribute("pageVolume") != null) {
                    item.setText(4, meta.getAttributeValue("pageVolume"))
                }

                // Inserts go to button and adds listener to the button
                handleGoToButton(item)
            }
        }

        private fun handleGoToButton(item: TableItem) {
            manager!!.waitForFormatting(true)
            val editor = TableEditor(metaDataTable)
            val goToButton = Button(metaDataTable, SWT.PUSH)
            val pageType = item.getText(0)

            // Not all metadata have new pages. If it's a page type change, the "new" attribute is left blank
            // So take the original page instead.
            val newPage = item.getText(2).ifEmpty { item.getText(1) }
            goToButton.addSelectionListener(object : SelectionAdapter() {
                // Call go to page
                override fun widgetSelected(e: SelectionEvent) {
                    val goToPage = GoToPageDialog(manager!!)
                    if (pageType == "PRINT") {
                        goToPage.braillePageButton.selection = false
                        goToPage.printPageButton.selection = true
                    } else if (pageType == "BRAILLE") {
                        goToPage.printPageButton.selection = false
                        goToPage.braillePageButton.selection = true
                    }
                    val pageFound: Boolean = try {
                        val pageMapElement = goToPage.findPage(newPage)
                        if (pageMapElement != null) {
                            goToPage.scrollToTextAfterNode(pageMapElement.second.node)
                            true
                        } else {
                            false
                        }
                    } catch (ex: Exception) {
                        false
                    } finally {
                        goToPage.dialog.close()
                    }
                    if (!pageFound) {
                        throw BBNotifyException(
                            pageType.uppercase() + " Page '" + newPage + "' not found. Recent page changes may have overridden this edit."
                        )
                    }
                }
            })
            goToButton.text = "Go To"
            goToButton.computeSize(SWT.DEFAULT, metaDataTable!!.itemHeight)
            editor.grabHorizontal = true
            editor.minimumHeight = goToButton.size.y
            editor.minimumWidth = goToButton.size.x
            editor.setEditor(goToButton, item, 5)
        }

        private fun packColumns() {
            for (i in metaDataTable!!.columns.indices) {
                metaDataTable!!.getColumn(i).pack()
            }
        }

        private fun getType(type: String): String {
            return if (type == "printPage") {
                "PRINT"
            } else "BRAILLE"
        }

        private fun addDeleteOKAndCancelButton() {
            val buttonComp = EasySWT.makeComposite(dialog, 4)
            makeSpace(buttonComp, 265, 0)
            deleteButton = EasySWT.makePushButton(
                buttonComp, "Delete", BUTTON_WIDTH, 1
            ) { deleteMetaData() }.also {
                EasySWT.addSwtBotKey(it, SWTBOT_OK_BUTTON)
            }
            val okButton = EasySWT.makePushButton(
                buttonComp, "OK", BUTTON_WIDTH, 1
            ) {
                refreshDocumentAndClose()
                if (shell != null && !shell!!.isDisposed) {
                    shell!!.close()
                }
            }
            EasySWT.addSwtBotKey(okButton, SWTBOT_OK_BUTTON)
            EasySWT.makePushButton(
                buttonComp,
                "Cancel",
                BUTTON_WIDTH,
                1
            ) { cancelPageChangeList() }
        }

        private fun deleteMetaData() {
            val metaDataList: List<Element> = metaDataList
            // Check the items in the metaDataTable. If it's checked, then
            // delete.
            val tableItems = metaDataTable!!.items
            for (i in tableItems.indices) {
                // Table items and meta data should have a 1 to 1 correspondence
                val item = tableItems[i]
                if (item.checked) {
                    val remove = metaDataList[i]
                    remove.detach()
                    deletedMeta.add(remove)
                }
            }
            repopulateTable()
        }

        //Re-adds the metadata that have been deleted
        private fun cancelPageChangeList() {
            val head = getDocumentHead(manager!!.doc)
            if (head != null) {
                for (element in deletedMeta) {
                    head.appendChild(element)
                }
                refreshDocumentAndClose()
            }
        }
    }

    override val topMenu: TopMenu
        get() = TopMenu.EDIT

    override fun onRun(bbData: BBSelectionData) {
        open(bbData.manager)
    }

    override val sharedItem: SharedItem
        get() = SharedItem.EDIT_PAGE_NUMBER

    // There should be two tabs under the dialog: one tab for adding new
    // changes, one tab to list out all the changes
    fun open(manager: Manager?) {
        this.manager = manager
        shell = Shell(parent, SWT.DIALOG_TRIM)
        shell!!.text = "Page Number Dialog"
        shell!!.layout = GridLayout(1, true)
        createTabs()
        EasySWT.setLargeDialogSize(shell!!)
        addKeyListeners()
        shell!!.open()
        shell!!.defaultButton = okButton
        folder!!.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(event: SelectionEvent) {
                if (folder!!.selectionIndex == 0) {
                    shell!!.defaultButton = okButton
                } else if (folder!!.selectionIndex == 1) {
                    shell!!.defaultButton = deleteButton
                    //You need to create new contents every time the tab is clicked
                    pageChangeListTab!!.createContentsInPageChangeListTab()
                }
            }
        })
        shell!!.layout()
    }

    private fun createTabs() {
        folder = TabFolder(shell, SWT.NONE)
        PageChangeTab(folder)
        pageChangeListTab = PageChangeListTab(folder)
    }

    private fun findRunover(ppi: String): String {
        return if (!ppi.contains("-")) ppi else ppi.substring(ppi.indexOf('-') + 1)
    }

    private val metaDataList: List<Element>
        // Retrieve list of all added metadata in the document
        get() = getUTDMeta(manager!!.doc)

    private fun refreshDocumentAndClose() {
        manager!!.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, false, manager!!.document.rootElement))
        shell!!.close()
        metaCounter = 0
        deletedMeta = ArrayList()
    }

    private fun addKeyListeners() {
        EasyListeners.keyPress(shell!!) { e: KeyEvent ->
            if (e.keyCode == SWT.ESC.code) {
                shell!!.close()
            }
        }
    }

    private fun getBlockParent(child: Node): Node {
        return if (BBX.BLOCK.isA(child.parent)) {
            child.parent
        } else getBlockParent(child.parent)
    }

    private fun getIntersectionInclusive(start: Node, end: Node): List<Node> {
        val intersection: MutableList<Node> = ArrayList()
        intersection.add(start)
        intersection.add(end)
        if (start !== end) {
            //V1 - Do not use following-sibling in case the two nodes are in different containers
            val siblingsAfter = FastXPath.following(start).filterIsInstance<Element>()
            val siblingsBefore = FastXPath.preceding(end).filterIsInstance<Element>()
            intersection.addAll(siblingsAfter.filter { sibling -> BBX.BLOCK.isA(sibling) && sibling in siblingsBefore })
        }
        return intersection
    }

    companion object {
        const val MENU_NAME = "Edit Page Number"
        const val SWTBOT_OK_BUTTON = "pageNumberDialog.ok"
        const val SWTBOT_APPLY_BUTTON = "pageNumberDialog.apply"
        const val SWTBOT_DELETE_PRINT_INDICATOR = "pageNumberDialog.deletePage"
        const val SWTBOT_PRINT_PAGE_NUMBER_TEXT = "pageNumberDialog.printPageNumberText"
        private fun makeGroup(parent: Composite, text: String, maxColumns: Int): Group {
            val newGroup = Group(parent, SWT.NONE)
            newGroup.text = text
            newGroup.layout = GridLayout(maxColumns, false)
            return newGroup
        }

        private fun makeSpace(parent: Composite, width: Int, height: Int) {
            val newLabel = Label(parent, SWT.NONE)
            if (parent.layout is GridLayout) {
                val newData = GridData()
                newData.widthHint = width
                newData.heightHint = height
                newLabel.layoutData = newData
            } else if (parent.layout is RowLayout) {
                val newData = RowData()
                newData.height = height
                newLabel.layoutData = newData
            }
        }

        const val MAX_COLUMNS = 4
        const val SMALL_BOX_WIDTH = 30
        const val LARGE_BOX_WIDTH = 50
        const val BUTTON_WIDTH = 60
    }
}

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
package org.brailleblaster.frontmatter

import nu.xom.*
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBX.TPageCategory
import org.brailleblaster.bbx.BBX.TPageSection
import org.brailleblaster.frontmatter.VolumeUtils.getOrCreateTPage
import org.brailleblaster.frontmatter.VolumeUtils.getVolumeElements
import org.brailleblaster.frontmatter.VolumeUtils.updateEndOfVolume
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.ui.BBStyleableText
import org.brailleblaster.perspectives.braille.ui.BBStyleableText.EmphasisTags
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.events.BuildMenuEvent
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.MenuManager.addMenuItem
import org.brailleblaster.perspectives.mvc.modules.views.DebugModule
import org.brailleblaster.settings.UTDManager
import org.brailleblaster.tools.DebugMenuToolListener
import org.brailleblaster.utd.formatters.TPageFormatter
import org.brailleblaster.utd.properties.EmphasisType
import org.brailleblaster.utd.utils.UTDHelper.Companion.stripUTDRecursive
import org.brailleblaster.utils.swt.EasySWT
import org.brailleblaster.utils.swt.MenuBuilder
import org.brailleblaster.util.FormUIUtils
import org.brailleblaster.utils.UTD_NS
import org.brailleblaster.utils.gui.PickerDialog
import org.brailleblaster.utils.swt.EasyListeners
import org.brailleblaster.utils.swt.SubMenuBuilder
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.ScrolledComposite
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Consumer
import kotlin.math.max
import kotlin.math.min

class TPagesDialog : DebugMenuToolListener {
    private lateinit var doc: Document
    private var manager: Manager? = null
    private lateinit var m: UTDManager
    private lateinit var shell: Shell
    private var texts: MutableList<BBStyleableText> = mutableListOf()
    private var tPageContainers: MutableList<Element> = mutableListOf()
    private var elementMap: MutableMap<Any, Element?>? = null
    private var storedVolumes: MutableList<MutableMap<Any, Element?>?> = mutableListOf()
    private var symbolsTable: Table? = null
    private var curVolume = 0
    private var curVolLabel: Label? = null
    private var onFinish: Consumer<Shell?>? = null
    private var prefix = ""
    private var prefixDesc = ""
    private var includePrefix = false
    private var addedTPages: MutableList<Element> = mutableListOf()
    private var volumes: List<Element>? = null
    private var removePrefixButton: Button? = null
    private var titlePageCentered = false

    override val title: String
        get() = localeHandler["&TranscriberGeneratedPages"]

    override fun onRun(bbData: BBSelectionData) {
        if (bbData.manager.isEmptyDocument) {
            //See RT 6603
            bbData.manager.notify(localeHandler["emptyDocMenuWarning"])
        } else {
            manager = bbData.manager
            onFinish = Consumer {
                val changedNodes: MutableList<Node> = ArrayList()
                changedNodes.add(BBX.getRoot(manager!!.doc))
                bbData.manager.simpleManager.dispatchEvent(
                    org.brailleblaster.perspectives.mvc.events.ModifyEvent(
                        Sender.NO_SENDER,
                        changedNodes,
                        true
                    )
                )
            }
            open(bbData.manager.wpManager.shell)
        }
    }

    override fun onEvent(event: SimpleEvent) {
        if (event is BuildMenuEvent && DebugModule.enabled) {
            addMenuItem(this)
        }
    }

    fun open(parent: Shell) {
        doc = manager!!.doc
        m = manager!!.document.settingsManager
        createContents(parent)
        volumes = getVolumeElements(doc)
        tPageContainers = ArrayList()
        addedTPages = ArrayList()
        val firstTPage = orCreateFirstTPageContainer
        tPageContainers.add(firstTPage)
        if (volumes!!.isNotEmpty()) {
            val lastVolume = volumes!!.last()
            volumes!!.forEach(Consumer { v: Element ->
                if (v !== lastVolume) {
                    val newTPage = getOrCreateTPage(v)
                    tPageContainers.add(newTPage)
                    if (newTPage.childCount == 0) addedTPages.add(newTPage) //This tpage element was added by TPagesDialog
                }
            })
        }
        storedVolumes = loadAllVolumes()
        curVolume = 0
        elementMap = parseVolume(0)
        findPrefix()
        updateTexts(elementMap)
        curVolLabel!!.text = "Volume 1 of " + tPageContainers.size
        EasySWT.buildGridData().setColumns(1).setAlign(SWT.CENTER, SWT.CENTER).applyTo(curVolLabel!!)
        titlePageCentered = checkCentering()
        createTitlePageMenu()
        curVolLabel!!.parent.layout(true)
        shell.pack()
        FormUIUtils.setLargeDialogSize(shell)
        shell.open()
    }

    private val orCreateFirstTPageContainer: Element
        get() {
            var earliestSection = BBX.getRoot(doc)

            while (earliestSection.childCount > 0 && BBX.SECTION.isA(earliestSection.getChild(0))) {
                earliestSection = earliestSection.getChild(0) as Element
            }

            if (earliestSection.childCount > 0) {
                val child = earliestSection.getChild(0)
                if (BBX.CONTAINER.TPAGE.isA(child)) return child as Element
            }

            val newTPage = BBX.CONTAINER.TPAGE.create()
            earliestSection.insertChild(newTPage, 0)
            addedTPages.add(newTPage)
            return newTPage
        }

    private fun createContents(parent: Shell) {
        shell = Shell(parent, SWT.APPLICATION_MODAL or SWT.DIALOG_TRIM)
        shell.text = "Create Transcriber-Generated Pages"
        shell.layout = GridLayout(1, false)

        val folder = TabFolder(shell, SWT.NONE)
        folder.layoutData = GridData(SWT.FILL, SWT.FILL, true, true) //Needed for ScrolledComposite

        val titleTab = TabItem(folder, SWT.NONE)
        titleTab.text = "Title Page 1"

        val title2Tab = TabItem(folder, SWT.NONE)
        title2Tab.text = "Title Page 2"

        val symbolsTab = TabItem(folder, SWT.NONE)
        symbolsTab.text = "Special Symbols"

        val transNotesTab = TabItem(folder, SWT.NONE)
        transNotesTab.text = "Transcriber's Notes"

        val titleContainer = EasySWT.makeComposite(folder, 1)
        EasySWT.buildGridData().setAlign(SWT.FILL, SWT.FILL).applyTo(titleContainer)
        makeTitlePage(titleContainer)
        titleTab.control = titleContainer

        val title2Container = EasySWT.makeComposite(folder, 1)
        makeSecondTitlePage(title2Container)
        title2Tab.control = title2Container

        val ssContainer = EasySWT.makeComposite(folder, 1)
        makeSpecialSymbolsPage(ssContainer)
        symbolsTab.control = ssContainer

        val tnContainer = EasySWT.makeComposite(folder, 1)
        makeTranscriberNotesPage(tnContainer)
        transNotesTab.control = tnContainer

        val buttonPanel = EasySWT.buildComposite(shell).apply {
            this.setEqualColumnWidth(true)
            this.columns = 2
        }.build()
        EasySWT.buildGridData().setAlign(SWT.FILL, SWT.DEFAULT).applyTo(buttonPanel)
        val volNav = Group(buttonPanel, SWT.NONE)
        volNav.text = "Volume Navigation"
        volNav.layout = GridLayout(4, false)
        EasySWT.buildGridData().setColumns(2).setAlign(SWT.FILL, SWT.DEFAULT).setGrabSpace(
            horizontally = true,
            vertically = false
        ).applyTo(volNav)
        val prevButton = EasySWT.makePushButton(volNav, "Previous Volume", 1) { _: SelectionEvent? ->
            saveChanges(curVolume)
            elementMap = loadVolume(max(0.0, (curVolume - 1).toDouble()).toInt())
            updateTexts(elementMap)
        }
        EasySWT.buildGridData().setAlign(SWT.END, null).setHint(BUTTON_WIDTH, null).applyTo(prevButton)

        curVolLabel = EasySWT.makeLabel(volNav, "Volume 1 of 1", 1).also {
            EasySWT.buildGridData().setAlign(SWT.CENTER, SWT.CENTER).applyTo(it)
        }


        val nextButton = EasySWT.makePushButton(volNav, "Next Volume", 1) {
            saveChanges(curVolume)
            elementMap = loadVolume(
                min((tPageContainers.size - 1).toDouble(), (curVolume + 1).toDouble()).toInt()
            )
            updateTexts(elementMap)
        }
        EasySWT.buildGridData().setAlign(SWT.BEGINNING, null).setHint(BUTTON_WIDTH, null).applyTo(nextButton)

        val copyButton = EasySWT.makePushButton(volNav, "Copy Current Volume", BUTTON_WIDTH, 1) {
            if (EasySWT.makeEasyYesNoDialog(
                    "Confirm Copy",
                    "The current volume's t-page will be copied for each other volume. Any unsaved changes in other volumes will be lost. Continue?",
                    parent
                )
            ) copyVolumes(curVolume)
        }
        EasySWT.buildGridData().setHint(BUTTON_WIDTH, null).setAlign(SWT.RIGHT, null).setGrabSpace(
            horizontally = true,
            vertically = false
        )
            .setColumns(1).applyTo(copyButton)

        EasySWT.makeLabel(buttonPanel, "", 1)
        val okCancelPanel = EasySWT.makeComposite(buttonPanel, 2)
        EasySWT.buildGridData().setAlign(SWT.END, null).setGrabSpace(horizontally = true, vertically = false)
            .setColumns(1).applyTo(okCancelPanel)
        val okButton = EasySWT.makePushButton(okCancelPanel, "Ok", BUTTON_WIDTH, 1) {
            saveChanges(curVolume)
            generateTPages()
        }
        EasySWT.addSwtBotKey(okButton, SWTBOT_OK_BUTTON)
        EasySWT.makePushButton(okCancelPanel, "Cancel", BUTTON_WIDTH, 1) {
            addedTPages.forEach(Consumer { obj: Element -> obj.detach() })
            shell.close()
        }

        folder.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                if (folder.selection[0] === titleTab) {
                    createTitlePageMenu()
                } else if (folder.selection[0] === title2Tab) {
                    createSecondTitlePageMenu()
                } else if (folder.selection[0] === symbolsTab) {
                    deleteMenu()
                } else if (folder.selection[0] === transNotesTab) {
                    createTranscriberNotesMenu()
                }
            }
        })
    }

    /*
	 * Returns true if title page has previously been centered
	 */
    private fun checkCentering(): Boolean {
        //Check each volume for a title page (because a tpage may not necessarily have a title page yet)
        for (volume in storedVolumes) {
            //Check for each title page category
            for (category in TPageCategory.entries) {
                if (volume!!.containsKey(category)) {
                    val titlePageCategory = volume[category]
                    //Skip this volume if it's not actually in the document
                    if (titlePageCategory == null || titlePageCategory.parent == null || titlePageCategory.document == null) {
                        continue
                    }
                    //If element has TPageFormatter's centered attribute, it will be centered
                    return (titlePageCategory.parent as Element).getAttribute(
                        TPageFormatter.CENTERED_ATTR.localName,
                        UTD_NS
                    ) != null
                }
            }
        }

        return false
    }

    private fun createTitlePageMenu() {
        deleteMenu()
        val mb = MenuBuilder(shell)

        mb.addToMenu("Settings")
        mb.addCheckItem("Center title page", 0, titlePageCentered) { e: SelectionEvent ->
            titlePageCentered = (e.widget as MenuItem).selection
            createTitlePageMenu() //Recreate menu so that margin menu gets greyed out
            //Toggle the painted margins on each text box
            texts.forEach(Consumer { text: BBStyleableText ->
                //Second title page and TN page has no margins so skip it
                if (text.text.data !== TPageSection.TRANSCRIBER_NOTES
                    && text.text.data !== TPageSection.SECOND_TITLE_PAGE
                ) {
                    text.setCustomMargins(!titlePageCentered)
                }
            })
        }

        makeEmphasisTranslationAndMarginMenu(mb)

        mb.build()
    }

    private fun createSecondTitlePageMenu() {
        deleteMenu()
        val mb = MenuBuilder(shell)
        makeEmphasisTranslationAndMarginMenu(mb)
        mb.build()
    }

    private fun createTranscriberNotesMenu() {
        deleteMenu()
        val mb = MenuBuilder(shell)
        makeEmphasisTranslationAndMarginMenu(mb)
        mb.addToMenu("Heading").addPushItem("Centered Heading", 0) {
            if (currentText != null) {
                currentText!!.toggleTag(BBStyleableText.StyleTags.TPAGE_HEADING)
            }
        }
        mb.build()
    }

    private fun deleteMenu() {
        if (shell.menuBar != null) {
            shell.menuBar.dispose()
        }
    }

    private fun makeEmphasisTranslationAndMarginMenu(mb: MenuBuilder) {
        mb.addToMenu("Emphasis")
        for (tag in EmphasisTags.entries) {
            if (tag == EmphasisTags.ITALICBOLD || tag == EmphasisTags.CTEXACT || tag == EmphasisTags.CTUNCONTRACTED) continue
            mb.addPushItem(tag.tagName, tag.defaultAccelerator) {
                if (currentText != null) currentText!!.toggleTag(tag)
            }
        }
        mb.addToMenu("Translation")
            .addPushItem("Uncontracted", EmphasisTags.CTUNCONTRACTED.defaultAccelerator) {
                if (currentText != null) currentText!!.toggleTag(EmphasisTags.CTUNCONTRACTED)
            }
            .addPushItem(
                "Direct", EmphasisTags.CTEXACT.defaultAccelerator
            ) { if (currentText != null) currentText!!.toggleTag(EmphasisTags.CTEXACT) }

        mb.addToMenu("Margin", !titlePageCentered)

        var indentLevel = 0
        while (indentLevel <= MAX_INDENT_LEVEL) {
            val sub = SubMenuBuilder(shell)
            var runoverLevel = 0
            while (runoverLevel <= MAX_INDENT_LEVEL) {
                val indentLevel2 = indentLevel //Lambdas!
                val runoverLevel2 = runoverLevel
                sub.addPushItem(
                    (indentLevel + 1).toString() + "-" + (runoverLevel + 1),
                    SWT.NONE
                ) {
                    if (currentText != null) {
                        currentText!!.applyMargin(indentLevel2, runoverLevel2)
                    }
                }
                runoverLevel += 2
            }
            mb.addSubMenu("Indent " + (indentLevel + 1), sub)
            indentLevel += 2
        }
    }

    private fun loadVolume(volumeNum: Int): MutableMap<Any, Element?>? {
        curVolume = volumeNum
        curVolLabel!!.text = "Volume " + (curVolume + 1) + " of " + tPageContainers.size
        //EasySWT's applyTo method appropriately sets the width based on the size of the text
        EasySWT.buildGridData().setColumns(1).setAlign(SWT.CENTER, SWT.CENTER).applyTo(curVolLabel!!)
        curVolLabel!!.parent.layout(true)
        return storedVolumes[volumeNum]
    }

    private fun parseVolume(volumeNum: Int): MutableMap<Any, Element?>? {
        if (volumeNum >= tPageContainers.size) {
            log.error("Volume $volumeNum does not exist")
            return null
        }
        val elementMap = HashMap<Any, Element?>()
        val curVolume = tPageContainers[volumeNum]
        val titlePageNode = findSection(curVolume, TPageSection.TITLE_PAGE)
        if (titlePageNode != null) {
            for (category in TPageCategory.entries) {
                val categoryNode = findCategory(titlePageNode, category)
                if (categoryNode != null) {
                    elementMap[category] = categoryNode
                }
            }
        }
        for (page in TPageSection.entries) {
            if (page == TPageSection.TITLE_PAGE) continue
            val pageNode = findSection(curVolume, page)
            if (pageNode != null) {
                elementMap[page] = pageNode
            } else if (page == TPageSection.TRANSCRIBER_NOTES) {
                val container = BBX.CONTAINER.TPAGE_SECTION.create(TPageSection.TRANSCRIBER_NOTES)
                val block = BBX.BLOCK.STYLE.create("Centered Heading")
                block.appendChild(TRANSCRIBER_NOTES_HEADING)
                container.appendChild(block)
                elementMap[page] = container
            }
        }

        return elementMap
    }

    private fun findPrefix() {
        if (storedVolumes.isEmpty()) return
        val ssElement = storedVolumes[0]!![TPageSection.SPECIAL_SYMBOLS] ?: return
        var query = ssElement.query("descendant::*[@prefix]")
        if (query.size() > 0) {
            val prefixElement = query[0] as Element
            prefix = prefixElement.getAttributeValue("prefix")
            log.debug("Found prefix: $prefix")
            query = ssElement.query("descendant::*[@isPrefix]")
            if (query.size() > 0) {
                val parent = query[0] as Element
                if (parent.childCount > 1) {
                    prefixDesc = parent.getChild(1).value.trim { it <= ' ' }
                    log.debug("Found prefix description: $prefixDesc")
                }
            }
        }
    }

    private fun loadAllVolumes(): MutableList<MutableMap<Any, Element?>?> {
        val storedVolumes: MutableList<MutableMap<Any, Element?>?> = ArrayList()
        for (i in tPageContainers.indices) {
            storedVolumes.add(parseVolume(i))
        }
        return storedVolumes
    }

    private fun insertAutoFillData(symbols: List<List<SpecialSymbols.Symbol>>) {
        saveChanges(curVolume)
        for (i in symbols.indices) {
            val volume = symbols[i]
            if (volume.isEmpty()) {
                continue
            }
            val names: MutableList<String> = ArrayList()
            val descriptions: MutableList<String> = ArrayList()
            if (includePrefix) {
                names.add(prefix)
                descriptions.add(prefixDesc)
            }
            for (symbol in volume) {
                names.add(symbol.symbol)
                var desc = symbol.desc
                if (desc == null) desc = ""
                descriptions.add(desc)
            }
            val volumeMap = storedVolumes[i]
            volumeMap!![TPageSection.SPECIAL_SYMBOLS] = getSSElement(names, descriptions)
        }
        elementMap = loadVolume(curVolume)
        updateTexts(elementMap)
    }

    private fun saveChanges(volumeNum: Int) {
        updateMap()
        storedVolumes.removeAt(volumeNum)
        storedVolumes.add(volumeNum, elementMap)
    }

    private fun copyVolumes(volumeNum: Int) {
        updateMap()
        val curVol = elementMap
        val size = storedVolumes.size
        val storedVolumesCopy: MutableList<MutableMap<Any, Element?>?> = mutableListOf()
        repeat(size) {
            val newHashMap = HashMap<Any, Element?>()
            for (key in curVol!!.keys) {
                newHashMap[key] = curVol[key]?.copy()
            }
            storedVolumesCopy.add(newHashMap)
        }
        storedVolumes = storedVolumesCopy
    }

    private fun updateTexts(elementMap: MutableMap<Any, Element?>?) {
        for (text in texts) {
            text.clear()
        }
        symbolsTable!!.removeAll()
        for (category in TPageCategory.entries) {
            val mapEntry = elementMap!![category] ?: continue
            for (text in texts) {
                if (text.text.data === category) {
                    text.setXML(mapEntry)
                }
            }
        }
        for (section in TPageSection.entries) {
            if (section == TPageSection.TITLE_PAGE || section == TPageSection.SPECIAL_SYMBOLS) continue
            val mapEntry = elementMap!![section]
            if (mapEntry == null) {
                if (section == TPageSection.TRANSCRIBER_NOTES) {
                    for (text in texts) {
                        if (text.text.data === section && text.text.text == TRANSCRIBER_NOTES_HEADING) {
                            //Add linebreak after Transcriber's Notes heading
                            text.text.append(System.lineSeparator())
                            break
                        }
                    }
                }
                continue
            }
            for (text in texts) {
                if (text.text.data === section) {
                    text.setXML(mapEntry)
                }
            }
        }

        val ssEntry = elementMap!![TPageSection.SPECIAL_SYMBOLS]
        if (ssEntry != null) {
            updateSSTable(ssEntry)
        }
    }

    private fun updateSSTable(parent: Element) {
        val parentCopy = parent.copy()
        stripUTDRecursive(parentCopy)
        symbolsTable!!.clearAll()
        var i = 0
        if (parentCopy.childElements.size() > 0 && parentCopy.childElements[0].value == SS_PAGE_HEADING) i = 1
        while (i < parentCopy.childElements.size()) {
            val element = parentCopy.childElements[i]
            var symbol = ""
            var desc = ""
            if (element.childCount == 0) {
                i++
                continue
            }
            if (element.getChild(0) is Element) {
                symbol = element.getChild(0).value
                if (element.childCount > 1) {
                    if (element.getChild(1) is Text) {
                        desc = element.getChild(1).value.trim { it <= ' ' }
                    }
                }
            } else if (element.getChild(0) is Text) {
                symbol = SS_HEADING
                desc = element.getChild(0).value
            }

            if (symbol.isNotEmpty() || desc.isNotEmpty()) {
                if (symbol == prefix && desc == prefixDesc) {
                    includePrefix = true
                }
                val newItem = TableItem(symbolsTable, SWT.NONE)
                newItem.setText(arrayOf(symbol, desc))
            }
            i++
        }
    }

    private fun makeTitlePage(container: Composite) {
        val sc = ScrolledComposite(container, SWT.V_SCROLL or SWT.H_SCROLL or SWT.BORDER)
        sc.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        sc.expandVertical = true
        sc.expandHorizontal = true

        val innerContainer = EasySWT.makeComposite(sc, 1)

        texts = ArrayList()
        for (category in TPageCategory.entries) {
            val newGroup = makeGroup(
                innerContainer,
                if (category == TPageCategory.AUTHOR) "Author(s)" else category.name.lowercase(
                        Locale.getDefault()
                    ).replace("_".toRegex(), " ")
                    .replaceFirstChar { it.titlecaseChar() },
                1
            )
            val newText = makeBBTextTitle(newGroup)
            texts.add(newText)
            EasySWT.buildGridData().setHint(TEXT_WIDTH, TEXT_HEIGHT).applyTo(newText.text)
            newText.text.data = category
            newGroup.layout()
            innerContainer.layout()
        }

        sc.content = innerContainer
        sc.setMinSize(innerContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT))
    }

    private fun makeSecondTitlePage(container: Composite) {
        val textContainer = EasySWT.makeComposite(container, 1)
        EasySWT.buildGridData().setAlign(SWT.FILL, SWT.FILL).setGrabSpace(horizontally = true, vertically = true)
            .applyTo(textContainer)
        val newText = makeBBText(textContainer)
        newText.setNewLineWrapStyle(TPAGE_ELEMENT_STYLE2)
        newText.setFontSize(11)
        newText.text.data = TPageSection.SECOND_TITLE_PAGE
        EasySWT.buildGridData().setAlign(SWT.FILL, SWT.FILL).setGrabSpace(horizontally = true, vertically = true)
            .applyTo(newText.text)
        texts.add(newText)
    }

    private fun makeSpecialSymbolsPage(container: Composite) {
        val symbolsContainer = EasySWT.makeComposite(container, 1)
        EasySWT.buildGridData().setAlign(SWT.CENTER, SWT.FILL).setGrabSpace(horizontally = false, vertically = true)
            .applyTo(symbolsContainer)

        symbolsTable = Table(symbolsContainer, SWT.VIRTUAL or SWT.BORDER or SWT.FULL_SELECTION or SWT.V_SCROLL).apply {
            linesVisible = true
            headerVisible = true
        }.also {
            EasySWT.buildGridData().setHint(500, SWT.DEFAULT).setAlign(SWT.CENTER, SWT.FILL).setGrabSpace(
                horizontally = false,
                vertically = true
            )
                .applyTo(it)
        }

        val symbolColumn = TableColumn(symbolsTable, SWT.NONE)
        symbolColumn.width = 100
        symbolColumn.text = "Symbol"
        val descColumn = TableColumn(symbolsTable, SWT.NONE)
        descColumn.width = 415
        descColumn.text = "Description"

        val editButtonPanel = EasySWT.makeComposite(container, 3)
        EasySWT.buildGridData().setAlign(SWT.LEFT, SWT.FILL).applyTo(editButtonPanel)
        val ssEditButton = EasySWT.makePushButton(editButtonPanel, "Edit...", BUTTON_WIDTH, 1) {
            if (symbolsTable!!.selectionCount > 0) ChangeSymbolDialog().edit(
                shell,
                symbolsTable!!,
                symbolsTable!!.selection[0]
            )
        }
        ssEditButton.isEnabled = false

        EasySWT.makePushButton(
            editButtonPanel,
            "Add...",
            BUTTON_WIDTH,
            1
        ) { ChangeSymbolDialog().add(shell, symbolsTable!!) }

        val ssDeleteButton = EasySWT.makePushButton(editButtonPanel, "Delete", BUTTON_WIDTH, 1) {
            if (symbolsTable!!.selectionCount > 0) symbolsTable!!.remove(
                symbolsTable!!.selectionIndex
            )
        }

        val arrangeButtonPanel = EasySWT.makeComposite(container, 3)
        val ssMoveUpButton = EasySWT.makePushButton(arrangeButtonPanel, "Move Up", BUTTON_WIDTH, 1, null)
        val ssMoveDownButton = EasySWT.makePushButton(arrangeButtonPanel, "Move Down", BUTTON_WIDTH, 1, null)
        ssMoveUpButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                moveTableItemUp(symbolsTable!!)
                val index = symbolsTable!!.selectionIndex
                if (index == 0) ssMoveUpButton.isEnabled = false
                ssMoveDownButton.isEnabled = true
            }
        })
        ssMoveUpButton.isEnabled = false
        ssMoveDownButton.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                moveTableItemDown(symbolsTable!!)
                val index = symbolsTable!!.selectionIndex
                if (index == symbolsTable!!.itemCount - 1) ssMoveDownButton.isEnabled = false
                ssMoveUpButton.isEnabled = true
            }
        })
        EasySWT.makePushButton(arrangeButtonPanel, "Rearrange", BUTTON_WIDTH, 1) {
            organizeSymbols(
                symbolsTable!!
            )
        }

        val autoFillPanel = EasySWT.makeComposite(container, 4)
        EasySWT.makePushButton(
            autoFillPanel,
            "Insert Prefix...",
            BUTTON_WIDTH,
            1
        ) {
            EditPrefixDialog().open(shell) { symbol: String, symbolDesc: String ->
                this.addPrefix(
                    symbol,
                    symbolDesc
                )
            }
        }
        removePrefixButton = EasySWT.makePushButton(
            autoFillPanel,
            "Remove Prefix",
            BUTTON_WIDTH,
            1
        ) { removePrefix() }.apply {
            isEnabled = prefix.isNotEmpty()
        }
        EasySWT.makePushButton(
            autoFillPanel,
            "Auto Fill...",
            BUTTON_WIDTH,
            1
        ) {
            AutoFillSpecialSymbols(
                doc,
                m
            ) { symbols: List<List<SpecialSymbols.Symbol>> -> this.insertAutoFillData(symbols) }.openDialog(
                shell,
                curVolume
            )
        }
        EasySWT.makePushButton(
            autoFillPanel,
            "Auto Fill Options...",
            BUTTON_WIDTH,
            1
        ) { SpecialSymbolEditor().open(shell, m) }

        symbolsTable!!.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                if (symbolsTable!!.selectionCount > 0) {
                    ssEditButton.isEnabled = true
                    ssDeleteButton.isEnabled = true
                    ssMoveUpButton.isEnabled = symbolsTable!!.selectionIndex > 0
                    ssMoveDownButton.isEnabled = symbolsTable!!.selectionIndex < symbolsTable!!.itemCount - 1
                } else {
                    ssEditButton.isEnabled = false
                    ssDeleteButton.isEnabled = false
                    ssMoveUpButton.isEnabled = false
                    ssMoveDownButton.isEnabled = false
                }
            }
        })
    }

    private fun makeTranscriberNotesPage(container: Composite) {
        val textContainer = EasySWT.makeComposite(container, 1)
        EasySWT.buildGridData().setAlign(SWT.FILL, SWT.FILL).setGrabSpace(horizontally = true, vertically = true)
            .applyTo(textContainer)

        val newText = makeBBText(textContainer)
        newText.setFontSize(11)
        newText.text.data = TPageSection.TRANSCRIBER_NOTES
        EasySWT.buildGridData().setAlign(SWT.FILL, SWT.FILL).setGrabSpace(horizontally = true, vertically = true)
            .setColumns(1)
            .applyTo(newText.text)
        texts.add(newText)
    }

    private inner class ChangeSymbolDialog {
        fun add(parent: Shell?, table: Table) {
            open(parent, table, null)
        }

        fun edit(parent: Shell?, table: Table, editingItem: TableItem?) {
            open(parent, table, editingItem)
        }

        private fun open(parent: Shell?, table: Table, editingItem: TableItem?) {
            val editing = editingItem != null
            val addDialog = Shell(parent, SWT.DIALOG_TRIM)
            addDialog.layout = GridLayout(1, false)
            addDialog.text = "Add New Special Symbol"
            val addContents = EasySWT.makeComposite(addDialog, 2)

            EasySWT.makeLabel(addContents, "Type:", 1)
            val typeCombo = Combo(addContents, SWT.READ_ONLY)
            typeCombo.setItems("Symbol", "Heading")
            typeCombo.select(0)

            val symbolLabel = EasySWT.makeLabel(addContents, "ASCII Symbol:", 1)
            val symbolText = EasySWT.makeText(addContents, TEXT_WIDTH, 1, null)

            val descLabel = EasySWT.makeLabel(addContents, "Description:", 1)
            val descText = EasySWT.makeText(addContents, TEXT_WIDTH, 1) { e: KeyEvent ->
                addNewSS(
                    typeCombo.selectionIndex,
                    symbolText.text,
                    (e.source as org.eclipse.swt.widgets.Text).text,
                    editingItem,
                    table,
                    addDialog
                )
            }


            val pickSymbolPanel = EasySWT.makeComposite(addContents, 1)
            EasySWT.buildGridData().setColumns(2).applyTo(pickSymbolPanel)
            val pickFromListButton =
                EasySWT.makePushButton(pickSymbolPanel, "Pick From List...", 1, BUTTON_WIDTH) {
                    val pd = PickerDialog()
                    pd.headings = arrayOf("Symbol", "Description")
                    val symbols = SpecialSymbols.getSymbols()
                    val contents: MutableList<Array<String>> = ArrayList()
                    symbols.forEach(Consumer { sym: SpecialSymbols.Symbol ->
                        val newString = arrayOf(
                            sym.symbol,
                            sym.desc ?: ""
                        )
                        contents.add(newString)
                    })
                    pd.contents = contents
                    pd.message = "Select a symbol:"
                    pd.open(addDialog) { i: Int ->
                        if (i != -1) {
                            val selectedSymbol = contents[i]
                            symbolText.text = selectedSymbol[0]
                            descText.text = selectedSymbol[1]
                        }
                    }
                }

            typeCombo.addSelectionListener(object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent) {
                    if (typeCombo.selectionIndex == 1) {
                        symbolLabel.text = ""
                        symbolText.text = SS_HEADING
                        symbolText.isEnabled = false
                        descLabel.text = "Heading:"
                        pickFromListButton.isEnabled = false
                    } else {
                        symbolLabel.text = "ASCII Symbol:"
                        symbolText.text = ""
                        symbolText.isEnabled = true
                        descLabel.text = "Description:"
                        pickFromListButton.isEnabled = true
                    }
                }
            })

            val addButtonPanel = EasySWT.makeComposite(addContents, 2)
            EasySWT.buildGridData().setColumns(2).applyTo(addButtonPanel)

            val okButton = EasySWT.makePushButton(
                addButtonPanel, "Ok", BUTTON_WIDTH, 1
            ) {
                addNewSS(
                    typeCombo.selectionIndex,
                    symbolText.text,
                    descText.text,
                    editingItem,
                    table,
                    addDialog
                )
            }
            EasySWT.makePushButton(
                addButtonPanel, "Cancel", BUTTON_WIDTH, 1
            ) { addDialog.close() }

            if (editing) {
                okButton.text = "Edit"
                if (editingItem.getText(0) == SS_HEADING) {
                    typeCombo.select(1)
                    symbolLabel.text = ""
                    symbolText.isEnabled = false
                    descLabel.text = "Heading:"
                }
                if (prefix.isNotEmpty() && editingItem.getText(0).startsWith(prefix)) symbolText.text =
                    editingItem.getText(0).substring(prefix.length)
                else symbolText.text = editingItem.getText(0)
                descText.text = editingItem.getText(1)
            }

            addDialog.open()
            addDialog.pack()
        }
    }

    private inner class EditPrefixDialog {
        fun open(parent: Shell?, callback: BiConsumer<String, String>) {
            var storedPrefix = prefix
            if (storedPrefix.isEmpty()) {
                storedPrefix = SpecialSymbols.getPrefixDefault()[0]
            }
            var storedPrefixDesc = prefixDesc
            if (storedPrefixDesc.isEmpty()) {
                storedPrefixDesc = SpecialSymbols.getPrefixDefault()[1]
            }
            val dialog = Shell(parent, SWT.APPLICATION_MODAL or SWT.DIALOG_TRIM)
            dialog.layout = GridLayout(1, false)
            val prefixPanel = EasySWT.makeComposite(dialog, 1)
            EasySWT.makeLabel(prefixPanel, "Prefix to be assigned to each symbol:", 1)
            val definePrefix = EasySWT.makeComposite(prefixPanel, 2)
            EasySWT.makeLabel(definePrefix, "Prefix:", 1)
            val prefixText = EasySWT.makeText(definePrefix, TEXT_WIDTH, 1)
            prefixText.text = storedPrefix

            val includePrefixCheck =
                EasySWT.buildPushButton(prefixPanel).apply {
                    this.swtOptions = SWT.CHECK
                    this.text = "Include prefix in list"
                }.build()
            val includePrefixComp = EasySWT.makeComposite(prefixPanel, 2)
            val prefixLabel = EasySWT.makeLabel(includePrefixComp, "Symbol: ", TEXT_WIDTH, 2)
            EasySWT.makeLabel(includePrefixComp, "Description: ", 1)
            val prefixDescText = EasySWT.makeText(includePrefixComp, TEXT_WIDTH, 1)
            prefixDescText.isEnabled = prefixDesc.isNotEmpty()
            prefixDescText.text = storedPrefixDesc

            EasyListeners.selection(includePrefixCheck) {
                prefixDescText.isEnabled = includePrefixCheck.selection
            }
            EasyListeners.modify(prefixText) {
                prefixLabel.text = "Symbol: " + prefixText.text
            }

            val buttonPanel = EasySWT.makeComposite(prefixPanel, 3)
            EasySWT.buildGridData().setAlign(SWT.RIGHT, null).setGrabSpace(horizontally = true, vertically = false)
                .setColumns(2)
                .applyTo(buttonPanel)
            EasySWT.makePushButton(
                buttonPanel,
                "Make Default",
                BUTTON_WIDTH,
                1
            ) { SpecialSymbols.setPrefixDefault(prefixText.text, prefixDescText.text) }
            EasySWT.makePushButton(buttonPanel, "Ok", BUTTON_WIDTH, 1) {
                includePrefix = includePrefixCheck.selection
                callback.accept(prefixText.text, prefixDescText.text)
                dialog.close()
            }
            EasySWT.makePushButton(buttonPanel, "Cancel", BUTTON_WIDTH, 1) { dialog.close() }
            dialog.open()
            dialog.pack(true)
        }
    }

    private fun moveTableItemUp(symbolsTable: Table) {
        if (symbolsTable.selectionCount > 0 && symbolsTable.selectionIndex > 0) {
            val selectedItem = symbolsTable.selection[0]
            val newText = arrayOfNulls<String>(symbolsTable.columnCount)
            for (i in 0 until symbolsTable.columnCount) {
                newText[i] = selectedItem.getText(i)
            }
            val index = symbolsTable.selectionIndex
            symbolsTable.remove(index)
            val copyItem = TableItem(symbolsTable, SWT.NONE, index - 1)
            copyItem.setText(newText)
            symbolsTable.setSelection(index - 1)
        }
    }

    private fun moveTableItemDown(symbolsTable: Table) {
        if (symbolsTable.selectionCount > 0 && symbolsTable.selectionIndex < symbolsTable.itemCount - 1) {
            val selectedItem = symbolsTable.selection[0]
            val newText = arrayOfNulls<String>(symbolsTable.columnCount)
            for (i in 0 until symbolsTable.columnCount) {
                newText[i] = selectedItem.getText(i)
            }
            val index = symbolsTable.selectionIndex
            symbolsTable.remove(index)
            val copyItem = TableItem(symbolsTable, SWT.NONE, index + 1)
            copyItem.setText(newText)
            symbolsTable.setSelection(index + 1)
        }
    }

    private fun organizeSymbols(symbolsTable: Table) {
        if (symbolsTable.itemCount == 0) return
        val organizeDialog = MessageBox(shell, SWT.ICON_INFORMATION or SWT.YES or SWT.NO)
        organizeDialog.text = "Rearrange Symbols"
        organizeDialog.message =
            "The table will be arranged by cell complexity according to Braille Formats 2016. Continue?"
        if (organizeDialog.open() != SWT.YES) return
        val complexityOrder = "abcdefghijklmnopqrstuvxyz&=(!)*<%?:$]\\[w1234567890/+#>'-@^_\".;,"
        val items = symbolsTable.items
        for (i in 0 until items.size - 1) {
            for (j in 1 until items.size - i) {
                var prevChar = items[j - 1].getText(0).lowercase(Locale.getDefault())[0]
                var thisChar = items[j].getText(0).lowercase(Locale.getDefault())[0]
                var iterator = 0
                while (prevChar == thisChar) {
                    iterator++
                    if (iterator >= items[j - 1].getText(0).length) {
                        prevChar = '|'
                        break
                    } else if (iterator >= items[j].getText(0).length) {
                        thisChar = '|'
                        break
                    }
                    prevChar = items[j - 1].getText(0).lowercase(Locale.getDefault())[iterator]
                    thisChar = items[j].getText(0).lowercase(Locale.getDefault())[iterator]
                }
                if (complexityOrder.indexOf(prevChar) > complexityOrder.indexOf(thisChar)) {
                    val temp = items[j - 1]
                    items[j - 1] = items[j]
                    items[j] = temp
                }
            }
        }
        //Dumb SWT manipulation
        val keys = arrayOfNulls<String>(items.size)
        val values = arrayOfNulls<String>(items.size)
        for (i in items.indices) {
            keys[i] = items[i].getText(0)
            values[i] = items[i].getText(1)
        }
        symbolsTable.removeAll()
        for (i in keys.indices) {
            val newItem = TableItem(symbolsTable, SWT.NONE)
            newItem.setText(0, keys[i])
            newItem.setText(1, values[i])
        }
    }

    private fun addPrefix(symbol: String, symbolDesc: String) {
        saveChanges(curVolume)
        prefix = symbol
        prefixDesc = symbolDesc
        symbolsTable!!.removeAll()
        for (i in storedVolumes.indices) {
            val elementMap = storedVolumes[i]
            val symbols: MutableList<String> = ArrayList()
            val descriptions: MutableList<String> = ArrayList()
            if (includePrefix) {
                symbols.add(symbol)
                descriptions.add(symbolDesc)
            }
            val ssElement = elementMap!![TPageSection.SPECIAL_SYMBOLS]
            if (ssElement != null) {
                for (p in 0 until ssElement.childElements.size()) {
                    val symbolElement = ssElement.childElements[p].copy()
                    stripUTDRecursive(symbolElement)
                    if (symbolElement.childCount > 0 && symbolElement.getChild(0) is Element) {
                        val span = symbolElement.getChild(0) as Element
                        val localPrefix = span.getAttributeValue("prefix")
                        span.detach()
                        if (localPrefix != null) symbols.add(span.value.substring(localPrefix.length))
                        else symbols.add(span.value)
                        descriptions.add(symbolElement.value)
                    } else if (symbolElement.getAttribute(SS_HEADING_ATTR.localName) != null) {
                        symbols.add(SS_HEADING)
                        descriptions.add(symbolElement.value)
                    }
                }
            }
            elementMap[TPageSection.SPECIAL_SYMBOLS] = getSSElement(symbols, descriptions)
            if (i == curVolume) this.elementMap = elementMap
        }
        updateSSTable(storedVolumes[curVolume]!![TPageSection.SPECIAL_SYMBOLS]!!)
        if (removePrefixButton != null && !removePrefixButton!!.isDisposed) removePrefixButton!!.isEnabled =
            prefix.isNotEmpty()
    }

    private fun removePrefix() {
        //WARNING: DUMB CODE AHEAD
        if (includePrefix) {
            shell.setRedraw(false)
            val curVol = curVolume
            for (i in storedVolumes.indices) {
                //Open up each volume and remove the first entry of the symbols table
                //if it's the prefix description
                if (storedVolumes[i]!!.containsKey(TPageSection.SPECIAL_SYMBOLS)) {
                    elementMap = loadVolume(i)
                    updateTexts(elementMap)
                    if (symbolsTable!!.items.isNotEmpty() && symbolsTable!!.getItem(0)
                            .getText(0) == prefix && symbolsTable!!.getItem(0).getText(1) == prefixDesc
                    ) {
                        symbolsTable!!.remove(0)
                        saveChanges(i)
                    }
                }
            }
            elementMap = loadVolume(curVol)
            updateTexts(elementMap)
            shell.setRedraw(true)
        }
        includePrefix = false
        addPrefix("", "")
    }

    private fun findSection(element: Element, section: TPageSection): Element? {
        for (i in 0 until element.childElements.size()) {
            val curElement = element.childElements[i]
            if (BBX.CONTAINER.TPAGE_SECTION.isA(curElement)) {
                if (BBX.CONTAINER.TPAGE_SECTION.ATTRIB_TYPE[curElement] == section) {
                    return curElement
                }
            }
            val foundElement = findSection(curElement, section)
            if (foundElement != null) return foundElement
        }
        return null
    }

    private fun findCategory(element: Element, category: TPageCategory): Element? {
        for (i in 0 until element.childElements.size()) {
            val curElement = element.childElements[i]
            if (BBX.CONTAINER.TPAGE_CATEGORY.isA(curElement)) {
                if (BBX.CONTAINER.TPAGE_CATEGORY.ATTRIB_TYPE[curElement] == category) {
                    return curElement
                }
            }
            val foundElement = findCategory(curElement, category)
            if (foundElement != null) return foundElement
        }
        return null
    }

    private fun generateTPages() {
        for (curVol in tPageContainers.indices) {
            val elementMap = storedVolumes[curVol]
            val newParent = tPageContainers[curVol]
            if (newParent.childCount > 0) newParent.removeChildren()
            updateMap()

            val titlePageRoot = BBX.CONTAINER.TPAGE_SECTION.create(TPageSection.TITLE_PAGE)
            for (category in TPageCategory.entries) {
                val element = elementMap!![category]
                if (element != null) {
                    if (element.parent != null) element.detach()
                    titlePageRoot.appendChild(element)
                }
            }

            if (titlePageCentered) {
                titlePageRoot.addAttribute(TPageFormatter.CENTERED_ATTR.copy())
            } else {
                val attr = titlePageRoot.getAttribute(TPageFormatter.CENTERED_ATTR.localName, UTD_NS)
                if (attr != null) {
                    titlePageRoot.removeAttribute(attr)
                }
            }
            newParent.appendChild(titlePageRoot)

            for (page in TPageSection.entries) {
                val curPage = elementMap!![page]
                if (curPage != null) {
                    if (curPage.parent != null) curPage.detach()
                    when (page) {
                        TPageSection.SPECIAL_SYMBOLS -> {
                            if (curPage.childElements.size() == 0 || curPage.childElements[0].childCount == 0 || curPage.childElements[0].getChild(
                                    0
                                ).value != SS_PAGE_HEADING
                            ) {
                                val heading = BBX.BLOCK.STYLE.create(TPAGE_HEADING_STYLE)
                                //RT #3851: Non-breaking spaces are used so that the heading is correctly translated
                                heading.appendChild(SS_PAGE_HEADING)
                                curPage.insertChild(heading, 0)
                            }
                            newParent.appendChild(curPage)
                        }

                        TPageSection.TRANSCRIBER_NOTES -> {
                            val block = BBX.BLOCK.STYLE.create("Centered Heading")
                            block.appendChild(TRANSCRIBER_NOTES_HEADING)
                            //You need the transcriber notes title at the start of this page
                            curPage.insertChild(block, 0)
                            newParent.appendChild(curPage)
                            for (i in 0 until curPage.childElements.size()) {
                                val curChild = curPage.childElements[i]
                                //TODO: This shouldn't be necessary
                                if (BBX.BLOCK.STYLE.isA(curChild) && m.hasStyle(curChild, "Centered Heading")) {
                                    m.applyStyle(
                                        m.engine.styleDefinitions.getStyleByName(TPAGE_HEADING_STYLE),
                                        curChild
                                    )
                                }
                            }
                        }

                        else -> {
                            newParent.appendChild(curPage)
                        }
                    }
                } else if (page != TPageSection.TITLE_PAGE) {
                    //If there's no content, add an empty tag for TPageFormatter
                    newParent.appendChild(BBX.CONTAINER.TPAGE_SECTION.create(page))
                }
            }
        }


        //Issue #4646: Do not call updateEndOfVolume when no volumes exist
        //(eg TPage dialog is putting text at the beginning of the document)
        if (volumes!!.isNotEmpty()) {
            updateEndOfVolume(doc)
        }

        onFinish!!.accept(shell)
        shell.close()
    }

    private fun updateMap() {
        elementMap = HashMap()
        for (text in texts) {
            if (text.text.text.isNotEmpty()) {
                val data = text.text.data
                if (data is TPageCategory) {
                    elementMap!![data] = text.getXML(BBX.CONTAINER.TPAGE_CATEGORY.create(data))
                } else {
                    elementMap!![data] = text.getXML(BBX.CONTAINER.TPAGE_SECTION.create(data as TPageSection))
                }
            }
        }
        if (symbolsTable!!.itemCount > 0) {
            val items = symbolsTable!!.items
            val symbols: MutableList<String> = ArrayList()
            val desc: MutableList<String> = ArrayList()
            for (item in items) {
                symbols.add(item.getText(0))
                desc.add(item.getText(1))
            }
            elementMap!![TPageSection.SPECIAL_SYMBOLS] = getSSElement(symbols, desc)
        }
    }

    private fun getSSElement(names: List<String>, descriptions: List<String>): Element {
        require(names.size == descriptions.size) { "Symbols list and descriptions list are uneven" }
        val ssPage = BBX.CONTAINER.TPAGE_SECTION.create(TPageSection.SPECIAL_SYMBOLS)
        for (i in names.indices) {
            var symbol = names[i]
            val heading = symbol == SS_HEADING
            var desc = descriptions[i]

            val block = BBX.BLOCK.STYLE.create(if (heading) SS_HEADING_STYLE else TPAGE_ELEMENT_STYLE)
            if (!heading) {
                val directElement = BBX.INLINE.EMPHASIS.create(EmphasisType.NO_TRANSLATE)
                if (prefix.isNotEmpty()) directElement.addAttribute(Attribute("prefix", prefix))
                if (!symbol.startsWith(prefix)) symbol = prefix + symbol
                directElement.appendChild(symbol)
                block.appendChild(directElement)
                desc = " $desc"
            } else {
                block.addAttribute(SS_HEADING_ATTR.copy())
            }
            block.appendChild(desc)
            ssPage.appendChild(block)
            //If first element is the prefix, mark it with an attribute
            if (i == 0 && includePrefix && prefix.isNotEmpty()) {
                block.addAttribute(Attribute("isPrefix", "true"))
            }
        }
        return ssPage
    }

    private fun addNewSS(
        comboSelection: Int,
        symbol: String,
        desc: String,
        editingItem: TableItem?,
        symbolsTable: Table,
        addDialog: Shell
    ) {
        if (comboSelection == 1 && desc.isEmpty()) {
            val alert = MessageBox(addDialog, SWT.ICON_ERROR or SWT.OK)
            alert.text = "Error"
            alert.message = "Headings cannot be blank"
            alert.open()
        } else {
            val newTableItem = editingItem ?: TableItem(symbolsTable, SWT.NONE)
            if (symbol.isNotEmpty()) {
                newTableItem.setText(arrayOf(if (comboSelection == 1) symbol else prefix + symbol, desc))
            }
            addDialog.close()
        }
    }

    private fun makeBBTextTitle(parent: Composite): BBStyleableText {
        val newText = BBStyleableText(
            parent, null, if (titlePageCentered) SWT.NONE else BBStyleableText.CUSTOM_MARGINS,
            SWT.BORDER or SWT.MULTI or SWT.V_SCROLL or SWT.WRAP
        )
        newText.setNewLineWrapStyle(TPAGE_ELEMENT_STYLE)
        return newText
    }

    private fun makeBBText(parent: Composite): BBStyleableText {
        return BBStyleableText(parent, null, 0, SWT.BORDER or SWT.MULTI or SWT.V_SCROLL or SWT.WRAP)
    }

    private val currentText: BBStyleableText?
        get() {
            for (text in texts) {
                if (text.text.isFocusControl) {
                    return text
                }
            }
            return null
        }

    companion object {
        private val localeHandler = getDefault()
        const val SWTBOT_OK_BUTTON: String = "tpagesDialog.ok"
        private const val BUTTON_WIDTH = 100
        private const val TEXT_WIDTH = 400
        private const val SHELL_HEIGHT = 500
        private const val TRANSCRIBER_NOTES_HEADING = "TRANSCRIBER'S NOTES"
        private const val SS_HEADING = "<Heading>"
        private const val TPAGE_HEADING_STYLE = "TPage Heading"
        private const val TPAGE_ELEMENT_STYLE = "TPageElement"
        private const val TPAGE_ELEMENT_STYLE2 = "TPageElement 3-1"
        private const val SS_PAGE_HEADING = "SPECIAL\u00A0SYMBOLS\u00A0USED IN\u00A0THIS\u00A0VOLUME"
        private const val SS_HEADING_STYLE = "Special Symbol Heading"
        private val SS_HEADING_ATTR = Attribute("heading", "true")
        private const val MAX_INDENT_LEVEL = 8
        private const val TEXT_HEIGHT = 100

        private val log: Logger = LoggerFactory.getLogger(TPagesDialog::class.java)

        private fun makeGroup(parent: Composite, text: String, maxColumns: Int): Group {
            val newGroup = Group(parent, SWT.NONE)
            newGroup.text = text
            newGroup.layout = GridLayout(maxColumns, false)
            return newGroup
        }
    }
}

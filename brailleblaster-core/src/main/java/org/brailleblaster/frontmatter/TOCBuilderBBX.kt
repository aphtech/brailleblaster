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

import com.google.common.collect.Iterators.peekingIterator
import com.google.common.collect.PeekingIterator
import nu.xom.*
import org.brailleblaster.BBIni.debugging
import org.brailleblaster.BBIni.propertyFileManager
import org.brailleblaster.abstractClasses.BBEditorView
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBXUtils
import org.brailleblaster.bbx.BBXUtils.ListStyleData
import org.brailleblaster.bbx.findBlock
import org.brailleblaster.bbx.getAncestorListLevel
import org.brailleblaster.bbx.isPageNumEffectively
import org.brailleblaster.frontmatter.VolumeUtils.VolumeData
import org.brailleblaster.frontmatter.VolumeUtils.getOrCreateTOC
import org.brailleblaster.frontmatter.VolumeUtils.getVolumeElements
import org.brailleblaster.frontmatter.VolumeUtils.getVolumeNames
import org.brailleblaster.math.mathml.MathModule.Companion.blockContainsMath
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.searcher.Searcher
import org.brailleblaster.perspectives.braille.toolbar.CustomToolBarBuilder.Companion.userDefined
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.XMLSelection
import org.brailleblaster.perspectives.mvc.XMLTextCaret
import org.brailleblaster.perspectives.mvc.events.BBViewListener
import org.brailleblaster.perspectives.mvc.events.BuildMenuEvent
import org.brailleblaster.perspectives.mvc.events.BuildToolBarEvent
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.perspectives.mvc.modules.views.DebugModule
import org.brailleblaster.settings.UTDManager
import org.brailleblaster.settings.UTDManager.Companion.isStyle
import org.brailleblaster.tools.MenuToolModule
import org.brailleblaster.utd.IStyle
import org.brailleblaster.utd.Style
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.internal.xml.XMLHandler2
import org.brailleblaster.utd.toc.TOCAttributes
import org.brailleblaster.utd.toc.TOCAttributes.Companion.removeAll
import org.brailleblaster.utd.utils.UTDHelper.Companion.getTextChild
import org.brailleblaster.utd.utils.UTDHelper.Companion.stripUTDRecursive
import org.brailleblaster.utils.xom.childNodes
import org.brailleblaster.exceptions.BBNotifyException
import org.brailleblaster.perspectives.mvc.menu.MenuManager
import org.brailleblaster.util.FormUIUtils.makeButton
import org.brailleblaster.util.FormUIUtils.makeCheckbox
import org.brailleblaster.util.FormUIUtils.makeComboDropdown
import org.brailleblaster.util.FormUIUtils.makeText
import org.brailleblaster.util.FormUIUtils.newLabel
import org.brailleblaster.util.Notify.notify
import org.brailleblaster.utils.swt.ButtonBuilder
import org.brailleblaster.utils.swt.EasySWT.getWidthOfText
import org.brailleblaster.utils.swt.EasySWT.makeEasyYesNoDialog
import org.brailleblaster.wordprocessor.WPManager.Companion.getInstance
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.VerifyKeyListener
import org.eclipse.swt.events.VerifyEvent
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Combo
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Text
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Consumer
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * TOC Builder toolbar updated for BBX format
 */
class TOCBuilderBBX(private var manager: Manager) : MenuToolModule, BBViewListener, VerifyKeyListener {
    /**
     * Note: instance variable to reset in between tests
     *
     * @see .isEnabled
     */
    override var enabled: Boolean = propertyFileManager.getPropertyAsBoolean(SETTING_ENABLED, false) // public for tests
    private var indentText: Text? = null
    private var runoverText: Text? = null
    private var pageNumberPrefix: Text? = null
    private var overrideLevel: Button? = null
    private var findPageCheck: Button? = null
    private var headerCombo: Combo? = null
    private var enableChanged: Boolean = false

    override val title: String
        get() = "TOC Builder"

    override val topMenu: TopMenu
        get() = TopMenu.DEBUG

    override fun onRun(bbData: BBSelectionData) {
        enabled = !enabled
        propertyFileManager.saveAsBoolean(SETTING_ENABLED, enabled)
        enableChanged = true
        getInstance().buildToolBar()
    }

    override fun onEvent(event: SimpleEvent) {
        if (event is BuildMenuEvent && DebugModule.enabled) {
            this.manager = event.manager.manager
            MenuManager.add(this)
        }
        if (event is BuildToolBarEvent) {
            if (enabled) {
                getInstance().currentPerspective.addToToolBar(
                    userDefined { parent: Composite -> this.buildToolbar(parent) }
                )
            }
        }
    }

    private fun buildToolbar(parent: Composite) {
        //Arbitrary high nesting that no real book should reach
        //TODO: numColumns is 0 by default, is this a bug?
        (parent.layout as GridLayout).numColumns = 99

        val entryButton: ButtonBuilder = makeButton(parent)
            .text("TOC Entry (F4)")
            .swtBotId(SWTBOT_TOC_ENTRY_BUTTON)
            .onSelection { pressedApplyTitle() }

        overrideLevel = makeCheckbox(parent)
            .text("Override Margin:")
            .onSelection {
                val isSelected: Boolean = overrideLevel!!.selection
                propertyFileManager.save(
                    SETTING_OVERRIDE_LEVEL,
                    if (isSelected) "true" else "false"
                )
                indentText!!.isEnabled = isSelected
                runoverText!!.isEnabled = isSelected
            }
            .selected(isOverrideLevelEnabled)
            .swtBotId(SWTBOT_OVERRIDE_LEVEL)
            .get()

        indentText = makeText(parent)
            .text(CUR_INDENT.toString())
            .swtBotId(SWTBOT_INDENT_TEXT)
            .gridDataWidth(getWidthOfText("WW"))
            .get()

        newLabel(parent, "-")

        runoverText = makeText(parent)
            .text(CUR_RUNOVER.toString())
            .swtBotId(SWTBOT_RUNOVER_TEXT)
            .gridDataWidth(getWidthOfText("WW"))
            .get()

        findPageCheck = makeCheckbox(parent)
            .text("Find Page | Page Prefix")
            .onSelection {
                val isSelected: Boolean = findPageCheck!!.selection
                propertyFileManager.save(
                    SETTING_PAGE_PREFIX_ENABLED,
                    if (isSelected) "true" else "false"
                )
                pageNumberPrefix!!.isEnabled = isSelected
            }
            .selected(isFindPageEnabled)
            .swtBotId(SWTBOT_PAGE_PREFIX_CHECK)
            .get()

        pageNumberPrefix = makeText(parent)
            .text(findPagePrefix)
            .onModify {
                propertyFileManager.save(
                    SETTING_PAGE_PREFIX,
                    pageNumberPrefix!!.text
                )
            }
            .swtBotId(SWTBOT_PAGE_PREFIX_TEXT)
            .gridDataWidth(getWidthOfText("WW"))
            .get()

        newLabel(parent, "|")

        headerCombo = makeComboDropdown(parent)
            .add("Headings...")
            .add("Centered")
            .add("Centered NB")
            .add("Cell 5")
            .add("Cell 7")
            .select(0)
            .swtBotId(SWTBOT_HEADING_COMBO)
            .get()

        makeButton(parent)
            .text("Apply Heading")
            .onSelection {
                pressedApplyHeading(
                    getHeadingText(
                        headerCombo!!.text
                    )!!
                )
            }
            .swtBotId(SWTBOT_HEADING_BUTTON)

        newLabel(parent, "|")

        makeButton(parent)
            .text("Page Number")
            .onSelection { pressedApplyPage() }
            .swtBotId(SWTBOT_PAGE_NUM_BUTTON)

        newLabel(parent, "|")

        makeButton(parent)
            .text("Volume Split")
            .onSelection { pressedInsertVolumeSplit() }
            .swtBotId(SWTBOT_VOLUME_SPLIT_BUTTON)

        makeButton(parent)
            .text("Disperse to Volumes")
            .onSelection {
                regenerateAllTOC(
                    manager.doc
                )
            }
            .swtBotId(SWTBOT_DISPERSE_VOLUMES_BUTTON)

        makeButton(parent)
            .text("X")
            .setAccessibleName("Close TOC Builder")
            .onSelection { close() }

        if (enableChanged) {
            entryButton.get().setFocus()
            enableChanged = false
        }
    }

    override fun verifyKey(event: VerifyEvent) {
        if (!enabled) {
            return
        }

        log.debug("event {} widget is text view {}", event, event.source === manager.textView)
        if (event.keyCode == SWT.F4) {
            pressedApplyTitle()
            event.doit = false
        } else if (event.character == '+' || event.character == '=') {
            if (event.stateMask == SWT.ALT) {
                val indent: Int = indent
                log.debug("running, old indent {}", indent)
                indentText!!.text = "" + (indent + 2)
                event.doit = false
            } else if (event.stateMask == (SWT.SHIFT or SWT.ALT)) {
                val runover: Int = runover
                log.debug("running, old runover {}", runover)
                runoverText!!.text = "" + (runover + 2)
                event.doit = false
            }
        } else if (event.character == '-') {
            if (event.stateMask == SWT.ALT) {
                val indent: Int = indent
                log.debug("running, old indent {}", indent)
                indentText!!.text = "" + (indent - 2)
                event.doit = false
            } else if (event.stateMask == (SWT.SHIFT or SWT.ALT)) {
                val runover: Int = runover
                log.debug("running, old runover {}", runover)
                runoverText!!.text = "" + (runover - 2)
                event.doit = false
            }
        } else if (event.keyCode == SWT.ESC.code) {
            close()
        }
    }

    override fun initializeListener(view: BBEditorView) {
        //braille view's caret is unpredictable
        if (view.view !== manager.brailleView) {
            view.view.addVerifyKeyListener(this)
        }
    }

    override fun removeListener(view: BBEditorView) {
        //braille view's caret is unsupported
        if (view.view !== manager.brailleView) {
            view.view.removeVerifyKeyListener(this)
        }
    }

    private val indent: Int
        get() {
            val indent: Int
            try {
                indent = indentText!!.text.toInt()
            } catch (e: NumberFormatException) {
                throw BBNotifyException("Indent is not a number", e)
            }
            if (indent % 2 != 1) {
                throw BBNotifyException("Invalid indent")
            }
            return indent
        }

    private val runover: Int
        get() {
            val runover: Int
            try {
                runover = runoverText!!.text.toInt()
            } catch (e: NumberFormatException) {
                throw BBNotifyException("Runover is not a number", e)
            }
            if (runover % 2 != 1) {
                throw BBNotifyException("Invalid runover")
            }
            return runover
        }

    private val isOverrideLevelEnabled: Boolean
        get() = propertyFileManager.getProperty(
            SETTING_OVERRIDE_LEVEL,
            "true"
        ) == "true"

    private val isFindPageEnabled: Boolean
        get() {
            return propertyFileManager.getProperty(
                SETTING_PAGE_PREFIX_ENABLED,
                "true"
            ) == "true"
        }

    private val findPagePrefixOrException: String
        get() {
            check(isFindPageEnabled) { "Attempting to use findPagePrefix but findPage is disabled" }
            return findPagePrefix
        }

    private val findPagePrefix: String
        get() {
            return propertyFileManager.getProperty(SETTING_PAGE_PREFIX, "")
        }

    private fun pressedApplyTitle() {
        manager.stopFormatting()
        //map to BBX margin levels
        val indentTarget: Int = BBXUtils.indentToLevel(indent)
        val runoverTarget: Int = BBXUtils.runoverToLevel(runover)
        if (indentTarget > runoverTarget) {
            throw BBNotifyException("Indent cannot be larger than runover")
        }
        log.debug("set indent {} runover {}", indentTarget, runoverTarget)

        val modifiedNodes: MutableList<Node> = ArrayList()

        for (curBlock: Element in manager.simpleManager.currentSelection.selectedBlocks) {
            if (blockContainsMath(curBlock)) {
                notify("Cannot make TOC from math", "Error")
                return
            }
        }

        for (curBlock: Element in manager.simpleManager.currentSelection.selectedBlocks) {
            if (curBlock.document == null) {
                continue
            }
            //do not TOCify page num tags
            if (curBlock.isPageNumEffectively()) {
                continue
            }
            log.debug("ToXML {}", curBlock.toXML())
            if (isSkippableTOCBlock(curBlock)) {
                continue
            }

            var indentBlock: Int? = null
            var runoverBlock: Int? = null
            if (BBX.BLOCK.LIST_ITEM.isA(curBlock)) {
                indentBlock = BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL.get(curBlock)
                runoverBlock = curBlock.getAncestorListLevel()
            } else if (BBX.BLOCK.MARGIN.isA(curBlock)) {
                indentBlock = BBX.BLOCK.MARGIN.ATTRIB_INDENT.get(curBlock)
                runoverBlock = BBX.BLOCK.MARGIN.ATTRIB_RUNOVER.get(curBlock)
            } else {
                val style: IStyle? = manager.getStyle(curBlock)
                if (style != null) {
                    val listStyle: ListStyleData? = BBXUtils.parseListStyle(style.name)
                    if (listStyle != null) {
                        indentBlock = BBXUtils.indentToLevel(listStyle.indent)
                        runoverBlock = BBXUtils.runoverToLevel(listStyle.runover)
                    }
                }
            }

            val blockStyle: Style =
                manager.document.settingsManager.engine.styleMap.findValueOrDefault(curBlock) as Style
            val tocStyleName: String = "TOC " + blockStyle.name
            if (!isOverrideLevelEnabled
                && isStyle(blockStyle, "Heading")
                && manager.document.settingsManager.engine.styleDefinitions.getStyleByName(tocStyleName) != null
            ) {
                applyTOC(curBlock, "heading", uTDManager, modifiedNodes)
                //TODO: do something else besides overrideStyle however BBX sets overrideStyle
                BBX._ATTRIB_OVERRIDE_STYLE.set(curBlock, tocStyleName)
            } else {
                applyTOC(curBlock, "title", uTDManager, modifiedNodes)

                //Add current level
                if (!isOverrideLevelEnabled && indentBlock != null) {
                    BBX.BLOCK.MARGIN.ATTRIB_INDENT.set(curBlock, indentBlock)
                    BBX.BLOCK.MARGIN.ATTRIB_RUNOVER.set(curBlock, runoverBlock)
                } else {
                    BBX.BLOCK.MARGIN.ATTRIB_INDENT.set(curBlock, indentTarget)
                    BBX.BLOCK.MARGIN.ATTRIB_RUNOVER.set(curBlock, runoverTarget)
                }

                //Issue #4335: autorunover
                if (isOverrideLevelEnabled && indentBlock != null) {
                    val ancestorList: Element? = XMLHandler.ancestorVisitorElement(
                        curBlock
                    ) { node: Element? -> BBX.CONTAINER.LIST.isA(node) }
                    if (ancestorList != null) {
                        //Issue #4701: Don't set runover lower than what it is as nested lists will break on L3-3
                        if (BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.get(ancestorList) < runoverTarget) {
                            BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.set(ancestorList, runoverTarget)
                            modifiedNodes.add(ancestorList)
                        }

                        FastXPath.descendant(ancestorList)
                            .filter { node ->
                                BBX.BLOCK.MARGIN.isA(node)
                                        && BBX.BLOCK.MARGIN.ATTRIB_RUNOVER.get(node as Element?) < runoverTarget
                            }
                            .onEach { e -> modifiedNodes.add(e) }
                            .forEach { node ->
                                BBX.BLOCK.MARGIN.ATTRIB_RUNOVER.set(
                                    node as Element?,
                                    runoverTarget
                                )
                            }
                    }
                }
                log.debug("updated margins of {}", curBlock.toXML())

                BBX.transform(curBlock, BBX.BLOCK.MARGIN)
                BBX.BLOCK.MARGIN.ATTRIB_MARGIN_TYPE.set(curBlock, BBX.MarginType.TOC)
            }
        }

        manager.simpleManager.dispatchEvent(
            org.brailleblaster.perspectives.mvc.events.ModifyEvent(
                Sender.TOC,
                modifiedNodes,
                true
            )
        )
    }

    private fun pressedApplyHeading(style: String?) {
        //You have to check first if they chose a heading.
        if (style == null) {
            throw BBNotifyException("No heading was selected.")
        }

        manager.stopFormatting()
        val modifiedNodes: MutableList<Node> = ArrayList()

        for (curBlock: Element in manager.simpleManager.currentSelection.selectedBlocks) {
            if (isSkippableTOCBlock(curBlock)) {
                continue
            }
            applyTOC(curBlock, "heading", uTDManager, modifiedNodes)
            BBX.transform(curBlock, BBX.BLOCK.STYLE)
            //TODO: do something else besides set overrideStyle
            BBX._ATTRIB_OVERRIDE_STYLE.set(curBlock, style)
        }
        manager.simpleManager.dispatchEvent(
            org.brailleblaster.perspectives.mvc.events.ModifyEvent(
                Sender.TOC,
                modifiedNodes,
                true
            )
        )
        headerCombo!!.select(0)
    }

    private fun pressedApplyPage() {
        manager.stopFormatting()
        var pageWrapElem: Element

        val previousTitleBlock: Element?
        val modifiedNodes: MutableList<Node> = ArrayList()
        val currentSelection: XMLSelection = manager.simpleManager.currentSelection
        if (currentSelection.start === currentSelection.end) {
            pageWrapElem = if (currentSelection.start.node is Element) {
                currentSelection.start.node as Element
            } else {
                //assume the user meant the whole piece of text
                //The element needs to be an actual block (it could be inside an INLINE)
                currentSelection.start.node.findBlock()
                //				pageWrapElem = (Element) currentSelection.start.node().getParent();
            }

            log.debug("initial {}", pageWrapElem.toXML())
            previousTitleBlock = isValidToMakePage(pageWrapElem, true)

            val parentBlock: Element = pageWrapElem.findBlock()
            stripUTDRecursive(parentBlock)
            modifiedNodes.add(parentBlock)

            if (unwrapPage(pageWrapElem)) {
                manager.simpleManager.dispatchEvent(
                    org.brailleblaster.perspectives.mvc.events.ModifyEvent(
                        Sender.TOC,
                        modifiedNodes,
                        true
                    )
                )
                return
            }
        } else if (currentSelection.start !is XMLTextCaret || currentSelection.end !is XMLTextCaret) {
            throw BBNotifyException("Can only select text")
        } else if (currentSelection.start.node !== currentSelection.end.node) {
            throw BBNotifyException("Selecting multiple pieces of text for page number is unsupported")
        } else {
            //is valid
            val selectionStart: XMLTextCaret = currentSelection.start
            val selectionEnd: XMLTextCaret = currentSelection.end
            val textNode: nu.xom.Text = selectionEnd.node
            val parentBlock: Element = textNode.findBlock()

            if (unwrapPage(textNode)) {
                stripUTDRecursive(parentBlock)
                modifiedNodes.add(parentBlock)
                manager.simpleManager.dispatchEvent(
                    org.brailleblaster.perspectives.mvc.events.ModifyEvent(
                        Sender.TOC,
                        modifiedNodes,
                        true
                    )
                )
                return
            }

            val isFullySelected: Boolean =
                selectionStart.offset == 0 && selectionEnd.offset >= selectionEnd.node.value.length
            previousTitleBlock = isValidToMakePage(textNode, isFullySelected)

            //Find the parent title
            val parentTitle: Element? = XMLHandler.ancestorVisitorElement(textNode
            ) { curNode: Element -> isTocTitle(curNode) }

            //Find any existing TOC pages
            val existingPage: Element? = if (parentTitle == null)
                null
            else
                XMLHandler.childrenRecursiveVisitor(parentTitle
                ) { curNode: Element -> isTocPage(curNode) }

            //Strip useless brl
            modifiedNodes.add(parentBlock)
            stripUTDRecursive(parentBlock)

            if (isFullySelected) {
                pageWrapElem = textNode.parent as Element
            } else {
                val textNodeToWrap: nu.xom.Text
                if (selectionStart.offset == 0) {
                    val splitTextNodes: List<nu.xom.Text> =
                        XMLHandler2.splitTextNode(
                            textNode,
                            selectionEnd.offset
                        )
                    textNodeToWrap = splitTextNodes[0]
                } else {
                    val splitTextNodes: List<nu.xom.Text> =
                        XMLHandler2.splitTextNode(
                            textNode,
                            selectionStart.offset,
                            selectionEnd.offset
                        )
                    textNodeToWrap = splitTextNodes[1]
                }

                if (existingPage != null) {
                    stripUTDRecursive(existingPage)
                    pageWrapElem = existingPage

                    textNodeToWrap.detach()
                    //Add a space in between
                    var text: String = textNodeToWrap.value
                    if (text[text.length - 1] != ' ') {
                        text = "$text "
                    }
                    //When you get the text child of the element here,
                    //consider that your previous toc page number may be emphasized
                    if (getTextChild(pageWrapElem).value.isEmpty()) {
                        textNodeToWrap.value = text
                        pageWrapElem.insertChild(textNodeToWrap, 0)
                    } else {
                        text += pageWrapElem.removeChild(getTextChild(pageWrapElem)).value
                        textNodeToWrap.value = text
                        pageWrapElem.appendChild(textNodeToWrap)
                    }
                } else {
                    pageWrapElem = BBX.SPAN.OTHER.create()
                    textNodeToWrap.parent.insertChild(
                        pageWrapElem,
                        textNodeToWrap.parent.indexOf(textNodeToWrap)
                    )
                    textNodeToWrap.detach()
                    pageWrapElem.appendChild(textNodeToWrap)
                }
            }
        }

        //Don't allow TOC attributes to be applied to blocks or containers
        log.info("Page wrap elem {}", pageWrapElem.toXML())
        if (BBX.SECTION.isA(pageWrapElem) || BBX.CONTAINER.isA(pageWrapElem)) {
            throw BBNotifyException("Cannot select sections or containers")
        } else if (BBX.BLOCK.isA(pageWrapElem)) {
            val spanWrapper: Element = BBX.SPAN.OTHER.create()
            while (pageWrapElem.childCount != 0) {
                val curChild: Node = pageWrapElem.getChild(0)
                curChild.detach()
                spanWrapper.appendChild(curChild)
            }
            pageWrapElem.appendChild(spanWrapper)
            pageWrapElem = spanWrapper
        } else if (BBX.INLINE.isA(pageWrapElem)) {
            val spanWrapper: Element = BBX.SPAN.OTHER.create()
            pageWrapElem.parent.replaceChild(pageWrapElem, spanWrapper)
            spanWrapper.appendChild(pageWrapElem)
            pageWrapElem = spanWrapper
        }

        setTOCType(pageWrapElem, "page", uTDManager)

        if (previousTitleBlock != null) {
            pageWrapElem.detach()
            previousTitleBlock.appendChild(pageWrapElem)
            // live fixer will cleanup block
        }

        modifiedNodes.add(pageWrapElem.findBlock())

        pageNumStripPrecedingText(pageWrapElem, modifiedNodes)

        manager.simpleManager.dispatchEvent(
            org.brailleblaster.perspectives.mvc.events.ModifyEvent(
                Sender.TOC,
                modifiedNodes,
                true
            )
        )
    }

    /**
     * @return Preceeding block to move page to
     */
    private fun isValidToMakePage(node: Node, isFullySelected: Boolean): Element? {
        var node: Node = node
        if (BBXUtils.isPageNumAncestor(node)) {
            throw BBNotifyException("Cannot convert print page num to TOC page number")
        }

        if (isFullySelected) {
            //Make sure that your node is its parent or else comparison below will be inaccurate
            node = node.findBlock()
        }

        // Find an associated title
        val ancestorTitle: Element? = XMLHandler.ancestorVisitorElement(
            node
        ) { curAncestor: Element? ->
            BBX.BLOCK.isA(curAncestor) && TOCAttributes.TYPE.inElement(
                curAncestor!!
            )
        }

        val previousBlock: Element? = FastXPath.preceding(node)
            .filter { node -> BBX.BLOCK.isA(node) }
            .map { node ->
                Searcher.Mappers.toElement(
                    node!!
                )
            }
            .firstOrNull()

        //If it's a number and
        if (ancestorTitle != null) {
            // Inside a title, make sure were not the only item in that title
            val titleIsFullySelected: Boolean = ancestorTitle === node && isFullySelected

            //If your title only has at most 1 child, find the previous block.
            //If the previous block does not contain a page number, you can use this title as a page.
            //Return previous block.
            if (titleIsFullySelected && ancestorTitle.childNodes
                    .filter { node: Node -> Searcher.Filters.noUTDAncestor(node) }
                    .size <= 1
            ) {
                if (previousBlock != null && previousBlock.childCount <= 1 && FastXPath.descendant(previousBlock)
                        .filterIsInstance<Element>()
                        .none { elem -> TOCAttributes.TYPE.inElement(elem) && TOCAttributes.TYPE.getValue(elem) == "page" }
                ) {
                    return previousBlock
                }

                throw BBNotifyException("Cannot convert entire title to TOC page number $isFullySelected")
            }
            return null
        } else {
            // Not inside a toc title, maybe one before this
            if (previousBlock == null || !TOCAttributes.TYPE.inElement(previousBlock)) {
                throw BBNotifyException("Cannot make TOC page without previous title")
            }

            return previousBlock
        }
    }

    private fun unwrapPage(cursor: Node): Boolean {
        val ancestorPageNum: Element? = FastXPath.ancestorOrSelf(cursor)
            .filterIsInstance<Element>()
            .firstOrNull { elem -> TOCAttributes.TYPE.inElement(elem) && TOCAttributes.TYPE.getValue(elem) == "page" }

        if (ancestorPageNum == null) {
            return false
        }

        removeAll(ancestorPageNum)
        return true
    }

    fun isSkippableTOCBlock(curBlock: Element): Boolean {
        if (curBlock.document == null || BBXUtils.isPageNumAncestor(curBlock)) {
            return true
        }

        //Ignore blocks that just contain a page number
        val textNodes: List<Node> = FastXPath.descendant(curBlock)
            .filterIsInstance<nu.xom.Text>()
        if (textNodes.isEmpty()) {
            return true
        } else if (textNodes.size > 1) {
            return false
        }
        return BBXUtils.isPageNumAncestor(textNodes[0])
    }

    private fun applyTOC(block: Element, type: String, utdMan: UTDManager, modifiedNodes: MutableList<Node>) {
        modifiedNodes.add(block)
        if (isFindPageEnabled) {
            applyTOCPage(block, modifiedNodes, utdMan)
        }
        setTOCType(block, type, utdMan)
    }

    /**
     * Wrap page number in element with toc-type=page
     */
    private fun applyTOCPage(block: Element, modifiedNodes: MutableList<Node>, utdMan: UTDManager) {
        var pageStart: Int
        var moveableBlock: Element? = null
        var lastTextNode: nu.xom.Text?
        log.debug("curBlock {}", block.toXML())

        //Strip BRL or the xpath will find text inside it
        stripUTDRecursive(block)
        modifiedNodes.add(block)

        // do not re-wrap page numbers
        if (FastXPath.descendant(block)
                .filterIsInstance<Element>()
                .any { elem -> TOCAttributes.TYPE.inElement(elem) && TOCAttributes.TYPE.getValue(elem) == "page" }
        ) {
            return
        }

        lastTextNode = FastXPath.descendant(block)
            .filterIsInstance<nu.xom.Text>()
            .lastOrNull { node -> !BBXUtils.isPageNumAncestor(node) }
        log.debug("last Text node: {}", lastTextNode)
        if (lastTextNode == null) {
            // fail silently as it may be a page number or empty
            return
        }

        pageStart = indexOfPage(lastTextNode.value)
        log.debug("pageStart {}", pageStart)
        if (pageStart == -1) {
            val nextSiblingNode: Node? = XMLHandler.nextSiblingNode(block)
            if (nextSiblingNode != null && BBX.BLOCK.isA(nextSiblingNode)
                && !BBXUtils.isPageNumAncestor(nextSiblingNode)
            ) {
                val nextSibling: Element = nextSiblingNode as Element
                stripUTDRecursive(nextSibling)
                modifiedNodes.add(nextSibling)

                //Find a text node
                lastTextNode = FastXPath.descendant(nextSibling)
                    .filterIsInstance<nu.xom.Text>().lastOrNull { node -> !BBXUtils.isPageNumAncestor(node) }

                if (lastTextNode == null) {
                    // fail silently as it may be a page number or empty
                    return
                } else {
                    moveableBlock = isPageMovable(lastTextNode)
                    if (moveableBlock == null) {
                        // fail silently as it may be a page number or empty
                        return
                    }
                }

                // Only give last word
                pageStart = indexOfPage(lastTextNode.value)
                if (pageStart != -1 && !lastTextNode.value.substring(0, pageStart).isBlank()) {
                    // found text before page in next entry
                    log.debug("ignoring as most likely found another toc entry")
                    return
                }
            }
        }

        log.debug("Parsed page result: {}", pageStart)
        if (pageStart == -1) {
            log.debug("page not found")
            return
        }

        val nodeToWrap: nu.xom.Text
        if (pageStart != 0) {
            if (lastTextNode.value.substring(0, pageStart).trim { it <= ' ' }.isEmpty()) {
                // Going to wrap entire text node, only need to cleanup whitespace
                lastTextNode.value = lastTextNode.value.trim { it <= ' ' }
                nodeToWrap = lastTextNode
            } else {
                //Part of a full sentence, need to wrap in a TOC Page
                //Trim spaces as it will mess up the TOC Formatter

                val title: String = lastTextNode.value.substring(0, pageStart).trim { it <= ' ' }
                val page: String = lastTextNode.value.substring(pageStart).trim { it <= ' ' }

                lastTextNode.value = title

                nodeToWrap = Text(page)
                lastTextNode.parent.insertChild(nodeToWrap, lastTextNode.parent.indexOf(lastTextNode) + 1)

                if (lastTextNode.value.isBlank()) {
                    //Page was surrounded by spaces
//					lastTextNode.detach();
                    throw RuntimeException("page still surrounded by spaces?")
                }
            }
        } else {
            //Going to wrap the entire text node
            nodeToWrap = lastTextNode

            //Rel 7254: If current block contains only this node, use it as a title, not a page.
            //Page should not have a blank title, but the title can live without a page.
            if (nodeToWrap.findBlock().childCount == 1 && moveableBlock == null) {
                return
            }
        }

        //Re-use page parent if possible for cleanliness
        val newPageWrapper: Element
        val parent = nodeToWrap.parent
        when (parent.childCount) {
            1 if BBX.SPAN.isA(parent) -> {
                newPageWrapper = parent as Element
            }
            1 if BBX.INLINE.isA(parent) -> {
                newPageWrapper = BBX.SPAN.OTHER.create()
                XMLHandler2.wrapNodeWithElement(
                    parent,
                    newPageWrapper
                )
            }
            else -> {
                newPageWrapper = BBX.SPAN.OTHER.create()
                XMLHandler2.wrapNodeWithElement(
                    nodeToWrap,
                    newPageWrapper
                )
                log.trace("Wrapped with element in {}", newPageWrapper.parent.toXML())
            }
        }
        log.trace("Applying page attrib to {}", newPageWrapper.toXML())
        setTOCType(newPageWrapper, "page", utdMan)

        //Move page num if in subsequent block
        if (newPageWrapper.document == null) {
            throw NodeException("Node not attached to document", newPageWrapper)
        }

        BBX.SPAN.assertIsA(newPageWrapper)
        if (moveableBlock != null) {
            val movableChild: Node = moveableBlock.getChild(0)
            movableChild.detach()
            block.appendChild(movableChild)
            moveableBlock.detach()
            modifiedNodes.remove(moveableBlock)
        }
        if (newPageWrapper.document == null) {
            throw NodeException("Node not attached to document " + newPageWrapper.toXML(), block)
        }

        pageNumStripPrecedingText(newPageWrapper, modifiedNodes)
    }

    /**
     * Parse integers and roman numerals or return -1 if not found.
     */
    private fun indexOfPage(rawText: String): Int {
        if (rawText.isBlank()) {
            return -1
        }

        // Get last word between spaces (most likely to be the page number) if given a full piece of text
        val start: Int = if (rawText.contains(" "))
            rawText.lastIndexOf(' ') + 1
        else
            0
        var end: Int = rawText.indexOf(' ', start)
        if (end == -1) {
            end = rawText.length
        }
        val lastWord: String = rawText.substring(start, end)

        val pagePrefix: String = findPagePrefixOrException
        val result: String? = if (pagePrefix.isEmpty()) {
            parsePageNumber(lastWord)
        } else {
            val textToParse: String = lastWord.trimStart(*(pagePrefix.toCharArray()))
            if (textToParse == lastWord) {
                // User entered prefix but text has no prefix
                parsePageNumber(rawText)
            } else {
                parsePageNumber(textToParse)
            }
        }

        if (result == null) {
            return -1
        }
        return start
    }

    private fun parsePageNumber(input: String): String? {
        try {
            //Might be a number
            return "" + input.toInt()
        } catch (_: NumberFormatException) {
            //Might be a roman numeral
            val matcher: Matcher = MATCH_ROMAN_NUMERALS.matcher(input)
            val found: Boolean = matcher.find()
            log.debug("Input '{}' Matcher {} matched {}", input, matcher, found)
            if (found) {
                return matcher.group()
            }
        }
        return null
    }

    private fun pageNumStripPrecedingText(pageNum: Node, modifiedNodes: MutableList<Node>) {
        if (pageNum.document == null) {
            throw NodeException("Node not attached to doc", pageNum)
        }
        //Need to strip space so it doesn't affect formatting of title or line wrapping
        val precedingText: nu.xom.Text = FastXPath.preceding(pageNum).filterIsInstance<nu.xom.Text>().first()
        if (precedingText.value.endsWith(" ")) {
            precedingText.value = precedingText.value.trimEnd()
            if (precedingText.value.isEmpty()) {
                precedingText.detach()
            } else {
                modifiedNodes.add(precedingText)
            }
        }
    }

    fun pressedInsertVolumeSplit() {
        manager.stopFormatting()
        val modifiedNodes: MutableList<Element> = ArrayList()

        modifiedNodes.add(
            insertVolumeSplitBeforeParentBlock(
                manager.simpleManager.currentCaret.node
            )
        )

        val firstTOCNode: Element = Objects.requireNonNull(
            FastXPath.descendantFindFirst(
                manager.doc
            ) { curNode: Node? ->
                if (curNode !is Element) {
                    return@descendantFindFirst true
                }
                val curElem: Element = curNode
                if (BBX.BLOCK.TOC_VOLUME_SPLIT.isA(curElem)) {
                    return@descendantFindFirst false
                } else {
                    val attribute: Attribute? = TOCAttributes.TYPE.getAttribute(curElem)
                    return@descendantFindFirst attribute == null
                }
            }
        ) as Element
        if (TOCAttributes.TYPE.getAttribute(firstTOCNode) != null) {
            log.debug("firstTOCNode: {}", firstTOCNode.toXML())
            modifiedNodes.add(insertVolumeSplitBeforeParentBlock(firstTOCNode))
        }

        manager.simpleManager.dispatchEvent(
            org.brailleblaster.perspectives.mvc.events.ModifyEvent(
                Sender.TOC,
                modifiedNodes,
                true
            )
        )
    }

    private fun regenerateAllTOC(doc: Document) {
        manager.stopFormatting()
        val volumes = getVolumeElements(doc)
        if (volumes.isEmpty()) {
            throw BBNotifyException("Must create volumes first")
        }
        val volumeSplitsNum = FastXPath.descendant(doc)
            .count { node ->
                BBX.BLOCK.TOC_VOLUME_SPLIT.isA(node) && XMLHandler.ancestorElementNot(
                    node
                ) { node: Element? -> BBX.CONTAINER.VOLUME_TOC.isA(node) }
            }
        if (volumeSplitsNum == 0) {
            throw BBNotifyException("No TOC Volume Splits defined")
        } else if (volumeSplitsNum != volumes.size) {
            if (debugging) {
                test_diffTOCSplitsThanVolumes = true
                log.warn("More TOC Volumes than actual volumes")
            } else if (!makeEasyYesNoDialog(
                    "TOC Warning",
                    ("Defined " + volumeSplitsNum + " TOC Volumes but document has " + volumes.size + " actual volumes."
                            + (if (volumeSplitsNum > volumes.size)
                        " Additional TOC volumes will be added to last available volume."
                    else
                        "") + " Are you sure you want to disperse TOC entries?"),
                    manager.wpManager.shell
                )
            ) {
                return
            }
        }

        val modifiedElements: MutableList<Element> = ArrayList()

        //Step 1: Strip all existing autogenerated TOC so search doesn't find them
        //TODO: This is probably slow as all volume TOCs must be retranslated
        val volumeTocContainers: MutableList<Element> = ArrayList()
        for (curVolume: Element in volumes.dropLast(1)) {
            val tocContainer: Element = getOrCreateTOC(curVolume)
            tocContainer.removeChildren()
            log.debug("toc container: {}",
                XMLHandler2.toXMLSimple(tocContainer)
            )

            volumeTocContainers.add(tocContainer)
        }

        //Step 2: Find all TOC elements
        val volumeData: List<VolumeData> = getVolumeNames(volumes)
        val volumeDataItr: Iterator<VolumeData> = volumeData.iterator()
        var tocElements: List<Element> = FastXPath.descendantFindList(doc
        ) { results: List<Element>, curNode: Node? ->
            if (curNode !is Element) {
                return@descendantFindList false
            }
            val curElem: Element = curNode

            if (BBX.BLOCK.TOC_VOLUME_SPLIT.isA(curElem)) {
                //Step 2.1: Update all TOC Volume Splits removing potential placeholder/out of date text
                if (!volumeDataItr.hasNext()) {
                    return@descendantFindList false
                }

                curElem.removeChildren()

                // Issue #5468: Formats 2016 requires transcriber note indicators
                // which are now added by wrapping the word in an transcriber note emphasis
                curElem.appendChild(BBXUtils.wrapAsTransNote(volumeDataItr.next().nameLong))
                modifiedElements.add(curElem)
                return@descendantFindList true
            }

            if (TOCAttributes.TYPE.getAttribute(curElem) == null) {
                return@descendantFindList false
            }
            !XMLHandler.ancestorElementIs(
                curElem
            ) { o: Element -> results.contains(o) }
        }

        //Step 3: Copy TOC entries to subsequent volumes
        //Remove first volume's elements as they are not copied to the second volume
        tocElements.forEach(Consumer { tocElement: Element -> log.info("before toc element: {}", tocElement.toXML()) })
        tocElements = tocElements.subList(
            tocElements.indexOf(
                tocElements
                    .filter { node -> BBX.BLOCK.TOC_VOLUME_SPLIT.isA(node) }
                    .drop(1)
                    .firstOrNull() ?: tocElements[0]
            ),
            tocElements.size
        )
        tocElements.forEach(Consumer { tocElement: Element ->
            log.info(
                "Registered toc element: {}",
                tocElement.toXML()
            )
        })
        val tocItr: PeekingIterator<Element> = peekingIterator(tocElements.iterator())

        for (i in volumes.indices) {
            if (!tocItr.hasNext()) {
                break
            }

            val tocContainer: Element = volumeTocContainers[i]
            modifiedElements.add(tocContainer)
            tocContainer.removeChildren()
            log.info("toc {}", tocContainer.toXML())

            //Also copy pagenum tags to retain origional print page
            var previousPageNum: Element? = findAndAppendPrecedingPageNumber(tocItr.peek(), tocContainer, null)

            //Copy volume split if exists or first element of volume if not due to TOCSplitsSize != VolumeSize
            run {
                val volumeSplit: Element = tocItr.next()
                // Formats § 2.10.10 b
                if (!BBX.BLOCK.TOC_VOLUME_SPLIT.isA(volumeSplit)) {
                    tocContainer.appendChild(volumeSplit.copy())
                }
            }

            while (tocItr.hasNext() && !BBX.BLOCK.TOC_VOLUME_SPLIT.isA(tocItr.peek())) {
                previousPageNum = findAndAppendPrecedingPageNumber(tocItr.peek(), tocContainer, previousPageNum)
                val nextTOCEntry: Element = tocItr.next().copy()
                tocContainer.appendChild(nextTOCEntry)
            }
        }

        modifiedElements.addAll(
            tocElements
                .filter { node: Element? -> BBX.BLOCK.TOC_VOLUME_SPLIT.isA(node) }
        )
        manager.simpleManager.dispatchEvent(
            org.brailleblaster.perspectives.mvc.events.ModifyEvent(
                Sender.TOC,
                modifiedElements,
                true
            )
        )
    }

    private fun findAndAppendPrecedingPageNumber(
        tocEntry: Element,
        tocContainer: Element,
        previousPageNum: Element?
    ): Element? {
        val previousPageNumQuery: Nodes = XMLHandler2.query(
            tocEntry,
            "preceding::*[@{}:{}='{}'][1]",
            BBX._ATTRIB_TYPE.nsPrefix,
            BBX._ATTRIB_TYPE.name,  //Will also work for BBX.BLOCK.PAGE_NUM
            BBX.SPAN.PAGE_NUM.name
        )
        if (previousPageNumQuery.size() != 0) {
            val pageNumElem: Element = previousPageNumQuery.get(0) as Element
            if (pageNumElem === previousPageNum) {
                return previousPageNum
            }
            val pageNumToAppend: Element = pageNumElem.copy()
            if (BBX.SPAN.PAGE_NUM.isA(pageNumElem)) {
                BBX.transform(pageNumToAppend, BBX.BLOCK.PAGE_NUM)
            }
            tocContainer.appendChild(pageNumToAppend)
            return pageNumElem
        } else {
            return previousPageNum
        }
    }

    private val uTDManager: UTDManager
        get() {
            return manager.document.settingsManager
        }

    private fun close() {
        enabled = false
        propertyFileManager.saveAsBoolean(SETTING_ENABLED, enabled)
        getInstance().buildToolBar()
    }

    private fun getHeadingText(headingText: String): String? {
        return when (headingText) {
            "Centered" -> "TOC Centered Heading"
            "Centered NB" -> "TOC Centered Heading NB"
            "Cell 5" -> "TOC Cell 5 Heading"
            "Cell 7" -> "TOC Cell 7 Heading"
            else -> null
        }
    }

    companion object {
        const val SETTING_OVERRIDE_LEVEL: String = "tocBuilderBBX.overrideLevelEnabled"
        const val SETTING_PAGE_PREFIX_ENABLED: String = "tocBuilderBBX.pagePrefixEnabled"
        const val SETTING_PAGE_PREFIX: String = "tocBuilderBBX.pagePrefix"
        const val SETTING_ENABLED: String = "tocBuilderBBX.enabled"
        const val SWTBOT_TOC_ENTRY_BUTTON: String = "tocBuilderBBX.tocEntry"
        const val SWTBOT_PAGE_NUM_BUTTON: String = "tocBuilderBBX.pageNum"
        const val SWTBOT_PAGE_PREFIX_CHECK: String = "tocBuilderBBX.pagePrefixCheck"
        const val SWTBOT_PAGE_PREFIX_TEXT: String = "tocBuilderBBX.pagePrefix"
        const val SWTBOT_OVERRIDE_LEVEL: String = "tocBuilderBBX.overrideLevel"
        const val SWTBOT_INDENT_TEXT: String = "tocBuilderBBX.indent"
        const val SWTBOT_RUNOVER_TEXT: String = "tocBuilderBBX.runover"
        const val SWTBOT_HEADING_COMBO: String = "tocBuilderBBX.headingCombo"
        const val SWTBOT_VOLUME_SPLIT_BUTTON: String = "tocBuilderBBX.volumeSplit"
        const val SWTBOT_DISPERSE_VOLUMES_BUTTON: String = "tocBuilderBBX.regenerateVolumes"
        const val SWTBOT_HEADING_BUTTON: String = "tocBuilerBBX.headingButton"
        private val log: Logger = LoggerFactory.getLogger(TOCBuilderBBX::class.java)
        val MATCH_ROMAN_NUMERALS: Pattern = Pattern
            .compile("(?i)^M{0,4}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$", Pattern.CASE_INSENSITIVE)
        private const val CUR_INDENT: Int = 1
        private const val CUR_RUNOVER: Int = 3
        @JvmField
        var test_diffTOCSplitsThanVolumes: Boolean = false

        /**
         * Ancestor Block that can be unwrapped and moved
         */
        @JvmStatic
        fun isPageMovable(input: Node): Element? {
            if (input.document == null) {
                throw NodeException("Node not attached to document", input)
            }
            var curParent: Element = input.parent as Element
            var isMovable = true
            var movableBlock: Element? = null
            while (true) {
                if (curParent.childCount != 1) {
                    isMovable = false
                }
                if (BBX.BLOCK.isA(curParent)) {
                    movableBlock = curParent
                    break
                }
                if (curParent === input.document.rootElement) {
                    break
                }
                curParent = curParent.parent as Element
            }
            //You don't have to move if it's the same parent
            if (!isMovable) {
                return null
            }
            BBX.BLOCK.assertIsA(movableBlock)
            return movableBlock
        }

        private fun insertVolumeSplitBeforeParentBlock(someNode: Node): Element {
            //Find usable block
            val block: Element = XMLHandler.ancestorVisitorElement(
                someNode
            ) { node: Element? -> BBX.BLOCK.isA(node) }!!

            //Insert before
            val split: Element = BBX.BLOCK.TOC_VOLUME_SPLIT.create()
            split.appendChild("TOC Volume split placeholder")
            val blockParent: ParentNode = block.parent
            blockParent.insertChild(split, blockParent.indexOf(block))

            return split
        }

        private fun setTOCType(elem: Element, type: String, utdMan: UTDManager) {
            TOCAttributes.TYPE.getAttribute(elem)?.detach()
            BBXUtils.stripStyle(elem, utdMan)
            TOCAttributes.TYPE.add(elem, type)
        }

        @JvmStatic
        fun isEnabled(m: Manager): Boolean {
            return m.simpleManager.getModule(
                    TOCBuilderBBX::class.java
                )!!.enabled
        }

        private fun isTocTitle(curNode: Node): Boolean {
            return isTocElement(curNode, "title")
        }

        private fun isTocPage(curNode: Node): Boolean {
            return isTocElement(curNode, "page")
        }

        private fun isTocElement(curNode: Node, key: String): Boolean {
            if (curNode !is Element) {
                return false
            }

            val tocTypeAttrib: Attribute? = TOCAttributes.TYPE.getAttribute(curNode)
            return tocTypeAttrib != null && tocTypeAttrib.value == key
        }
    }
}

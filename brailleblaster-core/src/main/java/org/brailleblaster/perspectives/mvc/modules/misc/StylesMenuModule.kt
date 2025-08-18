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
package org.brailleblaster.perspectives.mvc.modules.misc

import nu.xom.*
import nu.xom.Text
import org.brailleblaster.BBIni.debugging
import org.brailleblaster.BBIni.propertyFileManager
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBX.ListType
import org.brailleblaster.bbx.BBXUtils
import org.brailleblaster.bbx.findBlock
import org.brailleblaster.bbx.findBlockChildOrNull
import org.brailleblaster.bbx.findBlockOrNull
import org.brailleblaster.math.mathml.MathModule
import org.brailleblaster.math.mathml.MathModule.Companion.isMath
import org.brailleblaster.math.mathml.MathModule.Companion.isSpatialMath
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.BBSimpleManager.SimpleListener
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.XMLNodeCaret
import org.brailleblaster.perspectives.mvc.XMLSelection
import org.brailleblaster.perspectives.mvc.XMLSelection.Companion.isValidTreeSelection
import org.brailleblaster.perspectives.mvc.XMLTextCaret
import org.brailleblaster.perspectives.mvc.events.BuildMenuEvent
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.menu.*
import org.brailleblaster.perspectives.mvc.modules.misc.TableSelectionModule.Companion.displayInvalidTableMessage
import org.brailleblaster.perspectives.mvc.modules.views.EmphasisModule.addEmphasis
import org.brailleblaster.settings.UTDManager.Companion.hasUtdStyleTag
import org.brailleblaster.settings.UTDManager.Companion.isStyle
import org.brailleblaster.tools.MenuToolModule
import org.brailleblaster.utd.Style
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.internal.xml.XMLHandler2
import org.brailleblaster.utd.properties.EmphasisType
import org.brailleblaster.utd.utils.TableUtils
import org.brailleblaster.utd.utils.TableUtils.isTableCopy
import org.brailleblaster.utd.utils.UTDHelper.Companion.stripUTDRecursive
import org.brailleblaster.utd.utils.dom.BoxUtils.unbox
import org.brailleblaster.exceptions.BBNotifyException
import org.brailleblaster.perspectives.mvc.menu.BBSeparator
import org.brailleblaster.util.Notify
import org.brailleblaster.util.Notify.notify
import org.brailleblaster.utils.BB_NS
import org.brailleblaster.utils.UTD_NS
import org.brailleblaster.utils.swt.EasySWT.setSizeAndLocationMiddleScreen
import org.brailleblaster.wordprocessor.WPManager
import org.brailleblaster.wordprocessor.WPManager.Companion.getInstance
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.events.SelectionListener
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.math.max

class StylesMenuModule(private val m: Manager) : SimpleListener {
    inner class RepeatStyleTool : MenuToolModule {
        override val topMenu: TopMenu
            get() = TopMenu.STYLES
        override val title: String
            get() = "Repeat Last Style"
        override val accelerator: Int
            get() = SWT.CTRL or 'R'.code
        override val sharedItem: SharedItem
            get() = SharedItem.REPEAT_LAST_STYLE

        override fun onRun(bbData: BBSelectionData) {
            repeatStyle()
        }
    }

    /**
     * Last style, reference by ID as the instance could have been replaced
     */
    private var lastStyleId: String? = null

    override fun onEvent(event: SimpleEvent) {
        if (event is BuildMenuEvent) {
            MenuManager.add(RepeatStyleTool())
            MenuManager.add(BBSeparator(TopMenu.STYLES))
            val smb = StyleMenuBuilder(getInstance().shell, m)
            smb.generateStylesMenu(
                { s: BBStyleSelection -> this.update(s) },
                { t: BBActionSelection -> this.updateAction(t) },
                { option: BBStyleOptionSelection -> this.updateStyleOption(option) },
                { e: BBSelectionData -> e.manager.simpleManager.initMenu(getInstance().shell) })
        }
    }

    private fun update(s: BBStyleSelection) {
        val newNodes: List<Node> = updateStyle(s.style as Style)
        if (newNodes.isNotEmpty()) {
            if (!(s.style.name == TRNOTE_ACTION_STYLE_NAME
                        || s.style.name == DESCRIPTION_STYLE_NAME)
            ) {
                reformat(newNodes)
            } else updateAction(BBActionSelection(EmphasisType.TRANS_NOTE, s.widget))
        }
    }

    private fun getColorName(s: Style): Shell {
        log.debug("Color box")
        val shell = Shell(WPManager.display, SWT.BORDER or SWT.SHELL_TRIM or SWT.SYSTEM_MODAL)
        setSizeAndLocationMiddleScreen(shell, 300, 150)
        shell.layoutData = GridData(4, 4, true, true)
        shell.layout = GridLayout(1, false)
        val l = Label(shell, 0)
        l.text = "Enter Color: "
        l.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        val text = Combo(shell, SWT.RESIZE or SWT.BORDER or SWT.H_SCROLL or SWT.V_SCROLL)


        val boxString = propertyFileManager.getProperty(COLOR_PROP, "")
        val boxes: Array<String>
        val array = mutableListOf<String>()
        if (boxString.isNotEmpty()) {
            boxes = boxString.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (box in boxes) {
                if (!array.contains(box)) {
                    array.add(box)
                }
            }
            for (i in array) { //Why on earth was this a sublist from 0,10 instead of a foreach?
                text.add(i)
            }
        }
        val textData = GridData(SWT.FILL, SWT.FILL, true, true)
        textData.widthHint = 100
        textData.minimumWidth = 100
        text.data = textData
        val g = Group(shell, 0)
        g.data = GridData(4, 4, true, true)
        g.layout = GridLayout(2, true)
        val ok = Button(g, SWT.PUSH)
        ok.text = "OK"
        ok.layoutData = GridData(4, 4, true, true)
        ok.addSelectionListener(object : SelectionListener {
            override fun widgetSelected(e: SelectionEvent) {
                if (text.text.isEmpty()) {
                    notify("You must enter a color or cancel", Notify.ALERT_SHELL_NAME)
                } else {
                    val color = text.text
                    val sb = StringBuilder()
                    array.iterator().forEachRemaining { z: String ->
                        if (z.isNotEmpty()) {
                            sb.append(z).append(",")
                        }
                    }
                    propertyFileManager.save(COLOR_PROP, "$color,$sb")
                    s.setColor(color)
                    if (m.simpleManager.currentSelection.start.node
                        == m.simpleManager.currentSelection.end.node
                    ) {
                        val n = boxBlock(m.simpleManager.currentSelection.start.node, s)
                        reformat(listOf(n))
                    } else {
                        val n = boxMultiBlocks(
                            m.simpleManager.currentSelection.start.node,
                            m.simpleManager.currentSelection.end.node, s
                        )!!
                        reformat(listOf(n))
                    }

                    shell.close()
                }
            }

            override fun widgetDefaultSelected(e: SelectionEvent) {
            }
        })
        val cancel = Button(g, SWT.PUSH)
        cancel.text = "Cancel"
        cancel.layoutData = GridData(4, 4, true, true)
        cancel.addSelectionListener(object : SelectionListener {
            override fun widgetSelected(e: SelectionEvent) {
                shell.close()
            }

            override fun widgetDefaultSelected(e: SelectionEvent) {
                shell.close()
            }
        })
        shell.defaultButton = ok
        shell.pack()
        return shell
    }

    private fun repeatStyle() {
        if (lastStyleId == null) {
            throw BBNotifyException("Must apply a style first before it can be repeated")
        }
        val styleToApply =
            m.document.settingsManager.engine.styleDefinitions.styles.firstOrNull { style: Style -> style.id == lastStyleId }
                ?: throw RuntimeException(
                    "Unable to find lastStyleId $lastStyleId"
                )
        val modifiedNodes = updateStyle(styleToApply)
        if (modifiedNodes.isNotEmpty()) {
            m.simpleManager.dispatchEvent(ModifyEvent(Sender.EMPHASIS, modifiedNodes, true))
        }
    }

    private fun updateStyle(style: Style): List<Node> {
        //Update before updating style
        m.checkForUpdatedViews()
        val currentSelection = m.simpleManager.currentSelection
        var startSelection = currentSelection.start.node
        var endSelection = currentSelection.end.node

        if (isTableSelected) {
            if (isAlwaysWrapStyle(style)) {
                val startTableParent = Manager.getTableParent(startSelection)
                //isTableSelected() can return true if the parent is a container with a table under it
                if (startTableParent != null) {
                    endSelection = startTableParent
                    startSelection = endSelection
                    detachTableCopy(startSelection)
                }
                if (startSelection !== endSelection) {
                    val endTableParent = Manager.getTableParent(endSelection)
                    if (endTableParent != null) {
                        endSelection = endTableParent
                        detachTableCopy(endSelection)
                    }
                }
            } else {
                warnTable()
                return ArrayList()
            }
        } else if (isSpatialMath(startSelection) ||
            isSpatialMath(endSelection)
        ) {
            if (style.id != "miscellaneous/doubleLine") {
                notify(MathModule.SPATIAL_MATH_WARNING, Notify.ALERT_SHELL_NAME)
                return ArrayList()
            }
        }

        m.stopFormatting()
        lastStyleId = style.id
        return applyStyle(style, startSelection, endSelection)
    }

    private fun updateAction(t: BBActionSelection) {
        if (isTableSelected) {
            warnTable()
            return
        }
        //TODO: This should be removed when TRNote/Transcriber note is taken out of the styles menu
        addEmphasis(m.simpleManager, t.action)
    }

    fun updateStyleOption(option: BBStyleOptionSelection) {
        //Update before updating style option
        m.checkForUpdatedViews()
        val currentSelection = m.simpleManager.currentSelection
        var startNode: Node? = currentSelection.start.node
        if (Manager.getTableParent(startNode) != null) {
            startNode = Manager.getTableParent(startNode)
        }
        var endNode: Node? = currentSelection.end.node
        if (Manager.getTableParent(endNode) != null) {
            endNode = Manager.getTableParent(endNode)
        }
        applyStyleOptionAndReformat(option, startNode, endNode)
    }


    fun applyStyleOptionAndReformat(option: BBStyleOptionSelection, start: Node?, end: Node?) {

        val blocks = getBlocks(start, end)
        val utdMan = m.document.settingsManager
        for (block in blocks) {
            utdMan.applyStyleWithOption(
                Objects.requireNonNull(utdMan.engine.getStyle(block)) as Style,
                option.option,
                option.value,
                block
            )
        }
        m.simpleManager.dispatchEvent(ModifyEvent(Sender.SIMPLEMANAGER, ArrayList(blocks), true))
    }

    fun boxBlock(start: Node, style: Style?): Node {
        val block = if ((BBX.CONTAINER.TABLE.isA(start) || BBX.CONTAINER.LIST.isA(start)
                    || BBX.CONTAINER.BOX.isA(start))
        ) start as Element else start.findBlock()
        XMLHandler2.wrapNodeWithElement(
            block,
            BBX.CONTAINER.BOX.create()
        )
        m.document.settingsManager.applyStyle(style, (block.parent as Element))
        return (block.parent)
    }

    fun boxMultiBlocks(start: Node, end: Node, style: Style): Node? {
        var b1 = if (isContainer(start)) start as Element else start.findBlock()
        var b2 = if (isContainer(end)) end as Element else end.findBlock()

        var tableParent = Manager.getTableParent(b1)
        if (tableParent != null) b1 = tableParent
        tableParent = Manager.getTableParent(b2)
        if (tableParent != null) b2 = tableParent
        val selectedSiblings = if (style.name == "Box" || style.name == "Color Box") {
            getBoxSiblings(b1, b2) // added for RT 6395
        } else {
            isValidTreeSelection(b1, b2) // if else added for RT 6771
        }
        if (selectedSiblings == null) {
            m.notify("Boxline selections cannot cross sections.\nPlease reapply your selection.")
            return null
        }

        val sidebar = BBX.CONTAINER.BOX.create()
        val treeParent = selectedSiblings[0].parent as Element
        treeParent.insertChild(sidebar, treeParent.indexOf(selectedSiblings[0]))

        var lastMovedElement: Element? = null
        for (selectedSibling in selectedSiblings) {
            selectedSibling.detach()
            if (BBX.CONTAINER.TABLE.isA(selectedSibling)
                && isTableCopy(selectedSibling as Element)
                && BBX.CONTAINER.TABLE.isA(lastMovedElement)
            ) {
                // don't move the table copy
                continue
            }
            if (selectedSibling is Element) lastMovedElement = selectedSibling
            sidebar.appendChild(selectedSibling)
        }
        m.document.settingsManager.applyStyle(style, sidebar)
        return sidebar
    }

    fun applyStyle(style: Style, start: Node, end: Node): List<Node> {
        val modifiedNodes: MutableList<Node> = ArrayList()
        if (isAlwaysWrapStyle(style)) {
            if (isAnyBox(style)) {
                movePageIndicators(start, end)
                if (start == end) {
                    if ((isBoxline(style) && isBoxline(start)) || (isFullBox(style) && isFullBox(start))
                        || (isColorBoxline(style) && isColorBoxline(start))
                        || (isColorFullBox(style) && isColorFullBox(start))
                    ) {
                        val container = start as Element
                        val child = container.findBlockChildOrNull()
                        unbox(container)
                        if (child != null) {
                            modifiedNodes.add(child)
                        }
                    } else {
                        if (style.name.contains("Color")) {
                            getColorName(style).open()
                        } else {
                            modifiedNodes.add(boxBlock(start, style))
                        }
                    }
                } else {
                    if (style.name.contains("Color")) {
                        getColorName(style).open()
                    } else {
                        val sidebar = boxMultiBlocks(start, end, style)
                        if (sidebar == null) {
                            return ArrayList()
                        } else {
                            modifiedNodes.add(sidebar)
                        }
                    }
                }
            } else if (isPoeticStanza(style)) {
                val stanza = BBX.CONTAINER.LIST.create(ListType.POEM_LINE_GROUP)
                wrapSelectedElements(m, stanza, start, end)
            } else if (isDontSplit(style)) {
                val dontSplit = BBX.CONTAINER.DONT_SPLIT.create()
                wrapSelectedElements(m, dontSplit, start, end)
            } else if (isList(style)) {
                val list = BBX.CONTAINER.LIST.create(ListType.NORMAL)
                wrapSelectedElements(m, list, start, end)
            } else if (isGuideWord(style)) {
                val guideWord = BBX.SPAN.GUIDEWORD.create()
                wrapSelectedText(guideWord, start, end)
            } else if (isPage(style)) {
                val page = if (XMLHandler.ancestorElementIs(start) { node: Element? -> BBX.BLOCK.isA(node) }) {
                    BBX.SPAN.PAGE_NUM.create()
                } else {
                    BBX.BLOCK.PAGE_NUM.create()
                }
                wrapSelectedPage(page, start, end)
            } else if (isDoubleLine(style)) {
                val doubleLineContainer = BBX.CONTAINER.DOUBLE_SPACE.create()
                wrapSelectedElements(m, doubleLineContainer, start, end)
            } else {
                throw UnsupportedOperationException("TODO")
            }

            // MenuModule is going to have a "group" of menu items with all the
            // ALWYAS_WRAP_STYLES in them
            // Some other code listening for selections is going to disable the
            // menu item
            // ^^^ The actual code may live in MenuModule but it should call a
            // util method here
            //// This needs to get all the top level elements and make sure they
            // have the same common parent
        } else if (start === end) {
            if (BBX.CONTAINER.isA(start) || BBX.SECTION.isA(start)) {
                val currentSelection = m.simpleManager.currentSelection
                val blocks = currentSelection.selectedBlocks
                for (block in blocks) {
                    if (BBX.BLOCK.PAGE_NUM.isA(block)) {
                        continue
                    }
                    BBXUtils.stripStyle(block, m)
                    m.document.settingsManager.applyStyle(style, block)
                }
                modifiedNodes.addAll(blocks)
            } else {
                val block = start.findBlockOrNull() ?: throw NodeException("start not inside block", start)
                BBXUtils.stripStyle(block, m)
                m.document.settingsManager.applyStyle(style, block)
                modifiedNodes.add(block)
            }
        } else {
            val blocks = getBlocks(start, end)
            if (isListItem(style)) {
                collectParents(blocks) // added for RT 5752
            }
            for (block in blocks) {
                BBXUtils.stripStyle(block, m)
                m.document.settingsManager.applyStyle(style, block)
            }
            modifiedNodes.addAll(blocks)
        }
        return modifiedNodes
    }

    fun wrapSelectedPage(page: Element, startNode: Node, endNode: Node) {
        var page = page
        var parent = startNode.parent as Element
        var cellsPerLine = (m.document.engine.pageSettings.drawableWidth
                / m.document.engine.brailleSettings.cellType.width.toDouble()).toInt()
        if (m.document.engine.pageSettings.isPrintPageNumberRange) {
            cellsPerLine -= 1
        }

        if (startNode is Text && startNode === endNode) {
            if ((m.simpleManager.currentSelection.start as XMLTextCaret).offset == (m
                    .simpleManager.currentSelection.end as XMLTextCaret).offset
            ) {
                m.notify("Invalid text for the Page style.  You must highlight a number. ")
            } else if (((m.simpleManager.currentSelection.end as XMLTextCaret).offset - (m
                    .simpleManager.currentSelection.start as XMLTextCaret).offset) < cellsPerLine
            ) {
                val ancestorElement = XMLHandler.ancestorVisitorElement(
                    endNode
                ) { node: Element? -> BBX.BLOCK.isA(node) }!!
                stripUTDRecursive(ancestorElement)

                val splitTextNode = XMLHandler2.splitTextNode(
                    startNode,
                    (m.simpleManager.currentSelection.start as XMLTextCaret).offset,
                    (m.simpleManager.currentSelection.end as XMLTextCaret).offset
                )

                //If the parent of the text node is an INLINE element, split the two text nodes into two separate INLINEs
                if (BBX.INLINE.isA(splitTextNode[0].parent)) {
                    val inline = splitTextNode[0].parent as Element
                    val inlineParent = inline.parent as Element
                    var inlineCopy = inline.copy()
                    inlineCopy.removeChildren()
                    inlineCopy.appendChild(splitTextNode[0].copy())
                    inlineParent.replaceChild(inline, inlineCopy)

                    var index = inlineParent.indexOf(inlineCopy) + 1
                    page.appendChild(splitTextNode[1].copy())
                    inlineParent.insertChild(page, index)
                    if (splitTextNode.size > 2) {
                        index++
                        inlineCopy = inlineCopy.copy()
                        inlineCopy.removeChildren()
                        inlineCopy.appendChild(splitTextNode[2].copy())
                        inlineParent.insertChild(inlineCopy, index)
                    }
                    parent = inlineParent
                } else {
                    page.appendChild(splitTextNode[1].copy())
                    parent.replaceChild(splitTextNode[1], page)
                }

                //Sanity check that the sibling before this one is not an empty text node
                //Blank files insert blank text nodes at the start that does not go away sometimes
                val itr = splitTextNode.iterator()
                while (itr.hasNext()) {
                    val next = itr.next()
                    if (next.value.isEmpty()) {
                        next.detach()
                        itr.remove()
                    }
                }
                if (BBX.BLOCK.isA(parent) && parent.childCount == 1) {
                    // user highlighted entire block, undo wrap
                    XMLHandler2.unwrapElement(page)
                    BBX.transform(parent, BBX.BLOCK.PAGE_NUM)
                    page = parent
                }
            } else {
                m.notify("Invalid text for the Page style. The highlighted value is too long.")
            }
        } else {
            m.notify("Invalid text for the Page style. Cannot highlight text between elements. Please apply the style only on the desired page number.")
            return
        }

        page.addAttribute(Attribute("page", "bbAdded"))
        m.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, true, parent))
    }

    private fun wrapSelectedText(guideWord: Element, startNode: Node, endNode: Node) {
        //Check if the word is already a guide word. If it is, set guideWords=false.
        if (isGuideWord(startNode)) {
            return
        }

        /*
         * You need to be able to wrap only the user selected text.
         * Ex: <BLOCK>Alphabet Reference</BLOCK>
         * Result: <BLOCK><SPAN type="GUIDE_WORD">Alphabet</SPAN> Reference</BLOCK>
         * Split the text node at the start and end offsets for the guide word selection
         * and wrap accordingly
         */
        var parent = startNode.parent as Element
        when (startNode) {
            is Text if startNode == endNode -> {
                val splitTextNode = XMLHandler2.splitTextNode(
                    startNode,
                    (m.simpleManager.currentSelection.start as XMLTextCaret).offset,
                    (m.simpleManager.currentSelection.end as XMLTextCaret).offset
                )
                if (splitTextNode[0].value.isEmpty()) {
                    splitTextNode[0].detach()
                }
                guideWord.appendChild(splitTextNode[1].copy())
                parent.replaceChild(splitTextNode[1], guideWord)
            }

            is Element if startNode.getAttribute("utd-style") != null && startNode.getAttributeValue("utd-style") == "Guide Word" -> {
                return
            }

            else -> {
                parent = getCommonParent(startNode, endNode)

                val blocks = getBlocks(startNode, endNode)

                for (element in blocks) {
                    var guide = BBX.SPAN.GUIDEWORD.create()
                    guide = copyChildrenToElement(element, guide)
                    element.appendChild(guide)
                }
            }
        }

        m.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, true, parent))
    }

    private fun isGuideWord(node: Node): Boolean {
        val guideWords = node.query("ancestor-or-self::node()[@utd-style='Guide Word']")
        if (guideWords.size() > 0) {
            val guideWord = guideWords[0] as Element?
            if (guideWord != null) {
                if (guideWord.getAttribute("guideWords") != null && guideWord.getAttributeValue("guideWords") == "false") {
                    guideWord.addAttribute(Attribute("guideWords", "true"))
                } else {
                    guideWord.addAttribute(Attribute("guideWords", "false"))
                }

                m.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, true, guideWord))
                return true
            }
        }
        return false
    }

    private fun copyChildrenToElement(from: Element, to: Element): Element {
        while (from.childCount > 1) {
            to.appendChild(from.removeChild(0).copy())
        }
        return to
    }

    // For RT 5752 added collectParents
    var parent0: ParentNode? = null

    private fun collectParents(blocks: LinkedHashSet<Element>) {
        var parent: ParentNode
        var index = -1
        var first = true
        for (block in blocks) {
            if (BBX.CONTAINER.TABLE.isA(block)) {
                val tableBrlCopy = Manager.getTableBrlCopy(block)
                tableBrlCopy?.detach()
            }
            parent = block.parent
            if (first) {
                first = false
                parent0 = parent
                index = parent0!!.indexOf(block)
            } else {
                if (parent != parent0) {
                    block.detach()
                    index++
                    parent0!!.insertChild(block, index)
                    if (parent.childCount == 0) {
                        parent.detach()
                    }
                }
            }
        }
    }

    private fun reformat(modifiedNodes: List<Node>) {
        m.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, modifiedNodes, true))
    }

    fun getBlocks(start: Node?, end: Node?): LinkedHashSet<Element> {
        //Get all nodes between (inclusive) start and end
        val nodes: MutableList<Node> = ArrayList()
        for (curNode in FastXPath.descendantAndFollowing(start)) {
            nodes.add(curNode)
            if (curNode === end) {
                break
            }
        }
        if (end is Element) {
            nodes.addAll(
                FastXPath.descendant(end)
            )
        }

        //Convert nodes to usable blocks without duplicates
        val blocks = LinkedHashSet<Element>()
        for (curNode in nodes) {
            if (curNode.findBlockOrNull() != null) {
                blocks.add(curNode.findBlock())
            }
        }
        return blocks
    }

    private fun isAnyBox(s: Style): Boolean {
        return isColorBoxline(s) || isColorFullBox(s) || isBoxline(s) || isFullBox(s)
    }

    private fun isColorBoxline(n: Node): Boolean {
        if (BBX.CONTAINER.isA(n)) {
            val style = m.getStyle(n)
            return style != null && isColorBoxline(style as Style)
        }

        return false
    }

    private fun isColorFullBox(n: Node): Boolean {
        if (BBX.CONTAINER.isA(n)) {
            val style = m.getStyle(n)
            return style != null && isColorFullBox(style as Style)
        }
        return false
    }

    private fun isColorBoxline(s: Style): Boolean {
        return s.name == "Color Box"
    }

    private fun isColorFullBox(s: Style): Boolean {
        return s.name == "Color Full Box"
    }

    private fun isBoxline(n: Node): Boolean {
        if (BBX.CONTAINER.isA(n)) {
            val style = m.getStyle(n)
            return style != null && isBoxline(style as Style)
        }

        return false
    }

    private fun isFullBox(n: Node): Boolean {
        if (BBX.CONTAINER.isA(n)) {
            val style = m.getStyle(n)
            return style != null && isFullBox(style as Style)
        }
        return false
    }

    private fun isBoxline(s: Style): Boolean {
        return s.name == "Box"
    }

    private fun isFullBox(s: Style): Boolean {
        return s.name == "Full Box"
    }

    private fun isPoeticStanza(s: Style): Boolean {
        return s.name == "Poetic Stanza"
    }

    private fun isDontSplit(s: Style): Boolean {
        return s.name == "Dont Split"
    }

    private fun isList(s: Style): Boolean {
        return s.name == "List Tag"
    }

    // For RT 5752 added isListItem
    private fun isListItem(s: Style): Boolean {
        return "List Item" == s.baseStyleName
    }

    private fun isGuideWord(s: Style): Boolean {
        return s.name == "Guide Word"
    }

    private fun isPage(s: Style): Boolean {
        return s.name == "Page"
    }

    private fun isDoubleLine(s: Style): Boolean {
        return s.name == "Double Spaced"
    }

    private fun detachTableCopy(table: Element) {
        val parent = table.parent
        if (parent != null && parent.indexOf(table) != parent.childCount - 1) {
            val checkSibling = parent.getChild(parent.indexOf(table) + 1)
            if (checkSibling is Element && checkSibling.getAttribute(
                    TableUtils.ATTRIB_TABLE_COPY,
                    UTD_NS
                ) != null
            ) {
                checkSibling.detach()
            }
        }
    }

    private fun isContainer(n: Node): Boolean {
        return BBX.CONTAINER.isA(n)
    }

    private val isTableSelected: Boolean
        get() = m.simpleManager.getModule(
            TableSelectionModule::class.java
        )!!.isTableSelected

    private fun warnTable() {
        displayInvalidTableMessage(m.wpManager.shell)
    }

    private fun movePageIndicators(start: Node, end: Node) {
        // this method removes page number indicators from the beginning and end
        // of elements that are going to be boxed

        // find the containers

        var b1 = if (isContainer(start)) start as Element else start.findBlock()
        var b2 = if (isContainer(end)) end as Element else end.findBlock()

        var tableParent = Manager.getTableParent(b1)
        if (tableParent != null) b1 = tableParent
        tableParent = Manager.getTableParent(b2)
        if (tableParent != null) b2 = tableParent

        // get the child elements so that page number indicators can be found
        val s1 = b1.childElements
        val s2 = b2.childElements
        val c1 = if (s1.size() > 0) s1[0] else null
        val ns2 = s2.size()
        val c2 = if (ns2 > 0) s2[ns2 - 1] else null

        // if a BBX.SPAN.PAGE_NUM element is found then
        // transform it to a block, detach, and reinsert before
        // and after the found blocks.
        if (null != c1 && BBX.SPAN.PAGE_NUM.isA(c1)) {
            BBX.transform(c1, BBX.BLOCK.PAGE_NUM)
            c1.detach()
            b1.parent.insertChild(c1, b1.parent.indexOf(b1))
        }

        if (null != c2 && BBX.SPAN.PAGE_NUM.isA(c2)) {
            BBX.transform(c2, BBX.BLOCK.PAGE_NUM)
            c2.detach()
            b2.parent.insertChild(c2, b2.parent.indexOf(b2) + 1)
        }
    }

    // added for RT 6395
    // a method for collecting the elements to go into a box
    fun getBoxSiblings(a: Element, b: Element): List<Node> {
        val nc1 = XMLNodeCaret(a)
        val nc2 = XMLNodeCaret(b)
        val xmlSelect = XMLSelection(nc1, nc2)
        val sibs = xmlSelect.selectedBlocks
        var i = 0
        while (i < sibs.size) {
            val tableParent = Manager.getTableParent(sibs[i])
            if (tableParent != null) {
                sibs.removeAt(i)
                if (!sibs.contains(tableParent)) {
                    sibs.add(i, tableParent)
                } else {
                    i--
                }
            }
            i++
        }
        collectParents(LinkedHashSet(sibs))

        return ArrayList<Node>(sibs)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(StylesMenuModule::class.java)

        //TODO: These really need to be style options....
        private val ALWAYS_WRAP_STYLES: List<String> = listOf(
            "Box",
            "Color Box",
            "Color Full Box",
            "Full Box",
            "Dont Split",
            "List Tag",
            "Poetic Stanza",
            "Guide Word",
            "Page",
            "Double Spaced"
        )
        const val TRNOTE_ACTION_STYLE_NAME: String = "TRNote"
        const val DESCRIPTION_STYLE_NAME: String = "Description"
        const val TRNOTE_ACTION_STYLE_DISPLAY_NAME: String = "Transcriber's Note"
        const val COLOR_PROP: String = "color_box"

        fun wrapSelectedElements(m: Manager, container: Element, startNode: Node, endNode: Node) {
            var startNode = startNode
            var endNode = endNode
            if (startNode is Text || isMath(startNode)) {
                startNode = startNode.findBlock()
            }
            if (endNode is Text || isMath(endNode)) {
                endNode = endNode.findBlock()
            }

            // TODO: This list query does NOT include poem
            val ancestorLists = startNode.query("ancestor::node()[@utd-style='List Tag']")
            val isNestedLineGroup = ListType.POEM_LINE_GROUP.isA(container)
                    && XMLHandler.ancestorElementIs(
                startNode
            ) { node: Element? -> ListType.POEM_LINE_GROUP.isA(node) }
            if (ancestorLists.size() > 0 || BBX.CONTAINER.LIST.isA(startNode)
                || hasUtdStyleTag(startNode as Element, "List Tag")
                || isNestedLineGroup
            ) {
                //Cannot do list within a list
                if (debugging) {
                    throw RuntimeException("Cannot have a list inside a list.")
                }
                val message = MessageBox(getInstance().shell)
                message.message = "Cannot have a list inside a list."
                message.open()
                return
            }

            //Get common parent of start and end node
            var parent = getCommonParent(startNode, endNode)

            if (m.simpleManager.currentSelection.isSingleNode) {
                if (BBX.SECTION.isA(startNode)) {
                    //Wrap contents instead of the section tag
                    val startElem = startNode
                    while (startElem.childCount != 0) {
                        val startChild = startElem.getChild(0)
                        startChild.detach()
                        container.appendChild(startChild)
                    }
                    startNode.appendChild(container)
                } else {
                    container.appendChild(startNode.copy())
                    if (parent == startNode) parent = parent.parent as Element

                    parent.replaceChild(startNode, container)
                }
            } else if (startNode == endNode) {
                parent = parent.parent as Element
                val index = parent.indexOf(startNode)
                container.appendChild(parent.removeChild(startNode))
                parent.insertChild(container, index)
            } else {
                val selectedSiblings = isValidTreeSelection(startNode, endNode)
                if (selectedSiblings == null) {
                    m.notify("Invalid selection. Cannot select part of a node.")
                    return
                }

                val treeParent = selectedSiblings[0].parent as Element
                treeParent.insertChild(container, treeParent.indexOf(selectedSiblings[0]))
                for (selectedSibling in selectedSiblings) {
                    selectedSibling.detach()
                    container.appendChild(selectedSibling)
                }
            }

            //if container is a list, handle list levels
            if (BBX.CONTAINER.LIST.isA(container)) {
                handleListLevels(container)
            }

            m.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, true, parent))
        }

        private fun handleListLevels(container: Element) {
            val name = BBX.BB_PREFIX + ":listLevel"
            val descendantLists = container.query("descendant::node()[@utd-style='List Tag']")
            if (descendantLists.size() > 0) {
                //Cannot do list within a list
                val message = MessageBox(getInstance().shell)
                message.message = "Cannot have a list inside a list."
                message.open()
                return
            }

            //Check the list items to see the max itemLevel
            val listItems = container.query("descendant::node()[@*[local-name()='itemLevel']]")

            var maxItemLevel = 0
            for (i in 0 until listItems.size()) {
                val listItem = listItems[i] as Element
                maxItemLevel =
                    max(listItem.getAttributeValue("itemLevel", BB_NS).toInt(), maxItemLevel)
            }

            if (ListType.POEM_LINE_GROUP.isA(container)) {
                val parentPoemList = XMLHandler.ancestorVisitorElement(
                    container
                ) { node: Element? -> ListType.POEM.isA(node) }
                if (parentPoemList == null) {
                    // in reality, broken document. Just set it on the lineGroup
                    //throw new NodeException("Poem line group not under list?", container);
                } else {
                    if (BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL[parentPoemList] != maxItemLevel) {
                        BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL[container] = maxItemLevel
                    }
                    return
                }
            }

            container.addAttribute(Attribute(name, BB_NS, maxItemLevel.toString()))
        }

        private fun getCommonParent(start: Node, end: Node): Element {
            var start = start
            var end = end
            if (start is Text) {
                start = start.parent
            }
            if (end is Text) {
                end = end.parent
            }

            return XMLHandler.findCommonParent(listOf(start as Element, end as Element))
        }

        fun isAlwaysWrapStyle(style: Style?): Boolean {
            return ALWAYS_WRAP_STYLES.any { curAlwaysUnwrap: String? ->
                isStyle(
                    style,
                    curAlwaysUnwrap!!
                )
            }
        }
    }
}

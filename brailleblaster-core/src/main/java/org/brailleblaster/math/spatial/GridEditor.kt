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
package org.brailleblaster.math.spatial

import nu.xom.Element
import nu.xom.ParentNode
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.findBlock
import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.math.numberLine.NumberLine
import org.brailleblaster.math.spatial.SpatialMathEnum.Passage
import org.brailleblaster.math.spatial.SpatialMathEnum.SpatialMathContainers
import org.brailleblaster.math.template.Template
import org.brailleblaster.math.template.TemplateConstants
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.XMLNodeCaret
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.events.XMLCaretEvent
import org.brailleblaster.perspectives.mvc.modules.views.DebugModule
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.util.Utils
import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.utils.swt.EasySWT
import org.brailleblaster.wordprocessor.WPManager
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.ScrolledComposite
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.MenuItem
import org.eclipse.swt.widgets.Shell
import java.util.function.Consumer

class GridEditor : ISpatialMathDialog {
    private var shell: Shell? = null
    private var page: Grid? = null
    private lateinit var _settingsMenu: Menu
    override fun register() {
        SpatialMathDispatcher.register(this)
    }

    init {
        loadPage()
        open()
        register()
    }

    private fun loadPage(): Grid? {
        if (SpatialMathUtils.currentIsSpatialGrid()) {
            val current = XMLHandler.ancestorVisitorElement(
                WPManager.getInstance().controller
                    .simpleManager.currentCaret.node
            ) { node: Element? -> BBX.CONTAINER.SPATIAL_GRID.isA(node) }
            page = Grid.getPageFromElement(current)
        } else if (Matrix.currentIsMatrix()) {
            val current = XMLHandler.ancestorVisitorElement(
                WPManager.getInstance().controller
                    .simpleManager.currentCaret.node
            ) { node: Element? -> BBX.CONTAINER.MATRIX.isA(node) }
            val matrix = Matrix.getContainerFromElement(current)
            page = Grid()
            page!!.array[0][0] = matrix
            page!!.settings.passage = matrix.settings.passage
        } else if (NumberLine.currentIsNumberLine()) {
            val current = XMLHandler.ancestorVisitorElement(
                WPManager.getInstance().controller
                    .simpleManager.currentCaret.node
            ) { node: Element? -> BBX.CONTAINER.NUMBER_LINE.isA(node) }!!
            val numberLine = NumberLine.getContainerFromElement(current)
            page = Grid()
            page!!.array[0][0] = numberLine
            page!!.settings.passage = numberLine.settings.passage
        } else {
            page = Grid()
        }
        return page
    }

    override fun createMenu(shell: Shell): Menu {
        val menu = Menu(shell, SWT.BAR)
        shell.menuBar = menu
        if (DebugModule.enabled) {
            addDebugMenu(menu)
        }
        _settingsMenu = createSettingsMenu(menu, shell)
        if (MathModuleUtils.isNemeth && page!!.hasTemplate()) {
            addIdentifierMenu()
        }
        addPassages(shell, _settingsMenu, page!!.settings, passagesClicked())
        addContainerTypeMenu(shell, menu)
        return menu
    }

    private fun addIdentifierMenu() {
        val identifierTranslation = MenuItem(_settingsMenu, SWT.CASCADE)
        identifierTranslation.text = TemplateConstants.IDENTIFIER_TRANSLATION
        val dropDownMenuBegin = Menu(shell, SWT.DROP_DOWN)
        identifierTranslation.menu = dropDownMenuBegin
        val mathTranslation = MenuItem(dropDownMenuBegin, SWT.RADIO)
        mathTranslation.text = TemplateConstants.MATH_TRANSLATION
        mathTranslation.selection = page!!.settings.isTranslateIdentifierAsMath
        mathTranslation.addListener(SWT.Selection) {
            if (!mathTranslation.selection) {
                return@addListener
            }
            page!!.settings.isTranslateIdentifierAsMath = true
            for (i in page!!.array.indices) {
                for (j in page!!.array[i].indices) {
                    val container = page!!.array[i][j]
                    if (container is Template) {
                        val template = page!!.array[i][j] as Template
                        template.settings.isTranslateIdentifierAsMath = true
                        template.identifier = MathText(print=template.identifier.print,
                            braille=Template.translateIdentifier(template.identifier.print, template))
                    }
                }
            }
            mathTranslation.selection = page!!.settings.isTranslateIdentifierAsMath
        }
        val literaryTranslation = MenuItem(dropDownMenuBegin, SWT.RADIO)
        literaryTranslation.text = TemplateConstants.LITERARY_TRANSLATION
        literaryTranslation.selection = !page!!.settings.isTranslateIdentifierAsMath
        literaryTranslation.addListener(SWT.Selection) {
            if (!literaryTranslation.selection) {
                return@addListener
            }
            page!!.settings.isTranslateIdentifierAsMath = false
            for (i in page!!.array.indices) {
                for (j in page!!.array[i].indices) {
                    val container = page!!.array[i][j]
                    if (container is Template) {
                        val template = page!!.array[i][j] as Template
                        template.settings.isTranslateIdentifierAsMath = false
                        template.identifier = MathText(print=template.identifier.print,
                            braille=Template.translateIdentifier(template.identifier.print, template))
                    }
                }
            }
            literaryTranslation.selection = !page!!.settings.isTranslateIdentifierAsMath
        }
    }

    private fun addDebugMenu(menu: Menu) {
        val cascadeMenu = MenuItem(menu, SWT.CASCADE)
        cascadeMenu.text = "Debug Spatial Math"
        val dropDownMenu = Menu(shell, SWT.DROP_DOWN)
        cascadeMenu.menu = dropDownMenu
        val item1 = MenuItem(dropDownMenu, SWT.NONE)
        item1.text = "Fill All"
        item1.addListener(SWT.Selection) {
            for (k in 0 until page!!.settings.rows) {
                for (h in 0 until page!!.settings.cols) {
                    val t = page!!.array[k][h]
                    t.widget?.fillDebug(t)
                }
            }
            SpatialMathDispatcher.dispatch()
        }
    }

    private fun passagesClicked(): Consumer<Passage> {
        return Consumer { e: Passage ->
            page!!.settings.passage = e
            for (i in page!!.array.indices) {
                for (j in page!!.array[i].indices) {
                    page!!.array[i][j].settings.passage = e
                }
            }
        }
    }

    private fun addContainerTypeMenu(shell2: Shell, menu: Menu) {
        val cascadeMenu = MenuItem(menu, SWT.CASCADE)
        cascadeMenu.text = CONTAINER_TYPE_LABEL
        val dropDownMenu = Menu(shell, SWT.DROP_DOWN)
        cascadeMenu.menu = dropDownMenu
        val id: Int = if (page!!.array[page!!.settings.rowIndex][page!!.settings.colIndex] is Matrix) {
            SpatialMathContainers.MATRIX.id
        } else if (page!!.array[page!!.settings.rowIndex][page!!.settings.colIndex] is Template) {
            SpatialMathContainers.TEMPLATE.id
        } else if (page!!.array[page!!.settings.rowIndex][page!!.settings.colIndex] is NumberLine) {
            SpatialMathContainers.NUMBER_LINE.id
        } else {
            SpatialMathContainers.CONNECTING.id
        }
        for (s in SpatialMathContainers.entries) {
            val blankBlock = MenuItem(dropDownMenu, SWT.RADIO)
            blankBlock.text = s.prettyName
            blankBlock.selection = s.id == id
            blankBlock.addListener(SWT.Selection) {
                when (s.id) {
                    SpatialMathContainers.NUMBER_LINE.id -> {
                        page!!.array[page!!.settings.rowIndex][page!!.settings.colIndex] = NumberLine()
                    }

                    SpatialMathContainers.MATRIX.id -> {
                        page!!.array[page!!.settings.rowIndex][page!!.settings.colIndex] = Matrix()
                    }

                    SpatialMathContainers.TEMPLATE.id -> {
                        page!!.array[page!!.settings.rowIndex][page!!.settings.colIndex] = Template()
                    }

                    else -> {
                        page!!.array[page!!.settings.rowIndex][page!!.settings.colIndex] = ConnectingContainer()
                    }
                }
                SpatialMathDispatcher.dispatch()
            }
        }
    }

    override fun open() {
        val m = WPManager.getInstance().controller
        if (shell == null || shell!!.isDisposed) {
            shell = Shell(
                m.display.activeShell,
                SWT.DIALOG_TRIM or SWT.APPLICATION_MODAL or SWT.BORDER or SWT.RESIZE
            )
        } else {
            val length = shell!!.children.size
            for (i in 0 until length) {
                shell!!.children[0].dispose()
            }
        }
        val gl = GridLayout(1, false)
        shell!!.text = MathModuleUtils.SPATIAL_COMBO
        shell!!.layout = gl
        val outerContainer = Composite(shell, SWT.NONE)
        outerContainer.layout = GridLayout(1, false)
        outerContainer.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        val sc = ScrolledComposite(outerContainer, SWT.V_SCROLL or SWT.H_SCROLL or SWT.BORDER)
        sc.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        sc.expandVertical = true
        sc.expandHorizontal = true
        val innerContainer = Composite(sc, SWT.NONE)
        innerContainer.layout = GridLayout(1, false)
        innerContainer.layoutData = GridData(SWT.FILL, SWT.FILL, true, true)
        val gridGroup = Composite(innerContainer, SWT.NONE)
        gridGroup.layout = GridLayout(2, false)
        gridGroup.layoutData = GridData(SWT.CENTER, SWT.CENTER, true, true)
        val containerGroup = EasySWT.makeGroup(gridGroup, SWT.NONE, 1, true)
        containerGroup.text = (ROW_GROUP + " " + (page!!.settings.rowIndex + 1) + " " + COL_LABEL + " "
                + (page!!.settings.colIndex + 1))
        sc.content = gridGroup
        val menu = createMenu(shell!!)
        val cc = page!!.array[page!!.settings.rowIndex][page!!.settings.colIndex]
        cc.widget?.getWidget(containerGroup, cc)
        cc.widget?.addMenuItems(shell!!, menu, _settingsMenu)
        page!!.array[page!!.settings.rowIndex][page!!.settings.colIndex] = cc
        val commandsGroup = EasySWT.makeGroup(innerContainer, SWT.NONE, 1, false)
        addCommands(
            commandsGroup,
            page!!,
            shell!!,
            { delete() },
            { ok() },
            { cancel() }
        )
        EasySWT.addEscapeCloseListener(shell!!)
        sc.content = innerContainer
        sc.setMinSize(innerContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT))
        shell!!.pack()
        shell!!.layout(true)
        shell!!.open()
        page!!.array[page!!.settings.rowIndex][page!!.settings.colIndex].widget
            ?.onOpen()
    }

    private fun delete() {
        if (SpatialMathUtils.currentIsSpatialGrid()) {
            deletePage()
            safeClose()
        } else if (Matrix.currentIsMatrix()) {
            Matrix.deleteMatrix()
            safeClose()
        } else if (NumberLine.currentIsNumberLine()) {
            NumberLine.deleteNumberLine()
            safeClose()
        }
    }

    private fun ok() {
        page!!.lines.clear()
        page!!.array.flatten().forEach { it.lines.clear() }
        extractFromTextBoxes()
        if (!checkPreFormat()) {
            return
        }
        formatGrid(page)
        insertIntoDoc()
    }

    private fun cancel() {
        safeClose()
    }

    private fun checkPreFormat(): Boolean {
        for (k in 0 until page!!.settings.rows) {
            for (h in 0 until page!!.settings.cols) {
                val container = page!!.array[k][h]
                if (!container.preFormatChecks()) {
                    return false
                }
            }
        }
        return true
    }

    private fun extractFromTextBoxes() {
        page!!.array[page!!.settings.rowIndex][page!!.settings.colIndex].widget
            ?.extractText()
    }

    private fun insertIntoDoc() {
        val gridElement: Element = makeBBXGridElement(page)
        try {
            SpatialMathBlock.format(gridElement, page!!.lines)
        } catch (e: MathFormattingException) {
            e.printStackTrace()
            MathFormattingException.notifyLong()
            return
        }
        var currentParent: ParentNode
        if (SpatialMathUtils.currentIsSpatialGrid()) {
            val current = XMLHandler.ancestorVisitorElement(
                WPManager.getInstance().controller
                    .simpleManager.currentCaret.node
            ) { node: Element? -> BBX.CONTAINER.SPATIAL_GRID.isA(node) }
            currentParent = current!!.parent
            currentParent.replaceChild(current, gridElement)
        } else {
            var current = WPManager.getInstance().controller.simpleManager.currentCaret.node
            if (BBX.CONTAINER.isA(current)) {
                currentParent = current.parent
            } else {
                current = current.findBlock()
                currentParent = current.parent
            }
            var index = currentParent.indexOf(current)
            while (!BBX.SECTION.isA(currentParent)) {
                current = currentParent as Element
                currentParent = current.parent
                index = currentParent.indexOf(current)
            }
            index++
            if (BBX.CONTAINER.TABLE.isA(current)) {
                //table copy
                index++
            }
            Utils.insertChildCountSafe(currentParent, gridElement, index)
        }
        Grid.addStylesToGridElement(gridElement)
        WPManager.getInstance().controller.simpleManager
            .dispatchEvent(ModifyEvent(Sender.TEXT, true, currentParent))
        safeClose()
    }

    override val settingsMenu: Menu
        get() = _settingsMenu

    override fun safeClose() {
        for (i in page!!.array.indices) {
            for (j in page!!.array[i].indices) {
                page!!.array[i][j].saveSettings()
            }
        }
        page!!.saveSettings()
        shell!!.close()
    }

    companion object {
        private val localeHandler = getDefault()

        @JvmField
        val CONTAINER_TYPE_LABEL = localeHandler["containerType"]
        val ROW_GROUP = localeHandler["rowGroup"]
        val NEXT_ROW_LABEL = localeHandler["nextRow"]
        val PREVIOUS_ROW_LABEL = localeHandler["previousRow"]
        val NEXT_COL_LABEL = localeHandler["nextCol"]
        val PREVIOUS_COL_LABEL = localeHandler["previousCol"]
        val COL_LABEL = localeHandler["col"]

        fun deletePage() {
            val current = XMLHandler.ancestorVisitorElement(
                WPManager.getInstance().controller
                    .simpleManager.currentCaret.node
            ) { node -> BBX.CONTAINER.SPATIAL_GRID.isA(node) }!!
            // get previous block for caret event
            val previous = XMLHandler.previousSiblingNode(current)
            val parent = current.parent
            parent.removeChild(current)
            WPManager.getInstance().controller.simpleManager
                .dispatchEvent(ModifyEvent(Sender.TEXT, true, parent))
            if (previous != null) {
                WPManager.getInstance().controller.simpleManager
                    .dispatchEvent(XMLCaretEvent(Sender.TEXT, XMLNodeCaret(previous)))
            }
        }

        fun formatGrid(page: Grid?) {
            for (k in 0 until page!!.settings.rows) {
                for (h in 0 until page.settings.cols) {
                    page.array[k][h].settings.passage = page.settings.passage
                    page.array[k][h].format()
                }
            }
            for (k in 0 until page.settings.rows) {
                for (h in 0 until page.settings.cols) {
                    if (page.array[k][h] is ConnectingContainer) {
                        (page.array[k][h] as ConnectingContainer).fillPageInfo(
                            page.array[k][h],
                            page, k, h
                        )
                    }
                }
            }
            page.format()
        }

        fun makeBBXGridElement(page: Grid?): Element {
            for (i in 0 until page!!.settings.rows) {
                for (j in 0 until page.settings.cols) {
                    val t = page.array[i][j]
                    val e: Element = when (t) {
                        is Template -> {
                            BBX.CONTAINER.TEMPLATE.create(t)
                        }

                        is Matrix -> {
                            BBX.CONTAINER.MATRIX.create(t)
                        }

                        is NumberLine -> {
                            BBX.CONTAINER.NUMBER_LINE.create(t)
                        }

                        else -> {
                            BBX.CONTAINER.CONNECTING_CONTAINER.create(t as ConnectingContainer)
                        }
                    }
                    try {
                        SpatialMathBlock.format(e, t.lines)
                    } catch (e1: MathFormattingException) {
                        e1.printStackTrace()
                    }
                }
            }
            return BBX.CONTAINER.SPATIAL_GRID.create(page)
        }

        fun addCommands(
            gridGroup: Composite?,
            page: Grid,
            shell: Shell,
            onDelete: Consumer<SelectionEvent>?,
            onOk: Consumer<SelectionEvent>?,
            onCancel: Consumer<SelectionEvent>?
        ) {
            val rowGroup = EasySWT.makeGroup(gridGroup, 0, page.settings.rows + 3, false)
            rowGroup.text = SpatialMathUtils.ROW_GROUP
            EasySWT.makeToggleButton(
                rowGroup, SpatialMathUtils.PREVIOUS_LABEL, 1
            ) {
                if (page.settings.rowIndex > 0) {
                    page.array[page.settings.rowIndex][page.settings.colIndex].widget
                        ?.extractText()
                    page.settings.rowIndex -= 1
                    SpatialMathDispatcher.dispatch()
                }
            }
            for (i in 0 until page.settings.rows) {
                val b = EasySWT.makeToggleButton(
                    rowGroup, (i + 1).toString(), 1
                ) { _: SelectionEvent? ->
                    page.array[page.settings.rowIndex][page.settings.colIndex].widget
                        ?.extractText()
                    page.settings.rowIndex = i
                    SpatialMathDispatcher.dispatch()
                }
                b.selection = i == page.settings.rowIndex
            }
            EasySWT.makeToggleButton(rowGroup, SpatialMathUtils.NEXT_LABEL, 1) {
                page.array[page.settings.rowIndex][page.settings.colIndex].widget
                    ?.extractText()
                page.saveSettings()
                if (page.settings.rowIndex + 2 > page.settings.rows) {
                    page.addRow()
                }
                page.settings.rowIndex += 1
                SpatialMathDispatcher.dispatch()
            }
            EasySWT.makePushButton(
                rowGroup, SpatialMathUtils.DELETE, 1
            ) {
                if (page.settings.rowIndex > 0) {
                    page.deleteRow(page.settings.rowIndex)
                    SpatialMathDispatcher.dispatch()
                }
            }
            val colGroup = EasySWT.makeGroup(gridGroup, 0, page.settings.cols + 3, false)
            colGroup.text = SpatialMathUtils.COL_LABEL
            EasySWT.makeToggleButton(
                colGroup, SpatialMathUtils.PREVIOUS_LABEL, 1
            ) {
                if (page.settings.colIndex > 0) {
                    page.array[page.settings.rowIndex][page.settings.colIndex].widget
                        ?.extractText()
                    page.settings.colIndex -= 1
                    SpatialMathDispatcher.dispatch()
                }
            }
            for (i in 0 until page.settings.cols) {
                val b = EasySWT.makeToggleButton(
                    colGroup, (i + 1).toString(), 1
                ) { _: SelectionEvent? ->
                    page.array[page.settings.rowIndex][page.settings.colIndex].widget
                        ?.extractText()
                    page.settings.colIndex = i
                    SpatialMathDispatcher.dispatch()
                }
                b.selection = i == page.settings.colIndex
            }
            EasySWT.makeToggleButton(colGroup, SpatialMathUtils.NEXT_LABEL, 1) {
                page.array[page.settings.rowIndex][page.settings.colIndex].widget
                    ?.extractText()
                page.saveSettings()
                if (page.settings.colIndex + 2 > page.settings.cols) {
                    page.addCol()
                }
                page.settings.colIndex += 1
                SpatialMathDispatcher.dispatch()
            }
            EasySWT.makePushButton(
                colGroup, SpatialMathUtils.DELETE, 1
            ) {
                if (page.settings.colIndex > 0) {
                    page.deleteCol(page.settings.colIndex)
                    SpatialMathDispatcher.dispatch()
                }
            }
            val actionsGroup = EasySWT.makeGroup(gridGroup, 0, 3, false)
            EasySWT.makePushButton(actionsGroup, SpatialMathUtils.OK_LABEL, 1, onOk)
            EasySWT.makePushButton(actionsGroup, SpatialMathUtils.CANCEL_LABEL, 1, onCancel)
            if (MathModuleUtils.currentIsSpatialMath()) {
                EasySWT.makePushButton(actionsGroup, SpatialMathUtils.DELETE_CONTAINER, 1, onDelete)
            }
        }
    }
}
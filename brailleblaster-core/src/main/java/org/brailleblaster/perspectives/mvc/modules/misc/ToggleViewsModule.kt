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

import org.brailleblaster.BBIni
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.views.style.BreadcrumbsToolbar
import org.brailleblaster.perspectives.mvc.BBSimpleManager.SimpleListener
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.ViewManager
import org.brailleblaster.perspectives.mvc.events.BuildMenuEvent
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.MenuManager.addSubMenu
import org.brailleblaster.perspectives.mvc.menu.SubMenuBuilder
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.perspectives.mvc.modules.views.DebugModule
import org.brailleblaster.tools.MenuTool
import org.brailleblaster.tools.ToggleViewTool
import org.brailleblaster.utils.swt.EasySWT
import org.brailleblaster.wordprocessor.WPManager
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.MenuItem
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.ToolItem
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.Consumer

class ToggleViewsModule(private val m: Manager) : SimpleListener {
    private val shell: Shell = m.wpManager.shell

    enum class Views {
        PRINT, BRAILLE, STYLE
    }

    private var windowedView: Views? = null
    private var currentViews: MutableList<Views> = loadSettings()
    private val parent: Composite = findParentComposite(currentViews)

    init {
        checkViews()
    }

    override fun onEvent(event: SimpleEvent) {
        if (event is BuildMenuEvent) {
            addSubMenu(
                SubMenuBuilder(
                    TopMenu.VIEW,
                    TOGGLE_SUBMENU_TITLE
                )
                .addCheckItem(
                    ToggleViewTool.TogglePrintViewTool(Views.PRINT in currentViews, makeSelectionListener(Views.PRINT))
                )
                .addCheckItem(
                    ToggleViewTool.ToggleBrailleViewTool(Views.BRAILLE in currentViews, makeSelectionListener(Views.BRAILLE))
                )
                .addCheckItem(
                    ToggleViewTool.ToggleStyleViewTool(Views.STYLE in currentViews, makeSelectionListener(Views.STYLE))
                )
                .addCheckItem(ToggleViewTool.ToggleBreadCrumbsToolbarTool(BreadcrumbsToolbar.enabled) {
                    BreadcrumbsToolbar.enabled = !BreadcrumbsToolbar.enabled
                    WPManager.getInstance().buildToolBar()
                })
                .addSeparator()
                .addItem(RearrangeViewsTool())
            )
            windowedView = ViewManager.windowedView
            if (DebugModule.enabled) {
                addSubMenu(
                    SubMenuBuilder(
                        TopMenu.WINDOW,
                        WINDOWIZE_TITLE
                    )
                        .addRadioItem(
                            "Print",
                            SWT.NONE,
                            windowedView == Views.PRINT
                        ) { e: BBSelectionData -> enableWindowedView(e, Views.PRINT) }.addRadioItem(
                            "Braille",
                            SWT.NONE,
                            windowedView == Views.BRAILLE
                        ) { e: BBSelectionData -> enableWindowedView(e, Views.BRAILLE) }.addRadioItem(
                            "Style",
                            SWT.NONE,
                            windowedView == Views.STYLE
                        ) { e: BBSelectionData -> enableWindowedView(e, Views.STYLE) }
                )
            }
        }
    }

    private fun enableWindowedView(e: BBSelectionData, view: Views) {
        log.info("enable")
        val menu = e.widget as MenuItem
        if (windowedView == view) {
            ViewManager.windowedView = null
            menu.selection = false
            windowedView = null
        } else {
            ViewManager.windowedView = view
            windowedView = view
        }
        m.viewManager.reparent()
    }

    private fun makeSelectionListener(view: Views): Consumer<BBSelectionData> {
        return Consumer { e: BBSelectionData ->
            val toolBarItem = e.toolBarItem
            val menuItem = e.menuItem
            val toolBarSource = e.widget is ToolItem
            val selection = if (toolBarSource) toolBarItem!!.selection else menuItem!!.selection
            if (!setVisible(view, selection)) { // They tried to get rid of the
                // last view, so set the
                // menu/toolbar item back
                if (toolBarSource) toolBarItem!!.selection = !toolBarItem.selection else menuItem!!.selection =
                    !menuItem.selection
            } else { // Keep the menu item and toolbar items in sync
                if (toolBarSource) menuItem!!.selection =
                    toolBarItem!!.selection else if (toolBarItem?.isDisposed == false) toolBarItem.selection =
                    menuItem!!.selection
            }
            m.viewManager.setTabList()
            m.viewManager.reparent()
        }
    }

    fun checkViews() {
        checkView(Views.STYLE, stylePane)
        checkView(Views.PRINT, printView)
        checkView(Views.BRAILLE, brailleView)
        setViewOrder(stylePane, printView, brailleView, m.viewManager)
        parent.layout(true)
    }

    private fun checkView(view: Views, widget: StyledText) {
        widget.isVisible = currentViews.contains(view)
    }

    private fun setVisible(view: Views, visible: Boolean): Boolean {
        m.viewManager.saveScreenProperties()
        if (visible && !currentViews.contains(view)) {
            currentViews.add(view)
        } else if (!visible) {
            if (currentViews.size == 1) {
                // Do not allow the final view to be removed
                invalidViewChange()
                return false
            }
            currentViews.remove(view)
        }
        checkViews()
        saveSettings(currentViews)
        m.viewManager.reparent()
        setViewOrder(stylePane, printView, brailleView, m.viewManager)
        return true
    }

    private fun findParentComposite(currentViews: MutableList<Views>): Composite {
        checkList(currentViews)
        val widget: StyledText = when (currentViews[0]) {
            Views.BRAILLE -> brailleView
            Views.PRINT -> printView
            Views.STYLE -> stylePane
        }
        return widget.parent
    }

    private fun invalidViewChange() {
        EasySWT.makeEasyOkDialog(ERROR_TITLE, ERROR_MESSAGE, shell)
    }

    private val stylePane: StyledText
        get() = m.stylePane.widget
    private val printView: StyledText
        get() = m.textView
    private val brailleView: StyledText
        get() = m.brailleView

    inner class RearrangeViewsTool : MenuTool {
        override val topMenu: TopMenu = TopMenu.VIEW
        override val title: String = REARRANGE
        override fun onRun(bbData: BBSelectionData) {
            ChangeViewOrderDialog()
        }
    }
    private inner class ChangeViewOrderDialog {
        init {
            val dialog = Shell(shell, SWT.DIALOG_TRIM)
            dialog.text = "Rearrange Views"
            dialog.layout = GridLayout(1, false)
            val container = EasySWT.makeComposite(dialog, 2)
            val list = org.eclipse.swt.widgets.List(container, SWT.SINGLE or SWT.BORDER)
            EasySWT.buildGridData().setHint(200, SWT.DEFAULT).applyTo(list)
            fillList(list)
            val moveUpButton = "Move Up"
            val moveDownButton = "Move Down"
            EasySWT.buildComposite(container).apply {
                this.columns = 1
                this.addButton(moveUpButton, 1) { moveUp(list) }
                this.addButton(moveDownButton, 1) { moveDown(list) }
            }.build()
            val okButton = "Ok"
            val cancelButton = "Cancel"
            EasySWT.buildGridData().setColumns(2)
                .applyTo(EasySWT.buildComposite(container).addButton(okButton, 1) {
                    saveNewList(list.items)
                    dialog.close()
                }.addButton(cancelButton, 1) { dialog.close() }.build())
            dialog.pack()
            dialog.open()
        }

        private fun fillList(list: org.eclipse.swt.widgets.List) {
            list.removeAll()
            currentViews.forEach(Consumer { v: Views ->
                list.add((v.name.lowercase(Locale.getDefault())).replaceFirstChar { it.titlecase() }) })
        }

        private fun saveNewList(items: Array<String>) {
            currentViews = ArrayList()
            for (item in items) {
                when (item.lowercase(Locale.getDefault())) {
                    Views.STYLE.name.lowercase(Locale.getDefault()) -> {
                        currentViews.add(Views.STYLE)
                    }
                    Views.PRINT.name.lowercase(Locale.getDefault()) -> {
                        currentViews.add(Views.PRINT)
                    }
                    Views.BRAILLE.name.lowercase(Locale.getDefault()) -> {
                        currentViews.add(Views.BRAILLE)
                    }
                }
            }
            saveSettings(currentViews)
            setViewOrder(stylePane, printView, brailleView, m.viewManager)
            m.viewManager.setTabList()
        }

        private fun moveUp(list: org.eclipse.swt.widgets.List) {
            val selection = list.selectionIndex
            if (selection > 0 && selection < currentViews.size) {
                val view = currentViews[selection]
                currentViews.removeAt(selection)
                currentViews.add(selection - 1, view)
                fillList(list)
                list.setSelection(selection - 1)
            }
        }

        private fun moveDown(list: org.eclipse.swt.widgets.List) {
            val selection = list.selectionIndex
            if (selection != currentViews.size - 1 && selection < currentViews.size && selection != -1) {
                val view = currentViews[selection]
                currentViews.removeAt(selection)
                currentViews.add(selection + 1, view)
                fillList(list)
                list.setSelection(selection + 1)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ToggleViewsModule::class.java)
        const val ERROR_TITLE = "Error"
        const val ERROR_MESSAGE = "At least one view must be active"
        const val TOGGLE_SUBMENU_TITLE = "Toggle Views"
        private const val REARRANGE = "Rearrange Views..."
        private const val USER_SETTING_KEY = "views"
        private const val USER_SETTING_DELIMITER = ","
        const val WINDOWIZE_TITLE = "Windowize"
        private val DEFAULT_ORDER = listOf(Views.STYLE, Views.PRINT, Views.BRAILLE)
        fun setViewOrder(style: StyledText, print: StyledText, braille: StyledText, viewManager: ViewManager) {
            val viewOrder = loadSettings()
            checkList(viewOrder)
            // Windowed view is in a different shell
            ViewManager.removeWindowedView(viewOrder)
            val parent = when (viewOrder[0]) {
                Views.STYLE -> {
                    style.moveAbove(null)
                    style.parent
                }

                Views.PRINT -> {
                    print.moveAbove(null)
                    print.parent
                }

                Views.BRAILLE -> {
                    braille.moveAbove(null)
                    braille.parent
                }
            }
            if (viewOrder.size > 1) {
                when (viewOrder[viewOrder.size - 1]) {
                    Views.STYLE -> style.moveBelow(null)
                    Views.PRINT -> print.moveBelow(null)
                    Views.BRAILLE -> braille.moveBelow(null)
                }
            }
            viewManager.setViewSizes()
            parent.layout(true)
        }

        @JvmStatic
		fun loadSettings(): MutableList<Views> {
            val propValue = BBIni.propertyFileManager.getProperty(USER_SETTING_KEY)
            if (propValue == null || propValue.trim { it <= ' ' }.isEmpty()) {
                return DEFAULT_ORDER.toMutableList()
            }
            val returnList: MutableList<Views> = ArrayList()
            val split = propValue.split(USER_SETTING_DELIMITER.toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
            for (value in split) {
                for (view in Views.entries) {
                    if (view.name == value) returnList.add(view)
                }
            }
            return returnList
        }

        fun saveSettings(newViews: List<Views>) {
            checkList(newViews)
            val sb = StringBuilder()
            for (i in newViews.indices) {
                sb.append(newViews[i])
                if (i != newViews.size - 1) sb.append(USER_SETTING_DELIMITER)
            }
            BBIni.propertyFileManager.save(USER_SETTING_KEY, sb.toString())
        }

        private fun checkList(views: List<Views>) {
            require(views.isNotEmpty()) { "There must be at least one view active" }
        }
    }
}
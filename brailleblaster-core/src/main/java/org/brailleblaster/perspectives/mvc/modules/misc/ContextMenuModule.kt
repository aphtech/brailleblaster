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

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.math.numberLine.NumberLine.Companion.currentIsNumberLine
import org.brailleblaster.math.spatial.Matrix
import org.brailleblaster.math.spatial.SpatialMathUtils.currentIsSpatialGrid
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.views.wp.BrailleView
import org.brailleblaster.perspectives.braille.views.wp.PageNumberDialog
import org.brailleblaster.perspectives.braille.views.wp.TextView
import org.brailleblaster.perspectives.mvc.BBSimpleManager.SimpleListener
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.XMLTextCaret
import org.brailleblaster.perspectives.mvc.events.BuildMenuEvent
import org.brailleblaster.perspectives.mvc.events.XMLCaretEvent
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.MenuManager
import org.brailleblaster.perspectives.mvc.menu.SharedItem
import org.brailleblaster.utd.UTDTranslationEngine
import org.brailleblaster.utils.localization.LocaleHandler
import org.brailleblaster.wordprocessor.WPManager
import org.eclipse.swt.SWT
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Menu
import org.eclipse.swt.widgets.MenuItem

class ContextMenuModule(private val manager: Manager) : SimpleListener {
    private val tv: TextView = manager.text
    private val bv: BrailleView = manager.braille
    private var items: List<ContextItem>? = null
    private var moduleInitialized = false

    override fun onEvent(event: SimpleEvent) {
        if (event is BuildMenuEvent) {
            items = buildItems()
            moduleInitialized = true
        }
        if (event is XMLCaretEvent && moduleInitialized) {
            disposeExistingMenus()
            if (manager.wpManager.isClosed()) {
                return
            }
            createMenu(event, tv.view)
            createMenu(event, bv.view)
        }
    }

    private fun buildItems(): List<ContextItem> {
        return listOf(
            ContextItem(
                Items.COPY.label,
                {true}, //Allows copy option in brailleView context menu, not just ctrl+c
                MenuManager.getSharedSelection(SharedItem.COPY)
            ),
            ContextItem(
                Items.COPY_UNICODE.label,
                {true}, //Curiously, the event sender is always from the Text View, never Braille.
                //Which means that dynamically renaming the label based on the view is impossible.
                MenuManager.getSharedSelection(SharedItem.COPY_UNICODE)
            ),
            ContextItem(
                Items.CUT.label,
                { event: XMLCaretEvent -> isMultiSelected(event) },
                MenuManager.getSharedSelection(SharedItem.CUT)
            ),
            ContextItem(
                Items.PASTE.label,
                { e: XMLCaretEvent -> !isInTable(e) },
                MenuManager.getSharedSelection(SharedItem.PASTE)
            ),
            ContextItem(
                Items.PASTE_AS_MATH.label,
                //There may be other predicates to add here.
                { e: XMLCaretEvent -> !isInTable(e) },
                MenuManager.getSharedSelection(SharedItem.PASTE_AS_MATH)
            ),
            ContextSubItem(
                Items.CHANGE_TRANSLATION.label,
                { event: XMLCaretEvent -> isMultiSelected(event) },
                SharedItem.CHANGE_TRANSLATION
            ),
            ContextItem(
                Items.EDIT_PAGE_NUMBER.label,
                { true },
                MenuManager.getSharedSelection(SharedItem.EDIT_PAGE_NUMBER)
            ),
            ContextItem(
                Items.EDIT_TABLE.label,
                { event: XMLCaretEvent -> isInTable(event) },
                MenuManager.getSharedSelection(SharedItem.EDIT_TABLE)
            ),
            ContextItem(
                Items.MATH_TOGGLE.label,
                { true },
                MenuManager.getSharedSelection(SharedItem.MATH_TOGGLE)
            ),
            ContextItem(
                Items.NUMBER_LINE.label,
                { currentIsNumberLine() },
                MenuManager.getSharedSelection(SharedItem.NUMBER_LINE)
            ),
            ContextItem(
                Items.MATRIX.label,
                { Matrix.currentIsMatrix() },
                MenuManager.getSharedSelection(SharedItem.MATRIX)
            ),
            ContextItem(
                Items.SPATIAL_COMBO.label,
                { currentIsSpatialGrid() },
                MenuManager.getSharedSelection(SharedItem.SPATIAL_COMBO)
            ),
            ContextItem(
                AlphabeticReferenceModule.EDIT_GUIDE_WORD,
                { event: XMLCaretEvent -> isGuideWord(event) },
                MenuManager.getSharedSelection(SharedItem.EDIT_GUIDE_WORD)
            )
        )
    }

    private fun createMenu(event: XMLCaretEvent, parent: Control) {
        val newMenu = Menu(parent)
        for (menuItem in items!!) {
            if (menuItem is ContextSubItem) {
                val subMenu = MenuManager.createSharedSubMenu(
                    menuItem.menuSource!!,
                    parent.shell, false
                )
                val smItem = MenuItem(newMenu, SWT.CASCADE)
                smItem.text = menuItem.name
                smItem.isEnabled = menuItem.test(event)
                smItem.menu = subMenu
            } else {
                val newItem = MenuItem(newMenu, SWT.PUSH)
                newItem.text = menuItem.name
                newItem.isEnabled = menuItem.test(event)
                newItem.addSelectionListener(object : SelectionAdapter() {
                    override fun widgetSelected(e: SelectionEvent) {
                        menuItem.execute(BBSelectionData(newItem, WPManager.getInstance()))
                    }
                })
            }
        }
        parent.menu = newMenu
    }

    private fun disposeExistingMenus() {
        if (tv.view.menu != null) {
            tv.view.menu.dispose()
        }
        if (bv.view.menu != null) {
            bv.view.menu.dispose()
        }
    }

    private fun isMultiSelected(event: XMLCaretEvent): Boolean {
        if (event.start.node !== event.end.node) {
            return true
        }
        return if (event.start is XMLTextCaret) {
            event.start.offset != (event.end as XMLTextCaret).offset
        } else false
    }

    private fun isInTable(event: XMLCaretEvent): Boolean {
        if (checkTable(event.start.node)) return true
        return if (event.start !== event.end) {
            checkTable(event.end.node)
        } else false
    }

    private fun checkTable(node: Node): Boolean {
        return if (node is Element && Manager.getTableParent(node) != null) true
        else Manager.getTableParent(node.parent) != null
    }

    private fun isGuideWord(event: XMLCaretEvent): Boolean {
        val engine: UTDTranslationEngine = event.manager.utdManager.engine
        val style = engine.getStyle(event.start.node)
        return style != null && style.isGuideWords
    }

    enum class Items(val label: String) {
        COPY(LocaleHandler.getDefault()["&Copy"].replace("&", "")),
        COPY_UNICODE(LocaleHandler.getDefault()["CopyUnicode"]),
        CUT(LocaleHandler.getDefault()["&Cut"].replace("&", "")),
        PASTE(LocaleHandler.getDefault()["&Paste"].replace("&", "")),
        PASTE_AS_MATH(LocaleHandler.getDefault()["PasteAsMath"]),
        HIDE(LocaleHandler.getDefault()["Hide"]),
        EDIT_PAGE_NUMBER(PageNumberDialog.MENU_NAME),
        EDIT_TABLE(LocaleHandler.getDefault()["editTable"]),
        CHANGE_TRANSLATION(LocaleHandler.getDefault()["ChangeTranslation"]),
        MATH_TOGGLE(MathModuleUtils.MATH_TOGGLE),
        NUMBER_LINE(MathModuleUtils.NUMBER_LINE),
        MATRIX(MathModuleUtils.MATRIX),
        SPATIAL_COMBO(MathModuleUtils.SPATIAL_COMBO);
    }

    private open class ContextItem(
        var name: String,
        private val enable: (XMLCaretEvent) -> Boolean,
        private val onSelect: (BBSelectionData) -> Unit
    ) {
        fun test(event: XMLCaretEvent): Boolean {
            return enable(event)
        }

        open fun execute(event: BBSelectionData) {
            onSelect(event)
        }
    }

    private class ContextSubItem(
        name: String,
        enable: (XMLCaretEvent) -> Boolean,
        var menuSource: SharedItem? = null)
            : ContextItem(name, enable, {}) {
                override fun execute(event: BBSelectionData) {}
            }
}
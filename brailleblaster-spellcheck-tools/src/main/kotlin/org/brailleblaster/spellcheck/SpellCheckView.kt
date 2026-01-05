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
package org.brailleblaster.spellcheck

import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.util.Notify
import org.brailleblaster.util.Notify.notify
import org.eclipse.swt.SWT
import org.eclipse.swt.events.FocusEvent
import org.eclipse.swt.events.FocusListener
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.graphics.Font
import org.eclipse.swt.graphics.GC
import org.eclipse.swt.layout.FormAttachment
import org.eclipse.swt.layout.FormData
import org.eclipse.swt.layout.FormLayout
import org.eclipse.swt.widgets.*
import org.eclipse.swt.widgets.List

internal class SpellCheckView(parent: Shell?, private val m: SpellCheckManager) {
    private var shell: Shell = Shell(parent, SWT.APPLICATION_MODAL or SWT.CLOSE)
    private var text: Text
    private lateinit var suggestionBox: List
    private var group: Group
    private var suggestionGroup: Group
    private var replace: Button
    private var replaceAll: Button
    private var ignore: Button
    private var ignoreAll: Button
    private var add: Button
    private var lastItem = 0
    private var currentWord: String? = null
    private var newFont: Font? = null

    init {
        shell.text = localeHandler["spellCheck"]
        setShellScreenLocation(shell.display, shell)
        shell.layout = FormLayout()
        shell.addListener(SWT.Close) { close() }
        shell.addListener(SWT.Traverse) { e: Event ->
            if (e.detail == SWT.TRAVERSE_ESCAPE) {
                close()
                e.detail = SWT.TRAVERSE_NONE
                e.doit = false
            }
        }
        text = Text(shell, SWT.BORDER)
        setLayout(text, 5, 95, 5, 15)
        text.addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent) {
                suggestionBox.deselect(suggestionBox.selectionIndex)
            }

            override fun focusLost(e: FocusEvent) {
                // TODO Auto-generated method stub			
            }
        })
        group = Group(shell, SWT.NONE)
        group.layout = FormLayout()
        setLayout(group, 65, 95, 17, 85)
        replace = Button(group, SWT.PUSH)
        replace.text = localeHandler["spellReplace"]
        setLayout(replace, 0, 100, 0, 20)
        replace.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                if (suggestionBox.selectionCount == 0) {
                    m.replace(text.text)
                    m.checkWord()
                } else if (!(suggestionBox.itemCount == 1 && suggestionBox.getItem(0) == localeHandler["noSuggestion"])) {
                    m.replace(suggestionBox.selection[0])
                    m.checkWord()
                } else {
                    notify(localeHandler["suggestionError"], Notify.ALERT_SHELL_NAME)
                }
            }
        })
        replaceAll = Button(group, SWT.PUSH)
        replaceAll.text = localeHandler["spellReplaceAll"]
        setLayout(replaceAll, 0, 100, 20, 40)
        replaceAll.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                if (suggestionBox.selectionCount == 0) {
                    m.replaceAll(currentWord!!, text.text)
                    m.checkWord()
                } else if (!(suggestionBox.itemCount == 1 && suggestionBox.getItem(0) == localeHandler["noSuggestion"])) {
                    m.replaceAll(currentWord!!, suggestionBox.selection[0])
                    m.checkWord()
                }
            }
        })
        ignore = Button(group, SWT.PUSH)
        //ignore.setText(LocaleHandler.get("spellIgnore"));
        ignore.text = "Skip"
        setLayout(ignore, 0, 100, 40, 60)
        ignore.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                m.checkWord()
            }
        })
        ignoreAll = Button(group, SWT.PUSH)
        //ignoreAll.setText(LocaleHandler.get("spellIgnoreAll"));
        ignoreAll.text = "Ignore Word"
        setLayout(ignoreAll, 0, 100, 60, 80)
        ignoreAll.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                m.ignoreWord(text.text)
                m.checkWord()
            }
        })
        add = Button(group, SWT.NONE)
        add.text = localeHandler["spellAdd"]
        setLayout(add, 0, 100, 80, 100)
        add.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                m.addWord(text.text)
                m.checkWord()
            }
        })
        suggestionGroup = Group(shell, SWT.NONE)
        suggestionGroup.layout = FormLayout()
        suggestionGroup.text = localeHandler["Suggestions"]
        setLayout(suggestionGroup, 5, 64, 17, 85)
        suggestionBox = List(suggestionGroup, SWT.MULTI or SWT.BORDER or SWT.V_SCROLL)
        setLayout(suggestionBox, 0, 100, 0, 100)
        suggestionBox.addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent) {
                suggestionBox.setSelection(lastItem)
            }

            override fun focusLost(e: FocusEvent) {
                lastItem = suggestionBox.selectionIndex
            }
        })
        val tabList = arrayOf<Control>(suggestionGroup, group, text)
        shell.tabList = tabList
        shell.open()
        checkFontSize(replace)
        checkFontSize(ignore)
        checkFontSize(ignoreAll)
        checkFontSize(add)
    }

    private fun setLayout(c: Control, left: Int, right: Int, top: Int, bottom: Int) {
        val data = FormData()
        data.left = FormAttachment(left)
        data.right = FormAttachment(right)
        data.top = FormAttachment(top)
        data.bottom = FormAttachment(bottom)
        c.layoutData = data
    }

    private fun setShellScreenLocation(display: Display, shell: Shell) {
        val primary = display.primaryMonitor
        val bounds = primary.bounds
        val rect = shell.bounds
        shell.setBounds(rect.x + rect.width / 4, rect.y + rect.height / 4, bounds.width / 4, bounds.height / 4)
    }

    fun setWord(word: String?, suggestions: Array<String>) {
        currentWord = word
        text.text = word
        suggestionBox.removeAll()
        for (suggestion in suggestions) {
            suggestionBox.add(suggestion)
        }
        suggestionBox.setFocus()
        suggestionBox.deselectAll()
        suggestionBox.select(0)
    }

    fun close() {
        shell.dispose()
        m.closeSpellChecker()
    }

    private fun checkFontSize(b: Button) {
        var charWidth = getFontWidth(b)
        var stringWidth = b.text.length * charWidth
        val fontData = b.font.fontData
        if (stringWidth > b.bounds.width) {
            while (stringWidth > b.bounds.width) {
                val sSize = fontData[0].getHeight() - 1
                fontData[0].setHeight(sSize)
                if (newFont != null && !newFont!!.isDisposed) newFont!!.dispose()
                newFont = Font(Display.getCurrent(), fontData[0])
                b.font = newFont
                charWidth = getFontWidth(b)
                stringWidth = b.text.length * charWidth
            }
        }
    }

    private fun getFontWidth(b: Button?): Int {
        val gc = GC(b)
        val fm = gc.fontMetrics
        gc.dispose()
        return fm.averageCharacterWidth.toInt()
    }

    companion object {
        private val localeHandler = getDefault()
    }
}

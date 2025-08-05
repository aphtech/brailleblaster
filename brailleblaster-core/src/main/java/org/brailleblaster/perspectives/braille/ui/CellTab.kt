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
import org.brailleblaster.bbx.BBX
import org.brailleblaster.math.mathml.MathModule
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.perspectives.braille.messages.TabInsertionMessage
import org.brailleblaster.perspectives.braille.searcher.Searcher
import org.brailleblaster.perspectives.braille.views.wp.TextView
import org.brailleblaster.perspectives.braille.views.wp.ViewStateObject
import org.brailleblaster.perspectives.mvc.menu.BBSelectionData
import org.brailleblaster.perspectives.mvc.menu.TopMenu
import org.brailleblaster.tools.MenuToolModule
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler2
import org.brailleblaster.exceptions.BBNotifyException
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.MessageBox
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Text
import org.slf4j.LoggerFactory

class CellTab(var manager: Manager, var currentElement: TextMapElement, var textView: TextView,
              var stateObj: ViewStateObject
) {
    var tabShell: Shell = Shell(manager.wpManager.shell)
    var tab: Text
    var buttonOK: Button
    var existingTab: Element? = null

    init {
        tabShell.setSize(400, 100)
        tabShell.text = "Set Cell Position"
        tabShell.layout = FillLayout(SWT.VERTICAL)
        tab = Text(tabShell, SWT.BORDER)
        buttonOK = Button(tabShell, SWT.PUSH)
        buttonOK.text = "OK"
        if (textView.view.caretOffset == currentElement.getEnd(manager.mapList)) {
            throw BBNotifyException("Cannot apply to end of line")
        } else if (MathModule.isMath(currentElement.node)) {
            throw BBNotifyException("Cannot apply to math")
        } else if (textView.view.caretOffset == currentElement.getStart(manager.mapList)) {
            existingTab = (FastXPath.preceding(currentElement.node)
                .firstOrNull { Searcher.Filters.noUTDAncestor(it) && BBX.SPAN.TAB.isA(it) && it is Element } as Element?)?.also { tab.text = BBX.SPAN.TAB.ATTRIB_VALUE.getAttribute(it).value }
        }
        intializeListeners()
    }

    private fun intializeListeners() {
        val cellsPerLine = (manager.document.engine.pageSettings.drawableWidth
                / manager.document.engine.brailleSettings.cellType.width.toDouble()).toInt()
        buttonOK.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                val v = getNumber(tab.text)
                if (v >= cellsPerLine) {
                    val msg = MessageBox(tabShell)
                    msg.message = "Cell position exceeds maximum number of cells per line."
                    msg.open()
                } else if (v >= 1) {
                    addTabElement(v, currentElement)
                    tabShell.close()
                } else tabShell.close()
            }
        })
        tab.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == SWT.CR.code || e.keyCode == SWT.KEYPAD_CR) {
                    val v = getNumber(tab.text)
                    if (v >= cellsPerLine) {
                        val msg = MessageBox(tabShell)
                        msg.message = "Cell position exceeds maximum number of cells per line."
                        msg.open()
                    } else if (v >= 1) {
                        addTabElement(v, currentElement)
                        tabShell.close()
                    } else tabShell.close()
                } else if (e.keyCode == SWT.ESC.code) {
                    tabShell.close()
                }
            }
        })
    }

    fun open() {
        tabShell.open()
    }

    private fun addTabElement(tabValue: Int, currentElement: TextMapElement) {
        val xIndex = textView.getString(
            stateObj.currentStart,
            textView.view.caretOffset - stateObj.currentStart
        ).length
        log.debug(
            "tabValue {} element {}",
            tabValue,
            existingTab?.let {
                XMLHandler2.toXMLSimple(
                    it
                )
            }
        )
        val existingTabValue = if (existingTab == null) null else BBX.SPAN.TAB.ATTRIB_VALUE[existingTab]
        val m =
            if (existingTab != null) TabInsertionMessage( /*stupid workaround as TabInsertionHandler is waaay to many layers away*/
                tabValue - existingTabValue!!,
                existingTab
            ) else TabInsertionMessage(tabValue, xIndex, currentElement)
        manager.dispatch(m)
    }

    private fun getNumber(num: String): Int {
        val `val`: Int = try {
            num.toInt()
        } catch (e: NumberFormatException) {
            0
        }
        return `val`
    }

    companion object {
        private val log = LoggerFactory.getLogger(CellTab::class.java)
        const val MENU_NAME = "Set Cell Position"
    }
}
object CellTabTool : MenuToolModule {
    override val topMenu: TopMenu = TopMenu.EDIT
    override val title: String = CellTab.MENU_NAME
    override fun onRun(bbData: BBSelectionData) {
        val tab = CellTab(bbData.manager, bbData.manager.mapList.current, bbData.manager.text, bbData.manager.text.state)
        tab.open()
    }
}
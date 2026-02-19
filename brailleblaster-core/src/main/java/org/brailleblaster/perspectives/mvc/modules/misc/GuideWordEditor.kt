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

import nu.xom.Attribute
import nu.xom.Element
import nu.xom.Text
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.utd.utils.getTextChild
import org.eclipse.swt.SWT
import org.eclipse.swt.events.KeyAdapter
import org.eclipse.swt.events.KeyEvent
import org.eclipse.swt.events.SelectionAdapter
import org.eclipse.swt.events.SelectionEvent
import org.eclipse.swt.layout.FillLayout
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Shell

object GuideWordEditor {
    private var contractionShell: Shell? = null
    fun open(manager: Manager) {
        val cs = contractionShell
        if (cs == null || cs.isDisposed) {
            contractionShell = Shell(manager.display.activeShell).apply {
                processShell(this, manager)
                open()
            }
        } else {
            cs.forceActive()
        }
    }

    private fun processShell(shell: Shell, manager: Manager) {
        shell.setSize(400, 100)
        shell.text = "Edit Guide Word"
        shell.layout = FillLayout(SWT.VERTICAL)
        createContents(shell, manager)
    }

    var contractedStr: String? = null
    private fun createContents(contractionShell: Shell, manager: Manager) {
        val contraction = org.eclipse.swt.widgets.Text(contractionShell, SWT.BORDER)
        val buttonOK = Button(contractionShell, SWT.PUSH)
        buttonOK.text = "OK"
        var text = manager.simpleManager.currentSelection.end.node
        if (text is Text) {
            text = text.parent
        }
        val dtElement = text as Element
        if (dtElement.getAttribute("contraction") == null) {
            contraction.text = getTextChild(dtElement).value
            contractedStr = getTextChild(dtElement).value
        } else {
            contraction.text = dtElement.getAttributeValue("contraction")
            contractedStr = dtElement.getAttributeValue("contraction")
        }
        contraction.addListener(SWT.Modify) { contractedStr = contraction.text }
        buttonOK.addSelectionListener(object : SelectionAdapter() {
            override fun widgetSelected(e: SelectionEvent) {
                dtElement.addAttribute(Attribute("contraction", contractedStr))
                manager.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, true, dtElement))
                contractionShell.close()
            }
        })
        contraction.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == SWT.CR.code || e.keyCode == SWT.KEYPAD_CR) {
                    dtElement.addAttribute(Attribute("contraction", contractedStr))
                    manager.simpleManager.dispatchEvent(ModifyEvent(Sender.NO_SENDER, true, dtElement))
                    contractionShell.close()
                }
            }
        })
    }
}
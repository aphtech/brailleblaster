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
package org.brailleblaster.utils.swt

import org.eclipse.swt.SWT
import org.eclipse.swt.custom.CCombo
import org.eclipse.swt.events.*
import org.eclipse.swt.widgets.*
import java.util.function.Consumer

/**
 * Provides an easy way to apply listeners to controls.
 */
object EasyListeners {
    @JvmStatic
    fun selection(button: Button, onSelection: Consumer<SelectionEvent>) {
        button.addSelectionListener(
            object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent) {
                    onSelection.accept(e)
                }
            })
    }

    @JvmStatic
    fun selection(combo: Combo, onSelection: Consumer<SelectionEvent>) {
        combo.addSelectionListener(
            object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent) {
                    onSelection.accept(e)
                }
            })
    }

    @JvmStatic
    fun keyPress(control: Control, onKeyPress: Consumer<KeyEvent>) {
        control.addKeyListener(
            object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    onKeyPress.accept(e)
                }
            })
    }

    fun keyReleased(control: Control, onKeyReleased: Consumer<KeyEvent>) {
        control.addKeyListener(
            object : KeyAdapter() {
                override fun keyReleased(e: KeyEvent) {
                    onKeyReleased.accept(e)
                }
            })
    }

    fun traverse(control: Control, onTraverse: Consumer<TraverseEvent>) {
        control.addTraverseListener(onTraverse::accept)
    }

    @JvmStatic
    fun modify(text: Text, onModify: Consumer<ModifyEvent>) {
        text.addModifyListener { t: ModifyEvent -> onModify.accept(t) }
    }

    @JvmStatic
    fun verifyNumbersOnly(text: Text) {
        text.addVerifyListener { e: VerifyEvent ->
            if (e.keyCode == SWT.BS.code || e.keyCode == SWT.DEL.code || e.keyCode == SWT.ARROW_RIGHT || e.keyCode == SWT.ARROW_LEFT) return@addVerifyListener
            if (e.text.chars().anyMatch { i: Int -> i !in 0x30..0x39 }) e.doit = false
        }
    }

    fun verifyHexadecimal(text: Text) {
        text.addVerifyListener { e: VerifyEvent ->
            if (e.keyCode == SWT.BS.code || e.keyCode == SWT.DEL.code || e.keyCode == SWT.ARROW_RIGHT || e.keyCode == SWT.ARROW_LEFT
            ) return@addVerifyListener
            //Also counts empty/blank strings so that a text box can be cleared.
            e.doit = (e.text.matches("-?[0-9a-fA-F]+".toRegex()) || e.text.isEmpty() || e.text.isBlank())
        }
    }

    /**
     * "Pauses" a listener by removing all listeners of the given type from the widget, running the
     * Consumer, and then re-adding all of the listeners.
     *
     * @param widget    Widget to be paused
     * @param type      SWT event type
     * @param procedure Code to be executed while paused
     */
    @JvmStatic
    fun pauseListener(widget: Widget, type: Int, procedure: Consumer<Widget>) {
        val listeners = widget.getListeners(type)
        for (listener in listeners) {
            widget.removeListener(type, listener)
        }
        procedure.accept(widget)
        for (listener in listeners) {
            widget.addListener(type, listener)
        }
    }

    fun selection(widget: CCombo, onSelect: Consumer<SelectionEvent>) {
        widget.addSelectionListener(
            object : SelectionAdapter() {
                override fun widgetSelected(e: SelectionEvent) {
                    onSelect.accept(e)
                }
            })
    }
}

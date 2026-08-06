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
package org.brailleblaster.embossers

import org.brailleblaster.libembosser.spi.EmbosserOption
import org.brailleblaster.libembosser.spi.OptionIdentifier
import org.brailleblaster.utils.localization.LocaleHandler
import org.brailleblaster.utils.ByteStringEncoding
import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.ScrolledComposite
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.Composite
import org.eclipse.swt.widgets.Control
import java.util.*
import kotlin.properties.Delegates

class EmbosserOptionsView(parent: Composite, style: Int = SWT.BORDER or SWT.H_SCROLL or SWT.V_SCROLL, options: Map<OptionIdentifier, EmbosserOption> = mapOf()) {
    @FunctionalInterface
    interface ValidateListener {
        fun onValidate(errors: Set<OptionIdentifier>)
    }
    private lateinit var mutableOptions: MutableMap<OptionIdentifier, EmbosserOption>
    var embosserOptions: Map<OptionIdentifier, EmbosserOption>
        get() = mutableOptions.toMap()
        set(value) {
            mutableOptions = value.toMutableMap()
            updateOptionsView(value)
        }
    var optionErrors: Set<OptionIdentifier> by Delegates.observable(linkedSetOf()) { _, _, newValue ->
        validateListeners.onEach { it.onValidate(newValue) }
    }
    private set
    private val validateListeners: MutableSet<ValidateListener> = mutableSetOf()
    fun addValidateListener(listener: ValidateListener) {
        validateListeners += listener
    }
    fun addValidateListener(listener: (Set<OptionIdentifier>) -> Unit) {
        addValidateListener(object : ValidateListener {
            override fun onValidate(errors: Set<OptionIdentifier>) {
                listener(errors)
            }
        })
    }
    fun removeValidateListener(listener: ValidateListener) {
        validateListeners -= listener
    }

    val control: Control
        field = ScrolledComposite(parent, style).apply {
            expandHorizontal = true
            expandVertical = true
            showFocusedControl = true
        }

    private fun updateOptionsView(options: Map<OptionIdentifier, EmbosserOption>) {
        control.content?.dispose()
        val newContent = if (options.isEmpty()) {
            EasySWT.makeLabel(control).text(LocaleHandler.getDefault()["EmbosserEditDialog.optionsView.noOptions"]).get()
        } else {
            val optionsContainer = Composite(control, SWT.NONE)
            optionsContainer.layout = GridLayout(2, false)
            for ((k, v) in options) {
                EasySWT.makeLabel(optionsContainer).text(k.getDisplayName(Locale.getDefault()))
                when(v) {
                    is EmbosserOption.BooleanOption -> {
                        val selIndex = if (v.boolean) 0 else 1
                        val combo = EasySWT.makeComboDropdown(optionsContainer).add(true.toString()).add(false.toString()).select(selIndex).get()
                        EasySWT.addSelectionListener(combo) { mutableOptions[k] = v.copy(combo.text) }
                    }
                    is EmbosserOption.ByteArrayOption -> {
                        val text = EasySWT.makeTextBuilder(optionsContainer).text(ByteStringEncoding.encode(v.bytes)).get()
                        text.addModifyListener { kotlin.runCatching { v.copy(ByteStringEncoding.decode(text.text)) }.onSuccess {
                            mutableOptions[k] = it
                            optionErrors = optionErrors - k
                        }.onFailure {
                            optionErrors = optionErrors + k
                        } }
                    }
                    else -> {
                        val text = EasySWT.makeTextBuilder(optionsContainer).text(v.value).get()
                        text.addModifyListener { kotlin.runCatching { v.copy(text.text)}.onSuccess {
                            mutableOptions[k] = it
                            optionErrors = optionErrors - k
                        }.onFailure { optionErrors = optionErrors + k } }
                    }
                }
            }
            optionsContainer
        }
        control.content = newContent
        control.setMinSize(newContent.computeSize(SWT.DEFAULT, SWT.DEFAULT))
    }
    init {
        embosserOptions = options
    }
}
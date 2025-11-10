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
package org.brailleblaster.math

import org.brailleblaster.utils.localization.LocaleHandler.Companion.getDefault
import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.perspectives.braille.toolbar.ToolBarBuilder
import org.brailleblaster.perspectives.mvc.modules.views.DebugModule
import org.brailleblaster.util.ImageHelper
import org.brailleblaster.utils.swt.EasySWT
import org.eclipse.swt.SWT
import org.eclipse.swt.custom.ScrolledComposite
import org.eclipse.swt.custom.StyledText
import org.eclipse.swt.events.ControlEvent
import org.eclipse.swt.events.ControlListener
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.layout.GridData
import org.eclipse.swt.layout.GridLayout
import org.eclipse.swt.widgets.*
import kotlin.io.path.exists

object AboutMathDialog {

    fun createDialog(parent: Shell?) {
        val shell = Shell(parent, SWT.DIALOG_TRIM or SWT.RESIZE)
        shell.data = GridData(4, 4, true, true)
        shell.layout = GridLayout(1, false)
        shell.text = MathModuleUtils.MATH_HELP
        val outer = Composite(shell, SWT.NONE)
        outer.layout = GridLayout(1, false)
        outer.layoutData = GridData(4, 4, true, true)
        val sc = ScrolledComposite(outer, SWT.V_SCROLL or SWT.H_SCROLL or SWT.BORDER)
        sc.expandHorizontal = true
        sc.expandVertical = true
        val comp = Composite(sc, SWT.NONE)
        comp.layoutData = GridData(4, 4, true, true)
        comp.layout = GridLayout(3, false)
        val debug = DebugModule.enabled
        for (item in MathOption.entries) {
            if (debug || item.enabled) {
                val text = Text(comp, SWT.NONE)
                text.data = GridData()
                text.text = item.prettyString
                val imageLabel = Label(comp, SWT.NONE)
                imageLabel.data = GridData()
                val i = getImage(item.key)
                if (i == null) {
                    imageLabel.text = item.prettyString
                } else {
                    imageLabel.image = i
                }

                // get description
                val description = localeHandler[MathModuleUtils.HELP_PREFIX_KEY + item.key]
                val textLabel = StyledText(comp, SWT.NONE)
                textLabel.data = GridData()
                textLabel.text = wrapText(description, 80)
                textLabel.editable = false
            }
        }
        addResizeListener(shell, sc, comp)
        EasySWT.addEscapeCloseListener(shell)
        sc.content = comp
        shell.pack()
        shell.layout(true)
        shell.open()
    }

    private fun addResizeListener(control: Control, sc: ScrolledComposite, comp: Composite) {
        control.addControlListener(object : ControlListener {
            override fun controlMoved(e: ControlEvent) {
                // TODO Auto-generated method stub
            }

            override fun controlResized(e: ControlEvent) {
                comp.layout()
                sc.setMinSize(comp.computeSize(SWT.DEFAULT, SWT.DEFAULT))
            }
        })
    }

    private val localeHandler = getDefault()
    private fun getImage(imgName: String): Image? {
        val imagePath = ImageHelper.imagesPath
        val fileName = ToolBarBuilder.TOOLBAR_FOLDER + "large" + "/" + imgName + ".png"
        return if (!imagePath.resolve(fileName).exists()) {
            null
        } else ImageHelper.createScaledImage(fileName, 1f)
    }

    private fun wrapText(s: String, charsPerLine: Int): String {
        val s2 = StringBuilder()
        var curChars = 0
        val array = s.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (i in array.indices) {
            if (curChars > charsPerLine) {
                s2.append("\n")
                curChars = 0
            }
            s2.append(array[i]).append(" ")
            curChars += array[i].length
        }
        return s2.toString()
    }
}
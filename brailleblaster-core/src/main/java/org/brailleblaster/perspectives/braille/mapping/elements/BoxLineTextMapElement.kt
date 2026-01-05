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
package org.brailleblaster.perspectives.braille.mapping.elements

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.perspectives.braille.mapping.interfaces.Uneditable
import org.brailleblaster.utd.actions.TransNoteAction
import java.util.*

class BoxLineTextMapElement(n: Node, parent: Element, isStartSeparator: Boolean) : TextMapElement(n),
    Uneditable {
    val parent: Element
    private var boxText = -1
    private var printText: String? = null
    val isStartSeparator: Boolean

    init {
        this.brailleList = LinkedList()
        this.parent = parent
        this.isStartSeparator = isStartSeparator
    }

    private fun makePrintText(): String {
        val str = StringBuilder()
        val num = brailleList[boxText].node.value.length
        var color = ""
        if ((brailleList[boxText].node.parent as Element).getAttributeValue("color") != null
            && !(brailleList[boxText].node.parent as Element).getAttributeValue("color").isNullOrEmpty()
        ) {
            color = TransNoteAction.START + (brailleList[boxText].node.parent as Element)
                .getAttributeValue("color") + TransNoteAction.END + " "
        }
        for (i in 0 until num) {
            if (i == num - 1 && !str.toString().contains(TransNoteAction.END)
                && str.toString().contains(TransNoteAction.START)
            ) {
                str.append(TransNoteAction.END)
            } else if (i < color.length) {
                str.append(color[i])
            } else {
                str.append('-')
            }
        }

        return str.toString()
    }

    override fun getText(): String {
        val pt = printText
        return if (pt != null) pt
        else {
            setTextIndex()
            makePrintText().also {
                printText = it
            }
        }
    }

    override fun textLength(): Int {
        return text.length
    }

    override fun getNodeParent(): Element {
        return this.parent
    }

    private fun setTextIndex() {
        for (i in brailleList.indices) {
            if (brailleList[i] is BrlOnlyBrlMapElement) boxText = i
        }
    }

    val isOpeningBoxline: Boolean
        get() = node.parent == parent
}

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
package org.brailleblaster.perspectives.mvc

import nu.xom.Text

/**
 * Represent a position in XML
 */
class XMLTextCaret @JvmOverloads constructor(
    textNode: Text,
    offset: Int,
    cursorPosition: CursorPosition? = CursorPosition.ALL
) : XMLNodeCaret(textNode, cursorPosition!!) {
	val offset: Int

    init {
        //this.offset = offset;
        val textLen = textNode.value.length
        if (offset !in 0..textLen) {
            this.cursorPosition = CursorPosition.AFTER
            this.offset = textLen
        } else this.offset = offset
    }

    override val node: Text
        get() = super.node as Text

    override fun toString(): String {
        return "XMLTextCaret{node=" + node + "offset=" + offset + "/" + node.value.length + ", cursorPosition=" + cursorPosition + '}'
    }
}

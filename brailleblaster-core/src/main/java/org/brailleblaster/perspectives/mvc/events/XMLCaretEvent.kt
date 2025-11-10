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
package org.brailleblaster.perspectives.mvc.events

import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.SimpleEvent
import org.brailleblaster.perspectives.mvc.XMLNodeCaret

class XMLCaretEvent @JvmOverloads constructor(sender: Sender, start: XMLNodeCaret?, end: XMLNodeCaret? = start) :
    SimpleEvent(sender) {
    @JvmField
	  val start: XMLNodeCaret
    @JvmField
	  val end: XMLNodeCaret

    init {
        if (start == null) {
            throw NullPointerException("start")
        } else if (end == null) {
            throw NullPointerException("end")
        }
        this.start = start
        this.end = end
    }

    val isSingleNode: Boolean
        get() = start === end

    override fun toString(): String {
        return "XMLCaretEvent{sender=" + sender + ", start=" + start + ", end=" + (if (end === start) "same as start" else end) + '}'
    }
}
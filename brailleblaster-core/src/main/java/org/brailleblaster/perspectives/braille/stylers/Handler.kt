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
package org.brailleblaster.perspectives.braille.stylers

import nu.xom.Element
import nu.xom.Node
import nu.xom.ParentNode
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.perspectives.braille.mapping.elements.WhiteSpaceElement
import org.brailleblaster.perspectives.braille.mapping.interfaces.Uneditable
import org.brailleblaster.perspectives.braille.mapping.maps.MapList
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.viewInitializer.ViewInitializer
import org.brailleblaster.perspectives.braille.views.wp.BrailleView
import org.brailleblaster.perspectives.braille.views.wp.TextView
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.utd.IStyle
import org.brailleblaster.utd.actions.PageAction

abstract class Handler(@JvmField protected val manager: Manager, @JvmField protected val vi: ViewInitializer, @JvmField protected val list: MapList) {
    @JvmField
	protected val text: TextView = manager.text
    protected val braille: BrailleView = manager.braille

    protected fun reformat(n: Node, translate: Boolean) {
        manager.simpleManager.dispatchEvent(ModifyEvent(Sender.HANDLER, translate, n))
    }

    protected fun reformat(nodes: List<Node>, translate: Boolean) {
        manager.simpleManager.dispatchEvent(ModifyEvent(Sender.HANDLER, nodes, translate))
    }

    protected fun removeListeners() {
        text.removeListeners()
        braille.removeListeners()
    }

    protected fun initializeListeners() {
        text.initializeListeners()
        braille.initializeListeners()
    }

    protected fun getParent(indexes: ArrayList<Int>): ParentNode? {
        var e = if (indexes.isNotEmpty()) manager.document.doc.document.getChild(indexes[0]) as Element else null

        for (i in 1 until indexes.size) e = e!!.getChild(indexes[i]) as Element

        return e
    }

    protected fun isBoxLine(e: Element?): Boolean {
        val style = getStyle(e)
        return !style.startSeparator.isNullOrEmpty() && !style.endSeparator.isNullOrEmpty()
    }


    protected fun isWhitespace(t: TextMapElement?): Boolean {
        return t is WhiteSpaceElement
    }

    protected fun readOnly(t: TextMapElement?): Boolean {
        return t is Uneditable
    }

    protected fun readOnly(e: Element?): Boolean {
        return manager.getAction(e) is PageAction || isBoxLine(e)
    }

    protected fun getStyle(n: Node?): IStyle {
        return manager.document.engine.styleMap.findValueOrDefault(n!!)
    }

    protected fun findParent(t: TextMapElement): Element {
        return manager.document.getParent(t.node)
    }

}

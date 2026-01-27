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
package org.brailleblaster.abstractClasses

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.utd.properties.UTDElements

object ViewUtils {

    fun followsMoveTo(n: Node): Boolean {
        val parent = n.parent as Element
        val index = parent.indexOf(n)

        return index > 0 && UTDElements.MOVE_TO.isA(parent.getChild(index - 1))
    }

    fun followsBraillePage(n: Node): Boolean {
        val parent = n.parent as Element
        val index = parent.indexOf(n)

        if (index > 0 && (parent.getChild(index - 1) is Element)) {
            return UTDElements.BRLONLY.isA(parent.getChild(index - 1))
        }
        return false
    }

    fun followsNewPage(n: Node): Boolean {
        val parent = n.parent as Element
        val index = parent.indexOf(n)

        return index > 0 && UTDElements.NEW_PAGE.isA(parent.getChild(index - 1))
    }

    @JvmStatic
    fun getAttributeValue(e: Element, `val`: String?): String? {
        val atr = e.getAttribute(`val`)
        return atr?.value
    }
}

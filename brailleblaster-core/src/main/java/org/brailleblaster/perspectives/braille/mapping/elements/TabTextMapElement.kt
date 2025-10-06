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
import nu.xom.ParentNode
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.interfaces.Deletable
import org.brailleblaster.perspectives.braille.mapping.interfaces.Uneditable
import org.brailleblaster.utd.utils.UTDHelper.Companion.stripUTDRecursive
import org.brailleblaster.util.Utils.combineAdjacentTextNodes

open class TabTextMapElement(n: Node) : TextMapElement(n), Deletable, Uneditable {
    override fun deleteNode(m: Manager): ParentNode? {
        val parent = node.parent as Element
        node.detach()
        stripUTDRecursive(parent)
        combineAdjacentTextNodes(parent)
        return parent
    }
}

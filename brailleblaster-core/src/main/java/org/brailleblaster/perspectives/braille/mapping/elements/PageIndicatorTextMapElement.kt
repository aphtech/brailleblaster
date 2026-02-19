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
import org.brailleblaster.bbx.BBX
import org.brailleblaster.exceptions.OutdatedMapListException
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.mapping.interfaces.Deletable
import org.brailleblaster.perspectives.braille.mapping.interfaces.Uneditable
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.stripUTDRecursive
import org.brailleblaster.utils.xom.childNodes
import org.brailleblaster.util.Utils

class PageIndicatorTextMapElement(node: Element?) : TextMapElement(node), Uneditable, Deletable {

    init {
        require(!(!BBX.BLOCK.PAGE_NUM.isA(node) && !BBX.SPAN.PAGE_NUM.isA(node))) { "PageIndicatorTME must be a PageNum block or PageNum span" }
    }

    /**
     * Searches UTDML markup to find the print page translation within a pagenum element
     * this is the braille representation in the UTDML markup, not the text representation
     *
     * @param e: Element to search
     * @return the text node containing the page representation, null if not found
     */
    fun findBraillePageNode(e: Node): Element? = e.childNodes.firstOrNull { UTDElements.BRL.isA(it) } as Element?

    override fun getText(): String =
        brailleList.filter { brailleMapElement -> UTDElements.BRLONLY.isA(brailleMapElement.node) }
            .firstNotNullOfOrNull { (it.node as Element).getAttributeValue("printIndicator") } ?: ""

    override fun getNodeParent(): Element {
        return node as Element
    }

    override fun deleteNode(m: Manager): ParentNode? {
        if (BBX.BLOCK.PAGE_NUM.isA(node)) {
            val parent = node.parent
            node.detach()
            return parent
        } else {
            if (BBX.SPAN.PAGE_NUM.isA(node)) {
                val parent = node.parent as Element
                node.detach()
                parent.stripUTDRecursive()
                Utils.combineAdjacentTextNodes(parent)
                return parent
            }
        }
        throw OutdatedMapListException("PageIndicator " + node.value + " is not a block and has no parent span")
    }
}

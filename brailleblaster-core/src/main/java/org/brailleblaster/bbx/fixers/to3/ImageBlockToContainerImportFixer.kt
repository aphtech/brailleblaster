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
package org.brailleblaster.bbx.fixers.to3

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.fixers.AbstractFixer
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utils.xom.childNodes

class ImageBlockToContainerImportFixer : AbstractFixer() {
    override fun fix(matchedNode: Node) {
        BBX.SPAN.IMAGE.assertIsA(matchedNode)
        convertImageBlockToContainer(matchedNode as Element)
    }

    companion object {
        @JvmStatic
		fun convertImageBlockToContainer(imgElem: Element) {
            if (!BBX.SPAN.IMAGE.isA(imgElem) && !BBX.CONTAINER.IMAGE.isA(imgElem)) {
                throw NodeException("Not an image", imgElem)
            }
            val parentBlock = requireNotNull(
                XMLHandler.ancestorVisitorElement(imgElem) { node -> BBX.BLOCK.isA(node) }) {
                "Cannot find a parent block"
        }
            if (BBX.SPAN.IMAGE.isA(imgElem) && imgElem.childCount != 0) {
                val wrappingBlock = BBX.BLOCK.DEFAULT.create()
                for (curChild in imgElem.childNodes) {
                    curChild.detach()
                    wrappingBlock.appendChild(curChild)
                }
                imgElem.appendChild(wrappingBlock)
            }
            BBX.transform(imgElem, BBX.CONTAINER.IMAGE)

            //ignore everything in between
            imgElem.detach()
            parentBlock.parent.replaceChild(parentBlock, imgElem)
        }
    }
}
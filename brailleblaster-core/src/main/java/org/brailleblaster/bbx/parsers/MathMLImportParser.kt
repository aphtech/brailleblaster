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
package org.brailleblaster.bbx.parsers

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.parsers.ImportParser.OldDocumentAction
import org.brailleblaster.math.mathml.MathModuleUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Suppress("UNUSED")
class MathMLImportParser : ImportParser {
    override fun parseToBBX(oldNode: Node, bbxCursor: Element): OldDocumentAction {
        val mathMLContainer = BBX.INLINE.MATHML.create()
        if (oldNode is Element) {
            val newMathML = oldNode.copy()
            try {
                MathModuleUtils.setASCIIText(newMathML)
            } catch (e: Exception) {
                logger.error("Problem generating ASCIIMath", e)
            }
            mathMLContainer.appendChild(newMathML)
            bbxCursor.appendChild(mathMLContainer)
        } else {
            logger.error("Not importing node ${oldNode.toXML()} as not an Element")
        }
        return OldDocumentAction.NEXT_SIBLING
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(MathMLImportParser::class.java)
    }
}
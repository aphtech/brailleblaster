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
package org.brailleblaster.pandoc

import nu.xom.Element
import nu.xom.XPathContext
import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.utils.xml.MATHML_NS
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class FixMathML : FixerInf {

    private var bbUri: String? = null
    private var rootElement: Element? = null
    override fun setFixer(fixer: Fixer) {
        bbUri = fixer.bbUri
        rootElement = fixer.rootElement
    }

    override fun process() {
        val xpathContext = XPathContext().apply {
            addNamespace("m", MATHML_NS)
            addNamespace("bb", bbUri)
        }
        val mathElements = rootElement!!.query("descendant-or-self::m:math", xpathContext)
        mathElements.forEach {
            try {
                MathModuleUtils.setASCIIText(it as Element)
            } catch(e: Exception) {
                logger.error("Problem generating ASCIIMath", e)
            }
        }
    }
    companion object {
        val logger: Logger = LoggerFactory.getLogger(FixMathML::class.java)
    }

}
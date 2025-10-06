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
package org.brailleblaster.utd.actions

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.utd.ITranslationEngine
import org.brailleblaster.utd.TextSpan
import org.brailleblaster.utd.exceptions.UTDTranslateException
import org.brailleblaster.utd.properties.UTDElements
import org.mwhapples.jlouis.Louis
import org.mwhapples.jlouis.TranslationException
import jakarta.xml.bind.annotation.XmlElement

class AttributeAction : IAction {
    @XmlElement(name = "attribute")
    var attributeName: String
        private set

    // Needed for JAXB
    @Suppress("UNUSED")
    private constructor() {
        attributeName = ""
    }

    constructor(attrName: String) {
        attributeName = attrName
    }

    override fun applyTo(node: Node, context: ITranslationEngine): List<TextSpan> {
        val result: MutableList<TextSpan> = ArrayList()
        val text = (node as Element).getAttributeValue(attributeName)?:""
        val ts = TextSpan(node, text)
        ts.isTranslated = true
        val translator = context.brailleTranslator
        val brailleSettings = context.brailleSettings
        // TODO:  convert to LibLouisAPH
        val tables = brailleSettings.mainTranslationTable
        val mode = if (brailleSettings.isUseAsciiBraille) 0 else Louis.TranslationModes.DOTS_IO or Louis.TranslationModes.UC_BRL
        try {
            val transResult = translator.translateString(tables, text, mode)
            val brlElement = UTDElements.BRL.create()
            brlElement.appendChild(transResult)
            ts.brlElement = brlElement
        } catch (e: TranslationException) {
            throw UTDTranslateException("Problem with Braille translation, see log for details", e)
        }
        result.add(ts)
        return result
    }
}
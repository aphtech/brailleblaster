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

package org.brailleblaster.utd.mathactions

import nu.xom.Attribute
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.utd.BrailleSettings
import org.brailleblaster.utd.MathBraileCode
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.ITranslationEngine
import org.brailleblaster.utd.MathIndicators
import org.brailleblaster.utd.TextSpan
import org.brailleblaster.utd.actions.IBlockAction
import org.brailleblaster.utd.asciimath.AsciiMathConverter
import org.brailleblaster.utd.properties.ContentType
import org.brailleblaster.utd.utils.getAssociatedBrlElement
import org.brailleblaster.utils.braille.BrailleUnicodeConverter
import org.brailleblaster.utils.xom.childNodes
import org.brailleblaster.utils.braille.singleThreadedMathCAT
import java.util.concurrent.ExecutionException

private fun String.applyAsciiBraille(asciiBraille: Boolean): String = if (asciiBraille) BrailleUnicodeConverter.unicodeToAsciiLouis(this) else this
private fun String.applyMathIndicators(mathIndicators: MathIndicators): String = "${mathIndicators.start}$this${mathIndicators.end}"
private fun Element.isMathML(): Boolean = localName == "math"
private fun Element.isMathTME(): Boolean = localName == "tme" && (getAttributeValue("emphasis")?:"").split(',').contains("MATH")
private fun createMathTextSpan(node: Node, text: String, braille: String): TextSpan {
    val brl = UTDElements.BRL.create().apply {
        val indices = generateSequence { "0" }.take(braille.length).joinToString(" ")
        addAttribute(Attribute("index", indices))
        addAttribute(Attribute("text", text))
        insertChild(braille, 0)
    }
    node.parent?.let { parent ->
        val index = parent.indexOf(node)
        if (getAssociatedBrlElement(parent, index) != null) {
            parent.removeChild(index+1)
        }
        parent.insertChild(brl, index+1)
    }
    return TextSpan(node, text).apply {
    isTranslated = true
        contentType = ContentType.Math
        brlElement = brl
    }
}

@Suppress("UNUSED")
class MathCatAction : IBlockAction {
    override fun applyTo(node: Node, context: ITranslationEngine): List<TextSpan> {
        val brailleSettings = context.brailleSettings
        return when(node) {
            is Element -> translateElement(node, brailleSettings)
            is Text -> listOf(createMathTextSpan(node, node.value, translateAsciiMath(node.value, brailleSettings.mathBrailleCode).applyMathIndicators(brailleSettings.mathIndicators).applyAsciiBraille(brailleSettings.isUseAsciiBraille)))
            else -> throw RuntimeException("Cannot translate node ${node.toXML()}")
        }
    }
    private fun translateElement(element: Element, brailleSettings: BrailleSettings): List<TextSpan> = when {
            element.isMathML() -> listOf(createMathTextSpan(element, "", translateMathML(element.toXML(), brailleSettings.mathBrailleCode).applyMathIndicators(brailleSettings.mathIndicators).applyAsciiBraille(brailleSettings.isUseAsciiBraille)))
            element.isMathTME() -> element.childNodes.filterIsInstance<Text>().map { createMathTextSpan(it, it.value, translateAsciiMath(it.value, brailleSettings.mathBrailleCode).applyMathIndicators(brailleSettings.mathIndicators).applyAsciiBraille(brailleSettings.isUseAsciiBraille)) }
            else -> throw RuntimeException("Unknown element type ${element.toXML()}")
    }
}

fun translateAsciiMath(text: String, brailleCode: MathBraileCode): String {
    return AsciiMathConverter.toMathML(text,true).joinToString(separator = "") { translateMathML(it.toXML(), brailleCode) }
}
private fun translateMathML(mml: String, brailleCode: MathBraileCode): String = try {
        singleThreadedMathCAT {
            setPreference("BrailleCode", brailleCode.preferenceName)
            setMathml(mml)
            braille
        }
    } catch (ex: ExecutionException) {
        throw RuntimeException("Problem translating MathML \"$mml\"", ex.cause)
    }

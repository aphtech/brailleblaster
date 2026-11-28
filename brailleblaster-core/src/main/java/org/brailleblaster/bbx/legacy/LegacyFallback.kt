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
package org.brailleblaster.bbx.legacy

import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utils.xml.BB_NS
import org.brailleblaster.utils.xml.UTD_NS

/**
 * Turns content that cannot be converted exactly into plain Body Text blocks, keeping its text.
 */
object LegacyFallback {
    const val BODY_TEXT = "Body Text"
    private const val DESCRIBE_TEXT_LIMIT = 40

    /** A new `BLOCK` with style Body Text holding [text]. */
    fun bodyTextBlock(text: String): Element = BBX.BLOCK.STYLE.create(BODY_TEXT).apply { appendChild(text) }

    /**
     * Text of [node] that a reader would see: descendant text, skipping UTD translation markup.
     */
    fun visibleText(node: Node): String = when (node) {
        is Text -> node.value
        is Element -> if (node.namespaceURI == UTD_NS) "" else
            (0 until node.childCount).joinToString("") { visibleText(node.getChild(it)) }
        else -> ""
    }

    /**
     * Body Text blocks that keep all the text of [elem]: one per outermost descendant `BLOCK`,
     * or a single block for all of its text when it has no blocks. Blank text produces no block.
     */
    fun toBodyTextBlocks(elem: Element): List<Element> {
        val blocks = if (BBX.BLOCK.isA(elem)) listOf(elem) else outermostBlocks(elem)
        val texts = if (blocks.isEmpty()) listOf(visibleText(elem)) else blocks.map { visibleText(it) }
        return texts.map { it.trim() }.filter { it.isNotEmpty() }.map { bodyTextBlock(it) }
    }

    /**
     * Replace [elem] in its parent with Body Text blocks holding its text.
     * @return a description of what was replaced, for the import report
     */
    fun replaceWithBodyText(elem: Element): String {
        val description = describe(elem)
        val parent = elem.parent
        var index = parent.indexOf(elem)
        toBodyTextBlocks(elem).forEach { parent.insertChild(it, ++index) }
        elem.detach()
        return description
    }

    /** Short description of an element for logs and the import report, e.g. `BLOCK[FOO] "some text"`. */
    fun describe(elem: Element): String {
        val type = elem.getAttributeValue("type", BB_NS) ?: elem.getAttributeValue("type")
        val text = visibleText(elem).trim().replace(Regex("\\s+"), " ")
        val shortText = if (text.length > DESCRIBE_TEXT_LIMIT) text.take(DESCRIBE_TEXT_LIMIT) + "…" else text
        return buildString {
            append(elem.localName)
            if (type != null) append("[").append(type).append("]")
            if (shortText.isNotEmpty()) append(" \"").append(shortText).append("\"")
        }
    }

    private fun outermostBlocks(elem: Element): List<Element> = FastXPath.descendant(elem)
        .filterIsInstance<Element>()
        .filter { BBX.BLOCK.isA(it) }
        .filter { block -> generateSequence(block.parent as? Element) { it.parent as? Element }
            .takeWhile { it !== elem }
            .none { BBX.BLOCK.isA(it) } }
        .toList()
}

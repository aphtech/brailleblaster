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
package org.brailleblaster.utd.internal

import nu.xom.*
import org.brailleblaster.utd.config.DocumentUTDConfig
import org.brailleblaster.utd.utils.UTDHelper.Companion.endsWithWhitespace
import org.brailleblaster.utd.utils.UTDHelper.Companion.startsWithWhitespace
import org.brailleblaster.utils.xom.attributes
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class NormaliserFactory : NodeFactory() {
    private var blockElementNames = DEFAULT_BLOCK_ELEMENTS

    /**
     * NOTE: Only normalise in finishMakingDocument to prevent re-normalising
     */
    override fun finishMakingDocument(document: Document) {
        if (DocumentUTDConfig.NIMAS.getSetting(document, "isNormalised") == "true") {
            log.info("Document already normalised, not normalising again")
            return
        }


        // initial normaliseSpace pass, must run before element processing
        for (curText in document.query("//text()").filterIsInstance<Text>()) {
            val value = curText.value
            val newValue = normaliseSpace(value)
            //In all real textbooks indentations is outside the element and contains newlines
            if (newValue.isBlank() && value.any { it in "\r\n" }) {
                curText.detach()
                continue
            }
            if (value != newValue) {
                curText.value = newValue
            }
        }


        // Post process elements
        for (element in document.query("//*").filterIsInstance<Element>()) {
            val childCount = element.childCount
            val whitespaceTexts: MutableSet<Text> = LinkedHashSet()
            var previousChild: Node? = null
            for (i in 0 until childCount) {
                val child = element.getChild(i)
                if (previousChild != null) {
                    if (previousChild is Text && child is Element && previousChild.value.isBlank() && blockElementNames.contains(
                            child.localName
                        )
                    ) {
                        // block element preceded by whitespace
                        whitespaceTexts.add(previousChild)
                    } else if (child is Text && previousChild is Element && child.value.isBlank() && blockElementNames.contains(
                            previousChild.localName
                        )
                    ) {
                        whitespaceTexts.add(child)
                    }
                }
                previousChild = child
            }
            for (text in whitespaceTexts) {
                text.detach()
            }


            // Post process attributes
            for (curAttribute in element.attributes) {
                val value = normaliseSpace(curAttribute.value).trim { it <= ' ' }
                if (value != curAttribute.value) {
                    curAttribute.value = value
                }
            }
        }


        //Post process end and beginning spaces leaving them if there is another sibling element
        for (curText in document.query("//text()").filterIsInstance<Text>()) {
            var value = curText.value
            check(value.isNotEmpty()) { "Completely empty text node: $value" }


            val textParent = curText.parent
            val textIndex = textParent.indexOf(curText)

            val textGrandParent = textParent.parent
            val textParentIndex = textGrandParent.indexOf(textParent)

            // MWhapples: Do we really want to do it this way, this is format specific stuff.
            // REMOVE_ADJACENT_SPACE_ELEMENTS should be done on a per-instance basis
            // so a different list can be loaded for the format being handled.
            //RT3466: Strip extraneous spaces around img tags
            var charsToTrim = startsWithWhitespace(value)
            if (textIndex >= 1) {
                val previousNode = textParent.getChild(textIndex - 1)
                if (previousNode is Element && REMOVE_ADJACENT_SPACE_ELEMENTS.contains(
                        previousNode.localName
                    ) && charsToTrim > 0
                ) {
                    value = value.substring(charsToTrim)
                }
            }
            charsToTrim = endsWithWhitespace(value)
            if (textIndex + 1 < textParent.childCount) {
                val nextNode = textParent.getChild(textIndex + 1)
                if (nextNode is Element && REMOVE_ADJACENT_SPACE_ELEMENTS.contains(
                        nextNode.localName
                    ) && charsToTrim > 0
                ) {
                    value = value.dropLast(charsToTrim)
                }
            }
            if (value.isEmpty()) {
                //It was not blank at the start but now is, above just removed all the spaces
                curText.detach()
            }

            if (value.isBlank()) {
                //Not next to an image, but this is intended as it made it past makeText()
                continue
            }

            //XML formatters will put elements after a text node on a new line
            charsToTrim = startsWithWhitespace(value)
            if (charsToTrim > 0 && value.take(charsToTrim).any { it in "\n\r" }) {
                value = " " + value.drop(charsToTrim)
            }

            charsToTrim = endsWithWhitespace(value)
            if (charsToTrim > 0 && value.substring(value.length - charsToTrim).any { it in "\n\r" }) {
                value = value.dropLast(charsToTrim) + " "
            }

            //Strip trailing spaces if text is at the start or end of the parent
            var stripCharsLength = startsWithWhitespace(value)
            if (stripCharsLength > 0 && textParent.childCount == 1 && textParentIndex == 0) {
                //Only text inside tag, that tag is at the begining of the line
                value = value.substring(stripCharsLength)
            }
            charsToTrim = startsWithWhitespace(value)
            if (charsToTrim > 0 && textParent.childCount == 1 && textParentIndex == 0) {
                //Only text inside tag, that tag is at the begining of the line
                value = value.substring(charsToTrim)
            }
            stripCharsLength = endsWithWhitespace(value)
            if (stripCharsLength > 0 && textParent.childCount == 1 && textParentIndex == textGrandParent.childCount - 1) {
                //Last element with nothing after
                value = value.dropLast(stripCharsLength)
            }
            charsToTrim = endsWithWhitespace(value)
            if (charsToTrim > 0 && textParent.childCount == 1 && textParentIndex == textGrandParent.childCount - 1) {
                //Last element with nothing after
                value = value.dropLast(charsToTrim)
            }

            if (curText.value != value) {
                curText.value = value
            }
        }
        DocumentUTDConfig.NIMAS.setSetting(document, IS_NORMALISED_KEY, "true")
        val result = DocumentUTDConfig.NIMAS.getSetting(document, IS_NORMALISED_KEY)
        check(result == "true") { "Unknown setting $result" }
    }

    private fun normaliseSpace(data: String): String {
        val charData = data.toCharArray()
        val result = StringBuilder()
        for (i in charData.indices) {
            if (charData[i] == '\t' || charData[i] == '\n' || charData[i] == '\r') {
                charData[i] = ' '
            }
            if (i == 0 || charData[i - 1] != ' ' || charData[i] != ' ') {
                result.append(charData[i])
            }
        }
        return result.toString()
    }

    override fun makeDocType(rootElementName: String?, publicID: String?, systemID: String): Nodes {
        // Identify what format the document is so that it can be normalised correctly.
        blockElementNames = if (publicID != null) {
            when (publicID) {
                "-//NISO//DTD dtbook 2005-3//EN" -> DTBOOK_BLOCK_ELEMENTS
                "-//W3C//DTD HTML 4.01//EN", "-//W3C//DTD HTML 4.01 Transitional//EN", "-//W3C//DTD HTML 4.01 Frameset//EN", "-//W3C//DTD XHTML 1.0 Strict//EN", "-//W3C//DTD XHTML 1.0 Transitional//EN", "-//W3C//DTD XHTML 1.0 Frameset//EN", "-//W3C//DTD XHTML 1.1//EN" -> HTML_BLOCK_ELEMENTS
                else -> DEFAULT_BLOCK_ELEMENTS
            }
        } else if (rootElementName != null) {
            when (rootElementName) {
                "dtbook", "DTBOOK" -> DTBOOK_BLOCK_ELEMENTS
                "html", "HTML" -> HTML_BLOCK_ELEMENTS
                else -> DEFAULT_BLOCK_ELEMENTS
            }
        } else {
            DEFAULT_BLOCK_ELEMENTS
        }
        log.info("blockElementNames: $blockElementNames")
        return super.makeDocType(rootElementName, publicID, systemID)
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(NormaliserFactory::class.java)
        const val IS_NORMALISED_KEY: String = "isNormalised"
        private val REMOVE_ADJACENT_SPACE_ELEMENTS: List<String> = listOf("img", "imggroup", "td")
        private val HTML_BLOCK_ELEMENTS: List<String> = listOf(
            "html",
            "body",
            "h1",
            "h2",
            "h3",
            "h4",
            "h5",
            "h6",
            "div",
            "ul",
            "ol",
            "li",
            "dl",
            "dt",
            "dd",
            "p",
            "table",
            "tr",
            "td",
            "th"
        )
        private val DTBOOK_BLOCK_ELEMENTS: List<String> = listOf(
            "dtbook",
            "book",
            "frontmatter",
            "bodymatter",
            "rearmatter",
            "h1",
            "h2",
            "h3",
            "h4",
            "h5",
            "h6",
            "hd",
            "level1",
            "level2",
            "level3",
            "level4",
            "list",
            "li",
            "dl",
            "dt",
            "dd",
            "p",
            "div",
            "table",
            "tr",
            "td",
            "th"
        )
        private val DEFAULT_BLOCK_ELEMENTS: List<String> = listOf()
    }
}

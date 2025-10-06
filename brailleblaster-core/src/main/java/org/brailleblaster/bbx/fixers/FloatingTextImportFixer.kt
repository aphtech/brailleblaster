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
package org.brailleblaster.bbx.fixers

import jakarta.xml.bind.Unmarshaller
import jakarta.xml.bind.annotation.XmlAttribute
import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBX.BlockSubType
import org.brailleblaster.bbx.BBX.SpanSubType
import org.brailleblaster.utd.NamespaceMap
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.matchers.INodeMatcher
import org.slf4j.LoggerFactory

@Suppress("UNUSED")
class FloatingTextImportFixer : AbstractFixer {
    @XmlAttribute
    private val styleName: String?

    @XmlAttribute
    private val blockType: BlockSubType?

    @XmlAttribute
    private val spanType: SpanSubType?

    // Need no-arg constructor for JAXB.
    @Suppress("UNUSED")
    private constructor() {
        styleName = null
        blockType = null
        spanType = null
    }

    @Suppress("UNUSED")
    constructor(styleName: String?, blockType: BlockSubType?, spanType: SpanSubType?) {
        this.styleName = styleName
        this.blockType = blockType
        this.spanType = spanType
    }

    override fun fix(matchedNode: Node) {
        val wrapperBlock: Element = if (spanType != null) {
            spanType.create()
        } else if (blockType != null) {
            blockType.create()
        } else {
            BBX.BLOCK.STYLE.create(styleName)
        }
        matchedNode.parent.insertChild(
            wrapperBlock,
            matchedNode.parent.indexOf(matchedNode)
        )
        log.trace("Starting with cursor {}",
            XMLHandler.toXMLSimple(matchedNode)
        )
        var cursor: Node? = matchedNode
        //cursor can be null when there's no more elements
        while (cursor != null && Matcher.INSTANCE.isMatch(cursor, NamespaceMap())) {
            val nextNode = XMLHandler.nextSiblingNode(cursor)
            cursor.detach()
            wrapperBlock.appendChild(cursor)
            cursor = nextNode
        }
    }

    override fun afterUnmarshal(unmarshaller: Unmarshaller?, parent: Any?) {
        if (spanType != null) {
            if (blockType != null || styleName != null) {
                throw RuntimeException("Cannot specify both spanType and blocktype or styleName")
            }
            return
        }
        require(!(blockType != null && styleName != null)) { "Cannot specify both style and block type" }
        require(!(blockType == null && styleName!!.isEmpty())) { "Missing styleName attribute " }
    }

    class Matcher : INodeMatcher {
        override fun isMatch(node: Node, namespaces: NamespaceMap): Boolean {
            return node is Text || BBX.SPAN.isA(node) || BBX.INLINE.isA(node)
        }

        companion object {
            var INSTANCE = Matcher()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(FloatingTextImportFixer::class.java)
    }
}
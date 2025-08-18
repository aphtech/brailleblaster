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
import jakarta.xml.bind.annotation.XmlElement
import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.utd.NamespaceMap
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.matchers.INodeMatcher
import org.slf4j.LoggerFactory
import java.util.*

@Suppress("UNUSED")
class SplittingImportFixer : AbstractFixer {
    @XmlElement
    private val matcher: INodeMatcher?

    constructor() {
        matcher = null
    }

    constructor(matcher: INodeMatcher?) {
        this.matcher = matcher
    }

    override fun fix(matchedNode: Node) {
        val matcher = Objects.requireNonNull(matcher, "Matcher should not be null")
        //Get highest ancestor we can split out of
        //otherwise a deeply nested element can be matched twice which fails in BookToBBXConverter.fix
//		Element elementToSplit = FastXPath.ancestor(matchedNode)
//				.stream()
//				.reduce(null, (result, curAncestor) -> matcher.isMatch(curAncestor, null) ? curAncestor : result);
//		if (elementToSplit == null) {
//			throw new NodeException("Node's ancestors don't pass matcher " + matcher, matchedNode);
//		}
        log.trace("fix.1 matchedNode: {}", matchedNode.toString())
        val parent = matchedNode.parent as Element
        log.trace("fix.2 matchedNode parent: {}", parent.toString())

        /*
		TODO: Use closest ancestor to properly handle a triple nested lists 
		(innermost was being added to outermost, skipping middle)
		BookToBBX has also allowed re-matching the same element in some cases
		*/
        val elementToSplit = FastXPath.ancestor(matchedNode)
            .firstOrNull { curAncestor: Element -> matcher!!.isMatch(curAncestor, NamespaceMap()) } ?: throw NodeException("Node's ancestors don't pass matcher $matcher", matchedNode)
        log.trace("fix.3 Element to split: {}", elementToSplit.toString())
        NodeTreeSplitter.split(elementToSplit, matchedNode as Element)

        // cleanup empty elements
        FastXPath.ancestorOrSelf(parent)
            .filter { curNode: Node -> curNode.childCount == 0 }
            .forEach { obj: Node -> obj.detach() }
    }

    override fun afterUnmarshal(unmarshaller: Unmarshaller?, parent: Any?) {
        requireNotNull(matcher) { "Missing matcher" }
    }

    companion object {
        private val log = LoggerFactory.getLogger(SplittingImportFixer::class.java)
    }
}
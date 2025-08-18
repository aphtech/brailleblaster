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

import nu.xom.Attribute
import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler2
import org.slf4j.LoggerFactory

object NodeTreeSplitter {
    private val log = LoggerFactory.getLogger(NodeTreeSplitter::class.java)

    /**
     * Splits element at nested node, eg the following structure
     *
     *
     * `<p>before <span>nested <br/>after</span></p> `
     *
     *
     * with root=`<p>` and splitAt=`<br/>` will give
     *
     *
     * `<p>before<span> nested</span></p>
     * <br/>
     * <p><span>after</span></p>
    ` *
     *
     * @see org.brailleblaster.bbx.fixers.NodeTreeSplitter
     *
     * @param root Where the element tree is split at
     * @param splitAt The root of what gets duplicated in the split element
     * @return The after root or null if empty
     */
	@JvmStatic
	fun split(root: Element?, splitAt: Node?): Element? {
        if (root == null) {
            throw NullPointerException("oldRootElement")
        } else if (splitAt == null) {
            throw NullPointerException("oldTrigger")
        } else if (root === splitAt) {
            log.error("root " + XMLHandler2.toXMLSimple(root))
            log.error("splitAt " + XMLHandler2.toXMLSimple(
                splitAt
            )
            )
            throw NodeException("root == splitAt, splitAt: ", splitAt)
        } else if (FastXPath.descendant(root).none { node: Node -> node === splitAt }) {
            root.addAttribute(Attribute("root", "this"))
            throw NodeException("SplitAt isn't under elem w/ root=this attrub", splitAt)
        }
        var splitCursor: Node = splitAt
        var parent = splitAt.parent as Element
        var splitStart: Int
        var newRoot: Element? = null
        while (splitCursor !== root) {
            log.trace(
                "cursor " + XMLHandler2.toXMLSimple(splitCursor)
                        + " parent " + XMLHandler2.toXMLSimple(
                    parent
                )
            )
            // index of next splitCursor sibling
            splitStart = parent.indexOf(splitCursor) + 1
            log.trace(
                "index {} splitStart {} parent.children {}",
                parent.indexOf(splitCursor),
                splitStart,
                parent.childCount
            )
            if (splitStart == -1) {
                throw NodeException("index not found?", splitCursor)
            }
            if (splitStart - 1 < parent.childCount) {
                // recreate parent
                val prevNewRoot = newRoot
                newRoot = XMLHandler2.shallowCopy(parent)
                //			root.getParent().insertChild(newRoot, root.getParent().indexOf(root) + 1);
                log.trace("Making root " + newRoot.toXML())
                if (prevNewRoot != null) {
                    prevNewRoot.detach()
                    newRoot.appendChild(prevNewRoot)
                }

                // append existing and remaining tree
                while (parent.childCount > splitStart) {
                    val next = parent.getChild(splitStart)
                    log.debug("moving into root " + next.toXML())
                    next.detach()
                    newRoot.appendChild(next)
                }

                // cleanup empty elements
                if (newRoot.childCount == 0) {
                    newRoot.detach()
                    newRoot = null
                }
            }


            // go up
            splitCursor = parent
            parent = splitCursor.parent as Element
        }
        if (parent !== root.parent) {
            throw NodeException("Unexpected parent of root", parent)
        }

        // check if newRoot was even nessesary (eg simple splits)
        if (newRoot != null) {
            parent.insertChild(newRoot, parent.indexOf(root) + 1)
        }

        // put splitAt in middle
        splitAt.detach()
        parent.insertChild(splitAt, parent.indexOf(root) + 1)

        // Clean up while trying to preserve existing references
        if (newRoot != null && root.childCount == 0) {
            log.trace("Re-using empty root before element")
            // parent children sublist [rootEmpty, splitAt, newRootMinusSplitAt]

            // restore elements under root
            while (newRoot.childCount != 0) {
                val firstChild = newRoot.getChild(0)
                firstChild.detach()
                root.appendChild(firstChild)
            }
            // make splitAt first again
            splitAt.detach()
            parent.insertChild(splitAt, parent.indexOf(root))
            newRoot.detach()
        } else if (newRoot != null && newRoot.childCount == 0) {
            newRoot.detach()
        } else {
            log.trace("both roots filled")
        }
        if (root.childCount == 0) {
            // essentially preformed a basic unwrap
            root.detach()
        }
        return newRoot
    }
}
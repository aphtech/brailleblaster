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
package org.brailleblaster.search

import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import nu.xom.XPathContext
import org.brailleblaster.bbx.BBX
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.braille.stylers.StyleHandler
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.search.SearchCriteria.StyleFormatting
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.utils.stripUTDRecursive

class ReplaceAll(private val man: Manager, private val click: Click) {
    private var numberReplaceAlls = 0
    var tableDoubles: Int = 0
        private set

    //Ideally I'd like this to be a general case of Replace, or for Replace to be a special case of this.
    fun replaceAll(click: Click): Int {
        numberReplaceAlls = 0
        //First add the nodes with matching text to the array
        var nodeArray: List<Node> = replaceAllText()

        if (click.settings.findHasAttributes() && click.settings.replaceHasAttributes()) {
            numberReplaceAlls = nodeArray.size
        }

        for (node in nodeArray) {
            stripUTDRecursive(node.parent as Element)
        }

        click.settings.replaceStyleFormatting?.let {
            nodeArray = replaceAllStyles(it, nodeArray)
        }

        if (click.settings.replaceHasEmphasis()) {
            nodeArray = replaceAllEmphasis(nodeArray)
        }

        if (click.settings.replaceHasContainer()) {
            //Technically can't replace containers...
            replaceAllContainers(nodeArray)
        }

        if (nodeArray.isEmpty()) {
            return -1
        }

        man.simpleManager.dispatchEvent(ModifyEvent(Sender.SEARCH, nodeArray, true))
        man.waitForFormatting(true)
        return numberReplaceAlls
    }

    private fun replaceAllText(): ArrayList<Node> {
        tableDoubles = 0

        if (!click.settings.replaceHasText() && click.settings.replaceHasAttributes()) {
            click.settings.replaceString = click.settings.findString
        }

        val namespace = man.doc.rootElement.namespaceURI
        val xpathContext = XPathContext("dtb", namespace)

        val replaceAllNodes = man.doc.query("//text()", xpathContext)
        if (replaceAllNodes.size() == 0) {
            return ArrayList()
        }
        val nodeArray = ArrayList<Node>()
        for (i in 0 until replaceAllNodes.size()) {
            var tn = replaceAllNodes[i] as Text
            if (!SearchUtils.checkCorrectAttributes(replaceAllNodes[i], click)) {
                continue
            }
            if ((replaceAllNodes[i].parent as Element).localName == "doctitle") {
                continue
            }
            var nodeValue = tn.value
            var matches = SearchUtils.matchPhrase(nodeValue, click, false)
            var loops = 0
            val initialMatches = matches.indices.size
            numberReplaceAlls += initialMatches
            if (XMLHandler.ancestorVisitor(tn.parent) { node: Node? -> BBX.CONTAINER.TABLE.isA(node) } != null) {
                tableDoubles += initialMatches
                continue
            } else if (SearchUtils.isUneditable(tn)) {
                continue
            }
            var add = false
            while (matches.hasNext()) {
                add = true
                /*
         * loop to replace multiple matches in a single text node
         */
                if (loops >= initialMatches) {
                    break
                }
                val pair = matches.next
                val start = pair.start
                val end = pair.end
                nodeValue = tn.value
                SearchUtils.dealWithReplaceCase(click, replaceAllNodes[i].value)
                val before = nodeValue.take(start)
                val after = nodeValue.drop(end)
                val value = before + click.settings.replaceString + after
                tn.value = value
                loops++
                tn = replaceAllNodes[i] as Text
                nodeValue = tn.value
                matches = SearchUtils.matchPhrase(nodeValue, click, false)
            }
            if (add) {
                nodeArray.add(replaceAllNodes[i])
            }
        }
        numberReplaceAlls -= tableDoubles
        tableDoubles = (tableDoubles / 2)
        return nodeArray
    }

    private fun replaceAllEmphasis(nodeArray: List<Node>): List<Node> {
        val newArray: MutableList<Node> = ArrayList()
        val addEmphasisEnum =
            SearchUtils.makeEnumFromList(click.settings.replaceEmphasisFormatting, false)
        val removeEmphasisEnum =
            SearchUtils.makeEnumFromList(click.settings.replaceEmphasisFormatting, true)

        for (n in nodeArray) {
            var tn = n as Text
            val nodeValue = tn.value
            var matches = SearchUtils.matchPhrase(nodeValue, click, true)

            val element = n.parent
            if (element is Element) {
                if (BBX.INLINE.EMPHASIS.isA(element)) {
                    val existingEnum = BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS[element]

                    existingEnum.removeAll(removeEmphasisEnum)
                    existingEnum.addAll(addEmphasisEnum)

                    //All emphasis removed from the set
                    if (existingEnum.isEmpty()) {
                        //We removed the only emphasis, get rid of the wrapper
                        val pn = element.parent as Element
                        stripUTDRecursive(pn)
                        n.detach()
                        pn.replaceChild(element, n)
                    } else {
                        //Set the modified emphasis
                        BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS[element] = existingEnum
                    }
                    newArray.add(n)
                } else {
                    while (matches.hasNext()) {
                        val pair = matches.next
                        val re = SearchUtils.addEmphasisNoViews(tn, pair.start, pair.end, addEmphasisEnum)
                        for (j in 0..re.index) {
                            newArray.add(re.nodes[j])
                        }
                        if (re.nodes.size > re.index + 1) {
                            tn = re.nodes[re.index + 1]
                            matches = SearchUtils.matchPhrase(tn.value, click, true)
                        } else {
                            break
                        }
                    }
                }
            }
        }
        val parents: MutableList<Node> = ArrayList()
        for (node in newArray) {
            parents.add(node.parent)
        }

        return SearchUtils.cleanEmptyInlineAttributes(parents)
    }

    private fun replaceAllContainers(nodeArray: List<Node>) {
        //Turns out it's not this simple.
        SearchUtils.changeContainer(nodeArray, click.settings.replaceContainerFormatting, man)

        /*
    for (Node value : nodeArray) {
      Node node = value.getParent();
      while (node != null && !BBX.CONTAINER.isA(node)) {
        //Keep going until you find the Container node
        node = node.getParent();
      }
      //StyleHandler.addStyle((Element) node, click.getSettings().replaceStyleFormatting.getStyle(), man);
      //Need some way to change Containers...it's not necessarily the same process as styles.
      //newArray.add(value);
    }
     */
    }

    private fun replaceAllStyles(styleFormatting: StyleFormatting, nodeArray: List<Node>): List<Node> {
        val newArray: MutableList<Node> = ArrayList()
        for (value in nodeArray) {
            var node: Node? = value.parent
            while (node != null && !BBX.BLOCK.isA(node)) {
                node = node.parent
            }
            //If the replaceStyleFormatting is not null, change the style on the nodes
            // the style can't be a negative on Replace operations - "Not Body Text" wouldn't make sense.
            if (node != null) {
                StyleHandler.addStyle(node as Element, styleFormatting.style, man)
                newArray.add(value)
            }
        }
        return newArray
    }
}

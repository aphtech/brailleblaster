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
import org.brailleblaster.bbx.BBX
import org.brailleblaster.math.mathml.MathModuleUtils
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.perspectives.mvc.modules.misc.StylesMenuModule
import org.brailleblaster.search.SearchCriteria.*
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.internal.xml.splitNode
import org.brailleblaster.utd.properties.EmphasisType
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.stripUTDRecursive
import org.brailleblaster.utils.xom.childNodes
import org.brailleblaster.util.Notify
import org.brailleblaster.util.Notify.notify
import java.util.*
import java.util.regex.Pattern
import kotlin.math.min

object SearchUtils {
    //TODO: What is the full list of containers we want?
    val CONTAINERS: List<String> = listOf("Box", "Full Box", "Color Box", "Color Full Box")

    //From Styles List:
    //"Incidental Note with Heading","Incidental Note without Heading", "Footnote", "Alphabetic Division",
    //"Attribution", "Color Box", "Source Citation", "Color Full Box", "Guide Word"};
    //From BBX.java:
    //subTypes = ImmutableList.of(LIST, DONT_SPLIT, TABLE, TABLE_ROW, TABLETN, DOUBLE_SPACE, BOX, IMAGE, PRODNOTE,
    //NOTE, PROSE, STYLE, TPAGE, TPAGE_SECTION, TPAGE_CATEGORY, VOLUME_TOC, VOLUME, FALLBACK, OTHER,
    //CAPTION, BLOCKQUOTE, DEFAULT, NUMBER_LINE, MATRIX, TEMPLATE, SPATIAL_GRID,CONNECTING_CONTAINER);
    @JvmStatic
    fun isContainerStyle(s: String): Boolean {
        for (container in CONTAINERS) {
            if (s == container) {
                return true
            }
        }
        return false
    }

    /**
     * Uses Java Matcher. Put all edge cases here.
     *
     * @param viewString
     * @return
     */
    @JvmStatic
    fun matchPhrase(viewString: String, click: Click, matchReplace: Boolean): MatchObject {
        //System.out.println("SearchUtils.MatchPhrase " + (matchReplace ? " Replacing " : "") + view);
        var view = viewString
        var phrase =
            requireNotNull(if (matchReplace) click.settings.replaceString else click.settings.findString)
        if (!click.settings.isFindCaseSensitive) {
            view = view.lowercase(Locale.getDefault())
            phrase = phrase.lowercase(Locale.getDefault())
        }
        if (phrase.contains('\'')) {
            view = view.replace("\u2019".toRegex(), "'")
            phrase = phrase.replace("'".toRegex(), "'")
        }
        //Had a nice fix to this jumbled code; got lost in my git mixup...
        val whitespace = "\\s+"
        val words = phrase.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        var s: StringBuilder
        if (phrase.contains('(') || phrase.contains(')') || phrase.contains('*')
            || phrase.contains('$') || phrase.contains('.') || phrase.contains('+') || phrase.contains('?')
        ) {
            /*
       * dollar sign, left and right parenthesis, asterisk, period, plus
       * sign, question mark
       */
            s = StringBuilder(Pattern.quote(phrase))
        } else { //No special chars
            s = StringBuilder(words[0])
            for (word in words.drop(1)) {
                s.append(whitespace)
                s.append(word)
            }
            if (phrase.length > 1 && phrase[phrase.length - 1] == ' ') {
                s.append(whitespace)
            }
            if (click.settings.isWholeWord) {
                s = StringBuilder("\\b$s\\b")
            }
        }

        val pattern = Pattern.compile(s.toString())
        val matcher = pattern.matcher(view)
        val matches = MatchObject(view)
        if (matcher.find()) {
            matches.add(StartEndPair(matcher.start(), matcher.end()))
        }
        while (matcher.find()) { //Why a while loop here?
            matches.add(StartEndPair(matcher.start(), matcher.end()))
        }
        return matches
    }

    /**
     * @param node
     * @return This method will check the node to make sure the attributes are a
     * match to the search criteria.
     */
    @JvmStatic
    fun checkCorrectAttributes(node: Node, click: Click): Boolean {
        //System.out.println("SearchUtils.checkCorrectAttributes");
        if (node !is Text) {
            return false
        }
        if (isBraille(node)) {
            return false
        }
        if (XMLHandler.ancestorVisitorElement(node) { n: Element -> n.localName == "head" } != null) {
            return false
        }
        val ele = node.parent as Element
        //System.out.println("Checking UTD attributes: emphasis, styles, containers.");
        return utdEmphasis(ele, click) && utdStyles(node, click) && utdContainer(ele, click)
    }

    fun hasFindCriteria(search: Click): Boolean {
        return search.settings.findHasText() || search.settings.findHasAttributes()
    }

    //Helper for utdActions
    internal fun makeEnumFromList(inputList: List<EmphasisFormatting>, isNegated: Boolean): EnumSet<EmphasisType> {
        val es = EnumSet.noneOf(EmphasisType::class.java)

        for (ef in inputList) {
            if (ef.isNot == isNegated) {
                es.add(ef.emphasis)
            }
        }

        return es
    }

    //Really the Styles and the Containers are both in the "overrideStyle" portion of the XML.
    // Only the Container node has a container name in that field, its children do not.
    //Might not be so hard to adapt the style finder...
    // but I kind of prefer the clear split between the methods here and in the menu.
    internal fun utdContainer(ele: Element, so: Click): Boolean {
        if (so.settings.findHasContainer()) {
            //Containers are really the element's parent; hierarchy is Section->Container->Blocks
            if (!BBX.CONTAINER.isA(ele.parent)) {
                return false
            }

            //System.out.println("Potential match for element " + ele);
            val n = ele.parent as Element
            for (cf in so.settings.findContainerFormatting) {
                if (ele.getAttributeValue("utd-style") != null) {
                    val rs = n.getAttributeValue("utd-style") == cf.name
                    return cf.isNot xor rs
                } else if (n.getAttributeValue("overrideStyle") != null) {
                    val rs = n.getAttributeValue("overrideStyle") == cf.name
                    return cf.isNot xor rs
                }
            }
            //If no definite matches, return false
            return false
        }
        //If no containers are specified, every element is a valid match
        return true
    }

    //Return true if the element emphasis matches, false otherwise
    //Emphasis replacement doesn't actually happen here - it's just a check for the emphasis in the element
    internal fun utdEmphasis(ele: Element?, so: Click): Boolean {
        if (so.settings.findHasEmphasis()) { //Confirm that we have criteria in the emphasis list
            if (!BBX.INLINE.EMPHASIS.isA(ele)) { //Double check that the current element has an inline emphasis
                return false
            }
            val emphasisBits = BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS[ele]
            val findEmphasis = makeEnumFromList(so.settings.findEmphasisFormatting, false)
            val findNegatedEmphasis = makeEnumFromList(so.settings.findEmphasisFormatting, true)

            return emphasisBits.containsAll(findEmphasis) && emphasisBits
                .none { o: EmphasisType -> findNegatedEmphasis.contains(o) }
        }
        //if there are no emphasis criteria to look for, every element is a valid match
        return true
    }

    internal fun utdStyles(node: Node, click: Click): Boolean {
        if (!click.settings.findHasStyle()) {
            return true //If there are no style criteria, every element is a potential match
        }
        try {
            //Grab an element that matches the stylesFormatting
            val parent = XMLHandler.ancestorVisitorElement(
                node
            ) { n -> hasStyles(n, click.settings.findStyleFormatting) }
            if (parent != null) {
                //if the element is not null after checking the criteria, it's probably valid
                return true
            }
        } catch (_: NullPointerException) {
            throw SearchException(
                """Null pointer thrown searching for "${click.settings.findString}" with styles: ${
                    click.settings.printStyleFormatting(
                        click.settings.findStyleFormatting
                    )
                }
On node $node"""
            )
        }
        //Styles either haven't matched or something went wrong
        return false
    }

    internal fun hasStyles(el: Element, sfl: List<StyleFormatting>): Boolean {
        //System.out.println("SearchUtils.hasStyles");
        if (BBX.BLOCK.isA(el)) { //Blocks only, no containers
            for (sf in sfl) {
                val utdStyle = el.getAttributeValue("utd-style")
                if (utdStyle != null) {
                    val rs = utdStyle == sf.style
                    return sf.isNot xor rs
                } else {
                    val overrideStyle = el.getAttributeValue("overrideStyle")
                    if (overrideStyle != null) {
                        val rs = overrideStyle == sf.style
                        return sf.isNot xor rs
                    }
                }
            }
        }
        return false
    }

    internal fun isUneditable(node: Node): Boolean {
        return XMLHandler.ancestorVisitor(node) { n: Node? -> BBX.CONTAINER.TABLE.isA(n) } != null || MathModuleUtils.isMath(
            node
        )
    }

    @JvmStatic
    fun checkUneditable(node: Node): Boolean {
        if (isUneditable(node)) {
            if (isUneditable(node)) {
                notify(
                    """
                        Text is part of an uneditable element.
                        Please use the table editor and/or math editor to edit these instances.
                        """.trimIndent(),
                    Notify.ALERT_SHELL_NAME
                )
                return true
            }
        }
        return false
    }

    @JvmStatic
    fun isBraille(node: Node): Boolean {
        var curNode = node
        val doc = curNode.document
        while (curNode !== doc) {
            if (UTDElements.BRL.isA(curNode)) {
                return true
            }
            curNode = curNode.parent
        }
        return false
    }

    internal fun addEmphasisNoViews(node: Text, start: Int, end: Int, et: EnumSet<EmphasisType>?): EmphasisReturn {
        val nodesWithEmpty =
            node.splitNode(start, end)
        val splitTextNode: MutableList<Text> = ArrayList()
        var index = 0

        for (text in nodesWithEmpty) {
            if (text.value.isNotEmpty()) {
                splitTextNode.add(text)
            } else {
                text.detach()
            }
        }

        if (splitTextNode.size == 1) {
            wrapNewInEmphasis(et, splitTextNode[0])
        } else if (splitTextNode.size == 2) {
            if (start == 0) {
                wrapNewInEmphasis(et, splitTextNode[0])
            } else {
                index = 1
                wrapNewInEmphasis(et, splitTextNode[1])
            }
        } else if (splitTextNode.size == 3) {
            index = 1
            wrapNewInEmphasis(et, splitTextNode[1])
        }

        return EmphasisReturn(splitTextNode, index)
    }

    internal fun wrapNewInEmphasis(et: EnumSet<EmphasisType>?, node: Text): Node {
        val newEmphasisWrapper = BBX.INLINE.EMPHASIS.create(et)
        val parent = node.parent
        val index = parent.indexOf(node)
        node.detach()
        newEmphasisWrapper.appendChild(node)
        parent.insertChild(newEmphasisWrapper, index)
        return node
    }

    //Override current emphasis set in the element with a new one.
    internal fun modifyEmphasis(node: Node, ef: List<EmphasisFormatting>) {
        val addEmphasisEnum = makeEnumFromList(ef, false)
        val removeEmphasisEnum = makeEnumFromList(ef, true)
        val element = node.parent
        if (element is Element) {
            if (BBX.INLINE.EMPHASIS.isA(element)) {
                val existingEnum = BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS[element]
                //System.out.println("Modifying emphasis...");
                existingEnum.removeAll(removeEmphasisEnum)
                existingEnum.addAll(addEmphasisEnum)

                // all unwanted emphasis removed from enum set
                if (existingEnum.isEmpty()) {
                    // we removed the only emphasis, get rid of the wrapper
                    val pn = element.parent as Element
                    stripUTDRecursive(pn)
                    node.detach()
                    pn.replaceChild(element, node)
                } else {
                    // reset the modified emphasis
                    BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS[element] = existingEnum
                }
                //System.out.println("Modification complete.");
                //BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS.set(element, addEmphasisEnum);
            } else {
                wrapNewInEmphasis(addEmphasisEnum, node as Text)
            }
        }
    }

    /**
     * this method will change the replace string if the user wants to match the
     * case of the found word when replacing. there are three cases: all
     * capitalized, all lowercase, or the first character capitalized and the
     * rest lowercase
     */
    internal fun dealWithReplaceCase(click: Click, nodeString: String) {
        val findString = Objects.requireNonNull(click.settings.findString)
        val start = nodeString.lowercase(Locale.getDefault()).indexOf(findString!!.lowercase(Locale.getDefault()))
        val foundString = nodeString.substring(start, start + findString.length)
        val replaceString = Objects.requireNonNull(click.settings.replaceString)
        val newReplaceString = StringBuilder()
        var allcaps = true
        val stop = min(replaceString!!.length.toDouble(), foundString.length.toDouble()).toInt()

        for (findchar in foundString.toCharArray()) {
            if (!Character.isUpperCase(findchar) && Character.isAlphabetic(findchar.code)) {
                allcaps = false
                break
            }
        }

        var i = 0
        var j = 0
        while (i < stop) {
            val findChar = foundString[i]
            val replaceChar = replaceString[j]
            if (allcaps || (i == 0 && Character.isUpperCase(findChar))) {
                val converted = replaceChar.uppercaseChar()
                newReplaceString.append(converted)
            } else {
                val converted = replaceChar.lowercaseChar()
                newReplaceString.append(converted)
            }
            i++
            j++
        }

        val leftoverChars = replaceString.length - foundString.length
        if (leftoverChars > 0) {
            var k = 0
            var m = foundString.length
            while (k < leftoverChars) {
                val leftoverChar = replaceString[m]
                // if every character so far has been capitalized, keep up the trend
                val converted = if (allcaps) {
                    leftoverChar.uppercaseChar()
                } else {
                    leftoverChar.lowercaseChar()
                }
                newReplaceString.append(converted)
                k++
                m++
            }
        }
        click.settings.replaceString = (newReplaceString.toString())
    }

    /**
     * @param modifiedNodes
     * @return clean-up method to remove any empty <INLINE bb:emphasis = "">
    </INLINE> */
    internal fun cleanEmptyInlineAttributes(modifiedNodes: List<Node>): List<Node> {
        val newNodes: MutableList<Node> = ArrayList()
        val toDelete: MutableList<Node> = ArrayList()
        modifiedNodes.forEach { e: Node ->
            e.childNodes.forEach { child: Node ->
                if (BBX.INLINE.EMPHASIS.isA(e) && BBX.INLINE.EMPHASIS.ATTRIB_EMPHASIS[e as Element].isEmpty()) {
                    val childText = Text(child.value)
                    val parent = e.parent
                    parent.insertChild(childText, parent.indexOf(e))
                    newNodes.add(childText)
                    toDelete.add(e)
                } else {
                    newNodes.add(e)
                }
            }
        }
        for (node in toDelete) {
            node.detach()
        }
        return newNodes
    }

    @JvmStatic
    fun inTable(m: Manager): Boolean {
        return m.mapList.current.isReadOnly
    }

    internal fun removeContainer(list: List<Node>, replaceRemoveContainerString: String, m: Manager) {
        when (replaceRemoveContainerString) {
            "Box", "Full Box", "Color Box", "Color Full Box" -> {
                val box = XMLHandler.ancestorVisitor(list[0]) { node: Node? -> BBX.CONTAINER.BOX.isA(node) }
                if (box != null) {
                    m.simpleManager.getModule(StylesMenuModule::class.java)!!.applyStyle(
                        m.document.engine.styleDefinitions.getStyleByName(replaceRemoveContainerString)!!,
                        box, box
                    )
                }
            }

            "Poetic Stanza" -> { //This style is hidden in the other menus. Keep this code just in case.
                val stanza = XMLHandler.ancestorVisitor(
                    list[0]
                ) { n: Node? -> n is Element && n.getAttributeValue("utd-style") != null && n.getAttributeValue("utd-style") == "Poetic Stanza" }
                if (stanza != null) {
                    XMLHandler.unwrapElement(stanza as Element)
                }
            }

            "List Tag" -> {
                val lnode = XMLHandler.ancestorVisitor(list[0]) { node: Node? -> BBX.CONTAINER.LIST.isA(node) }
                if (lnode != null) {
                    XMLHandler.unwrapElement(lnode as Element)
                }
            }

            else -> println("Unknown container type $replaceRemoveContainerString was not removed")
        }
    }

    internal fun addContainer(list: List<Node>, replaceAddContainerString: String, m: Manager) {
        when (replaceAddContainerString) {
            "Box", "Full Box", "Color Box", "Color Full Box" -> {
                val box = XMLHandler.ancestorVisitor(list[0]) { node: Node? -> BBX.CONTAINER.BOX.isA(node) }
                if (box == null) {
                    m.simpleManager.getModule(StylesMenuModule::class.java)!!.applyStyle(
                        m.document.engine.styleDefinitions.getStyleByName(replaceAddContainerString)!!,
                        list[0], list[list.size - 1]
                    )
                }
            }

            "Poetic Stanza" -> { //This style is hidden in the other menus. Keep this code just in case.
                val stanza = XMLHandler.ancestorVisitor(
                    list[0]
                ) { n: Node? -> (n is Element) && n.getAttributeValue("utd-style") != null && n.getAttributeValue("utd-style") == "Poetic Stanza" }
                if (stanza == null) {
                    m.simpleManager.getModule(StylesMenuModule::class.java)!!.applyStyle(
                        m.document.engine.styleDefinitions.getStyleByName(replaceAddContainerString)!!,
                        list[0], list[list.size - 1]
                    )
                }
            }

            "List Tag" -> {
                val lnode = XMLHandler.ancestorVisitor(list[0]) { node: Node? -> BBX.CONTAINER.LIST.isA(node) }
                if (lnode == null) {
                    m.simpleManager.getModule(StylesMenuModule::class.java)!!.applyStyle(
                        m.document.engine.styleDefinitions.getStyleByName(replaceAddContainerString)!!,
                        list[0], list[list.size - 1]
                    )
                }
            }

            else -> println("Unknown container type $replaceAddContainerString was not added")
        }
    }

    internal fun changeContainer(list: List<Node>, cl: List<ContainerFormatting>, m: Manager) {
        //Turns out it's not this simple.

        for (cf in cl) {
            if (cf.isNot) {
                removeContainer(list, cf.name, m)
            } else {
                addContainer(list, cf.name, m)
            }
        }
    }

    class EmphasisReturn(val nodes: List<Text>, val index: Int)

    class MatchObject internal constructor(val view: String) {
        val indices: ArrayList<StartEndPair> = ArrayList()
        var currentIndex: Int = 0

        val isFirstHalf: Boolean
            get() {
                val index = indices[currentIndex].start
                val viewSize = view.length
                return index < viewSize / 2
            }

        fun hasNext(): Boolean {
            return currentIndex < indices.size
        }

        fun add(indexMatch: StartEndPair) {
            indices.add(indexMatch)
        }

        val next: StartEndPair
            get() {
                currentIndex++
                return indices[currentIndex - 1]
            }
    }

    class StartEndPair internal constructor(@JvmField val start: Int, @JvmField val end: Int)
}

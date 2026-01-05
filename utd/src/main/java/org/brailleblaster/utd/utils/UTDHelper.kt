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
package org.brailleblaster.utd.utils

import jakarta.xml.bind.JAXBElement
import nu.xom.*
import org.apache.commons.lang3.builder.ReflectionToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import org.brailleblaster.utd.Style
import org.brailleblaster.utd.exceptions.UTDException
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utils.xml.UTD_NS
import java.lang.reflect.Field
import java.util.function.Consumer

object UTDHelper {
    fun hasBaseStyle(style: Style?, baseStyle: String): Boolean {
        return generateSequence(style) { it.baseStyle }.any { it.name == baseStyle }
    }

    const val BRAILLE_SPACE: Char = '\u2800'

    /**
     * Get the brl elements representing the content. The brl elements may not
     * necessarily be directly associated to the node passed in.
     *
     * @Param node The node to find the brl elements for. This parameter should
     * not be null.
     * @Return the related brl elements representing the content. If there is no
     * brl elements related then an empty Nodes collection will be
     * returned.
     * @Throw NullPointerException when passing in null for the node parameter.
     */
    fun getBrlElements(node: Node?): Nodes {
        if (node == null) {
            throw NullPointerException()
        }

        val returnNode = Nodes()
        val associatedBrl = getAssociatedBrlElement(node)
        if (associatedBrl == null) {
            getDescendantBrlFast(node) { brl: Element ->
                val typeAttrib = brl.getAttributeValue("type")
                if (typeAttrib != null && (typeAttrib == "brlonly" || typeAttrib == "formatting")) return@getDescendantBrlFast
                returnNode.append(brl)
            }
        } else {
            returnNode.append(associatedBrl)
        }
        return returnNode
    }

    /**
     * Get the directly associated brl element for the given node.
     *
     * @Param node The node to find the directly associated brl element for.
     * This parameter should not be null.
     * @Return The directly associated brl element. If there is no associated
     * brl element then null will be returned.
     * @Throw NullPointerException When null is passed in for the node
     * parameter.
     */
    @JvmStatic
    fun getAssociatedBrlElement(node: Node?): Element? {
        if (node == null) {
            throw NullPointerException()
        }
        val parent = node.parent ?: return null

        val index = parent.indexOf(node)
        return getAssociatedBrlElement(parent, index)
    }

    /**
     * Get the directly associated brl element for the given child node.
     *
     * @param parent The parent element of the child node under query.
     * @param idx The index of the child node under query.
     * @return The directly associated brl element. If there is no associated
     * brl element then null will be returned.
     */
    @JvmStatic
    fun getAssociatedBrlElement(parent: ParentNode?, idx: Int): Element? {
        if (parent == null) {
            throw NullPointerException("The parent cannot be null")
        }
        val index = idx + 1
        if (index < parent.childCount) {
            val returnElement = parent.getChild(index)
            if (UTDElements.BRL.isA(returnElement)) {
                val typeAttr = (returnElement as Element).getAttributeValue("type")
                if ("brlonly" != typeAttr && "formatting" != typeAttr) {
                    return returnElement
                }
            }
        }

        return null
    }

    /**
     * Get the original node associated with the specified brl element.
     *
     * @Param brlElement The brl element which to find the original associated
     * node. This should not be null.
     * @Return The original node associated to the brl element. If the brl
     * element is a Braille only element then null will be returned.
     * @Throw NullPointerException if the brlElement parameter is null.
     * @Throw IllegalArgumentException when an element which is not a brl
     * element is used for brlElement parameter.
     * @Throw UTDException when the valid brl element does not have a previous
     * sibling.
     */
    @JvmStatic
    fun getAssociatedNode(brlElement: Element?): Node? {
        if (brlElement == null) {
            throw NullPointerException()
        }

        require((UTDElements.BRL.isA(brlElement))) { "This is not a brl element." }

        if ("formatting" == brlElement.getAttributeValue("type")) {
            return null
        }

        if ("true" == brlElement.getAttributeValue("brlonly")) {
            return null
        }
        val parent = brlElement.parent ?: return null

        val index = parent.indexOf(brlElement) - 1
        if (index < 0) {
            throw UTDException("Invalid UTD. " + brlElement.toXML())
        }
        return parent.getChild(index)
    }

    fun getDescendantBrlFastNodes(root: Node?): Nodes {
        val results = Nodes()
        getDescendantBrlFast(root) { node: Element? -> results.append(node) }
        return results
    }

    @JvmStatic
    fun getDescendantBrlFast(root: Node?): List<Element> {
        val results: MutableList<Element> = ArrayList()
        getDescendantBrlFast(root) { e: Element -> results.add(e) }
        return results
    }

    @JvmStatic
    fun getDescendantBrlFastFirst(root: Node?): Element? {
        val result = getDescendantBrlFast(root)
        if (result.isEmpty()) return null
        return result[0]
    }

    /**
     * Fast version of XPath descendant::utd:brl with callback
     * @param onBrl
     */
    @JvmStatic
    fun getDescendantBrlFast(root: Node?, onBrl: Consumer<Element>) {
        if (root !is ParentNode) {
            return
        }

        if (UTDElements.BRL.isA(root)) {
            onBrl.accept(root as Element)
            //Do not descend into BRL nodes
            return
        }
        val realRoot =
            XMLHandler.parentToElement(root)
        for (childNode in realRoot.childElements) {
            if (UTDElements.BRL.isA(childNode)) {
                onBrl.accept(childNode)
            } else  //Is a non-brl element but might have brl children
                getDescendantBrlFast(childNode, onBrl)
        }
    }

    fun getTableCopies(root: Node, onTable: Consumer<Element?>) {
        if (root !is ParentNode) {
            return
        }

        val realRoot =
            XMLHandler.parentToElement(root)
        for (childNode in realRoot.childElements) {
            if (childNode.getAttribute("class") != null && childNode.getAttributeValue("class")
                    .contains("utd:table")
            ) onTable.accept(childNode)
            else getTableCopies(childNode, onTable)
        }
    }

    /**
     * Checks given node for any brl nodes. Returns true on first brl node encountered and is thus
     * faster than using getDescendantBrl
     * @param root
     * @return
     */
    @JvmStatic
    fun containsBrl(root: Node): Boolean {
        if (root !is ParentNode) {
            return false
        }

        if (UTDElements.BRL.isA(root)) return true
        val realRoot =
            XMLHandler.parentToElement(root)
        for (childNode in realRoot.childElements) {
            if (UTDElements.BRL.isA(childNode)) {
                return true
            } else if (containsBrl(childNode)) {
                return true
            }
        }
        return false
    }

    /**
     * Extended Character.isWhitespace by also checking if its a braille space
     */
    fun isWhitespace(input: Char): Boolean {
        return Character.isWhitespace(input) || input == BRAILLE_SPACE
    }

    /**
     * Returns number of whitespace characters at end of string
     */
    @JvmStatic
    fun endsWithWhitespace(str: String?): Int {
        if (str == null) {
            throw NullPointerException("str")
        }
        if (str.isEmpty()) {
            return 0
        }
        var counter = 0
        for (i in str.length - 1 downTo 0) {
            if (isWhitespace(str[i])) {
                counter++
            } else {
                break
            }
        }
        return counter
    }

    /**
     * Returns number of whitespace characters at beginning of string
     */
    @JvmStatic
    fun startsWithWhitespace(str: String?): Int {
        if (str == null) {
            throw NullPointerException("str")
        }
        if (str.isEmpty()) {
            return 0
        }
        return str.takeWhile { isWhitespace(it) }.length
    }

    /**
     * Get XML of given node minus any brl elements
     * @param node
     * @return
     */
    fun toXMLnoBRL(node: Element): String {
        val nodeCopy = node.copy()
        getDescendantBrlFast(nodeCopy) { obj: Element -> obj.detach() }
        return nodeCopy.toXML()
    }

    /**
     * Get XML of given node minus all utd elements
     * @param node
     * @return
     */
    fun toXMLnoUTD(node: Element): String {
        val nodeCopy = node.copy()
        stripUTDRecursive(nodeCopy)
        return nodeCopy.toXML()
    }

    /**
     * Recursively remove all brl nodes and internal UTD attributes (utdAction, utdStyle)
     * @param rootRaw
     */
    @JvmStatic
    fun stripUTDRecursive(rootRaw: Document) = stripUTDRecursive(rootRaw.rootElement)

    /**
     * Recursively remove all brl nodes and internal UTD attributes (utdAction, utdStyle)
     * @param rootRaw
     */
    @JvmStatic
    fun stripUTDRecursive(rootRaw: Element) {
        if (UTDElements.BRL.isA(rootRaw) || (UTD_NS == rootRaw.namespaceURI && "tablebrl" == rootRaw.localName)) {
            rootRaw.detach()
            return
        }

        var attrib = rootRaw.getAttribute(UTDElements.UTD_ACTION_ATTRIB)
        if (attrib != null) rootRaw.removeAttribute(attrib)

        attrib = rootRaw.getAttribute(UTDElements.UTD_STYLE_ATTRIB)
        if (attrib != null) rootRaw.removeAttribute(attrib)

        for (curChild in rootRaw.childElements) {
            stripUTDRecursive(curChild)
        }
    }

    @JvmStatic
    fun stripBRLOnly(element: Element) {
        if (UTDElements.BRLONLY.isA(element) && (element.getAttribute("type") != null && element.getAttributeValue("type") == "guideDots")) {
            element.detach()
            return
        }
        if (UTDElements.MOVE_TO.isA(element)) {
            element.detach()
            return
        }

        for (child in element.childElements) {
            stripBRLOnly(child)
        }
    }

    @JvmStatic
    fun getTextChild(brlElement: Element?): Text {
        val text = Text("")
        if (brlElement != null) {
            for (i in 0 until brlElement.childCount) {
                if (brlElement.getChild(i) is Text) {
                    return brlElement.getChild(i) as Text
                }
            }
        }

        return text
    }

    @JvmStatic
    fun getFirstTextDescendant(brlElement: Element): Text {
        val text = Text("")
        for (i in 0 until brlElement.childCount) {
            if (brlElement.getChild(i) is Text) {
                return brlElement.getChild(i) as Text
            } else if (brlElement.getChild(i) is Element) {
                return getFirstTextDescendant(brlElement.getChild(i) as Element)
            }
        }

        return text
    }

    @JvmStatic
    fun getDocumentHead(document: Document?): Element? {
        if (document == null) return null
        val rootNode: Node = document.rootElement
        return findHead(rootNode)
    }

    private fun findHead(element: Node): Element? {
        for (i in 0 until element.childCount) {
            if (element.getChild(i) is Element) {
                val child = element.getChild(i) as Element

                if (child.localName == "head") {
                    return child
                } else if (child.localName == "book") {
                    return null
                }
                return findHead(child)
            }
        }

        return null
    }

    @JvmStatic
    @JvmOverloads
    fun autoToString(`object`: Any?, style: ToStringStyle? = null): String {
        return object : ReflectionToStringBuilder(`object`, style) {
            @Throws(IllegalArgumentException::class, IllegalAccessException::class)
            override fun getValue(field: Field): Any? {
                val value = field[getObject()]
                return if (value == null) {
                    null
                } else if (Node::class.java.isAssignableFrom(field.type)) {
                    XMLHandler.toXMLSimple(value as Node)
                } else if (JAXBElement::class.java.isAssignableFrom(field.type)) {
                    (value as JAXBElement<*>).value
                } else value
            }
        }.toString()
    }

    fun findCurrentVolumeNumber(currNode: Node?): Int {
        if (currNode == null) {
            return 0
        }


//		Nodes volumes = currNode.query("preceding::node()[@utd-style='Volume End']");
        val volumes = FastXPath.preceding(currNode)
            .filterIsInstance<Element>()
            .filter { curNode ->
                curNode.getAttribute("utd-style") != null && curNode.getAttributeValue(
                    "utd-style"
                ) == "Volume End"
            }
        if (volumes.isNotEmpty()) {
            val volumeBefore = volumes[0]
            val endOfVolumeText = getFirstTextDescendant(volumeBefore).value
            //Correction : It may say "End of Preliminary Volume []", so look for the last word
            val split = endOfVolumeText.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return split[split.size - 1].toInt() + 1
        }

        return 0
    }
}

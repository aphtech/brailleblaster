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
@file:JvmName("UTDHelper")

package org.brailleblaster.utd.utils

import jakarta.xml.bind.JAXBElement
import nu.xom.*
import org.apache.commons.lang3.builder.ReflectionToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import org.brailleblaster.utd.exceptions.UTDException
import org.brailleblaster.utd.internal.xml.FastXPath
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utils.xml.UTD_NS
import org.brailleblaster.utils.xom.childNodes
import java.lang.reflect.Field
import java.util.function.Consumer

const val BRAILLE_SPACE: Char = '\u2800'

/**
 * Get the brl elements representing the content. The brl elements may not
 * necessarily be directly associated to the node passed in.
 *
 * @Param this@getBrlElements The node to find the brl elements for. This parameter should
 * not be null.
 * @Return the related brl elements representing the content. If there is no
 * brl elements related then an empty Nodes collection will be
 * returned.
 * @Throw NullPointerException when passing in null for the node parameter.
 */
fun Node.getBrlElements(): Nodes {
    val returnNode = Nodes()
    val associatedBrl = this.getAssociatedBrlElement()
    if (associatedBrl == null) {
        this.getDescendantBrlFast { brl: Element ->
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
 * @Param this@getAssociatedBrlElement The node to find the directly associated brl element for.
 * This parameter should not be null.
 * @Return The directly associated brl element. If there is no associated
 * brl element then null will be returned.
 * @Throw NullPointerException When null is passed in for the node
 * parameter.
 */
fun Node.getAssociatedBrlElement(): Element? {
    val parent = this.parent ?: return null

    val index = parent.indexOf(this)
    return parent.getAssociatedBrlElement(index)
}

/**
 * Get the directly associated brl element for the given child node.
 *
 * @param this@getAssociatedBrlElement The parent element of the child node under query.
 * @param idx The index of the child node under query.
 * @return The directly associated brl element. If there is no associated
 * brl element then null will be returned.
 */
fun ParentNode.getAssociatedBrlElement(idx: Int): Element? {
    val index = idx + 1
    if (index < this.childCount) {
        val returnElement = getChild(index)
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
 * @Param this@getAssociatedNode The brl element which to find the original associated
 * node. This should not be null.
 * @Return The original node associated to the brl element. If the brl
 * element is a Braille only element then null will be returned.
 * @Throw NullPointerException if the brlElement parameter is null.
 * @Throw IllegalArgumentException when an element which is not a brl
 * element is used for brlElement parameter.
 * @Throw UTDException when the valid brl element does not have a previous
 * sibling.
 */
fun Element.getAssociatedNode(): Node? {
    require((UTDElements.BRL.isA(this))) { "This is not a brl element." }

    if ("formatting" == getAttributeValue("type")) {
        return null
    }

    if ("true" == getAttributeValue("brlonly")) {
        return null
    }
    val parent = this.parent ?: return null

    val index = parent.indexOf(this) - 1
    if (index < 0) {
        throw UTDException("Invalid UTD. " + toXML())
    }
    return parent.getChild(index)
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
 * @param this@containsBrl
 * @return
 */
fun Node.containsBrl(): Boolean {
    if (this !is ParentNode) {
        return false
    }

    if (UTDElements.BRL.isA(this)) return true
    val realRoot =
        XMLHandler.parentToElement(this)
    return realRoot.childElements.any { childNode ->
        UTDElements.BRL.isA(childNode) || childNode.containsBrl()
    }
}

/**
 * Extended Character.isWhitespace by also checking if its a braille space
 */
fun isUtdWhitespace(input: Char): Boolean {
    return Character.isWhitespace(input) || input == BRAILLE_SPACE
}

/**
 * Returns number of whitespace characters at end of string
 */
fun endsWithWhitespace(str: String): Int = str.takeLastWhile { isUtdWhitespace(it) }.length

/**
 * Returns number of whitespace characters at beginning of string
 */
fun startsWithWhitespace(str: String): Int = str.takeWhile { isUtdWhitespace(it) }.length

/**
 * Recursively remove all brl nodes and internal UTD attributes (utdAction, utdStyle)
 * @param this@stripUTDRecursive
 */
fun Document.stripUTDRecursive() = this.rootElement.stripUTDRecursive()

/**
 * Recursively remove all brl nodes and internal UTD attributes (utdAction, utdStyle)
 * @param this@stripUTDRecursive
 */
fun Element.stripUTDRecursive() {
    if (UTDElements.BRL.isA(this) || (UTD_NS == this.namespaceURI && "tablebrl" == this.localName)) {
        detach()
        return
    }

    var attrib = getAttribute(UTDElements.UTD_ACTION_ATTRIB)
    if (attrib != null) removeAttribute(attrib)

    attrib = getAttribute(UTDElements.UTD_STYLE_ATTRIB)
    if (attrib != null) removeAttribute(attrib)

    for (curChild in this.childElements) {
        curChild.stripUTDRecursive()
    }
}


fun Element.stripBRLOnly() {
    if (UTDElements.BRLONLY.isA(this) && (getAttribute("type") != null && getAttributeValue("type") == "guideDots")) {
        detach()
        return
    }
    if (UTDElements.MOVE_TO.isA(this)) {
        detach()
        return
    }

    for (child in this.childElements) {
        child.stripBRLOnly()
    }
}


fun getTextChild(brlElement: Element?): Text {
    return brlElement?.childNodes?.filterIsInstance<Text>()?.firstOrNull() ?: Text("")
}


fun getFirstTextDescendant(brlElement: Element): Text {
    return brlElement.childNodes.map { if (it is Element) getFirstTextDescendant(it) else it }.filterIsInstance<Text>().firstOrNull() ?: Text("")
}

fun Document?.getDocumentHead(): Element? = this?.rootElement?.findHead()

private fun Node.findHead(): Element? = childNodes.filterIsInstance<Element>().map { when(it.localName) {
    "head" -> it
    "book" -> null
    else -> it.findHead()
} }.firstOrNull()


@JvmOverloads
fun autoToString(obj: Any?, style: ToStringStyle? = null): String {
    return object : ReflectionToStringBuilder(obj, style) {
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

/**
 * Fast version of XPath descendant::utd:brl with callback
 * @param onBrl
 */
fun Node.getDescendantBrlFast(onBrl: Consumer<Element>) {
    if (this !is ParentNode) {
        return
    }

    if (UTDElements.BRL.isA(this)) {
        onBrl.accept(this as Element)
        //Do not descend into BRL nodes
        return
    }
    val realRoot =
        XMLHandler.parentToElement(this)
    for (childNode in realRoot.childElements) {
        if (UTDElements.BRL.isA(childNode)) {
            onBrl.accept(childNode)
        } else  //Is a non-brl element but might have brl children
            childNode.getDescendantBrlFast(onBrl)
    }
}

fun Node?.getDescendantBrlFast(): List<Element> = buildList {
    this@getDescendantBrlFast?.getDescendantBrlFast { add(it) }
}
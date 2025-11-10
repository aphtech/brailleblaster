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
package org.brailleblaster.utd.internal.xml

import nu.xom.*
import org.apache.commons.io.input.BOMInputStream
import org.apache.commons.lang3.StringUtils
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.exceptions.UTDException
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utd.utils.LocalEntityResolver
import org.brailleblaster.utd.utils.LocalEntityResolver.Companion.createXomBuilder
import org.brailleblaster.utils.xml.UTD_NS
import org.brailleblaster.utils.xom.childNodes
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.helpers.MessageFormatter
import org.xml.sax.SAXException
import java.io.*
import java.lang.reflect.Field
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import java.util.function.Predicate
import javax.xml.parsers.ParserConfigurationException

/**
 * Handles processing of XML documents from and to the disk
 */
open class XMLHandler {
    fun load(xmlPathInput: Path): Document {
        var baseUri = xmlPathInput.toUri().toString()
        // Due to an issue in XOM where the jar: protocol will fail XOM URI verification,
        // we alter the protocol to our custom one which will pass.
        // We will then need to handle it in the entity resolver.
        if (baseUri.startsWith("jar:")) {
            // Must re-encode for a valid url
            // Otherwise files with eg [ or ] will throw an MalformedURIException: Path contains invalid
            // character
            baseUri =
                (LocalEntityResolver.ENCODED_URI_PREFIX
                        + URLEncoder.encode(baseUri, StandardCharsets.UTF_8))
        }
        return load(xmlPathInput, baseUri)
    }

    fun load(xmlPathInput: Path, baseUri: String?): Document {
        try {
            val builder = createXomBuilder(false)
            return builder.build(
                BOMInputStream.builder().setInputStream(Files.newInputStream(xmlPathInput)).get(),
                baseUri
            )
        } catch (e: ValidityException) {
            throw RuntimeException("The document failed to validate in $xmlPathInput", e)
        } catch (e: ParsingException) {
            throw RuntimeException("The document XML is malformed in $xmlPathInput", e)
        } catch (e: IOException) {
            throw RuntimeException("Problem reading file $xmlPathInput", e)
        } catch (e: SAXException) {
            throw RuntimeException(
                "Problem creating the XML parser, may be Apache Xerces is not installed", e
            )
        } catch (e: ParserConfigurationException) {
            throw RuntimeException(
                "Problem creating the XML parser, may be Apache Xerces is not installed", e
            )
        }
    }

    fun load(xmlFileInput: File): Document {
        val xmlFile = xmlFileInput.getAbsoluteFile()
        if (!xmlFile.exists()) throw RuntimeException("XML File $xmlFile does not exist")
        try {
            val builder = createXomBuilder(false)
            return builder.build(xmlFile)
        } catch (e: ValidityException) {
            throw RuntimeException("The document failed to validate in $xmlFileInput", e)
        } catch (e: ParsingException) {
            throw RuntimeException("The document XML is malformed in $xmlFileInput", e)
        } catch (e: IOException) {
            throw RuntimeException("Problem reading file $xmlFileInput", e)
        } catch (e: SAXException) {
            throw RuntimeException(
                "Problem creating the XML parser, may be Apache Xerces is not installed", e
            )
        } catch (e: ParserConfigurationException) {
            throw RuntimeException(
                "Problem creating the XML parser, may be Apache Xerces is not installed", e
            )
        }
    }

    fun load(input: InputStream?): Document? {
        try {
            val builder = createXomBuilder(false)
            return builder.build(input)
        } catch (e: ValidityException) {
            throw RuntimeException("The document failed to validate", e)
        } catch (e: ParsingException) {
            throw RuntimeException("The document XML is malformed", e)
        } catch (e: IOException) {
            throw RuntimeException("Problem reading file", e)
        } catch (e: SAXException) {
            throw RuntimeException(
                "Problem creating the XML parser, may be Apache Xerces is not installed", e
            )
        } catch (e: ParserConfigurationException) {
            throw RuntimeException(
                "Problem creating the XML parser, may be Apache Xerces is not installed", e
            )
        }
    }

    fun load(input: Reader?): Document {
        try {
            val builder = createXomBuilder(false)
            return builder.build(input)
        } catch (e: ValidityException) {
            throw RuntimeException("The document failed to validate", e)
        } catch (e: ParsingException) {
            throw RuntimeException("The document XML is malformed", e)
        } catch (e: IOException) {
            throw RuntimeException("Problem reading file", e)
        } catch (e: SAXException) {
            throw RuntimeException(
                "Problem creating the XML parser, may be Apache Xerces is not installed", e
            )
        } catch (e: ParserConfigurationException) {
            throw RuntimeException(
                "Problem creating the XML parser, may be Apache Xerces is not installed", e
            )
        }
    }

    fun save(doc: Document, xmlFileOutput: File) {
        val xmlFile = xmlFileOutput.getAbsoluteFile()
        val overwriting = xmlFile.exists()
        try {
            FileOutputStream(xmlFileOutput).use { output ->
                save(doc, output)
                log.debug(
                    "Wrote UTD output to {}, overwriting {}", xmlFileOutput.getCanonicalPath(), overwriting
                )
            }
        } catch (e: IOException) {
            throw RuntimeException("Failed to save file to " + xmlFileOutput.absolutePath, e)
        }
    }

    @Throws(IOException::class)
    fun save(doc: Document, output: OutputStream) {
        val serializer = newSerializer(output)
        serializer.write(doc)
    }

    @Throws(IOException::class)
    protected open fun newSerializer(output: OutputStream): Serializer {
        return Serializer(output, "UTF-8")
    }

    /**
     * Provides a preformatted xml file for debugging
     */
    class Formatted : XMLHandler() {
        @Throws(IOException::class)
        override fun newSerializer(output: OutputStream): Serializer {
            val serializer = super.newSerializer(output)
            serializer.indent = 2
            return serializer
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(XMLHandler::class.java)
        private val FIELD_XPATHCONTEXT_NAMESPACES: Field

        init {
            try {
                // Due to ridiculous hiding in XOMs design, need to abuse reflection
                FIELD_XPATHCONTEXT_NAMESPACES = XPathContext::class.java.getDeclaredField("namespaces")
                FIELD_XPATHCONTEXT_NAMESPACES.setAccessible(true)
            } catch (ex: Exception) {
                throw RuntimeException("Failed to init fields", ex)
            }
        }

        fun findFirstText(someElement: Element): Text {
            return query(someElement, "descendant::text()[not(ancestor::utd:brl)]").filterIsInstance<Text>()
                .firstOrNull { it.value.isNotBlank() } ?: throw RuntimeException("Couldn't find text in " + someElement.toXML())
        }

        @JvmStatic
        fun queryElements(node: Node, xpathPattern: String, vararg xpathArgs: Any?): List<Element> {
            return query(node, xpathPattern, *xpathArgs).filterIsInstance<Element>()
        }

        fun wrapNodeWithElement(nodeToWrap: Node, wrapper: Element) {
            nodeToWrap.parent.replaceChild(nodeToWrap, wrapper)
            wrapper.appendChild(nodeToWrap)
        }

        fun unwrapElement(elem: Element) {
            if (elem.getChildCount() > 0) {
                // Reverse insert as insertChild will move over subsequent children
                val elemParent = elem.parent
                val parentIndex = elemParent.indexOf(elem)
                while (elem.getChildCount() != 0) {
                    val curChildNode = elem.getChild(elem.getChildCount() - 1)
                    curChildNode.detach()
                    elemParent.insertChild(curChildNode, parentIndex)
                }
            }
            elem.detach()
        }

        /**
         * Avoids potentially expensive deep copies
         */
        fun shallowCopy(elem: Element): Element {
            try {
                val method = Element::class.java.getDeclaredMethod("copyTag", Element::class.java)
                method.setAccessible(true)
                return method.invoke(null, elem) as Element
            } catch (e: Exception) {
                throw UTDException("Failed to invoke writeStartTag method", e)
            }
        }

        /**
         * Utility to query using [.toXPathContext] and with slf4j string arguments
         */
        @JvmStatic
        fun query(node: Node, xpathPattern: String, vararg xpathArgs: Any?): Nodes {
            val realXpath = MessageFormatter.arrayFormat(xpathPattern, xpathArgs).message
            log.trace(
                "Executing XPath {} on {}",
                realXpath,
                when (node) {
                    is Document -> "Document"
                    is Element -> toXMLStartTag(node)
                    else -> node.toString()
                }
            )

            // Handle nodes potentially without a suitable parent to get namespaces from (ie detached text node)
            // Using namespaces in that case is broken anyway
            val nodeAsParent = node as? ParentNode ?: node.parent
            val context: XPathContext? = if (nodeAsParent != null) toXPathContext(nodeAsParent) else null

            return node.query(realXpath, context)
        }

        /**
         * Get start tag without children or end tab by calling private method
         * Element.writeStartTag(Element element, StringBuilder result)
         */
        fun toXMLStartTag(element: Element): String {
            val encoding = "UTF-8"
            try {
                ByteArrayOutputStream().use { bytes ->
                    object : Serializer(bytes, encoding) {
                        @Throws(IOException::class)
                        public override fun writeStartTag(element: Element?) {
                            super.writeStartTag(element)
                        }
                    }.writeStartTag(element)
                    return bytes.toString(encoding)
                }
            } catch (e: IOException) {
                throw RuntimeException("Encoding not supported, this should not have happened.", e)
            }
        }

        @JvmStatic
        fun toXMLSimple(node: Node): String {
            return if (node is Element) {
                toXMLStartTag(node)
            } else {
                node.toString()
            }
        }

        /**
         * Use XOM XML formatter
         *
         * @param node Any node or document
         */
        @JvmStatic
        fun toXMLPrettyPrint(node: ParentNode): String? {
            try {
                val out = ByteArrayOutputStream()
                val serializer = Serializer(out, StandardCharsets.UTF_8.name())
                serializer.indent = 2

                val wrapperDoc: Document?
                if (node is Document) {
                    wrapperDoc = node
                } else {
                    val nodeCopy = node.copy() as Element?
                    wrapperDoc = Document(nodeCopy)
                }
                wrapperDoc
                    .rootElement
                    .addNamespaceDeclaration(UTDElements.UTD_PREFIX, UTD_NS)

                serializer.write(wrapperDoc)
                return out.toString(StandardCharsets.UTF_8)
            } catch (e: Exception) {
                throw NodeException("Unable to pretty print", node, e)
            }
        }

        /**
         * Get a usable XPath context with the default namespace bound to book: prefix (otherwise they are
         * difficult to query correctly or change between books), and the utd namespace bound to the utd:
         * prefix
         */
        private fun toXPathContext(someElement: ParentNode): XPathContext {
            val element: Element = parentToElement(someElement)
            val context = XPathContext.makeNamespaceContext(element)

            // Reassign default namespace to book
            val bookNS = element.namespaceURI
            if (StringUtils.isNotBlank(bookNS)) {
                context.addNamespace("book", bookNS)
            } else {
                // Doc doesn't have a namespace, but its needed for queries using book: NS prefix
                // According to XOM addNamespace("book", "") means "remove the book namespace"...
                // But an empty string still works in the underlying XPath impl
                try {
                    @Suppress("UNCHECKED_CAST")
                    val map = FIELD_XPATHCONTEXT_NAMESPACES.get(context) as MutableMap<String?, String?>
                    map["book"] = ""
                } catch (e: Exception) {
                    throw RuntimeException("Failed to get map", e)
                }
            }

            // Add utd specific namespace
            context.addNamespace(UTDElements.UTD_PREFIX, UTD_NS)

            context.addNamespace("m", "http://www.w3.org/1998/Math/MathML")

            return context
        }

        /**
         * Split a text node at the given positions
         */
        fun splitTextNode(textNode: Text, vararg splitPos: Int): List<Text> {
            log.trace("Input string '{}' split {}", textNode.value, splitPos.contentToString())
            requireNotNull(textNode.parent) { "TextNode must have parent" }
            require(splitPos.isNotEmpty()) { "Must specify Positions to split" }
            var lastPos = -2
            for (curSplitPos in splitPos) {
                // Below useless so tidying up
                // if (curSplitPos <= 0) {
                //					throw new NodeException("Postion " + curSplitPos + " must be greater than 0",
                // textNode);
                //				} else
                if (curSplitPos in 1..lastPos) {
                    throw NodeException("Positions must sorted and uniq", textNode)
                }
                lastPos = curSplitPos
            }

            val replacementNodes: MutableList<Text> = mutableListOf()

            // do splitting
            val text = textNode.value
            var lastStart = 0
            var insertIndex = textNode.parent.indexOf(textNode)
            val splitPosItr: MutableIterator<Int?> = Arrays.stream(splitPos).iterator()
            while (lastStart != text.length) {
                var finished = false
                val textPart: String?
                if (splitPosItr.hasNext()) {
                    val curSplitPos: Int = splitPosItr.next()!!
                    textPart = text.substring(lastStart, curSplitPos)
                    lastStart = curSplitPos
                } else {
                    textPart = text.substring(lastStart)
                    finished = true
                }

                val replacementNode = Text(textPart)
                textNode.parent.insertChild(replacementNode, insertIndex++)
                replacementNodes.add(replacementNode)

                if (finished) {
                    break
                }
            }
            textNode.detach()

            return replacementNodes.toList()
        }

        /**
         * Converts generic ParentNode into a usable element - If document, gets the root element - If
         * Element, casts ParentNode to Element
         */
        fun parentToElement(someParentNode: ParentNode): Element {
            return if (someParentNode is Document)
                someParentNode.rootElement
            else
                someParentNode as Element
        }

        @JvmStatic
        fun nodeToElementOrParentOrDocRoot(node: Node): Element? {
            return when (node) {
                is Element -> {
                    node
                }

                is Document -> {
                    node.rootElement
                }

                else -> {
                    node.parent as Element?
                }
            }
        }

        /**
         * Create element copying the namespace from the given element
         */
        fun newElement(someDOMElement: ParentNode, localName: String): Element {
            val nameSpace: String? = parentToElement(someDOMElement).namespaceURI
            return Element(localName, nameSpace)
        }


        fun ancestorVisitorElement(node: Node, onAncestor: Predicate<Element>): Element? {
            return ancestorVisitor(
                node
            ) { n: Node -> n is Element && onAncestor.test(n) } as? Element
        }

        /**
         *
         */
        fun ancestorVisitor(node: Node, onAncestor: Predicate<Node>): Node? {
            val document = node.document
                ?: throw RuntimeException("Element is detached from a document element " + node.toXML())
            if (node === document) return null

            if (onAncestor.test(node)) return node

            // Can be given text nodes which should just go up
            val parentNode = node.parent
            if (parentNode === document) return null
            if (parentNode == null) throw RuntimeException("Has document but parent is null? " + node.toXML())
            return ancestorVisitor(parentNode, onAncestor)
        }

        fun ancestorVisitorFatal(node: Node, onAncestor: Predicate<Element>): Element {
            val result = ancestorVisitorElement(node, onAncestor) ?: throw NodeException("Unable to match node ", node)
            return result
        }

        fun ancestorElementNot(node: Node, matcher: Predicate<Element>): Boolean {
            return ancestorVisitorElement(node, matcher) == null
        }

        fun ancestorElementIs(node: Node, matcher: Predicate<Element>): Boolean {
            return ancestorVisitorElement(node, matcher) != null
        }

        /**
         * Utility to linearly get ancestors
         */
        fun ancestor(element: Node?): MutableList<Element> = ancestorOrSelf(element?.parent as Element?).toMutableList()

        /**
         * Utility to linerally get ancestors
         */
        fun ancestorOrSelf(element: Element?): List<Element> {
            val ancestors: MutableList<Element> = mutableListOf()
            var curAncestor = element
            while (curAncestor != null) {
                ancestors.add(curAncestor)

                val parent = curAncestor.parent
                if (parent is Document) {
                    break
                }
                curAncestor = parent as Element?
            }
            return ancestors.toList()
        }

        /**
         * Lowest Common Ancestor Problem
         */
        fun findCommonParent(elements: List<Element>): Element {
            require(!elements.isEmpty()) { "elements list cannot be empty" }
            for (element in elements) {
                log.debug("in list {}", element.toXML())
                if (element.document == null) {
                    throw NodeException(
                        "Element #" + elements.indexOf(element) + " isn't attached to document", element
                    )
                }
            }

            var curParent: ParentNode = elements[0]
            if (elements.size == 1) return curParent as Element

            var counter = 1
            val doc = curParent.document
            while (true) {
                val remainingElements: MutableSet<Element?> = HashSet<Element?>(elements)
                val curCounter = counter
                childrenRecursiveVisitor(
                    (curParent as Element?)!!
                ) { curElem: Element? ->
                    if (remainingElements.contains(curElem)) {
                        log.debug("Found at iteration {} element {}", curCounter, curElem!!.toXML())
                        remainingElements.remove(curElem)
                    }
                    false
                }
                if (remainingElements.isEmpty()) break
                curParent = curParent.parent
                if (curParent == doc) throw RuntimeException("Unable to find common parent at iteration $counter")
                counter++
            }
            return curParent
        }

        /**
         * Call given callback on every recursive child element until it returns true
         */
        fun childrenRecursiveVisitor(curElem: Element, onElement: Predicate<Element>): Element? {
            if (onElement.test(curElem)) {
                return curElem
            }
            for (iterableXOMChildren in curElem.childElements) {
                val result: Element? = childrenRecursiveVisitor(iterableXOMChildren, onElement)
                if (result != null) {
                    return result
                }
            }
            return null
        }

        fun childrenRecursiveNodeVisitor(curNode: Node, onElement: Predicate<Node>): Node? {
            if (onElement.test(curNode)) {
                return curNode
            }
            for (iterableXOMChildren in curNode.childNodes) {
                val result: Node? = childrenRecursiveNodeVisitor(iterableXOMChildren, onElement)
                if (result != null) {
                    return result
                }
            }
            return null
        }

        /**
         * Apply given namespace to every revursive child element
         */
        fun setNamespaceRecursive(root: Element, namespaceURI: String?) {
            childrenRecursiveVisitor(
                root
            ) { curElement: Element? ->
                curElement!!.namespaceURI = namespaceURI
                false
            }
        }

        /**
         * Gets the next sibling node under the given nodes parent
         */
        @JvmStatic
        fun nextSiblingNode(node: Node): Node? {
            val parent = node.parent ?: throw NodeException("Node doesn't have parent", node)

            val index = parent.indexOf(node)
            if (index == parent.getChildCount() - 1) {
                // Last child node of parent
                return null
            }

            return parent.getChild(index + 1)
        }

        /**
         * Gets the next sibling node under the given nodes parent
         */
        @JvmStatic
        fun previousSiblingNode(node: Node): Node? {
            val parent = node.parent ?: throw NodeException("Node doesn't have parent", node)

            val index = parent.indexOf(node)
            if (index == 0) {
                // First child node of parent
                return null
            }

            return parent.getChild(index - 1)
        }

        /**
         * Returns the node after the current node. If last node in parent, return the parent's (or
         * ancestors) following sibling
         */
        fun followingNode(node: Node): Node? {
            if (node is Document) {
                return null
            }

            if (node.parent == null) {
                throw NodeException("Start node doesn't have parent", node)
            }

            val parent = node.parent
            val index = parent.indexOf(node)
            if (index == parent.getChildCount() - 1) {
                // Last entry in parent, get parents sibling
                return followingNode(parent)
            }

            return parent.getChild(index + 1)
        }

        /**
         * Java impl of following:: xpath query, will start searching after current node
         */
        fun followingVisitor(startNode: Node, onNode: Predicate<Node>): Node? {
            return followingWithSelfVisitor(followingNode(startNode), onNode)
        }

        fun followingWithSelfVisitor(startNode: Node?, onNode: Predicate<Node>): Node? {
            var curNode = startNode
            while (curNode != null) {
                //			log.trace("Current node " + XMLHandler.toXMLSimple(curNode));
                if (onNode.test(curNode)) return curNode

                curNode = if (curNode.childCount != 0) {
                    curNode.getChild(0)
                } else {
                    followingNode(curNode)
                }
            }
            return null
        }
    }
}

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
package org.brailleblaster.utd.internal.xml;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import nu.xom.*;
import org.apache.commons.lang3.StringUtils;
import org.brailleblaster.utd.exceptions.NodeException;
import org.brailleblaster.utd.exceptions.UTDException;
import org.brailleblaster.utd.properties.UTDElements;
import org.brailleblaster.utils.NamespacesKt;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Stream;

public class XMLHandler2 {
    public static final Logger log = LoggerFactory.getLogger(XMLHandler2.class);
    private static final Field FIELD_XPATHCONTEXT_NAMESPACES;

    static {
        try {
            // Due to ridiculous hiding in XOMs design, need to abuse reflection
            FIELD_XPATHCONTEXT_NAMESPACES = XPathContext.class.getDeclaredField("namespaces");
            FIELD_XPATHCONTEXT_NAMESPACES.setAccessible(true);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to init fields", ex);
        }
    }
    public static void wrapNodeWithElement(@NotNull Node nodeToWrap, @NotNull Element wrapper) {
        nodeToWrap.getParent().replaceChild(nodeToWrap, wrapper);
        wrapper.appendChild(nodeToWrap);
    }

    public static void unwrapElement(@NotNull Element elem) {
        if (elem.getChildCount() > 0) {
            // Reverse insert as insertChild will move over subsequent children
            int parentIndex = elem.getParent().indexOf(elem);
            while (elem.getChildCount() != 0) {
                Node curChildNode = elem.getChild(elem.getChildCount() - 1);
                curChildNode.detach();
                elem.getParent().insertChild(curChildNode, parentIndex);
            }
        }
        elem.detach();
    }

    /**
     * Avoids potentially expensive deep copies
     */
    public static Element shallowCopy(@NotNull Element elem) {
        try {
            Method method = Element.class.getDeclaredMethod("copyTag", Element.class);
            method.setAccessible(true);
            return (Element) method.invoke(null, elem);
        } catch (Exception e) {
            throw new UTDException("Failed to invoke writeStartTag method", e);
        }
    }

    public static Stream<Node> queryStream(
            @NotNull Node node, @NotNull String xpathPattern, Object... xpathArgs) {
        return Streams.stream(query(node, xpathPattern, xpathArgs));
    }

    /**
     * Utility to query using {@link #toXPathContext(ParentNode) } and with slf4j string arguments
     */
    public static Nodes query(@NotNull Node node, @NotNull String xpathPattern, Object... xpathArgs) {
        String realXpath = MessageFormatter.arrayFormat(xpathPattern, xpathArgs).getMessage();
        log.trace(
                "Executing XPath {} on {}",
                realXpath,
                node instanceof Document
                        ? "Document"
                        : node instanceof Element ? toXMLStartTag((Element) node) : node.toString());

        // Handle nodes potentially without a suitable parent to get namespaces from (ie detached text node)
        // Using namespaces in that case is broken anyway
        ParentNode nodeAsParent = node instanceof ParentNode ? (ParentNode) node : node.getParent();
        XPathContext context = nodeAsParent != null ? toXPathContext(nodeAsParent) : null;

        return node.query(realXpath, context);
    }

    public static List<Element> queryElements(@NotNull Node node, @NotNull String xpathPattern, Object... xpathArgs) {
        return Lists.newArrayList(Iterables.filter(query(node, xpathPattern, xpathArgs), Element.class));
    }

    public static Text findFirstText(@NotNull Element someElement) {
        return queryStream(someElement, "descendant::text()[not(ancestor::utd:brl)]")
                .map(n -> (Text) n)
                .filter(t -> !t.getValue().isBlank())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Couldn't find text in " + someElement.toXML()));
    }

    /**
     * Get start tag without children or end tab by calling private method
     * Element.writeStartTag(Element element, StringBuilder result)
     */
    public static String toXMLStartTag(@NotNull Element element) {
        final String encoding = "UTF-8";
        try(ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            new Serializer(bytes, encoding) {
                @Override
                public void writeStartTag(Element element) throws IOException {
                    super.writeStartTag(element);
                }
            }.writeStartTag(element);
            return bytes.toString(encoding);
        } catch (IOException e) {
            throw new RuntimeException("Encoding not supported, this should not have happened.", e);
        }
    }

    public static String toXMLSimple(@NotNull Node node) {
        if (node instanceof Element) {
            return toXMLStartTag((Element) node);
        } else {
            return node.toString();
        }
    }

    /**
     * Use XOM XML formatter
     *
     * @param node Any node or document
     */
    public static String toXMLPrettyPrint(@NotNull ParentNode node) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Serializer serializer = new Serializer(out, StandardCharsets.UTF_8.name());
            serializer.setIndent(2);

            Document wrapperDoc;
            if (node instanceof Document) {
                wrapperDoc = (Document) node;
            } else {
                Element nodeCopy = (Element) node.copy();
                wrapperDoc = new Document(nodeCopy);
            }
            wrapperDoc
                    .getRootElement()
                    .addNamespaceDeclaration(UTDElements.UTD_PREFIX, NamespacesKt.UTD_NS);

            serializer.write(wrapperDoc);
            return out.toString(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new NodeException("Unable to pretty print", node, e);
        }
    }

    /**
     * Get a usable XPath context with the default namespace bound to book: prefix (otherwise they are
     * difficult to query correctly or change between books), and the utd namespace bound to the utd:
     * prefix
     */
    private static XPathContext toXPathContext(@NotNull ParentNode someElement) {
        Element element = parentToElement(someElement);
        XPathContext context = XPathContext.makeNamespaceContext(element);

        // Reassign default namespace to book
        String bookNS = element.getNamespaceURI();
        if (StringUtils.isNotBlank(bookNS)) {
            context.addNamespace("book", bookNS);
        } else {
            // Doc doesn't have a namespace, but its needed for queries using book: NS prefix
            // According to XOM addNamespace("book", "") means "remove the book namespace"...
            // But an empty string still works in the underlying XPath impl
            try {
                @SuppressWarnings("unchecked")
                Map<String, String> map = (Map<String, String>) FIELD_XPATHCONTEXT_NAMESPACES.get(context);
                map.put("book", "");
            } catch (Exception e) {
                throw new RuntimeException("Failed to get map", e);
            }
        }

        // Add utd specific namespace
        context.addNamespace(UTDElements.UTD_PREFIX, NamespacesKt.UTD_NS);

        context.addNamespace("m", "http://www.w3.org/1998/Math/MathML");

        return context;
    }

    /**
     * Split a text node at the given positions
     */
    public static List<Text> splitTextNode(@NotNull Text textNode, int... splitPos) {
        log.trace("Input string '{}' split {}", textNode.getValue(), Arrays.toString(splitPos));
        Preconditions.checkNotNull(textNode.getParent(), "TextNode must have parent");
        Preconditions.checkArgument(splitPos.length != 0, "Must specify Positions to split");
        int lastPos = -2;
        for (int curSplitPos : splitPos) {
            // Below useless so tidying up
            // if (curSplitPos <= 0) {
            //					throw new NodeException("Postion " + curSplitPos + " must be greater than 0",
            // textNode);
            //				} else
            if (curSplitPos > 0 && curSplitPos <= lastPos) {
                throw new NodeException("Positions must sorted and uniq", textNode);
            }
            lastPos = curSplitPos;
        }

        List<Text> replacementNodes = new ArrayList<>();

        // do splitting
        String text = textNode.getValue();
        int lastStart = 0;
        int insertIndex = textNode.getParent().indexOf(textNode);
        Iterator<Integer> splitPosItr = Arrays.stream(splitPos).iterator();
        while (lastStart != text.length()) {
            boolean finished = false;
            String textPart;
            if (splitPosItr.hasNext()) {
                int curSplitPos = splitPosItr.next();
                textPart = text.substring(lastStart, curSplitPos);
                lastStart = curSplitPos;
            } else {
                textPart = text.substring(lastStart);
                finished = true;
            }

            Text replacementNode = new Text(textPart);
            textNode.getParent().insertChild(replacementNode, insertIndex++);
            replacementNodes.add(replacementNode);

            if (finished) {
                break;
            }
        }
        textNode.detach();

        return replacementNodes;
    }

    /**
     * Converts generic ParentNode into a usable element - If document, gets the root element - If
     * Element, casts ParentNode to Element
     */
    @NotNull
    public static Element parentToElement(@NotNull ParentNode someParentNode) {
        return someParentNode instanceof Document
                ? ((Document) someParentNode).getRootElement()
                : (Element) someParentNode;
    }

    public static Element nodeToElementOrParentOrDocRoot(Node node) {
        if (node instanceof Element) {
            return (Element) node;
        } else if (node instanceof Document) {
            return ((Document) node).getRootElement();
        } else {
            return (Element) node.getParent();
        }
    }

    /**
     * Create element copying the namespace from the given element
     */
    public static Element newElement(ParentNode someDOMElement, String localName) {
        String nameSpace = parentToElement(someDOMElement).getNamespaceURI();
        return new Element(localName, nameSpace);
    }
}

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
package org.brailleblaster.testrunners;


import java.io.File;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.StringUtils;
import org.brailleblaster.utd.exceptions.NodeException;
import org.brailleblaster.utd.internal.xml.FastXPath;
import org.brailleblaster.utd.internal.xml.XMLHandler;
import org.brailleblaster.utd.utils.UTDHelper;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.ParentNode;
import nu.xom.Text;
import org.brailleblaster.bbx.BBX;
import org.brailleblaster.utils.xml.NamespacesKt;

/**
 * Utilities for XML..m
 */
public class TestXMLUtils {
    public static String generateBookString(String headXmlContent, String bookXmlContent) {
        return "<?xml version='1.0' encoding='UTF-8'?>"
                + "<!DOCTYPE dtbook"
                + "  PUBLIC '-//NISO//DTD dtbook 2005-3//EN'"
                + "  'http://www.daisy.org/z3986/2005/dtbook-2005-3.dtd'>"
                + "<dtbook version='2005-3' "
                + "xmlns='http://www.daisy.org/z3986/2005/dtbook/' "
                + "xmlns:utd='" + NamespacesKt.UTD_NS + "' "
                + "xmlns:m='" + "http://www.w3.org/1998/Math/MathML" + "' "
                + "xmlns:bb='" + NamespacesKt.BB_NS + "' "
                + ">"
                + "<head>"
                + "<utd:isNormalised>true</utd:isNormalised>"
                + headXmlContent
                + "</head>"
                + "<book bbtestroot='true' testid='testroot'>"
                + bookXmlContent
                + "</book>"
                + "</dtbook>";
    }

    public static Document generateBookDoc(String headXML, String bodyXML) {
        String xml = generateBookString(headXML, bodyXML);
        return new XMLHandler().load(new StringReader(xml));
    }

    public static File generateBookTestFile(String headXML, String bodyXML) {
        Document doc = generateBookDoc(headXML, bodyXML);
        File testFile = new File(BBViewTestRunner.TEST_DIR, "TestBook.xml");
        new XMLHandler().save(doc, testFile);
        return testFile;
    }

    public static Element getTestIdElement(Document doc, String id) {
        return getTestIdElementStream(doc, id)
                .findFirst()
                .orElseThrow(() -> new NodeException("testid of " + id + " not found in doc", doc));
    }

    public static List<Element> getTestIdElements(Document doc, String id) {
        return getTestIdElementStream(doc, id)
                .collect(Collectors.toList());
    }

    private static Stream<Element> getTestIdElementStream(Document doc, String id) {
        return StreamSupport.stream(FastXPath.descendant(doc).spliterator(), false)
                .filter(curNode -> curNode instanceof Element
                        && ((Element) curNode).getAttribute("testid") != null
                        && ((Element) curNode).getAttribute("testid").getValue().equals(id)
                ).map(curNode -> (Element) curNode);
    }

    public static Text getByTextNode(Document doc, String textValue) {
        return StreamSupport.stream(FastXPath.descendant(doc).spliterator(), false)
                .filter(node -> node instanceof Text && node.getValue().equals(textValue))
                .findFirst()
                .map(node -> (Text) node)
                .get();
    }

    public static Element getElementByOccurance(Document doc, String elementName, int occurrence) {
        return StreamSupport.stream(FastXPath.descendant(doc).spliterator(), false)
                .filter(node -> node instanceof Element && ((Element) node).getLocalName().equals(elementName))
                .skip(occurrence)
                .findFirst()
                .map(node -> (Element) node)
                .get();
    }

    public static String toXMLnoNS(Node node) {
        Node copiedNode = node.copy();
        if (copiedNode instanceof ParentNode) {
            UTDHelper.stripUTDRecursive((Element) copiedNode);
        }
        String xml = copiedNode.toXML();
        xml = StringUtils.replace(xml, " xmlns:utd=\"http://brailleblaster.org/ns/utd\"", "");
        xml = StringUtils.replace(xml, " xmlns=\"http://www.daisy.org/z3986/2005/dtbook/\"", "");
        return xml;
    }

    protected static File docToFile(Document doc) {
        File testFile = new File(BBViewTestRunner.TEST_DIR, "TestBook.xml");
        new XMLHandler().save(doc, testFile);
        return testFile;
    }

    public static Element getBookRoot(Document doc) {
        return StreamSupport.stream(FastXPath.descendant(doc).spliterator(), false)
                .filter(n -> n instanceof Element)
                .map(n -> (Element) n)
                .filter(elem -> "testroot".equals(elem.getAttributeValue("testid")))
                .findFirst()
                .orElseThrow(() -> new NodeException("asdfasdf", doc));
    }

    public static XMLElementAssert assertRootSection(Document doc) {
        return new XMLElementAssert(
                doc.getRootElement(),
                null
        )
                .stripUTDAndCopy()
                .child(1)
                .isSection(BBX.SECTION.ROOT)
                .validate();
    }

    public static XMLElementAssert assertNimasRoot(Document doc) {
        return new XMLElementAssert(getTestIdElement(doc, "testroot"), null);
    }
}

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
package org.brailleblaster.utd.matchers;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.brailleblaster.utd.NamespaceMap;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import nu.xom.Comment;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.ProcessingInstruction;
import nu.xom.Text;
import org.brailleblaster.utd.internal.JAXBUtils;

import static org.testng.Assert.*;

public class NodeNameMatcherTest {
    private NamespaceMap namespaceMap;

    @BeforeClass
    private void setupMethod() {
        namespaceMap = new NamespaceMap();
        namespaceMap.addNamespace("xhtml", "http://www.w3.org/1999/xhtml");
        namespaceMap.addNamespace("m", "http://www.w3.org/1998/Math/MathML");
    }

    @DataProvider(name = "nodeNamesProvider")
    private Object[][] nodeNamesProvider() {
        return new Object[][]{{"p", "xhtml"}, {"h1", "xhtml"}, {"math", "m"}};
    }

    @Test(dataProvider = "nodeNamesProvider")
    public void constructWithNameNoNamespace(String nodeName, String namespace) {
        NodeNameMatcher matcher = new NodeNameMatcher(nodeName);
        assertEquals(matcher.getNodeName(), nodeName);
        assertNull(matcher.getNamespace());
    }

    @Test(dataProvider = "nodeNamesProvider")
    public void constructWithNameAndNamespace(String nodeName, String namespace) {
        NodeNameMatcher matcher = new NodeNameMatcher(nodeName, namespace);
        assertEquals(matcher.getNodeName(), nodeName);
        assertEquals(matcher.getNamespace(), namespace);
    }

    @Test
    public void isMatcher() {
        assertTrue(INodeMatcher.class.isAssignableFrom(NodeNameMatcher.class));
    }

    @Test
    public void matchNoNamespace() {
        NodeNameMatcher matcher = new NodeNameMatcher("p");
        Element node = new Element("p");
        assertTrue(matcher.isMatch(node, new NamespaceMap()));
        node = new Element("h1");
        assertFalse(matcher.isMatch(node, new NamespaceMap()));
    }

    @Test(dataProvider = "nodeNamesProvider")
    public void notMatchNonElements(String nodeName, String namespace) {
        NodeNameMatcher matcher = new NodeNameMatcher(nodeName, namespace);
        Node node = new Text("Some text");
        assertFalse(matcher.isMatch(node, new NamespaceMap()));
        node = new ProcessingInstruction("include", "somefile.txt");
        assertFalse(matcher.isMatch(node, new NamespaceMap()));
        node = new Comment("Some comment");
        assertFalse(matcher.isMatch(node, new NamespaceMap()));
    }

    @Test
    public void matchWithNamespaces() {
        NodeNameMatcher matcher = new NodeNameMatcher("p", "xhtml");
        Element node = new Element("p", "http://www.w3.org/1999/xhtml");
        assertTrue(matcher.isMatch(node, namespaceMap));
        node = new Element("b", "http://www.w3.org/1999/xhtml");
        assertFalse(matcher.isMatch(node, namespaceMap));
        node = new Element("p", "http://www.w3.org/1998/Math/MathML");
        assertFalse(matcher.isMatch(node, namespaceMap));
    }

    // Fields aren't final because JAXB sets them; nodeNameParts is a cache derived from nodeName
    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(NodeNameMatcher.class)
                .suppress(Warning.STRICT_INHERITANCE, Warning.NONFINAL_FIELDS)
                .withIgnoredFields("nodeNameParts")
                .verify();
    }

    @Test(dataProvider = "nodeNamesProvider")
    public void marshal(String nodeName, String namespace) throws ParserConfigurationException, JAXBException {
        NodeNameMatcher matcher = new NodeNameMatcher(nodeName, namespace);
        JAXBElement<NodeNameMatcher> element = new JAXBElement<>(new QName("matcher"), NodeNameMatcher.class, matcher);
        JAXBContext jc = JAXBContext.newInstance(NodeNameMatcher.class);
        Marshaller m = jc.createMarshaller();
        m.setEventHandler(JAXBUtils.FAIL_ON_EXCEPTIONS_HANDLER);
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = documentBuilder.newDocument();
        m.marshal(element, doc);
        org.w3c.dom.Element result = doc.getDocumentElement();
        assertFalse(result.hasChildNodes());
        assertEquals(result.getAttributes().getLength(), 2);
        assertEquals(result.getAttribute("nodeName"), nodeName);
        assertEquals(result.getAttribute("namespace"), namespace);
    }

    @Test(dataProvider = "nodeNamesProvider")
    public void unmarshal(String nodeName, String namespace) throws JAXBException, ParserConfigurationException {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = documentBuilder.newDocument();
        org.w3c.dom.Element element = doc.createElement("matcher");
        element.setAttribute("nodeName", nodeName);
        element.setAttribute("namespace", namespace);
        doc.appendChild(element);
        JAXBContext jc = JAXBContext.newInstance(NodeNameMatcher.class);
        Unmarshaller um = jc.createUnmarshaller();
        um.setEventHandler(JAXBUtils.FAIL_ON_EXCEPTIONS_HANDLER);
        NodeNameMatcher result = um.unmarshal(doc, NodeNameMatcher.class).getValue();
        assertEquals(result.getNodeName(), nodeName);
        assertEquals(result.getNamespace(), namespace);
    }

    @Test
    public void testEqualsAndHashCodeSameObject() {
        INodeMatcher matcher = new NodeNameMatcher("sidebar");
        assertEquals(matcher, matcher);
        assertEquals(matcher.hashCode(), matcher.hashCode());
    }

    @Test
    public void testEqualsAndHashCodeSimilarObjects() {
        INodeMatcher matcher1 = new NodeNameMatcher("sidebar");
        INodeMatcher matcher2 = new NodeNameMatcher("sidebar");
        assertEquals(matcher2, matcher1);
        assertEquals(matcher1, matcher2);
        assertEquals(matcher1.hashCode(), matcher2.hashCode());
    }

    @Test
    public void testEqualsAndHashCodeSubtype() {
        INodeMatcher matcher = new NodeNameMatcher("sidebar");
        INodeMatcher subMatcher = new NodeAttributeMatcher("sidebar", "box", "true");
        assertNotEquals(subMatcher, matcher);
        assertNotEquals(matcher, subMatcher);
    }
}

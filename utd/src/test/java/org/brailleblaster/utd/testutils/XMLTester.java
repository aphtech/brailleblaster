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
package org.brailleblaster.utd.testutils;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import nu.xom.Document;
import nu.xom.Element;

import nu.xom.Node;
import nu.xom.canonical.Canonicalizer;
import org.brailleblaster.utd.internal.xml.XMLHandler;

import static org.testng.Assert.assertEquals;

/**
 * Assert methods for comparing XML.
 */
public class XMLTester {
    /**
     * Assert that two XOM nodes are similar.
     * <p>
     * This method will attempt to canonicalise both the actual and expected nodes and then will compare the results. If the canonicalised forms are equal then this will not raise any assertion errors.
     *
     * @param actual   The actual node created by the code under test.
     * @param expected The expected node which should be compared against.
     * @throws Exception
     */
    public static void assertSimilarXML(Node actual, Node expected) throws Exception {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        Canonicalizer canonicaliser = new Canonicalizer(byteStream);
        canonicaliser.write(actual);
        byteStream.reset();
        canonicaliser.write(expected);
        assertEquals(byteStream.toString(StandardCharsets.UTF_8), byteStream.toString(StandardCharsets.UTF_8));
    }

    public static String generateBookString(String headXmlContent, String bookXmlContent) {
        return "<?xml version='1.0' encoding='UTF-8'?>"
                + "<!DOCTYPE dtbook"
                + "  PUBLIC '-//NISO//DTD dtbook 2005-3//EN'"
                + "  'http://www.daisy.org/z3986/2005/dtbook-2005-3.dtd'>"
                + "<dtbook version='2005-3' xmlns='http://www.daisy.org/z3986/2005/dtbook/' xmlns:utd='http://brailleblaster.org/ns/utd'>"
                + "<head testid='head'>"
                + "<utd:isNormalised>true</utd:isNormalised>"
                + headXmlContent
                + "</head>"
                + "<book testid='book'>"
                + "<level1 testid='contentRoot'>"
                + bookXmlContent
                + "</level1>"
                + "</book>"
                + "</dtbook>";
    }

    public static Document generateBookDoc(String headXML, String bodyXML) {
        String xml = generateBookString(headXML, bodyXML);
        return new XMLHandler().load(new StringReader(xml));
    }

    public static Element getTestIdElement(Document doc, String testId) {
        return (Element) XMLHandler.query(doc, "descendant::*[@testid='{}']", testId).get(0);
    }
}

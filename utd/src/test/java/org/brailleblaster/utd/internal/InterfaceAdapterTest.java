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
package org.brailleblaster.utd.internal;

import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import nu.xom.Node;

import org.brailleblaster.utd.ITranslationEngine;
import org.brailleblaster.utd.TextSpan;
import org.brailleblaster.utd.actions.IAction;
import org.brailleblaster.utd.matchers.XPathMatcher;
import org.jspecify.annotations.NonNull;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class InterfaceAdapterTest {
    public static class TestAction implements IAction {
        @Override
        public @NonNull List<TextSpan> applyTo(@NonNull Node node, @NonNull ITranslationEngine context) {
            return Collections.emptyList();
        }
    }

    @Test
    public void marshalNull() throws Exception {
        InterfaceAdapter adapter = new InterfaceAdapter();
        assertNull(adapter.marshal(null));
    }

    @Test
    public void marshalAction() throws Exception {
        InterfaceAdapter adapter = new InterfaceAdapter();
        IAction action = new TestAction();
        Object resultObj = adapter.marshal(action);
        assertTrue(resultObj instanceof Element);
        Element result = (Element) resultObj;
        assertEquals(result.getLocalName(), "object");
        assertEquals(result.getAttribute("type"), TestAction.class.getName());
    }

    @Test
    public void marshalMatcher() throws Exception {
        XPathMatcher matcher = new XPathMatcher("//xhtml:p");
        InterfaceAdapter adapter = new InterfaceAdapter();
        Object resultObj = adapter.marshal(matcher);
        assertTrue(resultObj instanceof Element);
        Element result = (Element) resultObj;
        assertEquals(result.getLocalName(), "object");
        assertEquals(result.getAttribute("type"), matcher.getClass().getName());
        assertEquals(result.getAttribute("expression"), matcher.getExpression());
    }

    @Test
    public void unmarshalNull() throws Exception {
        InterfaceAdapter adapter = new InterfaceAdapter();
        assertNull(adapter.unmarshal(null));
    }

    @Test
    public void unmarshalAction() throws Exception {
        InterfaceAdapter adapter = new InterfaceAdapter();
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.newDocument();
        Element element = doc.createElement("object");
        element.setAttribute("type", TestAction.class.getName());
        Object obj = adapter.unmarshal(element);
        assertTrue(obj instanceof TestAction);
    }

    @Test
    public void unmarshalMatcher() throws Exception {
        InterfaceAdapter adapter = new InterfaceAdapter();
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = documentBuilder.newDocument();
        Element element = doc.createElement("object");
        element.setAttribute("type", XPathMatcher.class.getName());
        element.setAttribute("expression", "//p");
        Object resultObj = adapter.unmarshal(element);
        assertTrue(resultObj instanceof XPathMatcher);
        XPathMatcher matcher = (XPathMatcher) resultObj;
        assertEquals(matcher.getExpression(), "//p");
    }
}

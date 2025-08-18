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
package org.brailleblaster.utd.xslt;

import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import kotlin.text.Charsets;
import org.testng.ITest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import com.google.common.io.Resources;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.xslt.XSLException;
import nu.xom.xslt.XSLTransform;

public class TransformTest implements ITest {
    private final String transformResource;
    private final Map<String, Object> transformParams;
    private final Nodes inputNodes;
    private final Nodes expectedNodes;
    private final String testName;

    @Override
    public String getTestName() {
        return testName;
    }

    @Factory(dataProvider = "transformsProvider")
    public TransformTest(String transformResource, String testResource, Element testNode) {
        String testId = testNode.getAttributeValue("name");
        if (testId == null) {
            testId = String.format("Test at position %d with no name", testNode.getParent().indexOf(testNode));
        }
        this.testName = String.format("Transforming %s from resource %s with %s", testId, testResource, transformResource);
        this.transformResource = transformResource;
        this.transformParams = new HashMap<>();
        Nodes paramNodes = testNode.query("param");
        for (int i = 0; i < paramNodes.size(); i++) {
            Node paramNode = paramNodes.get(i);
            if (paramNode instanceof Element) {
                Element paramElem = (Element) paramNode;
                String paramName = paramElem.getAttributeValue("name");
                String paramValue = paramElem.getAttributeValue("value");
                if (paramName != null && paramValue != null) {
                    transformParams.put(paramName, paramValue);
                }
            }
        }
        this.inputNodes = testNode.query("input/node()");
        this.expectedNodes = testNode.query("expected/node()");
    }

    @DataProvider(name = "transformsProvider")
    public static Iterator<Object[]> transformsProvider() throws IOException {
        List<Object[]> data = new ArrayList<>();
        List<String> testResources = Resources.readLines(Resources.getResource(TransformTest.class, "/org/brailleblaster/utd/xslt/tests_list.txt"), Charsets.UTF_8);
        for (String xr : testResources) {
            String xmlResource = xr.trim();
            if (xmlResource.isEmpty() || xmlResource.startsWith("#")) {
                continue;
            }
            Builder builder = new Builder();
            Document testsDoc;
            try (InputStream inStream = Resources.getResource(TransformTest.class, xmlResource).openStream()) {
                testsDoc = builder.build(inStream);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                // Was obviously not a valid test
                continue;
            }
            Nodes xsltTransforms = testsDoc.query("/tests/@xsltResource");
            String transform = xsltTransforms.size() == 1 ? transform = xsltTransforms.get(0).getValue() : "/org/brailleblaster/utd/xslt/default.xsl";
            Nodes tests = testsDoc.query("/tests/test[not(@fromXml='false')]");
            for (int i = 0; i < tests.size(); i++) {
                Node testNode = tests.get(i);
                data.add(new Object[]{transform, xmlResource, testNode});
            }
        }
        return data.iterator();
    }

    @Test
    public void xslTransform() throws IOException, ParsingException, XSLException {
        Builder builder = new Builder();
        Document stylesheet;
        try (InputStream inStream = Resources.getResource(this.getClass(), transformResource).openStream()) {
            stylesheet = builder.build(inStream);
        }
        XSLTransform transform = new XSLTransform(stylesheet);
        for (Map.Entry<String, Object> e : transformParams.entrySet()) {
            transform.setParameter(e.getKey(), e.getValue());
        }
        Nodes actualNodes = transform.transform(inputNodes);
        assertEquals(actualNodes.size(), expectedNodes.size(), "Not expected number of nodes returned");
        for (int j = 0; j < actualNodes.size(); j++) {
            assertEquals(actualNodes.get(j).toXML(), expectedNodes.get(j).toXML());
        }
    }
}

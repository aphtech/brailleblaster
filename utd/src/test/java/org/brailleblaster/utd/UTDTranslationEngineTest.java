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
package org.brailleblaster.utd;

import nu.xom.*;
import org.brailleblaster.utd.actions.GenericBlockAction;
import org.brailleblaster.utd.actions.IAction;
import org.brailleblaster.utd.actions.IBlockAction;
import org.brailleblaster.utd.config.DocumentUTDConfig;
import org.brailleblaster.utd.internal.NormaliserFactory;
import org.brailleblaster.utd.internal.xml.XMLHandler;
import org.brailleblaster.utd.matchers.NodeNameMatcher;
import org.brailleblaster.utd.matchers.XPathMatcher;
import org.brailleblaster.utd.testutils.XMLTester;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.*;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

public class UTDTranslationEngineTest {
    private final Builder normalisingBuilder = new Builder(new NormaliserFactory());
    private final String[] testDocumentNames = new String[]{
            "basic1.xml", "basic2.xml", "rootBlockTest.xml", "lineSpacingTest.xml", "lineSpacingPageWrap.xml", "lineSpacingMultipleParagraphs.xml",
    };

    private Document loadResourceDocument(String resourceName) throws ParsingException, IOException {
        InputStream inStream = null;
        Document doc = null;
        try {
            inStream = getClass().getResourceAsStream(resourceName);
            doc = normalisingBuilder.build(inStream);

            //Normalisation will add a isNormalised element to the head which breaks the basic equality tests here
            DocumentUTDConfig.NIMAS.getHeadElement(doc).detach();
        } finally {
            if (inStream != null) {
                inStream.close();
            }
        }
        return doc;
    }

    private StyleMap createTestStyleMap() {
        StyleMap styleMap = new StyleMap();
        Style style = new Style();
        style.setName("heading1");
        style.setLinesBefore(2);
        style.setLinesAfter(2);
        styleMap.put(new NodeNameMatcher("h1"), style);
        style = new Style();
        style.setName("para");
        style.setLinesBefore(1);
        style.setLinesAfter(1);
        styleMap.put(new NodeNameMatcher("p"), style);
        styleMap.put(new NodeNameMatcher("textSegment"), style);
        style = new Style();
        style.setName("doubleLine");
        // Set the lineSpacing for double line spacing
        style.setLineSpacing(2);
        styleMap.put(new NodeNameMatcher("doubleLine"), style);
        return styleMap;
    }

    @Test
    public void defaultConstructor() {
        UTDTranslationEngine engine = new UTDTranslationEngine();
        assertNotNull(engine.getActionMap());
        assertNotNull(engine.getStyleMap());
        assertNotNull(engine.getBrailleSettings());
        assertNotNull(engine.getPageSettings());
        assertNotNull(engine.getBrailleTranslator());
        assertNotNull(engine.getStyleDefinitions());
    }

    @Test
    public void copyingConstructor() {
        UTDTranslationEngine originalEngine = new UTDTranslationEngine();
        UTDTranslationEngine copyEngine = new UTDTranslationEngine(originalEngine);
        assertNotSame(copyEngine, originalEngine);
        assertEquals(copyEngine.getActionMap(), originalEngine.getActionMap());
        assertEquals(copyEngine.getBrailleSettings(), originalEngine.getBrailleSettings());
        assertEquals(copyEngine.getBrailleTranslator(), originalEngine.getBrailleTranslator());
        assertEquals(copyEngine.getPageSettings(), originalEngine.getPageSettings());
        assertEquals(copyEngine.getStyleDefinitions(), originalEngine.getStyleDefinitions());
        assertEquals(copyEngine.getStyleMap(), originalEngine.getStyleMap());
    }

    @Test
    public void actionMapGettersAndSetters() {
        UTDTranslationEngine context = new UTDTranslationEngine();
        ActionMap actionMap = new ActionMap();
        context.setActionMap(actionMap);
        assertSame(context.getActionMap(), actionMap);
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public void actionMapSetNull() {
        UTDTranslationEngine context = new UTDTranslationEngine();
        context.setActionMap(null);
    }

    @Test
    public void styleMapGettersAndSetters() {
        UTDTranslationEngine context = new UTDTranslationEngine();
        StyleMap styleMap = new StyleMap();
        context.setStyleMap(styleMap);
        assertSame(context.getStyleMap(), styleMap);
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public void styleMapSetNull() {
        UTDTranslationEngine context = new UTDTranslationEngine();
        context.setStyleMap(null);
    }

    @Test
    public void brailleSettingsGettersAndSetters() {
        UTDTranslationEngine context = new UTDTranslationEngine();
        BrailleSettings brlSettings = new BrailleSettings();
        context.setBrailleSettings(brlSettings);
        assertSame(context.getBrailleSettings(), brlSettings);
    }

    @Test
    public void pageSettingsGetterAndSetters() {
        UTDTranslationEngine context = new UTDTranslationEngine();
        PageSettings pageSettings = new PageSettings();
        context.setPageSettings(pageSettings);
        assertSame(context.getPageSettings(), pageSettings);
    }

    @DataProvider(name = "testDocumentsProvider")
    public Iterator<Object[]> translateDocumentProvider() throws Exception {
        List<Object[]> data = new ArrayList<>();
        ActionMap actionMap = new ActionMap();
        actionMap.put(new XPathMatcher("self::p"), new GenericBlockAction());
        for (String documentName : this.testDocumentNames) {
            data.add(new Object[]{loadResourceDocument("/org/brailleblaster/utd/inputDocuments/" + documentName), loadResourceDocument("/org/brailleblaster/utd/simpleTranslatedDocuments/" + documentName), actionMap});
        }
        return data.iterator();
    }

    @Test(dataProvider = "testDocumentsProvider")
    public void translateWithDocumentAsParameter(Document inputDoc, Document expectedDoc, IActionMap actionMap) {
        UTDTranslationEngine context = new UTDTranslationEngine();
        context.setActionMap(actionMap);
        Nodes resultNodes = context.translate(inputDoc, true);
        assertEquals(resultNodes.size(), 1);
        assertNotSame(resultNodes.get(0), inputDoc);
        assertEquals(resultNodes.get(0).toXML(), expectedDoc.toXML());
    }

    @Test(dataProvider = "testDocumentsProvider")
    public void translateDocument(Document inputDoc, Document expectedDoc, IActionMap actionMap) {
        UTDTranslationEngine context = new UTDTranslationEngine();
        context.setActionMap(actionMap);
        Document result = context.translateDocument(inputDoc, true);
        assertNotSame(result, inputDoc);
        assertEquals(result.toXML(), expectedDoc.toXML());
    }

    @DataProvider(name = "testNodesProvider")
    private Iterator<Object[]> testNodesProvider() {
        List<Object[]> nodes = new ArrayList<>();
        nodes.add(new Object[]{new Element("e")});
        return nodes.iterator();
    }

    @Test(dataProvider = "testNodesProvider")
    public void translateWithNodeAsParameter(Node inputNode) {
        UTDTranslationEngine engine = new UTDTranslationEngine();
        IAction action = new GenericBlockAction();
        IActionMap actionMap = new ActionMap();
        actionMap.put(new XPathMatcher("self::*"), action);
        engine.setActionMap(actionMap);
        Nodes resultNodes = engine.translate(inputNode, true);
        assertEquals(resultNodes.size(), 1);
        assertNotSame(resultNodes.get(0), inputNode);
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void translateNoneBlockNode() {
        Element root = new Element("root");
        Element e = new Element("e");
        root.appendChild(e);
        UTDTranslationEngine engine = new UTDTranslationEngine();
        System.out.println("About to run test");
        engine.translate(e);
    }

    @DataProvider(name = "formatDocumentProvider")
    private Iterator<Object[]> formatDocumentProvider() throws ParsingException, IOException {
        List<Object[]> data = new ArrayList<>();
        StyleMap styleMap = createTestStyleMap();
        for (String documentName : this.testDocumentNames) {
            data.add(new Object[]{loadResourceDocument("/org/brailleblaster/utd/translatedDocuments/" + documentName), loadResourceDocument("/org/brailleblaster/utd/formattedDocuments/" + documentName), styleMap});
        }
        return data.iterator();
    }

    //TODO: Reactivate after formatter changes
    @Test(dataProvider = "formatDocumentProvider", enabled = true)
    public void formatDocument(Document inputDoc, Document expectedDoc, StyleMap styleMap) {
        UTDTranslationEngine context = new UTDTranslationEngine();
        context.setStyleMap(styleMap);
        Document result = context.format(inputDoc);
        assertEquals(result.toXML(), expectedDoc.toXML());
    }

    //	TODO: Reactivate after formatter changes
    @Test(dataProvider = "formatDocumentProvider", enabled = true)
    public void formatRootElement(Document inputDoc, Document expectedDoc, StyleMap styleMap) {
        UTDTranslationEngine context = new UTDTranslationEngine();
        context.setStyleMap(styleMap);
        Document result = context.format(inputDoc.getRootElement());
        assertEquals(result.toXML(), expectedDoc.toXML());
    }

    @DataProvider(name = "translateAndFormatDocumentProvider")
    public Iterator<Object[]> translateAndFormatDocumentProvider() throws ParsingException, IOException {
        List<Object[]> data = new ArrayList<>();
        ActionMap actionMap = new ActionMap();
        actionMap.put(new XPathMatcher("self::p"), new GenericBlockAction());
        StyleMap styleMap = createTestStyleMap();
        for (String documentName : this.testDocumentNames) {
            data.add(new Object[]{loadResourceDocument("/org/brailleblaster/utd/inputDocuments/" + documentName), loadResourceDocument("/org/brailleblaster/utd/simpleFormattedDocuments/" + documentName), actionMap, styleMap});
        }
        return data.iterator();
    }

    //	TODO: Reactivate after formatter changes
    @Test(dataProvider = "translateAndFormatDocumentProvider", enabled = true)
    public void translateAndFormatDocument(Document inputDoc, Document expectedDoc, IActionMap actionMap, StyleMap styleMap) {
        UTDTranslationEngine context = new UTDTranslationEngine();
        context.setActionMap(actionMap);
        context.setStyleMap(styleMap);
        Document result = context.translateAndFormatDocument(inputDoc, true);
        assertNotSame(result, inputDoc);
        assertEquals(result.toXML(), expectedDoc.toXML());
    }

    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void formatDetachedNode() {
        Element p = new Element("p");
        UTDTranslationEngine context = new UTDTranslationEngine();
        context.format(p);
    }

    @DataProvider(name = "findTranslationBlockProvider")
    public Iterator<Object[]> findTranslationBlockProvider() {
        List<Object[]> dataList = new ArrayList<>();
        Element a = new Element("a");
        Element b = new Element("b");
        Element c = new Element("c");
        Element d = new Element("d");
        a.appendChild(b);
        b.appendChild(c);
        c.appendChild(d);
        IBlockAction blockAction = mock(IBlockAction.class);
        IActionMap actionMap = new ActionMap();
        actionMap.put(new XPathMatcher("self::c"), blockAction);
        dataList.add(new Object[]{c, c, actionMap});
        actionMap = new ActionMap();
        actionMap.put(new XPathMatcher("self::b"), blockAction);
        dataList.add(new Object[]{c, b, actionMap});
        actionMap = new ActionMap();
        actionMap.put(new XPathMatcher("self::a"), blockAction);
        dataList.add(new Object[]{c, a, actionMap});
        actionMap = new ActionMap();
        actionMap.put(new XPathMatcher("self::b"), blockAction);
        actionMap.put(new XPathMatcher("self::d"), blockAction);
        dataList.add(new Object[]{c, b, actionMap});
        actionMap = new ActionMap();
        dataList.add(new Object[]{c, a, actionMap});
        Element root = new Element("root");
        new Document(root);
        dataList.add(new Object[]{root, root, actionMap});
        return dataList.iterator();
    }

    @Test(dataProvider = "findTranslationBlockProvider")
    public void findTranslationBlock(Node inputNode, Node expectedNode, IActionMap actionMap) {
        ITranslationEngine engine = new UTDTranslationEngine();
        engine.setActionMap(actionMap);

        Node result = engine.findTranslationBlock(inputNode);

        assertSame(result, expectedNode);
    }


    /*
     * <sample><p><strong>Some text.</strong></p></sample>
     */
    @Test
    public void testGetStyle() {
        StyleMap styleMap = new StyleMap();
        StyleStack stack = new StyleStack();

        Style style = new Style();
        style.setIndent(5);
        Element sample = new Element("sample");
        styleMap.put(new XPathMatcher("self::sample"), style);
        stack.push(style);

        Style style1 = new Style();
        style1.setLinesBefore(1);
        style1.setFirstLineIndent(2);
        Element p = new Element("p");
        styleMap.put(new XPathMatcher("self::p"), style1);
        stack.push(style1);

        Element strong = new Element("strong");

        Node text = new Text("Some text.");
        strong.appendChild(text);
        p.appendChild(strong);
        sample.appendChild(p);

        UTDTranslationEngine engine = new UTDTranslationEngine();
        engine.setStyleMap(styleMap);

        IStyle ret = engine.getStyle(text);
        assertNotNull(ret);
        assertEquals(ret, style1, "Unexpected resule " + ret);
        assertEquals(ret.getLinesBefore(), 1);
        assertEquals(ret.getFirstLineIndent().intValue(), 2);
    }

    @Test
    public void getTranslationBlocksTest() {
        UTDTranslationEngine engine = new UTDTranslationEngine();

        //Need an example action map for block elements
        ActionMap actionMap = new ActionMap();
        actionMap.put(new NodeNameMatcher("p-inner"), new GenericBlockAction());
        actionMap.put(new NodeNameMatcher("p-outer"), new GenericBlockAction());
        actionMap.put(new NodeNameMatcher("p-more"), new GenericBlockAction());
        actionMap.put(new NodeNameMatcher("book"), new GenericBlockAction());
        engine.setActionMap(actionMap);

        String inputXML = XMLTester.generateBookString("",
                "<p-outer testid='outer'>outer text<p-inner testid='inner'>inner <b testid='other'>text</b></p-inner></p-outer>"
                        + "<p-more testid='more'>more text</p-more>");
        Document doc = new XMLHandler().load(new StringReader(inputXML));

        assertEquals(engine.findTranslationBlocks(Arrays.asList(XMLTester.getTestIdElement(doc, "inner"),
                XMLTester.getTestIdElement(doc, "outer")
        )), Collections.singletonList(XMLTester.getTestIdElement(doc, "outer")
        ));

        assertEquals(engine.findTranslationBlocks(Arrays.asList(XMLTester.getTestIdElement(doc, "outer"),
                XMLTester.getTestIdElement(doc, "inner")
        )), Collections.singletonList(XMLTester.getTestIdElement(doc, "outer")
        ));

        assertEquals(engine.findTranslationBlocks(Arrays.asList(XMLTester.getTestIdElement(doc, "more"),
                XMLTester.getTestIdElement(doc, "inner")
        )), Arrays.asList(XMLTester.getTestIdElement(doc, "more"),
                XMLTester.getTestIdElement(doc, "inner")
        ));

        assertEquals(engine.findTranslationBlocks(Arrays.asList(XMLTester.getTestIdElement(doc, "more"),
                XMLTester.getTestIdElement(doc, "other")
        )), Arrays.asList(XMLTester.getTestIdElement(doc, "more"),
                XMLTester.getTestIdElement(doc, "inner")
        ));
    }
}

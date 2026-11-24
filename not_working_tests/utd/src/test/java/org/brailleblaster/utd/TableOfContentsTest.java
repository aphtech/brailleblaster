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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.testng.annotations.Test;

import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.Text;
import nu.xom.converters.DOMConverter;
import nu.xom.Builder;

import org.w3c.dom.DOMImplementation;

public class TableOfContentsTest {
    private final String xmlDataPath = "/org/brailleblaster/utd/tableOfContents.xml";
    private final String tableDataPath = "/org/brailleblaster/utd/tableOfContents2.xml";
//	File file = new File("C:/Users/Rezy/Documents/bb-samples/output.xml");
//	File file2 = new File("C:/Users/Rezy/Documents/bb-samples/output2.xml");

    /*
     * Use for textExploreLevel and testAddHeadingInfo
     *
     * <root>
     * 	<level1>
     * 		<h1>
     * 			Text 1<brl /><newpage brlnumber="2" printnumber="5"/><imggroup/>
     * 		</h1>
     * 		<level2>
     * 			<h2>
     * 				Text 2<brl ><newpage brlnumber="4" printnumber="7"/></brl>
     * 			</h2>
     * 		</level2>
     *  </level1>
     * </root>
     */
    public Document documentBuilder() {
        Element root = new Element("root");
        Document sample = new Document(root);

        Element level1 = new Element("level1");
        root.appendChild(level1);

        Element h1 = new Element("h1");
        level1.appendChild(h1);

        // Text
        Text text = new Text("Text 1");
        h1.appendChild(text);
        // Brl
        Element brl = new Element("brl");
        h1.appendChild(brl);
        // New page
        Element newPage = new Element("newpage");
        newPage.addAttribute(new Attribute("brlnumber", "2"));
        newPage.addAttribute(new Attribute("printnumber", "5"));
        h1.appendChild(newPage);
        // imggroup
        Element img = new Element("imggroup");
        h1.appendChild(img);

        Element level2 = new Element("level2");
        level1.appendChild(level2);

        Element h2 = new Element("h2");
        level2.appendChild(h2);

        Text text2 = new Text("Text 2");
        h2.appendChild(text2);
        Element brl2 = new Element("brl");
        Element newPage2 = new Element("newpage");
        newPage2.addAttribute(new Attribute("brlnumber", "4"));
        newPage2.addAttribute(new Attribute("printnumber", "7"));
        brl2.appendChild(newPage2);
        h2.appendChild(brl2);

        return sample;
    }

    @Test(enabled = false, expectedExceptions = IllegalArgumentException.class)
    public void testApplyTOC() throws Exception {
        TableOfContents table = new TableOfContents();
        Builder builder = new Builder();
        InputStream in = null;
        InputStream in2 = null;
        try {
            in = getClass().getResourceAsStream(tableDataPath);
            Document document = builder.build(in);

            table.applyTOC("update", document, null);

            Nodes pageList = document.query("descendant::*[contains(name(), 'lic')]"
                    + "[@class=\"tocpage\"]");

            // Check if updatePrintTOC is called fine
            for (int i = 0; i < pageList.size(); i++) {
                assertTrue(pageList.get(i).getChildCount() > 0);
            }
        } finally {
            if (in != null) {
                in.close();
            }
        }
        try {
            in2 = getClass().getResourceAsStream(xmlDataPath);
            Document document = builder.build(in2);
            table.applyTOC("generate", document, null);

            Nodes tocList = document.query("descendant::*[contains(name(), 'list')]"
                    + "[@class=\"toc\"][@depth=\"1\"]");

            assertEquals(tocList.size(), 2);
            // First list: new and updated TOC
            assertEquals(tocList.get(1).getChildCount(), 2);
            // <li semantics="style,list">
            Element li = (Element) tocList.get(1).getChild(0);
            assertEquals(li.getLocalName(), "li");

            table.applyTOC("translate", document, null);
        } finally {

            if (in2 != null) {
                in2.close();
            }
        }
    }

    @Test(enabled = false)
    public void testUpdatePrintTOC() throws Exception {
        TableOfContents table = new TableOfContents();
        Builder builder = new Builder();
        try (InputStream in = getClass().getResourceAsStream(tableDataPath)) {
            Document inputDocument = builder.build(in);

            table.updatePrintTOC(inputDocument);

            Nodes pageList = inputDocument.query("descendant::*"
                    + "[contains(name(), 'lic')][@class=\"tocpage\"]");

            // Check if exploreList is called fine
            for (int i = 0; i < pageList.size(); i++) {
                assertTrue(pageList.get(i).getChildCount() > 0);
            }

            org.w3c.dom.Document domDocument = DocumentBuilderFactory.
                    newInstance().newDocumentBuilder().newDocument();
            DOMImplementation impl = domDocument.getImplementation();
            domDocument = DOMConverter.convert(inputDocument, impl);

//			TransformerFactory fact = TransformerFactory.newInstance();
//			Transformer trans = fact.newTransformer();
//			trans.setOutputProperty(OutputKeys.INDENT, "yes");
//			trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount","2");
//			DOMSource source = new DOMSource(domDocument);
//			StreamResult result = new StreamResult(file2);
//			trans.transform(source, result);
        }

    }

    @Test(enabled = false)
    public void testExploreList() throws Exception {
        TableOfContents table = new TableOfContents();
        Builder builder = new Builder();
        try (InputStream in = getClass().getResourceAsStream(tableDataPath)) {
            Document document = builder.build(in);

            Element parentList = (Element) document.query("descendant::*[contains(name(), 'list')][@class=\"toc\" and @depth=\"1\"][1]").get(0);
            Nodes li = document.query("child::*[contains(name(), 'li')]");

            List<String> pages = new ArrayList<>();
            // Verify lic pages are empty at this point
            for (int i = 0; i < li.size(); i++) {
                Element tocTitle = (Element) li.get(i).query("child::*[contains(name(), 'lic')][@class=\"toc\"]").get(0);
                Node title = tocTitle.query("child::text()").get(0);
                String str = title.getValue().trim();

                String headText = "descendant::*[contains(name(), 'h" + 1
                        + "')][text()[contains(.,'" + str + "')]]";
                Node heading = document.query(headText).get(0);

                Element newpage = null;
                Nodes newpages = heading.query("descendant::*[contains(name(), 'newpage')][1]");
                if (newpages.size() == 0) {
                    newpage = (Element) heading.query("preceding::newpage[1]").get(0);
                } else {
                    newpage = (Element) newpages.get(0);
                }
                pages.add(newpage.getAttributeValue("brlnumber"));
            }

            table.exploreList(document, parentList, 1);

            Nodes pageList = document.query("descendant::list[@class=\"tocpage\"]");

            for (int i = 0; i < pageList.size(); i++) {
                Node page = pageList.get(i);
                assertEquals(page.getChildCount(), 1);
                //assertEquals(page.getChild(0).toString(), "brl");
                assertEquals(page.getChild(0).getChildCount(), 2);

                Node brlPage = page.getChild(0);
                //assertEquals(brlPage.getChild(0).toString(), "dots");
                assertEquals(brlPage.getChild(1).getValue(), pages.get(i));
            }
        }
    }

    @Test(enabled = false)
    public void testGenerateTOC() throws Exception {
        TableOfContents generate = new TableOfContents();
        Builder builder = new Builder();
        try (InputStream in = getClass().getResourceAsStream(xmlDataPath)) {
            Document inputDocument = builder.build(in);

            generate.generateTOC(inputDocument);

            Nodes tocList = inputDocument.query("descendant::*[contains(name(), 'list')][@class=\"toc\"][@depth=\"1\"]");

            assertEquals(tocList.size(), 2);
            //Second list: old TOC
            assertEquals(tocList.get(0).getChildCount(), 1);
            assertEquals(tocList.get(0).getChild(0).getValue(), "Imagine a TOC here...");
            //<li semantics="style,list">
//			assertEquals(tocList.get(0).getFirstChild().getAttributes().getLength(), 1);


            //First list: new and updated TOC
            assertEquals(tocList.get(1).getChildCount(), 2);

//			org.w3c.dom.Document domDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
//			DOMImplementation impl = domDocument.getImplementation();
//			domDocument = DOMConverter.convert(inputDocument, impl);
//
//			TransformerFactory fact = TransformerFactory.newInstance();
//			Transformer trans = fact.newTransformer();
//			trans.setOutputProperty(OutputKeys.INDENT, "yes");
//			trans.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
//			DOMSource source = new DOMSource(domDocument);
//			StreamResult result = new StreamResult(file);
//			trans.transform(source, result);
        }
    }

    @Test(enabled = false)
    public void testParseFile() throws Exception {
        TableOfContents generate = new TableOfContents();
        Builder builder = new Builder();
        try (InputStream in = getClass().getResourceAsStream(xmlDataPath)) {
            Document inputDocument = builder.build(in);

            Element root = generate.parseXML(inputDocument);

            //Element root = (Element) toc.getChild(0);

            // Check backbone of TOC
            assertEquals(root.getLocalName(), "table");
            assertEquals(root.getChildCount(), 1);
            Element list = (Element) root.getChild(0);
            assertEquals(list.getLocalName(), "list");

            // Check if exploreLevel was called - has a li
            Element li = (Element) list.getChild(0);
            assertEquals(li.getLocalName(), "li");
        }
    }

    /*
     * <root>
     * 	<level1>
     * 		<h1>
     * 			Text<brl/><newpage brlnumber="2" printnumber="5"/><imggroup/>
     * 		</h1>
     * 		<level2>
     * 			<h2>
     * 				Text 2<brl><newpage brlnumber="4" printnumber="7"/></brl>
     * 			</h2>
     * 		</level2>
     * 	</level1>
     *
     * 	<list>
     * 		<li ... >
     * 			<lic ... >Text<brl/></lic>
     * 			<lic ... ></dots>2</lic>
     * 			<list ... >
     * 				<li ... >
     * 					<lic ... >Text 2<brl/></lic>
     * 					<lic ... ><dots/>4</lic>
     * 				</li>
     * 			</list>
     * 		</li>
     * 	</list>
     * </root>
     */
    @Test(enabled = false)
    public void testExploreLevel() {
        Document sample = documentBuilder();
        Element rootElement = sample.getRootElement();

        Element listElement = new Element("list");
        listElement.addAttribute(new Attribute("class", "toc"));

        rootElement.appendChild(listElement);

        TableOfContents generate = new TableOfContents();
        generate.exploreLevel(rootElement, listElement, 1, sample);

        Element li = (Element) listElement.getChild(0);
        assertEquals(li.getLocalName(), "li");
        assertEquals(li.getChildCount(), 3);
        Element list = (Element) li.getChild(2);
        assertEquals(list.getLocalName(), "list");
        assertEquals(list.getChildCount(), 1);
        Element li2 = (Element) list.getChild(0);
        assertEquals(li2.getLocalName(), "li");

        // Test if addHeadingInfo was indeed called correctly
        assertEquals(li.getChild(0).getChildCount(), 2);
        //.assertEquals(li2.getChild(0).toXML(), null);
        assertEquals(li2.getChild(0).getChildCount(), 2);
    }

    /*
     * <root>
     * 	<level1>
     * 		<h1>Text 1<brl/><newpage brlnumber="2" printnumber="5"/><imggroup/></h1>
     * 		<level2>
     * 			<h2>Text 2<brl><newpage brlnumber="4" printnumber="7"/></brl></h2>
     * 		</level2>
     * 	</level1>
     *
     * 	<result>
     * 		<lic ... >Text 1<brl/></lic>
     * 		<lic ... ><dots/>2</lic>
     * 	</result>
     * </root>
     */
    @Test(enabled = false)
    public void testAddHeadingInfo() {
        Document sample = documentBuilder();

        Element root = sample.getRootElement();
        Element appendTo = new Element("result");
        root.appendChild(appendTo);

        Element h1 = (Element) root.getChild(0).getChild(0);
        TableOfContents generate = new TableOfContents();
        generate.addHeadingInfo(h1, appendTo, sample);

        Element lic = (Element) appendTo.getChild(0);

        assertEquals(lic.getLocalName(), "lic");
        assertEquals(lic.getChildCount(), 2);
        assertEquals(lic.getChild(0).getValue(), "Text 1");

        // Test for <newpage>
        Element licPage = appendTo.getChildElements().get(1);

        assertEquals(licPage.getLocalName(), "lic");
        assertEquals(licPage.getChildCount(), 1);

        Element dots = (Element) licPage.getChildElements().get(0).getChild(0);
        Node brl = licPage.getChildElements().get(0).getChild(1);
        assertEquals(dots.getLocalName(), "dots");
        assertEquals(brl.getValue(), "2");
    }
}
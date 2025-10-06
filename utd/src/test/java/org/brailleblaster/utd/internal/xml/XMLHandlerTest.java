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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.InputStream;
import java.io.StringReader;
import java.util.*;

import org.brailleblaster.utd.testutils.XMLTester;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.Text;

public class XMLHandlerTest {

	@Test
	public void findCommonParentTest() {
		String inputXML = XMLTester.generateBookString("",
				"<p-outer testid='outer'>outer text"
				+ "<p-inner testid='inner'>inner <b testid='bold'>text</b></p-inner>"
				+ "<p-inner2 testid='inner2'>inner2 <b testid='bold2'>text</b></p-inner2>"
				+ "</p-outer>"
				+ "<p-more testid='more'>more text</p-more>");
		Document doc = new XMLHandler().load(new StringReader(inputXML));

		//Single element handling
		Assert.assertEquals(XMLHandler.Companion.findCommonParent(Collections.singletonList(
                XMLTester.getTestIdElement(doc, "inner")
        )), XMLTester.getTestIdElement(doc, "inner"));

		//Nested
		Assert.assertEquals(XMLHandler.Companion.findCommonParent(Arrays.asList(
				XMLTester.getTestIdElement(doc, "inner"),
				XMLTester.getTestIdElement(doc, "bold")
		)), XMLTester.getTestIdElement(doc, "inner"));

		Assert.assertEquals(XMLHandler.Companion.findCommonParent(Arrays.asList(
				XMLTester.getTestIdElement(doc, "inner"),
				XMLTester.getTestIdElement(doc, "outer")
		)), XMLTester.getTestIdElement(doc, "outer"));

		Assert.assertEquals(XMLHandler.Companion.findCommonParent(Arrays.asList(
				XMLTester.getTestIdElement(doc, "outer"),
				XMLTester.getTestIdElement(doc, "bold2")
		)), XMLTester.getTestIdElement(doc, "outer"));

		//Other
		Assert.assertEquals(XMLHandler.Companion.findCommonParent(Arrays.asList(
				XMLTester.getTestIdElement(doc, "inner"),
				XMLTester.getTestIdElement(doc, "inner2"),
				XMLTester.getTestIdElement(doc, "more")
		)), XMLTester.getTestIdElement(doc, "contentRoot"));

		Assert.assertEquals(XMLHandler.Companion.findCommonParent(Arrays.asList(
				XMLTester.getTestIdElement(doc, "inner"),
				XMLTester.getTestIdElement(doc, "inner2")
		)), XMLTester.getTestIdElement(doc, "outer"));

		Assert.assertEquals(XMLHandler.Companion.findCommonParent(Arrays.asList(
				XMLTester.getTestIdElement(doc, "bold"),
				XMLTester.getTestIdElement(doc, "bold2")
		)), XMLTester.getTestIdElement(doc, "outer"));

		Assert.assertEquals(XMLHandler.Companion.findCommonParent(Arrays.asList(
				XMLTester.getTestIdElement(doc, "bold"),
				XMLTester.getTestIdElement(doc, "bold2"),
				XMLTester.getTestIdElement(doc, "more")
		)), XMLTester.getTestIdElement(doc, "contentRoot"));
	}

	@Test
	public void followingSiblingTest() {
		Document doc = XMLTester.generateBookDoc("", "<p testid='p1'>test<b testid='b'>bold</b></p>"
				+ "outside"
				+ "<p testid='p2'>testing</p>");

		List<Node> output = new ArrayList<>();
		XMLHandler.Companion.followingVisitor(XMLTester.getTestIdElement(doc, "p1"), (Node curNode) -> {
			output.add(curNode);
			return false;
		});

		List<Node> expectedList = Lists.newArrayList(
				XMLTester.getTestIdElement(doc, "contentRoot").getChild(1),
				XMLTester.getTestIdElement(doc, "p2"),
				XMLTester.getTestIdElement(doc, "p2").getChild(0)
		);
		assertEquals(output, expectedList, "Failed!"
				+ System.lineSeparator() + "Origional list: " + output
				+ System.lineSeparator() + "New list:       " + expectedList);
	}

	@Test
	public void followingRecursive() {
		Document doc = XMLTester.generateBookDoc("", "<p testid='p1'>test<b testid='b'>bold</b></p>"
				+ "outside"
				+ "<p testid='p2'>testing</p>");

		List<Node> output = new ArrayList<>();
		XMLHandler.Companion.followingVisitor(XMLTester.getTestIdElement(doc, "head"), (Node curNode) -> {
			output.add(curNode);
			return false;
		});

		List<Node> expectedList = Lists.newArrayList(
				XMLTester.getTestIdElement(doc, "book"),
				XMLTester.getTestIdElement(doc, "contentRoot"),
				XMLTester.getTestIdElement(doc, "p1"),
				XMLTester.getTestIdElement(doc, "p1").getChild(0),
				XMLTester.getTestIdElement(doc, "b"),
				XMLTester.getTestIdElement(doc, "b").getChild(0),
				XMLTester.getTestIdElement(doc, "contentRoot").getChild(1),
				XMLTester.getTestIdElement(doc, "p2"),
				XMLTester.getTestIdElement(doc, "p2").getChild(0)
		);
		assertEquals(output, expectedList, "Failed!"
				+ System.lineSeparator() + "Origional list: " + output
				+ System.lineSeparator() + "New list:       " + expectedList);
	}
	@Test
	public void loadStreamTest() {
		Document doc = null;
        try (InputStream in = getClass().getResourceAsStream("/org/brailleblaster/utd/internal/xml/whitespaceLoadTest.xml")) {
            doc = new XMLHandler().load(in);
        } catch (Exception e) {
            fail("Problem loading resource", e);
        }
		// Text in paragraphs should be maintained
		Nodes results = Objects.requireNonNull(doc).query("//*[@id='para1']");
		assertEquals(results.size(), 1);
		Element e = (Element)results.get(0);
		assertEquals(e.getChildCount(), 1);
		assertEquals(e.getChild(0).getClass(), Text.class);
		assertEquals(e.getChild(0).getValue(), "Some text");
		
		// Blank space inside paragraphs with no further blocks are maintained.
		results = doc.query("//*[@id='para2']");
		assertEquals(results.size(), 1);
		e = (Element)results.get(0);
		assertEquals(e.getChildCount(), 1);
		assertEquals(e.getChild(0).getClass(), Text.class);
		assertEquals(e.getChild(0).getValue(), " ");
		
		// Blanks at the same level as blocks are stripped even though inside a paragraph.
		results = doc.query("//*[@id='para3']");
		assertEquals(results.size(), 1);
		e = (Element)results.get(0);
		assertEquals(e.getChildCount(), 1);
		assertEquals(e.getChild(0).getClass(), Element.class);
		
		// Texts in a list are stripped.
		results = doc.query("//*[@id='list1']");
		assertEquals(results.size(), 1);
		e = (Element)results.get(0);
		for (int i = 0; i < e.getChildCount(); i++) {
			assertEquals(e.getChild(i).getClass(), Element.class);
		}
		
		// Text in a li is maintained.
		results = doc.query("//*[@id='li1']");
		assertEquals(results.size(), 1);
		e = (Element)results.get(0);
		assertEquals(e.getChildCount(), 1);
		assertEquals(e.getChild(0).getClass(), Text.class);
		assertEquals(e.getChild(0).getValue(), "List item");
		
		// A blank in a li with no further blocks is maintained.
		results = doc.query("//*[@id='li2']");
		assertEquals(results.size(), 1);
		e = (Element)results.get(0);
		assertEquals(e.getChildCount(), 1);
		assertEquals(e.getChild(0).getClass(), Text.class);
		assertEquals(e.getChild(0).getValue(), " ");
		
		// A blank in a list item where a nested list exists should be stripped.
		results = doc.query("//*[@id='li3']");
		assertEquals(results.size(), 1);
		e = (Element)results.get(0);
		assertEquals(e.getChildCount(), 1);
		assertEquals(e.getChild(0).getClass(), Element.class);
		
		// Blank in a list item not adjacent to a nested list should not be stripped.
		results = doc.query("//*[@id='li4']");
		assertEquals(results.size(), 1);
		e = (Element)results.get(0);
		assertEquals(e.getChildCount(), 4);
		assertEquals(e.getChild(0).getClass(), Element.class);
		assertEquals(e.getChild(1).getClass(), Text.class);
		assertEquals(e.getChild(1).getValue(), " ");
		assertEquals(e.getChild(2).getClass(), Element.class);
		assertEquals(e.getChild(3).getClass(), Element.class);
	}
}

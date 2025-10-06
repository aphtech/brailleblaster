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
package org.brailleblaster.bbx.fixer;

import java.util.List;

import com.google.common.collect.Lists;
import org.brailleblaster.bbx.fixers.NodeTreeSplitter;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.brailleblaster.testrunners.XMLElementAssert;
import org.testng.annotations.Test;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Text;
import org.brailleblaster.utd.exceptions.NodeException;
import org.brailleblaster.utd.internal.xml.FastXPath;

public class NodeTreeSplitterTest {
	@Test
	public void unwrapChild() {
		Document doc = TestXMLUtils.generateBookDoc("", 
				"<p testid='p'>"
				+ "before"
				+ "<br testid='br'/>"
				+ "after"
				+ "</p>");
		Element root = TestXMLUtils.getBookRoot(doc);
		Element p = TestXMLUtils.getTestIdElement(doc, "p");
		Element br = TestXMLUtils.getTestIdElement(doc, "br");
		
		Text beforeText = (Text) p.getChild(0);
		Text afterText = (Text) p.getChild(2);

		testNodeTreeSplit(p, br);

		new XMLElementAssert(root, null)
				.nextChildIs(childAssert -> childAssert
						.hasName("p")
						.hasText("before")
						.elementEquals(p)
						.childEquals(0, beforeText)
				).nextChildIs(childAssert -> childAssert
						.hasName("br")
						.childCount(0)
						.elementEquals(br)
				).nextChildIs(childAssert -> childAssert
						.hasName("p")
						.hasText("after")
						.childEquals(0, afterText)
				).noNextChild();
	}

	@Test
	public void unwrapGrandChild() {
		Document doc = TestXMLUtils.generateBookDoc("", 
				"<p testid='p'>"
				+ "before"
				+ "<span>spanbefore"
				+ "<br testid='br'/>"
				+ "spanafter</span>"
				+ "after"
				+ "</p>");
		Element root = TestXMLUtils.getBookRoot(doc);
		Element p = TestXMLUtils.getTestIdElement(doc, "p");
		Element br = TestXMLUtils.getTestIdElement(doc, "br");

		Text beforeText = (Text) p.getChild(0);
		Element span = (Element) p.getChild(1);
		Text beforeSpanText = (Text) span.getChild(0);
		Text afterSpanText = (Text) span.getChild(2);
		Text afterText = (Text) p.getChild(2);
		
		testNodeTreeSplit(p, br);

		new XMLElementAssert(root, null)
				.nextChildIs(childAssert -> childAssert
						.nextChildIsText("before")
						.nextChildIs(childAssert2 -> childAssert2
								.hasName("span")
								.hasText("spanbefore")
								.elementEquals(span)
								.childEquals(0, beforeSpanText)
						).noNextChild()
						.elementEquals(p)
						.childEquals(0, beforeText)
				).nextChildIs(childAssert -> childAssert
						.hasName("br")
						.childCount(0)
						.elementEquals(br)
				).nextChildIs(childAssert -> childAssert
						.hasName("p")
						.nextChildIs(childAssert2 -> childAssert2
								.hasName("span")
								.hasText("spanafter")
								.childEquals(0, afterSpanText)
						).nextChildIsText("after")
						.childEquals(1, afterText)
				).noNextChild();
	}
	
	@Test
	public void unwrapGrandChildAndRootChildren() {
		Document doc = TestXMLUtils.generateBookDoc("", 
				"<p testid='p'>"
				+ "before"
				+ "<span1>more<spanNest>nested</spanNest>aftermore</span1>"
				+ "<span2>spanbefore<br testid='br'/>spanafter</span2>"
				+ "after"
				+ "<span3>evenmore</span3>"
				+ "</p>");
		Element root = TestXMLUtils.getBookRoot(doc);
		Element p = TestXMLUtils.getTestIdElement(doc, "p");
		Element br = TestXMLUtils.getTestIdElement(doc, "br");

		testNodeTreeSplit(p, br);

		new XMLElementAssert(root, null)
				.nextChildIs(childAssert -> childAssert
						.hasName("p")
						.nextChildIsText("before")
						.nextChildIs(childAssert2 -> childAssert2
								.hasName("span1")
								.hasText("more")
						).nextChildIs(childAssert2 -> childAssert2
								.hasName("span2")
								.hasText("spanbefore")
						).noNextChild()
				).nextChildIs(childAssert -> childAssert
						.hasName("br")
						.childCount(0)
				).nextChildIs(childAssert -> childAssert
						.hasName("p")
						.nextChildIs(childAssert2 -> childAssert2
								.hasName("span2")
								.hasText("spanafter")
						).nextChildIsText("after")
						.nextChildIs(childAssert2 -> childAssert2
								.hasName("span3")
								.hasText("evenmore")
						).noNextChild()
				).noNextChild();
	}
	
	@Test
	public void unwrapNestedAfter() {
		Document doc = TestXMLUtils.generateBookDoc("", 
				"<p testid='p'>"
				+ "before"
				+ "<br testid='br'/>"
				+ "<span2>makeMe</span2>"
				+ "</p>");
		Element root = TestXMLUtils.getBookRoot(doc);
		Element p = TestXMLUtils.getTestIdElement(doc, "p");
		Element br = TestXMLUtils.getTestIdElement(doc, "br");
		
		Text beforeText = (Text) p.getChild(0);
		Text spanText = (Text) p.getChild(2).getChild(0);
		
		testNodeTreeSplit(p, br);
		
		new XMLElementAssert(root, null)
				.nextChildIs(pAssert -> pAssert
					.hasName("p")
					.hasText("before")
					.elementEquals(p)
					.childEquals(0, beforeText)
				).nextChildIs(brAssert -> brAssert
						.hasName("br")
						.childCount(0)
						.elementEquals(br)
				).nextChildIs(pAssert -> pAssert
					.hasName("p")
					.nextChildIs(span2Assert -> span2Assert
						.hasName("span2")
						.childEquals(0, spanText)
					).noNextChild()
				).noNextChild();
	}
	
	@Test
	public void unwrapTargetAtStart() {
		Document doc = TestXMLUtils.generateBookDoc("", 
				"<p testid='p'><span testid='target'/>this is</p>");
		Element root = TestXMLUtils.getBookRoot(doc);
		Element p = TestXMLUtils.getTestIdElement(doc, "p");
		Element span = TestXMLUtils.getTestIdElement(doc, "target");
		
		Text text = (Text) p.getChild(1);

		testNodeTreeSplit(p, span);
		
		new XMLElementAssert(root, null)
				.nextChildIs(childAssert -> childAssert
						.hasName("span")
						.childCount(0)
						.elementEquals(span)
				).nextChildIs(childAssert -> childAssert
						.hasName("p")
						.nextChildIsText("this is")
						.noNextChild()
						.elementEquals(p)
						.childEquals(0, text)
				).noNextChild();
	}
	
	@Test
	public void unwrapTargetAtEnd() {
		Document doc = TestXMLUtils.generateBookDoc("", 
				"<p testid='p'>this is<span testid='target'/></p>");
		Element root = TestXMLUtils.getBookRoot(doc);
		Element p = TestXMLUtils.getTestIdElement(doc, "p");
		Element br = TestXMLUtils.getTestIdElement(doc, "target");

		testNodeTreeSplit(p, br);
		
		new XMLElementAssert(root, null)
				.nextChildIs(childAssert -> childAssert
						.hasName("p")
						.nextChildIsText("this is")
						.noNextChild()
				).nextChildIs(childAssert -> childAssert
						.hasName("span")
						.childCount(0)
				).noNextChild();
	}
	
	@Test
	public void unwrapTargetOnlyChild() {
		Document doc = TestXMLUtils.generateBookDoc("", 
				"<p testid='p'><span testid='target'/></p>");
		Element root = TestXMLUtils.getBookRoot(doc);
		Element p = TestXMLUtils.getTestIdElement(doc, "p");
		Element br = TestXMLUtils.getTestIdElement(doc, "target");

		// do not use testNodeTreeSplit as the paragraph should be removed
		NodeTreeSplitter.split(p, br);
		
		new XMLElementAssert(root, null)
				.nextChildIs(childAssert -> childAssert
						.hasName("span")
						.childCount(0)
				).noNextChild();
	}
	
	@Test
	public void unwrapSplitAfterWithMultipleElements() {
		Document doc = TestXMLUtils.generateBookDoc("", 
				"<p testid='p'>"
				+ "<spanTarget testid='target'>text1</spanTarget>"
				+ "<span2>test2</span2>"
				+ "text3"
				+ "</p>"
		);
		Element root = TestXMLUtils.getBookRoot(doc);
		Element p = TestXMLUtils.getTestIdElement(doc, "p");
		Element spanTarget = TestXMLUtils.getTestIdElement(doc, "target");

		testNodeTreeSplit(p, spanTarget);
		
		new XMLElementAssert(root, null)
				.nextChildIs(childAssert -> childAssert
						.hasName("spanTarget")
						.elementEquals(spanTarget)
						.nextChildIsText("text1")
						.noNextChild()
				).nextChildIs(childAssert -> childAssert
						.hasName("p")
						.elementEquals(p)
						.nextChildIs(spanAssert -> spanAssert
								.hasName("span2")
								.nextChildIsText("test2")
						).nextChildIsText("text3")
						.noNextChild()
				).noNextChild();
	}
	
	@Test
	public void unwrapLastContainerInsideContainer() {
		Document doc = TestXMLUtils.generateBookDoc("", 
				"<root testid='splitRoot'>"
				+ "<container>"
				+ "nested 1"
				+ "<nestedContainer testid='splitAt'>Text something</nestedContainer>"
				+ "</container>"
				+ "<p>after</p>"
				+ "</root>"
		);
		Element root = TestXMLUtils.getBookRoot(doc);
		Element splitRoot = TestXMLUtils.getTestIdElement(doc, "splitRoot");
		Element splitAt = TestXMLUtils.getTestIdElement(doc, "splitAt");

		testNodeTreeSplit(splitRoot, splitAt);
		
		new XMLElementAssert(root, null)
				.nextChildIs(beforeAssert -> beforeAssert
						.hasName("root")
						.nextChildIs(containerAssert -> containerAssert
								.hasName("container")
								.nextChildIsText("nested 1")
						).noNextChild()
				).nextChildIs(nestedAssert -> nestedAssert
						.hasName("nestedContainer")
						.nextChildIsText("Text something")
				).nextChildIs(afterAssert -> afterAssert
						.hasName("root")
						.nextChildIs(containerAssert -> containerAssert
								.hasName("p")
								.nextChildIsText("after")
						).noNextChild()
				).noNextChild();
	}
	
	@Test
	public void unwrapLastContainerInsideContainer_Other() {
		Document doc = TestXMLUtils.generateBookDoc("", 
				"<root testid='splitRoot'>"
				+ "<container>"
				+ "nested 1"
				+ "<another><nestedContainer testid='splitAt'>Text something</nestedContainer></another>"
				+ "</container>"
				+ "<p>after</p>"
				+ "</root>"
		);
		Element root = TestXMLUtils.getBookRoot(doc);
		Element splitRoot = TestXMLUtils.getTestIdElement(doc, "splitRoot");
		Element splitAt = TestXMLUtils.getTestIdElement(doc, "splitAt");

		testNodeTreeSplit(splitRoot, splitAt);
		
		new XMLElementAssert(root, null)
				.nextChildIs(beforeAssert -> beforeAssert
						.hasName("root")
						.nextChildIs(containerAssert -> containerAssert
								.hasName("container")
								.nextChildIsText("nested 1")
						).noNextChild()
				).nextChildIs(nestedAssert -> nestedAssert
						.hasName("nestedContainer")
						.nextChildIsText("Text something")
				).nextChildIs(afterAssert -> afterAssert
						.hasName("root")
						.nextChildIs(containerAssert -> containerAssert
								.hasName("p")
								.nextChildIsText("after")
						).noNextChild()
				).noNextChild();
	}
	
	@Test
	public void unwrapOnlyContainerInsideContainer() {
		Document doc = TestXMLUtils.generateBookDoc("", 
				"<p testid='splitRoot'><span>before<br testid='splitAt'/>after</span></p>"
		);
		
		Element root = TestXMLUtils.getBookRoot(doc);
		Element splitRoot = TestXMLUtils.getTestIdElement(doc, "splitRoot");
		Element splitAt = TestXMLUtils.getTestIdElement(doc, "splitAt");
		
		testNodeTreeSplit(splitRoot, splitAt);
		
		new XMLElementAssert(root, null)
				.nextChildIs(p -> p
						.hasName("p")
						.nextChildIs(span -> span
								.hasName("span")
								.nextChildIsText("before")
								.noNextChild()
						).noNextChild()
				).nextChildIs(br -> br
						.hasName("br")
						.childCount(0)
				).nextChildIs(p -> p
						.hasName("p")
						.nextChildIs(span -> span
								.hasName("span")
								.nextChildIsText("after")
								.noNextChild()
						).noNextChild()
				).noNextChild();
	}
		
	
	//TODO: Should this be implemented? what about paragraphs inside imggroups 
//	@Test
//	public void unwrapTargetOnlyChildNested() {
//		Document doc = TestXMLUtils.generateBookDoc("", 
//				"<p testid='p'><span><inline testid='target'/></span></p>");
//		Element root = TestXMLUtils.getBookRoot(doc);
//		Element p = TestXMLUtils.getTestIdElement(doc, "p");
//		Element br = TestXMLUtils.getTestIdElement(doc, "target");
//
//		NodeTreeSplitter.split(p, br);
//
//		new XMLElementAssert(root, null)
//				.nextChildIs(childAssert -> childAssert
//						.hasName("inline")
//						.childCount(0)
//				).noNextChild();
//	}
	
	private static void testNodeTreeSplit(Element root, Node splitAt) {
		List<Node> allNodes = Lists.newArrayList(FastXPath.descendant(root.getDocument()));
		
		NodeTreeSplitter.split(root, splitAt);
		
		for (Node origNode : allNodes) {
			if (origNode.getDocument() == null) {
				throw new NodeException("node is not attached to document: " + origNode.toXML() + " | root: ", root);
			}
		}
	}
	
	public void assertHasDocument(Node node) {
		if (node.getDocument() == null) {
			throw new AssertionError("node has no document " + node.toXML());
		}
	}
}

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

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Text;

public class FastXPathTest {
	private static final Logger log = LoggerFactory.getLogger(FastXPathTest.class);

	@Test
	public void descendantTest() {
		Document doc = new XMLHandler().load(new File("src/test/resources/org/brailleblaster/utd/config/nimas.xml"));

		XMLHandler.Companion.childrenRecursiveNodeVisitor(doc.getRootElement(), node -> {
			//Test both node in doc and node without doc and parent
			for (Node curNode : Arrays.asList(node, node.copy())) {
				List<Node> fastResult = Lists.newArrayList(FastXPath.descendant(curNode));
				List<Node> xomResult = Lists.newArrayList(XMLHandler.query(curNode, "descendant::node()"));
				assertListEquals(fastResult, xomResult);
			}
			return false;
		});
	}

	@Test
	public void descendantOrSelfTest() {
		Document doc = new XMLHandler().load(new File("src/test/resources/org/brailleblaster/utd/config/nimas.xml"));

		XMLHandler.Companion.childrenRecursiveNodeVisitor(doc.getRootElement(), node -> {
			//Test both node in doc and node without doc and parent
			for (Node curNode : Arrays.asList(node, node.copy())) {
				List<Node> fastResult = Lists.newArrayList(FastXPath.descendantOrSelf(curNode));
				List<Node> xomResult = Lists.newArrayList(XMLHandler.query(curNode, "descendant-or-self::node()"));
				assertListEquals(fastResult, xomResult);
			}
			return false;
		});
	}

	@Test
	public void followingTest() {
		Document doc = new XMLHandler().load(new File("src/test/resources/org/brailleblaster/utd/config/nimas.xml"));

		XMLHandler.Companion.childrenRecursiveNodeVisitor(doc.getRootElement(), node -> {
			//Test both node in doc and node without doc and parent
			List<Node> testNodes = new ArrayList<>();
			testNodes.add(node);
			if (node.getChildCount() > 0) {
				Node copiedNode = node.copy();
				testNodes.add(copiedNode.getChild(0));
			}

			for (Node curNode : testNodes) {
				log.debug("attached " + (curNode.getParent() != null) + " curNode " + curNode);
				List<Node> fastResult = Lists.newArrayList(FastXPath.following(curNode));
				List<Node> xomResult = Lists.newArrayList(XMLHandler.query(curNode, "following::node()"));
				assertListEquals(fastResult, xomResult);
			}
			return false;
		});
	}

	@Test
	public void ancestorTeset() {
		Document doc = new XMLHandler().load(new StringReader("<root><p>text</p></root>"));

		Element p = (Element) doc.getRootElement().getChild(1);
		Assert.assertEquals(p.getLocalName(), "p");
		Text text = (Text) p.getChild(0);
		
		StreamSupport.stream(FastXPath.ancestor(text).spliterator(), false)
				.filter(something -> something.getAttributeCount() == 1);

		Iterator<? extends Element> iterator = FastXPath.ancestor(text).iterator();
		Assert.assertEquals(iterator.next(), p);
		Assert.assertEquals(iterator.next(), doc.getRootElement());
	}

	@Test
	public void ancestorOrSelfTest() {
		Document doc = new XMLHandler().load(new StringReader("<root><p>text</p></root>"));

		Element p = (Element) doc.getRootElement().getChild(1);
		Assert.assertEquals(p.getLocalName(), "p");
		Text text = (Text) p.getChild(0);

		Iterator<Node> iterator = FastXPath.ancestorOrSelf(text).iterator();
		Assert.assertEquals(iterator.next(), text);
		Assert.assertEquals(iterator.next(), p);
		Assert.assertEquals(iterator.next(), doc.getRootElement());
	}

	private static void assertListEquals(List<Node> fastResult, List<Node> xomResult) {
		for (int i = 0; i < Math.max(fastResult.size(), xomResult.size()); i++) {
			String fastEntry = (i < fastResult.size()) ? fastResult.get(i).toString() : "[is null]";
			String xomEntry = (i < xomResult.size()) ? xomResult.get(i).toString() : "[is null]";

			Assert.assertTrue(i < fastResult.size(),
					"fastResult ends before " + i + " but xom has " + xomEntry
					+ dumpLists(fastResult, xomResult, i)
			);
			Assert.assertTrue(i < xomResult.size(),
					"xomResult ends before " + i + " but fast has " + fastEntry
					+ dumpLists(fastResult, xomResult, i)
			);
			Assert.assertEquals(fastResult.get(i), xomResult.get(i),
					"Not equal at " + i
					+ dumpLists(fastResult, xomResult, i)
			);
		}
	}

	private static String dumpLists(List<Node> fastResult, List<Node> xomResult, int i) {
		return System.lineSeparator()
				+ "--- Fast ----"
				+ System.lineSeparator()
				+ fastResult.stream().map(fancyMap(i)).collect(Collectors.joining())
				+ System.lineSeparator()
				+ "--- Xom ----"
				+ System.lineSeparator()
				+ xomResult.stream().map(fancyMap(i)).collect(Collectors.joining());
	}

	private static Function<Node, String> fancyMap(int i) {
		return new Function<>() {
            int counter = 0;

            @Override
            public String apply(Node t) {
                String result = t.toString();
                if (counter++ == i) {
                    result += "<--------";
                }
                return result + System.lineSeparator();
            }
        };
	}
}

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
package org.brailleblaster.perspectives.mvc;


import java.io.StringReader;
import java.util.List;

import com.google.common.collect.Iterables;
import org.apache.commons.lang3.tuple.Pair;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.brailleblaster.utd.internal.xml.XMLHandler;
import org.brailleblaster.util.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.google.common.base.Objects;

import nu.xom.Document;
import nu.xom.Node;
import nu.xom.ParentNode;

public class XMLSelectionTest {
    private static final Logger log = LoggerFactory.getLogger(XMLSelectionTest.class);

    // blocks_sectionSelection_Test opens a BBTestRunner, see XMLSelectionGuiTest

    private static final Document doc = new XMLHandler().load(new StringReader(
            "<root>"
                    + "<p>heading</p>"
                    + "<list>"
                    + "<li>first</li>"
                    + "<li>second</li>"
                    + "</list>"
                    + "<p>middle</p>"
                    + "<list>"
                    + "<li>third<em>italic</em></li>"
                    + "<li>forth<b>bold</b></li>"
                    + "</list>"
                    + "<p>last</p>"
                    + "</root>"
    ));

    @Test
    public void validTree_siblingsAdjacent() {
        XMLSelection selection = new XMLSelection(
                new XMLNodeCaret(TestXMLUtils.getByTextNode(doc, "heading").getParent()),
                new XMLNodeCaret(TestXMLUtils.getElementByOccurance(doc, "list", 0))
        );
        assertPairEquals(
                selection.isValidTreeSelection(),
                Pair.of(
                        TestXMLUtils.getByTextNode(doc, "heading").getParent(),
                        TestXMLUtils.getElementByOccurance(doc, "list", 0)
                )
        );
    }

    @Test
    public void validTree_siblingsNonAdjacent() {
        XMLSelection selection = new XMLSelection(
                new XMLNodeCaret(TestXMLUtils.getByTextNode(doc, "heading").getParent()),
                new XMLNodeCaret(TestXMLUtils.getByTextNode(doc, "middle").getParent())
        );
        assertPairEquals(
                selection.isValidTreeSelection(),
                Pair.of(
                        TestXMLUtils.getByTextNode(doc, "heading").getParent(),
                        TestXMLUtils.getByTextNode(doc, "middle").getParent()
                )
        );
    }

    @Test
    public void validTree_nestedSimple_Last() {
        XMLSelection selection = new XMLSelection(
                new XMLNodeCaret(TestXMLUtils.getByTextNode(doc, "heading").getParent()),
                new XMLNodeCaret(TestXMLUtils.getByTextNode(doc, "second").getParent())
        );
        assertPairEquals(
                selection.isValidTreeSelection(),
                Pair.of(
                        TestXMLUtils.getByTextNode(doc, "heading").getParent(),
                        TestXMLUtils.getElementByOccurance(doc, "list", 0)
                )
        );
    }

    @Test
    public void validTree_nestedSimple_FirstInvalid() {
        XMLSelection selection = new XMLSelection(
                new XMLNodeCaret(TestXMLUtils.getByTextNode(doc, "heading").getParent()),
                new XMLNodeCaret(TestXMLUtils.getByTextNode(doc, "first").getParent())
        );
        assertPairEquals(
                selection.isValidTreeSelection(),
                null
        );
    }

    @Test
    public void validTree_nestedComplex_LastInList() {
        XMLSelection selection = new XMLSelection(
                new XMLNodeCaret(TestXMLUtils.getByTextNode(doc, "heading").getParent()),
                new XMLNodeCaret(TestXMLUtils.getByTextNode(doc, "forth").getParent())
        );
        assertPairEquals(
                selection.isValidTreeSelection(),
                Pair.of(
                        TestXMLUtils.getByTextNode(doc, "heading").getParent(),
                        TestXMLUtils.getElementByOccurance(doc, "list", 1)
                )
        );
    }

    @Test
    public void validTree_nestedComplex_LastInNested() {
        XMLSelection selection = new XMLSelection(
                new XMLNodeCaret(TestXMLUtils.getByTextNode(doc, "heading").getParent()),
                new XMLNodeCaret(TestXMLUtils.getByTextNode(doc, "bold").getParent())
        );
        assertPairEquals(
                selection.isValidTreeSelection(),
                Pair.of(
                        TestXMLUtils.getByTextNode(doc, "heading").getParent(),
                        TestXMLUtils.getElementByOccurance(doc, "list", 1)
                )
        );
    }

    @Test
    public void validTree_nestedComplex_LastInNestedInvalid() {
        XMLSelection selection = new XMLSelection(
                new XMLNodeCaret(TestXMLUtils.getByTextNode(doc, "heading").getParent()),
                new XMLNodeCaret(TestXMLUtils.getByTextNode(doc, "italic").getParent())
        );
        assertPairEquals(
                selection.isValidTreeSelection(),
                null
        );
    }

    @Test
    public void validTree_nestedComplex_FirstInNestedInvalid() {
        XMLSelection selection = new XMLSelection(
                new XMLNodeCaret(TestXMLUtils.getByTextNode(doc, "heading").getParent()),
                new XMLNodeCaret(TestXMLUtils.getByTextNode(doc, "italic").getParent())
        );
        assertPairEquals(
                selection.isValidTreeSelection(),
                null
        );
    }

    private static void assertPairEquals(List<Node> actual, Pair<ParentNode, ParentNode> expected) {
        if (actual == expected
                || (actual != null
                && expected != null
                && Objects.equal(actual.get(0), expected.getLeft())
                && Objects.equal(Iterables.getLast(actual), expected.getRight()))) {
            return;
        }
        throw new AssertionError(Utils.formatMessage(
                "Expected {} and {} but found {} and {}",
                expected == null ? null : expected.getLeft().toXML(),
                expected == null ? null : expected.getRight().toXML(),
                actual == null ? null : actual.get(0).toXML(),
                actual == null ? null : Iterables.getLast(actual).toXML()
        ));
    }
}

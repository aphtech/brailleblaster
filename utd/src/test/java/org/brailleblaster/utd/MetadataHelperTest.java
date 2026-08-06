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

import nu.xom.Element;
import nu.xom.Document;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

public class MetadataHelperTest {

    public Document nodeBuilder() {
        Element root = new Element("dtbook");
        Document doc = new Document(root);

        Element head = new Element("head");

        Element frontmatter = new Element("frontmatter");
        Element p = new Element("p");
        Element strong = new Element("strong");
        p.appendChild(strong);
        frontmatter.appendChild(p);

        root.appendChild(head);
        root.appendChild(frontmatter);

        return doc;
    }

    @Test(enabled = false)
    public void addMetaTest() {
        Document doc = nodeBuilder();
        MetadataHelper.INSTANCE.changeBraillePageNumber(doc, "#a", "#b", null, false, true);
        try {
            Element head = (Element) doc.getChild(0).getChild(0);
            Element meta = (Element) head.getChild(0);
            assertEquals(meta.getAttributeValue("original"), "#a", "Attribute \"original\" not found in element: " + meta.toXML());
            assertEquals(meta.getAttributeValue("type"), "braillePage", "Attribute \"type\" not found in element: " + meta.toXML());
        } catch (ClassCastException | IndexOutOfBoundsException e) {
            fail("Malformed XML: " + doc.toXML(), e);
        }
    }

    @Test(enabled = false)
    public void testFindPageChange() {
        Document doc = nodeBuilder();
        assertNull(MetadataHelper.INSTANCE.findPrintPageChange(doc.getDocument(), "1"));
        MetadataHelper.INSTANCE.changePrintPageNumber(doc, "1", "2", null, false);
        MetadataHelper.INSTANCE.changeBraillePageNumber(doc, "a", "b", null, false, true);
        try {
            Element head = (Element) doc.getChild(0).getChild(0);
            Element meta1 = (Element) head.getChild(0);
            Element meta2 = (Element) head.getChild(1);
            Element findMeta1 = MetadataHelper.INSTANCE.findPrintPageChange(doc.getDocument(), "1");
            Element findMeta2 = MetadataHelper.INSTANCE.findBraillePageChange(doc.getDocument(), "a");
            assertNotNull(findMeta1, "Meta tag for Print Page Change not found in document: " + doc.toXML());
            assertNotNull(findMeta2, "Meta tag for Braille Page Change not found in document: " + doc.toXML());
            assertEquals(meta1.toXML(), findMeta1.toXML(), "Meta tags not equal: " + meta1.toXML() + " || " + findMeta1.toXML());
            assertEquals(meta2.toXML(), findMeta2.toXML(), "Meta tags not equal: " + meta2.toXML() + " || " + findMeta2.toXML());
        } catch (ClassCastException | IndexOutOfBoundsException e) {
            fail("Malformed XML: " + doc.toXML(), e);
        }
    }

    @Test(enabled = false)
    public void testAdaptPageChangeWithNew() {
        Document doc = nodeBuilder();
        assertNull(MetadataHelper.INSTANCE.findPrintPageChange(doc.getDocument(), "1"));
        MetadataHelper.INSTANCE.changePrintPageNumber(doc, "1", "2", null, false);
        MetadataHelper.INSTANCE.changePrintPageNumber(doc, "2", "3", null, false);
        try {
            Element findMeta1 = MetadataHelper.INSTANCE.findPrintPageChange(doc.getDocument(), "1");
            assertNotNull(findMeta1, "Meta tag for Print Page Change not found in document: " + doc.toXML());
            assertEquals(findMeta1.getAttributeValue("new"), "3");
        } catch (ClassCastException | IndexOutOfBoundsException e) {
            fail("Malformed XML: " + doc.toXML(), e);
        }
    }

    @Test(enabled = false)
    public void testAdaptPageChangeWithBlank() {
        Document doc = nodeBuilder();
        assertNull(MetadataHelper.INSTANCE.findPrintPageChange(doc.getDocument(), "1"));
        MetadataHelper.INSTANCE.changePrintPageNumber(doc, "1", "2", null, false);
        MetadataHelper.INSTANCE.markBlankPrintPageNumber(doc, "2", null, false);
        try {
            Element findMeta1 = MetadataHelper.INSTANCE.findPrintPageChange(doc.getDocument(), "1");
            assertNotNull(findMeta1, "Meta tag for Print Page Change not found in document: " + doc.toXML());
            assertEquals(findMeta1.getAttributeValue("blank"), "true");
        } catch (ClassCastException | IndexOutOfBoundsException e) {
            fail("Malformed XML: " + doc.toXML(), e);
        }
    }

    @Test(enabled = false)
    public void testAdaptPageChangeWithPageTypeNewAndCL() {
        Document doc = nodeBuilder();
        assertNull(MetadataHelper.INSTANCE.findPrintPageChange(doc.getDocument(), "1"));
        MetadataHelper.INSTANCE.changePrintPageNumber(doc, "1", "2", null, false);
        MetadataHelper.INSTANCE.changePrintPageNumber(doc, "2", "3", "a", "P_PAGE", null, false);
        try {
            Element findMeta1 = MetadataHelper.INSTANCE.findPrintPageChange(doc.getDocument(), "1");
            assertNotNull(findMeta1, "Meta tag for Print Page Change not found in document: " + doc.toXML());
            assertEquals(findMeta1.getAttributeValue("new"), "3");
            assertEquals(findMeta1.getAttributeValue("cl"), "a");
            assertEquals(findMeta1.getAttributeValue("pageType"), "P_PAGE");
        } catch (ClassCastException | IndexOutOfBoundsException e) {
            fail("Malformed XML: " + doc.toXML(), e);
        }
    }

    @Test(enabled = false)
    public void testAdaptPageChangeWithBraillePage() {
        Document doc = nodeBuilder();
        assertNull(MetadataHelper.INSTANCE.findPrintPageChange(doc.getDocument(), "1"));
        MetadataHelper.INSTANCE.changeBraillePageNumber(doc, "1", "2", null, false, true);
        MetadataHelper.INSTANCE.changeBraillePageNumber(doc, "2", "3", null, false, true);
        try {
            Element findMeta1 = MetadataHelper.INSTANCE.findBraillePageChange(doc.getDocument(), "1");
            assertNotNull(findMeta1, "Meta tag for Braille Page Change not found in document: " + doc.toXML());
            assertEquals(findMeta1.getAttributeValue("new"), "3");
        } catch (ClassCastException | IndexOutOfBoundsException e) {
            fail("Malformed XML: " + doc.toXML(), e);
        }
    }

    @Test(enabled = false)
    public void testAdaptPageChangeWithBraillePageRunningHead() {
        Document doc = nodeBuilder();
        assertNull(MetadataHelper.INSTANCE.findPrintPageChange(doc.getDocument(), "1"));
        MetadataHelper.INSTANCE.changeBraillePageNumber(doc, "1", "2", null, false, false);
        MetadataHelper.INSTANCE.changeBraillePageNumber(doc, "2", "3", null, false, true);
        try {
            Element findMeta1 = MetadataHelper.INSTANCE.findBraillePageChange(doc.getDocument(), "1");
            assertNotNull(findMeta1, "Meta tag for Braille Page Change not found in document: " + doc.toXML());
            assertEquals(findMeta1.getAttributeValue("new"), "3");
            assertNull(findMeta1.getAttribute("runHead"));
        } catch (ClassCastException | IndexOutOfBoundsException e) {
            fail("Malformed XML: " + doc.toXML(), e);
        }
    }

    @Test(enabled = false)
    public void testRunningHeadWithBraillePage() {
        Document doc = nodeBuilder();
        assertNull(MetadataHelper.INSTANCE.findPrintPageChange(doc.getDocument(), "1"));
        MetadataHelper.INSTANCE.changeBraillePageNumber(doc, "1", "2", null, false, false);

        try {
            Element findMeta1 = MetadataHelper.INSTANCE.findBraillePageChange(doc.getDocument(), "1");
            assertNotNull(findMeta1, "Meta tag for Braille Page Change not found in document: " + doc.toXML());
            assertEquals(findMeta1.getAttributeValue("runHead"), "false");
        } catch (ClassCastException | IndexOutOfBoundsException e) {
            fail("Malformed XML: " + doc.toXML(), e);
        }

        MetadataHelper.INSTANCE.changeBraillePageNumber(doc, "1", "2", null, false, true);
        try {
            Element findMeta1 = MetadataHelper.INSTANCE.findBraillePageChange(doc.getDocument(), "1");
            assertNotNull(findMeta1, "Meta tag for Braille Page Change not found in document: " + doc.toXML());
            assertNull(findMeta1.getAttribute("runHead"));
        } catch (ClassCastException | IndexOutOfBoundsException e) {
            fail("Malformed XML: " + doc.toXML(), e);
        }
    }
}

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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Elements;

import org.apache.commons.lang3.StringUtils;
import org.brailleblaster.utd.properties.UTDElements;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.Iterables;

import static org.testng.Assert.*;

public class PageBuilderTest {
    @DataProvider(name = "nonUTDElementsProvider")
    public Object[][] nonUTDElementsProvider() {
        return new Object[][]{
                {new Element("p")},
                {new Element("a")},
                {new Element("brl")},
                {new Element("br")},
        };
    }

    @Test(dataProvider = "nonUTDElementsProvider", expectedExceptions = IllegalArgumentException.class)
    public void addBrlRejectNonUTD(Element element) {
        PageBuilder pageBuilder = new PageBuilder(new UTDTranslationEngine(), new Cursor());
        pageBuilder.addBrl(element);
    }

    @Test(dataProvider = "nonUTDElementsProvider", expectedExceptions = IllegalArgumentException.class)
    public void removeBrlRejectNonUTD(Element element) {
        PageBuilder pageBuilder = new PageBuilder(new UTDTranslationEngine(), new Cursor());
        pageBuilder.removeBrl(element);
    }

    @Test
    public void addBrlAcceptBrl() {
        PageBuilder pageBuilder = new PageBuilder(new UTDTranslationEngine(), new Cursor());
        Element element = UTDElements.BRL.create();
        Set<PageBuilder> result = pageBuilder.addBrl(element);
        assertEquals(result.size(), 1);
        assertTrue(result.contains(pageBuilder));
    }

    @Test
    public void removeBrlAcceptBrl() {
        Element brlElement = UTDElements.BRL.create();
        PageBuilder pb = new PageBuilder(new UTDTranslationEngine(), new Cursor());
        boolean result = pb.removeBrl(brlElement);
        assertFalse(result);
    }

    @Test
    public void addBrlLineWrap() {
        Element brlElement = UTDElements.BRL.create();
        final UTDTranslationEngine engine = new UTDTranslationEngine();
        PageBuilder pb = new PageBuilder(engine, new Cursor());
        pb.setFirstLineIndent(2);
        pb.setLeftIndent(0);
        brlElement.appendChild(stringGenerator(25));
        try {
            Set<PageBuilder> result = pb.addBrl(brlElement);
            assertEquals(result.size(), 1);
            assertTrue(result.contains(pb));
        } catch (IndexOutOfBoundsException e) {
            fail("Line wrapping error occurred", e);
        }
    }

    //	@Test
    public void addGuideDots() {
        PageBuilder pb = new PageBuilder(new UTDTranslationEngine(), new Cursor());
        pb.setFirstLineIndent(0);
        pb.setLeftIndent(0);
        Element brlElement = UTDElements.BRL.create();
        brlElement.appendChild("Test");
        Element brlElement2 = UTDElements.BRL.create();
        brlElement2.appendChild("EOL");
        Element brlElement3 = UTDElements.BRL.create();
        brlElement3.appendChild("012345678901234567890123456789012345");

        pb.setXY(0, 1);
        pb.addBrl(brlElement);
        pb.setX(37);
        pb.addBrl(brlElement2);
        pb.setX(4);
        pb.fillSpace('"', 1, 4);
        pb.setXY(0, 2);
        pb.addBrl(brlElement3);
        pb.fillSpace('"', 1, 5);
        pb.writeUTD();
        Element moveTo = (Element) brlElement2.getChild(0);
        Element dots = (Element) brlElement2.getChild(1);
        assertEquals(moveTo.getAttributeValue("hPos"), "31");
        assertEquals(dots.getLocalName(), "dots");
        assertEquals(dots.getChild(0).getValue().length(), 31);
        Elements childElements = brlElement3.getChildElements();
        for (int i = 0; i < childElements.size(); i++) {
            assertNotEquals(childElements.get(i).getLocalName(), "dots");
        }
    }

    //Skipping the last line of the page
    @Test
    public void addBrlPageWrap() {
        Element brlElement = UTDElements.BRL.create();
        String loremIpsum = "Lorem ipsum dolor sit amet, no discere constituto ius, eu vim semper nonumes nostrum.";
        loremIpsum = StringUtils.repeat(loremIpsum, 100);
        brlElement.appendChild(loremIpsum);
        final UTDTranslationEngine engine = new UTDTranslationEngine();
        PageBuilder pb = new PageBuilder(engine, new Cursor());
        Set<PageBuilder> result = pb.addBrl(brlElement, new RegexLineWrapper());
        assertEquals(result.size(), 10);
        Iterator<PageBuilder> it = result.iterator();
        assertSame(it.next(), pb);
    }

    @Test
    public void addBrlPendingPages() {
        Element brlElement = UTDElements.BRL.create();
        String loremIpsum = stringGenerator(15);
        brlElement.appendChild(loremIpsum);
        PageBuilder pb = new PageBuilder(new UTDTranslationEngine(), new Cursor());
        pb.addAtLeastPages(2);
        Set<PageBuilder> result = pb.addBrl(brlElement);
        assertEquals(result.size(), 3);
        Iterator<PageBuilder> it = result.iterator();
        assertSame(it.next(), pb);
    }

    @Test
    public void removeBrl() {
        Element brl1 = UTDElements.BRL.create();
        brl1.appendChild("Long text to be line wrapped at least once ");
        Element brl2 = UTDElements.BRL.create();
        brl2.appendChild("Another piece of long text that needs to have some line wrapping done to it");
        Element brl3 = UTDElements.BRL.create();
        brl3.appendChild("Text that is never inserted into the document.");
        PageBuilder pb = new PageBuilder(new UTDTranslationEngine(), new Cursor());
        pb.setFirstLineIndent(2);
        pb.setLeftIndent(0);
        pb.addBrl(brl1);
        pb.addBrl(brl2);
        assertTrue(pb.removeBrl(brl1));
        assertFalse(pb.removeBrl(brl3));
        assertTrue(pb.removeBrl(brl2));
    }

    @Test
    public void stickToTop() {
        Element brl1 = UTDElements.BRL.create();
        brl1.appendChild("This should stay on the first line");
        PageBuilder pb = new PageBuilder(generateDefaultEngine(), new Cursor());
        pb.addAtLeastLinesBefore(3);
        pb.addBrl(brl1);
        pb.setY(0);
        assertFalse(pb.isEmptyNumberLine());
        pb.setY(1);
        assertTrue(pb.isEmptyLine());
        pb.removeBrl(brl1);
        pb.setY(0);
        pb.setForceSpacing(true);
        pb.addAtLeastLinesBefore(3);
        pb.addBrl(brl1);
        pb.setY(0);
        assertTrue(pb.isEmptyNumberLine());
        pb.setY(3);
        assertFalse(pb.isEmptyLine());
    }

    @Test
    public void containsBrl() {
        Element brl1 = generateBrl(5);
        Element brl2 = generateBrl(3);
        PageBuilder pb = new PageBuilder(generateDefaultEngine(), new Cursor());
        pb.addBrl(brl1);
        assertTrue(pb.containsBrl(brl1));
        assertFalse(pb.containsBrl(brl2));
    }

    @Test
    public void processSpacing() {
        Element brl1 = generateBrl(5);
        PageBuilder pb = new PageBuilder(generateDefaultEngine(), new Cursor());
        pb.addBrl(brl1);
        int startY = pb.getY();
        pb.addAtLeastLinesBefore(2);
        pb.processSpacing();
        assertEquals(startY + 2, pb.getY());
    }

    @Test
    public void findBlankLine() {
        Element brl1 = generateBrl(1);
        Element brl2 = generateBrl(1);
        PageBuilder pb = new PageBuilder(generateDefaultEngine(), new Cursor());
        pb.addBrl(brl1);
        pb.addAtLeastLinesBefore(2);
        pb.addBrl(brl2);
        assertEquals(pb.findFirstBlankLine(), 1);
        assertEquals(pb.findLastBlankLine(), 3);
    }

    //	@Test
    public void runningHeads() {
        UTDTranslationEngine engine = generateDefaultEngine();
        engine.getPageSettings().setRunningHead("Test");
        PageBuilder pb = new PageBuilder(engine, new Cursor());
        Element brl1 = generateBrl(1);
        pb.addBrl(brl1);
        pb.setY(0);
        assertFalse(pb.isEmptyNumberLine(), "Running head not added:\n" + pb);
        pb.setY(1);
        assertFalse(pb.isEmptyLine());
        pb.setY(2);
        assertTrue(pb.isEmptyLine());
    }

    @Test
    public void multiplePageRunningHeads() {
        UTDTranslationEngine engine = generateDefaultEngine();
        engine.getPageSettings().setRunningHead("Test");
        PageBuilder pb = new PageBuilder(engine, new Cursor());
        Set<PageBuilder> results = new LinkedHashSet<>();
        results.add(pb);
        final int ITERATIONS = 10;
        for (int i = 0; i < ITERATIONS; i++) {
            pb = Iterables.getLast(results);
            Element brl = generateBrl(1);
            pb.addBrl(brl);
            pb.addAtLeastPages(2);
        }
        for (int i = 1; i < results.size(); i++) {
            assertEquals(pb.findFirstBlankLine(), 2, "Running head improperly added:\n" + pb);
        }
    }

    @Test
    public void removePrintPageIndicator() {
        Element brl1 = generateBrl(5);
        PageBuilder pb = new PageBuilder(generateDefaultEngine(), new Cursor());
        pb.addBrl(brl1);
        int endLine = pb.getY();
        Element ppi = UTDElements.BRL.create();
        ppi.addAttribute(new Attribute("printPage", "5"));
        ppi.addAttribute(new Attribute("printPageBrl", "e"));
        ppi.addAttribute(new Attribute("pageType", "NORMAL"));
        pb.setY(3);
        pb.setX(0);
        int inserted = pb.insertPrintPageIndicator(ppi);
        assertEquals(inserted, 1);
        assertEquals(pb.findLastBlankLine(), 4);
        pb.deletePrintPageIndicator(false);
        assertEquals(endLine + 1, pb.findLastBlankLine(), "PrintPageIndicator not deleted:\n" + pb);
    }

    @Test
    public void longWordLineWrap() {
        Element brl1 = UTDElements.BRL.create();
        brl1.appendChild("abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz");
        PageBuilder pb = new PageBuilder(generateDefaultEngine(), new Cursor());
        pb.addBrl(brl1);
        assertEquals(pb.findFirstBlankLine(), 3);
    }

    @Test
    public void multiElementLineWrap() {
        Element brl1 = UTDElements.BRL.create();
        brl1.appendChild("1234 6789 1234 6789 1234 6789 1234 67");
        Element brl2 = UTDElements.BRL.create();
        brl2.appendChild("890123456789");
        PageBuilder pb = new PageBuilder(generateDefaultEngine(), new Cursor());
        pb.setLineSpacing(1);
        pb.addBrl(brl1);
        pb.addBrl(brl2);
        pb.writeUTD();
        assertEquals(brl1.getChild(brl1.getChildCount() - 1).getValue(), "67", "Line wrap split between elements:\n" + pb);
    }

    @Test
    public void trailingSpaceLineWrap() {
        Element brl1 = UTDElements.BRL.create();
        brl1.appendChild("1234 6789 1234 6789 1234 6789 1234 67890 ");
        final UTDTranslationEngine engine = generateDefaultEngine();
        PageBuilder pb = new PageBuilder(engine, new Cursor());
        pb.setY(1);
        pb.addBrl(brl1);
        assertEquals(pb.getY(), 2);
        assertTrue(pb.isEmptyLine(), "Trailing space not trimmed:\n" + pb);
    }

    @Test
    public void testSkipTop() {
        Element brl1 = generateBrl(10);
        PageBuilder pb = new PageBuilder(generateDefaultEngine(), new Cursor());
        pb.setSkipNumberLineTop(true);
        pb.addBrl(brl1);
        pb.setY(0);
        assertFalse(pb.isEmptyNumberLine(), "Top line not skipped:\n" + pb);
        pb.setY(1);
        assertFalse(pb.isEmptyLine());
    }

    @Test
    public void testSkipBottom() {
        Element brl1 = generateBrl(200);
        PageBuilder pb = new PageBuilder(generateDefaultEngine(), new Cursor());
        pb.setSkipNumberLineBottom(true);
        pb.addBrl(brl1);
        pb.setY(23);
        assertFalse(pb.isEmptyLine());
        pb.setY(24);
        assertTrue(pb.isEmptyNumberLine(), "Bottom line not skipped:\n" + pb);
    }

    @DataProvider(name = "cursorProvider")
    public Object[][] cursorProvider() {
        Cursor cursor = new Cursor();
        Cursor cursor1 = new Cursor(0.3, 3.5);
        Cursor cursor2 = new Cursor(4.5, 10.3);

        return new Object[][]{
                {cursor}, {cursor1}, {cursor2},
        };
    }

    private UTDTranslationEngine generateDefaultEngine() {
        BrailleSettings brailleSettings = new BrailleSettings();
        brailleSettings.setUseAsciiBraille(true);
        UTDTranslationEngine newEngine = new UTDTranslationEngine();
        newEngine.setBrailleSettings(brailleSettings);
        return newEngine;
    }

    private Element generateBrl(int words) {
        Element brl = UTDElements.BRL.create();
        brl.appendChild(stringGenerator(words));
        return brl;
    }

    private String stringGenerator(int words) {
        String loremIpsum = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas suscipit ultricies viverra. Aliquam non nulla nisl. Morbi sodales, libero eget ultrices efficitur, orci rutrum erat, a condimentum ante arcu at dolor. Mauris sit amet erat a nibh dictum molestie ac sed eros";
        String[] wordArray = loremIpsum.split(" ");
        StringBuilder returnString = new StringBuilder();
        while (words > wordArray.length) {
            returnString.append(StringUtils.join(wordArray, " ", 0, wordArray.length));
            words -= wordArray.length;
        }
        returnString.append(StringUtils.join(wordArray, " ", 0, words));
        return returnString.toString();
    }
}

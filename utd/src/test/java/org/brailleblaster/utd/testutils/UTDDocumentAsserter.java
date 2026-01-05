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
package org.brailleblaster.utd.testutils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import com.google.common.collect.Lists;
import org.brailleblaster.utd.UTDTranslationEngine;
import org.brailleblaster.utd.internal.elements.MoveTo;
import org.brailleblaster.utd.internal.xml.FastXPath;
import org.brailleblaster.libembosser.spi.BrlCell;
import org.brailleblaster.utd.properties.UTDElements;

import nu.xom.Document;
import nu.xom.Text;
import nu.xom.Element;
import nu.xom.Node;

/**
 * UTDDocumentAsserter gathers every text node and assigns it to a moveTo
 * and provides methods to ensure text appears at the correct location in
 * the document. It's highly encouraged to end every test with
 * hasNoOtherMoveTo to ensure there is no stray UTD formatting.
 */
public class UTDDocumentAsserter {
    private final Pages pages;
    private final UTDTranslationEngine engine;

    public UTDDocumentAsserter(Document doc, UTDTranslationEngine engine) {
        this.engine = engine;
        List<Element> brlNodes = findBrlElements(doc);
        pages = createAssertionList(brlNodes);
    }

    /**
     * Asserts that the given text starts at the given cell position on the current
     * page.
     * Note that one moveTo may be assigned to multiple text nodes. In this case,
     * it combines every text node into one string to check against the text parameter.
     */
    public UTDDocumentAsserter hasTextAt(String text, int hPosCell, int vPosCell) {
        Page curPage = pages.getCurrentPage();
        MoveToPair foundMoveTo = findMoveToAt(hPosCell, vPosCell, curPage.getMoveTos());
        if (foundMoveTo.getText().equals(text)) {
            curPage.getMoveTos().remove(foundMoveTo);
            return this;
        } else {
            throw new AssertionError("MoveTo at " + hPosCell + ", " + vPosCell + " has text " + foundMoveTo.getText() + ", expected: " + text);
        }
    }

    /**
     * Assert that the current page has the given print page number in ASCII braille
     */
    public UTDDocumentAsserter hasPrintPageNumber(String printPageNumber) {
        String curPageNumber = pages.getCurrentPage().getPrintPageNum();
        if (curPageNumber == null && printPageNumber != null) {
            throw new AssertionError("Page " + pages.getCurrentPageNumber() + " has no print page number");
        } else if (curPageNumber != null && !curPageNumber.equals(printPageNumber)) {
            throw new AssertionError("Page " + pages.getCurrentPageNumber() + " has print page number " + curPageNumber + ", expected " + printPageNumber);
        }
        return this;
    }

    /**
     * Assert that the current page has the given braille page number in ASCII braille
     */
    public UTDDocumentAsserter hasBraillePageNumber(String braillePageNumber) {
        String curPageNumber = pages.getCurrentPage().getBraillePageNum();
        if (curPageNumber == null && braillePageNumber != null) {
            throw new AssertionError("Page " + pages.getCurrentPageNumber() + " has no braille page number");
        } else if (curPageNumber != null && !curPageNumber.equals(braillePageNumber)) {
            throw new AssertionError("Page " + pages.getCurrentPageNumber() + " has braille page number " + curPageNumber + ", expected " + braillePageNumber);
        }
        return this;
    }

    /**
     * Advance to the next page. If no such page exists, an AssertionError is thrown.
     */
    public UTDDocumentAsserter nextPage() {
        pages.nextPage();
        return this;
    }

    /**
     * Asserts that <code>hasTextAt()</code> has been called on every moveTo. Note that
     * moveTos that are used for page numbers are not included in this assertion. It's
     * highly encouraged to end every test with hasNoOtherMoveTo to ensure there is no
     * stray UTD formatting.
     */
    public void hasNoOtherMoveTo() {
        StringBuilder error = new StringBuilder();
        ArrayList<Page> allPages = pages.getPageList();
        for (Page page : allPages) {
            if (!page.getMoveTos().isEmpty()) {
                error.append("Page ").append(allPages.indexOf(page)).append(" has remaining moveTo(s):").append(System.lineSeparator());
                page.getMoveTos().forEach(mtp -> error.append(mtp.moveTo.toXML()).append(" for text: ").append(mtp.getText()).append(System.lineSeparator()));
            }
        }
        if (!error.isEmpty()) {
            throw new AssertionError(error.toString());
        }
    }

    /**
     * Creates a list of every brl element in the document
     */
    private List<Element> findBrlElements(Document doc) {
        List<Element> returnList = new ArrayList<>();
        StreamSupport.stream(((Iterable<Node>)FastXPath.descendant(doc.getRootElement())::iterator).spliterator(), false)
                .filter(UTDElements.BRL::isA)
                .forEach(n -> returnList.add((Element) n));
        return returnList;
    }

    /**
     * Constructs the Page objects which track the assertions being made
     * for each page.
     */
    private Pages createAssertionList(List<Element> brlElements) {
        Pages pages = new Pages();
        ArrayList<MoveToPair> moveToList = new ArrayList<>();
        boolean firstNewPage = true;
        String curPrintPage = null;
        String curBraillePage = null;

        for (Element brl : brlElements) {
            //Compile all descendants of the brl element into a list
            List<Node> descendants = Lists.newArrayList(FastXPath.descendant(brl)::iterator);

            for (Node child : descendants) {
                if (child instanceof Element) {
                    if (UTDElements.MOVE_TO.isA(child)) {
                        //Create a new MoveToPair
                        moveToList.add(new MoveToPair((Element) child));
                    } else if (UTDElements.NEW_PAGE.isA(child)) {
                        //Add the current moveTo list to the page and make a new
                        //moveTo list
                        if (firstNewPage) {
                            //But don't do anything if it's the first page
                            firstNewPage = false;
                        } else {
                            pages.addPage(newPage(moveToList, curPrintPage, curBraillePage));
                            curBraillePage = null;
                            curPrintPage = null;
                            moveToList = new ArrayList<>();
                        }
                    }
                } else if (child instanceof Text) {
                    if (moveToList.isEmpty()) {
                        //Text inside a brl should always follow a moveTo
                        throw new AssertionError("Text " + child.getValue() + " occurred before first moveTo");
                    }

                    //If child is a page number, do not consider it as part of the moveTos
                    //and remove the last moveTo added
                    if (UTDElements.BRL_PAGE_NUM.isA(child.getParent())) {
                        curBraillePage = child.getValue();
                        removePrevMoveTo(moveToList, (Text) child);
                    } else if (UTDElements.PRINT_PAGE_NUM.isA(child.getParent())) {
                        curPrintPage = child.getValue();
                        removePrevMoveTo(moveToList, (Text) child);
                    } else {
                        //Not a page number, add it to the previous moveToList
                        moveToList.getLast().addText((Text) child);
                    }
                }
            }
        }

        //Create the last page
        if (!moveToList.isEmpty() || curPrintPage != null || curBraillePage != null) {
            pages.addPage(newPage(moveToList, curPrintPage, curBraillePage));
        }
        return pages;
    }

    /**
     * Helper method that finds the moveTo that points to the given cell position
     */
    private MoveToPair findMoveToAt(int hPosCell, int vPosCell, ArrayList<MoveToPair> moveTos) {
        BrlCell cellType = engine.getBrailleSettings().getCellType();
        for (MoveToPair moveToPair : moveTos) {
            MoveTo moveTo = (MoveTo) moveToPair.moveTo;
            int hPos = cellType.getCellsForWidth(BigDecimal.valueOf(moveTo.getHPos().doubleValue()));
            if (hPos == hPosCell) {
                int vPos = cellType.getLinesForHeight(BigDecimal.valueOf(moveTo.getVPos().doubleValue()));
                if (vPos == vPosCell) {
                    return moveToPair;
                }
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Remaining moveTos:").append(System.lineSeparator());
        for (MoveToPair moveToPair : moveTos) {
            sb.append(moveToPair.getText())
                    .append(": ")
                    .append(cellType.getCellsForWidth(BigDecimal.valueOf(((MoveTo) moveToPair.moveTo).getHPos().doubleValue())))
                    .append(", ")
                    .append(cellType.getLinesForHeight(BigDecimal.valueOf(((MoveTo) moveToPair.moveTo).getVPos().doubleValue())))
                    .append(System.lineSeparator());
        }
        throw new AssertionError("MoveTo at " + hPosCell + ", " + vPosCell + " on page " + pages.getCurrentPageNumber() + " not found" + System.lineSeparator() + sb);
    }

    /**
     * Create a new page object with the given moveTo list and page numbers
     */
    private Page newPage(ArrayList<MoveToPair> moveTos, String printPageNumber, String braillePageNumber) {
        Page newPage = new Page(moveTos);
        newPage.setPrintPageNum(printPageNumber);
        newPage.setBraillePageNum(braillePageNumber);
        return newPage;
    }

    /**
     * Used to remove page numbers from a list of moveTos
     */
    private void removePrevMoveTo(ArrayList<MoveToPair> moveTos, Text pageNumber) {
        if (moveTos.getLast().moveTo.getValue().isEmpty()) {
            moveTos.removeLast();
        } else {
            throw new AssertionError("Page number " + pageNumber + " has no moveTo");
        }
    }

    /**
     * Maintains a list of pages and the current page that the UTDDocumentAsserter is
     * currently asserting on.
     */
    private static class Pages {
        private int curPage = 0;
        private final ArrayList<Page> pages;

        public Pages() {
            pages = new ArrayList<>();
        }

        public Page getCurrentPage() {
            if (pages.isEmpty()) {
                throw new AssertionError("No pages were created");
            }
            return pages.get(curPage);
        }

        public Page nextPage() {
            curPage++;
            if (curPage >= pages.size()) {
                throw new AssertionError("No new page found");
            }
            return pages.get(curPage);
        }

        public void addPage(Page page) {
            pages.add(page);
        }

        public int getCurrentPageNumber() {
            return curPage;
        }

        public ArrayList<Page> getPageList() {
            return pages;
        }
    }

    /**
     * Tracks the moveTos and the page numbers of the current page
     */
    private static class Page {
        private final ArrayList<MoveToPair> moveTos;
        private String printPageNum;
        private String braillePageNum;

        public Page(ArrayList<MoveToPair> moveTos) {
            this.moveTos = moveTos;
        }

        public ArrayList<MoveToPair> getMoveTos() {
            return moveTos;
        }

        public void setPrintPageNum(String printPageNum) {
            this.printPageNum = printPageNum;
        }

        public String getPrintPageNum() {
            return printPageNum;
        }

        public void setBraillePageNum(String braillePageNum) {
            this.braillePageNum = braillePageNum;
        }

        public String getBraillePageNum() {
            return braillePageNum;
        }
    }

    /**
     * An object that tracks a moveTo and every text node that corresponds
     * to that moveTo
     */
    private static class MoveToPair {
        private final List<Text> nodes;
        private final Element moveTo;

        public MoveToPair(Element moveTo) {
            this.moveTo = moveTo;
            nodes = new ArrayList<>();
        }

        public String getText() {
            StringBuilder sb = new StringBuilder();
            for (Text text : nodes) {
                sb.append(text.getValue());
            }
            return sb.toString();
        }

        public void addText(Text text) {
            nodes.add(text);
        }
    }
}

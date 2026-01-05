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
package org.brailleblaster.perspectives.braille.mapping.elements;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.ParentNode;
import org.brailleblaster.abstractClasses.BBEditorView;
import org.brailleblaster.abstractClasses.ViewUtils;
import org.brailleblaster.libembosser.spi.BrlCell;
import org.brailleblaster.perspectives.braille.Manager;
import org.brailleblaster.utd.PageSettings;
import org.brailleblaster.utd.properties.UTDElements;
import org.brailleblaster.wordprocessor.FontManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Listener;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class PageIndicator implements PaintedElement {
    private int line;
    private PaintedText runningHead;
    private PaintedText guideWords;
    private PaintedText braillePageNum;
    private PaintedText printPageNum;
    private final Manager manager;
    private Node printPageElement;
    private Node braillePageElement;
    public Listener listener;
    final BrlCell cellType;
    private final static int PAGE_NUM_PADDING = 3;
    private final int BRAILLE_LINE_OFFSET = -20;
    private final int PRINT_LINE_OFFSET = 5;

    public PageIndicator(Manager manager) {
        this.manager = manager;
        cellType = manager.getDocument().getEngine().getBrailleSettings().getCellType();
    }

    public PageIndicator(Manager manager, int line) {
        this.manager = manager;
        this.line = line;
        cellType = manager.getDocument().getEngine().getBrailleSettings().getCellType();
    }

    /**
     * Looks through the xml starting at the given point to look for print and braille page numbers.
     * Also finds guide words and running heads.
     *
     * @param startPoint The node to begin searching from. This should almost always be a newPage element.
     */
    public void findPageNums(Node startPoint) {
        if (UTDElements.BRL_PAGE_NUM.isA(startPoint)) {
            //When adding the final page indicator, startPoint will just be BrlPageNum
            //(if the braille page number is at the bottom)
            braillePageElement = startPoint;
            braillePageNum = new PaintedText(parsePage(startPoint.getValue()), findHPos(braillePageElement));
            handleRunningHeads(startPoint);
            handleGuideWords(startPoint);
            return;
        }
        if (UTDElements.PRINT_PAGE_NUM.isA(startPoint)) {
            //likewise for PrintPageNum
            printPageElement = startPoint;
            printPageNum = new PaintedText(retrievePrintPage((Element) startPoint), findHPos(printPageElement));
            handleRunningHeads(startPoint);
            handleGuideWords(startPoint);
            return;
        }

        handleRunningHeads(startPoint);
        handleGuideWords(startPoint);

        //Print page number
//		boolean backwards = pageSettings.getEvenPrintPageNumberAt().isBottom();
        int isPos = isPageNumberBottom(Integer.parseInt(((Element) startPoint).getAttributeValue("brlnum")), "PRINT");
        boolean backwards = isPos == 1;
        if (isPos != -1) {
            printPageElement = findPageNum(startPoint, backwards, UTDElements.PRINT_PAGE_NUM);
            if (printPageElement != null) {
                printPageNum = new PaintedText(retrievePrintPage((Element) printPageElement), findHPos(printPageElement));
            }
        }

        //Braille page number
//		backwards = pageSettings.getEvenBraillePageNumberAt().isBottom();
        isPos = isPageNumberBottom(Integer.parseInt(((Element) startPoint).getAttributeValue("brlnum")), "BRAILLE");
        if (isPos != -1) {
            backwards = isPos == 1;
            braillePageElement = findPageNum(startPoint, backwards, UTDElements.BRL_PAGE_NUM);
            if (braillePageElement != null) {
                braillePageNum = new PaintedText(parsePage(braillePageElement.getValue()), findHPos(braillePageElement));
            }
        }
    }

    private void handleRunningHeads(Node startPoint) {
        PageSettings pageSettings = manager.getDocument().getEngine().getPageSettings();
        //Running head
        Node runningHeadNode = searchForNode(startPoint, (n) -> n instanceof Element &&
                (isRunningHead((Element) n) || UTDElements.NEW_PAGE.isA(n)));
        if (runningHeadNode != null && !UTDElements.NEW_PAGE.isA(runningHeadNode)) {
            String runningHeadText;
            if (!pageSettings.getRunningHead().isEmpty())
                runningHeadText = pageSettings.getRunningHead(); //Use the print version of the running head if available
            else
                runningHeadText = runningHeadNode.getValue();
            runningHead = new PaintedText(runningHeadText, findHPos(runningHeadNode));
        }
    }

    private void handleGuideWords(Node startPoint) {
        //Guide words
        String guideWordsText;
        Node guideWordsNode = searchForPreviousNode(startPoint, (n) -> n instanceof Element &&
                (isGuideWord((Element) n) || UTDElements.NEW_PAGE.isA(n)));
        if (guideWordsNode != null && !UTDElements.NEW_PAGE.isA(guideWordsNode)) {
            if (((Element) guideWordsNode).getAttribute("printIndicator") != null) {
                guideWordsText = ((Element) guideWordsNode).getAttributeValue("printIndicator");
            } else {
                guideWordsText = guideWordsNode.getValue();
            }
            guideWords = new PaintedText(guideWordsText, findHPos(guideWordsNode));
        }
    }

    /*
     * -1 - NONE
     *  0 - FALSE
     *  1 - TRUE
     */
    private int isPageNumberBottom(int newPageNum, String keyword) {
        PageSettings pageSettings = manager.getDocument().getEngine().getPageSettings();
        boolean isEven = newPageNum % 2 == 0;

        if (keyword.equals("PRINT")) {
            if (isEven) {
                if (!pageSettings.getEvenPrintPageNumberAt().isTop()) {
                    if (pageSettings.getOddPrintPageNumberAt().isBottom()) {
                        return 1;
                    }
                } else {
                    return 0;
                }
            } else {
                if (!pageSettings.getOddPrintPageNumberAt().isTop()) {
                    if (pageSettings.getEvenPrintPageNumberAt().isBottom()) {
                        return 1;
                    }
                } else {
                    return 0;
                }
            }
        } else if (keyword.equals("BRAILLE")) {
            if (isEven) {
                if (!pageSettings.getEvenBraillePageNumberAt().isTop()) {
                    if (pageSettings.getOddBraillePageNumberAt().isBottom()) {
                        return 1;
                    }
                } else {
                    return 0;
                }
            } else {
                if (!pageSettings.getOddBraillePageNumberAt().isTop()) {
                    if (pageSettings.getEvenBraillePageNumberAt().isBottom()) {
                        return 1;
                    }
                } else {
                    return 0;
                }
            }
        }
        return -1;
    }

    private Node findPageNum(Node startPoint, boolean backwards, UTDElements element) {
        Node returnNode;
        Predicate<Node> test = (n) -> n instanceof Element && (element.isA(n) || UTDElements.NEW_PAGE.isA(n));
        if (backwards) {
            returnNode = searchForPreviousNode(startPoint, test);
        } else {
            returnNode = searchForNode(startPoint, test);
        }
        if (returnNode == null || UTDElements.NEW_PAGE.isA(returnNode)) {
            return null;
        }
        return returnNode;
    }

    //TODO: These methods are copy-pasted from Manager. These need to be better placed after the modularization is complete
    private Node searchForPreviousNode(Node startPoint, Predicate<Node> test) {
        Element curElement = (Element) startPoint.getParent();
        int startIndex = curElement.indexOf(startPoint) - 1;
        Node searchResult = searchDescendantsForNodeBackwards(curElement, startIndex, test);
        if (searchResult != null) {
            return searchResult;
        }
        ParentNode parent = curElement.getParent();
        if (parent == null || parent instanceof Document) {
            return null;
        }
        return searchForPreviousNode(curElement, test);
    }

    private Node searchDescendantsForNodeBackwards(Element parent, Predicate<Node> test) {
        return searchDescendantsForNodeBackwards(parent, parent.getChildCount() -1, test);
    }
    private Node searchDescendantsForNodeBackwards(Element parent, int startIndex, Predicate<Node> test) {
        for (int i = startIndex; i >= 0; i--) {
            Node child = parent.getChild(i);
            if (test.test(child)) {
                return child;
            } else if (child instanceof Element) {
                Node searchElement = searchDescendantsForNodeBackwards((Element) child, test);
                if (searchElement != null) {
                    return searchElement;
                }
            }
        }
        return null;
    }

    private Node searchForNode(Node startPoint, Predicate<Node> test) {
        Element curElement = (Element) startPoint.getParent();
        int startIndex = curElement.indexOf(startPoint) + 1;
        Node searchResult = searchDescendantsForNode(curElement, startIndex, test);
        if (searchResult != null) {
            return searchResult;
        }
        ParentNode parent = curElement.getParent();
        if (parent == null || parent instanceof Document)
            return null;
        return searchForNode(curElement, test);
    }

    private Node searchDescendantsForNode(Element parent, Predicate<Node> test) {
        return searchDescendantsForNode(parent, 0, test);
    }
    private Node searchDescendantsForNode(Element parent, int startIndex, Predicate<Node> test) {
        for (int i = startIndex; i < parent.getChildCount(); i++) {
            Node child = parent.getChild(i);
            if (test.test(child)) {
                return child;
            } else if (child instanceof Element) {
                Node searchElement = searchDescendantsForNode((Element) child, test);
                if (searchElement != null) {
                    return searchElement;
                }
            }
        }
        return null;
    }

    private int findHPos(Node node) {
        ParentNode parent = node.getParent();
        int index = parent.indexOf(node);
        if (index > 0 && UTDElements.MOVE_TO.isA(parent.getChild(index - 1))) {
            final String hPosAttribute = ViewUtils.getAttributeValue((Element) parent.getChild(index - 1), "hPos");
            if (hPosAttribute != null) {
                BigDecimal bd = new BigDecimal(hPosAttribute);
                double hPos = bd.doubleValue();
                return manager.getDocument().getEngine().getBrailleSettings().getCellType().getCellsForWidth(BigDecimal.valueOf(hPos));
            } else {
                return 0;
            }
        }
        return 0;
    }

    public void startListener(BBEditorView view) {
        if (listener != null)
            view.getView().removeListener(SWT.Paint, listener);
        listener = event -> {
            calculateNewPos(event.gc, view, 0, guideWords, braillePageNum);
            calculateNewPos(event.gc, view, 1, runningHead, printPageNum);
            int x1 = 0;
            int y1 = view.getView().getLinePixel(line == 0 ? 0 : line + 1);
            int x2 = view.getView().getBounds().width;
            FontManager.copyViewFont(manager, view, event.gc);
            final int brailleLine = y1 + BRAILLE_LINE_OFFSET;
            final int printLine = y1 + PRINT_LINE_OFFSET;

            if (guideWords != null) event.gc.drawText(guideWords.text, guideWords.pos, brailleLine);
            if (braillePageNum != null) event.gc.drawText(braillePageNum.text, braillePageNum.pos, brailleLine);

            if (runningHead != null) event.gc.drawText(runningHead.text, runningHead.pos, printLine);
            if (printPageNum != null) event.gc.drawText(printPageNum.text, printPageNum.pos, printLine);

            event.gc.drawLine(x1, y1, x2, y1);
        };
        view.getView().addListener(SWT.Paint, listener);
    }

    public void removeListener(@NotNull BBEditorView view) {
        if (listener != null)
            view.getView().removeListener(SWT.Paint, listener);
    }

    public boolean hasRunningHead() {
        return runningHead != null;
    }

    public boolean hasGuideWord() {
        return guideWords != null;
    }

    private void calculateNewPos(GC gc, BBEditorView view, int lineOffset, PaintedText... texts) {
        int curLine = line + lineOffset;
        int cellWidth = gc.textExtent(" ").x;
        int textViewLineWidth = curLine >= view.getView().getLineCount() ? 0 : view.getView().getLine(curLine).length() + (view.getView().getLineIndent(curLine) / view.getCharWidth());

        List<PaintedText> textList = Arrays.asList(texts);
        //Sort painted texts by position
        textList.sort((t1, t2) -> {
            if (t1 == null)
                return 1;
            if (t2 == null)
                return -1;
            return t1.pos - t2.pos;
        });

        int curEndPos = textViewLineWidth;
        for (PaintedText text : textList) {
            if (text == null)
                continue;
            text.pos = text.actualPos;

            //Text is overlapping either previously painted text or text inside of the styledtext widget
            if (text.actualPos - PAGE_NUM_PADDING < curEndPos) {
                text.pos = curEndPos + PAGE_NUM_PADDING;
            }

            curEndPos = text.pos;
            text.pos *= cellWidth;
        }
    }

    private boolean isGuideWord(Element node) {
        String typeAttr = node.getAttributeValue("type");
        return "guideWord".equals(typeAttr);
    }

    private boolean isRunningHead(Element node) {
        String typeAttr = node.getAttributeValue("type");
        return "runningHead".equals(typeAttr);
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getBraillePageNum() {
        if (braillePageNum != null) return braillePageNum.text;
        return "";
    }

    public String getPrintPageNum() {
        if (printPageNum != null) return printPageNum.text;
        return "";
    }

    public Node getPrintPageElement() {
        return printPageElement;
    }

    public Node getBraillePageElement() {
        return braillePageElement;
    }

    private String retrievePrintPage(Element printNode) {
        //This node needs to have both attributes: printPage and contLetter
        if (printNode.getAttribute("printPageBrl") == null && printNode.getAttribute("contLetter") == null) {
            throw new IllegalArgumentException("Missing needed attributes: printPage and/or contLetter");
        }

        return printNode.getAttributeValue("contLetter") + parsePage(printNode.getAttributeValue("printPageBrl"));
    }

    private String parsePage(String text) {
        StringBuilder num = new StringBuilder();
        int index = text.indexOf('#');
        if (index != -1) {
            for (int i = 0; i < index; i++) {
                if (text.charAt(i) == ',') {
                    num.append(Character.toUpperCase(text.charAt(i + 1)));
                    i++;
                } else
                    num.append(text.charAt(i));
            }

            for (int i = index + 1; i < text.length(); i++) {
                if (text.charAt(i) == 'j')
                    num.append('0');
                    //handles hyphenation between pages
                else if (text.charAt(i) == '-')
                    num.append('-');
                else if (text.charAt(i) == '4')
                    num.append('.');
                else if (text.charAt(i) == ',') {
                    num.append(Character.toUpperCase(text.charAt(i + 1)));
                    i++;
                } else if (text.charAt(i) != '#')
                    num.append((text.charAt(i) - 'a') + 1);
            }
        } else {
            text = text.replace(";", "");

            for (int i = index + 1; i < text.length(); i++) {
                if (text.charAt(i) == ',') {
                    num.append(Character.toUpperCase(text.charAt(i + 1)));
                    i++;
                } else
                    num.append(text.charAt(i));
            }
        }

        return num.toString();
    }

    private static class PaintedText {
        final String text;
        final int actualPos;
        int pos;

        public PaintedText(String text, int pos) {
            this.text = text;
            this.pos = actualPos = pos;
        }
    }
}

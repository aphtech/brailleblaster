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
package org.brailleblaster.perspectives.braille.mapping.maps;

import nu.xom.Element;
import nu.xom.Node;
import nu.xom.ParentNode;
import nu.xom.Text;
import org.brailleblaster.bbx.BBX;
import org.brailleblaster.exceptions.OutdatedMapListException;
import org.brailleblaster.perspectives.braille.Manager;
import org.brailleblaster.perspectives.braille.document.BrailleDocument;
import org.brailleblaster.perspectives.braille.mapping.elements.*;
import org.brailleblaster.perspectives.braille.messages.Message;
import org.brailleblaster.perspectives.braille.messages.Sender;
import org.brailleblaster.utd.internal.xml.FastXPath;
import org.brailleblaster.utd.internal.xml.XMLHandler;
import org.brailleblaster.utd.properties.UTDElements;
import org.brailleblaster.utd.utils.TableUtils;
import org.brailleblaster.utils.xml.NamespacesKt;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.stream.StreamSupport;

public class MapList extends LinkedList<@NonNull TextMapElement> {
    final @NonNull Manager dm;
    private TextMapElement current;
    private int currentIndex = -1;
    private int prevEnd, nextStart, prevBraille, nextBraille;

    public MapList(@NonNull Manager dm) {
        this.dm = dm;
    }

    public int findClosest(int offset, TextMapElement treeSelection, int low, int high) {
        int nodeIndex = getNodeIndex(treeSelection);
        int index = findClosestHelper(offset, nodeIndex, low, high);

        //Never return the end-of-line LineBreakElement if it can be avoided
        if (get(index) instanceof LineBreakElement && ((LineBreakElement) get(index)).isEndOfLine() && index > 0 && get(index - 1).getEnd(this) == get(index).getStart(this)) {
            index--;
        }

        return index;
    }

    /**
     * Binary search of MapList for offset
     */
    private int findClosestHelper(int location, int nodeIndex, int low, int high) {
        if (location <= this.get(0).getStart(this))
            return 0;
        else if (location >= this.getLast().getEnd(this))
            return this.indexOf(this.getLast());

        int mid = low + ((high - low) / 2);

        TextMapElement currentElement = this.get(mid);
        if (location >= currentElement.getStart(this) && location <= currentElement.getEnd(this)) {
            if (location == currentElement.getEnd(this) && location == this.get(mid + 1).getStart(this)) {

                if (mid == nodeIndex)
                    return mid;
                else if (mid + 1 == nodeIndex)
                    return mid + 1;
            } else if (location == currentElement.getStart(this) && location == this.get(mid - 1).getEnd(this)) {

                if (nodeIndex == mid - 1)
                    return mid - 1;
                else
                    return mid;
            } else {
                return mid;
            }
        } else if (location > currentElement.getEnd(this) && location < this.get(mid + 1).getStart(this)) {
            if (location - currentElement.getEnd(this) < this.get(mid + 1).getStart(this) - location) {
                return mid;
            } else if (location - currentElement.getEnd(this) > this.get(mid + 1).getStart(this) - location) {
                return mid + 1;
            } else {
                if (mid == nodeIndex)
                    return mid;
                else if (mid + 1 == nodeIndex)
                    return mid + 1;
                else
                    return mid;
            }
        }

        if (low > high)
            return -1;
        else if (location < this.get(mid).getStart(this))
            return findClosestHelper(location, nodeIndex, low, mid - 1);
        else
            return findClosestHelper(location, nodeIndex, mid + 1, high);
    }

    public int findClosestBraille(int offset, TextMapElement treeSelection) {
        //int location = (Integer)message.getValue("offset");
        int nodeIndex = getNodeIndex(treeSelection);

        return findClosestBrailleHelper(offset, nodeIndex);
    }

    private int findClosestBrailleHelper(int location, int nodeIndex) {
        if (!this.getFirst().brailleList.isEmpty() && location <= this.getFirst().brailleList.getFirst().getStart(this)) {
            return 0;
        } else if (!this.getLast().brailleList.isEmpty() && location >= this.getLast().brailleList.getLast().getEnd(this)) {
            return this.indexOf(this.getLast());
        }

        for (int i = 0; i < this.size(); i++) {
            if (!(this.get(i) instanceof TableTextMapElement) && this.get(i).brailleList.isEmpty())
                continue;
            int start = getStartBrailleOffset(this.get(i));
            int end = getEndBrailleOffset(this.get(i));

            if (location >= start && location <= end) {
                return i;
            } else if (i + 1 < this.size()) {
                int nextStart = getStartBrailleOffset(this.get(i + 1));
                if (nextStart != -1 && location > end && location < nextStart) {
                    if (location - end > nextStart - location) {
                        return i + 1;
                    } else if (location - end < nextStart - location) {
                        return i;
                    } else {
                        if (i == nodeIndex)
                            return i;
                        else if (i + 1 == nodeIndex)
                            return i + 1;
                        else
                            return i;
                    }
                }
            }
        }

        return 0;
    }

    private int getStartBrailleOffset(TextMapElement tme) {
        if (tme instanceof TableTextMapElement
                && !((TableTextMapElement) tme).tableElements.isEmpty()
                && !((TableTextMapElement) tme).tableElements.getFirst().brailleList.isEmpty()) {
            return ((TableTextMapElement) tme).tableElements.getFirst().brailleList.getFirst().getStart(this);
        } else if (!tme.brailleList.isEmpty()) {
            return tme.brailleList.getFirst().getStart(this);
        }
        return -1;
    }

    private int getEndBrailleOffset(TextMapElement tme) {
        if (tme instanceof TableTextMapElement
                && !((TableTextMapElement) tme).tableElements.isEmpty()
                && !((TableTextMapElement) tme).tableElements.getFirst().brailleList.isEmpty()) {
            return ((TableTextMapElement) tme).tableElements.getLast().brailleList.getLast().getEnd(this);
        } else if (!tme.brailleList.isEmpty()) {
            return tme.brailleList.getLast().getEnd(this);
        }
        return -1;
    }

    public TextMapElement getElementInRange(int offset) {
        int index = findClosest(offset, getCurrent(), 0, size() - 1);
        TextMapElement t = null;
        if (index != -1)
            t = get(index);

        if (t != null && offset >= t.getStart(this) && offset <= t.getEnd(this))
            return t;
        else
            return null;
    }

    /***
     * Return all elements that selected in text
     */
    public Set<TextMapElement> getElementInSelectedRange(int start, int end) {
        Set<TextMapElement> elementSelectedSet = new LinkedHashSet<>();
        Set<Element> parentElement = new LinkedHashSet<>();
        int j = start;
        while (j < end) {
            TextMapElement t = getElementInRange(j);

            if (t != null && !(t instanceof WhiteSpaceElement)) {
                Element currentParent = t instanceof BoxLineTextMapElement ? t.getNodeParent() : dm.getDocument().getParent(t.getNode());
                if (!(parentElement.contains(currentParent))) {
                    parentElement.add(currentParent);
                    elementSelectedSet.add(t);
                }

                j = t.getEnd(this) + 1;

            } else {
                j = j + 1;
            }
        }
        return elementSelectedSet;
    }

    /**
     * Evaluate for each index, most useful for smallcaps where an element will
     * only have one character
     */
    public Set<TextMapElement> getElementsOneByOne(int start, int end) {
        Set<TextMapElement> elementSelectedSet = new LinkedHashSet<>();
        for (int i = start; i < end; i++) {
            TextMapElement t = getClosest(i, true);

            if (t != null && !(t instanceof WhiteSpaceElement)) {
                elementSelectedSet.add(t);
            }
        }
        return elementSelectedSet;
    }

    public void adjustOffsets(String type, int index, int position) {
        TextMapElement t = get(index);

        if (type.equals("start")) {
            t.setStart(t.getStart(this) - position);
            if (!t.brailleList.isEmpty())
                t.brailleList.getFirst().setStart(t.brailleList.getFirst().getStart(this) - position);
        }

        if (type.equals("end")) {
            t.setEnd(t.getEnd(this) + position);
            if (!t.brailleList.isEmpty())
                t.brailleList.getLast().setEnd(t.brailleList.getLast().getEnd(this) + position);
        }
    }

    public @NonNull TextMapElement setCurrent(int index) {
        TextMapElement result = this.get(index);
        this.current = result;
        this.currentIndex = index;

        if (index > 0 && this.getPrevious(false) != null)
            this.prevEnd = this.getPrevious(false).getEnd(this);
        else
            this.prevEnd = -1;

        if (index != this.size() - 1 && this.getNext(false) != null)
            this.nextStart = this.getNext(false).getStart(this);
        else
            this.nextStart = -1;

        this.nextBraille = getNextBraille(index);
        this.prevBraille = getPreviousBraille(index);
        return result;
    }

    public @NonNull TextMapElement getCurrent() {
        if (this.current != null) {
            return this.current;
        } else {
            try {
                return setCurrent(0);
            } catch (IndexOutOfBoundsException e) {
                throw new OutdatedMapListException("MapList is empty", e);
            }
        }
    }

    public int getCurrentIndex() {
        if (isEmpty()) {
            return -1;
        } else {
            if (this.current == null) {
                currentIndex = findClosest(getFirst().getStart(this), getFirst(), 0, size() - 1);
            }
            return this.currentIndex;
        }
    }

    public int getCurrentBrailleEnd() {
        if (this.current.brailleList.isEmpty()) {
            //if statement added to handle blank brl in inline elements like T<span>HE</span>
            //which gets translated as word THE
            if (currentIndex > 0) {
                TextMapElement t = get(currentIndex - 1);
                if (!t.brailleList.isEmpty())
                    return t.brailleList.getLast().getEnd(this);
            }
            return 0;
        } else
            return this.current.brailleList.getLast().getEnd(this);
    }

    public int getCurrentBrailleOffset() {
        for (int i = 0; i < current.brailleList.size(); i++) {
            if (current.brailleList.get(i) instanceof NewPageBrlMapElement || current.brailleList.get(i).getStart(this) == TextMapElement.NOT_SET)
                continue;
            return current.brailleList.get(i).getStart(this);
        }
        return 0;
    }

    private int getNextBraille(int index) {
        TextMapElement tme = getNext(index, false);

        if (tme != null && !tme.brailleList.isEmpty())
            return tme.brailleList.getFirst().getStart(this);

        return -1;
    }

    private int getPreviousBraille(int index) {
        int localIndex = index - 1;

        while (localIndex >= 0 && this.get(localIndex).brailleList.isEmpty())
            localIndex--;

        if (localIndex >= 0)
            return this.get(localIndex).brailleList.getLast().getEnd(this);

        return -1;
    }

    public void getCurrentNodeData(Message m, int offset, TextMapElement treeSelection, Sender sender) {
        if (this.current == null) {
            int index;
            if (sender.equals(Sender.BRAILLE))
                index = findClosestBraille(offset, treeSelection);
            else
                index = findClosest(offset, treeSelection, 0, this.size() - 1);

            setCurrent(index);
        }

        m.put("start", current.getStart(this));
        m.put("end", current.getEnd(this));
        m.put("previous", prevEnd);
        m.put("next", nextStart);
        m.put("brailleStart", getCurrentBrailleOffset());
        m.put("brailleEnd", getCurrentBrailleEnd());
        m.put("nextBrailleStart", nextBraille);
        m.put("previousBrailleEnd", prevBraille);
        //m.put("pageRanges", getPageRanges());
        m.put("currentElement", current);
    }

    public int getNodeIndex(TextMapElement t) {
        return this.indexOf(t);
    }

    public void findTextMapElements(ArrayList<Node> textList, ArrayList<TextMapElement> itemList) {
        int pos = 0;
        for (Node node : textList) {
            for (int j = pos; j < this.size(); j++) {
                if (get(j) instanceof TableTextMapElement && node.equals(get(j).getNodeParent())) {
                    itemList.add(this.get(j));
                    pos = j + 1;
                    break;
                }

                if (get(j) instanceof BoxLineTextMapElement && node.equals(get(j).getNode())) {
                    itemList.add(this.get(j));
                    pos = j + 1;
                    break;
                }

                if (node.equals(this.get(j).getNode())) {
                    itemList.add(this.get(j));
                    pos = j + 1;
                    break;
                }
            }
        }
    }

    /**
     * @param index:  starting index
     * @param parent: parent to check for all subsequent children in the maplist
     * @return returns an arraylist of TextMapElements that comprise an element in the XML
     */
    public List<TextMapElement> findTextMapElements(int index, Element parent) {
        ArrayList<TextMapElement> list = new ArrayList<>();
        BrailleDocument doc = dm.getDocument();

        int countDown = index - 1;
        int countUp = index + 1;
        while (countDown >= 0 && get(countDown).getNodeParent() != null
                && !(this.get(countDown) instanceof PageIndicatorTextMapElement)
                && doc.getParent(this.get(countDown).getNode()).equals(parent)) {

            list.addFirst(this.get(countDown));
            countDown--;
        }

        list.add(this.get(index));

        while (countUp < this.size() && get(countUp).getNodeParent() != null
                && !(this.get(countUp) instanceof PageIndicatorTextMapElement)
                && doc.getParent(this.get(countUp).getNode()).equals(parent)) {

            list.add(this.get(countUp));
            countUp++;
        }

        return list;
    }

    /**
     * @param index:  starting index
     * @param parent: parent to check for all subsequent children in the maplist
     * @return returns an arraylist of the indexes that comprise an element in the XML
     */
    public List<Integer> findTextMapElementRange(int index, Element parent) {
        parent = dm.getDocument().getParent(parent);

        ArrayList<Integer> list = new ArrayList<>();
        BrailleDocument doc = dm.getDocument();

        int countDown = index - 1;
        int countUp = index + 1;
        while (countDown >= 0 && get(countDown).getNodeParent() != null && doc.getParent(this.get(countDown).getNode()).equals(parent)) {
            list.addFirst(countDown);
            countDown--;
        }

        list.add(index);

        while (countUp < this.size() && get(countUp).getNodeParent() != null && doc.getParent(this.get(countUp).getNode()).equals(parent)) {
            list.add(countUp);
            countUp++;
        }

        return list;
    }

    /**
     * Find the TextMapElement that contains the given node
     */
    public @Nullable TextMapElement findNode(Node n) {
        int index = findNodeIndex(n, 0);
        if (index == -1)
            return null;
        return get(index);
    }

    /**
     * Finds the index f an element in the list using a node as the key
     *
     * @param n:          node to find
     * @param startIndex: since index is a linear search higher valus that 0 can b specified as the starting point
     * @return -1 if not found, index value if found
     */
    public int findNodeIndex(Node n, int startIndex) {
        Element usableElement = (Element) (n instanceof Text ? n.getParent() : n);
        if (usableElement.getNamespaceURI().equals(NamespacesKt.MATHML_NS)) {
            //Text nodes under a <m:math> tag are not in the map list
            //However they are all under an element in the m: namespace
            //Find the INLINE.MATHML tag (which wraps <m:math>) then get <m:math>
            n = XMLHandler.Companion.ancestorVisitor(
                    n,
                    curAncestor -> BBX.INLINE.MATHML.isA(curAncestor.getParent())
            );
        }
        Node potentialTable = XMLHandler.Companion.ancestorVisitor(Objects.requireNonNull(n), e -> BBX.CONTAINER.TABLE.isA(e) && "simple".equals(((Element) e).getAttributeValue("format")));
        if (potentialTable != null) {
            n = potentialTable;
        }

        if (BBX.CONTAINER.TABLE.isA(n) && !TableUtils.isTableCopy((Element) n)) {
            ParentNode parent = n.getParent();
            int index = parent.indexOf(n);
            if (index + 1 < parent.getChildCount()
                    && BBX.CONTAINER.TABLE.isA(parent.getChild(index + 1))
                    && TableUtils.isTableCopy((Element) parent.getChild(index + 1))) {
                n = parent.getChild(index + 1);
            }
        }
        List<Node> children = new ArrayList<>();
        children.add(n);
        if (n instanceof Element && !BBX.CONTAINER.TABLE.isA(n)) {
            children.addAll(StreamSupport.stream(((Iterable<Node>)FastXPath.descendant(n)::iterator).spliterator(), false).filter(node ->
                    XMLHandler.Companion.ancestorElementNot(node, UTDElements.BRL::isA)).toList());
        }

        for (int i = startIndex; i < this.size(); i++) {
            Node node = get(i).getNode();
            if (node == null) {
                continue;
            }
            for (Node child : children) {
                if (node.equals(child)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public void clearList() {
        this.clear();
        this.current = null;
        currentIndex = -1;
    }

    public void resetList() {
        int size = size();
        for (int i = 0; i < size; i++) {
            get(i).resetOffsets();
            for (int j = 0; j < get(i).brailleList.size(); j++) {
                get(i).brailleList.get(j).setOffsets(0, 0);
            }
        }
    }

    public boolean inPrintPageRange(int offset) {
        if (size() > 0) {
            int index = findClosest(offset, null, 0, size() - 1);
            if (get(index) instanceof PageIndicatorTextMapElement) {
                return offset >= get(index).getStart(this) && offset <= get(index).getEnd(this);
            }
        }

        return false;
    }

    public boolean inBraillePageRange(int offset) {
        if (size() > 0) {
            int index = findClosestBraille(offset, null);
            if (get(index) instanceof PageIndicatorTextMapElement) {
                return offset >= get(index).brailleList.getFirst().getStart(this) && offset <= get(index).brailleList.getLast().getEnd(this);
            }
        }

        return false;
    }

    /**
     * Searches for either the opening or closing boxline of a pair
     *
     * @param b: Boxline from which the opening or closing boxline is found
     * @return The BRLOnlyMapElement containing the opening or closing boxline of a typical pair
     */
    public @Nullable BoxLineTextMapElement findJoiningBoxline(BoxLineTextMapElement b) {
        int index = indexOf(b);
        if (b.getNodeParent().indexOf(b.getNode()) == 0) {
            for (int i = index + 1; i < size(); i++) {
                if (get(i) instanceof BoxLineTextMapElement && get(i).getNodeParent().equals(b.getNodeParent()))
                    return (BoxLineTextMapElement) get(i);
            }
        } else if (b.getNodeParent().indexOf(b.getNode()) == b.getNodeParent().getChildCount() - 1) {
            for (int i = index - 1; i >= 0; i--) {
                if (get(i) instanceof BoxLineTextMapElement && get(i).getNodeParent().equals(b.getNodeParent()))
                    return (BoxLineTextMapElement) get(i);
            }
        }

        return null;
    }

    public @Nullable TextMapElement findClosestNonWhitespace(WhiteSpaceElement wse) {
        TextMapElement first = null;
        TextMapElement last = null;
        int index = indexOf(wse);

        if (index == 0) {
            last = findNextNonWhitespace(index);
        } else if (index == size() - 1) {
            first = findPreviousNonWhitespace(index);
        } else {
            last = findNextNonWhitespace(index);
            first = findPreviousNonWhitespace(index);
        }
        if (first != null) {
            if (last == null)
                return first;
            else {
                if (first instanceof PageIndicatorTextMapElement)
                    return last;
                else if (last instanceof PageIndicatorTextMapElement)
                    return first;
                else if (wse.getStart(this) - first.getEnd(this) <= last.getStart(this) - wse.getEnd(this))
                    return first;
                else
                    return last;
            }
        } else return last;
    }

    public @Nullable TextMapElement findPreviousNonWhitespace(int index) {
        TextMapElement t = null;
        while (index >= 0 && t == null) {
            if (!(get(index) instanceof WhiteSpaceElement))
                t = get(index);

            index--;
        }

        return t;
    }

    private @Nullable TextMapElement findPrevious(int index) {
        TextMapElement t = null;
        while (index >= 0 && t == null) {
            if (!isIgnorableWhiteSpace(get(index)))
                t = get(index);

            index--;
        }

        return t;
    }

    public TextMapElement findNextNonWhitespace(int index) {
        TextMapElement t = null;
        while (index < size() && t == null) {
            if (!(get(index) instanceof WhiteSpaceElement))
                t = get(index);

            index++;
        }

        return t;
    }

    private TextMapElement findNext(int index) {
        TextMapElement t = null;
        while (index < size() && t == null) {
            if (!isIgnorableWhiteSpace(get(index)))
                t = get(index);

            index++;
        }
        return t;
    }

    /**
     * These elements should always be ignored when search the maplist
     */
    private boolean isIgnorableWhiteSpace(TextMapElement tme) {
        return (tme instanceof LineBreakElement && ((LineBreakElement) tme).isEndOfLine()) || tme instanceof PaintedWhiteSpaceElement;
    }

    public String printContents(boolean showBrailleList) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < size()) {
            TextMapElement curTME = get(i);
            sb.append(curTME.getClass().getSimpleName())
                    .append(" -- Index:")
                    .append(i)
                    .append("  Offsets:")
                    .append(curTME.getStart(this))
                    .append("-")
                    .append(curTME.getEnd(this))
                    .append(". Braillelist size: ")
                    .append(curTME.brailleList.size())
                    .append(" Node: ")
                    .append(curTME.getNode() == null ? "null" : XMLHandler.toXMLSimple(curTME.getNode()))
                    .append("\n");
            i++;
            if (showBrailleList) {
                int subI = 0;
                for (BrailleMapElement brailleMapElement : curTME.brailleList) {
                    sb.append("-")
                            .append(brailleMapElement.getClass().getSimpleName())
                            .append(" -- Index:")
                            .append(subI)
                            .append(" Braille: ")
                            .append(brailleMapElement.getNode() == null ? "null" : XMLHandler.toXMLSimple(brailleMapElement.getNode()))
                            .append("\n");
                    subI++;
                }
            }
        }
        return sb.toString();
    }

    public TextMapElement getPrevious(boolean ignoreWhitespace) {
        return getPrevious(getCurrentIndex(), ignoreWhitespace);
    }

    public TextMapElement getPrevious(int index, boolean ignoreWhitespace) {
        if (ignoreWhitespace) {
            return findPreviousNonWhitespace(index - 1);
        } else {
            if (index > 0)
                return findPrevious(index - 1);
            else
                return null;
        }
    }

    public TextMapElement getNext(boolean ignoreWhitespace) {
        return getNext(getCurrentIndex(), ignoreWhitespace);
    }

    public @Nullable TextMapElement getNext(int index, boolean ignoreWhitespace) {
        if (ignoreWhitespace) {
            return findNextNonWhitespace(index + 1);
        } else {
            if (!isEmpty() && index < size() - 1)
                return findNext(index + 1);
            else
                return null;
        }
    }

    public TextMapElement getClosest(int offset, boolean ignoreWhitespace) {
        TextMapElement t;
        if (ignoreWhitespace) {
            t = get(findClosest(offset, null, 0, size() - 1));
            if (t instanceof WhiteSpaceElement)
                t = findClosestNonWhitespace((WhiteSpaceElement) t);
        } else
            t = get(findClosest(offset, null, 0, size() - 1));

        return t;
    }

    /**
     * Get closest TextMapElement, similar to getCurrent() but never returns null
     *
     * @return the closest text map element.
     */
    public @NonNull TextMapElement getCurrentNonWhitespace(int pos) {
        return getClosest(pos, true);
    }

    public TextMapElement getFirstUsable() {
        return this.stream()
            .filter(textMapElement -> !(textMapElement instanceof PaintedWhiteSpaceElement) && !(textMapElement instanceof LineBreakElement))
            .findFirst()
            .orElse(null);
    }

    public TextMapElement getLastUsable() {
        return this.stream()
            .filter(textMapElement -> !(textMapElement instanceof PaintedWhiteSpaceElement) && !(textMapElement instanceof LineBreakElement))
            .reduce((first, second) -> second)
            .orElse(null);
    }

    public int getPageCount() {
        return (int) stream().filter(e -> e instanceof PageIndicatorTextMapElement).count();
    }


    public boolean containsNode(Node n) {
        return stream().map(AbstractMapElement::getNode).anyMatch(node -> node != null && node.equals(n));
    }

    public int getPrevEnd() {
        return prevEnd;
    }

    public int getNextStart() {
        return nextStart;
    }

    public int getNextBraille() {
        return nextBraille;
    }

}

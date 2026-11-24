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
import java.util.Objects;

import org.brailleblaster.utd.properties.NumberLinePosition;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class StyleStackTest {
    private Style[] styles;
    private StyleStack stack;

    @BeforeClass
    public void setupClass() {
        styles = new Style[4];
        Style style1 = new Style();
        style1.setLinesBefore(1);
        style1.setLinesAfter(1);
        style1.setDontSplit(false);
        style1.setNewPagesBefore(0);
        style1.setNewPagesAfter(1);
        style1.setKeepWithNext(false);
        style1.setKeepWithPrevious(true);
        style1.setIndent(null);
        style1.setLineLength(2);
        style1.setOrphanControl(4);
        style1.setSkipNumberLines(NumberLinePosition.BOTH);
        style1.setFirstLineIndent(2);
        style1.setPageNum(true);
        styles[0] = style1;
        Style style2 = new Style();
        style2.setLinesBefore(1);
        style2.setLinesAfter(2);
        style2.setDontSplit(true);
        style2.setNewPagesBefore(1);
        style2.setNewPagesAfter(0);
        style2.setKeepWithNext(true);
        style2.setKeepWithPrevious(false);
        style2.setSkipNumberLines(NumberLinePosition.TOP);
        style2.setOrphanControl(1);
        style2.setFirstLineIndent(5);
        style2.setIndent(2);
        style2.setLineLength(null);
        style2.setGuideWords(true);
        styles[1] = style2;
        Style style3 = new Style();
        style3.setLinesBefore(3);
        style3.setLinesAfter(3);
        style3.setDontSplit(false);
        style3.setNewPagesBefore(0);
        style3.setNewPagesAfter(1);
        style3.setKeepWithNext(false);
        style3.setKeepWithPrevious(true);
        style3.setIndent(3);
        style3.setLineLength(1);
        style3.setOrphanControl(2);
        styles[2] = style3;
        Style style4 = new Style();
        style4.setLinesBefore(3);
        style4.setLinesAfter(3);
        style4.setDontSplit(false);
        style4.setNewPagesBefore(0);
        style4.setNewPagesAfter(1);
        style4.setKeepWithNext(false);
        style4.setKeepWithPrevious(true);
        style4.setIndent(3);
        style4.setLineLength(1);
        style4.setOrphanControl(2);
        style4.setSkipNumberLines(NumberLinePosition.BOTTOM);
        styles[3] = style4;

    }

    @BeforeMethod
    public void setupMethod() {
        stack = new StyleStack();
    }

    @Test
    public void isStack() {
        StyleStack styleStack = new StyleStack();
        assertNull(styleStack.peek());
        for (Style style : styles) {
            styleStack.push(style);
            assertSame(styleStack.peek(), style);
        }
        for (int i = styles.length - 1; i >= 0; i--) {
            assertSame(styleStack.peek(), styles[i]);
            styleStack.pop();
        }
        assertNull(styleStack.peek());
    }

    @Test
    public void findLinesBefore() {
        assertEquals(stack.getLinesBefore(), 0);
        for (Style style : styles) {
            stack.push(style);
            assertEquals(stack.getLinesBefore(), style.getLinesBefore());
        }

        for (int i = styles.length - 1; i >= 0; i--) {
            assertEquals(stack.getLinesBefore(), styles[i].getLinesBefore());
            stack.pop();
        }

        assertEquals(stack.getLinesBefore(), 0);
    }

    @Test
    public void findLinesAfter() {
        assertEquals(stack.getLinesAfter(), 0);
        for (Style style : styles) {
            stack.push(style);
            assertEquals(stack.getLinesAfter(), style.getLinesAfter());
        }

        for (int i = styles.length - 1; i >= 0; i--) {
            assertEquals(stack.getLinesAfter(), styles[i].getLinesAfter());
            stack.pop();
        }

        assertEquals(stack.getLinesAfter(), 0);
    }

    @Test
    public void findLeftMargin() {
        Integer inheritedValue = 0;

        for (Style style : styles) {
            stack.push(style);
            if (style.getIndent() != null) {
                inheritedValue = style.getIndent();
            }
            assertEquals(stack.getIndent(), inheritedValue, 0.01);
        }

        for (int i = styles.length - 1; i >= 0; i--) {
            Iterator<IStyle> iter = stack.iterator();

            if (styles[i].getIndent() == null) {
                inheritedValue = null;
                while (iter.hasNext() && inheritedValue == null) {
                    inheritedValue = iter.next().getIndent();
                }
                assertEquals(stack.getIndent(), Objects.requireNonNullElse(inheritedValue, 0), 0.01);
            } else {
                assertEquals(stack.getIndent(), styles[i].getIndent(), 0.01);
            }
            stack.pop();
        }

        assertEquals(stack.getIndent(), 0, 0.01);
    }

    @Test
    public void findRightMargin() {
        Integer inheritedValue = 0;

        for (Style style : styles) {
            stack.push(style);
            if (style.getLineLength() != null) {
                inheritedValue = style.getLineLength();
            }
            assertEquals(stack.getLineLength(), inheritedValue, 0.01);
        }

        for (int i = styles.length - 1; i >= 0; i--) {
            Iterator<IStyle> iter = stack.iterator();

            if (styles[i].getLineLength() == null) {
                inheritedValue = null;
                while (iter.hasNext() && inheritedValue == null) {
                    inheritedValue = iter.next().getLineLength();
                }
                assertEquals(stack.getLineLength(), Objects.requireNonNullElse(inheritedValue, 0), 0.01);
            } else {
                assertEquals(stack.getLineLength(), styles[i].getLineLength(), 0.01);
            }
            stack.pop();
        }

        assertEquals(stack.getLineLength(), 0, 0.01);
    }

    @Test
    public void findFirstLineIndent() {
        Integer inheritedValue = 0;

        for (Style style : styles) {
            stack.push(style);
            if (style.getFirstLineIndent() != null) {
                inheritedValue = style.getFirstLineIndent();
            }
            assertEquals(stack.getFirstLineIndent(), inheritedValue);
        }

        for (int i = styles.length - 1; i >= 0; i--) {
            int j = i;
            do {
                inheritedValue = styles[j].getFirstLineIndent();
                j--;
            } while (inheritedValue == null && j >= 0);
            if (inheritedValue == null) {
                assertEquals(stack.getFirstLineIndent(), 0, 0.01);
            } else {
                assertEquals(stack.getFirstLineIndent(), inheritedValue);
            }
            stack.pop();
        }

        assertEquals(stack.getFirstLineIndent(), Integer.valueOf(0));
    }

    @Test
    public void findNewPageBefore() {
        assertEquals(stack.getNewPagesBefore(), 0);
        for (Style style : styles) {
            stack.push(style);
            assertEquals(stack.getNewPagesBefore(), style.getNewPagesBefore());
        }

        for (int i = styles.length - 1; i >= 0; i--) {
            assertEquals(stack.getNewPagesBefore(), styles[i].getNewPagesBefore());
            stack.pop();
        }

        assertEquals(stack.getNewPagesBefore(), 0);
    }

    @Test
    public void findNewPageAfter() {
        assertEquals(stack.getNewPagesAfter(), 0);
        for (Style style : styles) {
            stack.push(style);
            assertEquals(stack.getNewPagesAfter(), style.getNewPagesAfter());
        }

        for (int i = styles.length - 1; i >= 0; i--) {
            assertEquals(stack.getNewPagesAfter(), styles[i].getNewPagesAfter());
            stack.pop();
        }

        assertEquals(stack.getNewPagesAfter(), 0);
    }

    @Test
    public void findKeepWithNext() {
        assertFalse(stack.isKeepWithNext());
        for (Style style : styles) {
            stack.push(style);
            assertEquals(stack.isKeepWithNext(), style.isKeepWithNext());
        }

        for (int i = styles.length - 1; i >= 0; i--) {
            assertEquals(stack.isKeepWithNext(), styles[i].isKeepWithNext());
            stack.pop();
        }

        assertFalse(stack.isKeepWithNext());
    }

    @Test
    public void findKeepWithPrevious() {
        assertFalse(stack.isKeepWithPrevious());
        for (Style style : styles) {
            stack.push(style);
            assertEquals(stack.isKeepWithPrevious(), style.isKeepWithPrevious());
        }

        for (int i = styles.length - 1; i >= 0; i--) {
            assertEquals(stack.isKeepWithPrevious(), styles[i].isKeepWithPrevious());
            stack.pop();
        }

        assertFalse(stack.isKeepWithPrevious());
    }

    @Test
    public void findOrphanControl() {
        assertEquals(stack.getOrphanControl(), 1);
        for (Style style : styles) {
            stack.push(style);
            assertEquals(stack.getOrphanControl(), style.getOrphanControl());
        }

        for (int i = styles.length - 1; i >= 0; i--) {
            assertEquals(stack.getOrphanControl(), styles[i].getOrphanControl());
            stack.pop();
        }

        assertEquals(stack.getOrphanControl(), 1);
    }

    @Test
    public void findLineSpacing() {
        Style style1 = new Style();
        style1.setLineSpacing(3);
        Style style2 = new Style();
        style2.setLineSpacing(2);
        Style style3 = new Style();
        StyleStack stack = new StyleStack();
        stack.push(style1);
        stack.push(style2);
        stack.push(style3);
        // Find the inner most style with lineSpacing not set to zero.
        assertEquals(stack.getLineSpacing(), 2);
        // Prove that it changes as value of lineSpacing on the styles change
        style3.setLineSpacing(1);
        assertEquals(stack.getLineSpacing(), 1);
        // Show that value 0 is responsible
        style3.setLineSpacing(0);
        assertEquals(stack.getLineSpacing(), 2);
        // Show it is not the lowest non-zero value
        style2.setLineSpacing(4);
        assertEquals(stack.getLineSpacing(), 4);
    }

    @Test
    public void findLineSpacingForEmptyStack() {
        StyleStack stack = new StyleStack();
        assertEquals(stack.getLineSpacing(), 1);
    }

    @Test
    public void findLineSpacingWhenNoStyleDefinesIt() {
        Style s1 = new Style();
        Style s2 = new Style();
        Style s3 = new Style();
        StyleStack stack = new StyleStack();
        stack.push(s1);
        stack.push(s2);
        stack.push(s3);
        assertEquals(stack.getLineSpacing(), 1);
        // Show that even once explicitly set to zero in the styles this default is still used.
        // IE. lineSpacing=0 leads to fall through along the stack.
        s1.setLineSpacing(0);
        s2.setLineSpacing(0);
        s3.setLineSpacing(0);
        assertEquals(stack.getLineSpacing(), 1);
    }

    @Test
    public void findSkipNumberLines() {
        NumberLinePosition position = NumberLinePosition.NONE;
        assertEquals(stack.getSkipNumberLines(), position);

        for (Style style : styles) {
            stack.push(style);
            if (!style.getSkipNumberLines().equals(NumberLinePosition.NONE)) {
                position = style.getSkipNumberLines();
            }
            assertEquals(stack.getSkipNumberLines(), position);
        }

        for (int i = styles.length - 1; i >= 0; i--) {
            int j = i;
            do {
                position = styles[j].getSkipNumberLines();
                j--;
            } while (position == NumberLinePosition.NONE && j >= 0);
            assertEquals(stack.getSkipNumberLines(), position);
            stack.pop();
        }

        assertEquals(stack.getSkipNumberLines(), NumberLinePosition.NONE);
    }

    /**
     * Test both StyleStack's equals (special since it extends a List) and Style's
     */
    @Test
    public void equalsAndHashCodeTest() {
        StyleStack stack = makeTestStack();
        assertNotEquals(new StyleStack(), stack);
        assertEquals(makeTestStack(), stack);
        assertEquals(stack.hashCode(), makeTestStack().hashCode());

        Style style1 = new Style();
        Style style2 = new Style();
        style2.setLinesBefore(2);
        Style style3 = new Style();
        StyleStack stack1 = new StyleStack();
        StyleStack stack2 = new StyleStack();
        stack1.push(style1);
        stack2.push(style2);
        stack1.push(style3);
        stack2.push(style3);

        assertNotEquals(stack2, stack1);
    }
    private static StyleStack makeTestStack() {
        Style style = new Style();
        style.setFirstLineIndent(20);
        style.setLinesAfter(12);

        Style style2 = new Style();
        style2.setFirstLineIndent(98);
        style2.setLinesAfter(50);

        StyleStack stack = new StyleStack("stack");
        StyleStack stack1nested = new StyleStack("stackNested");
        stack1nested.add(new Style(style, "stackNestedStyle-1", "stackNestedStyle-1"));
        stack1nested.add(new Style(style2, "stackNestedStyle-2", "stackNestedStyle-2"));
        stack1nested.add(new Style(style, "stackNestedStyle-3", "stackNestedStyle-3"));
        stack.add(stack1nested);
        stack.add(new Style(style2, "stackStyle-1", "stackStyle-1"));
        stack.add(new Style(style, "stackStyle-2", "stackStyle-2"));
        return stack;
    }

    public void testGuideWords() {
        assertFalse(stack.isGuideWords());
        for (Style style : styles) {
            stack.push(style);
            assertEquals(stack.isGuideWords(), style.isGuideWords());
        }

        for (int i = styles.length - 1; i >= 0; i--) {
            assertEquals(stack.isGuideWords(), styles[i].isGuideWords());
            stack.pop();
        }

        assertFalse(stack.isGuideWords());
    }

    public void testPageNum() {
        assertFalse(stack.isPageNum());
        for (Style style : styles) {
            stack.push(style);
            assertEquals(stack.isPageNum(), style.isPageNum());
        }

        for (int i = styles.length - 1; i >= 0; i--) {
            assertEquals(stack.isPageNum(), styles[i].isPageNum());
            stack.pop();
        }

        assertFalse(stack.isPageNum());
    }

}

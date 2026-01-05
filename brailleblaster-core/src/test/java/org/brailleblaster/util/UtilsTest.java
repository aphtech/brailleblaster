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
package org.brailleblaster.util;

import nu.xom.Node;
import nu.xom.Text;
import org.brailleblaster.bbx.BBX;
import org.brailleblaster.bbx.BBXUtils;
import org.brailleblaster.testrunners.BBXDocFactory;
import org.brailleblaster.utd.internal.xml.FastXPath;
import org.testng.Assert;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import java.util.stream.StreamSupport;

public class UtilsTest {
    @Test
    public void removeRegionStringTest() {
        assertEquals(Utils.removeRegionString(1, 2, "Hello"), "Hllo");
        assertEquals(Utils.removeRegionString(1, 3, "Hello"), "Hlo");
        assertEquals(Utils.removeRegionString(1, 4, "Hello"), "Ho");
        assertEquals(Utils.removeRegionString(1, 5, "Hello"), "H");
        assertEquals(Utils.removeRegionString(0, 5, "Hello"), "");
        assertEquals(Utils.removeRegionString(0, 4, "Hello"), "o");
        assertEquals(Utils.removeRegionString(0, 3, "Hello"), "lo");
        assertEquals(Utils.removeRegionString(0, 2, "Hello"), "llo");
        assertEquals(Utils.removeRegionString(0, 1, "Hello"), "ello");
        assertEquals(Utils.removeRegionString(0, 0, "Hello"), "Hello");
    }

    @Test
    public void removeRegionRangeStringTest() {
        assertEquals(Utils.removeRegionRangeString(1, 1, "Hello"), "Hllo");
        assertEquals(Utils.removeRegionRangeString(1, 2, "Hello"), "Hlo");
        assertEquals(Utils.removeRegionRangeString(1, 3, "Hello"), "Ho");
        assertEquals(Utils.removeRegionRangeString(1, 4, "Hello"), "H");
        assertEquals(Utils.removeRegionRangeString(0, 5, "Hello"), "");
        assertEquals(Utils.removeRegionRangeString(0, 4, "Hello"), "o");
        assertEquals(Utils.removeRegionRangeString(0, 3, "Hello"), "lo");
        assertEquals(Utils.removeRegionRangeString(0, 2, "Hello"), "llo");
        assertEquals(Utils.removeRegionRangeString(0, 1, "Hello"), "ello");
        assertEquals(Utils.removeRegionRangeString(0, 0, "Hello"), "Hello");
    }

    @Test
    public void pageNumTest() {
        BBXDocFactory docFactory = new BBXDocFactory()
                .append(BBX.CONTAINER.OTHER.create(), child -> child
                        .append(BBX.BLOCK.DEFAULT.create(), child2 -> child2
                                .append(BBX.SPAN.PAGE_NUM.create(), child3 -> child3
                                        .append("ii")
                                )
                        ));

        Node textNode = StreamSupport.stream(((Iterable<Node>)FastXPath.descendant(docFactory.root)::iterator).spliterator(), false)
                .filter(node -> node instanceof Text)
                .findFirst()
                .get();
        Assert.assertTrue(BBXUtils.isPageNumAncestor(textNode));
        Assert.assertTrue(BBXUtils.isPageNumAncestor(textNode.getParent()));
    }
}

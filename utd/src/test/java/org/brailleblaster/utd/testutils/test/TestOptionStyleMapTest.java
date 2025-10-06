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
package org.brailleblaster.utd.testutils.test;

import java.io.StringReader;
import java.util.stream.StreamSupport;

import nu.xom.Document;
import nu.xom.Element;
import org.brailleblaster.utd.IStyle;
import org.brailleblaster.utd.Style;
import org.brailleblaster.utd.internal.xml.FastXPath;
import org.brailleblaster.utd.internal.xml.XMLHandler;
import org.brailleblaster.utd.testutils.TestOptionStyleMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestOptionStyleMapTest {
    private static final Logger log = LoggerFactory.getLogger(TestOptionStyleMapTest.class);
    private static final TestOptionStyleMap MAP = new TestOptionStyleMap();

    @Test
    public void basicStyleTest() {
        Document doc = new XMLHandler().load(new StringReader("<root><p linesBefore='20' format='TOC'/></root>"));
        log.trace("yay");

        Element pTag = StreamSupport.stream(FastXPath.descendant(doc).spliterator(), false)
                .filter(curNode -> curNode instanceof Element && ((Element) curNode).getLocalName().equals("p"))
                .findFirst()
                .map(node -> (Element) node)
                .get();

        Style givenStyle = (Style) MAP.findValueOrDefault(pTag);
        Assert.assertNotNull(givenStyle, "Style is null");
        Assert.assertNotEquals(givenStyle.getName(), "DEFAULT");
        Assert.assertEquals(givenStyle.getLinesBefore(), 20);
        Assert.assertEquals(givenStyle.getFormat(), IStyle.Format.TOC);
    }
}

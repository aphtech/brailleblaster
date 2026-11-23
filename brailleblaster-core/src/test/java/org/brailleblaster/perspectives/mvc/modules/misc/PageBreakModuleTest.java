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
package org.brailleblaster.perspectives.mvc.modules.misc;

import org.brailleblaster.GuiTest;
import static org.brailleblaster.TestUtils.getInnerSection;

import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.eclipse.swt.SWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import nu.xom.Document;
import org.brailleblaster.bbx.utd.BBXDynamicOptionStyleMap;

@GuiTest
public class PageBreakModuleTest {
    private static final Logger log = LoggerFactory.getLogger(PageBreakModuleTest.class);
    private final String para1 = "First Paragraph";
    private final String para2 = "Second Paragraph";
    private final Document doc = TestXMLUtils.generateBookDoc("",
            "<p>" + para1 + "</p>" +
                    "<p>" + para2 + "</p>");

    @Test(enabled = false)
    public void pageBreakBeginningOfBlock() {
        BBTestRunner test = new BBTestRunner(doc);
        test.textViewTools.navigateToText(para2);
        test.textViewTools.pressShortcut(SWT.CTRL, SWT.CR);
        log.debug(test.getDoc().toXML());
        getInnerSection(test)
                .nextChildIs(c -> c.isBlockWithStyle("Body Text")
                        .nextChildIsText(para1))
                .nextChildIs(c -> c.hasBBNamespaceAttribute(BBXDynamicOptionStyleMap.OPTION_ATTRIB_PREFIX + "newPagesBefore", "1")
                        .nextChildIsText(para2));
    }

    @Test(enabled = false)
    public void pageBreakEndOfBlock() {

    }

    @Test(enabled = false)
    public void pageBreakMiddleOfBlock() {

    }
}

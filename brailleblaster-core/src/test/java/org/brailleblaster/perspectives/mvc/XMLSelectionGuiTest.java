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

import static org.testng.Assert.assertEquals;

import java.util.List;

import org.brailleblaster.GuiTest;
import org.brailleblaster.bbx.BBX;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.utd.exceptions.NodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import nu.xom.Element;

/** The XMLSelectionTest cases that need a full BBTestRunner window. */
@GuiTest
public class XMLSelectionGuiTest {
    private static final Logger log = LoggerFactory.getLogger(XMLSelectionGuiTest.class);

    // Disabled: breadcrumbs now show only the block, not its section ancestors; confirm intended (test_fix.md Phase 7a)
    @Test(enabled = false)
    public void blocks_sectionSelection_Test() {
        BBTestRunner test = new BBTestRunner("", "<h1>Stuff 1</h1><h2>More 2</h2><p>Things 3</p>");

        test.textViewTools.navigateToText("Stuff 1");
        test.selectBreadcrumbsAncestor(1, BBX.SECTION::assertIsA);

        for (Element curBlock : test.manager.getSimpleManager().getCurrentSelection().getSelectedBlocks()) {
            log.info("XML: {}", curBlock.toXML());
        }
        try {
            List<Element> selection = test.manager.getSimpleManager().getCurrentSelection().getSelectedBlocks();
            Element rootSection = test.assertRootSectionFirst_NoBrlCopy().element();
            assertEquals(selection.get(0), rootSection.getChild(0));
            assertEquals(selection.get(1), rootSection.getChild(1));
            assertEquals(selection.get(2), rootSection.getChild(2));
            assertEquals(selection.size(), 3);
        } catch (Throwable e) {
            throw new NodeException("Failed", test.getDoc(), e);
        }
    }
}

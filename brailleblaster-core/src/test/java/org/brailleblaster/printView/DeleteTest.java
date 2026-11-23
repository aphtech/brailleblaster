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
package org.brailleblaster.printView;

import org.brailleblaster.GuiTest;
import java.io.File;

import org.brailleblaster.frontmatter.TOCBuilderBBX;
import org.brailleblaster.frontmatter.TOCBuilderTest;
import org.brailleblaster.testrunners.BBTestRunner;
import org.eclipse.swtbot.swt.finder.keyboard.Keystrokes;
import org.testng.annotations.Test;

@GuiTest
public class DeleteTest {
    // Disabled: NPE in TOCBuilderBBX.isEnabled, module not registered (Phase 8 TOC) (test_fix.md Phase 7a)
    @Test(enabled = false)
    public void deleteToc_issue6438() {
        BBTestRunner test = new BBTestRunner("", "<p>test 1</p><p>other 2</p>");
        TOCBuilderTest.openTocTools(test);

        test.textViewTools.navigateToText("test");
        test.clickButtonWithId(TOCBuilderBBX.SWTBOT_TOC_ENTRY_BUTTON);
        test.textViewTools.selectFromTo("test", "1");
        test.textViewTools.pressShortcut(Keystrokes.DELETE);
    }

    // Disabled: fixture printView/RT6497.bbx was never committed to this repo; needs a new document (test_fix.md Phase 7a)
    @Test(enabled = false)
    public void deleteSection_rt6497() {
        BBTestRunner test = new BBTestRunner(new File("src/test/resources/org/brailleblaster/printView/RT6497.bbx"));

        test.textViewTools.selectFromTo("Everyday", "UNIT 8 Perimeter and Area");
        test.textViewTools.pressShortcut(Keystrokes.DELETE);
    }
}

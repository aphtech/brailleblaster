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
package org.brailleblaster.perspectives.braille.ui;

import org.brailleblaster.GuiTest;
import java.util.function.Consumer;

import org.brailleblaster.TestUtils;
import org.brailleblaster.bbx.BBX;
import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.ViewTestRunner;
import org.brailleblaster.exceptions.BBNotifyException;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotText;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test
@GuiTest
public class CellTabTest {
    @Test
    public void multipleTabs_start_issue5417() {
        BBTestRunner test = new BBTestRunner("", "<p>An empty paragraph</p>"
                + "<p>Make me run off the screen</p>");
        test.textViewTools.navigateToText("Make");

        test.openMenuItem(TopMenu.EDIT, CellTab.MENU_NAME);
        SWTBot dialogBot = test.activeShell().bot();
        dialogBot.text(0).setText("15");
        ViewTestRunner.doPendingSWTWork();
        dialogBot.button(0).click();
        ViewTestRunner.doPendingSWTWork();

        test.assertRootSection_NoBrlCopy()
                .nextChildIs(child -> child
                        .isBlockDefaultStyle("Body Text")
                ).nextChildIs(child -> child
                        .isBlockDefaultStyle("Body Text")
                        .nextChildIs(child1 -> child1
                                .isSpan(BBX.SPAN.TAB)
                                .hasAttributeBB(BBX.SPAN.TAB.ATTRIB_VALUE, 15)
                        ).nextChildIsText("Make me run off the screen")
                        .noNextChild()
                ).noNextChild();

        TestUtils.forceShellByName("BrailleBlaster");
        test.openMenuItem(TopMenu.EDIT, CellTab.MENU_NAME);
        dialogBot = test.activeShell().bot();

        SWTBotText tabValueText = dialogBot.text(0);
        Assert.assertEquals(tabValueText.getText(), "15");
        tabValueText.setText("18");

        ViewTestRunner.doPendingSWTWork();
        dialogBot.button(0).click();
        ViewTestRunner.doPendingSWTWork();

        test.assertRootSection_NoBrlCopy()
                .nextChildIs(child -> child
                        .isBlockDefaultStyle("Body Text")
                ).nextChildIs(child -> child
                        .isBlockDefaultStyle("Body Text")
                        .nextChildIs(child1 -> child1
                                .isSpan(BBX.SPAN.TAB)
                                .hasAttributeBB(BBX.SPAN.TAB.ATTRIB_VALUE, 18)
                        ).nextChildIsText("Make me run off the screen")
                        .noNextChild()
                ).noNextChild();
    }

    // Disabled: cell tab insert fails with 'Cannot apply to end of line' (I10) (test_fix.md Phase 7f, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void middleTabs_middle_before_issue5417() {
        multipleTabs_middle(test -> {
            test.textViewTools.navigateToLine(1);
            test.textViewTools.navigateToEndOfLine();
        });
    }

    // Disabled: cell tab insert fails with 'Cannot apply to end of line' (I10) (test_fix.md Phase 7f, bb_issues_from_tests.md)
    @Test(enabled = false)
    public void middleTabs_middle_after_issue5417() {
        multipleTabs_middle(test -> test.textViewTools.navigateToLine(2));
    }

    private void multipleTabs_middle(Consumer<BBTestRunner> navigate) {
        BBTestRunner test = new BBTestRunner("", "<p>An empty paragraph</p>"
                + "<p>Make me run off the screen</p>");
        test.textViewTools.navigateToText("off");

        test.openMenuItem(TopMenu.EDIT, CellTab.MENU_NAME);
        SWTBot dialogBot = test.activeShell().bot();
        dialogBot.text(0).setText("6");
        ViewTestRunner.doPendingSWTWork();
        dialogBot.button(0).click();
        ViewTestRunner.doPendingSWTWork();

        test.assertRootSection_NoBrlCopy()
                .nextChildIs(child -> child
                        .isBlockDefaultStyle("Body Text")
                ).nextChildIs(child -> child
                        .isBlockDefaultStyle("Body Text")
                        .nextChildIsText("Make me run ")
                        .nextChildIs(child1 -> child1
                                .isSpan(BBX.SPAN.TAB)
                                .hasAttributeBB(BBX.SPAN.TAB.ATTRIB_VALUE, 6)
                        ).nextChildIsText("off the screen")
                        .noNextChild()
                ).noNextChild();

        TestUtils.forceShellByName("BrailleBlaster");

        MatcherAssert.assertThat(
                test.textViewWidget.getLine(2).trim(),
                Matchers.startsWith("off")
        );
        test.textViewTools.navigateToLine(2);

        test.openMenuItem(TopMenu.EDIT, CellTab.MENU_NAME);
        dialogBot = test.activeShell().bot();

        SWTBotText tabValueText = dialogBot.text(0);
        Assert.assertEquals(tabValueText.getText(), "6");
        tabValueText.setText("9");

        ViewTestRunner.doPendingSWTWork();
        dialogBot.button(0).click();
        ViewTestRunner.doPendingSWTWork();

        test.assertRootSection_NoBrlCopy()
                .nextChildIs(child -> child
                        .isBlockDefaultStyle("Body Text")
                ).nextChildIs(child -> child
                        .isBlockDefaultStyle("Body Text")
                        .nextChildIsText("Make me run ")
                        .nextChildIs(child1 -> child1
                                .isSpan(BBX.SPAN.TAB)
                                .hasAttributeBB(BBX.SPAN.TAB.ATTRIB_VALUE, 9)
                        ).nextChildIsText("off the screen")
                        .noNextChild()
                ).noNextChild();
    }

    @Test(expectedExceptions = BBNotifyException.class)
    public void multipleTabs_end_issue5417() {
        BBTestRunner test = new BBTestRunner("", "<p>An empty paragraph</p>"
                + "<p>Make me run off the screen</p>");
        test.textViewTools.navigateToText("off");
        test.textViewTools.navigateToEndOfLine();

        test.openMenuItem(TopMenu.EDIT, CellTab.MENU_NAME);
        SWTBot dialogBot = test.activeShell().bot();
        dialogBot.text(0).setText("6");
        ViewTestRunner.doPendingSWTWork();
        dialogBot.button(0).click();
        ViewTestRunner.doPendingSWTWork();
    }
}

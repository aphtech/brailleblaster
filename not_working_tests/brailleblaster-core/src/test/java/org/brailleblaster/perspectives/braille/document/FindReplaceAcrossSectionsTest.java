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
package org.brailleblaster.perspectives.braille.document;

import org.brailleblaster.GuiTest;
import static org.testng.Assert.assertEquals;

import java.io.File;

import org.brailleblaster.TestFiles;
//import org.brailleblaster.TestGroups;
import org.brailleblaster.TestUtils;
import org.brailleblaster.testrunners.BBTestRunner;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

//@Test(groups = TestGroups.SLOW_TESTS)
@GuiTest
public class FindReplaceAcrossSectionsTest {

    private final static Logger log = LoggerFactory.getLogger(FindReplaceAcrossSectionsTest.class);

    @Test(enabled = false)
    public void annoyingTestThatHasToBeRunFirstCalledReplaceAttributionsCollections() {
        BBTestRunner bb = new BBTestRunner(new File(TestFiles.collections));

        FindReplaceBotHelper.openDialog(bb);
        FindAndReplaceTest.cleanSettings(TestUtils.refreshReturnActiveBot(bb), bb);

        FindReplaceBotHelper.clickFormatting(bb);
        FindReplaceBotHelper.addStylesAndContainers(bb);
        TestUtils.refreshReturnActiveBot(bb).list(0).select("Attribution");
        FindReplaceBotHelper.modifyFind(bb);
        FindReplaceBotHelper.addStylesAndContainers(bb);
        TestUtils.refreshReturnActiveBot(bb).list(0).select("Centered Heading");
        FindReplaceBotHelper.modifyReplace(bb);
        FindReplaceBotHelper.done(bb);

        int wordCount = 0;
        int iterations = 0;
        int tableCount = 0;
        FindAndReplaceTest.REPLACE_OUTCOME outcome;
        while (iterations < 20) {
            iterations++;
            outcome = FindAndReplaceTest.clickReplace(bb);
            log.error(outcome.name());
            if (outcome.equals(FindAndReplaceTest.REPLACE_OUTCOME.EXCEPTION)) {
                tableCount++;
            } else if (outcome.equals(FindAndReplaceTest.REPLACE_OUTCOME.SUCCESS)) {
                wordCount++;
            } else if (outcome.equals(FindAndReplaceTest.REPLACE_OUTCOME.END_DOC)) {
                break;
            }
        }
        assertEquals(wordCount + tableCount, 6);
    }

    @Test(enabled = false)
    public void findAcrossSectionsLitBook() {
        BBTestRunner bb = new BBTestRunner(new File(TestFiles.litBook));
        FindReplaceBotHelper.openDialog(bb);
        FindAndReplaceTest.cleanSettings(TestUtils.refreshReturnActiveBot(bb), bb);
        TestUtils.refreshReturnActiveBot(bb).comboBox(0).setText("paintings");
        int wordCount = 0;
        int iterations = 0;
        FindAndReplaceTest.FIND_OUTCOME outcome;
        while (iterations < 20) {
            iterations++;
            outcome = FindAndReplaceTest.clickFind(bb);
            log.error(outcome.name());
            if (outcome.equals(FindAndReplaceTest.FIND_OUTCOME.SUCCESS)) {
                wordCount++;
            } else if (outcome.equals(FindAndReplaceTest.FIND_OUTCOME.END_DOC)) {
                break;
            }
        }
        assertEquals((wordCount), 5);
    }

    @Test(enabled = false)
    public void replaceAcrossSectionsLitBook() {

        BBTestRunner bb = new BBTestRunner(new File(TestFiles.litBook));
        FindReplaceBotHelper.openDialog(bb);
        FindAndReplaceTest.cleanSettings(TestUtils.refreshReturnActiveBot(bb), bb);
        TestUtils.refreshReturnActiveBot(bb).comboBox(0).setText("paintings");
        TestUtils.refreshReturnActiveBot(bb).comboBox(1).setText("success");
        int wordCount = 0;
        int iterations = 0;
        int tableCount = 0;
        FindAndReplaceTest.REPLACE_OUTCOME outcome;
        while (iterations < 20) {
            iterations++;
            outcome = FindAndReplaceTest.clickReplace(bb);
            log.error(outcome.name());
            if (outcome.equals(FindAndReplaceTest.REPLACE_OUTCOME.EXCEPTION)) {
                tableCount++;
            } else if (outcome.equals(FindAndReplaceTest.REPLACE_OUTCOME.SUCCESS)) {
                wordCount++;
            } else if (outcome.equals(FindAndReplaceTest.REPLACE_OUTCOME.END_DOC)) {
                break;
            }
        }
        assertEquals((wordCount + tableCount), 5);
    }

    @Test(enabled = false)
    public void findAcrossSectionsCollections() {
        BBTestRunner bb = new BBTestRunner(new File(TestFiles.collections));
        FindReplaceBotHelper.openDialog(bb);
        FindAndReplaceTest.cleanSettings(TestUtils.refreshReturnActiveBot(bb), bb);

        TestUtils.refreshReturnActiveBot(bb).comboBox(0).setText("teacher");
        int wordCount = 0;
        int iterations = 0;
        FindAndReplaceTest.FIND_OUTCOME outcome;
        while (iterations < 50) {
            iterations++;
            outcome = FindAndReplaceTest.clickFind(bb);
            log.error(outcome.name());
            if (outcome.equals(FindAndReplaceTest.FIND_OUTCOME.SUCCESS)) {
                wordCount++;
            } else if (outcome.equals(FindAndReplaceTest.FIND_OUTCOME.END_DOC)) {
                break;
            }
        }
        assertEquals((wordCount), 39);
    }

    @Test(enabled = false)
    public void findBoldOnly() {
        BBTestRunner bb = new BBTestRunner(new File(TestFiles.collections));
        FindReplaceBotHelper.openDialog(bb);
        FindAndReplaceTest.cleanSettings(TestUtils.refreshReturnActiveBot(bb), bb);

        FindReplaceBotHelper.clickFormatting(bb);
        TestUtils.refreshReturnActiveBot(bb).list(2).select("BOLD");
        FindReplaceBotHelper.modifyFind(bb);
        FindReplaceBotHelper.done(bb);
        TestUtils.refreshReturnActiveBot(bb).comboBox(0).setText("exploitation");
        int wordCount = 0;
        int iterations = 0;
        FindAndReplaceTest.FIND_OUTCOME outcome;
        while (iterations < 20) {
            iterations++;
            outcome = FindAndReplaceTest.clickFind(bb);
            log.error(outcome.name());
            if (outcome.equals(FindAndReplaceTest.FIND_OUTCOME.SUCCESS)) {
                wordCount++;
//				System.out.println(bb.manager.getMapList().getCurrent().getText()+" word count "+wordCount);
            } else if (outcome.equals(FindAndReplaceTest.FIND_OUTCOME.END_DOC)) {
                break;
            } else if (outcome.equals(FindAndReplaceTest.FIND_OUTCOME.WRONG_SHELL)) {
                break;
            }
        }
        assertEquals((wordCount), 3);
    }

    @Test(enabled = false)
    public void findUnBoldOnly() {
        BBTestRunner bb = new BBTestRunner(new File(TestFiles.collections));
        FindReplaceBotHelper.openDialog(bb);
        FindAndReplaceTest.cleanSettings(TestUtils.refreshReturnActiveBot(bb), bb);

        FindReplaceBotHelper.clickFormatting(bb);
        FindReplaceBotHelper.removeTextAttributes(bb);
        TestUtils.refreshReturnActiveBot(bb).list(2).select("BOLD");
        FindReplaceBotHelper.modifyFind(bb);
        FindReplaceBotHelper.done(bb);
        TestUtils.refreshReturnActiveBot(bb).comboBox(0).setText("opportunity");
        int wordCount = 0;
        int iterations = 0;
        FindAndReplaceTest.FIND_OUTCOME outcome;
        while (iterations < 20) {
            iterations++;
            outcome = FindAndReplaceTest.clickFind(bb);
            log.error(outcome.name());
            if (outcome.equals(FindAndReplaceTest.FIND_OUTCOME.SUCCESS)) {
                wordCount++;
            } else if (outcome.equals(FindAndReplaceTest.FIND_OUTCOME.END_DOC)) {
                break;
            }
        }
        assertEquals((wordCount), 11);
    }

    @Test(enabled = false)
    public void replaceAcrossSectionsCollections() {

        BBTestRunner bb = new BBTestRunner(new File(TestFiles.collections));
        FindReplaceBotHelper.openDialog(bb);
        SWTBot bot = TestUtils.refreshReturnActiveBot(bb);
        bot = FindAndReplaceTest.cleanSettings(bot, bb);

        bot.comboBox(0).setText("garbage");
        bot.comboBox(1).setText("success");
        int wordCount = 0;
        int tableCount = 0;
        int iterations = 0;
        FindAndReplaceTest.REPLACE_OUTCOME outcome;
        while (iterations < 20) {
            iterations++;
            outcome = FindAndReplaceTest.clickReplace(bb);
            log.error(outcome.name());
            if (outcome.equals(FindAndReplaceTest.REPLACE_OUTCOME.EXCEPTION)) {
                tableCount++;
            } else if (outcome.equals(FindAndReplaceTest.REPLACE_OUTCOME.SUCCESS)) {
                wordCount++;
            } else if (outcome.equals(FindAndReplaceTest.REPLACE_OUTCOME.END_DOC)) {
                break;
            }
        }
        assertEquals((wordCount + tableCount), 6);
        /*
         * 6 instances of garbage plus one more click to find before
         * replace
         */
    }

}

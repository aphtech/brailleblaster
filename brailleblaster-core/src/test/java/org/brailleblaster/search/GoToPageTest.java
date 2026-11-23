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
package org.brailleblaster.search;

import org.brailleblaster.GuiTest;
import nu.xom.Element;
import org.brailleblaster.TestFiles;
import org.brailleblaster.TestGroups;
import org.brailleblaster.bbx.BBX;
import org.brailleblaster.frontmatter.VolumeTest;
import org.brailleblaster.frontmatter.VolumeUtils;
import org.brailleblaster.perspectives.braille.mapping.elements.*;
import org.brailleblaster.perspectives.braille.views.wp.PageNumberDialog;
import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.ViewTestRunner;
import org.brailleblaster.utd.exceptions.NodeException;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.keyboard.Keystrokes;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.List;

@GuiTest
public class GoToPageTest {
    private static final Logger log = LoggerFactory.getLogger(GoToPageTest.class);

    @Test(groups = TestGroups.SLOW_TESTS, enabled = false)
    public void goToAll_simple() {
//		BBTestRunner bbTest = new BBTestRunner(new File("/home/leon/projects/aph/linuxdev/Combined Pages 14-15.bbx"));
        BBTestRunner bbTest = new BBTestRunner(new File(TestFiles.litBook));
        visitPages(bbTest, false);
    }

    @Test(groups = TestGroups.SLOW_TESTS, enabled = false)
    public void goToAll_volumes() {
//		BBTestRunner bbTest = new BBTestRunner(new File("/home/leon/projects/aph/linuxdev/Combined Pages 14-15.bbx"));
        BBTestRunner bbTest = new BBTestRunner(new File(TestFiles.litBook));

        for (int i = 1; i < 15; i++) {
            goToPage(bbTest, GoToPageDialog.PageType.ORDINAL, "" + (i * 100), false);
            bbTest.updateTextView();
            bbTest.textViewTools.pressKey(SWT.ARROW_DOWN, 2);

            BBX.VolumeType volumeType;
            if (i < 6) {
                volumeType = BBX.VolumeType.VOLUME_PRELIMINARY;
            } else if (i < 10) {
                volumeType = BBX.VolumeType.VOLUME;
            } else {
                volumeType = BBX.VolumeType.VOLUME_SUPPLEMENTAL;
            }
            VolumeTest.openInsertVolume(bbTest, volumeType);
        }

        visitPages(bbTest, true);
    }

    @Test(enabled = false)
    public void goToCombinedPage() {
        BBTestRunner test = new BBTestRunner(new File("./src/tests/resources/nimasbaseline/NIMASXMLGtDepJan2009_valid3.xml"));

        goToPage(test, GoToPageDialog.PageType.PRINT, "3", false);
        deletePageIndicator(test, "3");
        goToPage(test, GoToPageDialog.PageType.PRINT, "4", false);
        test.textViewTools.pressShortcut(Keystrokes.DOWN);
        deletePageIndicator(test, "4");

        test.textViewTools.clickContextMenu(PageNumberDialog.MENU_NAME);

        SWTBot pageNumBot = test.bot.activeShell().bot();
        SWTBotStyledText printPageNumberText = pageNumBot.styledTextWithId(PageNumberDialog.SWTBOT_PRINT_PAGE_NUMBER_TEXT);
        Assert.assertEquals(printPageNumberText.getText(), "2");
        printPageNumberText.setText("2-5");
        ViewTestRunner.doPendingSWTWork();
        pageNumBot.buttonWithId(PageNumberDialog.SWTBOT_OK_BUTTON).click();
        ViewTestRunner.doPendingSWTWork();

        goToPage(test, GoToPageDialog.PageType.PRINT, "1", false);

        goToPage(test, GoToPageDialog.PageType.PRINT, "2", false);

        //todo: issue #4660: middle pages
        expectException(() -> {
            SWTBot goToPageBot = goToPage(test, GoToPageDialog.PageType.PRINT, "3", false);
            goToPageBot.activeShell().close();
        });
        expectException(() -> {
            SWTBot goToPageBot = goToPage(test, GoToPageDialog.PageType.PRINT, "4", false);
            goToPageBot.activeShell().close();
        });

        goToPage(test, GoToPageDialog.PageType.PRINT, "5", false);
    }

    public SWTBot visitPages(BBTestRunner bbTest, boolean selectVolume) {
        for (SectionElement curSection : bbTest.manager.getSectionList()) {
            for (TextMapElement curMapping : curSection.list) {
                if (curMapping instanceof PageIndicatorTextMapElement) {
                    Element usableNode = (Element) (curMapping.getNode() instanceof Element
                            ? curMapping.getNode()
                            : curMapping.getNodeParent());
                    String brlPageNum = usableNode.getAttributeValue("printPage");
                    if (brlPageNum != null) {
                        System.out.println("PageMapElement: " + brlPageNum);
                        return goToPage(bbTest, GoToPageDialog.PageType.PRINT, brlPageNum, selectVolume);
                    } else {
                        throw new RuntimeException("uhh...");
                    }
                } else {
                    for (BrailleMapElement curBrailleElement : curMapping.brailleList) {
                        try {
                            if (curBrailleElement instanceof PrintPageBrlMapElement) {
                                //Check both the origional page and the translated braille equivelent
                                String origPage = ((Element) curBrailleElement.getNode()).getAttributeValue("printPage");
                                String braillePage = curBrailleElement.getNode().getValue();

                                System.out.println("PrintPageMapElement: " + origPage + " - " + braillePage);
                                return goToPage(bbTest, GoToPageDialog.PageType.PRINT, origPage, /*can only select braille*/false);
                            } else if (curBrailleElement instanceof BraillePageBrlMapElement) {
                                String page = ((Element) curBrailleElement.getNode()).getAttributeValue("untranslated");

                                System.out.println("BraillePageMapElement: " + page + " " + (curBrailleElement.getNode()).toXML());
                                return goToPage(bbTest, GoToPageDialog.PageType.BRAILLE, page, selectVolume);
                            }
                        } catch (Exception e) {
                            log.error("this is test", e);
                            ViewTestRunner.DEBUG_MODE();
                            throw new NodeException("fail", curBrailleElement.getNode(), e);
                        }
                    }
                }
            }
        }
        throw new RuntimeException("not found");
    }

    public static SWTBot goToPage(BBTestRunner bbTest, GoToPageDialog.PageType type, String page, boolean selectVolume) {
        bbTest.textViewBot.setFocus();

        bbTest.openMenuItem(TopMenu.NAVIGATE, "Go To Page");

        SWTBot bot = bbTest.bot.activeShell().bot();
        Assert.assertEquals(bot.activeShell().getText(), "Go To");

        if (selectVolume) {
            List<Element> volumeElements = VolumeUtils.getVolumeElements(bbTest.getDoc());
            int index = volumeElements.indexOf(bbTest.manager.getVolumeAtCursor());
            bot.comboBox(0).setSelection(index + 1);
            ViewTestRunner.doPendingSWTWork();
        }

        bot.text(0).setText(page);
        ViewTestRunner.doPendingSWTWork();
        bot.radio(type.ordinal()).click();
        ViewTestRunner.doPendingSWTWork();

        bot.button(0).click();
        ViewTestRunner.doPendingSWTWork();

        bbTest.updateViewReferences();
        bbTest.textViewBot.setFocus();
        ViewTestRunner.doPendingSWTWork();
        return bot;
    }

    public void deletePageIndicator(BBTestRunner test, String expectedPage) {
        test.textViewTools.clickContextMenu(PageNumberDialog.MENU_NAME);

        SWTBot pageNumBot = test.bot.activeShell().bot();
        Assert.assertEquals(
                pageNumBot.styledTextWithId(PageNumberDialog.SWTBOT_PRINT_PAGE_NUMBER_TEXT).getText(),
                expectedPage
        );
        pageNumBot.buttonWithId(PageNumberDialog.SWTBOT_DELETE_PRINT_INDICATOR).click();
        ViewTestRunner.doPendingSWTWork();
        pageNumBot.buttonWithId(PageNumberDialog.SWTBOT_OK_BUTTON).click();
        ViewTestRunner.doPendingSWTWork();
    }

    @FunctionalInterface
    public interface FailableRunnable {
        void run();
    }

    public void expectException(FailableRunnable run) {
        try {
            run.run();
            throw new AssertionError("expected failure");
        } catch (Exception e) {
            // yay
        }
    }
}

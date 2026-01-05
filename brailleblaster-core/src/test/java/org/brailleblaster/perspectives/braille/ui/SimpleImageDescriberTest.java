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

import nu.xom.Element;
import nu.xom.Node;
import org.brailleblaster.TestUtils;
import org.brailleblaster.bbx.BBX;
import org.brailleblaster.easierxml.SimpleImageDescriberDialog;
import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.BBXDocFactory;
import org.brailleblaster.testrunners.ViewTestRunner;
import org.brailleblaster.utd.exceptions.NodeException;
import org.brailleblaster.utd.internal.xml.FastXPath;
import org.brailleblaster.utd.internal.xml.XMLHandler;
import org.brailleblaster.utd.properties.EmphasisType;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotButton;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.brailleblaster.testrunners.ViewTestRunner.doPendingSWTWork;
import static org.testng.Assert.*;

@Test(enabled = false)
public class SimpleImageDescriberTest {

    private static final Logger log = LoggerFactory.getLogger(SimpleImageDescriberTest.class);

    private static final String dot1 = "\u2801";
    private static final String allDots = "\u283F";
    private static final String shellName = "Image Describer";

    private Event dot1Event() {
        Event e = new Event();
        e.character = 'f';
        return e;
    }

    private Event dot2Event() {
        Event e = new Event();
        e.character = 'd';
        return e;
    }

    private Event dot3Event() {
        Event e = new Event();
        e.character = 's';
        return e;
    }

    private Event dot4Event() {
        Event e = new Event();
        e.character = 'j';
        return e;
    }

    private Event dot5Event() {
        Event e = new Event();
        e.character = 'k';
        return e;
    }

    private Event dot6Event() {
        Event e = new Event();
        e.character = 'l';
        return e;
    }

    private void allDotsEvent(SWTBotStyledText describerTextBot) {
        /*
         * shortcut pressed events only notify verify listeners, not key
         * listeners, so I made key events.
         */
        describerTextBot.widget.notifyListeners(SWT.KeyDown, dot1Event());
        describerTextBot.widget.notifyListeners(SWT.KeyDown, dot2Event());
        describerTextBot.widget.notifyListeners(SWT.KeyDown, dot3Event());
        describerTextBot.widget.notifyListeners(SWT.KeyDown, dot4Event());
        describerTextBot.widget.notifyListeners(SWT.KeyDown, dot5Event());
        describerTextBot.widget.notifyListeners(SWT.KeyDown, dot6Event());

        describerTextBot.widget.notifyListeners(SWT.KeyUp, dot1Event());
        describerTextBot.widget.notifyListeners(SWT.KeyUp, dot2Event());
        describerTextBot.widget.notifyListeners(SWT.KeyUp, dot3Event());
        describerTextBot.widget.notifyListeners(SWT.KeyUp, dot4Event());
        describerTextBot.widget.notifyListeners(SWT.KeyUp, dot5Event());
        describerTextBot.widget.notifyListeners(SWT.KeyUp, dot6Event());
    }

    private void dot1Event(SWTBotStyledText describerTextBot) {
        describerTextBot.widget.notifyListeners(SWT.KeyDown, dot1Event());
        describerTextBot.widget.notifyListeners(SWT.KeyUp, dot1Event());
    }

    public static void openImageDescriber(BBTestRunner bbTest, boolean sixkey) {
        bbTest.openMenuItem(TopMenu.TOOLS, SimpleImageDescriberModule.MENU_ITEM_NAME);
        SWTBot describerBot = bbTest.bot.activeShell().bot();
        if (!sixkey) {
            describerBot.menu("Styled Text").click();
        }
    }

    @Test(enabled = false)
    public void paragraphsSingleInline() {
        BBTestRunner bbTest = new BBTestRunner(
                new BBXDocFactory().append(BBX.BLOCK.DEFAULT.create(),
                                blockFactory -> blockFactory.append("Text before")
                                        .append(BBX.SPAN.IMAGE.create(),
                                                childFactory -> childFactory.addAttribute(BBX.SPAN.IMAGE.ATTRIB_SOURCE,
                                                        "test.jpg"))
                                        .append("Text after"))
                        .append(BBX.BLOCK.DEFAULT.create(),
                                blockFactory -> blockFactory
                                        .append(BBX.SPAN.IMAGE.create(), childFactory -> childFactory
                                                .addAttribute(BBX.SPAN.IMAGE.ATTRIB_SOURCE, "test2.jpg"))
                                        .append("2Text after")));

        // Jump to first img tag
        bbTest.textViewTools.navigateToText("before");
        openImageDescriber(bbTest, false);
        ViewTestRunner.forceActiveShellHack();
        SWTBot describerBot = bbTest.bot.activeShell().bot();
        SWTBotStyledText describerTextBot = describerBot.styledText(0);

        describerTextBot.setText("This is a description");
        doPendingSWTWork();

        // TODO: Can't find widget if its not visible?
        // assertFalse(describerBot.buttonWithId(SimpleImageDescriberDialog.SWT_APPLY_ALL_BUTTON_ID).isVisible());
        describerBot.button(SimpleImageDescriberDialog.INLINE_ONE).click();
        doPendingSWTWork();

        bbTest.assertRootSection_NoBrlCopy()
                .nextChildIs(
                        childAssert -> childAssert.isBlockDefaultStyle("Body Text").nextChildIsText("Text before")
                                .nextChildIs(childAssert2 -> childAssert2
                                        .isSpan(BBX.SPAN.IMAGE).hasText("This is a description"))
                                .nextChildIsText("Text after").noNextChild())
                .nextChildIs(childAssert -> childAssert.isBlockDefaultStyle("Body Text")
                        .nextChildIs(childAssert2 -> childAssert2.isSpan(BBX.SPAN.IMAGE).childCount(0))
                        .nextChildIsText("2Text after").noNextChild())
                .noNextChild();
    }

    @Test(enabled = false)
    public void group_MultipleBlocks() {
        BBTestRunner bbTest = new BBTestRunner(new BBXDocFactory().append(BBX.CONTAINER.IMAGE.create(),
                groupFactory -> groupFactory.addAttribute(BBX.SPAN.IMAGE.ATTRIB_SOURCE, "test.jpg")
                        .append(BBX.BLOCK.DEFAULT.create(), blockFactory -> blockFactory.append("test block 1"))
                        .append(BBX.BLOCK.DEFAULT.create(), blockFactory -> blockFactory.append("test block 2"))));

        bbTest.textViewTools.navigateToText("block");
        openImageDescriber(bbTest, false);
        ViewTestRunner.forceActiveShellHack();
        SWTBot describerBot = bbTest.bot.activeShell().bot();
        SWTBotStyledText describerTextBot = describerBot.styledText(0);
        describerTextBot.setText("This is a description");
        doPendingSWTWork();

        describerBot.checkBox(SimpleImageDescriberDialog.CAPTION_STYLE_BOX).click();
        doPendingSWTWork();

        // TODO: Can't find widget if its not visible?
        // assertFalse(describerBot.buttonWithId(SimpleImageDescriberDialog.SWT_APPLY_ALL_BUTTON_ID).isVisible());
        describerBot.button(SimpleImageDescriberDialog.BLOCK_ONE).click();
        doPendingSWTWork();

        bbTest.assertRootSectionFirst_NoBrlCopy().isContainer(BBX.CONTAINER.IMAGE)
                .nextChildIs(childAssert -> childAssert.isBlockWithOverrideStyle("Caption").hasText("This is a description"))
                .noNextChild();
    }

    @Test(enabled = false)
    public void paragraphs_MultipleInstances() {
        BBTestRunner bbTest = new BBTestRunner(
                new BBXDocFactory()
                        .append(BBX.BLOCK.DEFAULT.create(),
                                blockFactory -> blockFactory.append("Text before")
                                        .append(BBX.SPAN.IMAGE.create(),
                                                childFactory -> childFactory.addAttribute(BBX.SPAN.IMAGE.ATTRIB_SOURCE,
                                                        "test.jpg"))
                                        .append("Text after"))
                        .append(BBX.BLOCK.DEFAULT.create(),
                                blockFactory -> blockFactory
                                        .append(BBX.SPAN.IMAGE.create(), childFactory -> childFactory
                                                .addAttribute(BBX.SPAN.IMAGE.ATTRIB_SOURCE, "test.jpg"))
                                        .append("2Text after")));

        // Jump to first img tag
        bbTest.textViewTools.navigateToText("before");
        openImageDescriber(bbTest, false);
        ViewTestRunner.forceActiveShellHack();
        SWTBot describerBot = bbTest.bot.activeShell().bot();
        SWTBotStyledText describerTextBot = describerBot.styledText(0);

        describerTextBot.setText("This is a description");
        doPendingSWTWork();

        assertTrue(describerBot.buttonWithId(SimpleImageDescriberDialog.SWTBOT_APPLY_ALL_BLOCK).isVisible());
        describerBot.button(SimpleImageDescriberDialog.makeAllString(true, "2")).click();
        doPendingSWTWork();

        bbTest.assertRootSection_NoBrlCopy()
                .nextChildIs(
                        childAssert -> childAssert.isBlockDefaultStyle("Body Text").nextChildIsText("Text before")
                                .nextChildIs(childAssert2 -> childAssert2.isSpan(BBX.SPAN.IMAGE)
                                        .hasText("This is a description"))
                                .nextChildIsText("Text after").noNextChild())
                .nextChildIs(
                        childAssert -> childAssert.isBlockDefaultStyle("Body Text")
                                .nextChildIs(childAssert2 -> childAssert2.isSpan(BBX.SPAN.IMAGE)
                                        .hasText("This is a description"))
                                .nextChildIsText("2Text after").noNextChild())
                .noNextChild();
    }

    @Test(enabled = false)
    public void paragraphsAndGroups() {
        BBTestRunner bbTest = new BBTestRunner(
                new BBXDocFactory()
                        .append(BBX.BLOCK.DEFAULT.create(),
                                blockFactory -> blockFactory.append("Text before")
                                        .append(BBX.SPAN.IMAGE.create(),
                                                childFactory -> childFactory.addAttribute(BBX.SPAN.IMAGE.ATTRIB_SOURCE,
                                                        "test.jpg"))
                                        .append("Text after"))
                        .append(BBX.CONTAINER.IMAGE.create(),
                                groupFactory -> groupFactory.addAttribute(BBX.SPAN.IMAGE.ATTRIB_SOURCE, "test.jpg")
                                        .append(BBX.BLOCK.DEFAULT.create(),
                                                blockFactory -> blockFactory.append("test block 1"))
                                        .append(BBX.BLOCK.DEFAULT.create(),
                                                blockFactory -> blockFactory.append("test block 2"))));

        // Jump to first img tag
        bbTest.textViewTools.navigateToText("before");
        openImageDescriber(bbTest, false);
        Shell[] shells = Display.getCurrent().getShells();
        for (Shell shell : shells) {
            log.error("shell " + shell.getText());
            if (shell.getText().equals("Image Describer")) {
                shell.forceActive();
                break;
            }
        }
        SWTBot describerBot = bbTest.bot.activeShell().bot();
        SWTBotStyledText describerTextBot = describerBot.styledText(0);

        describerTextBot.setText("This is a description");
        doPendingSWTWork();

        describerBot.button(SimpleImageDescriberDialog.makeAllString(true, "2")).click();
        doPendingSWTWork();

        bbTest.assertRootSection_NoBrlCopy()
                .nextChildIs(
                        childAssert -> childAssert.isBlockDefaultStyle("Body Text").nextChildIsText("Text before")
                                .nextChildIs(childAssert2 -> childAssert2.isSpan(BBX.SPAN.IMAGE)
                                        .hasText("This is a description"))
                                .nextChildIsText("Text after").noNextChild())
                .nextChildIs(childAssert -> childAssert.isContainer(BBX.CONTAINER.IMAGE).nextChildIs(
                                childAssert2 -> childAssert2.isBlockDefaultStyle("Caption").hasText("This is a description"))
                        .noNextChild())
                .noNextChild();
    }

    @Test(enabled = false)
    public void addDot1SpanAndContainer() {
        BBTestRunner bbTest = new BBTestRunner(
                new BBXDocFactory()
                        .append(BBX.BLOCK.DEFAULT.create(),
                                blockFactory -> blockFactory.append("Text before")
                                        .append(BBX.SPAN.IMAGE.create(),
                                                childFactory -> childFactory.addAttribute(BBX.SPAN.IMAGE.ATTRIB_SOURCE,
                                                        "test.jpg"))
                                        .append("Text after"))
                        .append(BBX.CONTAINER.IMAGE.create(),
                                groupFactory -> groupFactory.addAttribute(BBX.SPAN.IMAGE.ATTRIB_SOURCE, "test.jpg")
                                        .append(BBX.BLOCK.DEFAULT.create(),
                                                blockFactory -> blockFactory.append("test block 1"))
                                        .append(BBX.BLOCK.DEFAULT.create(),
                                                blockFactory -> blockFactory.append("test block 2"))));

        openImageDescriber(bbTest, true);
        SWTBot describerBot = TestUtils.refreshReturnActiveBot(bbTest);
        SWTBotStyledText describerTextBot = describerBot.styledText(0);

        dot1Event(describerTextBot);
        String s = describerTextBot.getText();
        Assert.assertEquals(s, dot1);

        describerBot.button(SimpleImageDescriberDialog.makeAllString(true, "2")).click();
        doPendingSWTWork();

        bbTest.assertRootSection_NoBrlCopy().nextChildIs(childAssert -> childAssert.isBlockDefaultStyle("Body Text")
                        .nextChildIsText("Text before")
                        .nextChildIs(childAssert2 -> childAssert2.isSpan(BBX.SPAN.IMAGE).child(0).isBlock(BBX.BLOCK.MARGIN)
                                .child(0).isInlineEmphasis(EmphasisType.NO_CONTRACT).nextChildIsText(dot1))
                        .nextChildIsText("Text after").noNextChild())
                .nextChildIs(childAssert -> childAssert.isContainer(BBX.CONTAINER.IMAGE)
                        .nextChildIs(childInline -> childInline.isBlock(BBX.BLOCK.MARGIN).child(0)
                                .isInlineEmphasis(EmphasisType.NO_CONTRACT).nextChildIsText(dot1))
                        .nextChildIs(
                                childAssert2 -> childAssert2.isBlockDefaultStyle("Caption").hasText("test block 1"))
                        .nextChildIs(
                                childAssert2 -> childAssert2.isBlockDefaultStyle("Caption").hasText("test block 2"))
                        .noNextChild())
                .noNextChild();
    }

    @Test(enabled = false)
    public void addAllDotsToSixImages() {
        BBTestRunner bbTest = new BBTestRunner(
                new BBXDocFactory().append(BBX.BLOCK.DEFAULT.create(), blockFactory -> blockFactory.append("Text node"))
                        .append(BBX.BLOCK.DEFAULT.create(),
                                blockFactory -> blockFactory.append(BBX.SPAN.IMAGE.create(),
                                        childFactory -> childFactory.addAttribute(BBX.SPAN.IMAGE.ATTRIB_SOURCE,
                                                "test.jpg")))
                        .append(BBX.BLOCK.DEFAULT.create(),
                                blockFactory -> blockFactory.append(BBX.SPAN.IMAGE.create(),
                                        childFactory -> childFactory.addAttribute(BBX.SPAN.IMAGE.ATTRIB_SOURCE,
                                                "test.jpg")))
                        .append(BBX.BLOCK.DEFAULT.create(),
                                blockFactory -> blockFactory.append(BBX.SPAN.IMAGE.create(),
                                        childFactory -> childFactory.addAttribute(BBX.SPAN.IMAGE.ATTRIB_SOURCE,
                                                "test.jpg")))
                        .append(BBX.BLOCK.DEFAULT.create(),
                                blockFactory -> blockFactory.append(BBX.SPAN.IMAGE.create(),
                                        childFactory -> childFactory.addAttribute(BBX.SPAN.IMAGE.ATTRIB_SOURCE,
                                                "test.jpg")))
                        .append(BBX.BLOCK.DEFAULT.create(),
                                blockFactory -> blockFactory.append(BBX.SPAN.IMAGE.create(),
                                        childFactory -> childFactory.addAttribute(BBX.SPAN.IMAGE.ATTRIB_SOURCE,
                                                "test.jpg")))
                        .append(BBX.BLOCK.DEFAULT.create(), blockFactory -> blockFactory.append(BBX.SPAN.IMAGE.create(),
                                childFactory -> childFactory.addAttribute(BBX.SPAN.IMAGE.ATTRIB_SOURCE, "test.jpg")))

        );

        openImageDescriber(bbTest, true);
        SWTBot describerBot = TestUtils.refreshReturnActiveBot(bbTest);
        SWTBotStyledText describerTextBot = describerBot.styledText(0);

        allDotsEvent(describerTextBot);
        String s = describerTextBot.getText();
        Assert.assertEquals(s, allDots);

        describerBot.button(SimpleImageDescriberDialog.makeAllString(true, "6")).click();
        doPendingSWTWork();

        System.out.println(XMLHandler.toXMLPrettyPrint(bbTest.manager.getDoc()));
        bbTest.assertRootSection_NoBrlCopy()
                .nextChildIs(
                        childAssert -> childAssert.isBlockDefaultStyle("Body Text")
                                .nextChildIsText(
                                        "Text node"))
                .nextChildIs(childAssert2 -> childAssert2.isBlock(BBX.BLOCK.DEFAULT).child(0).isSpan(BBX.SPAN.IMAGE)
                        .child(0)
                        .isBlock(BBX.BLOCK.MARGIN).child(0).isInlineEmphasis(
                                EmphasisType.NO_CONTRACT)
                        .nextChildIsText(allDots))
                .nextChildIs(childAssert2 -> childAssert2.isBlock(BBX.BLOCK.DEFAULT).child(0).isSpan(BBX.SPAN.IMAGE)
                        .child(0)
                        .isBlock(BBX.BLOCK.MARGIN).child(0).isInlineEmphasis(
                                EmphasisType.NO_CONTRACT)
                        .nextChildIsText(allDots))
                .nextChildIs(childAssert2 -> childAssert2.isBlock(BBX.BLOCK.DEFAULT).child(0).isSpan(BBX.SPAN.IMAGE)
                        .child(0)
                        .isBlock(BBX.BLOCK.MARGIN).child(0).isInlineEmphasis(
                                EmphasisType.NO_CONTRACT)
                        .nextChildIsText(allDots))
                .nextChildIs(childAssert2 -> childAssert2.isBlock(BBX.BLOCK.DEFAULT).child(0).isSpan(BBX.SPAN.IMAGE)
                        .child(0).isBlock(BBX.BLOCK.MARGIN).child(0)
                        .isInlineEmphasis(
                                EmphasisType.NO_CONTRACT)
                        .nextChildIsText(allDots))
                .nextChildIs(childAssert2 -> childAssert2.isBlock(BBX.BLOCK.DEFAULT).child(0).isSpan(BBX.SPAN.IMAGE)
                        .child(0).isBlock(BBX.BLOCK.MARGIN).child(0).isInlineEmphasis(EmphasisType.NO_CONTRACT)
                        .nextChildIsText(allDots))
                .nextChildIs(childAssert2 -> childAssert2.isBlock(BBX.BLOCK.DEFAULT).child(0).isSpan(BBX.SPAN.IMAGE)
                        .child(0).isBlock(BBX.BLOCK.MARGIN).child(0).isInlineEmphasis(EmphasisType.NO_CONTRACT)
                        .nextChildIsText(allDots))
                .noNextChild();
    }

    @Test(enabled = false)
    public void findPreviousBraille() {
        BBTestRunner bbTest = new BBTestRunner(new BBXDocFactory()
                .append(BBX.BLOCK.DEFAULT.create(), blockFactory -> blockFactory.append("Text node"))
                .append(BBX.BLOCK.DEFAULT.create(), blockFactory -> blockFactory.append(BBX.SPAN.IMAGE.create(),
                        childFactory -> childFactory.addAttribute(BBX.SPAN.IMAGE.ATTRIB_SOURCE, "test.jpg").append(
                                BBX.INLINE.EMPHASIS.create(EmphasisType.NO_CONTRACT),
                                childFactory2 -> childFactory2.append(allDots))))
                .append(BBX.BLOCK.DEFAULT.create(), blockFactory -> blockFactory.append(BBX.SPAN.IMAGE.create(),
                        childFactory -> childFactory.addAttribute(BBX.SPAN.IMAGE.ATTRIB_SOURCE, "test.jpg").append(
                                BBX.INLINE.EMPHASIS.create(EmphasisType.NO_CONTRACT),
                                childFactory2 -> childFactory2.append(dot1))))

        );

        openImageDescriber(bbTest, true);
        SWTBot describerBot = TestUtils.refreshReturnActiveBot(bbTest);
        SWTBotStyledText describerTextBot = describerBot.styledText(0);

        String s = describerTextBot.getText();
        Assert.assertEquals(s, allDots);

        dot1Event();

        describerBot.button("Next Image").click();
        doPendingSWTWork();

        TestUtils.forceShellByName(shellName);
        describerBot = bbTest.bot.activeShell().bot();
        describerTextBot = describerBot.styledText(0);

        s = describerTextBot.getText();
        Assert.assertEquals(s, dot1);
    }

    @Test(enabled = false)
    public void changePreviousBraille() {
        BBTestRunner bbTest = new BBTestRunner(new BBXDocFactory()
                .append(BBX.BLOCK.DEFAULT.create(), blockFactory -> blockFactory.append("Text node"))
                .append(BBX.BLOCK.DEFAULT.create(), blockFactory -> blockFactory.append(BBX.SPAN.IMAGE.create(),
                        childFactory -> childFactory.addAttribute(BBX.SPAN.IMAGE.ATTRIB_SOURCE, "test.jpg").append(
                                BBX.INLINE.EMPHASIS.create(EmphasisType.NO_CONTRACT),
                                childFactory2 -> childFactory2.append(allDots))))
                .append(BBX.BLOCK.DEFAULT.create(), blockFactory -> blockFactory.append(BBX.SPAN.IMAGE.create(),
                        childFactory -> childFactory.addAttribute(BBX.SPAN.IMAGE.ATTRIB_SOURCE, "test.jpg").append(
                                BBX.INLINE.EMPHASIS.create(EmphasisType.NO_CONTRACT),
                                childFactory2 -> childFactory2.append(dot1))))

        );

        openImageDescriber(bbTest, true);
        SWTBot describerBot = TestUtils.refreshReturnActiveBot(bbTest);
        SWTBotStyledText describerTextBot = describerBot.styledText(0);

        String s = describerTextBot.getText();
        Assert.assertEquals(s, allDots);

        dot1Event(describerTextBot);
        allDotsEvent(describerTextBot);

        describerBot.button(SimpleImageDescriberDialog.INLINE_ONE).click();
        doPendingSWTWork();

        TestUtils.forceShellByName(shellName);
        describerBot = bbTest.bot.activeShell().bot();
        describerBot.button("Next Image").click();
        doPendingSWTWork();

        TestUtils.forceShellByName(shellName);
        describerBot = bbTest.bot.activeShell().bot();
        describerTextBot = describerBot.styledText(0);

        s = describerTextBot.getText();
        Assert.assertEquals(s, dot1);

        describerBot.button("Previous Image").click();
        doPendingSWTWork();

        TestUtils.forceShellByName(shellName);
        describerBot = bbTest.bot.activeShell().bot();
        describerTextBot = describerBot.styledText(0);

        s = describerTextBot.getText();
        Assert.assertEquals(s, dot1 + allDots + allDots);
    }

    @Test(enabled = false)
    public void imageDescriber_insideTableCopy_rt4566() {
        //format=listed

        BBTestRunner bbTest = new BBTestRunner("", "<p>this is <img src='sigh.jpg'/> test</p>"
//				+ "</level1><level1>"
                + "<table format='listed'>"
                + "<tr><td>one</td><td>two<img src='sigh.jpg'/></td></tr>"
                + "<tr><td>some</td><td>text</td></tr>".repeat(60)
//				+ "</level1><level1>"
                + "<tr><td>three</td><td>four</td></tr>"
                + "</table>");


        bbTest.textViewTools.navigateToText("one");

//		bbTest.styleViewTools.navigateToText("[S-IMAGE:]");
        openImageDescriber(bbTest, false);
//		bbTest.openMenuItem(MenuManager.TopMenu.TOOLS, SimpleImageDescriberModule.MENU_ITEM_NAME);

        ViewTestRunner.forceActiveShellHack();
        SWTBot imgBot = bbTest.bot.activeShell().bot();
        imgBot.styledText(0).setText("oo");
        ViewTestRunner.doPendingSWTWork();

        SWTBotButton applyAllButton = imgBot.buttonWithId(SimpleImageDescriberDialog.SWTBOT_APPLY_ALL_INLINE);
        assertEquals(applyAllButton.getText(), SimpleImageDescriberDialog.makeAllString(false, "2"));
        applyAllButton.click();
        ViewTestRunner.doPendingSWTWork();

        // shell is still open
        imgBot.activeShell().close();

        bbTest.updateTextView();
        bbTest.updateViewReferences();
        bbTest.textViewTools.navigateToText("three");
    }

    @Test(enabled = false)
    public void imageInEmptyBlock_issue4781() {
        BBTestRunner bbTest = new BBTestRunner("", "<p>test</p><p><img src='test.jpg'/></p>");

        bbTest.textViewTools.navigateToText("est");
        openImageDescriber(bbTest, false);

        assertEquals(bbTest.textViewTools.getSelectionStripped(), "test");
    }

    @Test(enabled = false)
    public void imageInBlock_issue4781() {
        BBTestRunner bbTest = new BBTestRunner("", "<p>test<img src='test.jpg'/></p>");

        bbTest.textViewTools.navigateToText("est");
        openImageDescriber(bbTest, false);

        assertEquals(bbTest.textViewTools.getSelectionStripped(), "test");
    }

    @Test(enabled = false)
    public void imageInBlock_issue5406() {
        BBTestRunner bbTest = new BBTestRunner("",
                "<table>"
                        + "<caption>a captions</caption>"
                        + "<thead><tr><th>haha</th><th>lol</th></tr></thead>"
                        + "<tr><td>this</td><td>is</td></tr>"
                        + "<tr><td><img src='sigh.jpg'/>image</td><td>test</td></tr>"
                        + "<tfoot><tr><td>footer1</td><td>footer2</td></tr></tfoot>"
                        + "</table>"
        );

        bbTest.textViewTools.navigateToText("est");
        openImageDescriber(bbTest, false);

        // TODO: Re-enable when dependant issue is resolved
//		assertEquals(bbTest.textViewTools.getSelectionStripped(), "test");

        ViewTestRunner.forceActiveShellHack();
        SWTBot imgBot = bbTest.bot.activeShell().bot();
        String testText = "oo";
        imgBot.styledText(0).setText(testText);
        ViewTestRunner.doPendingSWTWork();

        SWTBotButton applyAllButton = imgBot.buttonWithId(SimpleImageDescriberDialog.SWTBOT_APPLY_ALL_INLINE);
        assertEquals(applyAllButton.getText(), SimpleImageDescriberDialog.makeAllString(false, "1"));
        applyAllButton.click();
        ViewTestRunner.doPendingSWTWork();

        // shell is still open
        imgBot.activeShell().close();

        bbTest.updateTextView();
        bbTest.updateViewReferences();

        bbTest.textViewTools.navigateToText(testText);

        List<Node> imageList = StreamSupport.stream(((Iterable<Node>)FastXPath.descendant(bbTest.getDoc())::iterator).spliterator(), false)
                .filter(BBX.SPAN.IMAGE::isA)
                .toList();
        try {
            // allow additional one for table copy
            assertEquals(imageList.size(), 2);
        } catch (AssertionError e) {
            throw new NodeException("mulitple image lists", imageList.getFirst(), e);
        }

        Element image = (Element) imageList.getFirst();
        assertEquals(image.getChildCount(), 1);
        assertEquals(image.getChild(0).getValue(), testText);

    }

}

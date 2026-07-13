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
package org.brailleblaster.testrunners;

import com.google.common.collect.Lists;
import nu.xom.Document;
import org.apache.commons.io.FileUtils;
import org.brailleblaster.utils.BBData;
import org.brailleblaster.BBIni;
import org.brailleblaster.perspectives.braille.Manager;
import org.brailleblaster.wordprocessor.WPManager;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;

import static org.testng.Assert.assertTrue;

public class BBViewTestRunner extends ViewTestRunner {
    private static final Logger log = LoggerFactory.getLogger(BBViewTestRunner.class);
    public static final File BB_PATH = BBData.INSTANCE.getBrailleblasterPath();
    public static final File BB_USER_PATH = BBData.INSTANCE.getUserDataPath();
    public static final File TEST_DIR = Paths.get("dist", "programData", "testFiles").toFile();

    public final WPManager wpManager;
    public final Manager manager;

    public SWTBotStyledText brailleViewBot;

    public BBViewTestRunner(String headXML, String bodyXML) {
        this(TestXMLUtils.generateBookDoc(headXML, bodyXML));
    }

    public BBViewTestRunner(Document doc) {
        //Moved since this() must be first
        this(TestXMLUtils.docToFile(doc));
    }

    public BBViewTestRunner(File inputFile) {
        //Put file in testFiles dir so debug handler can find it
        File testFile;
        if (!inputFile.getParentFile().equals(TEST_DIR)) {
            log.warn("Copying test file to " + TEST_DIR);
            try {
                FileUtils.copyFileToDirectory(inputFile, TEST_DIR);
            } catch (Exception e) {
                throw new RuntimeException("Can't copy file " + inputFile + " to " + TEST_DIR);
            }

            testFile = new File(TEST_DIR, inputFile.getName());
        } else {
            testFile = inputFile;
        }
        assertTrue(testFile.exists());
        assertTrue(testFile.isFile());

        BBIni.setDebuggingEnabled();
        BBIni.INSTANCE.initialize(Lists.newArrayList(testFile.getName()), BB_PATH, BB_USER_PATH);
        wpManager = WPManager.createInstance(null);
        doPendingSWTWork();

        bot = new SWTBot(wpManager.getShell());
        textViewBot = bot.styledText(0);
        brailleViewBot = bot.styledText(1);

        bot.menu("&Open").click();
        doPendingSWTWork();

        //Move mouse onto window to set current element and currectly trigger TextVerifyKeyListener
        Point shellCorner = WPManager.getInstance().getShell().getLocation();
        Point viewCordner = textViewBot.widget.getLocationAtOffset(textViewBot.widget.getCaretOffset());
        Display.getCurrent().setCursorLocation(shellCorner);
        Display.getCurrent().setCursorLocation(new Point(shellCorner.x + viewCordner.x + 20, shellCorner.y + viewCordner.y + 20));
        doPendingSWTWork();

        textViewBot.setFocus();
        textViewBot.navigateTo(0, 0);
        doPendingSWTWork();

        manager = wpManager.controller;
        textViewWidget = textViewBot.widget;
    }

    @Override
    public Document getDoc() {
        return manager.getDoc();
    }

    public void cut() {
        bot.menu("&Cut").click();
    }

    public void copy() {
        bot.menu("&Copy").click();
    }

    public void paste() {
        bot.menu("&Paste").click();
    }

    public void navigateBrailleView(int offset) {
        navigateTextView(brailleViewBot, offset);
    }

    public void updateTextView() {
        brailleViewBot.setFocus();
        textViewBot.setFocus();
        doPendingSWTWork();
    }
}

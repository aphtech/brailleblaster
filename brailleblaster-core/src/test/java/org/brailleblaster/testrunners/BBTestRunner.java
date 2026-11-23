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

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Text;
import org.apache.commons.lang3.StringUtils;
import org.brailleblaster.utils.BBData;
import org.brailleblaster.BBIni;
import org.brailleblaster.BBTestInit;
import org.brailleblaster.bbx.BBX;
import org.brailleblaster.perspectives.braille.Manager;
import org.brailleblaster.perspectives.braille.views.style.BreadcrumbsToolbar;
import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.utd.IStyle;
import org.brailleblaster.utd.internal.xml.XMLHandler;
import org.brailleblaster.util.Notify;
import org.brailleblaster.wordprocessor.WPManager;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.exceptions.WidgetNotFoundException;
import org.eclipse.swtbot.swt.finder.keyboard.Keystrokes;
import org.eclipse.swtbot.swt.finder.utils.Position;
import org.eclipse.swtbot.swt.finder.widgets.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.brailleblaster.testrunners.ViewTestRunner.doPendingSWTWork;
import static org.brailleblaster.testrunners.ViewTestRunner.stripNewlines;
import static org.testng.Assert.*;

/**
 * Updated test runner now that all 3 views are in the main shell
 */
public class BBTestRunner {
    public static final File BB_PATH = BBData.INSTANCE.getBrailleblasterPath();
    public static final File BB_USER_PATH = BBData.INSTANCE.getUserDataPath();
    public static final File TEST_DIR = BBViewTestRunner.TEST_DIR;

    public final WPManager wpManager;
    public final Manager manager;

    /*
     * DO NOT CHANGE ANY OF THESE REFERENCES OUTSIDE OF updateViewReferences
     */
    public final SWTBot bot;

    public SWTBotStyledText textViewBot;
    public final ViewTools textViewTools;
    public StyledText textViewWidget;

    public SWTBotStyledText brailleViewBot;
    public final ViewTools brailleViewTools;
    public StyledText brailleViewWidget;

    public final SWTBotStyledText styleViewBot = null;
    public final ViewTools styleViewTools = null;
    public final StyledText styleViewWidget = null;

    public final File debugFile;
    public final File debugSaveFile;

    public BBTestRunner(String headXML, String bodyXML) {
        this(headXML, bodyXML, null);
    }

    public BBTestRunner(String headXML, String bodyXML, Runnable postInitCallback) {
        this(TestXMLUtils.generateBookDoc(headXML, bodyXML), postInitCallback);
    }

    public BBTestRunner(BBXDocFactory factory) {
        //Moved since this() must be first
        this(factory.root.getDocument());
    }

    public BBTestRunner(Document doc) {
        this(doc, null);
    }

    public BBTestRunner(Document doc, Runnable postInitCallback) {
        //Moved since this() must be first
        this(docToTempFile(doc), postInitCallback);
    }

    protected static File docToTempFile(Document doc) {
        File testFile;
        try {
            testFile = File.createTempFile("bbTest", "something");
//			testFile.deleteOnExit();
        } catch (Exception e) {
            throw new RuntimeException("Cannot create test file", e);
        }
        new XMLHandler().save(doc, testFile);
        return testFile;
    }

    public BBTestRunner(File inputFile) {
        this(inputFile, null);
    }

    public BBTestRunner(File inputFile, Runnable postInitCallback) {
        debugFile = inputFile;
        assertTrue(debugFile.exists(), "input file " + inputFile + " does not exist");
        assertTrue(debugFile.isFile(), "input file " + inputFile + " is not a file");

        try {
            debugSaveFile = File.createTempFile("bbTestRunner", debugFile.getName() + ".out");
            debugSaveFile.deleteOnExit();
        } catch (Exception e) {
            throw new RuntimeException("Unable to make debug file", e);
        }
        BBTestInit.setupForTesting(new String[]{debugFile.getAbsolutePath(), debugSaveFile.getAbsolutePath()});
        assertEquals(BBIni.getDebugFilePath().toString(), debugFile.getAbsolutePath());
        assertEquals(BBIni.getDebugSavePath().toString(), debugSaveFile.getAbsolutePath());

        if (postInitCallback != null) {
            postInitCallback.run();
        }

        Notify.DEBUG_EXCEPTION = true;
        wpManager = WPManager.createInstance(null);
        doPendingSWTWork();
        ViewTestRunner.forceActiveShellHack();
        doPendingSWTWork();
        manager = wpManager.controller;

        Display.getCurrent().addFilter(SWT.KeyDown, (Event event) -> {
            if (event.keyCode == SWT.PAUSE) {
                System.out.println("Pause key pressed, aborting tests and killing JVM");
                System.exit(6);
            }
        });

		/*
		Move mouse onto window for
		- set current element and correctly trigger TextVerifyKeyListener
		- bug(?) in linux (at least i3) where the WPManager selectionListener doesn't run until the mouse gives focus to the view
		 */
        WPManager.getInstance().getFolder().setSelection(0);
        Point shellCorner = WPManager.getInstance().getShell().getLocation();
        Point shellSize = WPManager.getInstance().getShell().getSize();
//		Point viewCordner = manager.getTextView().getLocation();
//		Display.getCurrent().setCursorLocation(shellCorner);
        doPendingSWTWork();
        Display.getCurrent().setCursorLocation(new Point(shellCorner.x + shellSize.x - 100, shellCorner.y + shellSize.y - 100));
        doPendingSWTWork();

        try {
            bot = new SWTBot(wpManager.getShell());
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
            //make "using uninitialized variable" error happy
            throw e;
        }

        textViewTools = new ViewTools();
        textViewTools.setManager(manager);
        brailleViewTools = new ViewTools();
        updateViewReferences();
        textViewTools.settle();

        //TODO: no longer needed in BBX?
//		bot.menu("&Open").click();
//		doPendingSWTWork();
        textViewBot.setFocus();
        doPendingSWTWork();
        textViewBot.navigateTo(0, 0);
        doPendingSWTWork();

//		if (counter++ == 20) {
//			System.out.println("UTDmanager: " + DurationFormatUtils.formatDurationHMS(UTDManager.totalMilliLoad));
//			System.out.println("Archiver: " + DurationFormatUtils.formatDurationHMS(Manager.totalArchiverTime));
//			System.out.println("WPManager: " + DurationFormatUtils.formatDurationHMS(WPManager.totalOpenTime));
//			System.exit(0);
//		}
    }

    public Document getDoc() {
        return manager.getDoc();
    }

    /**
     * Menu lookup in BrailleBlaster's own shell. bot.menu() goes through SWTBot's active shell, which is null
     * when the window doesn't have OS focus ("The widget was null").
     */
    public SWTBotMenu findMenu(String text, boolean recursive) {
        org.eclipse.swtbot.swt.finder.widgets.SWTBotRootMenu menuBar = new SWTBotShell(wpManager.getShell()).menu();
        try {
            return menuBar.menu(text, recursive, 0);
        } catch (WidgetNotFoundException e) {
            // Styles show BANA display labels (bana-styles.properties), e.g. a style name maps to a friendlier label
            String label = org.brailleblaster.utils.localization.LocaleHandler.Companion.getBanaStyles(java.util.Locale.getDefault()).get(text);
            if (label.equals(text)) {
                throw e;
            }
            return menuBar.menu(label, recursive, 0);
        }
    }

    /**
     * The newest visible window of this BrailleBlaster, normally the dialog a test just opened. Use instead of
     * bot.activeShell(), which asks the OS for the focused window and is null when BrailleBlaster isn't focused.
     */
    public SWTBotShell activeShell() {
        Shell main = wpManager.getShell();
        Shell[] shells = Display.getCurrent().getShells();
        for (int i = shells.length - 1; i >= 0; i--) {
            Shell shell = shells[i];
            if (!shell.isDisposed() && shell.isVisible() && shell != main) {
                return new SWTBotShell(shell);
            }
        }
        return new SWTBotShell(main);
    }

    public void cut() {
        findMenu("&Cut", true).click(); doPendingSWTWork();
    }

    public void copy() {
        findMenu("&Copy", true).click(); doPendingSWTWork();
    }

    public void paste() {
        findMenu("&Paste", true).click(); doPendingSWTWork();
    }

    public void navigateBrailleView(int offset) {
        brailleViewTools.navigate(offset);
    }

    public void navigateTextView(int offset) {
        textViewTools.navigate(offset);
    }

    public void selectTree(SWTBot bot, String menu) {
        SWTBotMenu menuBot = findMenu("Tree", true);
        menuBot.menu(menu).click();
    }

    /**
     * Commits a pending text view edit. The old way moved focus away and back so TextView's focusLost listener ran,
     * but focus events don't fire reliably without OS focus, so by default do what that listener does.
     */
    public void updateTextView() {
        if (ViewTools.REAL_KEYS) {
            brailleViewBot.setFocus();
            textViewBot.setFocus();
        } else if (manager.getText().textChanged) {
            manager.getText().update(false);
            manager.getText().setCurrent(textViewWidget.getCaretOffset());
        }
        doPendingSWTWork();
    }

    public void clickButton(String buttonText) {
        bot.button(buttonText).click();
        doPendingSWTWork();
    }

    /**
     * @see org.brailleblaster.util.swt.EasySWT#addSwtBotKey(org.eclipse.swt.widgets.Control, java.lang.String)
     */
    public void clickButtonWithId(String swtBotId) {
        bot.buttonWithId(swtBotId).click();
        doPendingSWTWork();
    }

    public void clickCheckboxWithId(String swtBotId, boolean selected) {
        SWTBotCheckBox checkBox = bot.checkBoxWithId(swtBotId);
        if (checkBox.isChecked() != selected) {
            checkBox.click();
        }
        doPendingSWTWork();
    }

    public void clickComboIndexWithId(String swtBotId, int index) {
        SWTBotCombo checkBox = bot.comboBoxWithId(swtBotId);
        checkBox.setSelection(index);
        doPendingSWTWork();
    }

    public void selectToolbarOption(String menuItem) {
        findMenu(menuItem, true).click();
        doPendingSWTWork();
    }

    public void openMenuItem(TopMenu topLevelMenu, String... menuItem) {
        if (topLevelMenu == null) {
            throw new NullPointerException("topLevelMenu");
        }

        SWTBotMenu selectedMenu;
        try {
            selectedMenu = findMenu(StringUtils.capitalize(topLevelMenu.name().toLowerCase()), false);
        } catch (WidgetNotFoundException e) {
            throw new RuntimeException("TopMenu " + topLevelMenu + " not found?", e);
        }

        for (String curMenuItem : menuItem) {
            selectedMenu = _doClickMenu(selectedMenu, curMenuItem, false);
        }
        selectedMenu.click();
        doPendingSWTWork();
    }

    public void clickStyleMenuItem(String category, String menuItem) {
        SWTBotMenu selectedMenu = findMenu(StringUtils.capitalize(TopMenu.STYLES.name().toLowerCase()), false);
        selectedMenu = _doClickMenu(selectedMenu, category, false);
        selectedMenu = _doClickMenu(selectedMenu, menuItem, true);
        selectedMenu.click();
        doPendingSWTWork();
    }

    public static SWTBotMenu _doClickMenu(SWTBotMenu selectedMenu, String curMenuItem, boolean recursive) {
        try {
            return selectedMenu.menu(curMenuItem, recursive, 0);
        } catch (WidgetNotFoundException e) {
            // The Styles menu shows BANA display labels (bana-styles.properties, e.g. L1-3 -> "List 1"),
            // while tests name styles by their styleDefs name
            String label = org.brailleblaster.utils.localization.LocaleHandler.Companion.getBanaStyles(java.util.Locale.getDefault()).get(curMenuItem);
            if (!label.equals(curMenuItem)) {
                try {
                    return selectedMenu.menu(label, recursive, 0);
                } catch (WidgetNotFoundException ignored) {
                    // report the original name below
                }
            }
            StringBuilder sb = new StringBuilder();

            MenuItem curParent = selectedMenu.widget;
            while (curParent != null) {
                sb.insert(0, ") > ");

                Menu curSubMenu = curParent.getMenu();
                if (curSubMenu != null) {
                    for (int i = 0; i < curSubMenu.getItemCount(); i++) {
                        sb.insert(0, curSubMenu.getItem(i).getText());
                        if (i != curSubMenu.getItemCount() - 1) {
                            sb.insert(0, ", ");
                        }
                    }
                }

                sb.insert(0, " (with children: ")
                        .insert(0, curParent.getText());
                curParent = curParent.getParent().getParentItem();
            }
            sb.append(curMenuItem);

            throw new RuntimeException("Menu item " + sb + " not found", e);
        }
    }

    public void setTextWithId(String id, String text) {
        SWTBotText widget = bot.textWithId(id);
        widget.setText(text);
        doPendingSWTWork();
    }

    public SWTBotButton getBreadcrumbsAncestor(int ancestorLevel) {
        return getBreadcrumbsAncestor(ancestorLevel, "");
    }

    public SWTBotButton getBreadcrumbsAncestor(int ancestorLevel, String suffix) {
        List<SWTBotButton> ids = new ArrayList<>();
        int curId = 0;
        //breadcrumbs are ordered from the root down to the current element
        while (true) {
            try {
                String id = BreadcrumbsToolbar.SWTBOT_ANCESTOR_PREFIX + (curId++);
                ids.add(bot.buttonWithId(id));
            } catch (WidgetNotFoundException e) {
                int index = ids.size() - 1 - ancestorLevel;
                if (index >= ids.size() || index < 0) {
                    StringBuilder sb = new StringBuilder();
                    for (SWTBotButton curAncestorButton : ids) {
                        sb.append(curAncestorButton.getText()).append(" > ");
                    }
                    throw new ArrayIndexOutOfBoundsException("Wrong index " + sb);
                }
                return (suffix != null)
                        ? bot.buttonWithId(ids.get(index).getId() + suffix)
                        : ids.get(index);
            }
        }
    }

    /**
     * For {@code <p><b>text</b></p>} ancestorLevel=0 is bold, ancestorLevel=1 is paragraph
     *
     * @param ancestorLevel
     */
    public void selectBreadcrumbsAncestor(int ancestorLevel) {
        selectBreadcrumbsAncestor(ancestorLevel, "");
    }

    /**
     * For {@code <p><b>text</b></p>} ancestorLevel=0 is bold, ancestorLevel=1 is paragraph
     *
     * @param ancestorLevel
     * @param suffix        swtbot id suffix
     */
    public void selectBreadcrumbsAncestor(int ancestorLevel, String suffix) {
        SWTBotButton button = getBreadcrumbsAncestor(ancestorLevel, suffix);
        button.click();
        doPendingSWTWork();
    }

    /**
     * For {@code <p><b>text</b></p>} ancestorLevel=0 is bold, ancestorLevel=1 is paragraph
     *
     * @param ancestorLevel
     */
    public void selectBreadcrumbsAncestor(int ancestorLevel, Predicate<Node> expectedType) {
        selectBreadcrumbsAncestor(ancestorLevel);

        if (expectedType != null) {
            assertTrue(
                    expectedType.test(manager.getSimpleManager().getCurrentCaret().getNode()),
                    "Failed type check with " + expectedType.getClass()
            );
        }
    }

    public void selectBreadcrumbsAncestor(int ancestorLevel, Consumer<Node> expectedType) {
        selectBreadcrumbsAncestor(ancestorLevel);

        if (expectedType != null) {
            expectedType.accept(manager.getSimpleManager().getCurrentCaret().getNode());
        }
    }

    public XMLElementAssert assertRootSection_NoBrlCopy() {
        return assertElement(getDoc().getRootElement())
                .stripUTDAndCopy()
                .child(1)
                .isSection(BBX.SECTION.ROOT)
                .validate();
    }

    public XMLElementAssert assertRootSectionFirst_NoBrlCopy() {
        return assertRootSection_NoBrlCopy()
                .childCount(1)
                .nextChild();
    }

    public XMLElementAssert assertInnerSection_NoBrlCopy() {
        return assertRootSection_NoBrlCopy()
                .childCount(1)
                .nextChild()
                .isSection(BBX.SECTION.OTHER);
    }

    public XMLElementAssert assertInnerSectionFirst_NoBrlCopy() {
        return assertInnerSection_NoBrlCopy()
                .childCount(1)
                .nextChild();
    }

    public XMLElementAssert assertElement(Element elem) {
        return new XMLElementAssert(
                elem,
                manager.getDocument().getSettingsManager().getEngine().getStyleMap()
        );
    }

    /**
     * Calls getStyleCursorAncestor(0)
     *
     * @return
     * @see #getStyleCursorAncestor(int)
     */
    public IStyle getStyleCursor() {
        return getStyleCursorAncestor(0);
    }

    /**
     * If text node, will get parent element
     *
     * @param level How many times getParent will be called on the cursor.
     * @return
     */
    public IStyle getStyleCursorAncestor(int level) {
        Node cursor = manager.getSimpleManager().getCurrentCaret().getNode();
        while (cursor instanceof Text) {
            cursor = cursor.getParent();
        }
        for (int i = 0; i < level; i++) {
            cursor = cursor.getParent();
        }
        return getStyle(cursor);
    }

    public IStyle getStyle(Node node) {
        return manager.getDocument()
                .getSettingsManager()
                .getEngine()
                .getStyleMap()
                .findValueOrDefault(node);
    }

    public void updateViewReferences() {
        textViewWidget = manager.getTextView();
        textViewBot = new SWTBotStyledText(textViewWidget);
        textViewTools.setBot(textViewBot);

        brailleViewWidget = manager.getBrailleView();
        brailleViewBot = new SWTBotStyledText(brailleViewWidget);
        brailleViewTools.setBot(brailleViewBot);
    }

    /**
     * Keyboard and caret helpers for one view.
     * <p>
     * By default keys are delivered straight to the widget ({@link DirectKeys}), which doesn't need OS focus and so
     * works on local Windows runs. {@code -Dbb.test.realKeys=true} goes back to OS-level SWTBot keystrokes, e.g. to
     * debug real keyboard handling on Linux/Xvfb. See test_fix.md Phase 4c.
     */
    public static class ViewTools {
        public static final boolean REAL_KEYS = Boolean.getBoolean("bb.test.realKeys");

        public StyledText widget;
        public SWTBotStyledText bot;
        /** Set for the text view, whose caret can be placed through TextView like the Kotlin tests do */
        private Manager manager;

        public ViewTools() {
        }

        public ViewTools(SWTBotStyledText bot) {
            setBot(bot);
        }

        public void setBot(SWTBotStyledText bot) {
            this.bot = bot;
            this.widget = bot.widget;
        }

        /** Enables placing the caret through {@code manager.getText()} instead of arrow keys */
        public void setManager(Manager manager) {
            this.manager = manager;
        }

        public void pressKey(int keyCode, int times) {
            for (int i = 0; i < times; i++) {
                if (REAL_KEYS) {
                    bot.pressShortcut(KeyStroke.getInstance(keyCode));
                } else {
                    DirectKeys.press(widget, 0, keyCode, DirectKeys.characterFor(keyCode));
                }
            }

            doPendingSWTWork();
        }

        public void cutShortCut() {
            pressShortcut(SWT.CTRL, 'x');
        }

        public void pasteShortcut() {
            pressShortcut(SWT.CTRL, 'v');
        }

        public void pressShortcut(int modificationKeys, char c) {
            if (REAL_KEYS) {
                bot.pressShortcut(modificationKeys, c);
            } else {
                DirectKeys.press(widget, modificationKeys, Character.toLowerCase(c), DirectKeys.characterFor(modificationKeys, c));
            }
            doPendingSWTWork();
        }

        public void pressShortcut(int modificationKeys, int keyCode, char c) {
            if (REAL_KEYS) {
                bot.pressShortcut(modificationKeys, keyCode, c);
            } else {
                DirectKeys.press(widget, modificationKeys, keyCode, c);
            }
            doPendingSWTWork();
        }

        public void pressShortcut(KeyStroke... keys) {
            if (REAL_KEYS) {
                bot.pressShortcut(keys);
            } else {
                DirectKeys.press(widget, keys);
            }
            doPendingSWTWork();
        }

        /**
         * Move cursor in text view to offset
         *
         * @param offset
         */
        public void navigate(int offset) {
            if (REAL_KEYS) {
                navigate(bot, offset);
            } else if (manager != null) {
                settle();
                // Commit a pending edit first, as the arrow keys leaving the element would
                manager.getText().update(false);
                manager.getText().setCurrentElement(offset);
                doPendingSWTWork();
                assertEquals(widget.getCaretOffset(), offset, "View is not on given offset");
            } else {
                arrowTo(offset);
            }
        }

        /**
         * Lets the background Reformatter finish so the view text is final. Instant caret moves and offsets computed
         * from the text otherwise race it: edits are lost, or the caret lands where the text used to be.
         */
        public void settle() {
            if (!REAL_KEYS && manager != null) {
                manager.waitForFormatting(true);
                doPendingSWTWork();
            }
        }

        /** Moves the caret with emulated arrow keys, for views without a TextView to place it through */
        private void arrowTo(int offset) {
            int lastOffset = -1;
            int sameCount = 0;
            int curOffset;
            while ((curOffset = widget.getCaretOffset()) != offset) {
                DirectKeys.press(widget, 0, curOffset < offset ? SWT.ARROW_RIGHT : SWT.ARROW_LEFT, '\0');
                doPendingSWTWork();
                sameCount = curOffset == lastOffset ? sameCount + 1 : 0;
                lastOffset = curOffset;
                if (sameCount > 20) {
                    throw new RuntimeException("Same offset " + curOffset + " for 20 arrow keys, wanted " + offset);
                }
            }
        }

        public void navigateToLine(int line) {
            navigate(widget.getOffsetAtLine(line));
        }

        /**
         * Offset just after the last character of {@code line}. Old tests wrote this as
         * {@code getOffsetAtLine(line + 1) - 1}, which only works with one-character line breaks.
         */
        public int endOfLine(int line) {
            return widget.getOffsetAtLine(line) + widget.getLine(line).length();
        }

        public static void navigate(SWTBotStyledText textBot, int offset) {
            textBot.navigateTo(0, 0);
            doPendingSWTWork();
            if (offset == 0) {
                //view doesn't recognize navigateTo, trick it by arrowing which will trigger the listeners
                textBot.pressShortcut(Keystrokes.RIGHT);
                doPendingSWTWork();
                textBot.pressShortcut(Keystrokes.LEFT);
                doPendingSWTWork();
            } else {
                //Due to Windows newlines being \r\n
                int lastOffset = -1;
                int lastOffsetCount = 0;
                int curOffset;
                while ((curOffset = textBot.widget.getCaretOffset()) < offset) {
//				log.trace("pressing right, viewBot {}, index '{}'", curOffset, textBot.widget.getText().substring(curOffset, curOffset));
                    textBot.pressShortcut(Keystrokes.RIGHT);
                    doPendingSWTWork();
                    if (lastOffset != curOffset) {
                        lastOffset = curOffset;
                        lastOffsetCount = 0;
                    } else {
                        lastOffsetCount++;
                        int max = 20; //arbitrary num that should never happen
                        if (lastOffsetCount > max) {
                            throw new RuntimeException("Same offset for " + max + " times");
                        }
                    }
                }
            }

            doPendingSWTWork();
            int newOffset = textBot.widget.getCaretOffset();
            assertEquals(newOffset, offset, "View is not on given offset");
        }

        public void navigateRelative(int offset) {
            doPendingSWTWork();
            if (!REAL_KEYS) {
                // Relative navigation exists to exercise the arrow-key path, so keep using arrow keys
                arrowTo(offset);
                assertEquals(widget.getCaretOffset(), offset, "View is not on given offset");
                return;
            }
            int curOffset;
            while ((curOffset = bot.widget.getCaretOffset()) != offset) {
//				log.trace("pressing right, viewBot {}, index '{}'", curOffset, textBot.widget.getText().substring(curOffset, curOffset));
                if (curOffset < offset) {
                    bot.pressShortcut(Keystrokes.RIGHT);
                } else {
                    bot.pressShortcut(Keystrokes.LEFT);
                }
                doPendingSWTWork();
            }

            doPendingSWTWork();
            int newOffset = bot.widget.getCaretOffset();
            assertEquals(newOffset, offset, "View is not on given offset");
        }

        public void navigateToText(String text) {
            navigateToText(text, 0);
        }

        public void navigateToText(String text, int skip) {
            if (skip < 0) {
                throw new IllegalArgumentException("skip must be positive");
            }
            settle();
            // widget, not bot: SWTBot's getText() turns \r\n into \n, so its offsets are wrong on Windows
            int index = widget.getText().indexOf(text);
            assertNotEquals(index, -1, "Cannot find string '" + text + "' in text: " + widget.getText());
            for (int i = 0; i < skip; i++) {
                index = widget.getText().indexOf(text, index + text.length());
                assertNotEquals(index, -1, "Cannot find string '" + text + "' at skip " + i + " in text: " + widget.getText());
            }
            navigate(index);
        }

        public void navigateToTextRelative(String text) {
            navigateToTextRelative(text, 0);
        }

        public void navigateToTextRelative(String text, int skip) {
            if (skip < 0) {
                throw new IllegalArgumentException("skip must be positive");
            }
            settle();
            int index = widget.getText().indexOf(text);
            for (int i = 0; i < skip; i++) {
                index = widget.getText().indexOf(text, index + text.length());
            }
            assertNotEquals(index, -1, "Cannot find string '" + text + "' in text: " + widget.getText());
            navigateRelative(index);
        }

        public void navigateToEndOfLine() {
            int curLinePos = bot.cursorPosition().column;
            for (int i = 0; i < bot.getTextOnCurrentLine().length() - curLinePos; i++) {
                pressKey(SWT.ARROW_RIGHT, 1);
            }
        }

        public void selectLeft(int chars) {
//			updateSelection(chars, 0);
            for (int i = 0; i < chars; i++) {
                pressShortcut(SWT.SHIFT, SWT.ARROW_LEFT, '\0');
            }
        }

        public void selectRight(int chars) {
            selectRight(chars, false);
        }

        public void selectRight(int chars, boolean skipNewlines) {
            for (int i = 0; i < chars; i++) {
                pressShortcut(SWT.SHIFT, SWT.ARROW_RIGHT, '\0');

                if (skipNewlines && bot.getSelection().endsWith(System.lineSeparator()) && widget.getCaretOffset() != widget.getText().length() - 1) {
                    i--;
                }
            }
        }

        public void selectToEndOfLine() {
            Position cursorPosition = bot.cursorPosition();

            bot.selectRange(
                    cursorPosition.line,
                    cursorPosition.column,
                    bot.getTextOnCurrentLine().length() - cursorPosition.column
            );
            doPendingSWTWork();
        }

        public void select(String text) {
            selectFromTo(text, text);
        }

        public void selectFromTo(String from, String to) {
            navigateToText(from);
            selectToEndOf(to);
        }

        public void selectToEndOf(String text) {
            int curOffset = widget.getCaretOffset();
            int nextIndex = widget.getText().indexOf(text, curOffset);
            assertNotEquals(nextIndex, -1, "Cannot find string '" + text + "' in text: " + widget.getText());
            selectRight(nextIndex - curOffset + text.length());
        }

        private void updateSelection(int xDelta, int yDelta) {
            doPendingSWTWork();

            //TODO: Broken on Windows because of RT#4379 / Eclipse #497345
//			for (int i = 0; i < chars; i++) {
//				bot.pressShortcut(SWT.SHIFT, SWT.ARROW_RIGHT, '\0');
//			}
            //Use this ugly hack instead, which is essentially SWTBotStyledText.selectRange
            {
                Point selection = widget.getSelection();
                widget.setSelection(selection.x - xDelta, selection.y + yDelta);
                doPendingSWTWork();

                Event event = new Event();
                event.time = (int) System.currentTimeMillis();
                event.widget = widget;
                event.display = Display.getCurrent();

                widget.notifyListeners(SWT.Selection, event);
            }

            doPendingSWTWork();
        }

        public String getSelectionStripped() {
            return stripNewlines(bot.getSelection().trim());
        }

        /**
         * Offset must be set before calling method
         *
         * @param bot:   bot to enter text
         * @param text:  text to type
         * @param length
         */
        public void typeTextInRange(String text, int length) {
            selectRight(length);
            typeText(text);
        }

        public void typeText(String text) {
            settle();
            if (REAL_KEYS) {
                bot.typeText(text);
            } else {
                DirectKeys.type(widget, text);
            }
            doPendingSWTWork();
        }

        public void typeNewline() {
            typeLine("");
        }

        public void typeLine(String text) {
            typeText(text + System.lineSeparator());
        }

        public String getTextStripped() {
            return stripNewlines(bot.getText().trim());
        }

        public String getTextStripped(int offset) {
            return stripNewlines(widget.getText().substring(offset).trim());
        }

        public void clickContextMenu(String menuItem) {
            bot.contextMenu(menuItem).click();
            doPendingSWTWork();
        }
    }
}

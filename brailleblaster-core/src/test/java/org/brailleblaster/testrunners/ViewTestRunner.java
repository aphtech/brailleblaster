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

import static org.testng.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotStyledText;

import nu.xom.Document;
import org.brailleblaster.wordprocessor.WPManager;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.keyboard.Keystrokes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic utilities for running any unit test
 */
public abstract class ViewTestRunner {
	private static final Logger log = LoggerFactory.getLogger(ViewTestRunner.class);
	public SWTBot bot;
	public SWTBotStyledText textViewBot;
	
	public StyledText textViewWidget;
	public Document doc;

//	public abstract T start(Document doc) throws IOException;
//	
//	public abstract T start(File testFile) throws IOException;
	public abstract Document getDoc();

	public ViewTestRunner() {
		throw new UnsupportedOperationException("Going to be removed");
	}
	
	/** performs a arrow key opr keyboard shortcut event
	 * @param textBot
	 * @param keyCode : key to pressed
	 * @param times : number of times key is pressed
	 */
	public void pressKey(SWTBotStyledText textBot, int keyCode, int times){
		for(int i = 0; i < times; i++)
			textBot.pressShortcut(KeyStroke.getInstance(keyCode));
		
		doPendingSWTWork();
	}
	
	
	public void cutShortCut(SWTBotStyledText textBot){
		pressShortcut(textBot, SWT.CTRL, 'x');
	}
	
	public void pasteShortcut(SWTBotStyledText textBot){
		pressShortcut(textBot, SWT.CTRL, 'v');
	}
	
	private void pressShortcut(SWTBotStyledText textBot, int keyCode, char c){
		textBot.pressShortcut(keyCode, c);
		doPendingSWTWork();
	}
	
	/**
	 * Move cursor in text view to offset
	 *
	 * @param offset
	 */
	public void navigateTextView(int offset) {
		navigateTextView(textViewBot, offset);
	}
	
	public void navigateTextViewToLine(int line) {
		navigateTextView(textViewBot, textViewWidget.getOffsetAtLine(line));
	}

	public static void navigateTextView(SWTBotStyledText textBot, int offset) {
		textBot.navigateTo(0, 0);
		doPendingSWTWork();
		//Due to Windows newlines being \r\n 
		int curOffset;
		while ((curOffset = textBot.widget.getCaretOffset()) < offset) {
			log.trace("pressing right, viewBot {}, index '{}'", curOffset, textBot.widget.getText().substring(curOffset, curOffset + 10));
			textBot.pressShortcut(Keystrokes.RIGHT);
			doPendingSWTWork();

		}

		doPendingSWTWork();
		int newOffset = textBot.widget.getCaretOffset();
		assertEquals(newOffset, offset, "View is not on given offset");
	}

	public void navigateTextViewToText(String text) {
		int index = textViewBot.getText().indexOf(text);
		assertNotEquals(index, -1, "Cannot find string '" + text + "' in text: " + textViewBot.getText());
		navigateTextView(index);
	}

	public void selectTextViewRight(int chars) {
		for (int i = 0; i < chars; i++) {
			textViewBot.pressShortcut(SWT.SHIFT, SWT.ARROW_RIGHT, '\0');
		}
		doPendingSWTWork();
	}

	public String getTextViewText() {
		return textViewBot.getText();
	}

	public String getTextViewSelection() {
		return textViewBot.getSelection();
	}

	public String getTextViewSelectionStripped() {
		return stripNewlines(textViewBot.getSelection().trim());
	}
	
	public void selectTree(SWTBot bot, String menu){
		SWTBotMenu menuBot = bot.menu("Tree");
		menuBot.menu(menu).click();
	}
	
	
	/** Offset must be set before calling method
	 * @param bot: bot to enter text
	 * @param text: text to type
	 * @param length
	 */
	public void typeTextInRange(SWTBotStyledText bot, String text, int length){
		this.selectTextViewRight(length);
		typeText(bot, text);
	}
	
	public void typeText(SWTBotStyledText bot, String text){
		bot.typeText(text);
		doPendingSWTWork();
	}
	
	public void selectToolbarOption(String menuItem){
		bot.menu(menuItem).click();
		doPendingSWTWork();
	}

	public String getTextViewTextStripped() {
		return stripNewlines(textViewBot.getText().trim());
	}

	public String getTextViewTextStripped(int offset) {
		return stripNewlines(textViewBot.getText().substring(offset).trim());
	}

	/**
	 * Flushes the SWT Event queue. When SwtBot does most user actions, it moves 
	 * the OS cursor and keyboard to act like a user. The OS makes input events 
	 * and sends them to SWT to process like normal user input. If you don't
	 * call this, the UI won't change because SWT hasn't registered 
	 * you clicked something
	 * <p>
	 * This simulates {@link WPManager#start() } but without waiting for more 
	 * input from the OS.
	 */
	public static void doPendingSWTWork() {
		while (Display.getCurrent().readAndDispatch()) {
//			editor.display.sleep();
		}
	}

	/**
	 * Stop test execution and allow you to inspect BB
	 */
	public static void DEBUG_MODE() {
		if (System.getProperty("ciBuild") != null) {
			throw new AssertionError("DEBUG_MODE inside jenkins");
		}
		doPendingSWTWork();
		Display display = Display.getCurrent();
		while (true) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}

	/**
	 * Stop test execution for x sections and allow you to inspect BB, 
	 * useful for monitoring multi-step tests
	 * @param sec 
	 */
	public static void DEBUG_SLEEP(int sec) {
		try {
			Display display = Display.getCurrent();

			AtomicBoolean flag = new AtomicBoolean(false);
			display.timerExec(sec * 1000, () -> flag.set(true));
			while (!flag.get()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to sleep?!", e);
		}
	}

	public static String stripNewlines(String input) {
		return StringUtils.replaceEach(input, new String[]{"\n", "\r"}, new String[]{"", ""});
	}

	/**
	 * https://wiki.eclipse.org/SWTBot/Troubleshooting#No_active_Shell_when_running_SWTBot_tests_in_Xvfb
	 * *sigh
	 */
	public static void forceActiveShellHack() {
		Shell[] shells = Display.getCurrent().getShells();
		for (Shell shell : shells) {
			log.debug("shell " + shell.getText());
		}

		//Workaround for Search dialog where close button just does shell.setVisible(false)
		for (int i = shells.length - 1; i >= 0; i--) {
			Shell lastShell = shells[i];
			if (lastShell.getVisible()) {
				log.debug("Forcing active shell " + lastShell.getText());
				lastShell.setFocus();
				lastShell.forceActive();
//				boolean focused = lastShell.forceFocus();
//				if(!focused)
//					throw new NullPointerException("Didnt get focus");
				// Pray south to the SWT gods 3 times 
				break;
			}
		}
	}
	
	/**
	 * This is a workaround for dialogs that end with the readAndDispatch loop. A thread is created that will
	 * run the onActive consumer once the dialog appears. Use this method before opening the dialog.
	 * @param onActive The code that will be run after the shell appears
	 * @param shellTitle The title of the shell that is expected to open.
	 * @param bot The swtBot, which is necessary for getting the active shell
	 * @return a CountDownLatch that will count down when onActive has finished 
	 */
	public static CountDownLatch lockedShellHack(Consumer<SWTBotShell> onActive, String shellTitle, SWTBot bot){
		CountDownLatch lock = new CountDownLatch(1);
		bot.getDisplay().asyncExec(() -> {
			int waitLoops = 0;
			final int MAX_WAIT_LOOPS = 20;
			while(!bot.activeShell().getText().equals(shellTitle)){
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					throw new RuntimeException("Sleep interrupted");
				}
				waitLoops++;
				if(waitLoops == MAX_WAIT_LOOPS){
					throw new RuntimeException("Shell not found");
				}
			}
			onActive.accept(bot.activeShell());
			lock.countDown();
		});
		return lock;
	}

	public static String compareStringDetails(String given, String expected) {
		String error = null;
		for (int i = 0; i < Math.max(given.length(), expected.length()); i++) {
			char givenChar;
			if (i < given.length()) {
				givenChar = given.charAt(i);
			} else {
				error = "given ends at " + i + " while expected ends at " + expected.length();
				break;
			}

			char expectedChar;
			if (i < expected.length()) {
				expectedChar = expected.charAt(i);
			} else {
				error = "expected ends at " + i + " while given ends at " + given.length();
				break;
			}

			if (givenChar != expectedChar) {
				error = "char " + i + " given '" + givenChar + "'" + Character.getName(givenChar)
						+ "  expected '" + expectedChar + "' " + Character.getName(expectedChar);
				break;
			}
		}
		return error;
	}

	public static void assertEqualsDetails(String given, String expected) {
		if (!given.equals(expected)) {
			throw new AssertionError("Expected: " + expected
					+ System.lineSeparator()
					+ "Given   : " + given
					+ System.lineSeparator()
					+ "Reason: " + compareStringDetails(given, expected)
			);
		}
	}
}

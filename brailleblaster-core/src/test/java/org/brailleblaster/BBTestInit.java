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
package org.brailleblaster;

import org.apache.commons.io.FileUtils;
import org.brailleblaster.utils.PropertyFileManager;
import org.brailleblaster.utils.BBData;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;
import org.testng.collections.Lists;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Properties;

/**
 * This class is called very early in TestNG's init
 */
public class BBTestInit extends TestListenerAdapter {
	private static final Logger log = LoggerFactory.getLogger(BBTestInit.class);
	private static final NullPropertyFileManager USER_SETTINGS_TPL;

	static {
		//Must exist so BBIni doesn't try to create it 
		File userSettingsDist = BBData.INSTANCE.getBrailleblasterPath("programData", "settings", "user_settings.properties");
		File userSettingsUser = BBData.INSTANCE.getUserDataPath("programData", "settings", "user_settings.properties");

		//must create so bbbootstrap doesn't try to create
		if (!userSettingsUser.exists()) {
			try {
				FileUtils.touch(userSettingsUser);
			} catch (Exception e) {
				throw new RuntimeException("Unable to create " + userSettingsUser, e);
			}
		}

		USER_SETTINGS_TPL = new NullPropertyFileManager(userSettingsDist.getAbsolutePath());
		USER_SETTINGS_TPL.save("views", "STYLE,PRINT,BRAILLE");
	}

	public BBTestInit() {
		//This must be called very early in TestNG's init to prevent a chicken-and-egg problem in tests
		//as methods and parameters are loaded first, and their classes, which don't exist yet
		//because SWT isn't on the classpath...
		System.out.println("Initting BB for tests...");

		setupForTesting(new String[0]);

		System.out.println("Finished initting BB for tests");
	}

	@Override
	public void onTestSkipped(ITestResult tr) {
		//We should never have skipped tests as they can hide real problems (eg broken DataProviders)
		//since TestNG doesn't give us any exception
		tr.setStatus(ITestResult.FAILURE);
		super.onTestFailure(tr);
		log.error("============== Test skipped (dataprovider?), crashing as TestNG ignores Failure on skipped tests", tr.getThrowable());
		cleanupBB();

		// crash as TestNG no longer reports failures?
		System.exit(2);
	}

	@Override
	public void onTestStart(ITestResult result) {
		LoggerFactory.getLogger(getClass()).trace("+++ Test " + result.getName() + " - " + result.getTestName());
		super.onTestStart(result);
	}

	@Override
	public void onTestFailure(ITestResult tr) {
		super.onTestFailure(tr);
        /*
                                                  // IDK what we are doing with this class, but commenting this out bc getting
    // a null pointer for the shells is causing the test suite to not run the rest of the tests

		String shells = Utils.join(
				Display.getCurrent().getShells(), 
				Shell::getText, 
				" | "
		);
		
		boolean isSwtActive = Display.getCurrent() != null && !Display.getCurrent().isDisposed();

		String bbShellStatus = "disposed";
		if (isSwtActive
				&& WPManager.INSTANCE != null
				&& WPManager.INSTANCE.getShell() != null
				&& !WPManager.INSTANCE.getShell().isDisposed()) {
			Shell shell = WPManager.INSTANCE.getShell();
			SWTBot bot = new SWTBot();
			bbShellStatus = "isFocusControl=" + shell.isFocusControl()
					+ " isVisible=" + shell.isVisible()
					+ " isActive-Display=" + (shell == Display.getCurrent().getActiveShell())
					+ " isActive-SwtBot=" + (shell == bot.activeShell().widget);
		}

		String bbTextStatus = "disposed";
		if (isSwtActive
				&& WPManager.INSTANCE != null
				&& WPManager.INSTANCE.getCurrentManager() != null
				&& !WPManager.INSTANCE.getCurrentManager().getTextView().isDisposed()) {
			StyledText textView = WPManager.INSTANCE.getCurrentManager().getTextView();
			bbTextStatus = "isFocusControl=" + textView.isFocusControl()
					+ " isVisible=" + textView.isVisible();
		}

		tr.setThrowable(new RuntimeException(
				"Environment debug info, see cause for actual exception"
				+ System.lineSeparator() + "Shells: " + shells
				+ System.lineSeparator() + "Active Shell: " + Display.getCurrent().getActiveShell().getText()
				+ System.lineSeparator() + "BB Main Shell: " + bbShellStatus
				+ System.lineSeparator() + "Text view: " + bbTextStatus,
				 tr.getThrowable()
		));

		log.error("============== Test failed ", tr.getThrowable());
		cleanupBB();
        */
	}

	@Override
	public void onTestSuccess(ITestResult tr) {
		cleanupBB();
        /*
		try {
			ZipHandles._failOnHas();
		} catch (RuntimeException e) {
			tr.setStatus(ITestResult.FAILURE);
			tr.setThrowable(e);
		}
        */
		super.onTestSuccess(tr);
	}

	@Override
	public void onFinish(ITestContext testContext) {
		log.info("Finished with tests");
		for (ITestNGMethod curTest : testContext.getExcludedMethods()) {
			if (!curTest.getEnabled()) {
				log.warn("Test is disabled! " + curTest.getTestClass().getName() + "/" + curTest.getMethodName());
			}
		}
		super.onFinish(testContext);
	}

	/**
	 * Reset state between tests, put here to affect all tests by default
	 * since forgetting this will mess up further tests
	 */
	public static void cleanupBB() {
		Display display = Display.getCurrent();
		if (display != null && !display.isDisposed()) {
			log.info("Disposing display");
			display.dispose();
		} else {
			log.info("Display already disposed or does not exist");
		}
	}

	public static void setupForTesting(String[] args) {
		BBIni.setDebuggingEnabled();
//		Main.initBB(new String[0]);
		setupForBenchmarks(args);

//		BookToBBXConverter.devSetup(new String[0]);

		//Improve test performance
//		BBDocument.TEST_MODE = true;
	}

	public static void setupForBenchmarks(String[] args) {
		BBIni.bootDialogsEnabled = false;
		BBIni.INSTANCE.initialize(
				Arrays.asList(args),
				BBData.INSTANCE.getBrailleblasterPath(),
				BBData.INSTANCE.getUserDataPath(),
				new NullPropertyFileManager(USER_SETTINGS_TPL)
		);

		try {
			BBIni.setTestData(Files.createTempFile("bbTest", "recent_documents"));
		} catch (Exception e) {
			throw new RuntimeException("Cannot create test file for recent documents", e);
		}

		cleanupBB();
	}

	private static class NullPropertyFileManager extends PropertyFileManager {
		public NullPropertyFileManager(String filePath) {
			super(filePath);
		}

		public NullPropertyFileManager(String filePath, Properties prop) {
			super(new File(filePath), prop);
		}

		public NullPropertyFileManager(NullPropertyFileManager other) {
			super(other.getFile(), new Properties(other.getProp()));
		}

		@Override
		public void save() {
			// do nothing so settings are NOT saved to disk
		}
	}
}

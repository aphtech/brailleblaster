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
import org.eclipse.swt.widgets.Shell;
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
import java.util.stream.Collectors;

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
		log.error("============== Test failed {} (open shells: {})", tr.getName(), openShellTitles(), tr.getThrowable());
		// Without this a failed GUI test leaves its Display and dialogs behind, and every
		// later GUI test fails with "Invalid thread access"
		cleanupBB();
	}

	/** Titles of the shells still open on the current display, for failure logs */
	private static String openShellTitles() {
		Display display = Display.getCurrent();
		if (display == null || display.isDisposed()) {
			return "none";
		}
		return Arrays.stream(display.getShells())
				.map(shell -> shell.isDisposed() ? "<disposed>" : "'" + shell.getText() + "'")
				.collect(Collectors.joining(", "));
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
			// Close leftover dialogs and windows first so their dispose listeners run while the display is still alive
			for (Shell shell : display.getShells()) {
				try {
					if (!shell.isDisposed()) {
						shell.dispose();
					}
				} catch (RuntimeException e) {
					log.warn("Failed to dispose shell during cleanup", e);
				}
			}
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

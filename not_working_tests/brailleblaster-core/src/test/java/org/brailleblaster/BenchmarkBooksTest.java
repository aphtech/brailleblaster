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

import java.io.File;

import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.ViewTestRunner;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Benchmark books from users that exposed exceptions
 */
@GuiTest
public class BenchmarkBooksTest {
    @Test(groups = TestGroups.SLOW_TESTS, enabled = false)
    public void goToVolumeAtPageNumber() {
        BBTestRunner bbTest = new BBTestRunner(new File("/media/brailleblaster/nimas-books/4541-Literature Volumes and TOC.bbx"));

        bbTest.textViewBot.setFocus();

        bbTest.openMenuItem(TopMenu.NAVIGATE, "Go To Page");

        SWTBot bot = bbTest.bot.activeShell().bot();
        Assert.assertEquals(bot.activeShell().getText(), "Go To");

        bot.comboBox(0).setSelection(3);
        ViewTestRunner.doPendingSWTWork();
        Assert.assertEquals(bot.comboBox(0).getText(), "Volume 2");

        bot.button(0).click();
        ViewTestRunner.doPendingSWTWork();

        bbTest.updateViewReferences();
        bbTest.textViewBot.setFocus();
        ViewTestRunner.doPendingSWTWork();
    }

    /**
     * Book from rt4541
     * <p>
     * ============== Test failed
     * java.lang.RuntimeException: Unable to save ordinal volume 8
     * at org.brailleblaster.frontmatter.VolumeSaveDialog.clickSaveSingle(VolumeSaveDialog.java:163)
     * at org.brailleblaster.frontmatter.VolumeSaveDialog.lambda$new$53(VolumeSaveDialog.java:116)
     * at org.brailleblaster.util.FormUIUtils$2.widgetSelected(FormUIUtils.java:187)
     * at org.eclipse.swt.widgets.TypedListener.handleEvent(Unknown Source)
     * at org.eclipse.swt.widgets.EventTable.sendEvent(Unknown Source)
     * at org.eclipse.swt.widgets.Display.sendEvent(Unknown Source)
     * at org.eclipse.swt.widgets.Widget.sendEvent(Unknown Source)
     * at org.eclipse.swt.widgets.Display.runDeferredEvents(Unknown Source)
     * at org.eclipse.swt.widgets.Display.readAndDispatch(Unknown Source)
     * at org.brailleblaster.testrunners.ViewTestRunner.DEBUG_MODE(ViewTestRunner.java:182)
     * at org.brailleblaster.search.GoToPageTest.goToAll_simple(GoToPageTest.java:51)
     * at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
     * at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
     * at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
     * at java.lang.reflect.Method.invoke(Method.java:498)
     * at org.testng.internal.MethodInvocationHelper.invokeMethod(MethodInvocationHelper.java:86)
     * at org.testng.internal.Invoker.invokeMethod(Invoker.java:643)
     * at org.testng.internal.Invoker.invokeTestMethod(Invoker.java:820)
     * at org.testng.internal.Invoker.invokeTestMethods(Invoker.java:1128)
     * at org.testng.internal.TestMethodWorker.invokeTestMethods(TestMethodWorker.java:129)
     * at org.testng.internal.TestMethodWorker.run(TestMethodWorker.java:112)
     * at org.testng.TestRunner.privateRun(TestRunner.java:782)
     * at org.testng.TestRunner.run(TestRunner.java:632)
     * at org.testng.SuiteRunner.runTest(SuiteRunner.java:366)
     * at org.testng.SuiteRunner.runSequentially(SuiteRunner.java:361)
     * at org.testng.SuiteRunner.privateRun(SuiteRunner.java:319)
     * at org.testng.SuiteRunner.run(SuiteRunner.java:268)
     * at org.testng.SuiteRunnerWorker.runSuite(SuiteRunnerWorker.java:52)
     * at org.testng.SuiteRunnerWorker.run(SuiteRunnerWorker.java:86)
     * at org.testng.TestNG.runSuitesSequentially(TestNG.java:1244)
     * at org.testng.TestNG.runSuitesLocally(TestNG.java:1169)
     * at org.testng.TestNG.run(TestNG.java:1064)
     * at org.apache.maven.surefire.testng.TestNGExecutor.run(TestNGExecutor.java:132)
     * at org.apache.maven.surefire.testng.TestNGDirectoryTestSuite.executeSingleClass(TestNGDirectoryTestSuite.java:112)
     * at org.apache.maven.surefire.testng.TestNGDirectoryTestSuite.execute(TestNGDirectoryTestSuite.java:99)
     * at org.apache.maven.surefire.testng.TestNGProvider.invoke(TestNGProvider.java:147)
     * at org.apache.maven.surefire.booter.ForkedBooter.invokeProviderInSameClassLoader(ForkedBooter.java:290)
     * at org.apache.maven.surefire.booter.ForkedBooter.runSuitesInProcess(ForkedBooter.java:242)
     * at org.apache.maven.surefire.booter.ForkedBooter.main(ForkedBooter.java:121)
     * Caused by: java.lang.IllegalStateException: No finalBrf?! *
     * =------=
     * at org.brailleblaster.frontmatter.VolumeSaveDialog.volumeToBRF(VolumeSaveDialog.java:348)
     * at org.brailleblaster.frontmatter.VolumeSaveDialog.clickSaveSingle(VolumeSaveDialog.java:158)
     * ... 38 common frames omitted
     * <p>
     * private void clickSaveSingle() {
     * log.trace("saving all brf");
     * String path = generateBRFSaveDialog();
     * if (path == null) {
     * log.debug("canceled save");
     * return;
     * }
     * <p>
     * StringBuilder result = new StringBuilder();
     * for (int i = 0; i < volumes.size() + /*last dangling volume 1; i++) {
     * try {
     * String volumeBRF = volumeToBRF(utdManager, doc, i);
     * result.append(volumeBRF)
     * .append(BRFWriter.PAGE_SEPARATOR);
     * log.info("Generated " + volumeBRF.length() + " characters for ordinal volume " + i);
     * } catch (Exception e) {
     * throw new RuntimeException("Unable to save ordinal volume " + i, e);
     * }
     * }
     * <p>
     * try {
     * FileUtils.write(new File(path), result.toString(), StandardCharsets.UTF_8);
     * log.info("Wrote " + result.length() + " characters to " + path);
     * } catch (Exception e) {
     * throw new RuntimeException("Unable to write BRF to " + path, e);
     * }
     * <p>
     * shell.close();
     * }
     */

    //TODO: Saving all to folder in volume save dialog doesn't work
    @Test(enabled = false)
    public void threeVolumeTest_rt4541_1() {
        @SuppressWarnings("unused")
        BBTestRunner bbTest = new BBTestRunner(new File("/home/leon/aph-home/linuxdev/Literature Volumes3.bbx"));
        ViewTestRunner.DEBUG_MODE();
    }

    @Test(enabled = false)
    public void threeVolumeTest_rt4541_2() {
        @SuppressWarnings("unused")
        BBTestRunner bbTest = new BBTestRunner(new File("/home/leon/aph-home/linuxdev/KCI NIMAS Volumes.bbx"));
        ViewTestRunner.DEBUG_MODE();
    }


}

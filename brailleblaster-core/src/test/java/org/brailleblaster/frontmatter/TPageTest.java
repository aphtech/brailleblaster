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
package org.brailleblaster.frontmatter;

import org.brailleblaster.GuiTest;
import java.io.File;

import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.ViewTestRunner;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.testng.annotations.Test;

@GuiTest
public class TPageTest {
	// Needs the debug-tools modules, registered for tests by TestDebugModulesFactory
	@Test
	public void emptyDocument_rt4646() {
		BBTestRunner bbTest = new BBTestRunner(org.brailleblaster.utils.BBData.INSTANCE.getBrailleblasterPath("programData", "xmlTemplates", "bbxTemplate.bbx"));
		
		// T-Page Generator moved to the Debug menu (TPagesDialog is a DebugMenuToolModule)
		bbTest.openMenuItem(TopMenu.DEBUG, "T-Page Generator");
		
		SWTBot bot = bbTest.activeShell().bot();
		bot.styledText(0).typeText("this is a test");
		ViewTestRunner.doPendingSWTWork();
		bot.buttonWithId(TPagesDialog.SWTBOT_OK_BUTTON).click();
		ViewTestRunner.doPendingSWTWork();
	}
}

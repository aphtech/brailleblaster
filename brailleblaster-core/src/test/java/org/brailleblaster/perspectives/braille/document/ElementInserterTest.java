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
import static org.testng.Assert.*;
import static org.brailleblaster.testrunners.ViewTestRunner.*;

import java.util.function.Consumer;
import org.brailleblaster.TestGroups;
import org.brailleblaster.testrunners.BBViewTestRunner;
import org.eclipse.swt.SWT;
import org.testng.annotations.Test;

@Test(groups = TestGroups.BROKEN_TESTS)
@GuiTest
public class ElementInserterTest {
	static final Consumer<BBViewTestRunner> MAKE_BOLD = (BBViewTestRunner bbTest) -> {
		// bot.menu("Bold Toggle").click();
		bbTest.textViewBot.pressShortcut(SWT.CTRL, 'b');
	};
	static final Consumer<BBViewTestRunner> MAKE_CONTRACTED = (BBViewTestRunner bbTest) -> {
		// SWTBotMenu contextMenu = bot.styledText(0).contextMenu("Change
		// Translation");
		// contextMenu.setFocus();
		// contextMenu.contextMenu("Uncontracted").click();

		// TODO: This uses the Edit menu, why doesn't the right click menu
		// work?
		bbTest.bot.menu("Uncontracted Toggle").click();
	};

	private void doTest(String inputXML, String textToSelect, Consumer<BBViewTestRunner> action, String expectedXML) {
		BBViewTestRunner bbTest = new BBViewTestRunner("", inputXML);

		bbTest.navigateTextViewToText(textToSelect);
		bbTest.selectTextViewRight(textToSelect.length());
		assertEquals(bbTest.getTextViewSelection(), textToSelect);

		action.accept(bbTest);
		doPendingSWTWork();
	}

	@Test
	public void makeBold_Entire() {
		doTest("<p>Here is some <strong>very bold</strong> text stuff</p>",
				"Here is some very bold text stuff",
				MAKE_BOLD,
				"<p><strong>Here is some very bold text stuff</strong></p>"
		);
	}

	@Test
	public void makeBold_TextAndBoldAndText_Partial() {
		doTest("<p>Here is some <strong>very bold</strong> text stuff</p>",
				"some very bold text",
				MAKE_BOLD,
				"<p>Here is <strong>some very bold text</strong> stuff</p>"
		);
	}

	@Test
	public void makeBold_Contracted_issue3760() {
		doTest("<p>"
				+ "<strong>Here is some </strong>very bold "
				+ "<strong> text <span table=\"UNCONTRACTED\">stuff</span></strong>"
				+ "</p>",
				"stuff",
				MAKE_BOLD,
				"<p>"
				+ "<strong>Here is some </strong>very bold "
				+ "<strong> text </strong>"
				+ "<span table=\"UNCONTRACTED\">stuff</span>"
				+ "</p>"
		);
	}

	@Test
	public void makeBold_Italics_Entire() {
		doTest("<p><em>Here is some very</em><em> bold text stuff</em></p>",
				"Here is some very bold text stuff",
				MAKE_BOLD,
				"<p><em><strong>Here is some very</strong></em>"
				+ "<em><strong> bold text stuff</strong></em></p>"
		);
	}

	@Test
	public void makeContracted_ReglarAndEmphasis_Partial() {
		doTest("<p>Here is some <strong>very bold</strong> text</p>",
				"some very",
				MAKE_CONTRACTED,
				"<p>Here is "
				+ "<span table=\"UNCONTRACTED\">some </span>"
				+ "<strong><span table=\"UNCONTRACTED\">very</span> bold</strong>"
				+ " text</p>"
		);
	}

	@Test
	public void makeContracted_TextAndEmphasis_Entire() {
		doTest("<p>Here is some <strong>very bold</strong> text</p>",
				"some very bold",
				MAKE_CONTRACTED,
				"<p>Here is "
				+ "<span table=\"UNCONTRACTED\">some </span>"
				+ "<strong><span table=\"UNCONTRACTED\">very bold</span></strong>"
				+ " text</p>"
		);
	}

	@Test
	public void makeContracted_TextAndEmphasisAndText() {
		doTest("<p>Here is some <strong>very bold</strong> text stuff</p>",
				"some very bold text",
				MAKE_CONTRACTED,
				"<p>Here is "
				+ "<span table=\"UNCONTRACTED\">some </span>"
				+ "<strong><span table=\"UNCONTRACTED\">very bold</span></strong>"
				+ "<span table=\"UNCONTRACTED\"> text</span>"
				+ " stuff</p>"
		);
	}

	@Test
	public void makeContracted_TextAndEmphasisAndText_PartialWord() {
		doTest("<p>Here is some <strong>very bold</strong> text stuff</p>",
				"some very bold te",
				MAKE_CONTRACTED,
				"<p>Here is "
				+ "<span table=\"UNCONTRACTED\">some </span>"
				+ "<strong><span table=\"UNCONTRACTED\">very bold</span></strong>"
				+ "<span table=\"UNCONTRACTED\"> te</span>"
				+ "xt stuff</p>"
		);
	}

	@Test
	public void makeContracted_TextAndEmphasisAndContractedAndText() {
		doTest("<p>Here is some <strong>very <span table=\"UNCONTRACTED\">bold</span></strong> text stuff</p>",
				"some very bold te",
				MAKE_CONTRACTED,
				"<p>Here is "
				+ "<span table=\"UNCONTRACTED\">some </span>"
				+ "<strong><span table=\"UNCONTRACTED\">very bold</span></strong>"
				+ "<span table=\"UNCONTRACTED\"> te</span>"
				+ "xt stuff</p>"
		);
	}

	@Test
	public void makeContracted_BoldTextBold() {
		doTest("<p> <strong>A bolded passage </strong>, <strong> with a punctuation mark in it </strong> </p>",
				"in it",
				MAKE_CONTRACTED,
				"<p> <strong>A bolded passage </strong>, <strong> with a punctuation mark "
				+ "<span table=\"UNCONTRACTED\">"
				+ "in it</span> </strong> </p>"
		);
	}
}

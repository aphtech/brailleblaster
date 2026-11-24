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
package org.brailleblaster.bbx;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import nu.xom.Document;
import org.apache.commons.io.FileUtils;
import org.brailleblaster.archiver2.ArchiverFactory;
import org.brailleblaster.archiver2.TextArchiveLoader;
import org.brailleblaster.settings.DefaultNimasMaps;
import org.brailleblaster.testrunners.XMLElementAssert;
import org.testng.annotations.Test;

public class TextToBBXTest {
	static final DefaultNimasMaps maps = new DefaultNimasMaps();

	@Test
	public void formFeedCharacter_issue4372() throws IOException {
		importText("this" + TextArchiveLoader.FORM_FEED + "text\n" + TextArchiveLoader.FORM_FEED + "\nis good")
				.nextChildIs(childAssert -> childAssert
						.isBlockDefaultStyle("Body Text")
						.hasText("this")
				).nextChildIs(childAssert -> childAssert
						.isBlockDefaultStyle("Body Text")
						.hasText("text")
				).nextChildIs(childAssert -> childAssert
						.isBlockDefaultStyle("Body Text")
						.hasText("is good")
				);
	}

	private static XMLElementAssert importText(String text) throws IOException {
		File tmpFile = File.createTempFile("asdfasdfbb", "test");
		FileUtils.write(tmpFile, text, StandardCharsets.UTF_8);

		Document doc = ArchiverFactory.INSTANCE.load(tmpFile.toPath()).getBbxDocument();

		return new XMLElementAssert(BBX.getRoot(doc), maps.styleMap());
	}
}

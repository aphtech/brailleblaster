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
package org.brailleblaster.utd.utils;

import static org.testng.Assert.assertEquals;

import java.util.Arrays;

import org.brailleblaster.utd.ITranslationEngine;
import org.brailleblaster.utd.properties.BrailleTableType;
import org.brailleblaster.utd.testutils.MockTranslatorFactory;
import org.mwhapples.jlouis.TranslationException;
import org.mwhapples.jlouis.Louis.TypeForms;
import org.testng.annotations.Test;

public class TextTranslatorTest {
    @Test
    public void testTranslateString() throws TranslationException {
        String str = "Some text";
        String brlStr = ",\"s text";
        String index = "0 0 0 4 5 6 7 8";
        String indexOut = "0 0 0 0 3 4 5 6 7";
        short[] typeForms = new short[9];
        Arrays.fill(typeForms, TypeForms.PLAIN_TEXT);
        ITranslationEngine contextMock = MockTranslatorFactory.createTranslationEngine(str, brlStr, index, indexOut, typeForms);

        String result = TextTranslator.INSTANCE.translateText(str, contextMock, TypeForms.PLAIN_TEXT, BrailleTableType.LITERARY);

        assertEquals(result, brlStr);
    }
}

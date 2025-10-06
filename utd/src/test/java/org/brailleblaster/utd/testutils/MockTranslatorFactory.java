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
package org.brailleblaster.utd.testutils;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.brailleblaster.utd.ActionMap;
import org.brailleblaster.utd.BrailleSettings;
import org.brailleblaster.utd.IActionMap;
import org.brailleblaster.utd.IStyleMap;
import org.brailleblaster.utd.ITranslationEngine;
import org.brailleblaster.utd.PageSettings;
import org.brailleblaster.utd.StyleMap;
import org.brailleblaster.utd.config.StyleDefinitions;
import org.mwhapples.jlouis.Louis;
import org.mwhapples.jlouis.TranslationException;
import org.mwhapples.jlouis.TranslationResult;

public class MockTranslatorFactory {

    public static TranslationResult createMockTranslationResult(String braille, String indexes) {
        String[] indexArray = indexes.split(" ");
        int[] inPosArray = new int[indexArray.length];
        for (int i = 0; i < indexArray.length; i++) {
            inPosArray[i] = Integer.parseInt(indexArray[i]);
        }
        TranslationResult translationResultMock = mock(TranslationResult.class);
        when(translationResultMock.getTranslation()).thenReturn(braille);
        when(translationResultMock.getInputPos()).thenReturn(inPosArray);
        return translationResultMock;
    }

    public static TranslationResult createMockTranslationResult(String braille, String inPos, String outPos) {
        String[] indexArray = inPos.split(" ");
        int[] inPosArray = new int[indexArray.length];
        for (int i = 0; i < indexArray.length; i++) {
            inPosArray[i] = Integer.parseInt(indexArray[i]);
        }

        //For outpos
        indexArray = outPos.split(" ");
        int[] outPosArray = new int[indexArray.length];
        for (int i = 0; i < indexArray.length; i++) {
            outPosArray[i] = Integer.parseInt(indexArray[i]);
        }

        TranslationResult translationResultMock = mock(TranslationResult.class);
        when(translationResultMock.getTranslation()).thenReturn(braille);
        when(translationResultMock.getInputPos()).thenReturn(inPosArray);
        when(translationResultMock.getOutputPos()).thenReturn(outPosArray);
        return translationResultMock;
    }

    public static ITranslationEngine createTranslationEngine(String str, String brlStr, String index, String indexOut, short[] typeForms) throws TranslationException {
        return createTranslationEngine("en-us-g2.ctb", str, brlStr, index, indexOut, typeForms);
    }

    public static ITranslationEngine createTranslationEngine(String table, String str, String brlStr, String index, String indexOut, short[] typeForms) throws TranslationException {
        TranslationResult translationResultMock = createMockTranslationResult(brlStr, index, indexOut);
        Louis louisMock = mock(Louis.class);
        //ASCII BRAILLE MODE
        doReturn(translationResultMock).when(louisMock).translate(table, str, typeForms, 0, 0);

        BrailleSettings brailleSettings = new BrailleSettings();
        brailleSettings.setMainTranslationTable(table);
        brailleSettings.setUseAsciiBraille(true);

        return createTranslationEngine(louisMock, brailleSettings, new PageSettings());
    }

    public static ITranslationEngine createTranslationEngine(Louis translator, BrailleSettings brailleSettings, PageSettings pageSettings, IActionMap actionMap, StyleDefinitions styleDefinitions, IStyleMap styleMap) {
        ITranslationEngine engineMock = mock(ITranslationEngine.class);
        when(engineMock.getBrailleTranslator()).thenReturn(translator);
        when(engineMock.getBrailleSettings()).thenReturn(brailleSettings);
        when(engineMock.getPageSettings()).thenReturn(pageSettings);
        when(engineMock.getActionMap()).thenReturn(actionMap);
        when(engineMock.getStyleDefinitions()).thenReturn(styleDefinitions);
        when(engineMock.getStyleMap()).thenReturn(styleMap);
        return engineMock;
    }

    public static ITranslationEngine createTranslationEngine(Louis translator, BrailleSettings brailleSettings, PageSettings pageSettings, IActionMap actionMap) {
        return createTranslationEngine(translator, brailleSettings, pageSettings, actionMap, new StyleDefinitions(), new StyleMap());
    }

    public static ITranslationEngine createTranslationEngine(Louis translator, BrailleSettings brailleSettings, PageSettings pageSettings) {
        return createTranslationEngine(translator, brailleSettings, pageSettings, new ActionMap());
    }
}

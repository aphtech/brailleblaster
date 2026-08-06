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
package org.brailleblaster.utd;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import org.apache.commons.lang3.Strings;
import org.brailleblaster.utils.BBData;
import org.brailleblaster.TestGroups;
import org.brailleblaster.utd.config.UTDConfig;
import org.brailleblaster.utd.internal.NormaliserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Convert test books with BrailleBlaster's UTD config
 */
@Test(groups = TestGroups.BROKEN_TESTS)
public class BRFWriterTest {
    private static final Logger log = LoggerFactory.getLogger(BRFWriterTest.class);

    @DataProvider
    private Object[][] brfDataProvider() {
        //Tests are brf_testName.xml and brf_testName.brf
        String[] testNames = new String[]{
                "simple",
                //			"tables", //WIP
                "lineWrap"
        };

        List<Object[]> paramList = new ArrayList<>();
        for (String testName : testNames) {
            UTDTranslationEngine engine = newEngine();
            engine.getBrailleSettings().setUseAsciiBraille(true);
            engine.setBrailleSettings(UTDConfig.INSTANCE.loadBrailleSettings(
                    BBData.INSTANCE.getBrailleblasterPath("programData", "utd", "EBAE.brailleSettings.xml")));
            paramList.add(new Object[]{engine, testName, "EBAE-useAsciiBraille"});

            engine = newEngine();
            engine.getBrailleSettings().setUseAsciiBraille(false);
            engine.setBrailleSettings(UTDConfig.INSTANCE.loadBrailleSettings(
                    BBData.INSTANCE.getBrailleblasterPath("programData", "utd", "EBAE.brailleSettings.xml")));
            paramList.add(new Object[]{engine, testName, "EBAE-charToDots"});

            engine = newEngine();
            engine.getBrailleSettings().setUseAsciiBraille(true);
            engine.setBrailleSettings(UTDConfig.INSTANCE.loadBrailleSettings(
                    BBData.INSTANCE.getBrailleblasterPath("programData", "utd", "UEB.brailleSettings.xml")));
            paramList.add(new Object[]{engine, testName, "UEB-useAsciiBraille"});

            engine = newEngine();
            engine.getBrailleSettings().setUseAsciiBraille(false);
            engine.setBrailleSettings(UTDConfig.INSTANCE.loadBrailleSettings(
                    BBData.INSTANCE.getBrailleblasterPath("programData", "utd", "UEB.brailleSettings.xml")));
            paramList.add(new Object[]{engine, testName, "UEB-charToDots"});
        }
        return paramList.toArray(new Object[paramList.size()][]);
    }

    private static UTDTranslationEngine newEngine() {
        UTDTranslationEngine engine = new UTDTranslationEngine();

        //Ideally this would be SettingsManager, but any User defined configs would override the BB ones
        File bbProgramData = new File(BBData.INSTANCE.getBrailleblasterPath(), "programData");
        engine.getBrailleTranslator().setDataPath(bbProgramData.getAbsolutePath());
        File UTD_FOLDER = new File(bbProgramData, "utd");
        engine.setPageSettings(UTDConfig.INSTANCE.loadPageSettings(new File(UTD_FOLDER, "pageSettings.xml")));
        engine.setBrailleSettings(UTDConfig.INSTANCE.loadBrailleSettings(new File(UTD_FOLDER, "UEB.brailleSettings.xml")));
        engine.setStyleDefinitions(UTDConfig.loadStyleDefinitions(new File(UTD_FOLDER, "styleDefs.xml")));
        UTDConfig.INSTANCE.loadMappings(engine, UTD_FOLDER, "nimas");

        return engine;
    }

    //Disabled due to frequent changes in BRF
    @Test(enabled = false, dataProvider = "brfDataProvider")
    public void translateNormal(UTDTranslationEngine engine, String testName, String testType) throws Exception {
        log.debug("Using table {}", engine.getBrailleSettings().getMainTranslationTable());
        Document translatedDoc = toUTD(engine, testName);

        //Convert UTD to BRF
        String actualBrf;
        try {
            ByteArrayOutputStream actualBrfStream = new ByteArrayOutputStream();
            Writer actualBrfWriter = new OutputStreamWriter(actualBrfStream, StandardCharsets.UTF_8);
            engine.toBRF(translatedDoc, actualBrfWriter, BRFWriter.OPTS_DEFAULT, BRFWriter.EMPTY_PAGE_LISTENER);
            actualBrfWriter.flush();
            log.debug("length {}", actualBrfStream.size());
            actualBrf = actualBrfStream.toString();
        } catch (Exception e) {
            File utdOut = new File("brlTest-" + testName + ".utd.xml").getAbsoluteFile();
            log.warn("Encountered exception converting UTD to BRF, dumping UTD to " + utdOut);
            try (FileOutputStream output = new FileOutputStream(utdOut)) {
                Serializer serializer = new Serializer(output, "UTF-8");
                serializer.setIndent(4);
                serializer.setMaxLength(80);
                serializer.write(translatedDoc);
            }
            throw new RuntimeException("Failed to translate UTD to BRF, dumped UTD to " + utdOut, e);
        }

        //Load expected BRF
        log.debug("loading brf");
        String expectedBrf = input2String(load(testName + ".brf"));
        log.debug("loaded brf");

        diffOutput(testName, actualBrf, expectedBrf);
    }

    private static Document toUTD(UTDTranslationEngine engine, String testName) throws ParsingException, IOException {
        Builder builder = new Builder(new NormaliserFactory());
        Document doc = builder.build(load(testName + ".xml"));
        return engine.translateAndFormatDocument(doc);
    }

    /**
     * Compare line by line to find errors
     *
     * @param actual
     * @param expected
     */
    private static void diffOutput(String testName, String actual, String expected) {
        //Handle linux with \n
        expected = Strings.CS.replace(expected, "\r\n", System.lineSeparator());

        if (actual.equals(expected))
            //No need to do a fancy diff, they are the same
            return;

        //They are not the same, find the problem line
//		StringTokenizer actualTok = new StringTokenizer(actual, System.lineSeparator());
//				.setIgnoreEmptyTokens(false);
//		StringTokenizer expectedTok = new StringTokenizer(expected, System.lineSeparator());
//				.setIgnoreEmptyTokens(false);
        try {
//			for (int i = 0; i < Math.max(actualTok.size(), expectedTok.size()); i++) {
////				if (!actualTok.hasNext() || !expectedTok.hasNext())
////					//EOF of one. Everything else is the same, one is just too long
////					assertEquals(actualTok.size(), expectedTok.size(), "Too many/not enough lines");
////				else
//					assertEquals(actualTok.next(), expectedTok.next(), "Line " + i + " not equal");
//			}
            StringBuilder builder = new StringBuilder();
            int counter = 0;
            while (true) {
                counter++;
                char actualChar = actual.charAt(counter);
                char expectedChar = expected.charAt(counter);
                assertEquals(actualChar, expectedChar, "actual " + Character.getName(actualChar) + " expected " + Character.getName(expectedChar) + " Buffer " + builder + "|||");
                builder.append(actualChar);
            }
        } catch (Throwable e) {
            throw new AssertionError("Output different for test " + testName
                    + " (see cause exception for reason)"
                    + System.lineSeparator() + "------Actual " + null /*actualTok.size()*/ + " lines "
                    + actual.length() + " chars------"
                    + System.lineSeparator() + actual
                    + System.lineSeparator() + "------Expected " + null /*expectedTok.size()*/ + " lines "
                    + expected.length() + " chars------"
                    + System.lineSeparator() + expected, e);
        }

        //throw new AssertionError("Equal " + actual.equals(expected) + " but reached end, " + actual.length() + " - " + expected.length());
    }

    private static InputStream load(String pathSuffix) {
        String path = "brf_" + pathSuffix;
        InputStream stream = BRFWriterTest.class.getResourceAsStream(path);
        assertNotNull(stream, "Cannot find " + path);
        return stream;
    }

    public static String input2String(InputStream input) throws IOException {
        StringBuilder savedOutput = new StringBuilder();
        try (Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            int next;
            while ((next = reader.read()) != -1) {
                savedOutput.append((char) next);
            }
        }
        return savedOutput.toString();
    }
}

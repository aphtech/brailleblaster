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

import org.brailleblaster.bbx.BookToBBXConverter;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.apache.commons.lang3.Strings;
import org.brailleblaster.settings.UTDManager;
import org.brailleblaster.utd.internal.xml.XMLHandler;
import org.brailleblaster.utils.xml.NamespacesKt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.brailleblaster.testrunners.ViewTestRunner;
import org.brailleblaster.utd.properties.UTDElements;

public class BRFTestRunner {
    private static final Logger log = LoggerFactory.getLogger(BRFTestRunner.class);
    public static final int OPTS_STRIP_ENDING_PAGE = 1 << 1;
    public static final int OPTS_UNICODE = 1 << 2;
    protected final UTDManager utdManager;

    public BRFTestRunner(UTDManager utdManager) {
        this.utdManager = utdManager;
    }

    public BRFTestRunner() {
        this(new UTDManager());

        //For transcriber mode, uncomment this
//		utdManager.getEngine().getBrailleSettings().setUseAsciiBraille(false);
    }

    public void compareXMLtoBraille(String givenXmlInput, String expectedBrfOutput) throws IOException {
        compareXMLtoBraille("", givenXmlInput, expectedBrfOutput, 0);
    }

    public void compareXMLtoBraille(String headXmlContent, String bookXmlContent, String expectedBrfOutput, int opts) throws IOException {
        Document doc = TestXMLUtils.generateBookDoc(headXmlContent, bookXmlContent);
        compareXMLtoBraille(doc, expectedBrfOutput, opts,
                "=== Input Head XML"
                        + System.lineSeparator()
                        + headXmlContent
                        + System.lineSeparator()
                        + "=== Input Book XML"
                        + System.lineSeparator()
                        + bookXmlContent
                        + System.lineSeparator());
    }

    public void compareGeneratedXMLtoBraille(Document doc, String expectedBrfOutput, int opts) throws IOException {
        compareXMLtoBraille(doc, expectedBrfOutput, opts,
                "=== BBRoot"
                        + System.lineSeparator()
                        + XMLHandler.query(doc, "descendant::*[@bbtestroot]").get(0).toXML()
                        + System.lineSeparator()
        );
    }

    public void compareXMLtoBraille(Document doc, String expectedBrfOutput, int opts) throws IOException {
        compareXMLtoBraille(doc, expectedBrfOutput, opts, "");
    }

    public Document translate(Document doc, int opts) {
        // Translation only runs on BBX now (there is no nimas.actionMap.xml), so convert DTBook input
        // the way opening a book does
        if (!"bbdoc".equals(doc.getRootElement().getLocalName())) {
            doc = BookToBBXConverter.Companion.fromConfig("nimas").convert(doc);
        }
        utdManager.loadEngineFromDoc(doc, "bbx");

        utdManager.getEngine().getBrailleSettings().setUseAsciiBraille(!((OPTS_UNICODE & opts) == OPTS_UNICODE));
        doc = utdManager.getEngine().translateAndFormatDocument(doc);
        utdManager.getEngine().format(doc.getRootElement());
        return doc;
    }

    public void compareXMLtoBraille(Document doc, String expectedBrfOutput, int opts, String assertDebug) throws IOException {
        doc = translate(doc, opts);

        StringWriter brfWriter = new StringWriter();
        utdManager.getEngine().toBRF(doc, brfWriter, 0, BRFWriter.EMPTY_PAGE_LISTENER);
        String brfOutput = brfWriter.toString();
        log.debug("brfoutput: " + brfOutput);

        if ((OPTS_STRIP_ENDING_PAGE & opts) == OPTS_STRIP_ENDING_PAGE) {
            //Quick fix: Remove last line for now that contains page num
            brfOutput = brfOutput.substring(0, brfOutput.lastIndexOf('#'));
            brfOutput = StringUtils.stripEnd(brfOutput, null);
        }

        //Remove new page characters
        brfOutput = Strings.CS.replace(brfOutput, "" + BRFWriter.PAGE_SEPARATOR, "");

        if (!Objects.equals(brfOutput, expectedBrfOutput)) {
            File failedFile = new File("brfTestFailed.xml");
            new XMLHandler().save(doc, failedFile);

            String error = ViewTestRunner.compareStringDetails(brfOutput, expectedBrfOutput);

            String fullXML;
            {
                StringWriter sw = new StringWriter();
                Serializer serializer = new Serializer(WriterOutputStream.builder().setWriter(sw).setCharset(StandardCharsets.UTF_8).get(), StandardCharsets.UTF_8.name());
                serializer.setIndent(2);

                Element root = (Element) doc.query("descendant::*[@bbtestroot]").get(0);
                root.detach();
                Document wrapperDoc = new Document(root);
                wrapperDoc.getRootElement().addNamespaceDeclaration(UTDElements.UTD_PREFIX, NamespacesKt.UTD_NS);

                serializer.write(wrapperDoc);
                fullXML = sw.toString();
            }

            throw new AssertionError("BRF not equal (saved doc to " + failedFile.getAbsolutePath() + ")"
                    + System.lineSeparator()
                    + assertDebug
                    + "=== Generated BRF"
                    + System.lineSeparator()
                    + brfOutput
                    + System.lineSeparator()
                    + "=== Expected BRF"
                    + System.lineSeparator()
                    + expectedBrfOutput
                    + System.lineSeparator()
                    + "=== Error"
                    + System.lineSeparator()
                    + error
                    + System.lineSeparator()
                    + "=== Full XML"
                    + System.lineSeparator()
                    + fullXML
            );
        }
    }
}

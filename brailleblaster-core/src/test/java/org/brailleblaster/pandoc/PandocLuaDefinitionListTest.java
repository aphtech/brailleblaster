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
package org.brailleblaster.pandoc;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Nodes;
import org.brailleblaster.BBIni;
import org.brailleblaster.bbx.BookToBBXConverter;
import org.brailleblaster.util.PanDocKt;
import org.brailleblaster.utils.xml.NamespacesKt;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;

import static org.testng.Assert.*;

/**
 * Tests that the bbx.lua Pandoc custom writer correctly converts HTML
 * definition lists (&lt;dl&gt;/&lt;dt&gt;/&lt;dd&gt;) into the BBX DEFINITION list format,
 * including the required bb:listLevel and bb:itemLevel attributes that
 * FixNestedList requires.
 *
 * Key fix: Pandoc 3.x passes DefinitionList items as {[term_string] = defs_table}
 * (term is the map key), not as {term_string, defs_table} (numeric indices).
 */
public class PandocLuaDefinitionListTest {

    private String pandocCmd;
    private String luaDir;
    // BB_NS is a Kotlin const val — accessed as a static field from Java
    private static final String BB_NS = NamespacesKt.BB_NS;

    @BeforeClass
    public void init() {
        BookToBBXConverter.devSetup(new String[0]);
        pandocCmd = PanDocKt.getPANDOC_CMD();
        Path pandocDir = BBIni.INSTANCE.getProgramDataPath().resolve("pandoc");
        luaDir = pandocDir.resolve("lua").toString();
    }

    /**
     * Runs pandoc with bbx.lua on the given HTML string and returns the parsed
     * BBX Document after the fixers (FixMathML, FixNestedList, FixImage) have run.
     */
    private Document runPandoc(String html) throws Exception {
        File htmlFile = File.createTempFile("bb-deflist-test-", ".html");
        htmlFile.deleteOnExit();
        try (FileWriter fw = new FileWriter(htmlFile)) {
            fw.write(html);
        }

        File bbxFile = File.createTempFile("bb-deflist-out-", ".bbx");
        bbxFile.deleteOnExit();

        ProcessBuilder pb = new ProcessBuilder(
                pandocCmd,
                "--from=html+empty_paragraphs",
                "--to=bbx.lua",
                "--output=" + bbxFile.getAbsolutePath(),
                htmlFile.getAbsolutePath()
        ).directory(new File(luaDir));
        pb.environment().put("PANDOCCMD", pandocCmd);
        pb.redirectErrorStream(true);

        Process proc = pb.start();
        int status = proc.waitFor();
        assertEquals(status, 0, "pandoc exited with non-zero status; check bbx.lua for errors");

        Fixer fixer = new Fixer(bbxFile.getAbsolutePath());
        fixer.addFixer(new FixMathML());
        fixer.addFixer(new FixNestedList());
        fixer.addFixer(new FixImage());
        fixer.processFixers();

        return fixer.getDocument();
    }

    /** Returns the first CONTAINER with bb:listType="DEFINITION", or fails the test. */
    private Element findFirstDefinitionList(Document doc) {
        Nodes nodes = doc.query(
                "//*[local-name()='CONTAINER' and @*[local-name()='listType']='DEFINITION']"
        );
        assertFalse(nodes.size() == 0,
                "No DEFINITION list container found in BBX output:\n" + doc.toXML());
        return (Element) nodes.get(0);
    }

    /** Asserts that a LIST_ITEM block has the expected term text and definition text. */
    private void assertDefinitionItem(Element item, String expectedTerm, String expectedDef) {
        assertEquals(item.getLocalName(), "BLOCK");
        assertEquals(item.getAttributeValue("type", BB_NS), "LIST_ITEM");

        Element termSpan = findDefinitionTermSpan(item);
        assertTrue(termSpan.getValue().contains(expectedTerm),
                "Expected term '" + expectedTerm + "' in DEFINITION_TERM span, got: " + termSpan.getValue());

        assertTrue(item.getValue().contains(expectedDef),
                "Expected definition '" + expectedDef + "' in LIST_ITEM, got: " + item.getValue());
    }

    /** Finds the DEFINITION_TERM span inside a LIST_ITEM block. */
    private Element findDefinitionTermSpan(Element listItem) {
        Nodes spans = listItem.query(
                "*[local-name()='SPAN' and @*[local-name()='type']='DEFINITION_TERM']"
        );
        assertTrue(spans.size() > 0,
                "No DEFINITION_TERM span found in LIST_ITEM:\n" + listItem.toXML());
        return (Element) spans.get(0);
    }

    // -------------------------------------------------------------------------
    // Basic definition list: one <dt> per <dd>
    // -------------------------------------------------------------------------
    @Test
    public void basicDefinitionList() throws Exception {
        String html = "<html><body>"
                + "<dl>"
                + "<dt>Beast of Bodmin</dt><dd>A large feline inhabiting Bodmin Moor.</dd>"
                + "<dt>Morgawr</dt><dd>A sea serpent.</dd>"
                + "<dt>Owlman</dt><dd>A giant owl-like creature.</dd>"
                + "</dl>"
                + "</body></html>";

        Document doc = runPandoc(html);
        Element list = findFirstDefinitionList(doc);

        // Container must carry bb:listLevel (required by FixNestedList)
        assertNotNull(list.getAttribute("listLevel", BB_NS),
                "bb:listLevel attribute must be present on DEFINITION container");
        assertEquals(list.getAttributeValue("listType", BB_NS), "DEFINITION");

        // Expect 3 LIST_ITEM blocks
        assertEquals(list.getChildCount(), 3,
                "Expected 3 LIST_ITEM children for 3 dt/dd pairs");

        assertDefinitionItem((Element) list.getChild(0), "Beast of Bodmin", "A large feline inhabiting Bodmin Moor.");
        assertDefinitionItem((Element) list.getChild(1), "Morgawr", "A sea serpent.");
        assertDefinitionItem((Element) list.getChild(2), "Owlman", "A giant owl-like creature.");
    }

    // -------------------------------------------------------------------------
    // Definition list with inline formatting in the term
    // -------------------------------------------------------------------------
    @Test
    public void definitionListWithBoldTerm() throws Exception {
        String html = "<html><body>"
                + "<dl>"
                + "<dt><strong>Term One</strong></dt><dd>Definition of term one.</dd>"
                + "</dl>"
                + "</body></html>";

        Document doc = runPandoc(html);
        Element list = findFirstDefinitionList(doc);

        assertEquals(list.getChildCount(), 1);
        Element item = (Element) list.getChild(0);
        assertEquals(item.getLocalName(), "BLOCK");
        assertEquals(item.getAttributeValue("type", BB_NS), "LIST_ITEM");

        Element termSpan = findDefinitionTermSpan(item);
        assertTrue(termSpan.getValue().contains("Term One"),
                "Term text should be present even when wrapped in <strong>");
    }

    // -------------------------------------------------------------------------
    // Multiple <dd> per <dt>
    // -------------------------------------------------------------------------
    @Test
    public void definitionListMultipleDefinitions() throws Exception {
        String html = "<html><body>"
                + "<dl>"
                + "<dt>Sphinx</dt>"
                + "<dd>A mythical creature with a human head.</dd>"
                + "<dd>Also a riddle-poser in Greek mythology.</dd>"
                + "</dl>"
                + "</body></html>";

        Document doc = runPandoc(html);
        Element list = findFirstDefinitionList(doc);

        // 2 LIST_ITEM blocks: first has the term span, second is a continuation
        assertEquals(list.getChildCount(), 2,
                "Expected 2 LIST_ITEM blocks for 1 dt + 2 dd");

        Element first = (Element) list.getChild(0);
        assertDefinitionItem(first, "Sphinx", "A mythical creature with a human head.");

        Element second = (Element) list.getChild(1);
        assertEquals(second.getAttributeValue("type", BB_NS), "LIST_ITEM");
        assertTrue(second.getValue().contains("Also a riddle-poser"),
                "Second dd should appear as a continuation LIST_ITEM");
    }

    // -------------------------------------------------------------------------
    // Regression: FixNestedList must not throw NPE on a DEFINITION list
    // -------------------------------------------------------------------------
    @Test
    public void fixNestedListDoesNotCrashOnDefinitionList() throws Exception {
        String html = "<html><body>"
                + "<dl><dt>Alpha</dt><dd>First letter.</dd></dl>"
                + "</body></html>";

        // If FixNestedList crashes, runPandoc throws — this test catches that
        Document doc = runPandoc(html);
        assertNotNull(doc, "Document should not be null after fixers run");
        assertFalse(doc.toXML().isEmpty(), "Document XML should not be empty");
    }

    // -------------------------------------------------------------------------
    // End-to-end: Cryptids of Cornwall sample file
    // -------------------------------------------------------------------------
    @Test
    public void cryptidsOfCornwallTestFile() throws Exception {
        // Load the HTML from test resources (not from the personal ken-work scratch folder)
        try (java.io.InputStream is = PandocLuaDefinitionListTest.class
                .getResourceAsStream("definition-list-cryptids.html")) {
            assertNotNull(is, "Test resource definition-list-cryptids.html not found on classpath");
            String html = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);

            Document doc = runPandoc(html);

            // The intro paragraph must be present
            Nodes paras = doc.query("//*[local-name()='BLOCK' and contains(.,'Cryptids of Cornwall')]");
            assertTrue(paras.size() > 0, "Intro paragraph 'Cryptids of Cornwall' should be present");

            // The definition list itself must be present with all three entries
            Element list = findFirstDefinitionList(doc);
            assertEquals(list.getChildCount(), 3,
                    "Expected 3 LIST_ITEM blocks for Beast of Bodmin, Morgawr, Owlman");

            assertDefinitionItem((Element) list.getChild(0), "Beast of Bodmin", "A large feline");
            assertDefinitionItem((Element) list.getChild(1), "Morgawr", "sea serpent");
            assertDefinitionItem((Element) list.getChild(2), "Owlman", "owl-like creature");
        }
    }
}

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
package org.brailleblaster.utd.config;

import java.io.File;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Function;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParentNode;
import org.apache.commons.lang3.StringUtils;
import org.brailleblaster.utd.ActionMap;
import org.brailleblaster.utd.BrailleSettings;
import org.brailleblaster.utd.NamespaceMap;
import org.brailleblaster.utd.PageSettings;
import org.brailleblaster.utd.Style;
import org.brailleblaster.utd.StyleMap;
import org.brailleblaster.utd.actions.BoldAction;
import org.brailleblaster.utd.internal.xml.XMLHandler;
import org.brailleblaster.utd.matchers.NodeNameMatcher;
import org.brailleblaster.utd.matchers.XPathMatcher;
import org.brailleblaster.utd.properties.PageNumberPosition;
import org.brailleblaster.utd.properties.UTDElements;
import org.brailleblaster.utils.NamespacesKt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.testng.Assert.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

//TODO: Copy pasted methods could probably be condensed with a lambda and a DataProvider
public class DocumentUTDConfigTest {
	private static final Logger log = LoggerFactory.getLogger(DocumentUTDConfigTest.class);	
	private static final PageNumberPosition TEST_POSITION = PageNumberPosition.BOTTOM_LEFT;
	private static final String TEST_TABLE = "This is not the table your looking for";

	@DataProvider
	public Object[][] testExecDataProvider() {
		PageSettings pageSettings = new PageSettings();
		pageSettings.setLeftMargin(31.75);
		pageSettings.setRightMargin(12.3);
		assertNotSame(pageSettings.getEvenPrintPageNumberAt(), TEST_POSITION, "Defaults changed, update test");
		pageSettings.setEvenPrintPageNumberAt(TEST_POSITION);
		//Must set default value as empty strings cause XML elements to serialize or deserialize as <elem /> or <elem></elem>
		pageSettings.setRunningHead("Test Running Head");

		BrailleSettings brailleSettings = new BrailleSettings();
		assertNotSame(brailleSettings.getMainTranslationTable(), TEST_TABLE, "Defaults changed, update test");
		brailleSettings.setMainTranslationTable(TEST_TABLE);
		
		StyleDefinitions styleDefs = new StyleDefinitions();
		Style style1 = new Style();
		style1.setName("test");
		styleDefs.addStyle(style1);
		Style style2 = new Style();
		style2.setName("Test Style");
		styleDefs.addStyle(style2);
		
		StyleMap styleMap = new StyleMap();
		styleMap.getNamespaces().addNamespace("testutd", "http://www.w3.org/555555/xhtml");
		styleMap.put(new NodeNameMatcher("testbook"), style2);
		styleMap.put(new XPathMatcher("//p"), style1);
		
		ActionMap actionMap = new ActionMap();
		actionMap.getNamespaces().addNamespace("ddd", "http://www.w3.org/555555/xhtml");
		actionMap.put(new NodeNameMatcher("somebook"), new BoldAction());
		
		DocumentUTDConfig docConfig = DocumentUTDConfig.NIMAS;

		//Can't assign different lambda types to object arrays, so super-test hack
		ThrowsRun[] tests = new ThrowsRun[]{
			//
			() -> settingsLoadTest(pageSettings, docConfig::loadPageSettings),
			() -> settingsLoadTest(brailleSettings, docConfig::loadBrailleSettings),
			() -> settingsLoadTest(styleMap, (Document doc) -> docConfig.loadStyle(doc, styleDefs)),
//			() -> settingsLoadTest(actionMap, NimasUTDConfig::loadActions),
			//
			() -> settingsLoadMissingTest(PageSettings.class, docConfig::loadPageSettings),
			() -> settingsLoadMissingTest(BrailleSettings.class, docConfig::loadBrailleSettings),
			() -> settingsLoadMissingTest(StyleMap.class, (Document doc) -> docConfig.loadStyle(doc, styleDefs)),
			() -> settingsLoadMissingTest(ActionMap.class, docConfig::loadActions),
			//
			() -> settingsSaveTest(pageSettings, PageSettings.class, docConfig::savePageSettings),
			() -> settingsSaveTest(brailleSettings, BrailleSettings.class, docConfig::saveBrailleSettings),
			() -> settingsSaveTest(styleMap, StyleMap.class, docConfig::saveStyle),
			() -> settingsSaveTest(actionMap, ActionMap.class, docConfig::saveActions),
			//
			() -> settingsSaveOverwriteTest(pageSettings, PageSettings.class, docConfig::savePageSettings),
			() -> settingsSaveOverwriteTest(brailleSettings, BrailleSettings.class, docConfig::saveBrailleSettings),
			() -> settingsSaveOverwriteTest(styleMap, StyleMap.class, docConfig::saveStyle),
			() -> settingsSaveOverwriteTest(actionMap, ActionMap.class, docConfig::saveActions),
		};
		return Arrays.stream(tests)
				.map((ThrowsRun run) -> new ThrowsRun[]{run})
				.toArray(ThrowsRun[][]::new);
	}

	@FunctionalInterface
	public interface ThrowsRun {
		void run();
	}

	@Test(dataProvider = "testExecDataProvider")
	public void testExec(ThrowsRun run) throws Exception {
		run.run();
	}

	public <S> void settingsLoadTest(S expected, Function<Document, S> loadConfig) {
		Document doc = loadNimas();
		S fromBook = loadConfig.apply(doc);

		assertEquals(fromBook, expected);
	}

	public <S> void settingsLoadMissingTest(Class<S> type, Function<Document, S> loadConfig) {
		Document doc = loadNimas();
		getConfigElement(doc, type).detach();

		S docSettings = loadConfig.apply(doc);
		assertNull(docSettings, "Unknown settings, should of been removed");
	}

	public <S> void settingsSaveTest(S expected, Class<S> type, BiConsumer<Document, S> saveConfig) {
		//Doc for NimasUTDConfig with no config element
		Document doc = loadNimas();
		Element configElem = getConfigElement(doc, type);
		configElem.detach();

		//Run JAXB
		saveConfig.accept(doc, expected);

		//Reference doc with origional element where JAXB would put it
		Document docOrig = doc.getDocument();
		docOrig.getRootElement()
				.getChildElements("head", docOrig.getRootElement().getNamespaceURI())
				.get(0)
				.appendChild(configElem);

		assertEquals(doc.toXML(), docOrig.toXML());
	}

	public <S> void settingsSaveOverwriteTest(S expected, Class<S> type, BiConsumer<Document, S> saveConfig) {
		//Load config moving element where jaxb would put it
		Document docOrig = loadNimas();
		Element configElem = getConfigElement(docOrig, type);
		ParentNode configElemParent = configElem.getParent();
		configElemParent.removeChild(configElem);
		configElemParent.appendChild(configElem);

		//Run JAXB
		Document docTest = docOrig.copy();
		saveConfig.accept(docTest, expected);
		
		log.debug("size of docTest {} size of orig {}", 
				docTest.getRootElement().getChildElements("head", docTest.getRootElement().getNamespaceURI()).get(0).getChildElements("styleMap", NamespacesKt.UTD_NS).size(),
				docOrig.getRootElement().getChildElements("head", docTest.getRootElement().getNamespaceURI()).get(0).getChildElements("styleMap", NamespacesKt.UTD_NS).size()
		);
		

		assertEquals(docTest.toXML(), docOrig.toXML());
	}

	private static Document loadNimas() {
		return new XMLHandler().load(new File("src/test/resources/org/brailleblaster/utd/config/nimas.xml"));
	}

	private static Element getConfigElement(Document doc, Class<?> settingsClass) {
		Element root = doc.getRootElement();
		String xmlName = StringUtils.uncapitalize(settingsClass.getSimpleName());
        return root.getChildElements("head", root.getNamespaceURI())
				.get(0)
				.getChildElements(xmlName, NamespacesKt.UTD_NS)
				.get(0);
	}
	
	@Test
	public void testSaveStyleWithDefs() {
		Style baseStyle = new Style();
		baseStyle.setName(StyleDefinitions.DEFAULT_STYLE);
		baseStyle.setId(StyleDefinitions.DEFAULT_STYLE);
		baseStyle.setLinesBefore(1);
		
		Style newStyle = new Style(baseStyle, "New Style", "New Style");
		newStyle.setLinesBefore(5);
		assertEquals(newStyle.getBaseStyle(), baseStyle);
		assertEquals(newStyle.getBaseStyleName(), baseStyle.getName());
		
		StyleDefinitions styleDefs = new StyleDefinitions();
		styleDefs.addStyle(baseStyle);
		
		StyleMap styleMap = new StyleMap();
		styleMap.put(new NodeNameMatcher("something"), newStyle);
		//JAXB code needs a NamespaceMap to load
		styleMap.setNamespaces(new NamespaceMap(UTDElements.UTD_PREFIX, NamespacesKt.UTD_NS));
		
		//Dummy document to save styleMap into
		Document doc;
		{
			Element root = new Element("root");
			
			Element head = new Element("head");
			root.appendChild(head);
			
			doc = new Document(root);
		}
		DocumentUTDConfig.NIMAS.saveStyleWithDefs(doc, styleMap);
		
		styleMap = DocumentUTDConfig.NIMAS.loadStyle(doc, styleDefs);
		assertEquals(styleMap.size(), 1);
		assertEquals(((Style)styleMap.getValue(0)).getBaseStyle(), baseStyle);
		assertEquals(((Style)styleMap.getValue(0)).getBaseStyleName(), baseStyle.getName());
	}
}

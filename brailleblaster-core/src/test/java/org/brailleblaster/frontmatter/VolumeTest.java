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

import nu.xom.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.brailleblaster.BBIni;
import org.brailleblaster.bbx.BBX;
import org.brailleblaster.bbx.BBXUtils;
import org.brailleblaster.frontmatter.VolumeSaveDialog.Format;
import org.brailleblaster.perspectives.braille.messages.Sender;
import org.brailleblaster.perspectives.mvc.XMLTextCaret;
import org.brailleblaster.perspectives.mvc.events.XMLCaretEvent;
import org.brailleblaster.perspectives.mvc.menu.TopMenu;
import org.brailleblaster.perspectives.mvc.modules.misc.VolumeChangeModule;
import org.brailleblaster.perspectives.mvc.modules.misc.VolumeInsertModule;
import org.brailleblaster.search.GoToPageDialog;
import org.brailleblaster.testrunners.BBTestRunner;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.brailleblaster.testrunners.ViewTestRunner;
import org.brailleblaster.utd.exceptions.NodeException;
import org.brailleblaster.utd.internal.xml.FastXPath;
import org.brailleblaster.utd.internal.xml.XMLHandler;
import org.brailleblaster.utd.properties.UTDElements;
import org.brailleblaster.utd.utils.UTDHelper;
import org.eclipse.swt.SWT;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.utils.TableCollection;
import org.eclipse.swtbot.swt.finder.utils.TableRow;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.testng.Assert.assertEquals;

public class VolumeTest {
	private static final Logger log = LoggerFactory.getLogger(VolumeTest.class);

	public static void openInsertVolume(BBTestRunner test, BBX.VolumeType type) {
		test.openMenuItem(
				VolumeInsertModule.MENU_VOLUME_MANAGER_TOP, 
				VolumeInsertModule.MENU_VOLUME_MANAGER_NAME, 
				VolumeInsertModule.MENU_INSERT_VOLUME, 
				type.volumeMenuName
		);
	}
	
	@Test(enabled = false)
	public void insertVolumeParagraphListTest_issue3695() {
		BBTestRunner bbTest = new BBTestRunner("",
				"<p testid='first'>Paragraph 1</p><list testid='list'><li><p>Paragraph 2</p></li></list>");

		//The first list tag
		bbTest.textViewTools.navigateToText("Paragraph 2");
		bbTest.selectBreadcrumbsAncestor(1, BBX.CONTAINER.LIST::assertIsA);
		openInsertVolume(bbTest, BBX.VolumeType.VOLUME);

		Document doc = bbTest.getDoc();
		UTDHelper.stripUTDRecursive(doc);
		List<Element> volumes = VolumeUtils.getVolumeElements(doc);
		assertEquals(volumes.size(), 2);
		assertMatches(
				XMLHandler.nextSiblingNode(volumes.get(0)),
				TestXMLUtils.getTestIdElement(doc, "list")
		);
		assertMatches(
				XMLHandler.previousSiblingNode(volumes.get(1)),
				TestXMLUtils.getTestIdElement(doc, "list")
		);
	}

	@Test(enabled = false)
	public void insertVolumeTableTest_issue3695() {
		//Issue #3695
		BBTestRunner bbTest = new BBTestRunner("", "<p testid='first'>testing</p>"
				+ "<table testid='table'>"
				+ "<tr><td>Testing 1</td><td>testing 2</td></tr>"
				+ "<tr><td>Testing 3</td><td>testing 4</td></tr>"
				+ "</table>");

		//The first list tag
		bbTest.textViewTools.navigateToText("Testing 1");
		openInsertVolume(bbTest, BBX.VolumeType.VOLUME);

		Document doc = bbTest.getDoc();
		UTDHelper.stripUTDRecursive(doc);
		List<Element> volumes = VolumeUtils.getVolumeElements(doc);
		assertEquals(volumes.size(), 2);
		assertMatches(
				XMLHandler.nextSiblingNode(volumes.get(0)),
				TestXMLUtils.getTestIdElement(doc, "table")
		);
		assertMatches(
				XMLHandler.previousSiblingNode(volumes.get(1)),
				TestXMLUtils.getTestIdElements(doc, "table").get(1) //table copy
		);
	}

	@Test(enabled = false)
	public void insertVolumesParagraphText() {
		BBTestRunner bbTest = new BBTestRunner("",
				"<p testid='p1'>Paragraph 1</p><p testid='p2'>Paragraph 2</p><p testid='p3'>Paragraph 3</p>");

		//Insert in middle of word
		bbTest.textViewTools.navigateToText("aragraph 2");
		openInsertVolume(bbTest, BBX.VolumeType.VOLUME);

		Document doc = bbTest.manager.getDoc();
		List<Element> volumes = VolumeUtils.getVolumeElementsFatal(doc);
		assertEquals(volumes.size(), 2);
		assertEquals(
				XMLHandler.nextSiblingNode(volumes.get(0)),
				TestXMLUtils.getTestIdElement(doc, "p2")
		);
		assertEquals(
				XMLHandler.previousSiblingNode(volumes.get(1)),
				TestXMLUtils.getTestIdElement(doc, "p3")
		);
	}

	@Test(enabled = false)
	public void insertManyVolumes() {
		// While the literature book is in the repo, might as well use it as its much bigger
//		BBTestRunner bbTest = new BBTestRunner(new File("src/tests/resources/nimasbaseline/NIMASXMLGtDepJan2009_valid3.xml"));
//		int NUM_VOLUMES = 10;
		BBTestRunner bbTest = new BBTestRunner(new File("/media/brailleblaster/nimas-books/9780133268195NIMAS_revised.xml"));
		int NUM_VOLUMES = 20;

		int textNodeCount = (int) StreamSupport.stream(FastXPath.descendant(BBX.getRoot(bbTest.getDoc())).spliterator(), false)
				.filter(curNode -> curNode instanceof Text)
				.count();
		int textNodesPerVolume = textNodeCount / NUM_VOLUMES;
		log.trace("{} textNodes / {} Volumes = {} nodes per volume",
				NUM_VOLUMES,
				textNodeCount,
				textNodesPerVolume
		);

		List<Node> volumizedNodes = new LinkedList<>(); //Keeps track of insertions in case test fails
		Element startNode = BBX.getRoot(bbTest.getDoc());
		startNode.addAttribute(new Attribute("insertManyVolumes", "mee"));
		Outer:
		for (int i = 0; i < NUM_VOLUMES - 1; i++) {
			ViewTestRunner.doPendingSWTWork();
			bbTest.manager.waitForFormatting(true);
			ViewTestRunner.doPendingSWTWork();
			//find usable text node to scroll to
			log.debug("startNode doc " + startNode.getDocument());
			Element startNodeProcessing = StreamSupport.stream(FastXPath.descendantAndFollowing(bbTest.getDoc()).spliterator(), false)
					.filter(curNode -> curNode instanceof Element && ((Element) curNode).getAttribute("insertManyVolumes") != null)
					.findFirst()
					.map(curNode -> (Element) curNode)
					.orElseThrow(() -> new NodeException("failed to find insertManyVolumes", bbTest.getDoc()));
			startNodeProcessing.removeAttribute(startNodeProcessing.getAttribute("insertManyVolumes"));
			int endOffset = -1;
			Text nodeToVolumize = null;
			do {
				endOffset++;
				nodeToVolumize = (Text) StreamSupport.stream(FastXPath.descendantAndFollowing(startNodeProcessing).spliterator(), false)
                        .filter(curNode -> curNode instanceof Text)
                        .skip(textNodesPerVolume + endOffset)
                        .filter(curNode -> !BBXUtils.isPageNumAncestor(curNode))
                        .findFirst().orElse(null);
				if (nodeToVolumize == null) {
					//end of document
					break Outer;
				}
			} while (XMLHandler.Companion.ancestorVisitor(
					nodeToVolumize,
					curNode -> BBX.CONTAINER.TABLE.isA(curNode)
					|| BBX.CONTAINER.VOLUME.isA(curNode)
					|| UTDElements.BRL.isA(curNode)
					//workaround for lambda final variable nonsense
					|| (curNode instanceof Text && curNode.getValue().isEmpty())
			) != null);
			Element parent = (Element) nodeToVolumize.getParent();
			parent.addAttribute(new Attribute("insertManyVolumes", "mee"));
			startNode = parent;

			log.debug("Search #{} for {} in {}",
					i,
					nodeToVolumize,
					nodeToVolumize.getParent().toXML()
			);

			ViewTestRunner.doPendingSWTWork();
			volumizedNodes.add(nodeToVolumize);

			try{
				/*
				TODO #4713: Ideally this should not be needed. But without it there's 
				a significant number of thread racing and deadlocks, more than there
				was before when this was commented out
				*/
				bbTest.manager.waitForFormatting(true);
				ViewTestRunner.doPendingSWTWork();
				bbTest.manager.getSimpleManager().dispatchEvent(new XMLCaretEvent(Sender.SIMPLEMANAGER, new XMLTextCaret(nodeToVolumize, 0)));
				ViewTestRunner.doPendingSWTWork();
				bbTest.manager.waitForFormatting(true);
				ViewTestRunner.doPendingSWTWork();
				
				//sanity check
				String firstWord = nodeToVolumize.getValue();
				if (firstWord.contains(" ")) {
					firstWord = firstWord.substring(0, firstWord.indexOf(' '));
				}
				ViewTestRunner.doPendingSWTWork();
				assertThat(bbTest.textViewBot.getTextOnCurrentLine(), containsString(firstWord));
				if (true) {
	//				bbTest.manager.waitForFormatting(true);
				}
				openInsertVolume(bbTest, BBX.VolumeType.VOLUME);
				ViewTestRunner.doPendingSWTWork();
			} catch (Exception e){
				StringBuilder sb = new StringBuilder();
				for(int nodeNum = 0; nodeNum < volumizedNodes.size(); nodeNum++){
					sb.append(nodeNum).append(": ").append(volumizedNodes.get(nodeNum).toXML()).append(nodeNum == volumizedNodes.size() - 1 ? "" : System.lineSeparator());
				}
				throw new RuntimeException("Exception after inserting following volumes:" + System.lineSeparator() + sb, e);
			}
			//bbTestRunner reset?
			bbTest.textViewBot.setFocus();
			ViewTestRunner.doPendingSWTWork();

			bbTest.textViewBot.navigateTo(0, 0);
			ViewTestRunner.doPendingSWTWork();
		}
	}
	
	@Test(enabled = false )
	public void insertManyVolumes_byPage() {
		BBTestRunner test = new BBTestRunner(new File("/media/brailleblaster/nimas-books/9780133268195NIMAS_revised.xml"));
		int PRINT_PAGE_COUNTER = 100;
		
		for (int i = 1; i <= 9; i++) {
			int curPrintPage = i * PRINT_PAGE_COUNTER;
			
			test.openMenuItem(TopMenu.NAVIGATE, "Go To Page");
			
			SWTBot goToBot = test.bot.activeShell().bot();
			goToBot.text(0).setText("" + curPrintPage);
			ViewTestRunner.doPendingSWTWork();
			goToBot.radio(GoToPageDialog.PageType.PRINT.ordinal()).click();
			ViewTestRunner.doPendingSWTWork();
			goToBot.button(0).click();
			
			test.openMenuItem(TopMenu.TOOLS, "Volume Manager", "Insert", "Normal");
		}
	}
	
	@Test(enabled = false)
	public void deleteVolume() {
		StringBuilder outputXML = new StringBuilder();
		for (int i = 0; i < 100; i++) {
			outputXML.append("<p>paragraph ")
					.append(i)
					.append("</p>");
		}

		BBTestRunner bbTest = new BBTestRunner("", outputXML.toString());

		//Note: This splits the book into prelim 1 (actual first), and prelim 2 (pushed to the end of the document and called 4)
		bbTest.textViewTools.navigateToTextRelative("paragraph 10");
		openInsertVolume(bbTest, BBX.VolumeType.VOLUME);

		bbTest.textViewTools.navigateToTextRelative("paragraph 20");
		openInsertVolume(bbTest, BBX.VolumeType.VOLUME);
		
		List<VolumeUtils.VolumeData> volumeNames = VolumeUtils.INSTANCE.getVolumeNames(VolumeUtils.getVolumeElementsFatal(bbTest.getDoc()));
		assertEquals(volumeNames.size(), 3);
		
		bbTest.textViewTools.navigateToTextRelative("paragraph 15");
		bbTest.openMenuItem(VolumeInsertModule.MENU_VOLUME_MANAGER_TOP,
				VolumeInsertModule.MENU_VOLUME_MANAGER_NAME, 
				VolumeChangeModule.MENU_DELETE_CURRENT_VOLUME
		);
		
		volumeNames = VolumeUtils.INSTANCE.getVolumeNames(VolumeUtils.getVolumeElementsFatal(bbTest.getDoc()));
		assertEquals(volumeNames.size(), 2);
		VolumeUtils.VolumeData volumeData = volumeNames.get(0);
		assertEquals(volumeData.volumeTypeIndex, 1);
		UTDHelper.stripUTDRecursive(volumeData.element);
		assertEquals(volumeData.element.getChild(0).getValue(), "END OF VOLUME 1");
		
		bbTest.textViewTools.navigateToTextRelative("paragraph 25");
		bbTest.openMenuItem(VolumeInsertModule.MENU_VOLUME_MANAGER_TOP,
				VolumeInsertModule.MENU_VOLUME_MANAGER_NAME, 
				VolumeChangeModule.MENU_DELETE_CURRENT_VOLUME
		);
		
		assertEquals(VolumeUtils.getVolumeElements(bbTest.getDoc()).size(), 0);
	}

	@Test(enabled = false)
    
	public void saveSingleVolumeToBRF() throws IOException {
		StringBuilder outputXML = new StringBuilder();
		for (int i = 0; i < 25; i++) {
			outputXML.append("<p>paragraph ")
					.append(i)
					.append("</p>");
		}

		BBTestRunner bbTest = new BBTestRunner("", outputXML.toString());

		bbTest.textViewTools.navigateToText("paragraph 10");
		openInsertVolume(bbTest, BBX.VolumeType.VOLUME);

		assertThat(
				VolumeUtils.getVolumeElements(bbTest.getDoc()),
				iterableWithSize(2)
		);

		String[] resultBRF = new String[2];
		for (int i = 0; i < resultBRF.length; i++) {
			log.debug("--------------- starting " + i + " ----------------");
			//for some reason this is needed?
			ViewTestRunner.forceActiveShellHack();
			bbTest.openMenuItem(TopMenu.FILE, "Save BRF");

			SWTBot saveBot = bbTest.bot.activeShell().bot();
			SWTBotTable table = saveBot.table(0);
			assertEquals(table.rowCount(), 2, "Unexpected number of volumes");

			table.select(i);
			ViewTestRunner.doPendingSWTWork();
			TableCollection tableSelection = table.selection();
			assertEquals(tableSelection.rowCount(), 1, "Selected more than 1?");

			TableRow selectedRow = tableSelection.get(0);
			assertEquals(selectedRow.get(0), "Volume " + (i + 1));

			saveBot.buttonWithId(VolumeSaveDialog.SWTBOT_SAVE_FOLDER).click();
			ViewTestRunner.doPendingSWTWork();

			resultBRF[i] = FileUtils.readFileToString(bbTest.debugSaveFile, StandardCharsets.UTF_8);
		}

		assertEquals(
				StringUtils.countMatches(resultBRF[0], System.lineSeparator()),
				25,
				"Braille: " + System.lineSeparator() + resultBRF[0]
		);
		assertEquals(
				StringUtils.countMatches(resultBRF[1], System.lineSeparator()),
				25,
				"Braille: " + System.lineSeparator() + resultBRF[1]
		);
		Assert.assertNotEquals(resultBRF[0], resultBRF[1]);

		log.debug("File 1: " + resultBRF[0]);
		log.debug("File 2: " + resultBRF[1]);
	}

	@Test(enabled = false)
	public void saveMultipleVolumesToBRF() throws IOException {
		StringBuilder outputXML = new StringBuilder();
		for (int i = 0; i < 25; i++) {
			outputXML.append("<p>paragraph ")
					.append(i)
					.append("</p>");
		}

		BBTestRunner bbTest = new BBTestRunner("", outputXML.toString());

		bbTest.textViewTools.navigateToText("paragraph 10");
		openInsertVolume(bbTest, BBX.VolumeType.VOLUME);

		assertThat(
				VolumeUtils.getVolumeElements(bbTest.getDoc()),
				iterableWithSize(2)
		);

		bbTest.openMenuItem(TopMenu.FILE, "Save BRF");

		SWTBot saveBot = bbTest.bot.activeShell().bot();
		SWTBotTable table = saveBot.table(0);
		assertEquals(table.rowCount(), 2, "Unexpected number of volumes");

		table.select(0, 1);
		ViewTestRunner.doPendingSWTWork();
		TableCollection tableSelection = table.selection();
		assertEquals(tableSelection.rowCount(), 2, "Selected more than 1?");

		assertEquals(tableSelection.get(0).get(0), "Volume 1");
		assertEquals(tableSelection.get(1).get(0), "Volume 2");

		saveBot.buttonWithId(VolumeSaveDialog.SWTBOT_SAVE_FOLDER).click();
		ViewTestRunner.doPendingSWTWork();

		String volume1 = FileUtils.readFileToString(
				VolumeSaveDialog.getBRFPath(
						bbTest.debugFile.toPath(),
						BBX.VolumeType.VOLUME,
						1,
						Format.BRF
				), StandardCharsets.UTF_8
		);
		String volume2 = FileUtils.readFileToString(
				VolumeSaveDialog.getBRFPath(
						bbTest.debugFile.toPath(),
						BBX.VolumeType.VOLUME,
						2,
						Format.BRF
				), StandardCharsets.UTF_8
		);

		assertEquals(
				StringUtils.countMatches(volume1, System.lineSeparator()),
				25,
				"Braille: " + System.lineSeparator() + volume1
		);
		assertEquals(
				StringUtils.countMatches(volume2, System.lineSeparator()),
				25,
				"Braille: " + System.lineSeparator() + volume2
		);
		Assert.assertNotEquals(volume1, volume2);

		log.debug("File 1: " + volume1);
		log.debug("File 2: " + volume2);
	}

	private void assertMatches(Node left, Node right) {
		if (!left.equals(right)) {
			((Element) left).addAttribute(new Attribute("match", "left"));
			((Element) right).addAttribute(new Attribute("match", "right"));
			throw new NodeException("Node left " + left + " does not match right" + right, left);
		}
	}

	@Test(enabled = false)
	public void saveMultipleVolumes_fileNaming_rt4541() {
		StringBuilder outputXML = new StringBuilder();
		for (int i = 0; i < 100; i++) {
			outputXML.append("<p>paragraph ")
					.append(i)
					.append("</p>");
		}

		BBTestRunner bbTest = new BBTestRunner("", outputXML.toString());

		//Note: This splits the book into prelim 1 (actual first), and prelim 2 (pushed to the end of the document and called 4)
		bbTest.textViewTools.navigateToTextRelative("paragraph 10");
		openInsertVolume(bbTest, BBX.VolumeType.VOLUME_PRELIMINARY);

		bbTest.textViewTools.navigateToTextRelative("paragraph 20");
		openInsertVolume(bbTest, BBX.VolumeType.VOLUME_PRELIMINARY);

		bbTest.textViewTools.navigateToTextRelative("paragraph 30");
		openInsertVolume(bbTest, BBX.VolumeType.VOLUME_PRELIMINARY);

		bbTest.textViewTools.navigateToTextRelative("paragraph 40");
		openInsertVolume(bbTest, BBX.VolumeType.VOLUME);

		bbTest.textViewTools.navigateToTextRelative("paragraph 50");
		openInsertVolume(bbTest, BBX.VolumeType.VOLUME);

		bbTest.textViewTools.navigateToTextRelative("paragraph 60");
		openInsertVolume(bbTest, BBX.VolumeType.VOLUME);

		bbTest.textViewTools.navigateToTextRelative("paragraph 70");
		openInsertVolume(bbTest, BBX.VolumeType.VOLUME_SUPPLEMENTAL);

		bbTest.textViewTools.navigateToTextRelative("paragraph 80");
		openInsertVolume(bbTest, BBX.VolumeType.VOLUME_SUPPLEMENTAL);

		bbTest.textViewTools.navigateToTextRelative("paragraph 90");
		openInsertVolume(bbTest, BBX.VolumeType.VOLUME_SUPPLEMENTAL);

		assertThat(
				VolumeUtils.getVolumeElements(bbTest.getDoc()),
				iterableWithSize(10)
		);

		bbTest.openMenuItem(TopMenu.FILE, "Save BRF");
		SWTBot saveBot = bbTest.bot.activeShell().bot();
		if (false) {
			SWTBotTable volumesTable = saveBot.table(0);
			assertEquals(volumesTable.rowCount(), 10);
			saveBot.table(0).select(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
			ViewTestRunner.doPendingSWTWork();
			saveBot.buttonWithId(VolumeSaveDialog.SWTBOT_SAVE_FOLDER).click();
		} else {
			saveBot.buttonWithId(VolumeSaveDialog.SWTBOT_SAVE_FOLDER_ALL).click();
		}
		ViewTestRunner.doPendingSWTWork();

		Path brfSaveDir = BBIni.getDebugSavePath().getParent();
		String documentName = BBIni.getDebugFilePath().getFileName().toString();

		assertFileExists(brfSaveDir, documentName, "preliminary1");
		assertFileExists(brfSaveDir, documentName, "preliminary2");
		assertFileExists(brfSaveDir, documentName, "preliminary3");
		assertFileExists(brfSaveDir, documentName, "volume1");
		assertFileExists(brfSaveDir, documentName, "volume2");
		assertFileExists(brfSaveDir, documentName, "volume3");
		assertFileExists(brfSaveDir, documentName, "supplemental1");
		assertFileExists(brfSaveDir, documentName, "supplemental2");
		assertFileExists(brfSaveDir, documentName, "supplemental3");
		assertFileExists(brfSaveDir, documentName, "supplemental4");
	}

	private static void assertFileExists(Path root, String documentName, String suffix) {
		Path actualPath = root.resolve(documentName + "_" + suffix + ".brf");
		assertFileExists(actualPath);
	}

	private static void assertFileExists(Path path) {
		Assert.assertTrue(Files.exists(path), "Path does not exist: " + path);
	}

	@Test(enabled = false)
	public void saveNoVolumesToBRF() throws IOException {
		StringBuilder outputXML = new StringBuilder();
		for (int i = 0; i < 40; i++) {
			outputXML.append("<p>paragraph ")
					.append(i)
					.append("</p>");
		}

		BBTestRunner bbTest = new BBTestRunner("", outputXML.toString());

		assertThat(
				VolumeUtils.getVolumeElements(bbTest.getDoc()),
				iterableWithSize(0)
		);

		bbTest.openMenuItem(TopMenu.FILE, "Save BRF");

		String resultBRF = FileUtils.readFileToString(bbTest.debugSaveFile, StandardCharsets.UTF_8);

		assertEquals(
				StringUtils.countMatches(resultBRF, System.lineSeparator()),
				50,
				"Braille: " + System.lineSeparator() + resultBRF
		);

		log.debug("File: " + resultBRF);
	}
	
	@Test(enabled = false)
	public void autoLastVolume_normalVolume_rt4764() {
		StringBuilder outputXML = new StringBuilder();
		for (int i = 0; i < 100; i++) {
			outputXML.append("<p>paragraph ")
					.append(i)
					.append("</p>");
		}

		BBTestRunner bbTest = new BBTestRunner("", outputXML.toString());

		//Note: This splits the book into prelim 1 (actual first), and prelim 2 (pushed to the end of the document and called 4)
		bbTest.textViewTools.navigateToTextRelative("paragraph 10");
		openInsertVolume(bbTest, BBX.VolumeType.VOLUME_PRELIMINARY);

		bbTest.textViewTools.navigateToTextRelative("paragraph 20");
		openInsertVolume(bbTest, BBX.VolumeType.VOLUME);
		
		List<VolumeUtils.VolumeData> volumeNames = VolumeUtils.INSTANCE.getVolumeNames(VolumeUtils.getVolumeElementsFatal(bbTest.getDoc()));
		
		assertEquals(volumeNames.size(), 3);
		assertEquals(volumeNames.get(0).type, BBX.VolumeType.VOLUME_PRELIMINARY);
		assertEquals(volumeNames.get(1).type, BBX.VolumeType.VOLUME);
		assertEquals(volumeNames.get(2).type, BBX.VolumeType.VOLUME);
	}
	
	@Test(enabled = false)
	public void autoLastVolume_supplemental_rt4764() {
		StringBuilder outputXML = new StringBuilder();
		for (int i = 0; i < 100; i++) {
			outputXML.append("<p>paragraph ")
					.append(i)
					.append("</p>");
		}

		BBTestRunner bbTest = new BBTestRunner("", outputXML.toString());

		//Note: This splits the book into prelim 1 (actual first), and prelim 2 (pushed to the end of the document and called 4)
		bbTest.textViewTools.navigateToTextRelative("paragraph 10");
		openInsertVolume(bbTest, BBX.VolumeType.VOLUME_PRELIMINARY);

		bbTest.textViewTools.navigateToTextRelative("paragraph 20");
		openInsertVolume(bbTest, BBX.VolumeType.VOLUME_SUPPLEMENTAL);
		
		List<VolumeUtils.VolumeData> volumeNames = VolumeUtils.INSTANCE.getVolumeNames(VolumeUtils.getVolumeElementsFatal(bbTest.getDoc()));
		
		assertEquals(volumeNames.size(), 3);
		assertEquals(volumeNames.get(0).type, BBX.VolumeType.VOLUME_PRELIMINARY);
		assertEquals(volumeNames.get(1).type, BBX.VolumeType.VOLUME_SUPPLEMENTAL);
		assertEquals(volumeNames.get(2).type, BBX.VolumeType.VOLUME_SUPPLEMENTAL);
	}

	@Test(enabled = false)
	public void goToVolume() {
		StringBuilder outputXML = new StringBuilder();
		for (int i = 0; i < 40; i++) {
			outputXML.append("<p>paragraph ")
					.append(i)
					.append("</p>");
		}

		BBTestRunner bbTest = new BBTestRunner("", outputXML.toString());

		bbTest.textViewTools.navigateToText("paragraph 10");
		openInsertVolume(bbTest, BBX.VolumeType.VOLUME);

		bbTest.textViewBot.setFocus();
		bbTest.textViewTools.pressShortcut(SWT.CTRL, 'g');

		SWTBot goToBot = bbTest.bot.activeShell().bot();
		SWTBotCombo goToCombo = goToBot.comboBox(0);

		assertEquals(goToCombo.itemCount(), 3);
		assertEquals(goToCombo.items()[0], "");
		assertEquals(goToCombo.items()[1], "Volume 1");
		assertEquals(goToCombo.items()[2], "Volume 2");

		goToCombo.setSelection(1);
		ViewTestRunner.doPendingSWTWork();
		assertEquals(goToCombo.getText(), "Volume 1");

		goToBot.button(0).click();
		ViewTestRunner.doPendingSWTWork();

		//top of document
//		assertEquals(bbTest.textViewBot.cursorPosition().line, 0);
		assertEquals(bbTest.textViewBot.getTextOnCurrentLine(), "paragraph 0");

		//good, go to volume 2
		bbTest.textViewBot.setFocus();
		bbTest.textViewTools.pressShortcut(SWT.CTRL, 'g');

		goToBot = bbTest.bot.activeShell().bot();
		goToCombo = goToBot.comboBox(0);

		goToCombo.setSelection(2);
		ViewTestRunner.doPendingSWTWork();
		assertEquals(goToCombo.getText(), "Volume 2");

		goToBot.button(0).click();
		ViewTestRunner.doPendingSWTWork();

		assertEquals(
				bbTest.textViewBot.getTextOnCurrentLine(), 
				"paragraph 10", 
				"Text View: " + bbTest.textViewBot.getText()
		);
	}

	@Test(enabled = false)
	public void goToVolumeBraillePage() {
		StringBuilder outputXML = new StringBuilder();
		for (int i = 0; i < 40; i++) {
			outputXML.append("<p>paragraph ")
					.append(i)
					.append("</p>");
		}

		BBTestRunner bbTest = new BBTestRunner("", outputXML.toString());

		bbTest.textViewTools.navigateToText("paragraph 10");
		openInsertVolume(bbTest, BBX.VolumeType.VOLUME);

		bbTest.textViewBot.setFocus();
		bbTest.textViewTools.pressShortcut(SWT.CTRL, 'g');

		SWTBot goToBot = bbTest.bot.activeShell().bot();
		SWTBotCombo goToCombo = goToBot.comboBox(0);

		assertEquals(goToCombo.itemCount(), 3);
		assertEquals(goToCombo.items()[0], "");
		assertEquals(goToCombo.items()[1], "Volume 1");
		assertEquals(goToCombo.items()[2], "Volume 2");

		goToCombo.setSelection(1);
		ViewTestRunner.doPendingSWTWork();
		assertEquals(goToCombo.getText(), "Volume 1");

		//type braille page
		goToBot.text(0).typeText("1");
		ViewTestRunner.doPendingSWTWork();

		goToBot.button(0).click();
		ViewTestRunner.doPendingSWTWork();

		//top of document
//		assertEquals(bbTest.textViewBot.cursorPosition().line, 0);
		assertEquals(bbTest.textViewBot.getTextOnCurrentLine(), "END OF VOLUME 1");

		//good, go to volume 2
		bbTest.textViewBot.setFocus();
		bbTest.textViewTools.pressShortcut(SWT.CTRL, 'g');

		goToBot = bbTest.bot.activeShell().bot();
		goToCombo = goToBot.comboBox(0);

		goToCombo.setSelection(2);
		ViewTestRunner.doPendingSWTWork();
		assertEquals(goToCombo.getText(), "Volume 2");

		//type braille page
		goToBot.text(0).typeText("1");
		ViewTestRunner.doPendingSWTWork();

		goToBot.button(0).click();
		ViewTestRunner.doPendingSWTWork();

		assertEquals(bbTest.textViewBot.getTextOnCurrentLine(), "paragraph 34");
	}

//	@Test
//	public void insertNewVolumeExistingVolumeTest() {
//		doInsertNewVolumeTest(EmptyLambda::consumer);
//	}
//	@Test
//	public void insertNewVolumeNoExistingTest() {
//		doInsertNewVolumeTest((Manager manager) -> {
//			EasierFrontmatter.getVolumeElementsFatal(manager.getDoc()).get(0).detach();
//			assertEquals(EasierFrontmatter.getVolumeElements(manager.getDoc()).size(), 0);
//
//			manager.reformatDocument(manager.getDoc());
//			doPendingSWTWork();
//		});
//	}
//
//	private void doInsertNewVolumeTest(Consumer<Manager> beforeTest) {
//		BBViewTestRunner bbTest = new BBViewTestRunner("",
//				"<frontmatter><span utd:frontmatter-type='VOLUME'>"
//				+ "<span utd:frontmatter-type='VOLUME_END' utd:frontmatter-sort='10'>Placeholder</span>"
//				+ "</span></frontmatter>"
//				+ "<p>Paragraph 1</p>"
//				+ "<p>Paragraph 2</p>"
//				+ "<p>Paragraph 3</p>"
//		);
//		
//		beforeTest.accept(bbTest.manager);
//
//		bbTest.textViewBot.setFocus();
//		doPendingSWTWork();
//
//		bbTest.navigateTextViewToText("Paragraph 2");
//
//		bbTest.bot.menu("Insert Volume before Cursor").click();
//		doPendingSWTWork();
//
//		//Make testing easier
//		UTDHelper.stripUTDRecursive(bbTest.manager.getDoc());
//
//		List<Element> volumes = EasierFrontmatter.getVolumeElementsFatal(bbTest.manager.getDoc());
//		assertEquals(volumes.size(), 2);
//		assertEquals(XMLHandler.findFirstText(volumes.get(1)).getValue(), "END OF VOLUME 1",
//				"Unexpected volume contents " + UTDHelper.toXMLnoBRL(volumes.get(1)));
//
//		Nodes endElements = XMLHandler.query(bbTest.manager.getDoc(),
//				"descendant::*[@utd:{}='{}']",
//				EasierFrontmatter.TYPE_ATTRIB,
//				EasierFrontmatter.Types.VOLUME_END.name());
//		assertEquals(endElements.size(), 2);
//		assertEquals(endElements.get(0).toXML(),
//				"<span utd:frontmatter-type=\"VOLUME_END\" utd:frontmatter-sort=\"10\" utd:firstVolume=\"true\">First Volume Placeholder</span>");
//		assertEquals(endElements.get(1).toXML(),
//				"<span utd:frontmatter-type=\"VOLUME_END\" utd:frontmatter-sort=\"10\">END OF VOLUME 1</span>");
//
//		//When insertNewVolumeNoExistingTest removes all volumes, make sure the auto inserted one is placed correctly
//		assertEquals(((Element) volumes.get(0).getParent()).getLocalName(), "frontmatter");
//	}
//	@DataProvider
//	public Iterator<Object[]> insertNewVolumePrintPageTestDataProvider() {
//		List<Object[]> result = new ArrayList<>();
//		for (int i = 0; i < 41; i = i + 1) {
//			result.add(new Object[]{i});
//		}
//		return result.iterator();
////		return ImmutableList.of(new Object[]{8}).iterator();
//	}
//
//	@Test(dataProvider = "insertNewVolumePrintPageTestDataProvider", enabled = false)
//	public void insertNewVolumePrintPageTest(int lineOffset) {
//		WPManager wpManager = startBB(TOC_RAW_FILE);
//		Manager manager = getManager(wpManager);
//		StyledText textView = manager.getTextView();
//
//		//Page nums are weird
//		navigateTextView(textView.getOffsetAtLine(12) + lineOffset);
//		MatcherAssert.assertThat(textViewBot.getTextOnCurrentLine(), startsWith("------"));
//
//		bot.menu("Insert Volume before Cursor").click();
//		doPendingSWTWork();
//	}
//	@Test
//	public void insertMultipleNewVolumes2() {
//		BBViewTestRunner bbTest = new BBViewTestRunner("", ""
//				+ "<p>Paragraph 1</p>"
//				+ "<p>Paragraph 2</p>"
//				+ "<p>Paragraph 3</p>");
//
//		bbTest.navigateTextViewToText("Paragraph 2");
//		bbTest.bot.menu("Insert Preliminary Volume").click();
//		doPendingSWTWork();
//		ImmutableList<Element> volumes = EasierFrontmatter.getVolumeElementsFatal(bbTest.manager.getDoc());
//		assertEquals(volumes.size(), 2);
//		assertEquals(TestXMLUtils.toXMLnoNS(TestXMLUtils.getTestIdElement(bbTest.manager.getDoc(), "testroot")),
//				"<level1 bbtestroot=\"true\" testid=\"testroot\">"
//				+ "<span utd:frontmatter-type=\"VOLUME\">"
//				+ "<span utd:frontmatter-type=\"VOLUME_END\" utd:frontmatter-sort=\"10\" utd:firstVolume=\"true\">First Volume Placeholder</span>"
//				+ "</span>"
//				+ "<p>Paragraph 1</p>"
//				+ "<span utd:frontmatter-type=\"VOLUME_PRELIMINARY\">"
//				+ "<span utd:frontmatter-type=\"VOLUME_END\" utd:frontmatter-sort=\"10\">END OF VOLUME 1</span>"
//				+ "</span>"
//				+ "<p>Paragraph 2</p>"
//				+ "<p>Paragraph 3</p>"
//				+ "</level1>");
//
//		assertEquals(bbTest.bot.labelWithId("pageTotal").getText(), "of 2");
//
//		bbTest.navigateTextViewToText("aragraph 1");
//		doPendingSWTWork();
//		assertEquals(bbTest.bot.textWithId("pageCurrent").getText(), "1");
//		assertEquals(bbTest.bot.labelWithId("volumeInfo").getText(), "Volume 1");
//
//		bbTest.navigateTextViewToText("aragraph 3");
//		doPendingSWTWork();
//		assertEquals(bbTest.bot.textWithId("pageCurrent").getText(), "2");
//		assertEquals(bbTest.bot.labelWithId("volumeInfo").getText(), "Preliminary 1");
//
//		bbTest.navigateTextViewToText("Paragraph 3");
//		bbTest.bot.menu("Insert Volume before Cursor").click();
//		doPendingSWTWork();
//		volumes = EasierFrontmatter.getVolumeElementsFatal(bbTest.manager.getDoc());
//		assertEquals(volumes.size(), 3);
//		assertEquals(TestXMLUtils.toXMLnoNS(TestXMLUtils.getTestIdElement(bbTest.manager.getDoc(), "testroot")),
//				"<level1 bbtestroot=\"true\" testid=\"testroot\">"
//				+ "<span utd:frontmatter-type=\"VOLUME\">"
//				+ "<span utd:frontmatter-type=\"VOLUME_END\" utd:frontmatter-sort=\"10\" utd:firstVolume=\"true\">First Volume Placeholder</span>"
//				+ "</span>"
//				+ "<p>Paragraph 1</p>"
//				+ "<span utd:frontmatter-type=\"VOLUME_PRELIMINARY\">"
//				+ "<span utd:frontmatter-type=\"VOLUME_END\" utd:frontmatter-sort=\"10\">END OF VOLUME 1</span>"
//				+ "</span>"
//				+ "<p>Paragraph 2</p>"
//				+ "<span utd:frontmatter-type=\"VOLUME\">"
//				+ "<span utd:frontmatter-type=\"VOLUME_END\" utd:frontmatter-sort=\"10\">END OF PRELIMINARY VOLUME 1</span>"
//				+ "</span>"
//				+ "<p>Paragraph 3</p>"
//				+ "</level1>");
//
//		bbTest.navigateTextViewToText("aragraph 3");
//		doPendingSWTWork();
//		assertEquals(bbTest.bot.textWithId("pageCurrent").getText(), "3");
//		assertEquals(bbTest.bot.labelWithId("volumeInfo").getText(), "Volume 2");
//	}
//	@DataProvider
//	public Iterator<Object[]> slowInsertVolumeEverywhereTestDataProvider() {
//		List<Object[]> params = new ArrayList<>();
//
//		WPManager startBB = startBB(TOC_RAW_FILE);
//		Manager m = getManager(startBB);
//		for (int i = 0; i < m.getTextView().getCharCount(); i++) {
//			params.add(new Object[]{i});
//		}
//		log.debug("testSize " + params.size());
//
//		return params.iterator();
//	}
//
//	//WARNING: THIS IS EXTREMELY SLOW, CAN RUN OVER 1 HOUR
//	@Test(dataProvider = "slowInsertVolumeEverywhereTestDataProvider", enabled = false)
//	public void slowInsertVolumeEverywhereTest(int offset) {
//		WPManager startBB = startBB(TOC_RAW_FILE);
//		Manager manager = getManager(startBB);
//
//		textViewBot.setFocus();
//		doPendingSWTWork();
//		StyledText textView = manager.getTextView();
//
//		textViewBot.navigateTo(0, 0);
//		for (int i = 0; i < offset; i++) {
//			textViewBot.pressShortcut(KeyStroke.getInstance(SWT.ARROW_RIGHT));
//		}
//		doPendingSWTWork();
//
//		bot.menu("Insert Volume before Cursor").click();
//		doPendingSWTWork();
//	}
}

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
package org.brailleblaster.archiver;

import nu.xom.Element;
import nu.xom.Nodes;
import org.brailleblaster.archiver2.*;
import org.brailleblaster.bbx.BBX;
import org.brailleblaster.testrunners.TestXMLUtils;
import org.brailleblaster.utd.UTDTranslationEngine;
import org.brailleblaster.utd.exceptions.NodeException;
import org.brailleblaster.exceptions.BBNotifyException;
import org.hamcrest.MatcherAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.instanceOf;

@SuppressWarnings("deprecation")
public class ArchiverManagerTest {
	private static final Logger log = LoggerFactory.getLogger(ArchiverManagerTest.class);
	private final ArchiverFactory archiverManager = ArchiverFactory.INSTANCE;

	@Test(enabled = false)
	public void nimasFileTest() throws IOException {
		Archiver2 out = archiverManager.load(Paths.get("src/test/resources/fdr/fdr.xml"));
		MatcherAssert.assertThat(out, instanceOf(BBXArchiver.class));
		assertValidNimas(out, true);
	}
	
	private static Path createTempZip() throws IOException {
		return createTempZip("bb-test-tmp-");
	}
	
	private static Path createTempZip(String prefix) throws IOException {
		Path tmpZipFile = Files.createTempFile(prefix, "nimas.zip");
		Files.delete(tmpZipFile);
		FileSystem zipFS = ZipHandles.open(tmpZipFile, true);
		//Use existing FDR opf test files
		Path zipRoot = zipFS.getPath("/");
		Files.copy(
				Paths.get("src/test/resources/fdr/FDR Inagural Address.opf"),
				zipRoot.resolve("FDR Inagural Address.opf")
		);
		Files.copy(
				Paths.get("src/test/resources/fdr/fdr.xml"),
				zipRoot.resolve("FDR Inagural Address.xml")
		);
		ZipHandles.close(tmpZipFile);

		return tmpZipFile;
	}

	@Test
	public void nimasZipWithOPFTest() throws IOException {
		Archiver2 out = archiverManager.load(createTempZip());
		MatcherAssert.assertThat(out, instanceOf(BBZArchiver.class));
		assertValidNimas(out, true);
		out.close();
	}
	
	@Test(enabled = false)
	public void nimasZipNoOPFTest() throws IOException {
		Path tmpZipFile = Files.createTempFile("bb-test", "nimas.zip");
		Files.delete(tmpZipFile);
		FileSystem zipFS = ZipHandles.open(tmpZipFile, true);
		Path zipRoot = zipFS.getPath("/");
		Files.copy(
				Paths.get("src/test/resources/fdr/fdr.xml"),
				zipRoot.resolve("FDR Inagural Address.xml")
		);
		ZipHandles.close(tmpZipFile);

		Archiver2 out = archiverManager.load(tmpZipFile);
		MatcherAssert.assertThat(out, instanceOf(BBZArchiver.class));
		assertValidNimas(out, false);
		out.close();
	}

	private void assertValidNimas(Archiver2 arch, boolean testOPF) throws IOException {
		Path path = Paths.get("FDR Inagural Address.opf");
		{
			Nodes query = arch.getBbxDocument().query("descendant::bb:BLOCK", BBX.XPATH_CONTEXT);
			try {
				Assert.assertEquals(query.size(), 26);
				Assert.assertEquals(query.get(0).getValue(), "FDR's Inaugural Address, 1933");
			} catch (Error e) {
				throw new NodeException("Not found", arch.getBbxDocument(), e);
			}

			if (arch instanceof BBZArchiver && testOPF) {
				Path pathFromArchiver = arch.resolveSibling(path);
				Assert.assertTrue(Files.exists(pathFromArchiver), "File does not exist: " + pathFromArchiver.toUri());
			}

			((Element) query.get(0)).insertChild("test", 0);
		}

		Path tmpSaveFile;
		{
			log.warn("Save 1");
			Path pathBeforeSave = arch.getPath();
			tmpSaveFile = Files.createTempFile("bb-test", "save");
			Files.delete(tmpSaveFile);
			arch.save(tmpSaveFile, arch.getBbxDocument(), new UTDTranslationEngine(), Set.of());
			Assert.assertEquals(arch.getPath(), pathBeforeSave);
			
			if (arch instanceof BBZArchiver && testOPF) {
				Path opfPath = arch.resolveSibling(path);
				Assert.assertTrue(Files.exists(opfPath), "Cannot find " + opfPath.toUri());
			}
		}

		{
			log.warn("Save 2");

			Archiver2 saveArch = archiverManager.load(tmpSaveFile);
			Assert.assertEquals(
					saveArch.getBbxDocument().query("descendant::bb:BLOCK", BBX.XPATH_CONTEXT).get(0).getValue(),
					"testFDR's Inaugural Address, 1933"
			);

			//Test re-savability of a fresh BBZArchiver
			Path tmpSaveReAsFile = Files.createTempFile("bb-test", "saveReAs");
			Files.delete(tmpSaveReAsFile);
			saveArch.saveAs(tmpSaveReAsFile, saveArch.getBbxDocument(), new UTDTranslationEngine(), Set.of());
			Assert.assertEquals(saveArch.getPath(), tmpSaveReAsFile);
			
			if (saveArch instanceof BBZArchiver && testOPF) {
				Path opfPath = saveArch.resolveSibling(path);
				Assert.assertTrue(Files.exists(opfPath), "Cannot find " + opfPath.toUri());
			}
			saveArch.close();
		}

		{
			//Test saveAs of origional arch
			log.warn("Save 3");
			Path tmpSaveAsFile = Files.createTempFile("bb-test", "saveAs");
			Files.delete(tmpSaveAsFile);
			arch.saveAs(tmpSaveAsFile, arch.getBbxDocument(), new UTDTranslationEngine(), Set.of());
			Assert.assertEquals(arch.getPath(), tmpSaveAsFile);

			if (arch instanceof BBZArchiver && testOPF) {
				Path opfPath = arch.resolveSibling(path);
				Assert.assertTrue(Files.exists(opfPath), "Cannot find " + opfPath.toUri());
			}
		}
	}
	
	@Test(enabled = false)
	public void open_close_sameDoc() throws IOException {
		Path path = Paths.get("src/test/resources/fdr/fdr.xml");
		Archiver2 out1 = archiverManager.load(path);
		try {
			out1.close();
		} catch (IOException e) {
			log.error("First archiver instance failed to close", e);
		}
		
		Archiver2 out2 = archiverManager.load(path);
		try {
			out2.close();
		} catch (IOException e) {
			log.error("Second archiver instance failed to close", e);
		}
		out1.close();
	}
	
	@Test(enabled = false)
	public void open_multipleInstances_sameDoc() throws IOException {
		Path path = Paths.get("src/test/resources/fdr/fdr.xml");
		Archiver2 out1 = archiverManager.load(path);
		Archiver2 out2 = archiverManager.load(path);
		
		try {
			out1.close();
		} catch (IOException e) {
//			log.error("First archiver instance failed to close", e);
			throw new AssertionError("First archiver instance failed to close", e);
		}
		
		try {
			out2.close();
		} catch (IOException e) {
//			log.error("Second archiver instance failed to close", e);
			throw new AssertionError("Second archiver instance failed to close", e);
		}
		
		out1.close();
		out2.close();
	}
	
	@Test(enabled = false)
	public void text_asciiControlCharacters_issue6128() throws Exception {
		Path tmpFile = Files.createTempFile("bbTest", "text_asciiControlCharacters");
		Files.write(tmpFile, List.of("this\u001a is\u0007 test"));
		
		try (Archiver2 archive = archiverManager.load(tmpFile)) {
			TestXMLUtils.assertRootSection(archive.getBbxDocument())
					.nextChildIs(p -> p
							.hasText("this is test")
					).noNextChild();
		}
	}
	
	@Test(enabled =false)
	public void saveZipAgain_issue6359() throws IOException {
		try (Archiver2 archive = archiverManager.load(createTempZip())) {
			archive.save(archive.getPath(), archive.getBbxDocument(), new UTDTranslationEngine(), Set.of());
			archive.save(archive.getPath(), archive.getBbxDocument(), new UTDTranslationEngine(), Set.of());
		}
	}
	
	@Test(enabled = false)
	public void saveAsZipAgain_newFile_issue6359() throws IOException {
		try (Archiver2 archive = archiverManager.load(createTempZip())) {
			Path path = Files.createTempFile("archiverManager", ".bbz");
			archive.saveAs(path, archive.getBbxDocument(), new UTDTranslationEngine(), Set.of());
			archive.saveAs(path, archive.getBbxDocument(), new UTDTranslationEngine(), Set.of());
		}
	}
	
	@Test(enabled = false)
	public void saveAsZipAgain_newFile2_issue6359() throws IOException {
		Path origZipFile = createTempZip();
		try (Archiver2 archive = archiverManager.load(origZipFile)) {
			Path newZipArchive = Files.createTempFile("archiverManager", ".bbz");
			log.error("--------------------- SAVE AS #1 --");
			archive.saveAs(newZipArchive, archive.getBbxDocument(), new UTDTranslationEngine(), Set.of());
			log.error("--------------------- SAVE #2 --");
			archive.save(origZipFile, archive.getBbxDocument(), new UTDTranslationEngine(), Set.of());
			log.error("--------------------- SAVE AS #3 --");
			archive.saveAs(newZipArchive, archive.getBbxDocument(), new UTDTranslationEngine(), Set.of());
		}
	}
	
	@Test(enabled = false)
	public void saveAsZipAgain_newFil3_issue6359() throws IOException {
		Path origZipFile = createTempZip();
		try (Archiver2 archive = archiverManager.load(origZipFile)) {
			Path newZipArchive = Files.createTempFile("archiverManager", ".bbz");
			log.error("--------------------- SAVE AS #1 --");
			archive.save(origZipFile, archive.getBbxDocument(), new UTDTranslationEngine(), Set.of());
			log.error("--------------------- SAVE #2 --");
			archive.saveAs(newZipArchive, archive.getBbxDocument(), new UTDTranslationEngine(), Set.of());
			log.error("--------------------- SAVE AS #3 --");
			archive.save(origZipFile, archive.getBbxDocument(), new UTDTranslationEngine(), Set.of());
		}
	}
	
	@Test(enabled = false)
	public void multipleFiles() throws Exception {
		Path zip = createTempZip();
		Archiver2 archive = archiverManager.load(zip);
		Archiver2 archive2 = archiverManager.load(zip);
		Path path = Files.createTempFile("archiverManager", ".bbz");
		archive.saveAs(path, archive.getBbxDocument(), new UTDTranslationEngine(), Set.of());
		// should fail, can't have 2 tabs overwriting each other
		try {
			archive2.saveAs(path, archive.getBbxDocument(), new UTDTranslationEngine(), Set.of());
		} catch (BBNotifyException e) {
			log.error("on expected error", e);
			archive.close();
			archive2.close();
		}
	}
}

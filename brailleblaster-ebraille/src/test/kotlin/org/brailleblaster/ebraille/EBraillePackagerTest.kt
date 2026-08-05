/*
 * Copyright (C) 2026 American Printing House for the Blind
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
package org.brailleblaster.ebraille

import nu.xom.Builder
import nu.xom.Document
import nu.xom.Element
import org.brailleblaster.utd.BrailleSettings
import org.brailleblaster.utd.ITranslationEngine
import org.brailleblaster.utils.xml.OPF_NS
import org.mwhapples.jlouis.Louis
import org.mwhapples.jlouis.TranslationResult
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.jsoup.Jsoup
import org.testng.Assert
import org.testng.annotations.Test
import java.nio.file.Files
import java.util.zip.ZipFile

class EBraillePackagerTest {
    @Test
    fun exportProducesValidPackageAndSpecMetadata() {
        val outFile = Files.createTempFile("ebraille-phase2-", ".ebrl")
        try {
            val doc = Jsoup.parse("<html><body><h1>Chapter 1</h1><span role=\"doc-pagebreak\">1</span><p>Content</p></body></html>")

            EBraillePackager.createEbraillePackage(
                outFile,
                listOf(doc),
                title = "Sample Title",
                translationEngine = mockTranslationEngine()
            )

            ZipFile(outFile.toFile()).use { zip ->
                Assert.assertNotNull(zip.getEntry("mimetype"))
                Assert.assertNotNull(zip.getEntry("package.opf"))
                Assert.assertNotNull(zip.getEntry("index.html"))
                Assert.assertNotNull(zip.getEntry("ebraille/document0.html"))
                Assert.assertNotNull(zip.getEntry("META-INF/container.xml"))

                val opfDoc = readXml(zip, "package.opf")
                val metadata = firstChild(opfDoc.rootElement, "metadata", OPF_NS)

                Assert.assertEquals(firstDcValue(metadata, "title"), "Sample Title")
                Assert.assertEquals(firstDcValue(metadata, "format"), "eBraille 1.0")
                Assert.assertEquals(firstMetaProperty(metadata, "a11y:tactileGraphics"), "none")
                Assert.assertEquals(firstMetaProperty(metadata, "a11y:brailleCellType"), "6")
                Assert.assertNull(firstMetaProperty(metadata, "a11y:cellType"))
                Assert.assertNull(firstMetaProperty(metadata, "a11y:dateTranscribed"))

                val manifest = firstChild(opfDoc.rootElement, "manifest", OPF_NS)
                val spine = firstChild(opfDoc.rootElement, "spine", OPF_NS)
                Assert.assertTrue(manifest.childElements.size() >= 3)

                val navItem = findManifestByHref(manifest, "index.html")
                Assert.assertNotNull(navItem)
                Assert.assertEquals(navItem!!.getAttributeValue("properties"), "nav")

                val spineIds = mutableSetOf<String>()
                for (i in 0 until spine.childElements.size()) {
                    val idRef = spine.childElements[i].getAttributeValue("idref")
                    if (idRef != null) {
                        spineIds.add(idRef)
                    }
                }

                Assert.assertTrue(spineIds.contains("file0"))
                Assert.assertTrue(spineIds.contains("file2"))
                Assert.assertFalse(spineIds.contains("file1"))

                val indexHtml = zip.getInputStream(zip.getEntry("index.html")).bufferedReader(Charsets.UTF_8).use { it.readText() }
                Assert.assertTrue(indexHtml.contains("<title>Sample Title</title>"))
                Assert.assertTrue(indexHtml.contains("href=\"package.opf\""))
            }
        } finally {
            Files.deleteIfExists(outFile)
        }
    }

    @Test
    fun exportUsesProvidedStableManifestButRecalculatesVolatileFields() {
        val outFile = Files.createTempFile("ebraille-phase2-manifest-", ".ebrl")
        try {
            val customManifest = EBrailleManifest.defaults().copy(
                title = "Provided Manifest Title",
                tactileGraphics = "png"
            )
            val doc = Jsoup.parse("<html><body><h1>Heading</h1><p>Body</p></body></html>")

            EBraillePackager.createEbraillePackage(
                outFile,
                listOf(doc),
                title = "Ignored Export Title",
                translationEngine = mockTranslationEngine(),
                manifest = customManifest
            )

            ZipFile(outFile.toFile()).use { zip ->
                val opfDoc = readXml(zip, "package.opf")
                val metadata = firstChild(opfDoc.rootElement, "metadata", OPF_NS)

                Assert.assertEquals(firstDcValue(metadata, "title"), "Provided Manifest Title")
                Assert.assertEquals(firstMetaProperty(metadata, "a11y:tactileGraphics"), "none")
            }
        } finally {
            Files.deleteIfExists(outFile)
        }
    }

    private fun readXml(zip: ZipFile, path: String): Document = zip.getInputStream(zip.getEntry(path)).use { Builder().build(it) }

    private fun firstChild(parent: Element, localName: String, namespace: String): Element {
        val child = parent.getFirstChildElement(localName, namespace)
        Assert.assertNotNull(child, "Expected child element '$localName' in namespace '$namespace'")
        return child
    }

    private fun firstDcValue(metadata: Element, localName: String): String? {
        for (i in 0 until metadata.childElements.size()) {
            val child = metadata.childElements[i]
            if (child.localName == localName && child.namespaceURI == "http://purl.org/dc/elements/1.1/") {
                return child.value
            }
        }
        return null
    }

    private fun firstMetaProperty(metadata: Element, property: String): String? {
        for (i in 0 until metadata.childElements.size()) {
            val child = metadata.childElements[i]
            if (child.localName == "meta" && child.getAttributeValue("property") == property) {
                return child.value
            }
        }
        return null
    }

    private fun findManifestByHref(manifest: Element, href: String): Element? {
        for (i in 0 until manifest.childElements.size()) {
            val child = manifest.childElements[i]
            if (child.localName == "item" && child.getAttributeValue("href") == href) {
                return child
            }
        }
        return null
    }

    private fun mockTranslationEngine(): ITranslationEngine {
        val translationResult = Mockito.mock(TranslationResult::class.java)
        Mockito.`when`(translationResult.translation).thenReturn("x")
        Mockito.`when`(translationResult.inputPos).thenReturn(intArrayOf(0))

        val louis = Mockito.mock(Louis::class.java)
        Mockito.`when`(louis.translate(anyString(), anyString(), any(), anyInt(), anyInt())).thenReturn(translationResult)

        val settings = BrailleSettings().apply {
            isUseAsciiBraille = true
            mainTranslationTable = "en-us-g2.ctb"
        }

        val engine = Mockito.mock(ITranslationEngine::class.java)
        Mockito.`when`(engine.brailleTranslator).thenReturn(louis)
        Mockito.`when`(engine.brailleSettings).thenReturn(settings)
        return engine
    }
}
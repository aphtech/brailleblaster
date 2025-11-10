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
package org.brailleblaster.utd.utils

import nu.xom.Attribute
import nu.xom.Document
import nu.xom.Element
import org.assertj.core.api.Assertions
import org.brailleblaster.utd.internal.elements.*
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utils.xml.BB_NS
import org.brailleblaster.utils.xml.DC_NS
import org.brailleblaster.utils.xml.PEFNamespaceContext
import org.testng.Assert
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.math.BigDecimal
import java.util.function.IntPredicate
import javax.xml.xpath.*

class BBX2PEFConverterTest {
    private var findVolumes: XPathExpression? = null
    private var findRelativePages: XPathExpression? = null
    private var findRelativeSections: XPathExpression? = null
    private var findTitle: XPathExpression? = null
    private var findSubjects: XPathExpression? = null
    private var xpath: XPath? = null
    private var findRelativeRows: XPathExpression? = null
    private var findIdentifier: XPathExpression? = null

    @BeforeClass
    @Throws(XPathExpressionException::class)
    fun createXPathExpressions() {
        xpath = XPathFactory.newInstance().newXPath()
        xpath!!.namespaceContext = PEFNamespaceContext()
        findVolumes = xpath!!.compile("/pef:pef/pef:body/pef:volume")
        findRelativeSections = xpath!!.compile("./pef:section")
        findRelativePages = xpath!!.compile("./pef:page")
        findRelativeRows = xpath!!.compile(".//pef:row")
        findIdentifier = xpath!!.compile("/pef:pef/pef:head/pef:meta/dc:identifier")
        findTitle = xpath!!.compile("/pef:pef/pef:head/pef:meta/dc:title")
        findSubjects = xpath!!.compile("/pef:pef/pef:head/pef:meta/dc:subject")
    }

    @Throws(XPathExpressionException::class)
    fun assertPageEquals(actual: org.w3c.dom.Node?, expectedLines: Array<String>) {
        val rows = findRelativeRows!!.evaluate(actual, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(rows.length, expectedLines.size, "Incorrect number of lines")
        for (i in expectedLines.indices) {
            Assert.assertEquals(
                rows.item(i).textContent,
                expectedLines[i],
                String.format("Row %d does not match", i)
            )
        }
    }

    @Test
    @Throws(XPathExpressionException::class)
    fun testSetPageSize() {
        val c = BBX2PEFConverter()
        Assert.assertEquals(c.rows, 25, "Number of initial rows is incorrect")
        Assert.assertEquals(c.cols, 40, "Number of initial columns is not correct")
        val rowArray = intArrayOf(25, 20, 35, 10)
        val colArray = intArrayOf(40, 30, 35, 40)
        for (i in rowArray.indices) {
            c.setPageSize(rowArray[i], colArray[i])
            Assert.assertEquals(c.rows, rowArray[i], "Number of rows incorrect")
            Assert.assertEquals(c.cols, colArray[i], "Number of columns incorrect")
            c.onStartDocument(EMPTY_DOC)
            c.onEndDocument(EMPTY_DOC)
            val pef = c.pefDoc
            val vols = findVolumes!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
            Assert.assertTrue(vols.length > 0, "Resulting doc has no volumes")
            for (volIndex in 0..<vols.length) {
                Assert.assertTrue(
                    (xpath!!.evaluate(
                        String.format(
                            "count(self::node()[@cols='%d' and @rows='%d']) = 1",
                            colArray[i], rowArray[i]
                        ),
                        vols.item(volIndex), XPathConstants.BOOLEAN
                    ) as Boolean?)!!,
                    "Volume does not have correct number of cols and rows"
                )
            }
        }
    }

    @Test
    @Throws(XPathExpressionException::class)
    fun createMinimalDocument() {
        val c = BBX2PEFConverter()
        c.onStartDocument(EMPTY_DOC)
        c.onEndDocument(EMPTY_DOC)
        val checkVolAttributes =
            xpath!!.compile("count(self::node()[@cols='40' and @duplex='false' and @rowgap='0' and @rows='25']) = 1")
        val pef = c.pefDoc
        val vols = findVolumes!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertTrue(vols.length > 0, "Minimal document requires 1 volume")
        for (volIndex in 0..<vols.length) {
            val vol = vols.item(volIndex)
            Assert.assertTrue(
                (checkVolAttributes.evaluate(vol, XPathConstants.BOOLEAN) as Boolean?)!!,
                "Volume must have cols, duplex, rowgap and rows attributes"
            )
            val sections = findRelativeSections!!.evaluate(vol, XPathConstants.NODESET) as org.w3c.dom.NodeList
            Assert.assertTrue(sections.length > 0, "Volume must contain at least 1 section")
            for (sectionIndex in 0..<sections.length) {
                val section = sections.item(sectionIndex)
                val pages = findRelativePages!!.evaluate(section, XPathConstants.NODESET) as org.w3c.dom.NodeList
                Assert.assertTrue(pages.length > 0, "Section must have at least 1 page")
            }
        }
    }

    @Test
    @Throws(XPathExpressionException::class)
    fun testCreateBasicDocument() {
        // The test brl
        val brl = Brl()
        brl.appendChild(UTDElements.NEW_PAGE.create())
        brl.appendChild("⠠\u281e\u2811\u280c\u2800\u281e\u2811\u282d\u281e")

        val c = BBX2PEFConverter(rows = 25, cols = 40)
        c.onStartDocument(EMPTY_DOC)
        Assert.assertFalse(c.onStartElement(brl))
        c.onEndElement(brl)
        c.onEndDocument(EMPTY_DOC)
        val pef = c.pefDoc
        val identifiers = findIdentifier!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(identifiers.length, 1, "There should only be 1 identifier")
        Assert.assertEquals(identifiers.item(0).textContent, "TempID")
        val vols = findVolumes!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(vols.length, 1, "Incorrect volume count")
        val vol = vols.item(0) as org.w3c.dom.Element
        Assert.assertEquals(vol.getAttribute("rows"), "25", "Incorrect rows per page")
        Assert.assertEquals(vol.getAttribute("cols"), "40", "Incorrect columns per page")
        val sections = findRelativeSections!!.evaluate(vol, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(sections.length, 1, "Incorrect section count")
        val pages = findRelativePages!!.evaluate(sections.item(0), XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(pages.length, 1, "Incorrect page count")
        val p = pages.item(0)
        val expectedLines: Array<String> = arrayOf("\u2820\u281e\u2811\u280c\u2800\u281e\u2811\u282d\u281e")
        assertPageEquals(p, expectedLines)
    }

    @Test
    @Throws(XPathExpressionException::class)
    fun testBasicDocumentWithDefaultIdentifier() {
        // The test brl
        val brl = Brl()
        brl.appendChild(UTDElements.NEW_PAGE.create())
        brl.appendChild("\u2820\u281e\u2811\u280c\u2800\u281e\u2811\u282d\u281e")

        val c = BBX2PEFConverter(rows = 25, cols = 40, defaultIdentifier = "TestDoc0001")
        c.onStartDocument(EMPTY_DOC)
        Assert.assertFalse(c.onStartElement(brl))
        c.onEndElement(brl)
        c.onEndDocument(EMPTY_DOC)
        val pef = c.pefDoc
        val identifiers = findIdentifier!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(identifiers.length, 1, "There should only be 1 identifier")
        Assert.assertEquals(identifiers.item(0).textContent, "TestDoc0001")
        val vols = findVolumes!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(vols.length, 1, "Incorrect volume count")
        val vol = vols.item(0) as org.w3c.dom.Element
        Assert.assertEquals(vol.getAttribute("rows"), "25", "Incorrect rows per page")
        Assert.assertEquals(vol.getAttribute("cols"), "40", "Incorrect columns per page")
        val sections = findRelativeSections!!.evaluate(vol, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(sections.length, 1, "Incorrect section count")
        val pages = findRelativePages!!.evaluate(sections.item(0), XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(pages.length, 1, "Incorrect page count")
        val page = pages.item(0)
        val expectedLines: Array<String> = arrayOf("\u2820\u281e\u2811\u280c\u2800\u281e\u2811\u282d\u281e")
        assertPageEquals(page, expectedLines)
    }

    @Test
    @Throws(XPathExpressionException::class)
    fun basicDocumentMetaFromDC() {
        // The test DC elements
        var metaElement: Element?
        val headElement = Element("head", BB_NS)
        val identifierStr = "Example001"
        metaElement = Element("dc:identifier", DC_NS)
        metaElement.appendChild(identifierStr)
        headElement.appendChild(metaElement)
        val titleStr = "An example document"
        metaElement = Element("dc:title", DC_NS)
        metaElement.appendChild(titleStr)
        headElement.appendChild(metaElement)
        val subjectsList: List<String> = listOf("Braille", "Transcription")
        for (subject in subjectsList) {
            metaElement = Element("dc:subject", DC_NS)
            metaElement.appendChild(subject)
            headElement.appendChild(metaElement)
        }

        // The test brl
        val brl = Brl()
        brl.appendChild(UTDElements.NEW_PAGE.create())
        brl.appendChild("\u2820\u281e\u2811\u280c\u2800\u281e\u2811\u282d\u281e")

        val c = BBX2PEFConverter(rows = 25, cols = 40, defaultIdentifier = "TestDoc0001")
        c.onStartDocument(EMPTY_DOC)
        Assert.assertFalse(c.onStartElement(headElement))
        c.onEndElement(headElement)
        Assert.assertFalse(c.onStartElement(brl))
        c.onEndElement(brl)
        c.onEndDocument(EMPTY_DOC)
        val pef = c.pefDoc
        var nodes = findIdentifier!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(nodes.length, 1, "There can only be one identifier")
        Assert.assertEquals(nodes.item(0).textContent, identifierStr)
        nodes = findTitle!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(nodes.length, 1, "There should only be 1 title")
        Assert.assertEquals(nodes.item(0).textContent, titleStr)
        nodes = findSubjects!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
        val actualSubjects: MutableList<String?> = ArrayList(nodes.length)
        for (i in 0..<nodes.length) {
            actualSubjects.add(nodes.item(i).textContent)
        }
        Assertions.assertThat<String?>(actualSubjects).containsExactlyInAnyOrderElementsOf(subjectsList)
        val vols = findVolumes!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(vols.length, 1, "Incorrect volume count")
        val vol = vols.item(0) as org.w3c.dom.Element
        Assert.assertEquals(vol.getAttribute("rows"), "25", "Incorrect rows per page")
        Assert.assertEquals(vol.getAttribute("cols"), "40", "Incorrect columns per page")
        val sections = findRelativeSections!!.evaluate(vol, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(sections.length, 1, "Incorrect section count")
        val pages = findRelativePages!!.evaluate(sections.item(0), XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(pages.length, 1, "Incorrect page count")
        val p = pages.item(0)
        val expectedLines: Array<String> = arrayOf("\u2820\u281e\u2811\u280c\u2800\u281e\u2811\u282d\u281e")
        assertPageEquals(p, expectedLines)
    }

    @Test
    @Throws(XPathExpressionException::class)
    fun basicDocumentMetaFromMeta() {
        // The test DC elements
        var metaElement: Element?
        val headElement = Element("head", BB_NS)
        val identifierStr = "Example001"
        metaElement = Element("meta")
        metaElement.addAttribute(Attribute("name", "dc:identifier"))
        metaElement.appendChild(identifierStr)
        headElement.appendChild(metaElement)
        val titleStr = "An example document"
        metaElement = Element("meta")
        metaElement.addAttribute(Attribute("name", "dc:title"))
        metaElement.appendChild(titleStr)
        headElement.appendChild(metaElement)
        val subjectsList: List<String> = listOf("Braille", "Transcription")
        for (subject in subjectsList) {
            metaElement = Element("meta")
            metaElement.addAttribute(Attribute("name", "dc:subject"))
            metaElement.appendChild(subject)
            headElement.appendChild(metaElement)
        }

        // The test brl
        val brl = Brl()
        brl.appendChild(UTDElements.NEW_PAGE.create())
        brl.appendChild("\u2820\u281e\u2811\u280c\u2800\u281e\u2811\u282d\u281e")

        val c = BBX2PEFConverter(rows = 25, cols = 40, defaultIdentifier = "TestDoc0001")
        c.onStartDocument(EMPTY_DOC)
        Assert.assertFalse(c.onStartElement(headElement))
        c.onEndElement(headElement)
        Assert.assertFalse(c.onStartElement(brl))
        c.onEndElement(brl)
        c.onEndDocument(EMPTY_DOC)
        val pef = c.pefDoc
        var nodes = findIdentifier!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(nodes.length, 1, "There should only be 1 identifier")
        Assert.assertEquals(nodes.item(0).textContent, identifierStr)
        nodes = findTitle!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(nodes.length, 1, "There should only be 1 title")
        Assert.assertEquals(nodes.item(0).textContent, titleStr)
        nodes = findSubjects!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
        val actualSubjects: MutableList<String?> = ArrayList(nodes.length)
        for (i in 0..<nodes.length) {
            actualSubjects.add(nodes.item(i).textContent)
        }
        Assertions.assertThat<String?>(actualSubjects).containsExactlyInAnyOrderElementsOf(subjectsList)
        val vols = findVolumes!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(vols.length, 1, "Incorrect volume count")
        val vol = vols.item(0) as org.w3c.dom.Element
        Assert.assertEquals(vol.getAttribute("rows"), "25", "Incorrect rows per page")
        Assert.assertEquals(vol.getAttribute("cols"), "40", "Incorrect columns per page")
        val sections = findRelativeSections!!.evaluate(vol, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(sections.length, 1, "Incorrect section count")
        val pages = findRelativePages!!.evaluate(sections.item(0), XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(pages.length, 1, "Incorrect page count")
        val p = pages.item(0)
        val expectedLines: Array<String> = arrayOf("\u2820\u281e\u2811\u280c\u2800\u281e\u2811\u282d\u281e")
        assertPageEquals(p, expectedLines)
    }

    @Test
    @Throws(XPathExpressionException::class)
    fun testInsertAsciiBraille() {
        // The test brl
        val brl = Brl()
        brl.appendChild(UTDElements.NEW_PAGE.create())
        // Use ASCII Braille in brl, BBX2PEF will convert it.
        brl.appendChild(",te/ text")

        val c = BBX2PEFConverter(rows = 25, cols = 40)
        c.onStartDocument(EMPTY_DOC)
        Assert.assertFalse(c.onStartElement(brl))
        c.onEndElement(brl)
        c.onEndDocument(EMPTY_DOC)
        val pef = c.pefDoc
        val vols = findVolumes!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(vols.length, 1, "Incorrect volume count")
        val vol = vols.item(0) as org.w3c.dom.Element
        Assert.assertEquals(vol.getAttribute("rows"), "25", "Incorrect rows per page")
        Assert.assertEquals(vol.getAttribute("cols"), "40", "Incorrect columns per page")
        Assert.assertFalse(vol.getAttribute("duplex").toBoolean())
        val sections = findRelativeSections!!.evaluate(vol, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(sections.length, 1, "Incorrect section count")
        val pages = sections.item(0) as org.w3c.dom.NodeList
        Assert.assertEquals(pages.length, 1, "Incorrect page count")
        val p = pages.item(0)
        val expectedLines: Array<String> = arrayOf("\u2820\u281e\u2811\u280c\u2800\u281e\u2811\u282d\u281e")
        assertPageEquals(p, expectedLines)
    }

    @Test
    @Throws(XPathExpressionException::class)
    fun testDuplexMode() {
        // The test brl
        val brl = Brl()
        brl.appendChild(UTDElements.NEW_PAGE.create())
        // Use ASCII Braille in brl, BBX2PEF will convert it.
        brl.appendChild(",te/ text")

        val c = BBX2PEFConverter(rows = 25, cols = 40, isDuplex = true)
        c.onStartDocument(EMPTY_DOC)
        Assert.assertFalse(c.onStartElement(brl))
        c.onEndElement(brl)
        c.onEndDocument(EMPTY_DOC)
        val pef = c.pefDoc
        val vols = findVolumes!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(vols.length, 1, "Incorrect volume count")
        val vol = vols.item(0) as org.w3c.dom.Element
        Assert.assertEquals(vol.getAttribute("rows"), "25", "Incorrect rows per page")
        Assert.assertEquals(vol.getAttribute("cols"), "40", "Incorrect columns per page")
        Assert.assertTrue(vol.getAttribute("duplex").toBoolean(), "Duplex attribute is incorrect")
        val sections = findRelativeSections!!.evaluate(vol, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(sections.length, 1, "Incorrect section count")
        val pages = findRelativePages!!.evaluate(sections.item(0), XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(pages.length, 1, "Incorrect page count")
        val p = pages.item(0)
        val expectedLines: Array<String> = arrayOf("\u2820\u281e\u2811\u280c\u2800\u281e\u2811\u282d\u281e")
        assertPageEquals(p, expectedLines)
    }

    @Test
    @Throws(XPathExpressionException::class)
    fun testBasicDocWithMultipleLines() {
        // The test brl
        val brl = Brl()
        brl.appendChild(UTDElements.NEW_PAGE.create())
        brl.appendChild("\u2820\u2811\u280c\u2800\u281e\u2811\u282d")
        brl.appendChild(MoveTo(BigDecimal("0.0"), BigDecimal("10.0")))
        brl.appendChild("\u280e\u2811\u2809\u2815\u281d\u2819\u2800\u2807\u2814\u2811")

        val c = BBX2PEFConverter(rows = 25, cols = 40)
        c.onStartDocument(EMPTY_DOC)
        Assert.assertFalse(c.onStartElement(brl))
        c.onEndElement(brl)
        c.onEndDocument(EMPTY_DOC)
        val pef = c.pefDoc
        val vols = findVolumes!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(vols.length, 1, "Incorrect volume count")
        val vol = vols.item(0) as org.w3c.dom.Element
        Assert.assertEquals(vol.getAttribute("rows"), "25", "Incorrect rows per page")
        Assert.assertEquals(vol.getAttribute("cols"), "40", "Incorrect columns per page")
        val sections = findRelativeSections!!.evaluate(vol, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(sections.length, 1, "Incorrect section count")
        val pages = findRelativePages!!.evaluate(sections.item(0), XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(pages.length, 1, "Incorrect page count")
        val p = pages.item(0)
        val expectedLines: Array<String> = arrayOf(
            "\u2820\u2811\u280c\u2800\u281e\u2811\u282d",
            "\u280e\u2811\u2809\u2815\u281d\u2819\u2800\u2807\u2814\u2811"
        )
        assertPageEquals(p, expectedLines)
    }

    @Test
    @Throws(XPathExpressionException::class)
    fun testBasicDocWithEmptyLines() {
        // The test brl
        val brl = Brl()
        brl.appendChild(UTDElements.NEW_PAGE.create())
        brl.appendChild(MoveTo(BigDecimal("0.0"), BigDecimal("10.0")))
        brl.appendChild("\u2820\u2811\u280c\u2800\u281e\u2811\u282d")
        brl.appendChild(MoveTo(BigDecimal("0.0"), BigDecimal("40.0")))
        brl.appendChild("\u280e\u2811\u2809\u2815\u281d\u2819\u2800\u2807\u2814\u2811")

        val c = BBX2PEFConverter(rows = 25, cols = 40)
        c.onStartDocument(EMPTY_DOC)
        Assert.assertFalse(c.onStartElement(brl))
        c.onEndElement(brl)
        c.onEndDocument(EMPTY_DOC)
        val pef = c.pefDoc
        val vols = findVolumes!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(vols.length, 1, "Incorrect volume count")
        val vol = vols.item(0) as org.w3c.dom.Element
        Assert.assertEquals(vol.getAttribute("rows"), "25", "Incorrect rows per page")
        Assert.assertEquals(vol.getAttribute("cols"), "40", "Incorrect columns per page")
        val sections = findRelativeSections!!.evaluate(vol, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(sections.length, 1, "Incorrect section count")
        val pages = findRelativePages!!.evaluate(sections.item(0), XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(pages.length, 1, "Incorrect page count")
        val p = pages.item(0)
        val expectedLines: Array<String> = arrayOf(
            "",
            "\u2820\u2811\u280c\u2800\u281e\u2811\u282d",
            "", "",
            "\u280e\u2811\u2809\u2815\u281d\u2819\u2800\u2807\u2814\u2811"
        )
        assertPageEquals(p, expectedLines)
    }

    @Test
    @Throws(XPathExpressionException::class)
    fun testInsertPageNumbers() {
        // The test brl
        val brl = Brl()
        brl.appendChild(UTDElements.NEW_PAGE.create())
        // Use ASCII Braille in brl, BBX2PEF will convert it.
        brl.appendChild(",te/ text")
        brl.appendChild(MoveTo(BigDecimal("235.6"), BigDecimal("0.0")))
        brl.appendChild(BrlPageNumber("#a"))
        brl.appendChild(MoveTo(BigDecimal("235.6"), BigDecimal("240.0")))
        brl.appendChild(PrintPageNumber("#b"))

        val c = BBX2PEFConverter(rows = 25, cols = 40)
        c.onStartDocument(EMPTY_DOC)
        Assert.assertFalse(c.onStartElement(brl))
        c.onEndElement(brl)
        c.onEndDocument(EMPTY_DOC)
        val pef = c.pefDoc
        val vols = findVolumes!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(vols.length, 1, "Incorrect volume count")
        val vol = vols.item(0) as org.w3c.dom.Element
        Assert.assertEquals(vol.getAttribute("rows"), "25", "Incorrect rows per page")
        Assert.assertEquals(vol.getAttribute("cols"), "40", "Incorrect columns per page")
        val sections = findRelativeSections!!.evaluate(vol, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(sections.length, 1, "Incorrect section count")
        val pages = findRelativePages!!.evaluate(sections.item(0), XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(pages.length, 1, "Incorrect page count")
        val p = pages.item(0)
        val expectedLines: Array<String> = arrayOf(
            "\u2820\u281e\u2811\u280c\u2800\u281e\u2811\u282d\u281e\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u283c\u2801",
            "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
            "\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u283c\u2803"
        )
        assertPageEquals(p, expectedLines)
    }

    @Test
    @Throws(XPathExpressionException::class)
    fun testMultiplePages() {
        // The test brl
        val brl = Brl()
        brl.appendChild(UTDElements.NEW_PAGE.create())
        // Use ASCII Braille in brl, BBX2PEF will convert it.
        brl.appendChild(",te/ text")
        brl.appendChild(MoveTo(BigDecimal("235.6"), BigDecimal("0.0")))
        brl.appendChild(BrlPageNumber("#a"))
        brl.appendChild(MoveTo(BigDecimal("235.6"), BigDecimal("240.0")))
        brl.appendChild(PrintPageNumber("#b"))
        brl.appendChild(NewPage())
        brl.appendChild(MoveTo(BigDecimal("0.0"), BigDecimal("0.0")))
        brl.appendChild(",next page")
        brl.appendChild(MoveTo(BigDecimal("235.6"), BigDecimal("0.0")))
        brl.appendChild(BrlPageNumber("#b"))
        brl.appendChild(MoveTo(BigDecimal("229.4"), BigDecimal("240.0")))
        brl.appendChild(PrintPageNumber("a#b"))

        val c = BBX2PEFConverter(rows = 25, cols = 40)
        c.onStartDocument(EMPTY_DOC)
        Assert.assertFalse(c.onStartElement(brl))
        c.onEndElement(brl)
        c.onEndDocument(EMPTY_DOC)
        val pef = c.pefDoc
        val vols = findVolumes!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(vols.length, 1, "Incorrect volume count")
        val vol = vols.item(0) as org.w3c.dom.Element
        Assert.assertEquals(vol.getAttribute("rows"), "25", "Incorrect rows per page")
        Assert.assertEquals(vol.getAttribute("cols"), "40", "Incorrect columns per page")
        val sections = findRelativeSections!!.evaluate(vol, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(sections.length, 1, "Incorrect section count")
        val pages = findRelativePages!!.evaluate(sections.item(0), XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(pages.length, 2, "Incorrect page count")
        var p = pages.item(0)
        var expectedLines: Array<String> = arrayOf(
            "\u2820\u281e\u2811\u280c\u2800\u281e\u2811\u282d\u281e\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u283c\u2801",
            "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
            "\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u283c\u2803"
        )
        assertPageEquals(p, expectedLines)
        p = pages.item(1)
        expectedLines = arrayOf(
            "\u2820\u281d\u2811\u282d\u281e\u2800\u280f\u2801\u281b\u2811\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u283c\u2803",
            "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "",
            "\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2800\u2801\u283c\u2803"
        )
        assertPageEquals(p, expectedLines)
    }

    @Test
    @Throws(XPathExpressionException::class)
    fun testInsertBrlOnly() {
        // The test brl
        val brl = Brl()
        brl.appendChild(NewPage())
        // Use ASCII Braille in brl, BBX2PEF will convert it.
        brl.appendChild(",te/ text")
        brl.appendChild(MoveTo(BigDecimal("12.4"), BigDecimal("10.0")))
        val brlOnly = BrlOnly()
        brlOnly.appendChild("33 33 33")
        brl.appendChild(brlOnly)

        val c = BBX2PEFConverter(rows = 25, cols = 40)
        c.onStartDocument(EMPTY_DOC)
        Assert.assertFalse(c.onStartElement(brl))
        c.onEndElement(brl)
        c.onEndDocument(EMPTY_DOC)
        val pef = c.pefDoc
        val vols = findVolumes!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(vols.length, 1, "Incorrect volume count")
        val vol = vols.item(0) as org.w3c.dom.Element
        Assert.assertEquals(vol.getAttribute("rows"), "25", "Incorrect rows per page")
        Assert.assertEquals(vol.getAttribute("cols"), "40", "Incorrect columns per page")
        val sections = findRelativeSections!!.evaluate(vol, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(sections.length, 1, "Incorrect section count")
        val pages = findRelativePages!!.evaluate(sections.item(0), XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(pages.length, 1, "Incorrect page count")
        val p = pages.item(0)
        val expectedLines: Array<String> = arrayOf(
            "\u2820\u281e\u2811\u280c\u2800\u281e\u2811\u282d\u281e",
            "\u2800\u2800\u2812\u2812\u2800\u2812\u2812\u2800\u2812\u2812"
        )
        assertPageEquals(p, expectedLines)
    }

    @Test
    @Throws(XPathExpressionException::class)
    fun testInsertBrailleWithWhitespaceOverrun() {
        // The test brl
        val brl = Brl()
        brl.appendChild(NewPage())
        // Use ASCII Braille in brl, BBX2PEF will convert it.
        brl.appendChild("3333333333333333333333333333333333333333   	")

        val c = BBX2PEFConverter(rows = 25, cols = 40)
        c.onStartDocument(EMPTY_DOC)
        Assert.assertFalse(c.onStartElement(brl))
        c.onEndElement(brl)
        c.onEndDocument(EMPTY_DOC)
        val pef = c.pefDoc
        val vols = findVolumes!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(vols.length, 1, "Incorrect volume count")
        val vol = vols.item(0) as org.w3c.dom.Element
        Assert.assertEquals(vol.getAttribute("rows"), "25", "Incorrect rows per page")
        Assert.assertEquals(vol.getAttribute("cols"), "40", "Incorrect columns per page")
        val sections = findRelativeSections!!.evaluate(vol, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(sections.length, 1, "Incorrect section count")
        val pages = findRelativePages!!.evaluate(sections.item(0), XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(pages.length, 1, "Incorrect page count")
        val p = pages.item(0)
        val expectedLines: Array<String> = arrayOf("\u2812".repeat(40))
        assertPageEquals(p, expectedLines)
    }

    @Test
    @Throws(XPathExpressionException::class)
    fun testMultilineInsertBrailleWithWhitespaceOverrun() {
        // The test brl
        val brl = Brl()
        brl.appendChild(NewPage())
        // Use ASCII Braille in brl, BBX2PEF will convert it.
        brl.appendChild("3333333333333333333333333333333333333333   	")
        brl.appendChild(MoveTo(BigDecimal("0.0"), BigDecimal("10.0")))
        brl.appendChild("111")

        val c = BBX2PEFConverter(rows = 25, cols = 40)
        c.onStartDocument(EMPTY_DOC)
        Assert.assertFalse(c.onStartElement(brl))
        c.onEndElement(brl)
        c.onEndDocument(EMPTY_DOC)
        val pef = c.pefDoc
        val vols = findVolumes!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(vols.length, 1, "Incorrect volume count")
        val vol = vols.item(0) as org.w3c.dom.Element
        Assert.assertEquals(vol.getAttribute("rows"), "25", "Incorrect rows per page")
        Assert.assertEquals(vol.getAttribute("cols"), "40", "Incorrect columns per page")
        val sections = findRelativeSections!!.evaluate(vol, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(sections.length, 1, "Incorrect section count")
        val pages = findRelativePages!!.evaluate(sections.item(0), XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(pages.length, 1, "Incorrect page count")
        val p = pages.item(0)
        val expectedLines: Array<String> = arrayOf(
            "\u2812".repeat(40),
            "\u2802\u2802\u2802"
        )
        assertPageEquals(p, expectedLines)
    }

    @Test
    @Throws(XPathExpressionException::class)
    fun testMultipleVolumes() {
        // The test brl
        val brl = Brl()
        brl.appendChild(UTDElements.NEW_PAGE.create())
        // Use ASCII Braille in brl, BBX2PEF will convert it.
        brl.appendChild(",te/ text")
        val volumeContainer = Element("CONTAINER", BB_NS)
        volumeContainer.addAttribute(Attribute("bb:type", BB_NS, "VOLUME"))
        val endVolBrl = Brl()
        endVolBrl.appendChild(MoveTo(BigDecimal("0.0"), BigDecimal("20.0")))
        endVolBrl.appendChild(",5d volume")
        volumeContainer.appendChild(endVolBrl)
        val brl2 = Brl()
        brl2.appendChild(NewPage())
        brl2.appendChild(",text 9 vol#b")

        val c = BBX2PEFConverter(rows = 25, cols = 40)
        c.onStartDocument(EMPTY_DOC)
        Assert.assertFalse(c.onStartElement(brl))
        c.onEndElement(brl)
        Assert.assertTrue(c.onStartElement(volumeContainer))
        Assert.assertFalse(c.onStartElement(endVolBrl))
        c.onEndElement(endVolBrl)
        c.onEndElement(volumeContainer)
        Assert.assertFalse(c.onStartElement(brl2))
        c.onEndElement(brl2)
        c.onEndDocument(EMPTY_DOC)
        val pef = c.pefDoc
        val vols = findVolumes!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(vols.length, 2, "Incorrect volume count")
        var vol = vols.item(0) as org.w3c.dom.Element
        Assert.assertEquals(vol.getAttribute("rows"), "25", "Incorrect rows per page")
        Assert.assertEquals(vol.getAttribute("cols"), "40", "Incorrect columns per page")
        var sections = findRelativeSections!!.evaluate(vol, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(sections.length, 1, "Incorrect section count")
        var pages = findRelativePages!!.evaluate(sections.item(0), XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(pages.length, 1, "Incorrect page count")
        var p = pages.item(0)
        var expectedLines: Array<String> = arrayOf(
            "\u2820\u281e\u2811\u280c\u2800\u281e\u2811\u282d\u281e",
            "",
            "\u2820\u2822\u2819\u2800\u2827\u2815\u2807\u2825\u280d\u2811"
        )
        assertPageEquals(p, expectedLines)

        vol = vols.item(1) as org.w3c.dom.Element
        Assert.assertEquals(vol.getAttribute("rows"), "25", "Incorrect rows per page")
        Assert.assertEquals(vol.getAttribute("cols"), "40", "Incorrect columns per page")
        sections = findRelativeSections!!.evaluate(vol, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(sections.length, 1, "Incorrect section count")
        pages = findRelativePages!!.evaluate(sections.item(0), XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(pages.length, 1, "Incorrect page count")
        p = pages.item(0)
        expectedLines =
            arrayOf("\u2820\u281e\u2811\u282d\u281e\u2800\u2814\u2800\u2827\u2815\u2807\u283c\u2803")
        assertPageEquals(p, expectedLines)
    }

    @Test
    @Throws(XPathExpressionException::class)
    fun testMultipleVolumesWithFilter() {
        // The test brl
        val brl = Brl()
        brl.appendChild(UTDElements.NEW_PAGE.create())
        // Use ASCII Braille in brl, BBX2PEF will convert it.
        brl.appendChild(",te/ text")
        val volumeContainer = Element("CONTAINER", BB_NS)
        volumeContainer.addAttribute(Attribute("bb:type", BB_NS, "VOLUME"))
        val endVolBrl = Brl()
        endVolBrl.appendChild(MoveTo(BigDecimal("0.0"), BigDecimal("20.0")))
        endVolBrl.appendChild(",5d volume #a")
        volumeContainer.appendChild(endVolBrl)
        val brl2 = Brl()
        brl2.appendChild(NewPage())
        brl2.appendChild(",text 9 vol#b")
        val volumeContainer2 = Element("CONTAINER", BB_NS)
        volumeContainer2.addAttribute(Attribute("bb:type", BB_NS, "VOLUME"))
        val endVolBrl2 = Brl()
        endVolBrl2.appendChild(MoveTo(BigDecimal("0.0"), BigDecimal("20.0")))
        endVolBrl2.appendChild(",5d volume #b")
        volumeContainer2.appendChild(endVolBrl2)
        val brl3 = Brl()
        brl3.appendChild(NewPage())
        brl3.appendChild(",text 9 vol#c")

        val c = BBX2PEFConverter(rows = 25, cols = 40, volumeFilter = IntPredicate { x: Int -> x == 1 })
        c.onStartDocument(EMPTY_DOC)
        Assert.assertFalse(c.onStartElement(brl))
        c.onEndElement(brl)
        Assert.assertTrue(c.onStartElement(volumeContainer))
        Assert.assertFalse(c.onStartElement(endVolBrl))
        c.onEndElement(endVolBrl)
        c.onEndElement(volumeContainer)
        Assert.assertFalse(c.onStartElement(brl2))
        c.onEndElement(brl2)
        Assert.assertTrue(c.onStartElement(volumeContainer2))
        Assert.assertFalse(c.onStartElement(endVolBrl2))
        c.onEndElement(endVolBrl2)
        c.onEndElement(volumeContainer2)
        Assert.assertFalse(c.onStartElement(brl3))
        c.onEndElement(brl3)
        c.onEndDocument(EMPTY_DOC)
        val pef = c.pefDoc
        val vols = findVolumes!!.evaluate(pef, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(vols.length, 1, "Incorrect volume count")
        val vol = vols.item(0) as org.w3c.dom.Element
        Assert.assertEquals(vol.getAttribute("rows"), "25", "Incorrect rows per page")
        Assert.assertEquals(vol.getAttribute("cols"), "40", "Incorrect columns per page")
        val sections = findRelativeSections!!.evaluate(vol, XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(sections.length, 1, "Incorrect section count")
        val pages = findRelativePages!!.evaluate(sections.item(0), XPathConstants.NODESET) as org.w3c.dom.NodeList
        Assert.assertEquals(pages.length, 1, "Incorrect page count")
        val p = pages.item(0)
        val expectedLines: Array<String> = arrayOf(
            "\u2820\u281e\u2811\u282d\u281e\u2800\u2814\u2800\u2827\u2815\u2807\u283c\u2803",
            "",
            "\u2820\u2822\u2819\u2800\u2827\u2815\u2807\u2825\u280d\u2811\u2800\u283c\u2803"
        )
        assertPageEquals(p, expectedLines)
    }

    companion object {
        val EMPTY_DOC: Document = Document(Element("root"))
    }
}

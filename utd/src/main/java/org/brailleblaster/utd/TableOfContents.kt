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
package org.brailleblaster.utd

import nu.xom.*
import org.apache.commons.lang3.time.StopWatch
import org.brailleblaster.utd.exceptions.UTDTranslateException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.regex.Pattern

/**
 * This class is used to process the Table of Contents.
 *
 *
 * There are three choices for the TOC:
 * 1a. generateTOC() - Generate TOC from the headings at the default location.
 * 1b. generateTOC() - Generate TOC from a specific location.
 * 2. updatePrintTOC() - Modify the translated print TOC to reflect correct page numbers.
 */
class TableOfContents {
    /**
     * Accepts keyword to perform either update or generate TOC. To use the default
     * location (end of p-pages), type null for location.
     *
     * @param keyword  - "update" or "generate"
     * @param document - active document
     * @param location - Node location in the document
     * @throws IllegalArgumentException - when the keyword is neither of the above
     */
    fun applyTOC(keyword: String, document: Document, location: Node?) {
        if (keyword == "update") {
            updatePrintTOC(document)
        } else if (keyword == "generate") {
            if (location == null) {
                generateTOC(document)
            } else {
                generateTOC(document, location)
            }
        } else {
            throw IllegalArgumentException("Keywords accepted: update, generate.")
        }
    }

    /**
     * Finds all the <list>s in the given document with class=toc.
     *
     * @param document
    </list> */
    fun updatePrintTOC(document: Document) {
        val listNodes = document.query(
            "descendant::*[contains(name(), 'list')]"
                    + "[@class=\"toc\" and @depth=\"1\"]"
        )

        for (i in 0 until listNodes.size()) {
            exploreList(document, listNodes[i] as Element, 1)
        }
    }

    /**
     * Goes through the <list> to find each pair of <lic class="toc"> and
     * <lic class="tocpage"> so that the title within the first <lic> is searched
     * for a match in the document as a heading with the correct level.
     *
     *
     * tocpage is then updated so that it contains the correct <newPage> element
     * and it has a correct placeholder for the guide dots.
     *
     * @param document   - the document with the translated TOC
     * @param parentList - the list with attributes: <list class="toc" depth="1">
     * @param listNumber - the first number should be 1 and will be updated as it
     * goes through the children of the list
    </list></newPage></lic></lic></lic></list> */
    fun exploreList(document: Document, parentList: Element, listNumber: Int) {
        // Find <li>
        val unitGroup = parentList.query("child::*[contains(name(), 'li')]")

        for (i in 0 until unitGroup.size()) {
            // Find <lic class="toc"..>
            val tocTitle = unitGroup[i].query(
                "child::*[contains(name(), 'lic')]"
                        + "[@class='toc']"
            )[0]

            // Get the title concerned
            val title = tocTitle.query("child::text()")[0]
            val str = title.value.trim { it <= ' ' }

            // Find the title in another part of the
            // document that is present inside a heading tag
            val headText = ("descendant::*[contains(name(), 'h" + listNumber
                    + "')][contains(.,'" + str + "')]")
            val heading = document.query(headText)[0]

            // Search newpage preceding or closest to specific node
            val pages = heading.query("descendant::*[contains(name(), 'newPage')][1]")
            val newpage = if (pages.size() == 0) {
                heading.query("preceding::*[contains(name(), 'newPage')][1]")[0] as Element
            } else {
                pages[0] as Element
            }

            //Update the current page with the found page
            val tocPage = unitGroup[i].query(
                "child::*[contains(name(), 'lic')]"
                        + "[@class=\"tocpage\"]"
            )[0] as Element

            tocPage.removeChildren()

            val brlNumber: Node = Text(newpage.getAttributeValue("brlnum"))
            val dots: Node = Element("dots")
            val brlElement = Element("brl")
            brlElement.appendChild(dots)
            brlElement.appendChild(brlNumber)
            tocPage.appendChild(brlElement)

            // Check if another list is present
            val hasList = unitGroup[i].query("child::*[contains(name(), 'list')]")
            if (hasList.size() > 0) {
                val nextList = unitGroup[i].query("child::*[contains(name(), 'list')]")[0] as Element
                val listNum = nextList.getAttributeValue("depth").toInt()
                exploreList(document, nextList, listNum)
            }
        }
    }

    /**
     * Due to extremely inconsistent TOC's between publishers this attempts to guess the TOC content
     *
     * @param doc
     * @param engine
     */
    fun findTOCGuess(doc: Document, engine: UTDTranslationEngine) {
        val context = engine.actionMap.namespaces.xPathContext
        val rootElement = doc.rootElement

        //Try to find the toc root
        var tocRoots = rootElement.query("descendant::dsy:list[@class='toc' and @depth='1']", context)
        if (tocRoots.size() == 0) {
            log.warn("No TOC list found, using contents level")
            tocRoots = rootElement.query("descendant::dsy:*[contains(name(), 'level')][@class='contents'][1]", context)
            if (tocRoots.size() == 0) {
                throw UTDTranslateException("Unable to find TOC")
            }
        }

        //Ghetto
        for (i in 0 until tocRoots.size()) {
            val curRoot = tocRoots[i] as Element
            log.debug("Found {}", curRoot)

            //Ignore
            detachAll(curRoot, "descendant::dsy:*[@id='NIMAS9780133268195-lvl3-0001']", context)
            detachAll(curRoot, "descendant::dsy:sidebar", context)
            detachAll(curRoot, "descendant::dsy:pagenum", context)
            detachAll(curRoot, "descendant::dsy:img", context)

            var results: Nodes
            while (curRoot.query("descendant::text()[1]").also { results = it }.size() != 0) {
                val curText = results[0] as Text

                //Get value with any relevant markup
                var searchParent = curText.parent as Element
                //log.debug("Parent {}", searchParent);
                if (HEADER_MARKUP.contains(searchParent.localName.lowercase(Locale.getDefault()))) {
                    log.debug("---HEADER--- {}", searchParent.toXML())
                    searchParent.detach()
                    continue
                }

                //Bubble to the contains tags
                while (true) {
                    //log.debug("searchParent {}", searchParent.toXML());
                    val parentTag = searchParent.localName
                    if (TOP_STOP_TAGS.contains(parentTag)) {
                        break
                    }
                    searchParent = searchParent.parent as Element
                }

                val value = searchParent.toXML()
                searchParent.detach()

                //Find the last number which we are going to assume is the page number
                val numRaw = STRIP_TAGS.matcher(value).replaceAll("").trim { it <= ' ' }
                val numBuilder = StringBuilder()
                var inDigit = false
                for (k in numRaw.length - 1 downTo 0) {
                    val curChar = numRaw[k]
                    if (inDigit) {
                        if (Character.isDigit(curChar)) numBuilder.insert(0, curChar)
                        else break
                    } else {
                        if (Character.isDigit(curChar)) {
                            inDigit = true
                            numBuilder.insert(0, curChar)
                        }
                    }
                }
                val finalText = value.replace(numBuilder.toString(), "")
                log.debug("Found digit {} in {}", numBuilder, finalText)
            }
        }


        //		//Find the bodymatter
//		Nodes bodyResult = rootElement.query("descendant::dsy:bodymatter", context);
//		if (bodyResult.size() != 1)
//			throw new UTDTranslateException("Found " + bodyResult.size() + " bodymatter");
//		Element bodyElement = (Element) bodyResult.get(0);
//
//		Nodes headerResult = bodyElement.query("descendant::dsy:*[starts-with(name(), 'h')]", context);
//		int counter = 0;
//		for (int i = 0; i < headerResult.size(); i++) {
//			Element curRoot = (Element) headerResult.get(i);
//			String rootName = curRoot.toXML().replaceAll("<.+?>", "").trim();
////			log.debug("Header {}", rootName);
//			String newtest = StringUtils.replace(test, rootName, "");
//			if (!newtest.equals(test)) {
//				counter++;
//				test = newtest;
//			}
//		}
//
//		log.debug("Counter {} Remaining {}", counter, test);
    }

    fun findTOCDocumentPearson(doc: Document, engine: UTDTranslationEngine) {
        val context = engine.actionMap.namespaces.xPathContext

        //Pick the first element with contents which is going to be the highest level
        val rootResults = doc.rootElement
            .query("descendant::dsy:*[contains(name(), 'level')][@class='contents']", context)
        if (rootResults.size() == 0) throw UTDTranslateException("Cannot find any TOC")
        else if (rootResults.size() > 1) log.warn("Found {} levels with contents class", rootResults.size())

        val root = rootResults[0] as Element
        log.debug("Found root {}", root)
    }

    fun findTOCDocumentDaisyOfficial(doc: Document, engine: UTDTranslationEngine) {
        val context = engine.actionMap.namespaces.xPathContext
        val watch = StopWatch()
        watch.start()

        //Find root
        //TODO: Support pearson books
//		Nodes root = doc.getRootElement().query("descendant::dsy:level2[@class='contents']", context);
        //Standard Daisy books
        val tocRoots = doc.rootElement.query("descendant::dsy:list[@class='toc' and @depth='1']", context)
        if (tocRoots.size() == 0) {
            throw RuntimeException("Cannot find contents with size " + tocRoots.size())
        }

        val tocResults: MutableList<TOCEntry> = ArrayList()
        for (j in 0 until tocRoots.size()) findTOCDaisyOfficial(context, tocRoots[j] as Element, 1, tocResults)

        log.debug("------------")
        for ((depth, title, page) in tocResults) {
            log.debug("{} Page {} Title {}", "-".repeat(depth), page, title)
        }

        log.debug("Finished in {}", watch)
    }

    /**
     * Recursively build toc from books that use daisy toc/tocpage class
     *
     * @param context
     * @param tocRoot
     * @param depth
     * @param results
     */
    private fun findTOCDaisyOfficial(
        context: XPathContext,
        tocRoot: Element,
        depth: Int,
        results: MutableList<TOCEntry>
    ) {
        val childElements = tocRoot.getChildElements("li", context.lookup("dsy"))
        for (i in 0 until childElements.size()) {
            val tocEntry = childElements[i]

            try {
                //Title and page num with ridiculous workarounds for mismarked books

                var title: String?
                var page: String?
                val titleResult = tocEntry.query("child::dsy:lic[@class='toc']", context)
                val pageResult = tocEntry.query("child::dsy:lic[@class='tocpage']", context)
                if (titleResult.size() == 1 && pageResult.size() == 1) {
                    title = titleResult[0].value
                    page = pageResult[0].value
                } else if (titleResult.size() == 0 && pageResult.size() == 2) {
                    title = pageResult[0].value
                    page = pageResult[1].value
                    log.warn("Invalid markup (multiple pages) for TOC title {}", title)
                } else if (titleResult.size() == 2 && pageResult.size() == 0) {
                    title = titleResult[0].value
                    page = titleResult[1].value
                    log.warn("Invalid markup (multiple titles) for TOC title {}", title)
                } else {
                    title = null
                    page = null
                    log.warn("Invalid markup (no page or title)")
                }

                if (title != null) {
                    //Try to find the location in the body
                    val query = "descendant::dsy:*[contains(name(), 'h')][contains(.,\"$title\")]"
                    //log.debug("Query {}", query);
                    val bodyLocationResult = tocEntry.document
                        .query(query, context)
                    var bodyLocation: Element?
                    if (bodyLocationResult.size() != 1) {
                        log.warn("Failed to find header at depth {} query {}", depth, query)
                        bodyLocation = null
                    } else bodyLocation = bodyLocationResult[0] as Element

                    //Try to find pages that are less
                    if (results.isNotEmpty()) {
                        val oldpage = results[results.size - 1].page
                        try {
                            val pageint = page!!.toInt()
                            val oldpageint = oldpage!!.toInt()
                            if (pageint < oldpageint) log.warn("Pages out of order old page {} page {}", oldpage, page)
                        } catch (_: NumberFormatException) { // NOPMD
                            //log.debug("Cannot parse old page {} page {}", oldpage, page);
                        }
                    }

                    //log.debug("Depth {} Title {} page {}", depth, title, page);
                    results.add(TOCEntry(depth, title, page, tocEntry, bodyLocation))
                }

                val nextDepth = depth + 1
                val subToc = tocEntry.query("child::dsy:list[@depth='$nextDepth']", context)
                if (subToc.size() == 1) {
                    findTOCDaisyOfficial(context, subToc[0] as Element, nextDepth, results)
                }
            } catch (e: UTDTranslateException) {
                //Do not re-wrap
                throw e
            } catch (e: Exception) {
                throw UTDTranslateException("Failed on node " + tocEntry.toXML(), e)
            }
        }
    }

    /**
     * Embeds the generated TOC as the last child of the frontmatter
     *
     * @param document - translated document
     */
    fun generateTOC(document: Document) {
        val newTOC = parseXML(document)

        val frontmatter = document.query(
            "descendant::*[contains(name(), "
                    + "'frontmatter')]"
        )[0] as Element

        val newTOCNode = newTOC.getChild(0).copy()
        val level1 = Element("level1")
        val h1 = Element("h1")
        h1.appendChild("Table of Contents")
        level1.appendChild(h1)
        level1.appendChild(newTOCNode)

        frontmatter.appendChild(level1)
    }

    fun generateTOC(document: Document, location: Node) {
        val newTOC = parseXML(document)
        val newTOCNode = newTOC.getChild(0).copy()

        //Catch if location is not found?
        val parent = location.parent
        parent.insertChild(newTOCNode, parent.indexOf(location))
    }

    /**
     * Parse through the formatted and styled XML document.
     *
     * @param inputDocument - points to the XML document to be parsed
     * @return new Document for the generated TOC
     */
    fun parseXML(inputDocument: Document): Element {
        val root = Element("table")

        val rootElement = inputDocument.query(
            "descendant::*"
                    + "[contains(name(), 'book')]"
        )[0] as Element

        val addOne = Element("list")
        addOne.addAttribute(Attribute("class", "toc"))
        addOne.addAttribute(Attribute("depth", "1"))
        addOne.addAttribute(Attribute("type", "ol"))

        root.appendChild(addOne)

        exploreLevel(rootElement, addOne, 1, inputDocument)

        return root
    }

    /**
     * Helps parser search for the different levels within the document. Will
     * parse through all the levels at the moment. Are we certain we want to
     * stop at level3?
     *
     *
     * If including the frontmatter, the print TOC will be counted as one of the
     * levels so it will be added as part of the generated TOC. We need a catch
     * of some form to skip the print TOC.
     *
     * @param rootElement - The root element of the document
     * @param listElement - The list element created in the generated TOC
     * @param levelNum    - The level number. Start levelNum = 1, for level1
     * @param tocDocument - Added for testing. May be removed later on.
     */
    fun exploreLevel(
        rootElement: Element, listElement: Element,
        levelNum: Int, tocDocument: Document?
    ) {
        val searchLevel = "level$levelNum"

        // Go through all the level1's
        //Elements levelList = rootElement.getChildElements(searchLevel);
        val levelList = rootElement.query(
            "descendant::*[contains(name(), '"
                    + searchLevel + "')]"
        )
        for (i in 0 until levelList.size()) {
            val levelE = levelList[i] as Element

            val li = Element("li")
            li.addAttribute(Attribute("semantics", "style, list"))
            listElement.appendChild(li)

            val hTag = "h$levelNum"
            val headE = levelE.query(
                "descendant::*[contains(name(), '"
                        + hTag + "')]"
            )

            //Element headE = levelE.getChildElements(hTag).get(0);
            if (headE.size() > 0) {
                addHeadingInfo(headE[0] as Element, li, tocDocument)
            }

            val next = levelNum + 1
            val nextLevel = "level$next"
            val xpath = ("descendant::*[contains(name(), '" + nextLevel
                    + "')]")

            if (levelE.query(xpath).size() != 0) {
                val nextList = Element("list")
                nextList.addAttribute(Attribute("class", "toc"))
                nextList.addAttribute(Attribute("depth", next.toString()))
                nextList.addAttribute(Attribute("type", "ol"))
                li.appendChild(nextList)

                exploreLevel(levelE, nextList, next, tocDocument)
            }
        }
    }

    /**
     * This should be called whenever you encounter a level in exploreLevel() so
     * that it will add the heading information to the generated TOC
     * appropriately. Some heading may contain the <newPage> tag.
     *
     *
     * The <newPage> child to the heading will not be included.
     *
     * @param heading     - the heading element from the print where it copies
     * information
     * @param appendTo-   the element within the generated TOC where it will append
     * the heading information
     * @param tocDocument - added for testing -- may be removable
    </newPage></newPage> */
    fun addHeadingInfo(
        heading: Element, appendTo: Element,
        tocDocument: Document?
    ) {
        val lic = Element("lic")
        lic.addAttribute(Attribute("class", "toc"))
        val licPage = Element("lic")
        licPage.addAttribute(Attribute("class", "tocpage"))

        for (i in 0 until heading.childCount) {
            val addNode = heading.getChild(i).copy()
            //Does not get included
            if (addNode is Text) {
                lic.appendChild(addNode)
            }
            //verify, do not append <newPage>
            if (addNode is Element && addNode.query("self::*[contains(name(), 'brl')]").size() > 0) {
                val notText = addNode.query("child::node()[not(self::text())]")
                if (notText.size() != 0) {
                    for (k in 0 until notText.size()) {
                        addNode.removeChild(notText[k])
                    }
                }
                lic.appendChild(addNode)
            }
        }

        appendTo.appendChild(lic)

        log.debug("Heading {}", heading.toXML())
        val pages = heading.query("descendant::*[contains(name(), 'pagenum')][1]")
        val newpage = if (pages.size() == 0) {
            heading.query("preceding::*[contains(name(), 'pagenum')]")[0] as Element
        } else {
            pages[0] as Element
        }

        val dots: Node = Element("dots")
        val brlElement = Element("brl")
        brlElement.appendChild(dots)
        brlElement.appendChild(newpage.value)
        licPage.appendChild(brlElement)
        appendTo.appendChild(licPage)
    }

    @JvmRecord
    data class TOCEntry(
        val depth: Int,
        val title: String,
        val page: String?,
        val tocElement: Element,
        val bookElement: Element?
    ) {
        override fun toString(): String {
            return "Depth $depth Page $page Title '$title'"
        }
    }

    companion object {
        private val SIGNIFICANT_MARKUP = listOf("em", "strong", "p")
        private val HEADER_MARKUP = listOf("h1", "h2", "h3", "h4", "hd")

        private val TOP_STOP_TAGS = listOf("list", "level1", "level2", "level3", "level4", "level5", "li")
        private val TOP_CONTAINS_TAGS = listOf("li")
        private val STRIP_TAGS: Pattern = Pattern.compile("<.+?>")

        private fun detachAll(root: Element, xpath: String, context: XPathContext) {
            val results = root.query(xpath, context)
            var i = 0
            while (i < results.size()) {
                results[i].detach()
                i++
            }
            log.debug("Removed {} elements with query '{}'", i, xpath)
        }

        private val log: Logger = LoggerFactory.getLogger(TableOfContents::class.java)
    }
}

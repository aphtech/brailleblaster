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
package org.brailleblaster.bbx.parsers

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.annotation.*
import jakarta.xml.bind.annotation.adapters.XmlAdapter
import org.brailleblaster.utd.NamespaceMap
import org.brailleblaster.utd.NodeMatcherMap
import org.brailleblaster.utd.config.UTDConfig
import org.brailleblaster.utd.matchers.INodeMatcher
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Lookup map used to parse document
 */
class ImportParserMap : NodeMatcherMap<ImportParser?>(null as ImportParser?) {

    @XmlType(propOrder = ["namespaces", "semanticEntries"])
    @XmlRootElement(name = "importParserMap")
    class AdaptedParserMap @JvmOverloads constructor(@get:XmlElement(name = "entry") var semanticEntries: MutableList<Entry> = ArrayList()) {
        @XmlRootElement(name = "entry")
        @XmlAccessorType(XmlAccessType.FIELD)
        class Entry(
            @field:XmlElement(name = "matcher") private val matcher: INodeMatcher?, @field:XmlElement(
                name = "parser"
            ) private val parser: ImportParser?
        ) {
            // JAXB requires the following no args constructor
            private constructor() : this(null, null)

            fun getMatcher(): INodeMatcher {
                return matcher!!
            }

            fun getParser(): ImportParser {
                return parser!!
            }
        }

        var namespaces: NamespaceMap? = null
    }

    private class ParserMapAdapter : XmlAdapter<AdaptedParserMap?, ImportParserMap?>() {
        override fun marshal(actions: ImportParserMap?): AdaptedParserMap? = if (actions == null) {
            null
        } else {
            AdaptedParserMap().apply {
                semanticEntries.addAll(actions.map { (matcher, parser) ->
                    requireNotNull(parser)
                    AdaptedParserMap.Entry(matcher, parser)
                })
                namespaces = actions.namespaces
            }
        }

        override fun unmarshal(actionList: AdaptedParserMap?): ImportParserMap? {
            if (actionList == null) {
                return null
            }
            val actionMap = ImportParserMap()
            actionMap.namespaces = actionList.namespaces ?: NamespaceMap()
            for ((i, entry) in actionList.semanticEntries.withIndex()) {
                val matcher = entry.getMatcher()
                if (matcher == null) {
                    throw NullPointerException("Unexpected null matcher for parser " + entry.getParser())
                } else if (actionMap.containsKey(matcher)) {
                    //This will cause a cryptic "IndexOutOfBoundsException: Index: 2, Size: 1"
                    //Give more information so someone knows what to fix
                    throw RuntimeException("Detected duplicate matcher " + matcher + " entry " + entry.getParser())
                } else if (entry.getParser() == null) {
                    throw NullPointerException("Unexpected null parser for matcher $matcher")
                }
                actionMap.put(i, matcher, entry.getParser())
            }
            return actionMap
        }
    }
    companion object {
        private val log: Logger = LoggerFactory.getLogger(ImportParserMap::class.java)
        private val JAXB_CONTEXT: JAXBContext = try {
            JAXBContext.newInstance(AdaptedParserMap::class.java)
        } catch (e: Exception) {
            throw RuntimeException("Unable to init JAXBContext", e)
        }

        fun load(inputFile: File): ImportParserMap {
            log.debug("Loading parserMap from " + inputFile.absolutePath)
            val adapter = ParserMapAdapter()

            val map = adapter.unmarshal(UTDConfig.loadJAXB(inputFile, AdaptedParserMap::class.java, JAXB_CONTEXT))!!
            if (log.isDebugEnabled) {
                for ((key, value) in map) {
                    log.debug("Entry {}\t{}", key, value)
                }
            }
            return map
        }
    }
}

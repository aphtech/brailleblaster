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
package org.brailleblaster.bbx.fixers

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

class ImportFixerMap(defaultValue: ImportFixer? = null) : NodeMatcherMap<ImportFixer?>(defaultValue) {

    @XmlType(propOrder = ["namespaces", "semanticEntries"])
    @XmlRootElement(name = "fixerMap")
    class AdaptedFixerMap @JvmOverloads constructor(@get:XmlElement(name = "entry") var semanticEntries: MutableList<Entry> = ArrayList()) {
        @XmlRootElement(name = "entry")
        @XmlAccessorType(XmlAccessType.FIELD)
        class Entry(
            @field:XmlElement(name = "matcher") private val matcher: INodeMatcher?, @field:XmlElement(
                name = "fixer"
            ) private val fixer: ImportFixer?
        ) {
            // JAXB requires the following no args constructor
            private constructor() : this(null, null)

            fun getMatcher(): INodeMatcher {
                return matcher!!
            }

            fun getFixer(): ImportFixer {
                return fixer!!
            }
        }

        var namespaces: NamespaceMap? = null
    }

    private class FixerMapAdapter : XmlAdapter<AdaptedFixerMap?, ImportFixerMap?>() {
        override fun marshal(actions: ImportFixerMap?): AdaptedFixerMap? = if (actions == null) {
            null
        } else {
            AdaptedFixerMap().apply {
                semanticEntries.addAll(actions.map { (matcher, wrapper) ->
                    requireNotNull(wrapper)
                    AdaptedFixerMap.Entry(matcher, wrapper)
                })
                namespaces = actions.namespaces
            }
        }

        override fun unmarshal(actionList: AdaptedFixerMap?): ImportFixerMap? {
            if (actionList == null) {
                return null
            }
            val actionMap = ImportFixerMap()
            actionMap.namespaces = actionList.namespaces ?: NamespaceMap()
            for ((i, entry) in actionList.semanticEntries.withIndex()) {
                if (actionMap.containsKey(entry.getMatcher())) {
                    //This will cause a cryptic "IndexOutOfBoundsException: Index: 2, Size: 1"
                    //Give more information so someone knows what to fix
                    throw RuntimeException("Detected duplicate matcher " + entry.getMatcher() + " entry " + entry.getFixer())
                }
                actionMap.put(i, entry.getMatcher(), entry.getFixer())
            }
            return actionMap
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ImportFixerMap::class.java)
        private val JAXB_CONTEXT: JAXBContext = try {
            JAXBContext.newInstance(AdaptedFixerMap::class.java)
        } catch (e: Exception) {
            throw RuntimeException("Unable to init JAXBContext", e)
        }

        fun load(inputFile: File): ImportFixerMap {
            log.debug("Loading fixerMap from " + inputFile.absolutePath)
            val adapter = FixerMapAdapter()

            val map = adapter.unmarshal(
                UTDConfig.loadJAXB(inputFile, AdaptedFixerMap::class.java, JAXB_CONTEXT)
            )
            for ((key, value) in map!!) {
                log.debug("Entry {}	{}", key, value)
            }
            return map
        }
    }
}

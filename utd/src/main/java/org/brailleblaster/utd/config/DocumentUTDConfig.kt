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
package org.brailleblaster.utd.config

import jakarta.xml.bind.JAXBContext
import nu.xom.Document
import nu.xom.Element
import nu.xom.converters.DOMConverter
import nu.xom.converters.SAXConverter
import org.apache.commons.lang3.StringUtils
import org.brailleblaster.utd.*
import org.brailleblaster.utd.exceptions.UTDException
import org.brailleblaster.utd.internal.*
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.properties.UTDElements
import org.brailleblaster.utils.UTD_NS
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.xml.sax.Attributes
import org.xml.sax.ContentHandler
import org.xml.sax.Locator
import org.xml.sax.SAXException
import javax.xml.parsers.DocumentBuilderFactory

class DocumentUTDConfig(private val rootElement: String, private val rootNamespace: String) {
    class IgnoreNamespaceHandler(private val handler: ContentHandler, vararg namespaces: String) : ContentHandler {
        private val namespaces: List<String> = namespaces.toList()

        @Throws(SAXException::class)
        override fun characters(ch: CharArray, start: Int, length: Int) {
            handler.characters(ch, start, length)
        }

        @Throws(SAXException::class)
        override fun endDocument() {
            handler.endDocument()
        }

        @Throws(SAXException::class)
        override fun endElement(uri: String, localName: String, qName: String) {
            if (namespaces.contains(uri)) {
                handler.endElement("", localName, qName)
            } else {
                handler.endElement(uri, localName, qName)
            }
        }

        @Throws(SAXException::class)
        override fun endPrefixMapping(prefix: String) {
            handler.endPrefixMapping(prefix)
        }

        @Throws(SAXException::class)
        override fun ignorableWhitespace(ch: CharArray, start: Int, length: Int) {
            handler.ignorableWhitespace(ch, start, length)
        }

        @Throws(SAXException::class)
        override fun processingInstruction(target: String, data: String) {
            handler.processingInstruction(target, data)
        }

        override fun setDocumentLocator(locator: Locator) {
            handler.setDocumentLocator(locator)
        }

        @Throws(SAXException::class)
        override fun skippedEntity(name: String) {
            handler.skippedEntity(name)
        }

        @Throws(SAXException::class)
        override fun startDocument() {
            handler.startDocument()
        }

        @Throws(SAXException::class)
        override fun startElement(uri: String, localName: String, qName: String, atts: Attributes) {
            if (namespaces.contains(uri)) {
                handler.startElement("", localName, qName, atts)
            } else {
                handler.startElement(uri, localName, qName, atts)
            }
        }

        @Throws(SAXException::class)
        override fun startPrefixMapping(prefix: String, uri: String) {
            handler.startPrefixMapping(prefix, uri)
        }
    }

    fun loadPageSettings(doc: Document): PageSettings? {
        log.debug("Loading Page Settings from file document config")
        return loadFromDoc(doc, PageSettings::class.java, PageSettings::class.java)
    }

    fun savePageSettings(doc: Document, pageSettings: PageSettings) {
        log.debug("Writing Page Settings to document config")
        saveToDoc(doc, pageSettings, PageSettings::class.java)
    }

    fun loadBrailleSettings(doc: Document): BrailleSettings? {
        log.debug("Loading Braille Settings from document config")
        return loadFromDoc(doc, BrailleSettings::class.java, BrailleSettings::class.java)
    }

    fun saveBrailleSettings(doc: Document, brailleSettings: BrailleSettings) {
        log.debug("Writing Braille Settings to document config")
        saveToDoc(doc, brailleSettings, BrailleSettings::class.java)
    }

    fun loadActions(doc: Document): ActionMap? {
        log.debug("Loading ActionMap from document config")
        val adapter = ActionMapAdapter()
        return adapter.unmarshal(loadFromDoc(doc, AdaptedActionMap::class.java, ActionMap::class.java))
    }

    fun saveActions(doc: Document, actionMap: IActionMap) {
        log.debug("Writing ActionMap to document config")
        require(actionMap is ActionMap) {
            ("Only supports ActionMap, "
                    + "given " + actionMap + " " + actionMap.javaClass)
        }
        val adapter = ActionMapAdapter()
        saveToDoc(doc, adapter.marshal(actionMap)!!, ActionMap::class.java)
    }

    fun loadStyle(doc: Document, styleDefs: StyleDefinitions?): StyleMap? {
        log.debug("Loading StyleMap from document config")
        val adapter = StyleMapAdapter(styleDefs)
        return adapter.unmarshal(loadFromDoc(doc, AdaptedStyleMap::class.java, StyleMap::class.java))
    }

    fun saveStyle(doc: Document, styleMap: IStyleMap) {
        log.debug("Writing StyleMap to document config")
        require(styleMap is StyleMap) {
            ("Only supports StyleMap, "
                    + "given " + styleMap + " " + styleMap.javaClass)
        }
        val adapter = StyleMapAdapter(false)
        saveToDoc(doc, adapter.marshal(styleMap)!!, StyleMap::class.java)
    }

    fun saveStyleWithDefs(doc: Document, styleMap: IStyleMap) {
        log.debug("Writing StyleMap with definitions to document config")
        require(styleMap is StyleMap) {
            ("Only supports StyleMap, "
                    + "given " + styleMap + " " + styleMap.javaClass)
        }
        val adapter = StyleMapAdapter(true)
        saveToDoc(doc, adapter.marshal(styleMap)!!, StyleMap::class.java)
    }

    private fun saveToDoc(xomDoc: Document, jaxbValue: Any, headClass: Class<*>) {
        try {
            val jc = JAXBContext.newInstance(jaxbValue.javaClass, StyleOptionsFactory::class.java)
            val marshaller = jc.createMarshaller()

            marshaller.eventHandler = JAXBUtils.FAIL_ON_EXCEPTIONS_HANDLER

            val documentBuilderFactory = DocumentBuilderFactory.newInstance()
            val documentBuilder = documentBuilderFactory.newDocumentBuilder()
            val jaxbDoc = documentBuilder.newDocument()

            //Save JAXB to Java XML then convert to XOM
            marshaller.marshal(jaxbValue, jaxbDoc)
            val xomConfigDoc = DOMConverter.convert(jaxbDoc)
            val xomConfig = xomConfigDoc.rootElement.copy()
            xomConfig.detach()
            //Changing the JAXB namespace either requires a package annotation for every
            //class that might be stored here or oracle-jvm specific classes. Its easier
            //to do it from XOM
            XMLHandler.setNamespaceRecursive(xomConfig, UTD_NS)

            //Now can take the XOM JAXB and save it to the XOM doc
            //Remove the old config if it exists
            val headElem = getOrCreateHeadElement(xomDoc)
            val valueName = jaxbClassToElementName(headClass)
            for (curElem in headElem.getChildElements(valueName, UTD_NS)) {
                curElem.detach()
            }

            headElem.appendChild(xomConfig)
        } catch (e: Exception) {
            throw UTDException("Cannot save settings", e)
        }
    }

    private fun <V> loadFromDoc(xomDoc: Document, jaxbClass: Class<V>, headClass: Class<*>): V? {
        try {
            var valueElem = getConfigElement(xomDoc, headClass) ?: return null

            //Copy to new document as endDocument needs to be called on the ContentHandler
            valueElem = valueElem.copy()
            val parsableXomDoc = Document(valueElem)
            val jc = JAXBContext.newInstance(jaxbClass, StyleOptionsFactory::class.java)

            val unmarshaller = jc.createUnmarshaller()

            unmarshaller.eventHandler = JAXBUtils.FAIL_ON_EXCEPTIONS_HANDLER

            val unmarshallerHandler = unmarshaller.unmarshallerHandler
            //Use a SAXConverter, the handler used will ignore the UTD namespace
            //The UTD namespace needs to be ignored due to the hack in saveToDoc which avoids correct JAXB definitions
            val converter = SAXConverter(IgnoreNamespaceHandler(unmarshallerHandler, UTD_NS))
            converter.convert(parsableXomDoc)
            val result = unmarshallerHandler.result
            if (jaxbClass.isInstance(result)) {
                return jaxbClass.cast(result)
            } else {
                throw RuntimeException("Unexpected type for settings")
            }
        } catch (e: Exception) {
            throw UTDException("Cannot load settings", e)
        }
    }

    fun getHeadElement(doc: Document): Element? {
        val root = doc.rootElement
        val headResults = root.getChildElements(rootElement, getRootNamespace(doc))
        if (headResults.size() == 0) {
//			log.debug("Document doesn't have head element");
            return null
        }
        return headResults[0]
    }

    fun getOrCreateHeadElement(doc: Document): Element {
        val root = doc.rootElement
        val headResults = root.getChildElements(rootElement, getRootNamespace(doc))
        if (headResults.size() == 0) {
            log.debug("Document doesn't have head element, creating one")
            val headElem = Element(rootElement, getRootNamespace(doc))
            root.insertChild(headElem, 0)
            return headElem
        }
        return headResults[0]
    }

    fun getRootNamespace(doc: Document): String {
        return rootNamespace.ifBlank { doc.rootElement.namespaceURI }
    }

    fun getConfigElement(doc: Document, tagClass: Class<*>): Element? {
        val tagName = jaxbClassToElementName(tagClass)
        return getConfigElement(doc, tagName)
    }

    fun getConfigElement(doc: Document, elementName: String?): Element? {
        val headElem = getHeadElement(doc)
        if (headElem == null) {
            log.debug("Failed to find Element {}, Document doesn't have head element", elementName)
            return null
        }

        val headValueResults = headElem.getChildElements(elementName, UTD_NS)
        if (headValueResults.size() == 0) {
            log.debug("Failed to find Element {} in document head", elementName)
            return null
        }

        return headValueResults[0]
    }

    fun setSetting(doc: Document, key: String, value: String?) {
        var settingElem = getConfigElement(doc, key)
        if (settingElem == null) {
            settingElem = Element(UTDElements.UTD_PREFIX + ":" + key, UTD_NS)
            getOrCreateHeadElement(doc).appendChild(settingElem)
        }
        settingElem.removeChildren()
        settingElem.appendChild(value)
        log.debug("Added UTD setting to document: {}", settingElem.toXML())
    }

    fun getSetting(doc: Document, key: String?): String? {
        val settingElem = getConfigElement(doc, key) ?: return null
        log.debug("Found setting key {} in document with value {}", key, settingElem.value)
        return settingElem.value
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(DocumentUTDConfig::class.java)

        @JvmField
        val NIMAS: DocumentUTDConfig = DocumentUTDConfig("head", "")
        fun jaxbClassToElementName(tagClass: Class<*>): String {
            return StringUtils.uncapitalize(tagClass.simpleName)
        }
    }
}

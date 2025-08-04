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

import com.google.common.base.CaseFormat
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import jakarta.xml.bind.annotation.XmlElementRef
import jakarta.xml.bind.annotation.adapters.XmlAdapter
import org.apache.commons.io.input.BOMInputStream
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.reflect.MethodUtils
import org.brailleblaster.utd.*
import org.brailleblaster.utd.Style.StyleOption
import org.brailleblaster.utd.exceptions.UTDException
import org.brailleblaster.utd.internal.*
import org.brailleblaster.utd.internal.JAXBUtils.toXML
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.*
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamReader
import javax.xml.transform.stream.StreamResult

/**
 * Load a configuration from XML.
 */
object UTDConfig {
    private val log: Logger = LoggerFactory.getLogger(UTDConfig::class.java)
    val STYLE_FIELDS: Map<String, Field>
    val STYLE_FIELD_SETTERS: Map<Field, Method>
    val STYLE_FIELD_GETTERS: Map<Field, Method>
    @JvmField
    val STYLE_OPTION_FIELDS: Map<StyleOption, Field>
    private var JAXB_CONTEXT_SETTINGS: JAXBContext? = null
    private var JAXB_CONTEXT_ACTIONMAP: JAXBContext? = null
    private var JAXB_CONTEXT_STYLE_MAP: JAXBContext? = null
    private var JAXB_CONTEXT_STYLE_DEFINITIONS: JAXBContext? = null
    private var JAXB_XML_FACTORY: XMLInputFactory? = null
    private const val SHORTCUT_ID_TAG = "id"
    private const val SHORTCUT_KEY_COMBINATION_TAG = "key-combination"

    init {
        val fieldsDecl = Style::class.java.declaredFields
        val styleFields = mutableMapOf<String, Field>()
        val styleSetters = mutableMapOf<Field, Method>()
        val styleGetters = mutableMapOf<Field, Method>()
        val styleOptionFields = mutableMapOf<StyleOption, Field>()
        for (curField in fieldsDecl) {
            val name = curField.name
            if (Modifier.isStatic(curField.modifiers)) continue

            curField.isAccessible = true
            styleFields.put(name, curField)

            try {
                // Try #1 with get prefix
                val getterName = "get" + StringUtils.capitalize(name)
                var getter = MethodUtils.getAccessibleMethod(Style::class.java, getterName)
                val getterNameIs = "is" + StringUtils.capitalize(name)
                if (getter == null) {
                    // Try #2: Maybe its a boolean
                    getter = MethodUtils.getAccessibleMethod(Style::class.java, getterNameIs)
                }
                if (getter == null && name.startsWith("is")) {
                    // Try #3: Table fields start with is for some reason...
                    getter = MethodUtils.getAccessibleMethod(Style::class.java, name)
                }
                if (getter == null)  // No clue
                    throw NoSuchMethodException(
                        ("Cannot find method " + getterName + "() " + "or " + getterNameIs
                                + "() or " + name + "() in " + Style::class.java)
                    )
                styleGetters.put(curField, getter)

                // Try to guess setter name
                val fieldClass = getter.returnType
                val setterName = "set" + StringUtils.capitalize(name)
                val setter =
                    Style::class.java.getDeclaredMethod(setterName, fieldClass)
                styleSetters.put(curField, setter)

                val xmlRef = curField.getAnnotation(XmlElementRef::class.java)
                if (xmlRef != null) {
                    try {
                        val fieldStyleOption = StyleOption.valueOf(
                            CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, xmlRef.name)
                        )
                        styleOptionFields.put(fieldStyleOption, curField)
                    } catch (_: IllegalArgumentException) {
                        //ignore unrelated style fields
                    }
                }
            } catch (e: RuntimeException) {
                throw e
            } catch (ex: Exception) {
                throw RuntimeException("Unable to find methods for field $name", ex)
            }
        }
        STYLE_FIELDS = styleFields.toMap()
        STYLE_FIELD_SETTERS = styleSetters.toMap()
        STYLE_FIELD_GETTERS = styleGetters.toMap()
        STYLE_OPTION_FIELDS = styleOptionFields.toMap()

        // Expensive to make a new JAXBContext for each load, so only make
        // one on init
        try {
            JAXB_CONTEXT_SETTINGS = JAXBContext.newInstance(PageSettings::class.java, BrailleSettings::class.java)

            // AdaptedActionMap.Entry and AdaptedStyleMap.Entry conflict with
            // each other
            JAXB_CONTEXT_ACTIONMAP = JAXBContext.newInstance(AdaptedActionMap::class.java)
            JAXB_CONTEXT_STYLE_MAP =
                JAXBContext.newInstance(AdaptedStyleMap::class.java, StyleOptionsFactory::class.java)

            JAXB_CONTEXT_STYLE_DEFINITIONS = JAXBContext.newInstance(
                StyleDefinitions::class.java,
                StyleOptionsFactory::class.java
            )

            JAXB_XML_FACTORY = XMLInputFactory.newInstance()
        } catch (e: Exception) {
            throw RuntimeException("Unable to init JAXBContext", e)
        }
    }


    /**
     * Utility to load the styleMap and actionMap files with common prefixes and
     * standard suffixes
     */
    @JvmStatic
    fun loadMappings(engine: UTDTranslationEngine, mappingsDir: File, mappingsPrefix: String) {
        require(mappingsDir.isDirectory) { "$mappingsDir for mappings is not a directory" }
        require(mappingsPrefix.isNotBlank()) { "Mapping file prefix cannot be blank" }
        engine.styleMap = loadStyle(File(mappingsDir, "$mappingsPrefix.styleMap.xml"), engine.styleDefinitions)!!
        engine.actionMap = loadActions(File(mappingsDir, "$mappingsPrefix.actionMap.xml"))!!
    }

    fun saveMappings(engine: UTDTranslationEngine, mappingsDir: File, mappingsPrefix: String) {
        require(mappingsDir.isDirectory) { "$mappingsDir for mappings is not a directory" }
        require(!StringUtils.isBlank(mappingsPrefix)) { "Mapping file prefix cannot be blank" }
        saveStyle(File(mappingsDir, "$mappingsPrefix.styleMap.xml"), engine.styleMap)
        saveActions(File(mappingsDir, "$mappingsPrefix.actionMap.xml"), engine.actionMap)
    }

    @JvmStatic
    fun loadPageSettings(pageSettingsFile: File): PageSettings {
        log.debug("Loading Page Settings from file {}", pageSettingsFile)
        return loadJAXB(
            pageSettingsFile, PageSettings::class.java,
            JAXB_CONTEXT_SETTINGS!!
        )
    }

    fun savePageSettings(pageSettingsFile: File, pageSettings: PageSettings) {
        log.debug("Writing Page Settings to file {}", pageSettingsFile)
        saveJAXB(pageSettingsFile, pageSettings, JAXB_CONTEXT_SETTINGS!!)
    }

    @JvmStatic
    fun loadBrailleSettings(brailleSettingsFile: File): BrailleSettings {
        log.debug("Loading Braille Settings from file {}", brailleSettingsFile)
        return loadJAXB(
            brailleSettingsFile, BrailleSettings::class.java,
            JAXB_CONTEXT_SETTINGS!!
        )
    }

    @JvmStatic
    fun saveBrailleSettings(brailleSettingsFile: File, brailleSettings: BrailleSettings) {
        log.debug("Writing Braille Settings to file {}", brailleSettingsFile)
        saveJAXB(brailleSettingsFile, brailleSettings, JAXB_CONTEXT_SETTINGS!!)
    }

    @JvmStatic
    fun loadActions(actionsFile: File): ActionMap? {
        log.debug("Loading ActionMap from file {}", actionsFile)
        val adapter = ActionMapAdapter()
        return adapter.unmarshal(
            loadJAXB(
                actionsFile,
                AdaptedActionMap::class.java, JAXB_CONTEXT_ACTIONMAP!!
            )
        )
    }

    @JvmStatic
    fun saveActions(actionsFile: File, actionMap: IActionMap) {
        log.debug("Writing ActionMap to file {} - overwriting {}", actionsFile, actionsFile.exists())
        require(actionMap is ActionMap) { "Only supports ActionMap, " + "given " + actionMap + " " + actionMap.javaClass }
        val adapter = ActionMapAdapter()
        saveJAXB(
            actionsFile, adapter.marshal(actionMap),
            JAXB_CONTEXT_ACTIONMAP!!
        )
    }

    @JvmStatic
    fun loadStyle(styleFile: File, styleDefs: StyleDefinitions?): StyleMap? {
        log.debug("Loading StyleMap from file {}", styleFile)
        val adapter = StyleMapAdapter(styleDefs)
        val adapters: MutableList<XmlAdapter<*, *>> = ArrayList()
        adapters.add(ComparableStyleAdapter(styleDefs))
        return adapter.unmarshal(
            loadJAXB(
                styleFile,
                AdaptedStyleMap::class.java, JAXB_CONTEXT_STYLE_MAP!!, adapters
            )
        )
    }

    @JvmStatic
    fun saveStyle(styleFile: File, styleMap: IStyleMap) {
        log.debug("Writing StyleMap to file {} - overwriting {}", styleFile, styleFile.exists())
        require(styleMap is StyleMap) { "Only supports StyleMap, " + "given " + styleMap + " " + styleMap.javaClass }
        val adapter = StyleMapAdapter(false)
        saveJAXB(
            styleFile, adapter.marshal(styleMap),
            JAXB_CONTEXT_STYLE_MAP!!
        )
    }

    fun saveStyleWithDefs(styleFile: File, styleMap: IStyleMap) {
        log.debug("Writing StyleMap with definitions to file {} - overwriting {}", styleFile, styleFile.exists())
        require(styleMap is StyleMap) { "Only supports StyleMap, " + "given " + styleMap + " " + styleMap.javaClass }
        val adapter = StyleMapAdapter(true)
        saveJAXB(
            styleFile, adapter.marshal(styleMap),
            JAXB_CONTEXT_STYLE_MAP!!
        )
    }

    /**
     * Generic loader of JAXB XML to arbitrary objects
     */
    fun <V> loadJAXB(inputFile: File, valueClass: Class<V>?, jaxbContext: JAXBContext): V {
        return loadJAXB(inputFile, valueClass, jaxbContext, ArrayList())
    }

    fun <V> loadJAXB(
        inputFile: File,
        valueClass: Class<V>?,
        jaxbContext: JAXBContext,
        adapterInstances: List<XmlAdapter<*, *>>
    ): V {
        var inputXml: XMLStreamReader? = null
        // User Exception #118: File potentially may have UTF BOM
        try {
            BOMInputStream.builder().setInputStream(FileInputStream(inputFile)).get().buffered().use { input ->
                val unmarshaller = jaxbContext.createUnmarshaller()
                for (adapter in adapterInstances) {
                    unmarshaller.setAdapter(adapter)
                }
                val interfaceAdapter = InterfaceAdapter(adapterInstances)
                unmarshaller.setAdapter(interfaceAdapter)
                //See field javadoc for why this is necessary
                unmarshaller.eventHandler = JAXBUtils.FAIL_ON_EXCEPTIONS_HANDLER

                inputXml = JAXB_XML_FACTORY!!.createXMLStreamReader(input)
                val result = unmarshaller.unmarshal(inputXml, valueClass)
                return result.value
            }
        } catch (e: Exception) {
            throw UTDException("Cannot load settings from file $inputFile", e)
        } finally {
            if (inputXml != null) {
                try {
                    inputXml.close()
                } catch (ex: XMLStreamException) {
                    log.warn("Could not close XML stream", ex)
                }
            }
        }
    }

    /**
     * Generic saver of arbitrary objects to JAXB XML
     */
    fun <V> saveJAXB(outputFile: File, value: V, jaxbContext: JAXBContext) {
        try {
            BufferedOutputStream(FileOutputStream(outputFile)).use { output ->
                val marshaller = jaxbContext.createMarshaller()
                //See field javadoc for why this is necessary
                marshaller.eventHandler = JAXBUtils.FAIL_ON_EXCEPTIONS_HANDLER
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
                marshaller.marshal(value, output)
            }
        } catch (e: Exception) {
            throw UTDException("Cannot save settings to file $outputFile", e)
        }
    }

    @JvmStatic
    fun loadStyleDefinitions(styleDefsFile: File): StyleDefinitions {
        log.debug("Loading StyleDefinitions from file {}", styleDefsFile.absolutePath)
        if (!styleDefsFile.exists()) throw RuntimeException(
            ("Style definitions file at " + styleDefsFile + " does not exist "
                    + styleDefsFile.absolutePath)
        )
        // To get the style definitions into any matcher used in a blank line exception we need to use specific instances of StyleListAdapter
        // However that requires an instance of the style definitions. So we will copy the loaded ones into the one used by the adapter.
        val result = StyleDefinitions()
        val adapters: List<XmlAdapter<*, *>> = listOf(ComparableStyleAdapter(result))
        val tmpStyleDefs = loadJAXB(
            styleDefsFile,
            StyleDefinitions::class.java, JAXB_CONTEXT_STYLE_DEFINITIONS!!, adapters
        )
        result.updateFrom(tmpStyleDefs)
        return result
    }

    /**
     * Save styles to XML
     */
    @JvmStatic
    fun saveStyleDefinitions(styleDefsFile: File, styleDefs: StyleDefinitions) {
        log.debug("Writing Style Definitions to file {} - overwriting {}", styleDefsFile, styleDefsFile.exists())
        saveJAXB(styleDefsFile, styleDefs, JAXB_CONTEXT_STYLE_DEFINITIONS!!)
    }

    /*
     * Load the shortcut definitions file
     */
    @SuppressFBWarnings(
        value = ["OBL_UNSATISFIED_OBLIGATION"],
        justification = "Try with resources will close stream, spotbugs issue493"
    )
    fun loadShortcutDefinitions(shortcutDefsFile: File): ShortcutDefinitions {
        log.debug("Loading Shortcut Definitions from file {}", shortcutDefsFile.absolutePath)
        if (!shortcutDefsFile.exists()) throw RuntimeException(
            ("Shortcut definitions file at " + shortcutDefsFile + " does not exist "
                    + shortcutDefsFile.absolutePath)
        )
        try {
            FileInputStream(shortcutDefsFile).use { input ->
                val documentBuilderFactory = DocumentBuilderFactory.newInstance()
                val documentBuilder = documentBuilderFactory.newDocumentBuilder()
                val document = documentBuilder.parse(input)

                val root = document.documentElement
                root.normalize()

                val defs = ShortcutDefinitions()
                val rootChildren = root.childNodes
                for (i in 0 until rootChildren.length) {
                    val shortcutRoot = rootChildren.item(i)
                    val shortcutRootName = shortcutRoot.nodeName
                    if (shortcutRoot.nodeType == Node.ELEMENT_NODE) {
                        if (shortcutRootName == "shortcut") defs.addShortcut(loadShortcut(shortcutRoot, i, defs))
                        else throw RuntimeException("Unknown field $shortcutRootName at shortcut index $i")
                    }
                }
                return defs
            }
        } catch (e: Exception) {
            // Reflection wrapped exception. Unwrap to cleanup giant stack trace
            val cleanCause = if (e is InvocationTargetException) e.cause!! else e
            throw UTDException("Could not load ShortcutDefinitions from file $shortcutDefsFile", cleanCause)
        }
    }

    private fun loadShortcut(shortcutRoot: Node, shortcutIndex: Int, shortcutDefs: ShortcutDefinitions): Shortcut {
        var id: String? = ""
        var keyCombo: String? = ""
        val shortcutRootChildren = shortcutRoot.childNodes
        for (j in 0 until shortcutRootChildren.length) {
            val shortcutElement = shortcutRootChildren.item(j)
            val shortcutElementName = shortcutElement.nodeName
            if (shortcutElement.nodeType != Node.ELEMENT_NODE)  // Not a tag, most likely a blank #text element
                continue

            val elementValue = shortcutElement.textContent
            if (shortcutElementName == SHORTCUT_ID_TAG) id = elementValue
            else if (shortcutElementName == "key-combination") keyCombo = elementValue
        }
        return Shortcut(id!!, keyCombo!!)
    }

    /**
     * Save shortcuts to XML file
     */
    fun saveShortcutDefinitions(shortcutDefsFile: File, shortcutDefs: ShortcutDefinitions) {
        log.debug(
            "Writing Shortcut Definitions to file {} - overwriting {}",
            shortcutDefsFile,
            shortcutDefsFile.exists()
        )
        try {
            FileOutputStream(shortcutDefsFile).use { output ->
                val documentBuilderFactory = DocumentBuilderFactory.newInstance()
                val documentBuilder = documentBuilderFactory.newDocumentBuilder()
                val document = documentBuilder.newDocument()

                val root = document.createElement("ShortcutDefinitions")
                for (curShortcut in shortcutDefs.shortcuts) {
                    root.appendChild(saveShortcut("shortcut", curShortcut, document, shortcutDefs))
                }
                document.appendChild(root)
                toXML(root, StreamResult(output))
            }
        } catch (e: Exception) {
            throw UTDException("Could not save Shortcut Definitions to file $shortcutDefsFile", e)
        }
    }

    private fun saveShortcut(
        rootName: String,
        curShortcut: Shortcut,
        document: Document,
        shortcutDefs: ShortcutDefinitions
    ): Element {
        val shortcutRoot = document.createElement(rootName)
        // add id
        val idElement = document.createElement(SHORTCUT_ID_TAG)
        idElement.textContent = curShortcut.id
        shortcutRoot.appendChild(idElement)
        // add key combination
        val keyCombinationElement = document.createElement(SHORTCUT_KEY_COMBINATION_TAG)
        keyCombinationElement.textContent = curShortcut.keyCombination
        shortcutRoot.appendChild(keyCombinationElement)

        return shortcutRoot
    }
}

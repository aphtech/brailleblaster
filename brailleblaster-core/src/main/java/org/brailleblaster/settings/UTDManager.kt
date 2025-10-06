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
package org.brailleblaster.settings

import ch.qos.logback.classic.Level
import com.google.common.base.CaseFormat
import nu.xom.*
import org.brailleblaster.BBIni
import org.brailleblaster.bbx.BBX
import org.brailleblaster.bbx.BBXUtils
import org.brailleblaster.bbx.BBXUtils.ListStyleData
import org.brailleblaster.bbx.fixers.NodeTreeSplitter.split
import org.brailleblaster.bbx.utd.BBXDynamicOptionStyleMap
import org.brailleblaster.bbx.utd.BBXStyleMap
import org.brailleblaster.logging.getLogLevel
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.settings.TableExceptions.generateExceptionsTables
import org.brailleblaster.settings.TableExceptions.getTranslationExceptionFile
import org.brailleblaster.utd.*
import org.brailleblaster.utd.Style.StyleOption
import org.brailleblaster.utd.actions.IAction
import org.brailleblaster.utd.config.DocumentUTDConfig
import org.brailleblaster.utd.config.StyleDefinitions
import org.brailleblaster.utd.config.UTDConfig
import org.brailleblaster.utd.exceptions.NodeException
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.mwhapples.jlouis.LogLevels
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Paths
import java.util.function.Consumer
import java.util.stream.Stream

/**
 * UTD settings, translation, and applying styles and actions management all in
 * mostly self-contained place
 */
class UTDManager @JvmOverloads constructor(styleDefs: StyleDefinitions = loadStyleDefinitions(preferredFormatStandard)) {
  val engine: BBUTDTranslationEngine

  // Doc specific settings
  var brailleStandard: String =
    BBIni.propertyFileManager.getProperty(USER_SETTINGS_BRAILLE_STANDARD, "UEB")
    private set

  var predominantQuoteSetting = PredominantQuoteSetting.findValue(
    BBIni.propertyFileManager.getProperty(USER_SETTINGS_PREDOMINANT_QUOTE, ""))
    ?: PredominantQuoteSetting.DEFAULT_SETTING

  /**
   * The style definitions we started with, used for resetting the
   * StyleDefinitions objects
   *
   * Was previously final, but needs to be altered when the user changes the formatting standard. Should not be altered in other cases.
   */
  private val origStyleDefs: List<Style>
  private var bookTypeStyleMap: IStyleMap? = null
  var overrideStyleMap: IStyleMap? = null
  var bbxStyleMap: BBXStyleMap? = null
  var bbxOptionStyleMap: BBXDynamicOptionStyleMap? = null
  var bookTypeActionMap: IActionMap? = null
  var overrideActionMap: IActionMap? = null
  var mathMap: IActionMap? = null

  init {
    engine = newEngine()
    val translator = engine.brailleTranslator
    log.debug("Initting engine")

    // Set liblouis table paths if not set by user
    if (translator.dataPath == null) {
      // The JarResolver can handle either pointing at the actual
      // directory containing the files or the directory containing
      // liblouis/tables/.
      // It is recommended to point to the actual directory itself as this
      // is checked first and so will be slightly quicker.
      val programTablesPath = BBIni.programDataPath.resolve(Paths.get("org", "mwhapples", "jlouis", "tables")).toString()
      translator.dataPath = BBIni.userProgramDataPath.toString() + "," + programTablesPath
    }
    log.debug("Liblouis data path: " + translator.dataPath)
    // Style Defs need to be set for making the StyleMap
    origStyleDefs = styleDefs.styles.toList()
    engine.styleDefinitions = styleDefs
    when (getLogLevel()) {
      Level.ERROR -> {
        translator.setLogLevel(LogLevels.ERROR)
      }
      Level.WARN -> {
        translator.setLogLevel(LogLevels.WARNING)
      }
      Level.INFO -> {
        translator.setLogLevel(LogLevels.INFO)
      }
      Level.DEBUG -> {
        translator.setLogLevel(LogLevels.DEBUG)
      }
      else -> {
        translator.setLogLevel(LogLevels.ALL)
      }
    }
  }

  private fun newEngine(): BBUTDTranslationEngine {
    return BBUTDTranslationEngine()
  }

  fun loadEngineFromDoc(doc: Document, bookType: String) {
    val actionMap = UTDConfig.loadActions(BBIni.loadAutoProgramDataFile("utd", "$bookType.actionMap.xml"))!!
    log.debug("Loaded {} entries", actionMap.size)
    val styleMap = UTDConfig.loadStyle(
      BBIni.loadAutoProgramDataFile("utd", "$bookType.styleMap.xml"),
      engine.styleDefinitions
    )
    val mathMap = UTDConfig.loadActions(BBIni.loadAutoProgramDataFile("utd", "mathml.actionMap.xml"))
    loadEngineFromDoc(doc, actionMap, styleMap, mathMap)
  }

  private fun loadEngineFromDoc(doc: Document, actionMap: ActionMap, styleMap: StyleMap?, mathMap: ActionMap?) {
    log.debug("Loading engine")
    bookTypeActionMap = actionMap
    bookTypeStyleMap = styleMap
    this.mathMap = mathMap
    bbxStyleMap = BBXStyleMap(engine.styleDefinitions)
    bbxOptionStyleMap = BBXDynamicOptionStyleMap(engine.styleDefinitions) { engine.styleMap }

    // --- PageSettings ---
    val docPageSettings = DocumentUTDConfig.NIMAS.loadPageSettings(doc)
    val pageSettings = if (docPageSettings == null) {
      val settingsFile = BBIni.loadAutoProgramDataFile(UTD_FOLDER, PAGE_SETTINGS_NAME)
      log.debug("Document doesn't have pageSettings, loading defaults at {}", settingsFile.absolutePath)
      UTDConfig.loadPageSettings(settingsFile)
    } else {
      log.debug("Loading document specific pageSettings")
      docPageSettings
    }
    engine.pageSettings = pageSettings

    // --- BrailleSettings ---
    // User can only set the braille standard they are using
    brailleStandard = DocumentUTDConfig.NIMAS.getSetting(doc, USER_SETTINGS_BRAILLE_STANDARD) ?: brailleStandard
    predominantQuoteSetting =
      PredominantQuoteSetting.findValue(DocumentUTDConfig.NIMAS.getSetting(doc, USER_SETTINGS_PREDOMINANT_QUOTE) ?: "")
        ?: predominantQuoteSetting
    applyBrailleStandard(brailleStandard)

    // ---- Style/Action Maps ----
    log.debug("Loaded {} entries", actionMap.size)
    overrideActionMap = OverrideMap.generateOverrideActionMap(actionMap)
    overrideStyleMap = OverrideMap.generateOverrideStyleMap(engine.styleDefinitions)
    reloadMapsFromDoc()
    benchmarkCallback.accept(engine)
  }

  fun reloadMapsFromDoc() {
    val startTime = System.currentTimeMillis()
    // ----- Reset styleDefs -----
    // Reset back to the styles from the disk
    // Don't change the StyleDefinitions ref as this it's saved in a bunch
    // of places
    val styleDefs = engine.styleDefinitions
    styleDefs.clear()
    styleDefs.addStyles(origStyleDefs)

    // Extend default action/style maps with doc specific ones
    val newActionMap = ActionMultiMap()
    newActionMap.maps = mutableListOf(overrideActionMap, bookTypeActionMap, mathMap)
    engine.actionMap = newActionMap
    val newStyleMap = StyleMultiMap(engine.styleDefinitions.defaultStyle ?: Style())
    newStyleMap.maps = mutableListOf(bbxOptionStyleMap, overrideStyleMap, bbxStyleMap, bookTypeStyleMap)
    engine.styleMap = newStyleMap

    // Update style defs with document style map entries
    for (curDocStyleDef in bbxOptionStyleMap!!.generatedStyles) {
      styleDefs.addStyle(curDocStyleDef)
    }
    totalMilliLoad += System.currentTimeMillis() - startTime
  }

  fun testSetup() {
    generateExceptionsTables(engine, true, brailleStandard)
  }

  /**
   * Apply given action
   *
   * @param action
   * @param element
   */
  fun applyAction(action: IAction?, element: Element) {
    if (element.document == null) {
      throw NodeException("Element must be attached to document", element)
    }
    applyAction(action, element, element.document)
  }

  fun applyAction(action: IAction?, element: Element?, doc: Document?) {
    if (action == null) {
      throw NullPointerException("action")
    }
    if (element == null) {
      throw NullPointerException("element")
    }
    if (doc == null) {
      throw NullPointerException("doc")
    }
    require(!(element.document != null && element.document !== doc))
      { "Element is attached to different document than given" }
    val actionName = action.javaClass.simpleName
    if (overrideActionMap!!.values.any { it.javaClass.simpleName == actionName }) {
      // Use simpler override style map
      BBX._ATTRIB_OVERRIDE_ACTION[element] = actionName
      log.debug("Applied override action {} to {}", action,
          XMLHandler.toXMLStartTag(element)
      )
    } else {
      throw RuntimeException("Unhandlable action (does it take fields?) $action")
    }

    // Sanity check
    val lookedUpAction = engine.actionMap.findValueOrDefault(element)
    if (lookedUpAction != action) {
      throw NodeException("Expected element action to be $action, got $lookedUpAction", element)
    }
  }

  /**
   *
   * @param element
   * @return The passed in element
   */
  fun removeOverrideAction(element: Element): Element {
    if (element.document == null) {
      throw NodeException("Element must be attached to document", element)
    }
    val attrib = BBX._ATTRIB_OVERRIDE_ACTION.getAttribute(element)
    attrib?.detach()
    return element
  }

  /**
   * Apply style to given element, must be attached to document
   *
   * @see .applyStyle
   * @param style
   * @param element
   */
  fun applyStyle(style: Style?, element: Element) {
    if (element.document == null) {
      throw NodeException("Element must be attached to document", element)
    }
    applyStyle(style, element, element.document)
  }

  /**
   * Apply style to node that might not be attached to the given document
   *
   * @param style
   * @param element
   * @param doc
   */

  //Fundamental problem is I'm not sure how to start messing with this. General unfamiliarity with nodified XML -
  // I intentionally sidestepped that problem in Dots123 for the same reason.
  @JvmOverloads
  fun applyStyle(style: Style?, element: Element?, doc: Document?, stripStyle: Boolean = true) {
    if (style == null) {
      throw NullPointerException("style")
    }
    if (element == null) {
      throw NullPointerException("element")
    }
    if (doc == null) {
      throw NullPointerException("doc")
    }
    require(!(element.document != null && element.document !== doc)) {
      "Element is attached to different document than given"
    }
    if (stripStyle) {
      BBXUtils.stripStyle(element, this)
    }
    val itemStyleData = BBXUtils.parseListStyle(style.name)
    if (itemStyleData != null) {
      //println("UTDMgr.ApplyStyle style: ${style.name} itemStyleData: ${itemStyleData.toString()}")
      BBX.BLOCK.assertIsA(element)
      if (itemStyleData.listType == null) {
        //Margin style
        BBX.BLOCK.MARGIN.set(element)
        BBX.BLOCK.MARGIN.ATTRIB_MARGIN_TYPE[element] = itemStyleData.marginType
        BBX.BLOCK.MARGIN.ATTRIB_INDENT[element] = itemStyleData.indentLevel
        BBX.BLOCK.MARGIN.ATTRIB_RUNOVER[element] = itemStyleData.runoverLevel
      }
      else {
        //println("ListType is not null")
        val parentList =
          XMLHandler.ancestorVisitorElement(element) { node: Element? -> BBX.CONTAINER.LIST.isA(node) }
        // For RT 5752 added getPreviousBBXSiblingNode and getNextBBXSiblingNode
        val previousSiblingNode = getPreviousBBXSiblingNode(element)
        val nextSiblingNode = getNextBBXSiblingNode(element)
        var wrapperList: Element
        if (parentList != null) {
          //println("ParentList is not null")
          if (isCompatibleList(parentList, itemStyleData)) {
            //println("parentList is compatible with itemStyleData")
            wrapperList = parentList
          }
          else {
            //println("parentList is NOT compatible with itemStyleData")
            // check for potential already split list from a previously applied-before-reformat style
            val previousSiblingList = XMLHandler.previousSiblingNode(parentList)
            val nextSiblingList = XMLHandler.nextSiblingNode(parentList)
            val isNextSiblingList = (nextSiblingList is Element
                && BBX.PreFormatterMarker.LIST_SPLIT.has(nextSiblingList))
            val isPreviousSiblingList = (previousSiblingList is Element
                && BBX.PreFormatterMarker.LIST_SPLIT.has(previousSiblingList))
            if (isPreviousSiblingList || isNextSiblingList) {
              if (element.parent.childCount == 1) {
                element.parent.detach()
              }
              element.detach()
              wrapperList =
                (if (isPreviousSiblingList) previousSiblingList else nextSiblingList) as Element
              wrapperList.appendChild(element)
            }
            else {
              // must split the list ourselves
              log.debug("UTDMgr splitting list")
              //println("Splitting list")
              wrapperList = BBX.CONTAINER.LIST.create(itemStyleData.listType)
              BBX.PreFormatterMarker.ATTRIB_PRE_FORMATTER_MARKER[wrapperList] = BBX.PreFormatterMarker.LIST_SPLIT
              split(parentList, element)
              element.parent.replaceChild(element, wrapperList)
              wrapperList.appendChild(element)
              BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL[wrapperList] = itemStyleData.runoverLevel
            }
          }
        }
        else if (previousSiblingNode != null && BBX.CONTAINER.LIST.isA(previousSiblingNode)
          && BBX.CONTAINER.LIST.ATTRIB_LIST_TYPE[previousSiblingNode as Element?] == itemStyleData.listType
          && isCompatibleList(previousSiblingNode, itemStyleData))
        {
          element.detach()
          wrapperList = previousSiblingNode
          wrapperList.appendChild(element)
          if (nextSiblingNode != null && BBX.CONTAINER.LIST.isA(nextSiblingNode)
            && BBX.CONTAINER.LIST.ATTRIB_LIST_TYPE[nextSiblingNode as Element?] == itemStyleData.listType
            && isCompatibleList(nextSiblingNode, itemStyleData))
          {
            wrapperList = mergeContainers(wrapperList, nextSiblingNode)
          }
        }
        else if (nextSiblingNode != null && BBX.CONTAINER.LIST.isA(nextSiblingNode)
          && BBX.CONTAINER.LIST.ATTRIB_LIST_TYPE[nextSiblingNode as Element?] == itemStyleData.listType
          && isCompatibleList(nextSiblingNode, itemStyleData))
        {
          element.detach()
          wrapperList = nextSiblingNode
          wrapperList.insertChild(element, 0)
        }
        else {
          wrapperList = BBX.CONTAINER.LIST.create(itemStyleData.listType)
          element.parent.replaceChild(element, wrapperList)
          wrapperList.appendChild(element)
          BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL[wrapperList] = itemStyleData.runoverLevel
        }

        if (BBX.CONTAINER.LIST.ATTRIB_LIST_TYPE[wrapperList] == BBX.ListType.POEM_LINE_GROUP) {
          val ancestorPoem = XMLHandler.ancestorVisitorElement(wrapperList) { e: Element? ->
            BBX.CONTAINER.LIST.ATTRIB_LIST_TYPE.getOptional(e).orElse(null) == BBX.ListType.POEM
          }
          if (ancestorPoem != null) {
            wrapperList = ancestorPoem
          }
        }

        //apply style - be sure to comment this out once replace() is working with lists...
        log.debug("WrapperList XML: \n" + wrapperList.toXML() +
            "\nWlist: " + BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL[wrapperList] +
            " runover: " + itemStyleData.runoverLevel)


        if (BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL[wrapperList] < itemStyleData.runoverLevel) {
          BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL[wrapperList] = itemStyleData.runoverLevel
        }

        BBX.transform(element, BBX.BLOCK.LIST_ITEM)
        BBX.BLOCK.LIST_ITEM.ATTRIB_ITEM_LEVEL[element] = itemStyleData.indentLevel
      }
    }
    else if (overrideStyleMap!!.values.contains(style)) {
      // Use simpler override style map
      BBX._ATTRIB_OVERRIDE_STYLE[element] = style.name

      // Prevent breaking BBX when changing from the given style based on type
      if (BBX.BLOCK.isA(element)) {
        BBXUtils.stripStyleExceptOverrideStyle(element)
        BBX.transform(element, BBX.BLOCK.STYLE)
      }
      if (XMLHandler.ancestorElementIs(element) {
            node: Element? -> BBX.CONTAINER.LIST.isA(node) }) {
        BBXUtils.stripStyleExceptOverrideStyle(element)
        BBXUtils.checkListStyle(XMLHandler.ancestorVisitorElement(element) {
            node: Element? -> BBX.CONTAINER.LIST.isA(node) }, element, this)
      }
    }
    else {
      throw RuntimeException("Unhandled style " + style.name)
    }

    if (element.document == null) {
      throw NodeException("Element missing from document? " + XMLHandler.toXMLSimple(
          element
      ), doc)
    }

    // Sanity check
    val lookedUpStyle = engine.styleMap.findValueOrDefault(element)
    if (lookedUpStyle != style && bbxOptionStyleMap!!.getStyleOptions(element).isEmpty()) {
      throw NodeException(
        "Expected element style to be " + style.name
            + ", got " + lookedUpStyle.name
            + " . Detail: expected " + style
            + ", got " + lookedUpStyle,
        element
      )
    }
  }

  // For RT 5752 added getPreviousBBXSiblingNode and getNextBBXSiblingNode
  private fun getPreviousBBXSiblingNode(element: Node): Node? {
    var sibling: Node = element
    var isBBX = false
    while (!isBBX) {
      sibling = XMLHandler.previousSiblingNode(sibling) ?: return null
      if (sibling is Element) {
        if (sibling.namespacePrefix.equals("utd", ignoreCase = true)) {
          continue
        }
        isBBX = true
      }
    }
    return sibling
  }

  private fun getNextBBXSiblingNode(element: Node): Node? {
    var sibling: Node = element
    var isBBX = false
    while (!isBBX) {
      sibling = XMLHandler.nextSiblingNode(sibling) ?: return null
      if (sibling is Element) {
        if (sibling.namespacePrefix.equals("utd", ignoreCase = true)) {
          continue
        }
        isBBX = true
      }
    }
    return sibling
  }

  private fun mergeContainers(containerOne: Element, containerTwo: Element): Element {
    while (containerOne.childCount > 0) {
      containerTwo.insertChild(containerOne.removeChild(containerOne.childCount - 1), 0)
    }
    containerOne.detach()
    return containerTwo
  }

  /**
   *
   * @param element
   * @return The passed in element
   */
  fun removeOverrideStyle(element: Element): Element {
    if (element.document == null) {
      throw NodeException("Element must be attached to document " + element.toXML(), element)
    }
    val attrib = BBX._ATTRIB_OVERRIDE_STYLE.getAttribute(element)
    attrib?.detach()
    return element
  }

  fun applyStyleWithOption(curStyle: Style, styleOption: StyleOption, value: Any, element: Element): Style {
    return when (styleOption) {
      StyleOption.GUIDE_WORDS -> {
        element.addAttribute(Attribute("guideWords", java.lang.Boolean.toString((value as Boolean))))
        curStyle
      }

      StyleOption.PAGE_SIDE -> {
        val attribName = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, styleOption.name)
        element.addAttribute(Attribute(attribName, value.toString()))
        curStyle
      }

      StyleOption.LINES_BEFORE, StyleOption.LINES_AFTER -> {
        //This will cause you to have an extra blank page when you do a page-break--
        //An action which uses override styles but has a diff intention than the newPagesBefore style available in the style options
//			case NEW_PAGES_AFTER:
//			case NEW_PAGES_BEFORE:
        curStyle
      }

      else -> {
          val baseStyle = getBaseStyle(curStyle)
          log.debug("Got base style {} frorm {}", baseStyle!!.name, curStyle.name)

          // Extend default style if possible instead of changing to a BBX.BLOCK.STYLE with overrideStyle
          if (getBaseStyle(engine.getStyle(element) as Style?) != baseStyle) {
            applyStyle(baseStyle, element, element.document,  /*keep styling*/false)
          }
          BBXDynamicOptionStyleMap.setStyleOptionAttrib(element, styleOption, value)
          val newStyle = engine.getStyle(element) as Style?
          if (newStyle === baseStyle) {
            throw NodeException(
              "Style option $styleOption with value $value has no affect on style ${baseStyle.name}. Full: $newStyle",
              element
            )
          }
          newStyle!!
      }
    }
  }

  /**
   * @see .getBaseStyle
   */
  fun getBaseStyle(styleName: String, node: Node?): String {
    //Wrap with if to avoid relatively expensive getStyle()
    return if (styleName.startsWith(DOCUMENT_STYLE_NAME_PREFIX)) {
      getBaseStyle(engine.getStyle(node!!) as Style?)!!.name
    } else styleName
  }

  /**
   * WARNING: Must call m.refresh() after this as the whole translation will
   * change
   */
  fun updateBrailleStandard(doc: Document, std: String) {
    log.debug("Changing braille standard from {} to {}", brailleStandard, std)
    DocumentUTDConfig.NIMAS.setSetting(doc, USER_SETTINGS_BRAILLE_STANDARD, std)
    brailleStandard = std
    applyBrailleStandard(std)
  }

  fun updatePredominantQuote(doc: Document, pq: PredominantQuoteSetting) {
    DocumentUTDConfig.NIMAS.setSetting(doc, USER_SETTINGS_PREDOMINANT_QUOTE, pq.name)
    predominantQuoteSetting = pq
    applyBrailleStandard(brailleStandard)
  }

  fun applyBrailleStandard(std: String) {
    engine.brailleSettings = UTDConfig.loadBrailleSettings(
      BBIni.loadAutoProgramDataFileOrNull(
        UTD_FOLDER,
        std + BRAILLE_SETTINGS_NAME
      ) ?: BBIni.loadAutoProgramDataFileOrNull(UTD_FOLDER, "legacy", std + BRAILLE_SETTINGS_NAME) ?: throw RuntimeException("Braille standard file not found.")
    )
    val quotesTable = BBIni.programDataPath.resolve(Paths.get(
      "quotesTables",
      "$brailleStandard-quotes-${predominantQuoteSetting.name.lowercase()}.cti"
    )).toFile()
    if ((quotesTable.exists())) {
      engine.brailleSettings.mainTranslationTable =
        "${quotesTable.absolutePath},${engine.brailleSettings.mainTranslationTable}"
    }
    generateExceptionsTables(engine, false, brailleStandard)
  }

  val userShortcutDefinitionsFile: File
    get() = BBIni.userProgramDataPath.resolve(Paths.get(UTD_FOLDER, SHORTCUT_DEFS_NAME)).toFile()

  fun hasStyle(node: Node?, styleName: String): Boolean {
    val curStyle = engine.getStyle(node!!)
    return curStyle != null && isStyle(curStyle as Style?, styleName)
  }

  class BBUTDTranslationEngine : UTDTranslationEngine() {
    @JvmField
    var expectedTranslate = false
    override fun translate(doc: Node): Nodes {
      if (!expectedTranslate) {
        fail()
      }
      return super.translate(doc)
    }

    override fun translateDocument(doc: Document): Document {
      if (!expectedTranslate) {
        fail()
      }
      return super.translateDocument(doc)
    }

    override fun format(nodes: Node): Document {
      val origDoc = nodes.document
      return try {
        super.format(nodes)
      } catch (e: Exception) {
        val failNode = if (nodes.document != null) nodes else origDoc
        throw NodeException(
          "Failed to format " + if (nodes.document != null) "(orig node)" else "(orig doc)", failNode, e
        )
      }
    }

    fun fail() {
      throw UnsupportedOperationException("Unexpected call to translate, should most likely use ModifyEvent")
    }
  }

  val exceptionsTableFile: File?
    get() = getTranslationExceptionFile(engine.brailleSettings, brailleStandard)

  companion object {
    /**
     * See Benchmark class, this is the only way to (easily) hook into this
     */
    @JvmField
    var benchmarkCallback = Consumer { _: UTDTranslationEngine? -> }
    private val log = LoggerFactory.getLogger(UTDManager::class.java)
    const val UTD_FOLDER = "utd"
    const val STYLE_DEFS_NAME = "styleDefs.xml"
    private const val SHORTCUT_DEFS_NAME = "shortcutDefs.xml"
    const val BRAILLE_SETTINGS_NAME = ".brailleSettings.xml"
    private const val PAGE_SETTINGS_NAME = "pageSettings.xml"

    // Override settings
    const val USER_SETTINGS_BRAILLE_STANDARD = "brailleStandard"
    const val USER_SETTINGS_PREDOMINANT_QUOTE = "predominantQuote"
    const val USER_SETTINGS_FORMAT_STANDARD = "formatStandard"
    const val DOCUMENT_STYLE_NAME_PREFIX = "bbs-"
    var totalMilliLoad: Long = 0
    private fun isCompatibleList(list: Element, itemStyleData: ListStyleData): Boolean {
      val actualList = if (BBX.ListType.POEM_LINE_GROUP.isA(list) && !BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL.has(list)) {
        XMLHandler.ancestorVisitorElement(list.parent) { node: Element? -> BBX.CONTAINER.LIST.isA(node) }
      } else list
      return (BBX.CONTAINER.LIST.ATTRIB_LIST_LEVEL[actualList] <= itemStyleData.runoverLevel
          && BBX.CONTAINER.LIST.ATTRIB_LIST_TYPE.getOptional(actualList).orElse(null) == itemStyleData.listType)
    }

    @JvmStatic
    fun getBaseStyle(style: Style?): Style? {
      return Stream.iterate(
        style,
        { baseStyle: Style? -> baseStyle!!.name.startsWith(DOCUMENT_STYLE_NAME_PREFIX) }) { baseStyle: Style? -> baseStyle!!.baseStyle }
        .findFirst().orElse(style)
    }

    @JvmStatic
    val userPageSettingsFile: File
      get() = BBIni.userProgramDataPath.resolve(Paths.get(UTD_FOLDER, PAGE_SETTINGS_NAME)).toFile()
    val userBrailleSettingsFile: File
      get() = BBIni.userProgramDataPath.resolve(Paths.get(UTD_FOLDER, BRAILLE_SETTINGS_NAME)).toFile()
    val userStyleDefinitionsFile: File
      get() = BBIni.userProgramDataPath.resolve(Paths.get(UTD_FOLDER, STYLE_DEFS_NAME)).toFile()

    @JvmStatic
    val preferredFormatStandard: String
      get() = BBIni.propertyFileManager.getProperty(USER_SETTINGS_FORMAT_STANDARD, "BANA")

    /**
     * Check if given style is or has a parent with the given style name
     *
     * @param style
     * @param styleName
     * @return
     */
    @JvmStatic
    fun isStyle(style: Style?, styleName: String): Boolean {
      var curStyle = style
      while (curStyle != null) {
        if (curStyle.name == styleName) {
          return true
        }
        curStyle = curStyle.baseStyle
      }
      return false
    }

    @JvmStatic
    fun hasUtdStyleTag(element: Element, style: String): Boolean {
      return element.getAttributeValue("utd-style") != null && element.getAttributeValue("utd-style") == style
          || element.getAttributeValue("overrideStyle") != null && element.getAttributeValue("overrideStyle") == style
    }

    fun hasUtdActionTag(element: Element, style: String): Boolean {
      return element.getAttributeValue("utd-action") != null && element.getAttributeValue("utd-action") == style
          || element.getAttributeValue("overrideAction") != null && element.getAttributeValue("overrideAction") == style
    }

    /**
     * @param element
     * @return string of next print page number
     */
    @JvmStatic
    fun getNextPageNum(element: Element?): String {
      val pagenum = XMLHandler.followingWithSelfVisitor(
        element
      ) { n: Node -> n is Text && n.parent != null && BBX.BLOCK.PAGE_NUM.isA(n.parent) }
      return if (pagenum != null) {
        pagenum.value
      } else "Page Not Found"
    }

    @JvmStatic
    fun loadStyleDefinitions(formatStandard: String?): StyleDefinitions {
      return UTDConfig.loadStyleDefinitions(
        BBIni.loadAutoProgramDataFile(
          UTD_FOLDER, String.format("%s.%s", formatStandard, STYLE_DEFS_NAME)
        )
      )
    }

    @JvmStatic
    fun getCellsPerLine(m: Manager): Int {
      val pageSettings = m.document.engine.pageSettings
      val brailleCell = m.document.engine.brailleSettings.cellType
      val middle = pageSettings.drawableWidth
      return brailleCell.getCellsForWidth(middle.toBigDecimal())
    }
  }
}
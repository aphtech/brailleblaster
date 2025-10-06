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
package org.brailleblaster.math.template

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.BBIni
import org.brailleblaster.bbx.BBX
import org.brailleblaster.math.mathml.MathModule
import org.brailleblaster.math.spatial.*
import org.brailleblaster.math.spatial.SpatialMathEnum.Passage
import org.brailleblaster.math.spatial.SpatialMathEnum.SpatialMathContainers
import org.brailleblaster.math.template.TemplateOperand.TemplateOperandBuilder
import org.brailleblaster.perspectives.braille.mapping.elements.LineBreakElement
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.perspectives.braille.mapping.elements.WhiteSpaceElement
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.wordprocessor.WPManager

class Template : ISpatialMathContainer {
  override var settings = TemplateSettings()
  var ui = TemplateUISettings()
  var operands = ArrayList<TemplateOperand>()
  var solutions = ArrayList<TemplateOperand>()
  override var lines = ArrayList<Line>()
  var identifier: MathText = MathText()
  override var widestLine = 0
  override var widget = TemplateWidget()
  override var blank = false

  init {
    loadSettingsFromFile()
    settings = TemplateSettings()
  }

  override fun toString(): String {
    return lines.toString()
  }

  override fun format() {
    val template = if (MathModule.isNemeth) {
      when (settings.type) {
          SpatialMathEnum.TemplateType.SIMPLE_ENUM -> {
            NemethTemplate().format(this)
          }
          SpatialMathEnum.TemplateType.RADICAL_ENUM -> {
            NemethRadical().format(this)
          }
          else -> {
            NemethFractionTemplate().format(this)
          }
      }
    } else {
      when (settings.type) {
          SpatialMathEnum.TemplateType.SIMPLE_ENUM -> {
            UebTemplate().format(this)
          }
          SpatialMathEnum.TemplateType.RADICAL_ENUM -> {
            UebRadical().format(this)
          }
          else -> {
            UebFractionTemplate().format(this)
          }
      }
    }
    addIdentifier(template)
  }

  val brailleOperator: String
    get() = MathModule.translateMathPrint(settings.operator.symbol)

  var printOperands: List<String>
    get() = operands.flatMap {
        if (settings.type == SpatialMathEnum.TemplateType.FRACTION_ENUM) {
          listOf(it.whole.print, it.numerator.print, it.denominator.print)
        } else {
          listOf(it.whole.print)
        }
      }
    set(value) {
      operands.clear()
      if (settings.type == SpatialMathEnum.TemplateType.FRACTION_ENUM) {
        var i = 0
        while (i < value.size) {
          var whole = MathText(print=value[i],
            braille=MathModule.translateAsciiMath(value[i]))
          if (whole.print == "") whole = MathText(print="", braille="")
          var numerator = MathText(print=value[i + 1],braille=MathModule.translateAsciiMath(value[i + 1]))
          if (numerator.print == "") numerator = MathText(print="", braille="")
          var denominator = MathText(print=value[i + 2],
            braille=MathModule.translateAsciiMath(value[i + 2]))
          if (denominator.print == "") denominator = MathText(print="", braille="")
          operands.add(
            TemplateOperandBuilder().whole(whole).numerator(numerator)
              .denominator(denominator).build()
          )
          i += 3
        }
      } else {
        for (i in value.indices) {
          var whole = MathText(print=value[i],
            braille=MathModule.translateAsciiMath(value[i]))
          if (whole.print == "") whole = MathText(print="", braille="")
          operands.add(TemplateOperandBuilder().whole(whole).build())
        }
      }
    }
  val brailleOperands: List<String>
    get() = operands.flatMap {
        if (settings.type == SpatialMathEnum.TemplateType.FRACTION_ENUM) {
          listOf(it.whole.braille, it.numerator.braille, it.denominator.braille)
        } else {
          listOf(it.whole.braille)
        }
      }

  var printSolutions: List<String>
    get() = solutions.flatMap {
        if (settings.type == SpatialMathEnum.TemplateType.FRACTION_ENUM) {
          listOf(it.whole.print, it.numerator.print, it.denominator.print)
        } else {
          listOf(it.whole.print)
        }
      }
    set(value) {
      solutions.clear()
      if (settings.type == SpatialMathEnum.TemplateType.FRACTION_ENUM) {
        var i = 0
        while (i < value.size) {
          val whole = MathText(print=value[i],
            braille=MathModule.translateAsciiMath(value[i]))
          val numerator = MathText(print=value[i + 1],
            braille=MathModule.translateAsciiMath(value[i + 1]))
          val denominator = MathText(print=value[i + 2],
            braille=MathModule.translateAsciiMath(value[i + 2]))
          solutions.add(
            TemplateOperandBuilder().whole(whole).numerator(numerator).denominator(denominator).build()
          )
          i += 3
        }
      } else {
        for (i in value.indices) {
          if (value[i].isNotBlank()) {
            val whole = MathText(
              print = value[i],
              braille = MathModule.translateAsciiMath(value[i])
            )
            solutions.add(TemplateOperandBuilder().whole(whole).build())
          }
        }
      }
    }

  val brailleSolutions: List<String>
    get() {
      return solutions.flatMap {
        if (settings.type == SpatialMathEnum.TemplateType.FRACTION_ENUM) {
          listOf(it.whole.braille, it.numerator.braille, it.denominator.braille)
        } else {
          listOf(it.whole.braille)
        }
      }
    }

  override fun saveSettings() {
    BBIni.propertyFileManager.save(
      TemplateConstants.USER_SETTINGS_TEMPLATE_TYPE,
      settings.type.name
    )
    BBIni.propertyFileManager.save(
      TemplateConstants.USER_SETTINGS_TEMPLATE_OPERATOR,
      settings.operator.name
    )
    BBIni.propertyFileManager.save(
      TemplateConstants.USER_SETTINGS_TEMPLATE_PASSAGE,
      settings.passage.name
    )
    BBIni.propertyFileManager.save(
      TemplateConstants.USER_SETTINGS_OPERANDS,
      settings.operands.toString()
    )
    BBIni.propertyFileManager.save(
      TemplateConstants.USER_SETTINGS_SOLUTION,
      settings.solutions.toString()
    )
    BBIni.propertyFileManager.save(
      TemplateConstants.USER_SETTINGS_VERTICAL,
      settings.isStraightRadicalSymbol.toString()
    )
    BBIni.propertyFileManager.save(
      TemplateConstants.USER_SETTINGS_IDENTIFIER_TRANSLATION,
      settings.isTranslateIdentifierAsMath.toString()
    )
    BBIni.propertyFileManager.save(
      TemplateConstants.USER_SETTINGS_LINEAR,
      settings.isLinear.toString()
    )
  }

  /*
	 * Operator, operands, solution, type, straight vertical, linear, identifier
	 * translation, passage
	 */
  override fun loadSettingsFromFile() {
    val type = BBIni.propertyFileManager.getProperty(
      TemplateConstants.USER_SETTINGS_TEMPLATE_TYPE,
      SpatialMathEnum.TemplateType.SIMPLE_ENUM.name
    )
    val operator = BBIni.propertyFileManager.getProperty(
      TemplateConstants.USER_SETTINGS_TEMPLATE_OPERATOR,
      SpatialMathEnum.OPERATOR.PLUS_ENUM.name
    )
    var passage = BBIni.propertyFileManager.getProperty(
      TemplateConstants.USER_SETTINGS_TEMPLATE_PASSAGE,
      Passage.NONE.name
    )
    settings.type = SpatialMathEnum.TemplateType.valueOf(type)
    settings.operator = SpatialMathEnum.OPERATOR.valueOf(operator)
    try {
      settings.passage = Passage.valueOf(passage)
    } catch (_: IllegalArgumentException) {
      passage = Passage.NONE.name
      BBIni.propertyFileManager.save(TemplateConstants.USER_SETTINGS_TEMPLATE_PASSAGE, passage)
      settings.passage = Passage.valueOf(passage)
    }
    val operands =
      BBIni.propertyFileManager.getProperty(TemplateConstants.USER_SETTINGS_OPERANDS, "2")
    val solution =
      BBIni.propertyFileManager.getProperty(TemplateConstants.USER_SETTINGS_SOLUTION, "0")
    val vertical =
      BBIni.propertyFileManager.getProperty(TemplateConstants.USER_SETTINGS_VERTICAL, "false")
    val identifierString =
      BBIni.propertyFileManager.getProperty(
        TemplateConstants.USER_SETTINGS_IDENTIFIER_TRANSLATION, "false"
      )

    var identifier = java.lang.Boolean.valueOf(identifierString)
    if (identifier && !MathModule.isNemeth) {
      identifier = false
    }
    val linear = BBIni.propertyFileManager.getProperty(TemplateConstants.USER_SETTINGS_LINEAR, "true")
    settings.isLinear = java.lang.Boolean.valueOf(linear)
    settings.operands = Integer.valueOf(operands)
    settings.solutions = Integer.valueOf(solution)
    settings.isStraightRadicalSymbol = java.lang.Boolean.valueOf(vertical)
    settings.isTranslateIdentifierAsMath = identifier
  }

  override val typeEnum: SpatialMathContainers = SpatialMathContainers.TEMPLATE

  override val json: ISpatialMathContainerJson
    get() = TemplateJson().containerToJson(this) as TemplateJson

  override fun preFormatChecks(): Boolean {
    // TODO Auto-generated method stub
    return true
  }

  fun hasMinus(): Boolean {
    // TODO Auto-generated method stub
    return false
  }

  companion object {
    private const val IDENTIFIER_BUFFER = 1
    fun isEmpty(template: Template): Boolean {
      return operandsBlank(template) && solutionsBlank(template) && identifierBlank(template)
    }

    private fun identifierBlank(template: Template): Boolean {
      return template.identifier.print.isBlank()
    }

    private fun solutionsBlank(template: Template): Boolean {
      for (i in template.printSolutions.indices) {
        if (template.printSolutions[i].isNotBlank()) {
          return false
        }
      }
      return true
    }

    fun operandsBlank(template: Template): Boolean {
      for (i in template.printOperands.indices) {
        if (template.printOperands[i].isNotBlank()) {
          return false
        }
      }
      return true
    }

    fun addIdentifier(template: Template) {
      if (template.identifier.braille.isEmpty()) {
        return
      }
      val l = Line()
      l.elements.add(l.getTextSegment(template.identifier.braille))
      var widestLine = 0
      for (k in template.lines.indices) {
        val lineLength = template.lines[k].toString().length
        if (lineLength > widestLine) {
          widestLine = lineLength
        }
      }
      l.elements.add(l.getWhitespaceSegment(widestLine + IDENTIFIER_BUFFER))
      template.lines.add(0, l)
      for (i in 1 until template.lines.size) {
        template.lines[i].elements.add(
          0, template.lines[i]
            .getWhitespaceSegment(template.identifier.braille.length + IDENTIFIER_BUFFER)
        )
      }
    }

    fun currentIsTemplate(): Boolean {
      val current: Node? = XMLHandler.ancestorVisitorElement(
        WPManager.getInstance().controller
          .simpleManager.currentCaret.node
      ) { node: Element? -> BBX.CONTAINER.TEMPLATE.isA(node) }
      val isWhitespace = WPManager.getInstance().controller.mapList
        .current is WhiteSpaceElement
      return !isWhitespace && current != null
    }

    fun getTemplateFromElement(myTemplateElement: Element?): Template {
      var templateElement = myTemplateElement
      var template = Template()
      if (BBX.CONTAINER.TEMPLATE.isA(templateElement)) {
        if (!BBX.CONTAINER.TEMPLATE.ATTRIB_VERSION.has(templateElement)) {
          templateElement = VersionConverter.convertTemplate(templateElement)
        }
        val json = BBX.CONTAINER.TEMPLATE.JSON_TEMPLATE[templateElement] as TemplateJson
        template = json.jsonToContainer() as Template
      }
      return template
    }

    fun middleTemplate(currentElement: TextMapElement): Boolean {
      if (currentElement.node == null || currentElement is LineBreakElement) {
        return false
      }
      val template =
        XMLHandler.ancestorVisitorElement(currentElement.node) { node: Element? -> BBX.CONTAINER.TEMPLATE.isA(node) }
      return template != null
    }

    fun isTemplate(node: Node?): Boolean {
      if (node == null || node.document == null) {
        return false
      }
      return XMLHandler.ancestorElementIs(node) { elm: Element? -> BBX.CONTAINER.TEMPLATE.isA(elm) }
    }

    fun getTemplateParent(node: Node): Element? {
      return XMLHandler.ancestorVisitorElement(node) { elm: Element -> BBX.CONTAINER.TEMPLATE.isA(elm) }
    }

    @JvmStatic
    fun initialize(node: Node): Element {
      val e = node as Element
      val t = getTemplateFromElement(e)
      t.format()
      val newElement = BBX.CONTAINER.TEMPLATE.create(t)
      try {
        SpatialMathBlock.format(newElement, t.lines)
      } catch (e1: MathFormattingException) {
        e1.printStackTrace()
      }
      e.parent.replaceChild(e, newElement)
      return newElement
    }

    fun translateIdentifier(string: String, template: Template): String {
      return if (template.settings.isTranslateIdentifierAsMath)
        MathModule.translateAsciiMath(string)
        else MathModule.translateMainPrint(string)
    }

    fun hasRemainder(t: Template): Boolean {
      return t.settings.type == SpatialMathEnum.TemplateType.RADICAL_ENUM
          && t.brailleSolutions.size > 1
          && t.brailleSolutions[1].isNotBlank()
    }
  }
}
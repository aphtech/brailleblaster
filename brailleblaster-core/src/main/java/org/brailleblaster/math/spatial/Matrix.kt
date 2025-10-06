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
package org.brailleblaster.math.spatial

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.BBIni
import org.brailleblaster.bbx.BBX
import org.brailleblaster.math.mathml.MathModule
import org.brailleblaster.math.spatial.MatrixConstants.BracketType
import org.brailleblaster.math.spatial.MatrixConstants.BracketType.Companion.getLeft
import org.brailleblaster.math.spatial.MatrixConstants.BracketType.Companion.getRight
import org.brailleblaster.math.spatial.MatrixConstants.Wide
import org.brailleblaster.math.spatial.SpatialMathBlock.format
import org.brailleblaster.math.spatial.SpatialMathEnum.Passage
import org.brailleblaster.math.spatial.SpatialMathEnum.SpatialMathContainers
import org.brailleblaster.math.spatial.SpatialMathEnum.Translation
import org.brailleblaster.math.spatial.VersionConverter.convertMatrix
import org.brailleblaster.perspectives.braille.mapping.elements.LineBreakElement
import org.brailleblaster.perspectives.braille.mapping.elements.TextMapElement
import org.brailleblaster.perspectives.braille.mapping.elements.WhiteSpaceElement
import org.brailleblaster.perspectives.braille.messages.Sender
import org.brailleblaster.perspectives.mvc.XMLNodeCaret
import org.brailleblaster.perspectives.mvc.events.ModifyEvent
import org.brailleblaster.perspectives.mvc.events.XMLCaretEvent
import org.brailleblaster.perspectives.mvc.modules.views.DebugModule
import org.brailleblaster.settings.UTDManager.Companion.getCellsPerLine
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.util.Notify
import org.brailleblaster.util.Notify.notify
import org.brailleblaster.wordprocessor.WPManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*


class Matrix : ISpatialMathContainer {
  override var settings: MatrixSettings = MatrixSettings()
  override val lines: MutableList<Line> = ArrayList()
  private val cells: MutableList<MathText> = ArrayList()
  override val widget: MatrixWidget = MatrixWidget()
  override var widestLine: Int = 0
  override var blank: Boolean = false

  init {
    loadSettingsFromFile()
  }

  override fun format() {
    lines.clear()
    setPrintCellsFromModel()
    val brailleCells = brailleCells
    if (needWide(brailleCells)) {
      if (Wide.BLOCK_BLANK == settings.wideType) {
        matrixLog("Formatting wide matrix, block blank")
        formatBlankBlock(brailleCells)
      } else {
        matrixLog("Formatting wide matrix, indent")
        try {
          formatIndent(brailleCells)
        } catch (_: RuntimeException) {
          notify(MatrixConstants.FORMAT_INDENT_TOO_WIDE_WARNING, Notify.ALERT_SHELL_NAME)
        }
      }
    } else {
      matrixLog("Formatting skinny matrix")
      formatSkinny(brailleCells)
    }
  }

  val brailleCells: ArrayList<String>
    get() {
      val brailleCells = ArrayList<String>()
      for (cell in cells) {
        brailleCells.add(cell.braille)
      }
      return brailleCells
    }

  val asciiCells: ArrayList<String>
    get() {
      val asciiCells = ArrayList<String>()
      for (cell in cells) {
        asciiCells.add(cell.print)
      }
      return asciiCells
    }

  private fun formatSkinny(asciiMath: ArrayList<String>) {
    for (i in 0 until settings.rows) {
      val l = Line()
      l.elements.add(l.getTextSegment(getLeft(settings.bracketType)))
      for (j in 0 until settings.cols) {
        val s = asciiMath[i * settings.cols + j]
        l.elements.add(l.getTextSegment(s))
        if (j != settings.cols - 1) {
          l.elements.add(
            l.getWhitespaceSegment(
              calculateWhitespace(s.length, getLongestSegmentInColumn(asciiMath, j))
            )
          )
        }
      }
      l.elements
        .add(l.getTextSegment(getRight(settings.bracketType)))
      lines.add(l)
    }
  }

  private fun formatIndent(asciiMath: ArrayList<String>) {
    val cellsAllowed = getCellsPerLine(WPManager.getInstance().controller)
    for (i in 0 until settings.cols) {
      for (j in 0 until settings.rows) {
        /*
         * 1,1 1,2 1,3 2,1 2,2 2,3 3,1 3,2 3,1 iterate (1,1),(2,1),(3,1)(1,2)...to
         * format the columns as rows
         */
        val l = Line()
        l.elements.add(l.getWhitespaceSegment(2 * i))
        if (i == 0) {
          l.elements.add(
            l.getTextSegment(getLeft(settings.bracketType))
          )
        }
        val s = asciiMath[i + settings.cols * j]
        l.elements.add(l.getTextSegment(s))
        if (i == settings.cols - 1) {
          l.elements.add(
            l.getTextSegment(getRight(settings.bracketType))
          )
        }
        if (l.length() > cellsAllowed) {
          throw RuntimeException("Matrix formatting indentation failed")
        }
        lines.add(l)
      }
    }
  }

  private fun formatBlankBlock(asciiMath: ArrayList<String>) {
    /*
   * Fit to the page; determine column width and divide math text into them (cells
   * per line - brackets - one cell between columns) / columns = width; justify
   * matrix cell text in the top left; repeat brackets on every line
   */
    val columns = settings.cols
    val bracketWidth = settings.bracketType.label.length
    val cellsAllowed = getCellsPerLine(WPManager.getInstance().controller)
    val cellWidth = (cellsAllowed - (bracketWidth - 2) - (columns - 1)) / columns
    for (i in 0 until settings.rows) {
      val rowElements = ArrayList<String>()
      for (j in 0 until settings.cols) {
        val s = asciiMath[i * settings.cols + j]
        rowElements.add(s)
      }
      while (!doneRenderingRow(rowElements)) {
        val l = Line()
        l.elements.add(l.getTextSegment(getLeft(settings.bracketType)))
        val s = makeLine(rowElements, cellWidth)
        l.elements.add(l.getTextSegment(s))
        l.elements.add(l.getTextSegment(getRight(settings.bracketType)))
        lines.add(l)
      }
      if (!MathModule.isNemeth) {
        //If not Nemeth, add extra blank line if not the next-to-last row
        if (i + 1 < settings.rows) {
          val l = Line()
          l.elements.add(l.getTextSegment(getLeft(settings.bracketType)))
          val s = makeLine(rowElements, cellWidth)
          l.elements.add(l.getTextSegment(s))
          l.elements.add(l.getTextSegment(getRight(settings.bracketType)))
          lines.add(l)
        }
      }

    }
  }

  private fun getLongestSegmentInColumn(asciiMath: ArrayList<String>, column: Int): Int {
    var longest = 0
    for (i in 0 until settings.rows) {
      val s = asciiMath[settings.cols * i + column]
      if (s.length > longest) {
        longest = s.length
      }
    }
    return longest
  }

  private fun needWide(asciiMath: ArrayList<String>): Boolean {
    val columns = settings.cols
    val rows = settings.rows
    val bracketWidth = settings.bracketType.label.length
    val cellsAllowed = getCellsPerLine(WPManager.getInstance().controller)
    val startingWidth = bracketWidth * 2
    var thisWidth = startingWidth
    for (i in 0 until rows) {
      for (j in 0 until columns) {
        val colString = asciiMath[columns * i + j]
        thisWidth += colString.length
      }
      if (thisWidth > cellsAllowed) {
        matrixLog("Too wide, cells are $thisWidth")
        return true
      }
      thisWidth = startingWidth
    }
    matrixLog("Cells are appropriate width, cells are $thisWidth")
    return false
  }

  fun setPrintCells(array: ArrayList<String?>) {
    cells.clear()
    for (s in array) {
      val mt = MathText(
        s!!,
        MathModule.translateMathPrint(s), false
      )
      cells.add(mt)
    }
  }

  fun setPrintCellsFromModel() {
    cells.clear()
    for (i in settings.model.indices) {
      for (j in settings.model[i].indices) {
        val s = settings.model[i][j].text
        val ellipsis = settings.model[i][j].isEllipsis
        var mt: MathText
        if (Translation.ASCII_MATH == settings.translation) {
          if (s.isBlank() && settings.isAddEllipses) {
            settings.model[i][j].isEllipsis = true
            mt = if (MathModule.isNemeth) {
              MathText(
                NemethTranslations.PRINT_ELLIPSIS,
                MathModule.translateAsciiMath(NemethTranslations.BRAILLE_ELLIPSIS),
                false
              )
            } else {
              MathText(
                UebTranslations.ELLIPSIS,
                MathModule.translateAsciiMath(UebTranslations.ELLIPSIS), false
              )
            }
          } else {
            mt = MathText(s, MathModule.translateAsciiMath(s), false)
          }
        } else if (Translation.DIRECT == settings.translation) {
          if (s.isBlank() && settings.isAddEllipses) {
            settings.model[i][j].isEllipsis = true
            mt = if (MathModule.isNemeth) {
              MathText(
                NemethTranslations.PRINT_ELLIPSIS,
                MathModule.translateAsciiMath(NemethTranslations.BRAILLE_ELLIPSIS),
                false
              )
            } else {
              MathText(
                UebTranslations.ELLIPSIS,
                MathModule.translateAsciiMath(UebTranslations.ELLIPSIS), false
              )
            }
          } else {
            mt = MathText(s, s, false)
          }
        } else {
          if (s.isBlank() && settings.isAddEllipses) {
            settings.model[i][j].isEllipsis = true
            mt = if (MathModule.isNemeth) {
              MathText(
                NemethTranslations.PRINT_ELLIPSIS,
                MathModule.translateMathPrint(NemethTranslations.BRAILLE_ELLIPSIS),
                false
              )
            } else {
              MathText(
                UebTranslations.ELLIPSIS,
                MathModule.translateMainPrint(UebTranslations.ELLIPSIS), false
              )
            }
          } else {
            mt = if (MathModule.isNemeth || settings.passage == Passage.NUMERIC) {
              if (ellipsis) {
                MathText(
                  NemethTranslations.PRINT_ELLIPSIS,
                  MathModule.translateMathPrint(NemethTranslations.BRAILLE_ELLIPSIS),
                  false
                )
              } else {
                MathText(s, MathModule.translateMathPrint(s), false)
              }
            } else {
              MathText(s, MathModule.translateMainPrint(s), false)
            }
          }
        }

        cells.add(mt)
      }
    }
  }

  override fun saveSettings() {
    BBIni.propertyFileManager.saveAsInt(USER_SETTINGS_COLUMN, settings.cols)
    BBIni.propertyFileManager.saveAsInt(USER_SETTINGS_ROW, settings.rows)
    BBIni.propertyFileManager.saveAsEnum(USER_SETTINGS_OVERFLOW, settings.wideType)
    BBIni.propertyFileManager.saveAsEnum(USER_SETTINGS_GROUPING, settings.bracketType)
    BBIni.propertyFileManager.saveAsBoolean(USER_SETTINGS_ELLIPSIS, settings.isAddEllipses)
  }

  /*
 * Column, row, overflow style, grouping device, ellipsis
 */
  override fun loadSettingsFromFile() {
    val colString = BBIni.propertyFileManager.getProperty(
      USER_SETTINGS_COLUMN,
      MatrixSettings.DEFAULT_COLS.toString()
    )
    settings.cols = colString.toInt()
    val rowString = BBIni.propertyFileManager.getProperty(
      USER_SETTINGS_ROW,
      MatrixSettings.DEFAULT_ROWS.toString()
    )
    settings.rows = rowString.toInt()
    val wideString = BBIni.propertyFileManager.getProperty(
      USER_SETTINGS_OVERFLOW,
      MatrixSettings.DEFAULT_WIDE.name
    )
    val wide = Wide.valueOf(wideString)
    settings.wideType = wide
    val bracketString = BBIni.propertyFileManager.getProperty(
      USER_SETTINGS_GROUPING,
      MatrixSettings.DEFAULT_BRACKET.name
    )
    val bracket = BracketType.valueOf(bracketString)
    settings.bracketType = bracket
    val ellipsis = BBIni.propertyFileManager.getProperty(USER_SETTINGS_ELLIPSIS, "false")
    settings.isAddEllipses = ellipsis.toBoolean()
  }

  override val typeEnum: SpatialMathContainers
    get() = SpatialMathContainers.MATRIX

  override val json: ISpatialMathContainerJson
    get() = MatrixJson().containerToJson(this)

  override fun preFormatChecks(): Boolean {
    // TODO Auto-generated method stub
    return true
  }

  companion object {
    private const val USER_SETTINGS_COLUMN = "mx.column"
    private const val USER_SETTINGS_ROW = "mx.row"
    private const val USER_SETTINGS_OVERFLOW = "mx.overflow"
    private const val USER_SETTINGS_GROUPING = "mx.grouping"
    private const val USER_SETTINGS_ELLIPSIS = "mx.ellipsis"

    private val log: Logger = LoggerFactory.getLogger(Matrix::class.java)

    @JvmStatic
    fun matrixLog(s: String?) {
      if (DebugModule.enabled) {
        log.error(s)
      } else {
        log.debug(s)
      }
    }

    private fun makeLine(rowElements: ArrayList<String>, cellWidth: Int): String {
      val s = StringBuilder()
      for (i in rowElements.indices) {
        val string = rowElements[i]
        val length = string.length
        if (length > cellWidth) {
          val start = string.take(cellWidth)
          val end = string.drop(cellWidth)
          s.append(start)
          rowElements[i] = end
        } else {
          s.append(string).append(" ".repeat(cellWidth - string.length))
          rowElements[i] = ""
        }
        if (i != rowElements.size - 1) {
          s.append(" ")
        }
      }
      return s.toString()
    }

    private fun doneRenderingRow(rowElements: ArrayList<String>): Boolean {
      for (s in rowElements) {
        if (s.isNotEmpty()) {
          return false
        }
      }
      return true
    }

    private fun calculateWhitespace(length: Int, longestSegmentInColumn: Int): Int {
      return (longestSegmentInColumn + 1) - length
    }

    @JvmStatic
    fun middleMatrix(currentElement: TextMapElement): Boolean {
      if (currentElement.node == null || currentElement is LineBreakElement) {
        return false
      }
      val matrix =
        XMLHandler.ancestorVisitorElement(currentElement.node) { node: Element? -> BBX.CONTAINER.MATRIX.isA(node) }
      return matrix != null
    }

    fun getMatrixParent(node: Node): Element? {
      return XMLHandler.ancestorVisitorElement(node) { e: Element -> BBX.CONTAINER.MATRIX.isA(e) }
    }

    fun isMatrix(node: Node?): Boolean {
      if (node == null || node.document == null) {
        return false
      }
      return XMLHandler.ancestorElementIs(node) { e: Element -> BBX.CONTAINER.MATRIX.isA(e) }
    }

    @JvmStatic
    fun currentIsMatrix(): Boolean {
      val current: Node? = XMLHandler.ancestorVisitorElement(
        WPManager.getInstance().controller.simpleManager.currentCaret.node
      ) { node: Element? -> BBX.CONTAINER.MATRIX.isA(node) }
      val isWhitespace = WPManager.getInstance().controller.mapList
        .current is WhiteSpaceElement
      return !isWhitespace && current != null
    }

    fun getContainerFromElement(currentElement: Element?): Matrix {
      var current = currentElement
      var matrix = Matrix()
      if (BBX.CONTAINER.MATRIX.isA(current)) {
        if (!BBX.CONTAINER.MATRIX.ATTRIB_VERSION.has(current)) {
          current = convertMatrix(current)
        }
        val json = BBX.CONTAINER.MATRIX.JSON_MATRIX[current] as MatrixJson
        matrix = json.jsonToContainer() as Matrix
      }
      return matrix
    }

    @JvmStatic
    fun initialize(n: Node): Element {
      val e = n as Element
      val t = getContainerFromElement(e)
      t.format()
      val newElement = BBX.CONTAINER.MATRIX.create(t)
      try {
        format(newElement, t.lines)
      } catch (e1: MathFormattingException) {
        log.warn("Problem formatting spatial block when initialising node", e1)
      }
      e.parent.replaceChild(e, newElement)
      return newElement
    }

    fun deleteMatrix() {
      val current = XMLHandler.ancestorVisitorElement(
        WPManager.getInstance().controller.simpleManager.currentCaret.node
      ) { node: Element? -> BBX.CONTAINER.MATRIX.isA(node) }
      // get previous block for caret event
      val previous = XMLHandler.previousSiblingNode(current!!)
      val parent = current.parent
      parent.removeChild(current)
      WPManager.getInstance().controller.simpleManager
        .dispatchEvent(ModifyEvent(Sender.TEXT, true, (parent)))
      if (previous != null) {
        WPManager.getInstance().controller.simpleManager
          .dispatchEvent(XMLCaretEvent(Sender.TEXT, XMLNodeCaret(previous)))
      }
    }
  }
}

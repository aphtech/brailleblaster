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
package org.brailleblaster.math.ascii

import nu.xom.Element
import nu.xom.Node
import org.brailleblaster.utd.asciimath.AsciiMathConverter
import org.slf4j.LoggerFactory

object ASCII2MathML {

  private val log = LoggerFactory.getLogger(ASCII2MathML::class.java)

  /**
   * Don't call the parser from anywhere but here.
   *
   * @param st
   * @return math root node, detached from document,, or null
   */
	@JvmStatic
	fun translate(st: String): Node {
    var s = st
    if (s.contains("\r")) {
      log.debug("line feed to javascript parser")
    }
    if (s.contains("\n")) {
      log.debug("new line to javascript parser")
    }
    if (s.contains("!UNDEFINED!")) {
      log.debug("undefined to javascript parser")
    }
    s = s.replace("!UNDEFINED!", "")
    log.debug("Sending " + s.length + " chars to javascript parser")
    log.debug("Sending $s to the javascript parser")
    val nodes = AsciiMathConverter.toMathML(s, addAltText = true, stripMathMarkers = false)
    return if (nodes.size() < 1) {
      log.error("Error in translation to MathML.")
      Element("math")
    } else {
      val mathNode = nodes[0]
      // Copy may not be needed now.
      mathNode.copy() // cannot detach the node from the document because
      // it is the root and XOM will throw an exception.
      // Work around this by copying
    }
  }

  /**
   * Don't call the parser from anywhere but here.
   *
   * @param st
   * @return math root node, detached from document,, or null
   */
	@JvmStatic
	fun translateUseHTML(st: String): org.w3c.dom.Element {
    var s = st
    if (s.contains("\r")) {
      log.debug("line feed to javascript parser")
    }
    if (s.contains("\n")) {
      log.debug("new line to javascript parser")
    }
    if (s.contains("!UNDEFINED!")) {
      log.debug("undefined to javascript parser")
    }
    s = s.replace("!UNDEFINED!", "")
    log.debug("Sending " + s.length + " chars to javascript parser")
    log.debug("Sending $s to the javascript parser")
    return AsciiMathConverter.toMathMLHTMLNodes(s)
  }
}

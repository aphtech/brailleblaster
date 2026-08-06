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
package org.brailleblaster.utd.properties

import jakarta.xml.bind.annotation.XmlEnum
import org.mwhapples.jlouis.Louis.TypeForms

// longName is the pretty name
@XmlEnum
enum class EmphasisType(val jlouisTypeform: Short, val longName: String) {
  BOLD(TypeForms.BOLD, "Bold"),
  ITALICS(TypeForms.ITALIC, "Italics"),
  UNDERLINE(TypeForms.UNDERLINE, "Underline"),
  NO_TRANSLATE(TypeForms.NO_TRANSLATE, "No Translate"),
  TRANS_1(TypeForms.TRANS_1, "Transcriber-Defined 1"),
  TRANS_2(TypeForms.TRANS_2, "Transcriber-Defined 2"),
  TRANS_3(TypeForms.TRANS_3, "Transcriber-Defined 3"),
  TRANS_4(TypeForms.TRANS_4, "Transcriber-Defined 4"),
  TRANS_5(TypeForms.TRANS_5, "Transcriber-Defined 5"),
  SCRIPT(TypeForms.SCRIPT, "Script"),
  TRANS_NOTE(TypeForms.TRANS_NOTE, "Transcriber Note Symbols"),
  NO_CONTRACT(TypeForms.NO_CONTRACT, "No Contract");

  //From V2
  companion object {
    /**
     * @param longName
     * @return EmphasisType for corresponding pretty name, null if not found
     */
    fun getEmphasisType(longName: String): EmphasisType? {
      for (type in entries) {
        if (type.longName == longName) {
          return type
        }
      }
      return null
    }
  }
}
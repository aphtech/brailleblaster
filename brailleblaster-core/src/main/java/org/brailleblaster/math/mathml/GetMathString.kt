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
package org.brailleblaster.math.mathml

import nu.xom.Element
import nu.xom.Node
import nu.xom.Text
import org.brailleblaster.utils.xom.childNodes

fun interface GetMathString {
    fun getString(o: Any): String

    companion object {
        @JvmField
		val fromNode: GetMathString = GetMathString { n: Any ->
            if (n is Element && n.getAttributeValue("alttext") != null) MathModuleUtils.getMathText(n as Node) else getStringValueFromNode(
                n as Node,
                ""
            )
        }
        @JvmField
		val fromText: GetMathString = GetMathString { text: Any -> text as String }
        private fun getStringValueFromNode(n: Node, s: String): String {
            return if (n.childCount == 0) {
                if (n is Text) {
                    var childString = n.value
                    childString =
                        childString.replace("\n".toRegex(), "").replace("\r".toRegex(), "").replace(" ".toRegex(), "")
                    s + childString
                } else s
            } else {
                n.childNodes.fold(s) { acc, node -> getStringValueFromNode(node, acc) }
            }
        }
    }
}
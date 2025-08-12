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
package org.brailleblaster.util

import org.brailleblaster.settings.UTDManager

/**
 * Convenience class so we only have to parse the
 * styleId string once.
 * You can access the main class, sub class, title,
 * or whole string through the static methods to find
 * the matching styles.  sList will give you all of the
 * styles.
 */
class StyleId(utd: UTDManager) {
    init {
        utd.engine.styleDefinitions.styles.forEach { s ->
            val i = Id()
            val id = s.id
            val array = id.split("/").dropLastWhile { it.isEmpty() }
            when (array.size) {
                2 -> {
                    i.main = array[0]
                    i.title = array[1]
                }

                3 -> {
                    i.main = array[0]
                    i.sub = array[1]
                    i.title = array[2]
                }

                else -> {
                    throw RuntimeException("Parsing styles failed.")
                }
            }
            i.whole = id
            slist.add(i)
        }
    }

    class Id {
        var whole = ""
        var main = ""
        var sub = "NONE"
        var title = ""
    }

    companion object {
        var slist: MutableList<Id> = ArrayList()

        @JvmStatic
        fun getWholeFromMain(main: String): List<String> {
            return slist.filter { it.main == main }.map { it.whole }
        }
    }
}

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
package org.brailleblaster.utd.matchers

import jakarta.xml.bind.Unmarshaller

abstract class DelegatingMatcher : INodeMatcher {
    var matcher: INodeMatcher? = null
    override fun hashCode(): Int {
        val prime = 31
        var result = 1
        result = prime * result + matcher.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null) {
            return false
        }
        if (javaClass != other.javaClass) {
            return false
        }
        val o = other as DelegatingMatcher
        return matcher == o.matcher
    }

    fun afterUnmarshal(unmarshaller: Unmarshaller?, parent: Any?) {
        if (matcher == null) {
            throw Error("No matchers added for $this")
        }
    }
}
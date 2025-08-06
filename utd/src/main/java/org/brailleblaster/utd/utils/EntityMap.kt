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
package org.brailleblaster.utd.utils

import org.xml.sax.InputSource
import java.io.StringReader

class EntityMap private constructor(
    private val publicIdMap: Map<String, IEntitySource>,
    private val systemIdMap: Map<String, IEntitySource>
) {
    class Builder {
        private var publicIdBuilder = mutableMapOf<String, IEntitySource>()
        private var systemIdBuilder = mutableMapOf<String, IEntitySource>()
        fun put(publicId: String?, systemId: String?, source: IEntitySource): Builder {
            if (publicId != null) {
                publicIdBuilder[publicId] = source
            }
            if (systemId != null) {
                systemIdBuilder[systemId] = source
            }
            return this
        }

        fun build(): EntityMap {
            return EntityMap(publicIdBuilder.toMap(), systemIdBuilder.toMap())
        }
    }

    interface IEntitySource {
        val source: InputSource
    }

    class JarEntity(resourceName: String?) : IEntitySource {
        private val resourceName: String
        override val source: InputSource
            get() = InputSource(javaClass.getResourceAsStream(resourceName))

        init {
            requireNotNull(resourceName) { "Resource name cannot be null" }
            this.resourceName = resourceName
        }
    }

    class EmptyEntity : IEntitySource {
        override val source: InputSource
            get() = InputSource(StringReader(""))
    }

    fun containsSystemId(systemId: String): Boolean {
        return systemIdMap.containsKey(systemId)
    }

    fun containsPublicId(publicId: String): Boolean {
        return publicIdMap.containsKey(publicId)
    }

    fun getByPublicId(publicId: String): InputSource {
        return publicIdMap[publicId]!!.source
    }

    fun getBySystemId(systemId: String): InputSource {
        return systemIdMap[systemId]!!.source
    }

    companion object {
        @JvmStatic
        fun builder(): Builder {
            return Builder()
        }
    }
}
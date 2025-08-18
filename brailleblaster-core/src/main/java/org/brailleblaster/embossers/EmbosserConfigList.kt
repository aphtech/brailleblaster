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
package org.brailleblaster.embossers

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.File
import java.io.IOException

@Serializable
class EmbosserConfigList(private val embosserConfigs: MutableList<EmbosserConfig> = mutableListOf(), @Transient private var embossersFile: File? = null) : MutableList<EmbosserConfig> by embosserConfigs {

    private var defaultName: String? = null
    private var lastUsedName: String? = null
    @SerialName("useLast")
    var isUseLastEmbosser = true

    // When no default is set we resort to the first embosser.
    var defaultEmbosser: EmbosserConfig
        get() {
            if (embosserConfigs.isEmpty()) {
                throw NoSuchElementException()
            }
            // When no default is set we resort to the first embosser.
            return embosserConfigs.firstOrNull { e: EmbosserConfig -> e.name == defaultName } ?: embosserConfigs[0]
        }
        set(embosser) {
            require(embosserConfigs.contains(embosser)) { "Specified embosser is not in embosser list" }
            defaultName = embosser.name
        }
    var lastUsedEmbosser: EmbosserConfig
        get() {
            if (embosserConfigs.isEmpty()) {
                throw NoSuchElementException()
            }
            return embosserConfigs.firstOrNull { e: EmbosserConfig -> e.name == lastUsedName } ?: embosserConfigs[0]
        }
        set(embosser) {
            require(embosserConfigs.contains(embosser)) { "Specified embosser is not in embosser list" }
            lastUsedName = embosser.name
        }

    /**
     * Get the embosser as based upon user preferences.
     *
     *
     * This method will get the last used embosser should the preference for last used embosser
     * have been set, otherwise it will get the default embosser as defined by the user preferences.
     * This method delegates to the getLastUsedEmbosser() and getDefaultEmbosser() methods, so should
     * the embosser as defined by those preferences no longer exist then it will just return the first
     * embosser.
     *
     * @return The embosser as defined by the user's preferences.
     */
    val preferredEmbosser: EmbosserConfig
        get() = if (isUseLastEmbosser) lastUsedEmbosser else defaultEmbosser


    @Throws(IOException::class)
    fun saveEmbossers() {
        saveEmbossers(
            embossersFile ?: throw IllegalStateException(
                "The Embossers object has no default file name, use saveEmbossers(File) instead"
            )
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Throws(IOException::class)
    fun saveEmbossers(embossersFile: File) {
        embossersFile.outputStream().use { Json.encodeToStream(this, it) }
    }

    companion object {

        @OptIn(ExperimentalSerializationApi::class)
        fun loadEmbossers(
            embossersFile: File, s: () -> EmbosserConfigList = { EmbosserConfigList() }
        ): EmbosserConfigList {
            return try {
                embossersFile.inputStream().use { Json.decodeFromStream(it) }
            } catch (_: SerializationException) {
                s()
            } catch (_: IOException) {
                s()
            }.also { it.embossersFile = embossersFile }
        }

    }
}

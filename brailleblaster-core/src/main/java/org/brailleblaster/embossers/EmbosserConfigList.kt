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

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.IOException
import java.lang.reflect.Type

class EmbosserConfigList(private val embosserConfigs: MutableList<EmbosserConfig> = mutableListOf(), @kotlin.jvm.Transient private var embossersFile: File? = null) : MutableList<EmbosserConfig> by embosserConfigs {

    internal var defaultName: String? = null
    internal var lastUsedName: String? = null
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

    @Throws(IOException::class)
    fun saveEmbossers(embossersFile: File) {
        embossersFile.writer().use { GSON.toJson(this, it) }
    }

    companion object {
        val GSON: Gson = GsonBuilder()
            .registerTypeAdapter(EmbosserConfig::class.java, EmbosserConfig.Companion.GsonAdapter())
            .registerTypeAdapter(EmbosserConfigList::class.java, GsonAdapter())
            .create()

        fun loadEmbossers(
            embossersFile: File, s: () -> EmbosserConfigList = { EmbosserConfigList() }
        ): EmbosserConfigList {
            return try {
                embossersFile.reader().use { GSON.fromJson(it, EmbosserConfigList::class.java) }
            } catch (_: JsonParseException) {
                s()
            } catch (_: IOException) {
                s()
            }.also { it.embossersFile = embossersFile }
        }

        class GsonAdapter : JsonSerializer<EmbosserConfigList>, JsonDeserializer<EmbosserConfigList> {
            override fun serialize(src: EmbosserConfigList, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
                val obj = JsonObject()
                obj.add("embosserConfigs", context.serialize(src.toList()))
                obj.addProperty("defaultName", src.defaultName)
                obj.addProperty("lastUsedName", src.lastUsedName)
                obj.addProperty("useLast", src.isUseLastEmbosser)
                return obj
            }

            override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): EmbosserConfigList {
                val obj = json.asJsonObject
                val configsType = object : TypeToken<MutableList<EmbosserConfig>>() {}.type
                val configs: MutableList<EmbosserConfig> = context.deserialize(obj.get("embosserConfigs"), configsType) ?: mutableListOf()

                val list = EmbosserConfigList(configs)
                list.defaultName = obj.get("defaultName")?.asString
                list.lastUsedName = obj.get("lastUsedName")?.asString
                list.isUseLastEmbosser = obj.get("useLast")?.asBoolean ?: true

                return list
            }
        }
    }
}

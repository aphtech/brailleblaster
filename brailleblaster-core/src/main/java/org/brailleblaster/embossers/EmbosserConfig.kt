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
import org.brailleblaster.libembosser.EmbosserService
import org.brailleblaster.libembosser.spi.EmbossException
import org.brailleblaster.libembosser.spi.Embosser
import org.brailleblaster.libembosser.spi.EmbossingAttributeSet
import org.slf4j.LoggerFactory
import org.w3c.dom.Document
import java.io.InputStream
import java.lang.reflect.Type
import javax.print.DocFlavor
import javax.print.PrintService
import javax.print.PrintServiceLookup
import kotlin.jvm.optionals.getOrNull

class EmbosserConfig(val name: String = "", var printerName: String? = null) {
    var embosserDriver: Embosser? = null

    fun setEmbosserDriver(manufacturer: String, model: String) {
        embosserDriver = EmbosserService.getInstance()
            .embosserStream
            .filter { e: Embosser -> e.manufacturer == manufacturer && e.model == model }
            .findFirst().orElse(null)
    }

    val isActive: Boolean
        get() = printService != null && embosserDriver != null
    private var printService: PrintService?
        get() = getPrinterForName(printerName)
        set(value) {
            printerName = value?.name
        }

    fun embossBrf(inputStream: InputStream?, attributes: EmbossingAttributeSet?): Boolean {
        val p = printService
        return if (p != null) {
            embossBrf(inputStream, attributes, p)
        } else {
            logger.warn("Embosser device not available")
            false
        }
    }

    fun embossBrf(
        inputStream: InputStream?, attributes: EmbossingAttributeSet?, ps: PrintService
    ): Boolean {
        val driver = embosserDriver
        requireNotNull(driver) { "Config must have an embosser driver set to be able to emboss" }
        try {
            driver.embossBrf(
                ps, inputStream!!, attributes!!
            )
        } catch (e: EmbossException) {
            logger.warn("Unable to emboss", e)
            return false
        }
        return true
    }

    fun embossPef(pef: Document?, attributes: EmbossingAttributeSet?): Boolean {
        val p = printService
        return if (p != null) {
            embossPef(pef, attributes, p)
        } else {
            logger.warn("Embosser device not available")
            false
        }
    }

    fun embossPef(pef: Document?, attributes: EmbossingAttributeSet?, ps: PrintService): Boolean {
        val driver = embosserDriver
        requireNotNull(driver) { "Config must have an embosser driver set to be able to emboss" }
        try {
            driver.embossPef(
                ps, pef!!, attributes!!
            )
        } catch (e: EmbossException) {
            logger.warn("Unable to emboss", e)
            return false
        }
        return true
    }

    companion object {
        private val logger = LoggerFactory.getLogger(EmbosserConfig::class.java)
        private fun getPrinterForName(name: String?): PrintService? {
            val services = PrintServiceLookup.lookupPrintServices(DocFlavor.INPUT_STREAM.AUTOSENSE, null)
            return services.firstOrNull { p: PrintService -> p.name == name }
        }

        class GsonAdapter : JsonSerializer<EmbosserConfig>, JsonDeserializer<EmbosserConfig> {
            override fun serialize(src: EmbosserConfig, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
                val obj = JsonObject()
                obj.addProperty("name", src.name)
                obj.addProperty("printerName", src.printerName)

                val driver = src.embosserDriver
                obj.addProperty("embosserDriver", driver?.id)

                val options = JsonObject()
                driver?.options?.forEach { (k, v) ->
                    options.addProperty(k.id, v.value)
                }
                obj.add("embosserOptions", options)

                return obj
            }

            override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): EmbosserConfig {
                val obj = json.asJsonObject
                val name = obj.get("name")?.asString ?: ""
                val printerName = obj.get("printerName")?.asString

                val config = EmbosserConfig(name, printerName)

                val driverId = obj.get("embosserDriver")?.asString
                if (driverId != null) {
                    config.embosserDriver = EmbosserService.getInstance().embosserStream
                        .filter { it.id == driverId }
                        .findFirst()
                        .getOrNull()

                    val optionsObj = obj.getAsJsonObject("embosserOptions")
                    if (optionsObj != null && config.embosserDriver != null) {
                        val driver = config.embosserDriver!!
                        config.embosserDriver = driver.customize(driver.options.mapValues { (k, v) ->
                            optionsObj.get(k.id)?.let { v.copy(it.asString) } ?: v
                        })
                    }
                }

                return config
            }
        }
    }
}
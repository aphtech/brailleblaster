/*
 * Copyright (C) 2026 Michael Whapples
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

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import java.io.File

class EmbosserConfigListTest {

    @Test
    fun testDeserializationMatchesCurrentFormat() {
        val json = """
            {
              "embosserConfigs": [
                {
                  "name": "My Embosser",
                  "printerName": "Generic Text Only",
                  "embosserDriver": "aph-pixblaster",
                  "embosserOptions": {
                    "option1": "value1"
                  }
                }
              ],
              "defaultName": "My Embosser",
              "lastUsedName": "My Embosser",
              "useLast": true
            }
        """.trimIndent()

        val tempFile = File.createTempFile("embossers", ".json")
        tempFile.writeText(json)
        
        try {
            val list = EmbosserConfigList.loadEmbossers(tempFile)
            assertThat(list).hasSize(1)
            assertThat(list[0].name).isEqualTo("My Embosser")
            assertThat(list[0].printerName).isEqualTo("Generic Text Only")
            // embosserDriver might be null if not found in service, which is expected in test env
            assertThat(list.isUseLastEmbosser).isTrue()
            assertThat(list.preferredEmbosser.name).isEqualTo("My Embosser")
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun testSerializationPreservesFormat() {
        val list = EmbosserConfigList()
        val config = EmbosserConfig("New Embosser", "New Printer")
        list.add(config)
        list.isUseLastEmbosser = false
        list.defaultEmbosser = config

        val tempFile = File.createTempFile("embossers-save", ".json")
        try {
            list.saveEmbossers(tempFile)
            val savedJson = tempFile.readText()
            
            assertThat(savedJson).contains("\"name\":\"New Embosser\"")
            assertThat(savedJson).contains("\"printerName\":\"New Printer\"")
            assertThat(savedJson).contains("\"useLast\":false")
            assertThat(savedJson).contains("\"defaultName\":\"New Embosser\"")
            assertThat(savedJson).contains("\"embosserConfigs\":[")
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun testListDelegation() {
        val list = EmbosserConfigList()
        val config1 = EmbosserConfig("E1")
        val config2 = EmbosserConfig("E2")
        
        list.add(config1)
        list.add(config2)
        
        assertThat(list).hasSize(2)
        assertThat(list[0].name).isEqualTo("E1")
        assertThat(list[1].name).isEqualTo("E2")
        
        list.remove(config1)
        assertThat(list).hasSize(1)
        assertThat(list[0].name).isEqualTo("E2")
    }

    @Test
    fun testPreferredEmbosserLogic() {
        val list = EmbosserConfigList()
        val config1 = EmbosserConfig("Default")
        val config2 = EmbosserConfig("LastUsed")
        list.add(config1)
        list.add(config2)
        
        list.defaultEmbosser = config1
        list.lastUsedEmbosser = config2
        
        list.isUseLastEmbosser = false
        assertThat(list.preferredEmbosser.name).isEqualTo("Default")
        
        list.isUseLastEmbosser = true
        assertThat(list.preferredEmbosser.name).isEqualTo("LastUsed")
    }
}

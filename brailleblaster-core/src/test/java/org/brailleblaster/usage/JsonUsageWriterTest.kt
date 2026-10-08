/*
 * Copyright (C) 2026 Michael whapples
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
package org.brailleblaster.usage

import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.Test
import java.io.StringWriter
import java.time.Instant

class JsonUsageWriterTest {
    @Test
    fun testWriteSingleRecord() {
        val writer = JsonUsageWriter()
        val record = UsageRecord(
            tool = "TestTool",
            event = "TestEvent",
            message = "TestMessage",
            time = Instant.parse("2023-10-27T10:15:30.123Z")
        )
        val stringWriter = StringWriter()
        writer.write(record, stringWriter)

        val output = stringWriter.toString()
        assertThat(output).contains("\"time\":\"2023-10-27, 10:15:30.123\"")
        assertThat(output).contains("\"tool\":\"TestTool\"")
        assertThat(output).contains("\"event\":\"TestEvent\"")
        assertThat(output).contains("\"msg\":\"TestMessage\"")
    }

    @Test
    fun testWriteMultipleRecords() {
        val writer = JsonUsageWriter()
        val records = listOf(
            UsageRecord(
                tool = "Tool1",
                event = "Event1",
                message = "Msg1",
                time = Instant.parse("2023-10-27T10:15:30.123Z")
            ),
            UsageRecord(
                tool = "Tool2",
                event = "Event2",
                message = "Msg2",
                time = Instant.parse("2023-10-27T11:15:30.123Z")
            )
        )
        val stringWriter = StringWriter()
        writer.write(records, stringWriter)

        val output = stringWriter.toString()
        assertThat(output).startsWith("[")
        assertThat(output).endsWith("]")
        assertThat(output).contains("\"msg\":\"Msg1\"")
        assertThat(output).contains("\"msg\":\"Msg2\"")
    }
}

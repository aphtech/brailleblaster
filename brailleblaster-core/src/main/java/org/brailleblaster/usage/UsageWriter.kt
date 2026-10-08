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
package org.brailleblaster.usage

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.io.Writer
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

interface UsageWriter {
    fun write(record: UsageRecord, writer: Writer)
    fun write(records: Iterable<UsageRecord>, writer: Writer)
}

private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd, HH:mm:ss.SSS", Locale.US).withZone(ZoneOffset.ofHours(0))

class JsonUsageWriter : UsageWriter {
    private val gson = Gson()

    private fun toJson(record: UsageRecord): JsonObject {
        val jsonObject = JsonObject()
        jsonObject.addProperty("time", formatter.format(record.time))
        jsonObject.addProperty("tool", record.tool)
        jsonObject.addProperty("event", record.event)
        jsonObject.addProperty("msg", record.message)
        return jsonObject
    }

    override fun write(record: UsageRecord, writer: Writer) {
        gson.toJson(toJson(record), writer)
    }

    override fun write(records: Iterable<UsageRecord>, writer: Writer) {
        val jsonArray = JsonArray()
        for (record in records) {
            jsonArray.add(toJson(record))
        }
        gson.toJson(jsonArray, writer)
    }
}
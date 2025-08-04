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

import java.io.Closeable
import java.sql.Connection
import java.sql.DriverManager
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Consumer
import java.util.function.Predicate

const val BB_TOOL = "BrailleBlaster"

data class UsageRecord(val tool: String, val event: String, val message: String = "", val time: Instant = Instant.now())

interface UsageLogger : Iterable<UsageRecord> {
    // fun log(tool: String, event: String, message: String = "") = log(UsageRecord(tool, event, message))
    fun log(record: UsageRecord)
    fun clear()
}

sealed interface SizeLimit {
    data object Unlimited : SizeLimit
    @JvmInline
    value class Max(val limit: Int) : SizeLimit {
        init {
            require(limit >= 0)
        }
    }
}
class ListUsageLogger(val sizeLimit: SizeLimit = SizeLimit.Unlimited) : UsageLogger {
    private val usageList: MutableList<UsageRecord> = mutableListOf()
    override operator fun iterator(): Iterator<UsageRecord> = usageList.iterator()
    override fun spliterator(): Spliterator<UsageRecord> = usageList.spliterator()

    override fun log(record: UsageRecord) {
        when(sizeLimit) {
            SizeLimit.Unlimited -> usageList.add(record)
            SizeLimit.Max(0) -> {}
            is SizeLimit.Max -> {
                usageList.add(record)
                if (usageList.size > sizeLimit.limit) usageList.removeFirst()
            }
        }
    }

    override fun clear() = usageList.clear()
}


class SqliteUsageLogger(connectionString: String) : UsageLogger, AutoCloseable, Closeable {
    val connection: Connection = DriverManager.getConnection(connectionString)
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    override fun close() {
        executorService.shutdown()
        connection.close()
    }

    override fun log(record: UsageRecord) {
        executorService.submit {
            connection.prepareStatement(INSERT_RECORD_SQL).use { stmt ->
                stmt.apply {
                    setString(1, record.tool)
                    setString(2, record.event)
                    setString(3, record.message)
                    setLong(4, record.time.toEpochMilli())
                }.execute()
            }
        }
    }

    override fun clear() {
        executorService.submit {
            connection.createStatement().use { stmt ->
                stmt.execute(CLEAR_TABLE_SQL)
            }
        }
    }

    override fun iterator(): Iterator<UsageRecord> {
        return executorService.submit<List<UsageRecord>> {
            buildList {
                connection.createStatement().use { stmt ->
                    stmt.executeQuery(SELECT_ALL_RECORDS_SQL).use { results ->
                        while (results.next()) {
                            val tool = results.getString("tool")
                            val event = results.getString("event")
                            val message = results.getString("message")
                            val time = Instant.ofEpochMilli(results.getLong("time"))
                            add(UsageRecord(tool, event, message, time))
                        }
                    }
                }
            }
        }.get().iterator()
    }
    fun clearTo(time: Instant) {
        executorService.submit {
            connection.prepareStatement("DELETE FROM usage WHERE time <= ?").use { stmt ->
                stmt.apply { setLong(1, time.toEpochMilli()) }.execute()
            }
        }
    }
    init {
        executorService.submit {
            connection.createStatement().use { stmt ->
                stmt.execute(CREATE_TABLE_SQL)
            }
        }
    }
    companion object {
        private const val CREATE_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS usage (record_id INTEGER PRIMARY KEY, tool TEXT NOT NULL, event TEXT NOT NULL, message TEXT NOT NULL, time INTEGER NOT NULL)"
        private const val INSERT_RECORD_SQL = "INSERT INTO usage (tool, event, message, time) VALUES(?, ?, ?, ?)"
        private const val CLEAR_TABLE_SQL = "DELETE FROM usage"
        private const val SELECT_ALL_RECORDS_SQL = "SELECT tool, event, message, time FROM usage"
    }
}
class SimpleTextUsageLogger(private val delegate: UsageLogger, private val out: Appendable) : UsageLogger by delegate {
    override fun log(record: UsageRecord) {
        delegate.log(record)
        out.appendLine(record.toString())
    }

    override fun forEach(action: Consumer<in UsageRecord>) {
        delegate.forEach(action)
    }

    override fun spliterator(): Spliterator<UsageRecord> {
        return delegate.spliterator()
    }
}

private class FilteredUsageLogger(private val delegate: UsageLogger, private val predicate: Predicate<UsageRecord>) : UsageLogger by delegate {
    override fun log(record: UsageRecord) {
        if (predicate.test(record)) delegate.log(record)
    }

    override fun forEach(action: Consumer<in UsageRecord>) {
        delegate.forEach(action)
    }

    override fun spliterator(): Spliterator<UsageRecord> {
        return delegate.spliterator()
    }
}

fun UsageLogger.filterLogger(predicate: Predicate<UsageRecord>): UsageLogger = FilteredUsageLogger(this, predicate)

fun UsageLogger.logStart(tool: String, message: String = "", time: Instant = Instant.now()) = this.log(UsageRecord(tool = tool, event = "start", message = message, time = time))
fun UsageLogger.logEnd(tool: String, message: String = "", time: Instant = Instant.now()) = this.log(UsageRecord(tool = tool, event = "end", message = message, time = time))
fun UsageLogger.logDurationSeconds(tool: String, duration: Duration, time: Instant = Instant.now()) = this.log(
    UsageRecord(tool = tool, event = "duration-seconds", message = duration.toSeconds().toString(), time = time)
)
fun UsageLogger.logException(tool: String, e: Exception, time: Instant = Instant.now()) {
    val sb = StringBuilder(e.javaClass.canonicalName)
    var cause = e.cause
    val dejaVu: MutableSet<Throwable> = Collections.newSetFromMap(IdentityHashMap())
    while (cause != null) {
        sb.append('\n')
        cause = if (cause in dejaVu) {
            sb.append("Cycle detected")
            null
        } else {
            sb.append("Caused by: ${cause.javaClass.canonicalName}")
            dejaVu.add(cause)
            cause.cause
        }
    }
    this.log(UsageRecord(tool = tool, event = "exception", message = sb.toString(), time = time))
}
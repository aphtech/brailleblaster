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
package org.brailleblaster.bbx

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.apache.commons.lang3.exception.ExceptionUtils
import org.brailleblaster.Main.initBB
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.util.Utils
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.stream.Collectors
import kotlin.system.exitProcess

object BenchAllNimas {
    private val log = LoggerFactory.getLogger(BenchAllNimas::class.java)
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {
        initBB(args.asList())
        //		args = new String[]{"/home/leon/nimas-books", "6"};;
        var booksPath = Paths.get(args[0])
        if (!Files.exists(booksPath)) {
            if (args.size != 1) {
                System.err.println(BenchAllNimas::class.java.name + " <path to nimas-books folder on stevie>")
                exitProcess(1)
            }
            booksPath = Paths.get(args[0])
            if (!Files.exists(booksPath)) {
                System.err.println("Given path $booksPath does not exist")
                exitProcess(1)
            }
        }
        val threadPool = Executors.newFixedThreadPool(args[1].toInt(), ThreadFactoryBuilder().setDaemon(true).build())
        val converter = BookToBBXConverter.fromConfig()
        val tasks = Files.list(booksPath)
            .filter { curFile: Path -> curFile.toString().endsWith(".xml") || curFile.toString().endsWith(".zip") }
            .map { curFile: Path -> Exec(curFile, converter) }
            .collect(Collectors.toList())
        val startTime = System.currentTimeMillis()
        val results: MutableList<Pair<Path, Exception?>> = ArrayList()
        for ((index, curFuture) in threadPool.invokeAll(tasks).withIndex()) {
            val fut = curFuture.get()
            log.debug("{}/{} completed", index, tasks.size)
            results.add(fut)
        }
        log.error("------------ finished -------------")
        var exceptions = 0
        for ((key, value) in results) {
            log.error(
                "Book $key" + if (value != null) " exception " + ExceptionUtils.getStackTrace(
                    value
                ) else "no exception"
            )
            if (value != null) {
                exceptions++
            }
        }
        log.error("{}/{} books have exceptions", exceptions, results.size)
        log.error("ran in " + Utils.runtimeToString(startTime))
    }

    private class Exec(private val path: Path, private val converter: BookToBBXConverter) :
        Callable<Pair<Path, Exception?>> {
        @Throws(Exception::class)
        override fun call(): Pair<Path, Exception?> {
            return try {
                val load = XMLHandler().load(path)
                converter.convert(load, path.toString())
                path to null
            } catch (e: Exception) {  path to e
            }
        }
    }
}
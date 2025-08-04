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
package org.brailleblaster.perspectives.braille.viewInitializer

import nu.xom.Node
import org.brailleblaster.perspectives.braille.mapping.elements.SectionElement
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.*
import kotlin.math.max

class SectionMapSearcher(private val sectionList: ArrayList<SectionElement>) {
    fun search(n: Node?): Pair<Int, Int>? {
        val processors = Runtime.getRuntime().availableProcessors()
        //If there are less sections than processors, the above will be 0
        //which will cause an infinate loop when building callables below
        val sectionsPerProc = max((sectionList.size / processors).toDouble(), 1.0).toInt()

        log.trace(
            "Searching {} sections at {} sections / processor over {} procs",
            sectionList.size,
            sectionsPerProc,
            processors
        )

        val executor = Executors.newFixedThreadPool(processors)
        val ecs: CompletionService<Pair<Int, Int>> = ExecutorCompletionService(executor)
        val callables: MutableList<SearchCallable> = ArrayList()
        for (i in 0 until sectionList.size step sectionsPerProc) {
            if (i == sectionList.size - 1 || i + sectionsPerProc > sectionList.size) callables.add(
                SearchCallable(
                    n,
                    i,
                    sectionList.size
                )
            )
            else callables.add(SearchCallable(n, i, i + sectionsPerProc))
        }

        val futures: MutableList<Future<Pair<Int, Int>>> = ArrayList()
        var result: Pair<Int, Int>? = null

        try {
            val callableLength = callables.size
            for (s in callables) futures.add(ecs.submit(s))

            for (i in 0 until callableLength) {
                try {
                    val r = ecs.take().get()
                    if (r != null) {
                        result = r
                        break
                    }
                } catch (ignore: ExecutionException) {
                } catch (ignore: InterruptedException) {
                }
            }
        } finally {
            for (f in futures) f.cancel(true)

            executor.shutdownNow()
        }

        return result
    }

    private inner class SearchCallable(val n: Node?, val start: Int, val end: Int) : Callable<Pair<Int, Int>?> {
        override fun call(): Pair<Int, Int>? {
            for (i in start until end) {
                if (!sectionList[i].isVisible) {
                    val index = sectionList[i].list.findNodeIndex(n, 0)
                    if (index != -1) return i to index
                }
            }

            return null
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(SectionMapSearcher::class.java)
    }
}

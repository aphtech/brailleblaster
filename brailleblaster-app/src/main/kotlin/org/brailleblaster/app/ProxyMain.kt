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

package org.brailleblaster.app

import org.brailleblaster.utils.arch
import org.brailleblaster.utils.os
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory
import java.util.concurrent.ForkJoinWorkerThread
import kotlin.io.path.*

/**
 * A launcher to get the app running in a ClassLoader which can be modified.
 *
 *
 * The system class loader is not possible to update. In the past (JDK8 and earlier) the system
 * class loader extended URLClassLoader but this was never part of the Java specification and is no
 * longer true from JDK9. This launcher will set up a ClassLoader which is possible to modify at
 * runtime.
 */
object ProxyMain {
    @JvmStatic
    fun main(args: Array<String>) {
        System.setProperty(
            "java.util.concurrent.ForkJoinPool.common.threadFactory",
            "org.brailleblaster.app.BBForkJoinWorkerThreadFactory"
        )

        System.setProperty("app.dir", brailleblasterPath)
        Thread.currentThread().contextClassLoader = proxyClassLoader
        try {
            val mainClass = Class.forName("org.brailleblaster.Main", true, proxyClassLoader)
            val m = mainClass.getMethod("main", args.javaClass)
            m.invoke(null, args)
        } catch (e: ClassNotFoundException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        }
    }
}

private fun Path.listJars(): List<Path> {
    return if (isDirectory()) {
        listDirectoryEntries().filter { it.isJarFile }
    } else if (isJarFile) listOf(this)
    else listOf()
}

private val Path.isJarFile: Boolean
    get() = isRegularFile() && extension.equals("jar", ignoreCase = true)

private val proxyClassLoader: ClassLoader by lazy {
        val appClasspath: Array<URL> = (
            System.getProperty("java.class.path").split(File.pathSeparatorChar).mapNotNull { if (it.isNotBlank()) Path(it) else null }
                    + Path(brailleblasterPath, "lib").listJars()
                    + Path(brailleblasterPath, "native", "${os.name}-${arch.name}".lowercase(), "lib").listJars()
            ).mapNotNull { p ->
                try {
                    p.takeIf { it.exists() }?.toUri()?.toURL()
                } catch (_: MalformedURLException) {
                    null
                }
        }.toTypedArray()
    URLClassLoader("BBClassLoader", appClasspath, ClassLoader.getPlatformClassLoader())
}

@Suppress("UNUSED")
class BBForkJoinWorkerThreadFactory : ForkJoinWorkerThreadFactory {
    override fun newThread(pool: ForkJoinPool): ForkJoinWorkerThread {
        return BBForkJoinWorkerThread(pool, proxyClassLoader)
    }
}

private class BBForkJoinWorkerThread(pool: ForkJoinPool, classLoader: ClassLoader) : ForkJoinWorkerThread(pool) {
    init {
        contextClassLoader = classLoader
    }
}

val brailleblasterPath: String =
    (System.getenv("BBLASTER_WORK") ?: System.getProperty("org.brailleblaster.  distdir") ?: System.getProperty(
        "app.dir",
        ""
    )).let { url ->
        if (url.isNotBlank()) {
            File(url).absoluteFile
        } else {
            val jarFile = File(ProxyMain::class.java.protectionDomain.codeSource.location.toURI()).absoluteFile
            if (!jarFile.isDirectory) jarFile.parentFile else jarFile
        }
    }.absolutePath
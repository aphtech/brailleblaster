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
@file:JvmName("VersionInfo")
package org.brailleblaster.userHelp

import com.sun.jna.Platform
import org.apache.commons.lang3.SystemUtils
import org.brailleblaster.AppProperties
import org.mwhapples.jlouis.Louis
import java.io.IOException
import java.util.*

/**
 * Loads build properties file Jenkins inserts into the jar files before distributing in the maven
 * repository
 */
private val ALL_PROJECTS = listOf(Project.BB, Project.LIBLOUIS)

val versionsSimple: String
    get() = createSummaryString { obj: Project -> generateVersionsShort(obj) }
private val LIBLOUIS_INSTANCE = Louis()
private fun createSummaryString(generator: Generator): String = (ALL_PROJECTS.map { curProj ->
    try {
        generator.generate(curProj)
    } catch (e: Exception) {
        throw RuntimeException("Failed to load project " + curProj.name, e)
    }
} + listOf(javaVersion, oSVersion)).joinToString(separator = System.lineSeparator())

val javaVersion: String
    get() = "Java ${System.getProperty("java.version")} ${if (Platform.is64Bit()) "64-bit" else "32-bit"} ${Locale.getDefault()}"

val oSVersion: String
    get() = "${SystemUtils.OS_NAME} Version ${SystemUtils.OS_VERSION}"

private fun generateVersionsShort(project: Project): String {
    val date = project.date
    val builtOn = if (date != null) " built on $date" else ""
    return "${project.displayName}: ${project.version}${builtOn}"
}

sealed interface Project {
    data object BB : Project {
        override val displayName: String
            get() = AppProperties.displayName
        override val version: String
            get() = AppProperties.version
        override val versionWithRev: String
            get() = AppProperties.buildHash?.let { "${AppProperties.version} $it"} ?: AppProperties.version
        override val date: String
            get() = AppProperties.buildDate
    }

    data object LIBLOUIS : Project {
        override val displayName: String = "LibLouis"
        override val version: String by lazy {
            LIBLOUIS_INSTANCE.version
        }
        override val versionWithRev: String
            get() = version
        override val date: String? = null
    }

    val name: String
        get() = javaClass.simpleName
    val displayName: String
    val version: String
    val date: String?
    val versionWithRev: String
}

private fun interface Generator {
    @Throws(IOException::class)
    fun generate(project: Project): String?
}

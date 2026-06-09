/*
 * Copyright (C) 2025 Michael Whapples
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
package org.brailleblaster

import org.brailleblaster.utils.BBData
import java.nio.file.Path
import java.util.Properties
import kotlin.io.path.exists
import kotlin.io.path.reader

object AppProperties {
    private const val ABOUT_PROPERTIES = "about.properties"

    private fun loadProperties(): Properties {
        val properties = Properties()
        val candidatePaths = linkedSetOf<Path>()

        runCatching {
            candidatePaths.add(BBIni.bbDistPath.resolve(ABOUT_PROPERTIES))
        }
        candidatePaths.add(BBData.brailleblasterPath.toPath().resolve(ABOUT_PROPERTIES))

        for (path in candidatePaths) {
            if (!path.exists()) {
                continue
            }
            runCatching {
                path.reader(Charsets.UTF_8).use { properties.load(it) }
                return properties
            }
        }

        return properties
    }

    private fun property(name: String): String? = loadProperties().getProperty(name)

    val displayName: String
        get() = property("app.display-name") ?: "BrailleBlaster"
    val description: String
        get() = property("app.description") ?: displayName
    val version: String
        get() = property("app.version") ?: "Unknown"
    val vendor: String
        get() = property("app.vendor") ?: "Unknown"
    val buildDate: String
        get() = property("app.build-date") ?: "Unknown"
    @Suppress("Unused")
    val fsname: String
        get() = property("app.fsname") ?: "brailleblaster"
    val buildHash: String?
        get() = property("app.build-hash")
    @Suppress("Unused")
    val vcsUrl: String
        get() = property("app.vcs-url") ?: "https://github.com/aphtech/brailleblaster"
    @Suppress("Unused")
    val downloadUrl: String
        get() = property("app.site.base-url") ?: "https://github.com/aphtech/brailleblaster/releases/latest"
    val websiteUrl: String
        get() = property("app.website-url") ?: "https://www.brailleblaster.org"
}
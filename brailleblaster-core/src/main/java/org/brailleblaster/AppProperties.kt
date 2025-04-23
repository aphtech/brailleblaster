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

import java.util.Properties
import kotlin.io.path.reader

object AppProperties {
    private val properties: Properties = Properties().apply {
        runCatching {
            BBIni.bbDistPath.resolve("about.properties").reader(Charsets.UTF_8).use { load(it) }
        }
    }
    val displayName = properties.getProperty("app.display-name") ?: "BrailleBlaster"
    val description = properties.getProperty("app.description") ?: displayName
    val version = properties.getProperty("app.version") ?: "Unknown"
    val vendor = properties.getProperty("app.vendor") ?: "Unknown"
    val buildDate = properties.getProperty("app.build-date") ?: "Unknown"
    val fsname = properties.getProperty("app.fsname") ?: "brailleblaster"
    val buildHash: String? = properties.getProperty("app.build-hash")
    val vcsUrl = properties.getProperty("app.vcs-url") ?: "https://github.com/aphtech/brailleblaster"
    val downloadUrl = properties.getProperty("app.site.base-url") ?: "https://github.com/aphtech/brailleblaster/releases/latest"
    val websiteUrl = properties.getProperty("app.website-url") ?: "https://www.brailleblaster.org"
}
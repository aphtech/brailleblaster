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
package org.brailleblaster.ebraille

import java.io.File
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.notExists
import kotlin.io.path.writeText

class EBrailleBundle(file: File) : AutoCloseable {
    private val fs: FileSystem = FileSystems.newFileSystem(URI.create("jar:" + file.toURI().toString()), mapOf("create" to "true", "compressionMethod" to "STORED")).apply {
        if (this.rootDirectories.all { p ->
            p.listDirectoryEntries().isEmpty()
        }) {
            val mimetype = getPath("/mimetype")
            mimetype.writeText("application/epub+zip", charset = Charsets.UTF_8)
        }
    }
    private val volumesPath = fs.getPath("/ebraille").apply {
        if (notExists()) {
            createDirectories()
        }
    }
    fun addVolume(name: Path, content: String) {
        require(!name.isAbsolute)
        val volPath = volumesPath.resolve(name)
        volPath.writeText(content, charset = Charsets.UTF_8)
    }
    override fun close() {
        fs.close()
    }
}
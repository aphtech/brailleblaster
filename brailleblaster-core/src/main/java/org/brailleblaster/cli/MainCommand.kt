/*
 * Copyright (C) 2026 American Printing House for the Blind
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
package org.brailleblaster.cli

import org.brailleblaster.Main
import picocli.CommandLine
import java.nio.file.Path
import java.util.concurrent.Callable

@CommandLine.Command(versionProvider = VersionProvider::class, description = ["Launches the application in GUI mode"])
class MainCommand : Callable<Int> {
    @CommandLine.Option(names = ["-h", "--help"], usageHelp = true, description = ["display this help message"])
    var helpRequested = false
    @CommandLine.Option(names = ["-v", "--version"], versionHelp = true, description = ["display version info"])
    var versionInfoRequested = false
    @CommandLine.Option(names = ["--debug"], hidden = true)
    var debugArgs: String = ""
    @CommandLine.Parameters(paramLabel = "<input-file>", index = "0", description = ["The file to open"], defaultValue = CommandLine.Option.NULL_VALUE)
    var inputFile: Path? = null
    override fun call(): Int {
        return Main.start(inputFile, debugArgs.split(',').dropLastWhile { it.isEmpty() }.toList())
    }
}
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
package org.brailleblaster.utd.cli

import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import nu.xom.Document
import nu.xom.ParsingException
import org.brailleblaster.utd.*
import org.brailleblaster.utd.config.DocumentUTDConfig
import org.brailleblaster.utd.config.UTDConfig
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.internal.xml.XMLHandler.Formatted
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import kotlin.system.exitProcess

class File2BRF(
        pageSettings: File,
        brailleSettings: File,
        styleDefinitions: File,
        mappingsDir: File,
        mappingsPrefix: String,
        jlouisPath: File?,
        unicode: Boolean,
        debug: Boolean) {
    private class CLI {
        @Parameter(names = ["-bb", "--brailleblaster"], description = "Path to BrailleBlaster's dist/programData folder to autoload settings from. "
                + "Overrides all other config file options")
        var bbProgramPath: String? = null

        @Parameter(names = ["--pageSettings"])
        var pageSettingsPath: String? = null

        @Parameter(names = ["--brailleSettings"])
        var brailleSettingsPath: String? = null

        @Parameter(names = ["--styleDefinitions"])
        var styleDefinitionsPath: String? = null

        @Parameter(names = ["--mappingsDir"])
        var mappingsDir: String? = null

        @Parameter(names = ["--mappingsPrefix"])
        var mappingsPrefix: String? = null

        @Parameter(names = ["--jlouisTables"], description = "Optionally change jlouis tables path")
        var jlouisTablesPath: String? = null

        @Parameter(names = ["--utdOut"], description = "Optionally output translated UTD to file")
        var utdOut: String? = null

        @Parameter(arity = 2, description = "<untranslated file> <output brl file>", required = true)
        var io: List<String>? = null

        @Parameter(names = ["-h", "--help"], help = true)
        var help = false

        @Parameter(names = ["--unicode"], description = "Optionally output unicode braille instead of ascii")
        var unicode = false

        @Parameter(names = ["--debug"], description = "Optionally replace unset characters with . instead of space")
        var debug = false
        val isEngineConfigured: Boolean
            get() = (!bbProgramPath.isNullOrBlank())
                    || listOf(
                    pageSettingsPath,
                    brailleSettingsPath,
                    styleDefinitionsPath,
                    mappingsDir,
                    mappingsPrefix).all{ !it.isNullOrBlank() }

    }

    private val engine: UTDTranslationEngine
    private val xmlHandler: XMLHandler = Formatted()
    fun raw2UTD(rawBook: File): Document {
        val doc = xmlHandler.load(rawBook)
        var docActionMap = DocumentUTDConfig.NIMAS.loadActions(doc)
        if (docActionMap == null) {
            docActionMap = ActionMap()
        }
        var docStyleMap = DocumentUTDConfig.NIMAS.loadStyle(doc, engine.styleDefinitions)
        if (docStyleMap == null) {
            docStyleMap = StyleMap()
        }
        docActionMap.namespaces = engine.actionMap.namespaces
        docStyleMap.namespaces = engine.styleMap.namespaces
        val multiActionMap = ActionMultiMap()
        multiActionMap.maps = listOf(
                OverrideMap.generateOverrideActionMap(engine.actionMap as ActionMap),
                docActionMap,
                engine.actionMap)
        engine.actionMap = multiActionMap
        val multiStyleMap = StyleMultiMap(engine.styleMap.defaultValue)
        multiStyleMap.maps = listOf(
                OverrideMap.generateOverrideStyleMap(engine.styleDefinitions),
                docStyleMap,
                engine.styleMap)
        engine.styleMap = multiStyleMap
        return engine.translateAndFormatDocument(doc)
    }

    @Throws(IOException::class, ParsingException::class)
    fun raw2UTDFile(rawBook: File, utdOutput: File): Document {
        val result = raw2UTD(rawBook)
        xmlHandler.save(result, utdOutput)
        return result
    }

    @Throws(Exception::class)
    fun utd2brf(utdDocument: Document?, brfOutputFile: File?, unicode: Boolean) {
        val opts = if (unicode) BRFWriter.OPTS_OUTPUT_UNICODE else BRFWriter.OPTS_DEFAULT
        engine.toBRF(utdDocument!!, brfOutputFile!!, opts, BRFWriter.EMPTY_PAGE_LISTENER)
    }

    companion object {
        private val log = LoggerFactory.getLogger(File2BRF::class.java)
        @JvmStatic
        fun main(args: Array<String>) {
            try {
                //			args = new String[] {
                //				"-bb",
                //				"../brailleblaster/dist/programData",
                //				"/home/leon/projects/aph/linuxdev/tabletest.xml",
                //				"out.brf"
                //			};
                val cli = CLI()
                val cmdLine = JCommander(cli)
                cmdLine.programName = File2BRF::class.java.name
                cmdLine.parse(*args)
                if (cli.help) {
                    cmdLine.usage()
                    exitProcess(0)
                }
                if (!cli.isEngineConfigured) {
                    System.err.println("Must pass --bb or configure all engine options")
                    exitProcess(1)
                }
                val converter: File2BRF
                val bbProgramPath = cli.bbProgramPath?:""
                val mappingsDir = cli.mappingsDir?:""
                val mappingsPrefix = cli.mappingsPrefix?:""
                converter = if (bbProgramPath.isNotBlank()) {
                    val bbProgramFile = File(bbProgramPath)
                    if (!bbProgramFile.exists()) throw RuntimeException("Unable to find $bbProgramFile")
                    log.debug("BB program data {}", bbProgramFile)
                    val utdDir = File(bbProgramFile, "utd")
                    File2BRF(
                            File(utdDir, "pageSettings.xml"),
                            File(utdDir, "UEB.brailleSettings.xml"),
                            File(utdDir, "styleDefs.xml"),
                            if (mappingsDir.isNotBlank()) File(mappingsDir) else utdDir,
                            mappingsPrefix.ifBlank { "nimas" },
                            bbProgramFile,
                            cli.unicode,
                            cli.debug)
                } else {
                    File2BRF(
                            File(cli.pageSettingsPath!!),
                            File(cli.brailleSettingsPath!!),
                            File(cli.styleDefinitionsPath!!),
                            File(mappingsDir),
                            mappingsPrefix,
                            File(cli.jlouisTablesPath!!),
                            cli.unicode,
                            cli.debug)
                }

                // Translate raw book to UTD
                val utdBook: Document
                val inputBook = File(cli.io!![0])
                val utdOut = cli.utdOut
                utdBook = if (utdOut.isNullOrBlank()) converter.raw2UTD(inputBook) else converter.raw2UTDFile(inputBook, File(utdOut))

                // Translate UTD to BRF
                converter.utd2brf(utdBook, File(cli.io!![1]), cli.unicode)
            } catch (e: Exception) {
                log.error("Exception encountered in File2BRF", e)
            }
        }
    }

    init {
        if (jlouisPath != null) System.setProperty("jlouis.data.path", jlouisPath.absolutePath)
        engine = UTDTranslationEngine()
        engine.pageSettings = UTDConfig.loadPageSettings(pageSettings)
        engine.brailleSettings = UTDConfig.loadBrailleSettings(brailleSettings)
        engine.styleDefinitions = UTDConfig.loadStyleDefinitions(styleDefinitions)
        UTDConfig.loadMappings(engine, mappingsDir, mappingsPrefix)

        // As this is creating the engine don't need to worry about anything else
        engine.brailleSettings.isUseAsciiBraille = !unicode
    }
}
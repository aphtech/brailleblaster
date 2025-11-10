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
import com.beust.jcommander.ParameterException
import org.brailleblaster.utd.UTDTranslationEngine
import org.brailleblaster.utd.config.UTDConfig
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.mwhapples.jlouis.JarResolver
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Convert a file into UTD.
 *
 *
 * This is the command line tool for converting a file into UTD. It will be designed in such a way that it may still be possible to use the class from other Java classes, however it is primarily being designed as a command line tool and so may not be optimised for programatic use.
 */
class File2UTD {
    @Parameter(description = "infile outfile", arity = 2)
    private val files: List<String> = ArrayList()
    private lateinit var inFilePath: String
    private lateinit var outFilePath: String

    @Parameter(names = ["-t", "-table"], description = "The table to use for Braille translation")
    private var brlTable = "en-us-g2.ctb"

    @Parameter(names = ["-A", "-actionsFile"], description = "Actions file for processing document")
    private var actionsFile: String? = null

    @Parameter(names = ["-S", "-stylePath"], description = "Style file for processing document")
    private var stylePath: String? = null

    @Parameter(names = ["-sd", "-styleDefsPath"], description = "Style definitions file for processing document")
    private var styleDefsPath: String? = null

    @Parameter(names = ["-a", "-asciiBraille"], description = "Use ASCII Braille in brl nodes")
    private var useAsciiBraille = false

    @Parameter(names = ["-h", "-help"], help = true, description = "Show this help")
    private val help = false

    private constructor()

    constructor(
        inFilePath: String,
        outFilePath: String,
        brlTable: String?,
        actionsFile: String?,
        stylePath: String?,
        styleDefsPath: String?,
        useAsciiBraille: Boolean
    ) {
        this.inFilePath = inFilePath
        this.outFilePath = outFilePath
        this.brlTable = brlTable ?: this.brlTable
        this.actionsFile = actionsFile ?: this.actionsFile
        this.stylePath = stylePath ?: this.stylePath
        this.styleDefsPath = styleDefsPath ?: this.styleDefsPath
        this.useAsciiBraille = useAsciiBraille
    }

    fun run() {
        val inFile = File(inFilePath)
        val outFile = File(outFilePath)


        //Verify file exists as no other code does sanity checking
        if (!inFile.exists()) {
            throw RuntimeException("Input file $inFile does not exist")
        }
        if (outFile.exists()) {
            logger.warn("Overwriting output file $outFile")
            if (!outFile.delete()) {
                throw RuntimeException(String.format("Output file %s exists but cannot be overwritten", outFile.name))
            }
        }
        val brlTableFiles = JarResolver().resolveTable(this.brlTable, null)
        if (brlTableFiles.isNotEmpty() && !brlTableFiles[0].exists()) {
            throw RuntimeException("Table $brlTable not found")
        }

        val xmlHandler = XMLHandler()
        val doc = xmlHandler.load(inFile)

        val context = UTDTranslationEngine()


        //Load config
        actionsFile?.let {
            context.actionMap = UTDConfig.loadActions(File(it))!!
        }
        styleDefsPath?.let {
            context.styleDefinitions = UTDConfig.loadStyleDefinitions(File(it))
        }
        stylePath?.let {
            context.styleMap = UTDConfig.loadStyle(File(it), context.styleDefinitions)!!
        }

        context.brailleSettings.mainTranslationTable = brlTable
        context.brailleSettings.isUseAsciiBraille = useAsciiBraille
        val result = context.translateAndFormatDocument(doc)

        xmlHandler.save(result, outFile)
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(File2UTD::class.java)

        /**
         * The main function for File2UTD.
         *
         * @param args The command line arguments.
         */
        @JvmStatic
        fun main(args: Array<String>) {
            val app = File2UTD()
            val cmdLine = JCommander(app)
            cmdLine.programName = "File2UTD"
            try {
                cmdLine.parse(*args)
            } catch (e: ParameterException) {
                System.err.println(e.message)
                cmdLine.usage()
                return
            }
            if (app.help) {
                cmdLine.usage()
                return
            }
            if (app.files.size != 2) {
                System.err.println("You need to give both an input and output file")
                cmdLine.usage()
                return
            }
            app.inFilePath = app.files[0]
            app.outFilePath = app.files[1]
            app.run()
        }
    }
}

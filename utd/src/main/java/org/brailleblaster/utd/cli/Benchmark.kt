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
import com.google.common.base.Objects
import nu.xom.Document
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.commons.lang3.time.StopWatch
import org.brailleblaster.utd.PageSettings
import org.brailleblaster.utd.UTDTranslationEngine
import org.brailleblaster.utd.config.UTDConfig
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utils.MoreFileUtils.newFileIncrimented
import org.brailleblaster.utils.MoreFileUtils.newReaderUTF8
import org.brailleblaster.utils.MoreFileUtils.newWriterUTF8
import org.brailleblaster.libembosser.spi.BrlCell
import org.slf4j.LoggerFactory
import java.io.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.system.exitProcess

/**
 * Benchmark UTD loading a book and saving to BRF
 */
object Benchmark {
    private val log = LoggerFactory.getLogger(Benchmark::class.java)
    private val xmlHandler = XMLHandler()

    //Source: https://en.wikipedia.org/wiki/Braille_ASCII#Braille_ASCII_values
    //It's uppercase though, liblouis uses the lowercase equivelant
    private val ASCII_BRAILLE =
        " A1B'K2L@CIF/MSP\"E3H9O6R^DJG>NTQ,*5<-U8V.%[$+X!&;:4\\0Z7(_?W]#Y)=".replace('[', '{').replace(']', '}')
            .replace('\\', '|').replace('^', '~').replace('@', '`').lowercase(Locale.getDefault())

    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        dumpArgs(args)
        val cli = UTDCLI()
        val cmd = JCommander(cli)
        cmd.programName = "UTD Benchmarker"
        try {
            cmd.parse(*args)
        } catch (e: Exception) {
            System.err.println(e.message)
            cmd.usage()
            exitProcess(1)
        }
        if (!cli.bbProgramData!!.exists()) {
            System.err.println("Unable to find BrailleBlaster at " + cli.bbProgramData)
            exitProcess(3)
        }
        log.debug("BB program data {}", cli.bbProgramData)
        if (!cli.inputFile!!.exists()) {
            System.err.println("Unable to find book at " + cli.inputFile)
            exitProcess(4)
        }
        log.debug("Book {}", cli.inputFile)

        //Everything is valid, begin test
        val totalWatch = StopWatch()
        try {
            totalWatch.start()
            val initWatch = StopWatch()
            val engine: UTDTranslationEngine
            try {
                initWatch.start()
                engine = setup(cli.bbProgramData, cli)
                initWatch.stop()
            } finally {
                if (initWatch.nanoTime != 0L) printDebugResult(" - Init in $initWatch")
            }
            val xmlWatch = StopWatch()
            val doc: Document
            try {
                xmlWatch.start()
                doc = loadFile(cli.inputFile)
                xmlWatch.stop()
            } finally {
                if (xmlWatch.nanoTime != 0L) printDebugResult(" - XML loaded in $xmlWatch")
            }

            //Split up so stack traces can be understood without looking at line numbers
            val firstBRFFile = part1BenchAscii(engine, doc)
            part2BenchUnicode(engine, doc, firstBRFFile)
            part3BenchUnicodeFresh(engine, cli, firstBRFFile)
        } catch (e: Throwable) {
            log.error("Failed to benchmark", e)
            printDebugResult("Failed! " + ExceptionUtils.getMessage(e))
            //Print all the causes, skipping the first as its above
            var throwableList = ExceptionUtils.getThrowableList(e)
            throwableList = throwableList.subList(1, throwableList.size)
            for (t in throwableList) printDebugResult("Cause: " + ExceptionUtils.getMessage(t))
            newWriterUTF8("error.txt").use { writer -> writer.write(ExceptionUtils.getStackTrace(e)) }
        } finally {
            printDebugResult("Total execution in $totalWatch")
            newWriterUTF8("results.properties").use { writer ->
                val bookFilename = cli.inputFile!!.name
                val bookName = Path(bookFilename).nameWithoutExtension
                writer.write(bookName + "=" + totalWatch.time / 1000)
            }
        }
    }

    @Throws(Exception::class)
    fun part1BenchAscii(engine: UTDTranslationEngine, doc: Document): File {
        printDebugResult("Starting benchmark in ascii")
        engine.brailleSettings.isUseAsciiBraille = true
        val firstBRFFileOrig = runBenchmark(doc, engine)
        val firstBRFFile = File(firstBRFFileOrig.parent, firstBRFFileOrig.name + ".ascii")
        Files.move(firstBRFFileOrig.toPath(), firstBRFFile.toPath())
        return firstBRFFile
    }

    @Throws(Exception::class)
    fun part2BenchUnicode(engine: UTDTranslationEngine, doc: Document, firstBRFFile: File) {
        printDebugResult("Starting translate in unicode")
        engine.brailleSettings.isUseAsciiBraille = false
        val secondBRFFile = runBenchmark(doc, engine)
        compareUnicodeToAsciiBRF(firstBRFFile, secondBRFFile)
    }

    @Throws(Exception::class)
    fun part3BenchUnicodeFresh(engine: UTDTranslationEngine, cli: BenchmarkCLI, firstBRFFile: File) {
        printDebugResult("Starting translate in unicode on fresh document")
        engine.brailleSettings.isUseAsciiBraille = false
        val doc = loadFile(cli.inputFile)
        val thirdBRFFile = runBenchmark(doc, engine)
        compareUnicodeToAsciiBRF(firstBRFFile, thirdBRFFile)
    }

    /**
     * Translate, format, toBRF, format again, toBRF, compare toBRFs
     *
     * @return 2nd BRF output
     */
    @Throws(Exception::class)
    fun runBenchmark(document: Document, engine: UTDTranslationEngine): File {
        var doc = document
        val translateWatch = StopWatch()
        val formatWatch = StopWatch()
        val format2Watch = StopWatch()
        val outputWatch = StopWatch()
        val output2Watch = StopWatch()
        return try {
            translateWatch.start()
            doc = translateFile(engine, doc)
            translateWatch.stop()
            formatWatch.start()
            doc = formatFile(engine, doc)
            formatWatch.stop()
            outputWatch.start()
            var firstOutput = output(engine, doc)
            outputWatch.stop()

            //Do again to test reformatting
            val origFirstOutput = firstOutput
            firstOutput = File(firstOutput.parentFile, firstOutput.name + ".first")
            Files.move(origFirstOutput.toPath(), firstOutput.toPath())
            format2Watch.start()
            val nextDoc = formatFile(engine, doc)
            format2Watch.stop()
            output2Watch.start()
            val secondOutput = output(engine, nextDoc)
            output2Watch.stop()
            newReaderUTF8(firstOutput).use { expectedReader ->
                newReaderUTF8(secondOutput).use { givenReader ->
                    compareStreamReaders(
                        expectedReader,
                        firstOutput,
                        givenReader,
                        secondOutput
                    )
                }
            }
            Files.delete(firstOutput.toPath())
            //			Files.delete(secondOutput.toPath());
            secondOutput
        } finally {
            if (translateWatch.nanoTime != 0L) printDebugResult(" - Translated in $translateWatch")
            if (formatWatch.nanoTime != 0L) printDebugResult(" - Formatted in $formatWatch")
            if (outputWatch.nanoTime != 0L) printDebugResult(" - To BRF in $outputWatch")
            if (formatWatch.nanoTime != 0L) printDebugResult(" - 2nd Formatted in $formatWatch")
            if (outputWatch.nanoTime != 0L) printDebugResult(" - 2nd To BRF in $outputWatch")
        }
    }

    @Throws(IOException::class)
    fun compareUnicodeToAsciiBRF(expectedFile: File, givenFile: File) {
        newReaderUTF8(expectedFile).use { expectedReader ->
            FileInputStream(givenFile).use { givenInput ->
                //Convert unicode output back into ascii
                val givenReader = BufferedReader(object : InputStreamReader(givenInput, StandardCharsets.UTF_8) {
                    @Throws(IOException::class)
                    override fun read(cbuf: CharArray, offset: Int, length: Int): Int {
                        val readResult = super.read(cbuf, offset, length)
                        for (i in offset until offset + length) {
                            var orig = cbuf[i]
                            if (orig in ('\u2800'..'\u287F')) {
                                if (orig >= '\u2840') {
                                    //8-dot braille for un-needed capitalization, throw away bits 7 and 8
                                    val start = orig
                                    orig = (orig.code and 0xC0.inv()).toChar()
                                    log.debug(
                                        "Converted " + Character.getName(start.code)
                                                + " to " + Character.getName(orig.code)
                                                + " ascii " + ASCII_BRAILLE[orig.code - '\u2800'.code]
                                    )
                                }
                                val asciiBrailleOffset = orig.code - '\u2800'.code
                                cbuf[i] = ASCII_BRAILLE[asciiBrailleOffset]
                                //hack for translated Unicode characters in the format '\x1234'
                                if (cbuf[i] == '|' && i >= offset + 1 && i <= offset + length - 1 && cbuf[i - 1] == '\'' && cbuf[i + 1] == '\u282D' /*x*/) {
                                    cbuf[i] = '\\'
                                }
                            }
                        }
                        return readResult
                    }
                })
                compareStreamReaders(expectedReader, expectedFile, givenReader, givenFile)
            }
        }
    }

    @Throws(IOException::class)
    fun compareStreamReaders(
        expectedReader: BufferedReader, expectedFile: File,
        givenReader: BufferedReader, givenFile: File
    ) {
        var counter = 0
        while (true) {
            counter++
            val expectedLine = expectedReader.readLine()
            val givenLine = givenReader.readLine()
            if (expectedLine == null || givenLine == null) {
                log.debug("Files Equal")
                break
            }
            if (!Objects.equal(expectedLine, givenLine)) {
                throw AssertionError(
                    "Text not equal on line " + counter
                            + " for files expected " + expectedFile.name + " given " + givenFile.name
                            + System.lineSeparator() + "Expected |" + expectedLine + "|"
                            + System.lineSeparator() + "Given    |" + givenLine + "|"
                )
            }
        }
    }

    @Throws(Exception::class)
    fun setup(bbProgramDataPath: File?, cli: BenchmarkCLI): UTDTranslationEngine {
        val utdDir = File(bbProgramDataPath, "utd")
        if (!utdDir.exists()) {
            throw RuntimeException("Cannot find utd folder at " + utdDir.absolutePath)
        }

        //Going to use UTD defaults
        val engine = UTDTranslationEngine()
        engine.styleDefinitions = UTDConfig.loadStyleDefinitions(File(utdDir, "styleDefs.xml"))
        engine.shortcutDefinitions = UTDConfig.loadShortcutDefinitions(File(utdDir, "shortcutDefs.xml"))
        UTDConfig.loadMappings(engine, utdDir, "bbx")
        engine.brailleTranslator.dataPath = File(bbProgramDataPath, "org/mwhapples/jlouis/tables/").absolutePath
        log.info("liblouis data path: " + engine.brailleTranslator.dataPath)

        //Braille standard
        val brailleSettingsFile =
            File(utdDir, cli.brailleStandard!!.uppercase(Locale.getDefault()) + ".brailleSettings.xml")
        require(brailleSettingsFile.exists()) {
            ("Unknown braille standard " + cli.brailleStandard
                    + ", unable to find " + brailleSettingsFile.absolutePath)
        }
        log.debug("Using braille standard {}", brailleSettingsFile)
        engine.brailleSettings = UTDConfig.loadBrailleSettings(brailleSettingsFile)
        return engine
    }

    fun setupEngine(engine: UTDTranslationEngine, cli: BenchmarkCLI) {
        //Optionally add running head
        if (!cli.runningHead.isNullOrBlank()) {
            log.debug("Using running head " + cli.runningHead)
            engine.pageSettings.runningHead = cli.runningHead!!
        }

        //Optionally change page size
        val cell = engine.brailleSettings.cellType
        if (cli.linesPerPage > 0) {
            setNewPageHeight(engine.pageSettings, cell, cli.linesPerPage)
        }
        if (cli.cellsPerLine > 0) {
            setNewPageWidth(engine.pageSettings, cell, cli.cellsPerLine)
        }
    }

    @JvmStatic
	fun setNewPageHeight(pageSettings: PageSettings, cell: BrlCell, heightLines: Int) {
        pageSettings.paperHeight =
            (heightLines * cell.height.toDouble() + pageSettings.topMargin + pageSettings.bottomMargin)
        val newDrawableLines = cell.getLinesForHeight(
            BigDecimal(pageSettings.drawableHeight).setScale(0, RoundingMode.UP))
        if (newDrawableLines != heightLines) {
            throw RuntimeException(
                "Set drawable height to " + pageSettings.drawableHeight
                        + " but got " + newDrawableLines + " instead of requested " + heightLines
            )
        }
    }

    @JvmStatic
	fun setNewPageWidth(pageSettings: PageSettings, cell: BrlCell, widthCells: Int) {
        pageSettings.paperWidth =
            (widthCells * cell.width.toDouble() + pageSettings.leftMargin + pageSettings.rightMargin)
        val newDrawableLines = cell.getCellsForWidth(
            BigDecimal(pageSettings.drawableWidth).setScale(0, RoundingMode.UP))
        if (newDrawableLines != widthCells) {
            throw RuntimeException(
                "Set drawable height to " + pageSettings.drawableWidth
                        + " but got " + newDrawableLines + " instead of requested " + widthCells
            )
        }
    }

    @JvmStatic
	fun dumpArgs(args: Array<String>) {
        println("---- Given Arguments ---")
        for (arg in args) {
            println(arg)
        }
        println("---- End Arguments ---")
    }

    fun loadFile(bookFile: File?): Document {
        return xmlHandler.load(bookFile)
    }

    private fun translateFile(engine: UTDTranslationEngine, doc: Document): Document {
        return engine.translateDocument(doc)
    }

    private fun formatFile(engine: UTDTranslationEngine, doc: Document): Document {
        return engine.format(doc)
    }

    @Throws(IOException::class)
    fun output(engine: UTDTranslationEngine, doc: Document): File {
        val fileName = Paths.get(URI.create(doc.baseURI)).fileName
            ?: throw RuntimeException("Unable to get document's file name, document seems to have zero path elements in its URI")
        val docFileName = fileName.nameWithoutExtension
        val outputFile = newFileIncrimented(File("."), "benchmark_$docFileName", ".brf")
        engine.toBRF(doc, outputFile)
        return outputFile
    }

    @JvmStatic
	fun printDebugResult(message: String) {
        println("BENCHRESULT: $message")
    }

    open class BenchmarkCLI {
        @JvmField
		@Parameter(names = ["--input"], required = true)
        var inputFile: File? = null

        @JvmField
		@Parameter(names = ["--brailleStandard"], required = true)
        var brailleStandard: String? = null

        @JvmField
		@Parameter(names = ["--runningHead"])
        var runningHead: String? = null

        @JvmField
		@Parameter(names = ["--linesPerPage"])
        var linesPerPage = 0

        @JvmField
		@Parameter(names = ["--cellsPerLine"])
        var cellsPerLine = 0
        override fun toString(): String {
            return "BenchmarkCLI{inputFile=$inputFile," +
                " brailleStandard=$brailleStandard," +
                " runningHead=$runningHead," +
                " linesPerPage=$linesPerPage," +
                " cellsPerLine=$cellsPerLine}"
        }
    }

    private class UTDCLI : BenchmarkCLI() {
        @Parameter(names = ["--bb"], required = true)
        var bbProgramData: File? = null
    }
}
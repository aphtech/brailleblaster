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
package org.brailleblaster.archiver2

import org.brailleblaster.BBIni
import org.brailleblaster.perspectives.braille.Manager
import org.brailleblaster.util.Notify.showException
import org.eclipse.swt.widgets.Display
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension


/**
 * Handles saving a file
 */
class ArchiverRecoverThread(private val m: Manager) {
    private var handler: ScheduledFuture<*>? = null
    private var firstOpen = true

    //Add file to autoSave
    private fun addFile() {
        val fileName = m.archiver.path.fileName
        //println("Adding $fileName (ID $currentId) to Autosave")
        if (firstOpen) {
            val oldAutoSavePath = m.archiver.path.toString()
            if (oldAutoSavePath.contains(BBIni.autoSavePath.toString())) {
                saveAutoSaveFile(fileName)
                removeSavedFile(Path(oldAutoSavePath))
                firstOpen = false
                return
            }
            firstOpen = false
        }

        if (m.text.hasChanged && m.lastCopiedDoc != null && !BBIni.debugging) {
            saveFile(fileName)
        }
    }

    private fun saveFile(path: Path) {
        //println("Saving File $fileName")
        val fileName = fileReName(path)
        val arch = m.archiver
        val engine = m.document.engine
        arch.save(BBIni.autoSavePath.resolve(fileName), m.lastCopiedDoc, engine, emptySet())
        addRecentSave(BBIni.autoSavePath.resolve(fileName))
    }

    private fun saveAutoSaveFile(path: Path) {
        //println("Saving autosaved file $fileName - NOT renaming!")
        val fileName = fileReName(path)
        val arch = m.archiver
        val engine = m.document.engine
        arch.save(BBIni.autoSavePath.resolve(fileName), arch.bbxDocument, engine, emptySet())
        addRecentSave(BBIni.autoSavePath.resolve(fileName))
    }

    fun removeFile() {
        //Remove recovery information from disk
        removeFile(m.archiver.path.fileName)
    }

    //start the auto save process
    fun autoSave(status: Boolean) {
        //First attempt to cancel any existing autosave process
        handler?.cancel(false)
        if (status) {
            handler = scheduler.scheduleWithFixedDelay({
                try {
                    addFile()
                } catch (e: RuntimeException) {
                    Display.getDefault().syncExec { showException(e) }
                }
            }, AUTO_SAVE_MILLISECONDS, AUTO_SAVE_MILLISECONDS, TimeUnit.MILLISECONDS)
        }
    }


    companion object {
        private val log: Logger = LoggerFactory.getLogger(ArchiverRecoverThread::class.java)
        private val dateFormatter = SimpleDateFormat("yyyyMMddHHmmss")
        private val RECENT_SAVE_FILES = readRecentSaves()
        private const val MAX_RECENT_SAVES = 10
        private const val AUTO_SAVE_MILLISECONDS: Long =
            1000 * 60 * 5 // 5-minute timer - might be nice to be user configurable.

        private val scheduler = Executors.newSingleThreadScheduledExecutor()

        fun removeFile(path: Path) {
            //Remove recovery information from disk
            //println("Removing file (override method) $fileName (ID $currentId)")
            val fileName = fileReName(path)
            removeSavedFile(Path(BBIni.autoSavePath.resolve(fileName).toString()))
        }

        private fun removeSavedFile(savedPath: Path) {
            //Remove recovery information from disk
            //println("Removing saved file for $savedPath")
            try {
                Files.deleteIfExists(savedPath)
                deleteRecentSave(savedPath)
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }

        private fun fileReName(path: Path): String {
            //I dislike this system; why not append the datetime to the doc name?
            //Not necessarily pretty, but it'll be unique every time.
            val cal = Calendar.getInstance().time
            val dateTimeString = dateFormatter.format(cal)
            return "${path.nameWithoutExtension} $dateTimeString.${path.extension}"
        }

        @JvmStatic
        val recentSaves: List<Path>
            get() = RECENT_SAVE_FILES

        @JvmStatic
        fun readRecentSaves(): MutableList<Path> {
            try {
                return Files.readAllLines(BBIni.recentSaves, BBIni.charset)
                    .mapNotNull { strPath: String ->
                        try {
                            Path(strPath)
                        } catch (e: InvalidPathException) {
                            // Issue #6844: Do not catastrophically fail on corrupt File
                            log.error("Recent Saves file corrupted at " + BBIni.recentSaves, e)
                            null
                        }
                    }.toMutableList()
                //		} catch (IOException ex) {
//			throw new RuntimeException("Unable to load recent saves" , ex);
//		}
            } catch (e: Exception) {
                //Autosaves are a non-essential feature. Throwing an exception here will render BB unusable.
                showException(e)
                return mutableListOf()
            }
        }

        private fun writeRecentSaves() {
            synchronized(RECENT_SAVE_FILES) {
                try {
                    Files.write(
                        BBIni.recentSaves,
                        RECENT_SAVE_FILES.map { curPath: Path -> curPath.toAbsolutePath().toString() }
                    )
                } catch (e: IOException) {
                    throw RuntimeException("Unable to save recent docs file", e)
                }
            }
        }

        @Synchronized
        fun addRecentSave(path: Path) {
            synchronized(RECENT_SAVE_FILES) {
                RECENT_SAVE_FILES.remove(path)
                while (RECENT_SAVE_FILES.size >= MAX_RECENT_SAVES) {
                    RECENT_SAVE_FILES.removeAt(RECENT_SAVE_FILES.size - 1)
                }
                RECENT_SAVE_FILES.add(0, path)
                writeRecentSaves()
            }
        }

        @Synchronized
        fun deleteRecentSave(path: Path) {
            synchronized(RECENT_SAVE_FILES) {
                RECENT_SAVE_FILES.remove(path)
                writeRecentSaves()
            }
        }
    }
}

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
package org.brailleblaster.util

import org.brailleblaster.BBIni
import org.brailleblaster.utils.OS
import org.brailleblaster.utils.os
import org.slf4j.LoggerFactory
import java.awt.Toolkit
import java.io.BufferedInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import javax.sound.sampled.*

object SoundManager {
    //The horrible beep sound from BB1. Replace with the system default. X-platform if possible.
    private const val SELECTION_BELL = "selection_bell.wav"
    private val log = LoggerFactory.getLogger(SoundManager::class.java)
    fun playSelectionBell() {
        // Sound is unreliable and may deadlock on Linux, see #4438
        // Not sure why not on Mac.
        if (OS.Windows == os) {
            try {
                val audioFile = BBIni.programDataPath.resolve(Paths.get("sounds", SELECTION_BELL))
                val audioStream = AudioSystem.getAudioInputStream(BufferedInputStream(Files.newInputStream(audioFile)))
                val format = audioStream.format
                val info = DataLine.Info(Clip::class.java, format)
                val audioClip = AudioSystem.getLine(info) as Clip
                audioClip.addLineListener(getLineListener(audioClip))
                audioClip.open(audioStream)
                audioClip.start()
            } catch (e: UnsupportedAudioFileException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: LineUnavailableException) {
                e.printStackTrace()
            }
        }
    }

    /*
   * Play a system beep via AWT, for compatibility.
   * TODO: Problems in Windows 11?
   */
    fun playBeep() {
        try {
            //AWT style beep
            Toolkit.getDefaultToolkit().beep()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getLineListener(clip: Clip): LineListener {
        return LineListener { event: LineEvent ->
            val type = event.type
            log.debug("Line type {}", type)
            if (type === LineEvent.Type.STOP) clip.close()
        }
    }
}
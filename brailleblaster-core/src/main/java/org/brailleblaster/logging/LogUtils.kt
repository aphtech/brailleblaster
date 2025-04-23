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
@file:JvmName("LogUtils")
package org.brailleblaster.logging

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import org.apache.commons.lang3.exception.ExceptionUtils
import org.brailleblaster.BBIni
import org.brailleblaster.util.Utils
import org.slf4j.LoggerFactory
import java.io.File

private var loggingInitted = false

/** Point logback to its configuration file in programData/settings  */
fun initLogback(logbackConf: File) {
    val configFile = System.getProperty("logback.configurationFile")
    if (configFile == null) {
        System.setProperty("logback.configurationFile", logbackConf.path)
    }
    loggingInitted = true
}

/**
 * Will log to logback if initted or fall back to console
 *
 * @param clazz
 * @param message
 * @param args
 */
fun preLog(clazz: Class<*>?, message: String?, vararg args: Any?) {
    if (loggingInitted) LoggerFactory.getLogger(clazz).debug(message, *args) else println(
        Utils.formatMessage(
            message,
            *args
        )
    )
}

/**
 * Will log to logback if initted or fall back to console
 *
 * @param clazz
 * @param message
 * @param e
 * @param args
 */
fun preLogException(clazz: Class<*>?, message: String?, e: Throwable,  vararg args: Any?) {
    if (loggingInitted) LoggerFactory.getLogger(clazz).debug(message, *args) else println(
        Utils.formatMessage(
            message,
            *args
        ) + ExceptionUtils.getThrowableCount(e)
    )
}

private var USER_SETTINGS_LEVEL = "loglevel"

@JvmOverloads
fun getLogLevel(loggerName: String = Logger.ROOT_LOGGER_NAME): Level {
    val logger = LoggerFactory.getLogger(loggerName) as Logger
    return logger.level
}

@JvmOverloads
fun setLogLevel(level: Level?, loggerName: String = Logger.ROOT_LOGGER_NAME) {
    val logger = LoggerFactory.getLogger(loggerName) as Logger
    logger.level = level
}

/** Save and refresh log settings.  */
fun updateLogSettings() {
    val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    val level = rootLogger.level
    BBIni.propertyFileManager.save(USER_SETTINGS_LEVEL, level.levelStr)
}
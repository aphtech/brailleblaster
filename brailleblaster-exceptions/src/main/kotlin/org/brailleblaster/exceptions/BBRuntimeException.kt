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
package org.brailleblaster.exceptions

import org.slf4j.helpers.MessageFormatter

class BBRuntimeException(message: String, cause: Throwable?, vararg args: Any?) :
    RuntimeException(format(message, *args), cause) {

    companion object {
        private fun format(messagePattern: String, vararg args: Any?): String {
            return MessageFormatter.arrayFormat(messagePattern, args).message
        }
    }
}
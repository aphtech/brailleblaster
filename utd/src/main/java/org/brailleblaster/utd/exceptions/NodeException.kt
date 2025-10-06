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
package org.brailleblaster.utd.exceptions

import nu.xom.Attribute
import nu.xom.Element
import nu.xom.Node
import nu.xom.ParentNode
import org.brailleblaster.utd.internal.xml.XMLHandler
import org.brailleblaster.utd.utils.LocalEntityResolver
import org.brailleblaster.utils.MoreFileUtils
import java.io.File
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.*

/**
 * Detailed exception handler that saves the document to disk with an attribute on the problem
 * element that can be found with ctrl+f
 */
open class NodeException @JvmOverloads constructor(message: String, node: Node?, cause: Throwable? = null) :
    RuntimeException(
        handleInput(message, node), cause
    ) {
    companion object {
        /** Enable writing documents to disk, default enabled  */
        const val SAVE_TO_DISK_ENABLED_PROPERTY = "nodeexception.save"

        /**
         * Name of system property who's value is the folder to save to or file to overwrite on each
         * Exception. Eg setting to exception.xml will save all exceptions to that file. Default is
         * current directory, in BB it's the user settings folder
         */
        const val SAVE_TO_DISK_FOLDER_PROPERTY = "nodeexception.folder"

        /**
         * Prevent enormous exception messages from being generated if eg node is a section element or the
         * root element
         */
        private const val MAX_NODE_TOXML_CHARACTERS = 200
        const val ATTRIBUTE_NAME = "NodeExceptionID"
        const val FILENAME_PREFIX = "node_exception"
        private fun handleInput(origMessage: String, node: Node?): String {
            var message = origMessage
            when (node) {
                null -> {
                    return "$message [null]"
                }
                is Element -> {
                    val toXML = node.toXML().take(MAX_NODE_TOXML_CHARACTERS) + "...[excess toXML trimmed]"
                    message += " $toXML"
                }
                else -> {
                    message += " $node"
                }
            }

            // When there's no document there's nothing reliable to save to disk or lookup
            if (node.document == null) {
                message += " (node not attached to document)"
                return message
            }
            if (!(System.getProperty(SAVE_TO_DISK_ENABLED_PROPERTY, "true").toBooleanStrict())) {
                message += " (save to disk disabled)"
                return message
            }

            // Add a callback attribute that can be found with ctrl+f)
            var callbackID = UUID.randomUUID().toString()
            // Simplify since it doesn't actually need to be 100% unique for eternity
            callbackID = callbackID.substringBefore('-')
            val callbackAttrib = Attribute(ATTRIBUTE_NAME, callbackID)
            val parentElement: Element
            if (node is ParentNode) {
                parentElement =
                    XMLHandler.parentToElement(node)
                parentElement.addAttribute(callbackAttrib)
                message += " (search for string $callbackID on element"
            } else {
                val parent = node.parent
                parentElement =
                    XMLHandler.parentToElement(parent)
                parentElement.addAttribute(callbackAttrib)
                message += " (search for string " + callbackID + " on parent, child index " + parent.indexOf(node)
            }

            // Save entire document to disk
            val doc = node.document
            var filenameBase = FILENAME_PREFIX
            val origURI = doc.baseURI
            if (origURI.isNotBlank()) {
                val correctedUri = if (origURI.startsWith(LocalEntityResolver.ENCODED_URI_PREFIX)) {
                    URI(URLDecoder.decode(origURI.removePrefix(LocalEntityResolver.ENCODED_URI_PREFIX), StandardCharsets.UTF_8))
                } else {
                    URI.create(origURI)
                }
                val fileName = Paths.get(correctedUri).fileName
                    ?: throw RuntimeException(String.format("No path elements in the URI %s", origURI))
                var uri = fileName.toString()
                // Strip extension
                var extensionIndex = 0
                if (uri.isNotBlank() && uri.lastIndexOf('.').also { extensionIndex = it } != -1) {
                    uri = uri.take(extensionIndex)
                    filenameBase += "_$uri"
                }
            }
            val dest = File(System.getProperty(SAVE_TO_DISK_FOLDER_PROPERTY, "."))
            val outputFile = if (dest.isFile) dest else MoreFileUtils.newFileIncrimented(dest, filenameBase, ".xml")
            XMLHandler.Formatted().save(doc, outputFile)
            message += " in file " + outputFile.absolutePath + " )"
            return message
        }
    }
}
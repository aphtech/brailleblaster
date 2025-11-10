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
package org.brailleblaster.utd.utils

import nu.xom.Builder
import org.apache.commons.io.input.BOMInputStream
import org.brailleblaster.utd.internal.NormaliserFactory
import org.brailleblaster.utils.xml.EntityMap
import org.brailleblaster.utils.xml.LocalEntityMapHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.ext.EntityResolver2
import java.io.IOException
import java.io.StringReader
import java.net.URI
import java.net.URISyntaxException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParserFactory
import javax.xml.parsers.SAXParserFactory.newNSInstance

class LocalEntityResolver : EntityResolver2 {
    override fun resolveEntity(name: String?, publicId: String?, baseUri: String?, systemId: String): InputSource? {
        try {
            return resolveEntityImpl(name, publicId, baseUri, systemId)
        } catch (e: Exception) {
            throw RuntimeException(
                "Name '"
                        + name
                        + "' publicId '"
                        + publicId
                        + "' baseUri '"
                        + baseUri
                        + "' systemId '"
                        + systemId
                        + "'",
                e
            )
        }
    }

    private fun resolveEntityImpl(name: String?, publicId: String?, baseUri: String?, systemId: String): InputSource? {
        log.info(
            "Requested entity name={} publicId={} baseUri={} systemId={}",
            name,
            publicId,
            baseUri,
            systemId
        )
        if (publicId != null && localEntities.containsPublicId(publicId)) {
            return localEntities.getByPublicId(publicId)
        }

        // Issue #5953: Default disable fetching remote as some users do not have internet connections
        val noRemoteEntityLookup = System.getProperty(SETTING_DISABLE_REMOTE_ENTITY, "true") == "true"
        var absoluteSystemId: Boolean
        try {
            val systemIdUri = URI(systemId)
            absoluteSystemId = systemIdUri.isAbsolute
        } catch (_: URISyntaxException) {
            absoluteSystemId = false
        }

        if (baseUri != null && baseUri.startsWith(ENCODED_URI_PREFIX)) {
            try {
                val sourceURI: String
                if (absoluteSystemId) {
                    if (localEntities.containsSystemId(systemId)) {
                        return localEntities.getBySystemId(systemId)
                    } else {
                        sourceURI = systemId
                    }
                } else {
                    val correctedBaseUri =
                        URI(
                            URLDecoder.decode(
                                baseUri.substring(ENCODED_URI_PREFIX.length),
                                StandardCharsets.UTF_8
                            )
                        )
                    val basePath = Paths.get(correctedBaseUri)
                    val sourcePath = basePath.resolveSibling(systemId)
                    sourceURI = sourcePath.toUri().toString()
                }
                // Do not lookup remote entities when settings disable remote lookup.
                if (!noRemoteEntityLookup
                    || sourceURI.startsWith("jar:file:")
                    || sourceURI.startsWith("file:")
                ) {
                    return InputSource(sourceURI)
                }
            } catch (_: URISyntaxException) {
                // Do nothing, we try the fallbacks below.
            }
        }
        if (localEntities.containsSystemId(systemId)) {
            return localEntities.getBySystemId(systemId)
        }

        if (baseUri != null) {
            try {
                val sourcePathUri = URI(baseUri).resolve(systemId)
                val sourcePath = sourcePathUri.toString()
                val localLookup = sourcePath.startsWith("file:") || sourcePath.startsWith("jar:")
                if (!noRemoteEntityLookup || localLookup) {
                    if (!localLookup) {
                        // Complain loudly on console as this means were doing an HTTP call
                        // which may be rate limited on the server (eg w3c hosted XML DTDs)
                        log.error(
                            "No local entity found for publicId={} systemId={} fetching remotely",
                            publicId,
                            systemId
                        )
                        return null
                    }
                    return InputSource(
                        BOMInputStream.builder().setInputStream(Files.newInputStream(Paths.get(sourcePathUri))).get()
                    )
                }
            } catch (_: URISyntaxException) {
                // No problem, fallback.
                log.error("URI of entity is invalid: {}", baseUri)
            } catch (e: IOException) {
                log.error("Problem loading specified entity", e)
            }
        }
        log.error("No entity found for publicId={} systemId={} returning empty", publicId, systemId)
        return InputSource(StringReader(""))
    }

    override fun resolveEntity(publicId: String?, systemId: String): InputSource? {
        return resolveEntity(null, publicId, null, systemId)
    }

    override fun getExternalSubset(name: String, baseURI: String?): InputSource? {
        return null
    }

    companion object {
        /** System property: When "true" will return an empty entity if not found locally  */
        const val SETTING_DISABLE_REMOTE_ENTITY: String = "localEntityResolver.disableRemoteEntity"

        private val log: Logger = LoggerFactory.getLogger(LocalEntityResolver::class.java)
        const val ENCODED_URI_PREFIX: String = "jar-utd-encoded://"
        private lateinit var localEntities: EntityMap

        init {
            try {
                val f = SAXParserFactory.newInstance()
                f.isNamespaceAware = true
                f.isValidating = true
                val sp = f.newSAXParser()
                val xr = sp.xmlReader
                val entityMapHandler = LocalEntityMapHandler()
                xr.contentHandler = entityMapHandler
                xr.entityResolver = entityMapHandler
                xr.parse(
                    InputSource(
                        LocalEntityResolver::class.java.getResourceAsStream(
                            "/org/brailleblaster/utd/internal/xml/entityMap.xml"
                        )
                    )
                )
                localEntities = entityMapHandler.entityMap
            } catch (e: SAXException) {
                log.error("Problem with SAX parser for loading local XML entities", e)
            } catch (e: IOException) {
                log.error("Problem accessing local XML entities map file", e)
            } catch (e: ParserConfigurationException) {
                log.error("Problem creating parser", e)
                throw RuntimeException("Problem with parser", e)
            }
        }

        @JvmStatic
        @JvmOverloads
        @Throws(SAXException::class, ParserConfigurationException::class)
        fun createXomBuilder(validate: Boolean = false): Builder {
            val parser = newNSInstance("org.apache.xerces.jaxp.SAXParserFactoryImpl", null).newSAXParser().xmlReader
            parser.entityResolver = LocalEntityResolver()
            return Builder(parser, validate, NormaliserFactory())
        }

    }
}

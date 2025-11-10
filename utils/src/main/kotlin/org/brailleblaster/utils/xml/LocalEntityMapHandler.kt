package org.brailleblaster.utils.xml

import org.slf4j.LoggerFactory
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler
import java.io.IOException

class LocalEntityMapHandler : DefaultHandler() {
    private var entityMapBuilder: EntityMap.Builder? = null
    override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
        when (localName) {
            "entities" -> {
                entityMapBuilder = EntityMap.builder()
            }
            "jarEntity" -> {
                val publicId = attributes.getValue("publicId")
                val systemId = attributes.getValue("systemId")
                val localResourceName = attributes.getValue("resourceName")
                entityMapBuilder!!.put(publicId, systemId, EntityMap.JarEntity(localResourceName))
            }
            "emptyEntity" -> {
                val publicId = attributes.getValue("publicId")
                val systemId = attributes.getValue("systemId")
                entityMapBuilder!!.put(publicId, systemId, EntityMap.EmptyEntity())
            }
        }
    }

    @Throws(IOException::class, SAXException::class)
    override fun resolveEntity(publicId: String, systemId: String): InputSource {
        log.debug("Resolving publicId=\"{}\" systemId=\"{}\"", publicId, systemId)
        return if ("-//utd//DTD Entity Map 1.0//EN" == publicId || "entity-map.dtd" == systemId) {
            InputSource(javaClass.getResourceAsStream(DTD_LOCATION))
        } else super.resolveEntity(publicId, systemId)
    }

    val entityMap: EntityMap
        get() = entityMapBuilder!!.build()

    companion object {
        private val log = LoggerFactory.getLogger(LocalEntityMapHandler::class.java)
        private const val DTD_LOCATION = "/org/brailleblaster/utd/internal/xml/entity-map.dtd"
    }
}
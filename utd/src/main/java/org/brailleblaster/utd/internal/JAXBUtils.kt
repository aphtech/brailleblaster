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
package org.brailleblaster.utd.internal

import jakarta.xml.bind.ValidationEvent
import jakarta.xml.bind.ValidationEventHandler
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement
import org.apache.commons.lang3.ClassUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.StringWriter
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Member
import javax.xml.transform.OutputKeys
import javax.xml.transform.Result
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object JAXBUtils {
    private val log: Logger = LoggerFactory.getLogger(JAXBUtils::class.java)

    /**
     * Default behavior of JAXB is to completely ignore exceptions inside unmarshallers
     * And instead unmarshall the object to null (tested by throwing an exception inside XMLTypeAdapter.unmarshall)
     * (this btw is contrary to what the javadoc says the default behavior is)
     * This can cause nasty and unexplained NPE's
     * This should be added to all Marshaller and Unmarshaller
     */
	@JvmField
	val FAIL_ON_EXCEPTIONS_HANDLER: ValidationEventHandler = ValidationEventHandler { event: ValidationEvent ->
        var e = event.linkedException
        if (e is InvocationTargetException) {
            //Unwrap reflection exception, cleans up stack trace
            e = e.cause!!
        }

        val nested = ExceptionUtils.getThrowables(e)
        for (throwable in nested) {
            log.error("Throwable $throwable")
        }
        if (nested.size > 1 && nested[1] is JAXBException) {
            // w/o this the AccessorException re-wraps each time making a huge nested stack trace
            throw (nested[1] as JAXBException)
        }
        throw JAXBException("Failed while un/marshalling: " + event.message, e)
    }

    /**
     * JAXB completely ignores the "required" field in XMLAttribute
     * Apparently it's only used to generate a dtd which will validate the xml
     * As that is a roundabout, time-consuming way to do it, validate using reflection
     */
    fun validateRequiredFieldsInUnmarshall(sourceElement: Element, type: Class<*>?) {
        val membersWithAnnotation: MutableMap<String, Pair<Member, Annotation?>> = HashMap()
        for (curClass in ClassUtils.hierarchy(type, ClassUtils.Interfaces.INCLUDE)) {
            for (curField in curClass.declaredFields) {
                val attribAnnotation = curField.getDeclaredAnnotation(
                    XmlAttribute::class.java
                )
                var annotation: Annotation? = attribAnnotation
                if (attribAnnotation == null || !attribAnnotation.required) {
                    val elemAnnotation = curField.getDeclaredAnnotation(
                        XmlElement::class.java
                    )
                    annotation = elemAnnotation
                    if (elemAnnotation == null || !elemAnnotation.required) {
                        continue
                    }
                }

                val fieldName = curField.name
                check(!membersWithAnnotation.containsKey(fieldName)) { "Already contain config " + membersWithAnnotation[fieldName] }
                membersWithAnnotation[fieldName] = curField to annotation
            }

            for (curMethod in curClass.declaredMethods) {
                val attribAnnotation = curMethod.getDeclaredAnnotation(
                    XmlAttribute::class.java
                )
                var annotation: Annotation? = attribAnnotation
                if (attribAnnotation == null || !attribAnnotation.required) {
                    val elemAnnotation = curMethod.getDeclaredAnnotation(
                        XmlElement::class.java
                    )
                    annotation = elemAnnotation
                    if (elemAnnotation == null || !elemAnnotation.required) {
                        continue
                    }
                }

                var methodName = curMethod.name
                methodName = if (methodName.startsWith("set")) {
                    methodName.substring(3)
                } else if (methodName.startsWith("is")) {
                    methodName.substring(2)
                } else if (methodName.startsWith("get")) {
                    methodName.substring(3)
                } else {
                    throw RuntimeException("Unknown method name $curMethod")
                }
                methodName = methodName.replaceFirstChar { it.lowercase() }

                check(!membersWithAnnotation.containsKey(methodName)) { "Already contain config " + membersWithAnnotation[methodName] }
                membersWithAnnotation[methodName] = curMethod to annotation
            }
        }

        for ((name, value) in membersWithAnnotation) {
            val member = value.first
            val annotation = value.second
            log.debug("Required memeber {}", member)

            if (annotation is XmlAttribute) {
                if (!sourceElement.hasAttribute(name)) {
                    throw NullPointerException(
                        "Missing attribute " + name
                                + " in element " + toXML(sourceElement.parentNode)
                                + " as required by " + member
                    )
                }
            } else if (annotation is XmlElement) {
                val childNodes = sourceElement.childNodes
                var matched = 0
                var i = 0
                val len = childNodes.length
                while (i < len) {
                    val childNode = childNodes.item(i)
                    if (childNode is Element && childNode.localName == name) {
                        matched++
                    }
                    i++
                }
                if (matched != 1) {
                    throw NullPointerException(
                        "Missing element " + name
                                + " in element " + toXML(sourceElement.parentNode)
                                + " as required by " + member
                    )
                }
            }
        }
    }

    fun toXML(node: Node?): String {
        val writer = StringWriter()
        toXML(node, StreamResult(writer))
        return writer.toString()
    }

    @JvmStatic
	fun toXML(node: Node?, output: Result?) {
        try {
            val transformerFactory = TransformerFactory.newInstance()
            val transformer = transformerFactory.newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            val source = DOMSource(node)
            transformer.transform(source, output)
        } catch (ex: Exception) {
            throw RuntimeException("Failed to write XML", ex)
        }
    }

    class JAXBException : RuntimeException {
        constructor(message: String?) : super(message)

        constructor(message: String?, cause: Throwable?) : super(message, cause)
    }
}

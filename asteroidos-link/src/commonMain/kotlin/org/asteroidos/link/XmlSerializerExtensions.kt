/* AsteroidOS Link
 *
 * Copyright (c) 2023 Carlos Rafael Giani
 *
 * This project is released under the BSD 3-clause license.
 * See the LICENSE.adoc file for details.
 */

package org.asteroidos.link

import org.xmlpull.v1.XmlSerializer
import java.io.StringWriter

// XML extension functions for more readable XML serialization code.
//
// Based on https://www.schibsted.pl/blog/back-end/readable-xml-kotlin-extensions/
// and https://medium.com/android-news/how-to-generate-xml-with-kotlin-extension-functions-and-lambdas-in-android-app-976229f1e4d8

internal fun XmlSerializer.document(
    docName: String = "UTF-8",
    xmlStringWriter: StringWriter = StringWriter(),
    init: XmlSerializer.() -> Unit
): String {
    startDocument(docName, true)
    xmlStringWriter.buffer.setLength(0) //  refreshing string writer due to reuse
    setOutput(xmlStringWriter)
    init()
    endDocument()
    return xmlStringWriter.toString()
}

//  element
internal fun XmlSerializer.element(name: String, init: XmlSerializer.() -> Unit) {
    startTag("", name)
    init()
    endTag("", name)
}

//  element with attribute & content
internal fun XmlSerializer.element(
    name: String,
    content: String,
    init: XmlSerializer.() -> Unit
) {
    startTag("", name)
    init()
    text(content)
    endTag("", name)
}

//  element with content
internal fun XmlSerializer.element(name: String, content: String) =
    element(name) {
        text(content)
    }

internal fun XmlSerializer.attribute(name: String, value: String) =
    attribute("", name, value)

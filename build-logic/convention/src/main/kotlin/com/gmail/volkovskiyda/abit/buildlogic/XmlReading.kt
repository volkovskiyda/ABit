package com.gmail.volkovskiyda.abit.buildlogic

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * The DOM plumbing behind `:testSummary`, separated from what the documents mean.
 *
 * A real XML parse rather than a line scan, and that is not fussiness: Android lint embeds
 * multi-line rule explanations inside its attributes, which a regex reads wrong. Unreadable XML
 * comes back as null and counts as "not run" rather than failing the summary — a report this task
 * cannot parse is not a reason to lose the eight it can.
 */

internal fun File.xmlFiles(): List<File> =
    if (!isDirectory) emptyList() else walkTopDown().filter { it.isFile && it.extension == "xml" }.toList()

internal fun parse(file: File): Element? =
    runCatching {
        DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(file)
            .documentElement
    }.getOrNull()

internal fun Element.childElements(tag: String): List<Element> {
    val nodes = getElementsByTagName(tag)
    return (0 until nodes.length).map { nodes.item(it) as Element }
}

internal fun Element.intAttribute(name: String): Int = getAttribute(name).toIntOrNull() ?: 0

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

internal fun parseManifest(xml: String): Document =
    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(ByteArrayInputStream(xml.toByteArray()))

internal fun Document.elements(tag: String): List<Element> =
    (0 until getElementsByTagName(tag).length).map { getElementsByTagName(tag).item(it) as Element }

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.manifest

import app.morphe.util.asSequence
import org.w3c.dom.Document
import org.w3c.dom.Element

private val permissionTags = setOf("uses-permission", "uses-permission-sdk-23")
private val componentTags = setOf("activity", "activity-alias", "service", "receiver", "provider")

internal fun Document.disableApplicationComponents(names: Set<String>) {
    val application = getElementsByTagName("application").item(0) as Element
    application.childNodes.asSequence().filterIsInstance<Element>()
        .filter { it.tagName in componentTags && it.getAttribute("android:name") in names }
        .forEach { it.setAttribute("android:enabled", "false") }
}

internal fun Document.removeManifestMetadata(names: Set<String>) {
    getElementsByTagName("meta-data").asSequence().filterIsInstance<Element>()
        .filter { it.getAttribute("android:name") in names }
        .toList()
        .forEach { it.parentNode.removeChild(it) }
}

internal fun Document.removeUsesPermissions(names: Set<String>) {
    documentElement.childNodes.asSequence().filterIsInstance<Element>()
        .filter { it.tagName in permissionTags && it.getAttribute("android:name") in names }
        .toList()
        .forEach { it.parentNode.removeChild(it) }
}

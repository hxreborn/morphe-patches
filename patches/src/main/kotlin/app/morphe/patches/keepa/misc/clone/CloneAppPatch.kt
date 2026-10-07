/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.keepa.misc.clone

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.util.asSequence
import org.w3c.dom.Document
import org.w3c.dom.Element

@Suppress("unused")
val cloneAppPatch = resourcePatch(
    name = "Clone app",
    description = "Installs Keepa as a separate app alongside the original, with its own account " +
        "and price watches. Each copy needs a different clone number.",
    default = false,
) {
    compatibleWith(AppCompatibilities.KEEPA)

    dependsOn(removePairipProtectionPatch)

    val packageName by stringOption(
        key = "packageName",
        default = "com.keepa.mobile.clone1",
        values = (1..5).associate { "Clone $it" to "com.keepa.mobile.clone$it" },
        title = "Package name",
        description = "Package name of this copy.",
        required = true,
    ) {
        it!!.matches(Regex("^[a-z]\\w*(\\.[a-z]\\w*)+$")) && it != AppCompatibilities.KEEPA.packageName
    }

    finalize {
        document("AndroidManifest.xml").use { document -> cloneManifest(document, packageName!!) }
    }
}

internal fun cloneManifest(document: Document, clonePackageName: String) {
    val manifest = document.getElementsByTagName("manifest").item(0) as Element
    val originalPackageName = manifest.getAttribute("package")

    manifest.setAttribute("package", clonePackageName)

    fun renamePrefixed(tag: String, attribute: String) =
        document.getElementsByTagName(tag).asSequence()
            .filterIsInstance<Element>()
            .filter { it.getAttribute(attribute).startsWith("$originalPackageName.") }
            .forEach {
                it.setAttribute(
                    attribute,
                    clonePackageName + it.getAttribute(attribute).removePrefix(originalPackageName),
                )
            }

    renamePrefixed("provider", "android:authorities")
    renamePrefixed("permission", "android:name")
    renamePrefixed("uses-permission", "android:name")

    val cloneLabel = "Keepa ${clonePackageName.substringAfterLast('.')}"

    (document.getElementsByTagName("application").item(0) as Element).setAttribute("android:label", cloneLabel)

    document.getElementsByTagName("category").asSequence()
        .filterIsInstance<Element>()
        .filter { it.getAttribute("android:name") == "android.intent.category.LAUNCHER" }
        .map { category -> category.parentNode.parentNode as Element }
        .forEach { launcherActivity -> launcherActivity.setAttribute("android:label", cloneLabel) }
}

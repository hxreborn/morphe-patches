/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.androidauto

import app.morphe.patcher.patch.PatchException
import app.morphe.util.asSequence
import org.w3c.dom.Document
import org.w3c.dom.Element

private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
private const val NAME = "android:name"
private const val APP_CATEGORY = "android:appCategory"
private const val GAME_CATEGORY = "game"
private const val MAIN_ACTION = "android.intent.action.MAIN"
private const val LAUNCHER_CATEGORY = "android.intent.category.LAUNCHER"
private const val CAR_LAUNCHER_CATEGORY = "android.intent.category.CAR_LAUNCHER"
private val launcherComponents = listOf("activity", "activity-alias")

private fun Element.childElements(tag: String) = childNodes.asSequence()
    .filterIsInstance<Element>()
    .filter { it.tagName == tag }

private fun Element.declares(tag: String, name: String) = childElements(tag).any { it.getAttribute(NAME) == name }

internal fun declareParkedGame(manifest: Document) {
    val application = manifest.getElementsByTagName("application").item(0) as? Element
        ?: throw PatchException("AndroidManifest.xml has no <application>")
    val launcherFilters = launcherComponents
        .flatMap { tag -> manifest.getElementsByTagName(tag).asSequence().filterIsInstance<Element>().toList() }
        .flatMap { component -> component.childElements("intent-filter").toList() }
        .filter { it.declares("action", MAIN_ACTION) && it.declares("category", LAUNCHER_CATEGORY) }
    if (launcherFilters.isEmpty()) throw PatchException("No main launcher intent filter found")
    application.setAttributeNS(ANDROID_NAMESPACE, APP_CATEGORY, GAME_CATEGORY)
    launcherFilters.filterNot { it.declares("category", CAR_LAUNCHER_CATEGORY) }.forEach { filter ->
        filter.appendChild(manifest.createElement("category").apply {
            setAttributeNS(ANDROID_NAMESPACE, NAME, CAR_LAUNCHER_CATEGORY)
        })
    }
}

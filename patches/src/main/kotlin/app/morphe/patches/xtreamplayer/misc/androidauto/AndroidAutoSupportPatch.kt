/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.xtreamplayer.misc.androidauto

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.xtreamplayer.misc.premium.unlockPremiumPatch
import app.morphe.util.asSequence
import org.w3c.dom.Element

private val carServiceActions = setOf(
    "android.media.browse.MediaBrowserService",
    "androidx.car.app.CarAppService",
)

@Suppress("unused")
val androidAutoSupportPatch = resourcePatch(
    name = "Full Android Auto screen",
    description = "Shows full app in Android Auto while parked. " +
        "Requires Android 15 or newer and Unknown sources in Android Auto developer settings.",
) {
    compatibleWith(AppCompatibilities.XTREAM_PLAYER)
    dependsOn(unlockPremiumPatch)

    execute {
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as? Element
                ?: throw PatchException("AndroidManifest.xml has no <application>")
            val filters = application.getElementsByTagName("intent-filter").asSequence()
                .filterIsInstance<Element>().toList()
            val launcher = filters.singleOrNull { filter ->
                filter.parentNode.nodeName == "activity" &&
                    filter.getElementsByTagName("action").asSequence().filterIsInstance<Element>().any {
                        it.getAttribute("android:name") == "android.intent.action.MAIN"
                    } && filter.getElementsByTagName("category").asSequence().filterIsInstance<Element>().any {
                        it.getAttribute("android:name") == "android.intent.category.LAUNCHER"
                    }
            } ?: throw PatchException("Expected one main launcher activity")
            val carFilters = filters.filter { filter ->
                filter.parentNode.nodeName == "service" && (
                    filter.getElementsByTagName("action").asSequence().filterIsInstance<Element>().any {
                        it.getAttribute("android:name") in carServiceActions
                    } || filter.getElementsByTagName("category").asSequence().filterIsInstance<Element>().any {
                        it.getAttribute("android:name").startsWith("com.google.android.gms.car.category.")
                    }
                )
            }
            if (carFilters.isEmpty()) throw PatchException("Android Auto service filters not found")
            application.setAttribute("android:appCategory", "game")
            launcher.appendChild(document.createElement("category").apply {
                setAttribute("android:name", "android.intent.category.CAR_LAUNCHER")
            })
            carFilters.forEach { it.parentNode.removeChild(it) }
        }
    }
}

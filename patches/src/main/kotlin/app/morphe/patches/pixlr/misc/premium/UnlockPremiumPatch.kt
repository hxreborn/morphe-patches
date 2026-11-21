/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.pixlr.misc.premium

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.decode
import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.pixlr.ads.adRemovalEntitlementPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.findElementByAttributeValueOrThrow

private const val PREMIUM_BUTTON_ID = "@id/premium_button"

private val hidePremiumButtonPatch = patch {
    execute {
        decode("res/layout*")
        val layouts = resourceFile("res").listFiles().orEmpty()
            .filter { it.isDirectory && it.name.startsWith("layout") }
            .flatMap { it.listFiles().orEmpty().toList() }
            .filter { it.extension == "xml" && PREMIUM_BUTTON_ID in it.readText() }
        if (layouts.isEmpty()) throw PatchException("No layout declares $PREMIUM_BUTTON_ID")

        layouts.forEach { layout ->
            document("res/${layout.parentFile.name}/${layout.name}").use { document ->
                document.getElementsByTagName("*")
                    .findElementByAttributeValueOrThrow("android:id", PREMIUM_BUTTON_ID)
                    .setAttribute("android:visibility", "gone")
            }
        }
    }
}

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks premium stickers and effects. " +
        "Removes ads, subscription screens, the daily save limit and the video watermark. " +
        "AI tools need account credits.",
) {
    compatibleWith(AppCompatibilities.PIXLR)

    dependsOn(adRemovalEntitlementPatch, hidePremiumButtonPatch)

    execute {
        HasSubscriptionFingerprint.matchSingle().method.returnEarly(true)
    }
}

/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.kick.misc.theme

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.replaceAsciiInPlace
import app.morphe.util.findElementByAttributeValueOrThrow

private const val JAVASCRIPT_BUNDLE = "assets/index.android.bundle"
private const val DARK_BACKGROUND = "#090A0B"
private const val BLACK = "#000000"
private const val UPDATES_ENABLED = "expo.modules.updates.ENABLED"

@Suppress("unused")
val amoledThemePatch = resourcePatch(
    name = "AMOLED dark theme",
    description = "Replaces the dark theme background with pure black. " +
        "Disables over-the-air updates that would restore the original background.",
) {
    compatibleWith(AppCompatibilities.KICK)

    execute {
        if (!get(JAVASCRIPT_BUNDLE).replaceAsciiInPlace(DARK_BACKGROUND, BLACK)) {
            throw PatchException("Could not find the dark theme background")
        }

        document("AndroidManifest.xml").use { document ->
            document.getElementsByTagName("meta-data")
                .findElementByAttributeValueOrThrow("android:name", UPDATES_ENABLED)
                .setAttribute("android:value", "false")
        }
    }
}

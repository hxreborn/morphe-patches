/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.misc.androidauto

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.androidauto.declareParkedGame
import app.morphe.patches.shared.misc.requireStandardInstall

@Suppress("unused")
val parkedAppForAndroidAutoPatch = resourcePatch(
    name = "Parked app for Android Auto",
    description = "Lists the game on the Android Auto car screen while parked. " +
        "Requires Android 15 or newer and a standard install. " +
        "Android Auto needs Unknown sources turned on in its developer settings.",
) {
    compatibleWith(AppCompatibilities.CANDY_CRUSH_SAGA)
    availability(requireStandardInstall)

    execute {
        document("AndroidManifest.xml").use(::declareParkedGame)
    }
}

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vllo.misc.tracking

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.vllo.requireArm64Delta
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops AppsFlyer, Firebase Analytics, and Facebook from collecting usage data.",
) {
    compatibleWith(AppCompatibilities.VLLO)

    dependsOn(
        removePairipVirtualizationPatch,
        removePairipProtectionPatch,
        disableAnalyticsCollectionPatch,
    )

    availability(requireArm64Delta)

    execute {
        AppsFlyerStartFingerprint.matchSingle().method.returnEarly()
    }
}

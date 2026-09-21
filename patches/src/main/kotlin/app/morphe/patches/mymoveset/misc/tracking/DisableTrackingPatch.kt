/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.mymoveset.misc.tracking

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.mymoveset.misc.updates.disableOtaUpdatesPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops the install identifier from reaching Expo and usage events from " +
        "reaching Google. Disables OTA updates.",
) {
    compatibleWith(AppCompatibilities.MYMOVESET)

    dependsOn(disableOtaUpdatesPatch)

    execute {
        DefaultSchedulerScheduleFingerprint.matchSingle().method.returnEarly()
    }
}

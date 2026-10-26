/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.mymoveset.misc.tracking

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.expo.disableOtaUpdatesPatch
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops the install identifier from reaching Expo and usage events from " +
        "reaching Google. Disables OTA updates.",
) {
    compatibleWith(AppCompatibilities.MYMOVESET)

    dependsOn(disableOtaUpdatesPatch, removePairipProtectionPatch)

    execute {
        DefaultSchedulerScheduleFingerprint.matchSingle().method.returnEarly()
    }
}

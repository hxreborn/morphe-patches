/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.iototoy.misc.tracking

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops the AppBrain SDK from starting, loading ads and reading the advertising ID. " +
        "Disables ML Kit usage logging.",
) {
    compatibleWith(AppCompatibilities.IOTOTOY)
    dependsOn(removePairipProtectionPatch)

    execute {
        AppBrainInitFingerprint.matchSingle().method.returnEarly()
        ScheduleTransportEventFingerprint.matchSingle().method.returnEarly()
    }
}

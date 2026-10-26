/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.oneweather.misc.telemetry

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops installs, sessions and in-app events from reaching AppsFlyer.",
) {
    compatibleWith(AppCompatibilities.ONE_WEATHER)

    execute {
        AppsFlyerStartFingerprint.matchSingle().method.returnEarly()
    }
}

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.rise.misc.telemetry

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val disableTelemetryPatch = bytecodePatch(
    name = "Disable telemetry",
    description = "Stops crash and error reports from reaching Sentry.",
) {
    compatibleWith(AppCompatibilities.RISE)

    execute {
        SentryInitFingerprint.matchSingle().method.returnEarly()
    }
}

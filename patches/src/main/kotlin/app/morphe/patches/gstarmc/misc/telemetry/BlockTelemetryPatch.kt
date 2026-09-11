/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.gstarmc.misc.telemetry

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.gstarmc.misc.jiagu.enableRuntimeFeature
import app.morphe.patches.gstarmc.misc.jiagu.jiaguRuntimePatch
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val blockTelemetryPatch = rawResourcePatch(
    name = "Block telemetry",
    description = "Blocks the Umeng, ByteDance and ad network analytics endpoints.",
) {
    compatibleWith(AppCompatibilities.DWG_FASTVIEW)
    dependsOn(jiaguRuntimePatch)

    execute {
        enableRuntimeFeature("telemetry")
    }
}

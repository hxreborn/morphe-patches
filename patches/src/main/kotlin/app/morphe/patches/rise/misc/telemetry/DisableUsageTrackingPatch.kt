/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.rise.misc.telemetry

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val disableUsageTrackingPatch = bytecodePatch(
    name = "Disable usage tracking",
    description = "Stops app usage events from being uploaded. " +
        "Local usage tracking remains enabled.",
) {
    compatibleWith(AppCompatibilities.RISE)

    execute {
        val eventType = UsageEventToStringFingerprint.matchSingle().originalClassDef.type

        usageEventMapFingerprint(eventType).matchSingle().method
            .addInstructions(
                0,
                """
                    new-instance v0, Ljava/util/ArrayList;
                    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V
                    return-object v0
                """,
            )
    }
}

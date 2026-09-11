/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.rise.misc.telemetry

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities

private const val USAGE_EVENT_FACTORY_CLASS =
    "Lcom/risesci/nyx/RiseHealthKit/ApplicationUsageEvent\$Factory;"

@Suppress("unused")
val disableUsageTrackingPatch = bytecodePatch(
    name = "Disable usage tracking",
    description = "Stops app usage events from being uploaded. " +
        "Local usage tracking remains enabled.",
) {
    compatibleWith(AppCompatibilities.RISE)

    execute {
        mutableClassDefBy(USAGE_EVENT_FACTORY_CLASS)
            .methods.single { it.name == "map" && it.returnType == "Ljava/util/List;" }
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

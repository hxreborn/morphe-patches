/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.graphmessenger.misc.tracking

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.graphmessenger.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.util.findFreeRegister
import app.morphe.util.matchSingle

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops Firebase Analytics and AppMetrica from collecting and sending usage data.",
) {
    compatibleWith(AppCompatibilities.GRAPH_MESSENGER)
    availability(requireArm64)

    dependsOn(spoofSignaturePatch, disableAnalyticsCollectionPatch)

    execute {
        val activate = AppMetricaActivateFingerprint.matchSingle()
        val afterActivate = activate.instructionMatches.single().index + 1
        val register = activate.method.findFreeRegister(afterActivate)
        activate.method.addInstructions(
            afterActivate,
            """
                const/4 v$register, 0x0
                invoke-static { v$register }, Lio/appmetrica/analytics/AppMetrica;->setDataSendingEnabled(Z)V
            """,
        )
    }
}

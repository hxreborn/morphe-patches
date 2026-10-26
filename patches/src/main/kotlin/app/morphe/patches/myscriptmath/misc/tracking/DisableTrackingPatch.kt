/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.myscriptmath.misc.tracking

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.SetCrashlyticsCollectionEnabledFingerprint
import app.morphe.patches.shared.misc.analytics.SurvicateInitFingerprint
import app.morphe.patches.shared.misc.analytics.deactivateFirebaseAnalyticsPatch
import app.morphe.patches.shared.misc.analytics.disableFirebaseSessionsPatch

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Stops Firebase Analytics, Crashlytics, Firebase Sessions and Survicate surveys from " +
        "collecting usage data.",
) {
    compatibleWith(AppCompatibilities.MYSCRIPT_MATH)

    dependsOn(deactivateFirebaseAnalyticsPatch, disableFirebaseSessionsPatch)

    execute {
        SetCrashlyticsCollectionEnabledFingerprint.matchSingle().method.addInstruction(0, "const/4 p1, 0x0")
        SurvicateInitFingerprint.matchSingle().method.returnEarly()
    }
}

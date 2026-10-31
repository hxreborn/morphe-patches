/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.pixlr.misc.tracking

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.SingularInitFingerprint
import app.morphe.patches.shared.misc.analytics.deactivateFirebaseAnalyticsPatch
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableCrashlyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableFirebaseSessionsPatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Disables Firebase Analytics, Singular, Facebook App Events, Crashlytics " +
        "and the app's own usage events.",
) {
    compatibleWith(AppCompatibilities.PIXLR)

    dependsOn(
        disableAnalyticsCollectionPatch,
        deactivateFirebaseAnalyticsPatch,
        disableCrashlyticsCollectionPatch,
        disableFirebaseSessionsPatch,
    )

    execute {
        SingularInitFingerprint.matchSingle().method.returnEarly(false)

        ActivateFacebookAppFingerprint.matchSingle().method.returnEarly()

        EnableCrashlyticsCollectionFingerprint.matchSingle().apply {
            val enabledValue = instructionMatches.first()
            val putBoolean = instructionMatches.last()
            val enabledRegister = enabledValue.getInstruction<OneRegisterInstruction>().registerA
            val storedRegister = putBoolean.getInstruction<FiveRegisterInstruction>().registerE

            method.addInstruction(putBoolean.index, "const/4 v$storedRegister, 0x0")
            method.replaceInstruction(
                enabledValue.index,
                "sget-object v$enabledRegister, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;",
            )
        }

        SendMobileEventsFingerprint.matchSingle().apply {
            val enabledResult = instructionMatches.last()
            val enabledRegister = enabledResult.getInstruction<OneRegisterInstruction>().registerA
            method.addInstruction(enabledResult.index + 1, "const/4 v$enabledRegister, 0x0")
        }
    }
}

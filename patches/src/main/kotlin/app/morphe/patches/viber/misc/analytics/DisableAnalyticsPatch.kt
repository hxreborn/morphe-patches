/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.analytics

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.findFreeRegister
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.analytics.SetCrashlyticsCollectionEnabledFingerprint
import app.morphe.patches.shared.misc.analytics.disableAnalyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableCrashlyticsCollectionPatch
import app.morphe.patches.shared.misc.analytics.disableFirebaseSessionsPatch
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val disableAnalyticsPatch = bytecodePatch(
    name = "Disable analytics",
    description = "Stops Mixpanel, Adjust, Braze, Firebase, Crashlytics and Snap Kit from " +
        "collecting and sending usage data.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(disableAnalyticsCollectionPatch, disableCrashlyticsCollectionPatch, disableFirebaseSessionsPatch)

    execute {
        TrackerAllowedFingerprint.matchSingle().method.returnEarly(false)
        SnapKitInitFingerprint.matchSingle().method.returnEarly(false)
        SetCrashlyticsCollectionEnabledFingerprint.matchSingle().method.addInstruction(0, "const/4 p1, 0x0")

        TrackerConsentFingerprint.matchSingle().apply {
            val setEnabled = instructionMatches.first()
                .getInstruction<ReferenceInstruction>()
                .getReference<MethodReference>()!!
            val consentRegister = method.implementation!!.registerCount - 1
            val falseRegister = method.findFreeRegister(0, consentRegister)
            method.addInstructions(
                0,
                """
                    const/4 v$falseRegister, 0x0
                    invoke-interface { p1, v$falseRegister }, $setEnabled
                    return-void
                """,
            )
        }
    }
}

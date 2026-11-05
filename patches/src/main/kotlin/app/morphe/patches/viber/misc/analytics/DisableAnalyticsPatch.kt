/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.analytics

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.addFreeRegisters
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import app.morphe.patches.viber.shared.markPatchApplied
import app.morphe.patches.viber.shared.returnWhenEnabled
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val DESCRIPTION = "Stops Mixpanel, Adjust, Braze, Firebase, Crashlytics and Snap Kit from collecting and sending usage data."

@Suppress("unused")
val disableAnalyticsPatch = bytecodePatch(
    name = "Disable analytics",
    description = DESCRIPTION,
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)
    execute {
        markPatchApplied("disableAnalytics", DESCRIPTION)
        TrackerAllowedFingerprint.matchSingle().method.returnWhenEnabled("$VIBER_SETTINGS_CLASS->disableAnalytics()Z")
        SnapKitInitFingerprint.matchSingle().method.returnWhenEnabled("$VIBER_SETTINGS_CLASS->disableAnalytics()Z")
        FirebaseSessionsEnabledFingerprint.matchSingle().method.returnWhenEnabled("$VIBER_SETTINGS_CLASS->disableAnalytics()Z")
        CrashlyticsEnabledFingerprint.matchSingle().method.returnWhenEnabled("$VIBER_SETTINGS_CLASS->disableAnalytics()Z")
        TrackerConsentFingerprint.matchSingle().apply {
            val (setEnabled, _, _) = instructionMatches
            val reference = setEnabled.instruction.getReference<MethodReference>()!!
            val trackerRegister = method.implementation!!.registerCount - 1
            method.returnWhenEnabled("$VIBER_SETTINGS_CLASS->disableAnalytics()Z") { register -> """
                const/16 v$register, 0x0
                invoke-interface { v$trackerRegister, v$register }, $reference
                return-void
            """ }
        }
        FirebaseMetadataFingerprint.matchSingle().method.apply {
            val register = implementation!!.registerCount
            val keyRegister = register - 1
            val (expanded, parameterMoves) = addFreeRegisters(1)
            expanded.addInstructionsWithLabels(parameterMoves, """
                invoke-static/range {v$keyRegister .. v$keyRegister}, $VIBER_SETTINGS_CLASS->disableAnalyticsKey(Ljava/lang/String;)Z
                move-result v$register
                if-eqz v$register, :original
                sget-object v$register, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                return-object v$register
            """, ExternalLabel("original", expanded.getInstruction(parameterMoves)))
        }
    }
}

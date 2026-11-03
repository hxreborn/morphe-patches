/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.patreon.misc.tracking

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.patreon.misc.featureflags.featureFlagsPatch
import app.morphe.patches.patreon.misc.settings.PATREON_EXTENSION_PACKAGE
import app.morphe.patches.patreon.misc.settings.markFeaturePatched
import app.morphe.patches.patreon.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

private const val TRACKING_CLASS = "${PATREON_EXTENSION_PACKAGE}Tracking;"

private fun MutableMethod.skipWhenTrackingDisabled(returnInstructions: String) = addInstructionsWithLabels(
    0,
    """
        invoke-static { }, $TRACKING_CLASS->isDisabled()Z
        move-result v0
        if-eqz v0, :track
        $returnInstructions
    """,
    ExternalLabel("track", getInstruction(0)),
)

@Suppress("unused")
val disableTrackingPatch = bytecodePatch(
    name = "Disable tracking",
    description = "Disables Amplitude, AppsFlyer, Sentry, Sift and Mux data collection.",
) {
    compatibleWith(AppCompatibilities.PATREON)

    dependsOn(patchesSettingsPatch, featureFlagsPatch)
    extendWith("extensions/extension.mpe")

    execute {
        markFeaturePatched(TRACKING_CLASS)

        AmplitudeLogEventFingerprint.method.skipWhenTrackingDisabled(
            """
                const-wide/16 v0, -0x1
                return-wide v0
            """,
        )
        listOf(AppsFlyerInitFingerprint, AppsFlyerStartFingerprint, SiftOpenFingerprint, SiftCollectFingerprint)
            .forEach { it.method.skipWhenTrackingDisabled("return-void") }

        MuxStatsTrackingFingerprint.method.skipWhenTrackingDisabled("return-void")
        MuxStatsTrackingFingerprint.classDef.methods.single { it.name == "release" && it.parameterTypes.isEmpty() }
            .skipWhenTrackingDisabled("return-void")

        val setDsnIndex = SentryOptionsFingerprint.instructionMatches.single().index
        SentryOptionsFingerprint.method.apply {
            val dsnRegister = getInstruction<FiveRegisterInstruction>(setDsnIndex).registerD
            addInstructions(
                setDsnIndex,
                """
                    invoke-static { v$dsnRegister }, $TRACKING_CLASS->sentryDsn(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$dsnRegister
                """,
            )
        }
    }
}

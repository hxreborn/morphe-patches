/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.ai

import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import app.morphe.patches.viber.shared.markPatchApplied
import app.morphe.patches.viber.shared.returnWhenEnabled
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/viber/AiPreferences;"
private val AI_FEATURES_BANNER_PREFERENCE_SITES = 2..2

@Suppress("unused")
val disableAiFeaturesPatch: BytecodePatch = bytecodePatch(
    name = "Disable AI features",
    description = "Removes AI reply suggestions, chat summaries and image remix controls.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)

    execute {
        markPatchApplied("disableAiFeatures", disableAiFeaturesPatch)
        ImageRemixIntroductionFingerprint.matchSingle().method.returnWhenEnabled(
            "$VIBER_SETTINGS_CLASS->disableAiFeatures()Z",
        )
        ImageRemixPresetsFingerprint.matchSingle().apply {
            val (_, _, availability) = instructionMatches
            val register = (availability.instruction as OneRegisterInstruction).registerA
            method.addInstructions(
                availability.index + 1,
                """
                    invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->showImageRemix(Z)Z
                    move-result v$register
                """,
            )
        }
        listOf(
            ChatSummaryPreferenceFingerprint,
            ImageRemixPreferenceFingerprint,
            AiInChatPreferenceFingerprint,
            AiReplyPreferenceFingerprint,
            SmartRecapPreferenceFingerprint,
        ).forEach { redirectPreference(it.matchSingle(), "$EXTENSION_CLASS_DESCRIPTOR->hideFeature(Z)Z") }
        AiFeaturesBannerPreferenceFingerprint.matchAll(AI_FEATURES_BANNER_PREFERENCE_SITES)
            .forEach { redirectPreference(it, "$EXTENSION_CLASS_DESCRIPTOR->markDismissed(Z)Z") }
    }
}

private fun redirectPreference(match: Match, defaultOverride: String) {
    val (keyRead, preferenceCall) = match.instructionMatches
    val keyRegister = (keyRead.instruction as OneRegisterInstruction).registerA
    val call = preferenceCall.instruction as FiveRegisterInstruction
    if (call.registerD != keyRegister) {
        throw PatchException("The preference call in ${match.originalMethod} does not take the matched key")
    }
    val defaultRegister = call.registerE
    match.method.addInstructions(
        preferenceCall.index,
        """
            invoke-static { v$keyRegister }, $EXTENSION_CLASS_DESCRIPTOR->preferenceKey(Ljava/lang/String;)Ljava/lang/String;
            move-result-object v$keyRegister
            invoke-static { v$defaultRegister }, $defaultOverride
            move-result v$defaultRegister
        """,
    )
}

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.ai

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import app.morphe.patches.viber.shared.addFilterCallAfter
import app.morphe.patches.viber.shared.markPatchApplied
import app.morphe.patches.viber.shared.redirectPreferences
import app.morphe.patches.viber.shared.returnWhenEnabled

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/viber/AiPreferences;"
private const val PREFERENCE_KEY_OVERRIDE = "$EXTENSION_CLASS_DESCRIPTOR->preferenceKey(Ljava/lang/String;)Ljava/lang/String;"
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
            method.addFilterCallAfter(availability.index, "$EXTENSION_CLASS_DESCRIPTOR->showImageRemix(Z)Z")
        }
        listOf(
            ChatSummaryPreferenceFingerprint,
            ImageRemixPreferenceFingerprint,
            AiInChatPreferenceFingerprint,
            AiReplyPreferenceFingerprint,
            SmartRecapPreferenceFingerprint,
        ).map { it.matchSingle() }
            .redirectPreferences(PREFERENCE_KEY_OVERRIDE, "$EXTENSION_CLASS_DESCRIPTOR->hideFeature(Z)Z")
        AiFeaturesBannerPreferenceFingerprint.matchAll(AI_FEATURES_BANNER_PREFERENCE_SITES)
            .redirectPreferences(PREFERENCE_KEY_OVERRIDE, "$EXTENSION_CLASS_DESCRIPTOR->markDismissed(Z)Z")
    }
}

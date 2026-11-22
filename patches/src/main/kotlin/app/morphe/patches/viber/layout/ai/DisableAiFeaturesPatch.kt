/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.ai

import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.layout.business.ChatSelectionFingerprint
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import app.morphe.patches.viber.shared.addFilterCallAfter
import app.morphe.patches.viber.shared.markPatchApplied
import app.morphe.patches.viber.shared.redirectPreferences
import app.morphe.patches.viber.shared.returnWhenEnabled
import app.morphe.patches.shared.misc.extension.viberDisableAiFeaturesExtensionPatch

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/viber/AiPreferences;"
private const val PREFERENCE_KEY_OVERRIDE = "$EXTENSION_CLASS_DESCRIPTOR->preferenceKey(Ljava/lang/String;)Ljava/lang/String;"
private val AI_FEATURES_BANNER_PREFERENCE_SITES = 2..2

@Suppress("unused")
val disableAiFeaturesPatch: Patch = patch(
    name = "Disable AI features",
    description = "Removes AI and inline Translate.",
) {
    dependsOn(viberDisableAiFeaturesExtensionPatch)
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)

    execute {
        markPatchApplied("disableAiFeatures", disableAiFeaturesPatch)
        ChatSelectionFingerprint.clearMatch()
        ChatSelectionFingerprint.match().apply {
            val (selectionPrefix, _) = instructionMatches
            method.addFilterCallAfter(selectionPrefix.index, "$EXTENSION_CLASS_DESCRIPTOR->filterSelection(Ljava/lang/String;)Ljava/lang/String;")
        }
        SearchChatGptEntryPointAvailabilityFingerprint.method.returnWhenEnabled(
            "$VIBER_SETTINGS_CLASS->disableAiFeatures()Z",
        )
        SettingsChatGptRowFingerprint.match().apply {
            val (_, availability, _) = instructionMatches
            method.addFilterCallAfter(availability.index, "$EXTENSION_CLASS_DESCRIPTOR->showFeature(Z)Z")
        }
        InlineTranslateVisibilityFingerprint.match().apply {
            val (_, visibility, _, _) = instructionMatches
            method.addFilterCallAfter(visibility.index, "$EXTENSION_CLASS_DESCRIPTOR->showFeature(Z)Z")
        }
        ImageRemixIntroductionFingerprint.method.returnWhenEnabled(
            "$VIBER_SETTINGS_CLASS->disableAiFeatures()Z",
        )
        ImageRemixPresetsFingerprint.match().apply {
            val (_, _, availability) = instructionMatches
            method.addFilterCallAfter(availability.index, "$EXTENSION_CLASS_DESCRIPTOR->showImageRemix(Z)Z")
        }
        listOf(
            ChatSummaryPreferenceFingerprint,
            ImageRemixPreferenceFingerprint,
            AiInChatPreferenceFingerprint,
            AiReplyPreferenceFingerprint,
            SmartRecapPreferenceFingerprint,
        ).map { it.match() }
            .redirectPreferences(PREFERENCE_KEY_OVERRIDE, "$EXTENSION_CLASS_DESCRIPTOR->showFeature(Z)Z")
        NewsBriefBannerEligibilityFingerprint.method.returnWhenEnabled(
            "$VIBER_SETTINGS_CLASS->disableAiFeatures()Z",
        ) { register -> "sget-object v$register, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;\nreturn-object v$register" }
        LinkSummaryHostAllowedFingerprint.method.returnWhenEnabled(
            "$VIBER_SETTINGS_CLASS->disableAiFeatures()Z",
        )
        AiFeaturesBannerPreferenceFingerprint.matchAll(AI_FEATURES_BANNER_PREFERENCE_SITES)
            .redirectPreferences(PREFERENCE_KEY_OVERRIDE, "$EXTENSION_CLASS_DESCRIPTOR->markDismissed(Z)Z")
    }
}

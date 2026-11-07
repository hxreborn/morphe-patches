/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.promotions

import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import app.morphe.patches.viber.shared.addFilterCall
import app.morphe.patches.viber.shared.addFilterCallAfter
import app.morphe.patches.viber.shared.markPatchApplied

private const val SHOW_PROMOTION = "$VIBER_SETTINGS_CLASS->showPromotion(Z)Z"

@Suppress("unused")
val hidePromotionsPatch: BytecodePatch = bytecodePatch(
    name = "Hide Viber Out and channel promotions",
    description = "Hides the Viber Out banners and intro on the Calls tab and the channel and bot suggestions in the chat list.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)
    execute {
        markPatchApplied("hidePromotions", hidePromotionsPatch)
        ChatSuggestionsVisibilityFingerprint.matchSingle().method.addFilterCall(0, "p1", SHOW_PROMOTION)
        CallsBannerVisibilityFingerprint.matchSingle().apply {
            val (cardVisible, _, shadowVisible, _) = instructionMatches
            listOf(shadowVisible, cardVisible).forEach { method.addFilterCallAfter(it.index, SHOW_PROMOTION) }
        }
        ViberOutSectionVisibilityFingerprint.matchSingle().apply {
            val (_, _, sectionVisible, _) = instructionMatches
            method.addFilterCallAfter(sectionVisible.index, SHOW_PROMOTION)
        }
        CallsIntroFingerprint.matchSingle().apply {
            val (_, flagResult, _) = instructionMatches
            method.addFilterCallAfter(flagResult.index, SHOW_PROMOTION)
        }
    }
}

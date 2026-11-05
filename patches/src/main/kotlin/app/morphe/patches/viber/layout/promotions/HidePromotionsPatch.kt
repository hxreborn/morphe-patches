/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.promotions

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatch
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.viber.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.viber.misc.settings.patchesSettingsPatch
import app.morphe.patches.viber.shared.VIBER_SETTINGS_CLASS
import app.morphe.patches.viber.shared.markPatchApplied
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val hidePromotionsPatch: BytecodePatch = bytecodePatch(
    name = "Hide Viber Out and channel promotions",
    description = "Hides the Viber Out banners and intro on the Calls tab and the channel and bot suggestions in the chat list.",
) {
    compatibleWith(AppCompatibilities.VIBER)
    dependsOn(spoofSignaturePatch, patchesSettingsPatch)
    execute {
        markPatchApplied("hidePromotions", hidePromotionsPatch)
        ChatSuggestionsVisibilityFingerprint.matchSingle().method.filterPromotion(0, "p1")
        CallsBannerVisibilityFingerprint.matchSingle().apply {
            val (cardVisible, _, shadowVisible, _) = instructionMatches
            listOf(shadowVisible, cardVisible).forEach {
                method.filterPromotion(it.index + 1, "v${(it.instruction as OneRegisterInstruction).registerA}")
            }
        }
        ViberOutSectionVisibilityFingerprint.matchSingle().apply {
            val (_, _, sectionVisible, _) = instructionMatches
            method.filterPromotion(sectionVisible.index + 1, "v${(sectionVisible.instruction as OneRegisterInstruction).registerA}")
        }
        CallsIntroFingerprint.matchSingle().apply {
            val (_, flagResult, _) = instructionMatches
            method.filterPromotion(flagResult.index + 1, "v${(flagResult.instruction as OneRegisterInstruction).registerA}")
        }
    }
}

private fun MutableMethod.filterPromotion(index: Int, register: String) {
    addInstructions(
        index,
        """
            invoke-static/range { $register .. $register }, $VIBER_SETTINGS_CLASS->showPromotion(Z)Z
            move-result $register
        """,
    )
}

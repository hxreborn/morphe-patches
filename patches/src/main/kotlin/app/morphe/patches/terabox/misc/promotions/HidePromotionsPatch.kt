/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.promotions

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.terabox.misc.settings.PROMOTIONS_HIDDEN
import app.morphe.patches.terabox.misc.settings.TERABOX_PATCH_SETTINGS_CLASS
import app.morphe.patches.terabox.misc.settings.markPatchApplied
import app.morphe.patches.terabox.misc.settings.overrideWhenEnabled
import app.morphe.patches.terabox.misc.settings.patchesSettingsPatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val PREMIUM_HOME_CARD_TYPE = 6

private const val DESCRIPTION = "Removes Premium and sale offers, plus prize and campaign cards, speed-up prompts, " +
    "floating invites and coupon popups."

@Suppress("unused")
val hidePromotionsPatch = bytecodePatch(
    name = "Hide promotions",
    description = DESCRIPTION,
) {
    compatibleWith(AppCompatibilities.TERABOX)
    dependsOn(patchesSettingsPatch)

    execute {
        markPatchApplied("hidePromotions", DESCRIPTION)
        listOf(
            VipStatusSectionFingerprint,
            CreditsUnlockDividerFingerprint,
            OperationCardFingerprint,
            ActivityPopupFingerprint,
            DiscountPopupFingerprint,
            CouponPopupFingerprint,
            VipGuidePopupFingerprint,
            NewUserGiftPopupFingerprint,
            VideoPlayerUpsellFingerprint,
            AccelerateToastFingerprint,
            AccelerateTrialToastFingerprint,
            WebPlayerStutterGuideFingerprint,
            FloatWindowUpdateFingerprint,
        ).forEach { it.matchSingle().method.overrideWhenEnabled(PROMOTIONS_HIDDEN) { returnEarly() } }

        PremiumPopupLimitFingerprint.matchSingle().method.overrideWhenEnabled(PROMOTIONS_HIDDEN) { returnEarly(false) }

        SearchOperationEntryFingerprint.matchSingle().method.overrideWhenEnabled(PROMOTIONS_HIDDEN) { returnEarly(null) }

        SpeedUpSheetAutoShowFingerprint.matchSingle().apply {
            val flagResult = instructionMatches.last()
            val register = flagResult.getInstruction<OneRegisterInstruction>().registerA
            method.addInstructions(
                flagResult.index + 1,
                """
                    invoke-static/range { v$register .. v${register + 1} }, $TERABOX_PATCH_SETTINGS_CLASS->promotionAutoShow(J)J
                    move-result-wide v$register
                """,
            )
        }

        HomeCardVisibilityFingerprint.matchSingle().method.apply {
            val register = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()

            addInstructionsWithLabels(
                0,
                """
                    const/4 v$register, $PREMIUM_HOME_CARD_TYPE
                    if-ne p2, v$register, :check
                    invoke-static {}, $TERABOX_PATCH_SETTINGS_CLASS->isPromotionsHidden()Z
                    move-result v$register
                    if-eqz v$register, :check
                    const/4 v$register, 0x0
                    return v$register
                """,
                ExternalLabel("check", getInstruction(0)),
            )
        }
    }
}

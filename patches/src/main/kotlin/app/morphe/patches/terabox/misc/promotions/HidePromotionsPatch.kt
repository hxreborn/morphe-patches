/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.promotions

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.terabox.misc.fix.signature.spoofSignaturePatch
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.matchSingle

private const val PREMIUM_HOME_CARD_TYPE = 6

@Suppress("unused")
val hidePromotionsPatch = bytecodePatch(
    name = "Hide promotions",
    description = "Hides Premium upgrade cards and banners, prize cards, and sale, coupon and promotional popups.",
) {
    compatibleWith(AppCompatibilities.TERABOX)
    dependsOn(spoofSignaturePatch)

    execute {
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
        ).forEach { it.matchSingle().method.addInstructions(0, "return-void") }

        HomeCardVisibilityFingerprint.matchSingle().method.apply {
            val register = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()

            addInstructionsWithLabels(
                0,
                """
                    const/4 v$register, $PREMIUM_HOME_CARD_TYPE
                    if-ne p2, v$register, :check
                    const/4 v$register, 0x0
                    return v$register
                """,
                ExternalLabel("check", getInstruction(0)),
            )
        }
    }
}

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.hypic.misc.upselling

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val PROMOTION_SLOT_PATH = "/commerce/v1/resource_position"

@Suppress("unused")
val hideUpgradePromotionsPatch = bytecodePatch(
    name = "Hide upgrade promotions",
    description = "Hides the Pro button on the home screen, the Pro banner and promotions on the Me tab, " +
        "free trial tips in the editor and on export, and Plans & billing in Settings. " +
        "Closes subscription pages before they show.",
) {
    compatibleWith(AppCompatibilities.HYPIC)

    execute {
        HomeProButtonFingerprint.matchSingle().method.returnEarly(null)

        fun Fingerprint.treatSubscriptionsAsDisabled() = matchSingle().let { match ->
            val (_, subscriptionsEnabled) = match.instructionMatches
            val subscriptionsEnabledRegister =
                match.method.getInstruction<OneRegisterInstruction>(subscriptionsEnabled.index).registerA
            match.method.addInstruction(subscriptionsEnabled.index + 1, "const/4 v$subscriptionsEnabledRegister, 0x0")
        }
        SettingsPlansRowFingerprint.treatSubscriptionsAsDisabled()
        ProfileProEntranceFingerprint.treatSubscriptionsAsDisabled()

        val appFetch = LynxAppFetchFingerprint.matchSingle().method
        appFetch.addInstructionsWithLabels(
            0,
            """
                const-string v0, "$PROMOTION_SLOT_PATH"
                invoke-virtual { p1, v0 }, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                move-result v0
                if-eqz v0, :fetch
                const-string p1, ""
            """,
            ExternalLabel("fetch", appFetch.getInstruction(0)),
        )

        val slotRequest = PromotionSlotRequestFingerprint.matchSingle().method
        val (_, slotCallback) = slotRequest.parameterTypes
        slotRequest.addInstructionsWithLabels(
            0,
            """
                if-eqz p1, :done
                const/4 v0, 0x0
                invoke-interface { p1, v0 }, $slotCallback->onFinish(Z)V
                :done
                return-void
            """,
        )

        SubscribePageCreateFingerprint.matchSingle().let { match ->
            val (subscribePageReferenceStored) = match.instructionMatches
            match.method.addInstructions(
                subscribePageReferenceStored.index + 1,
                """
                    invoke-virtual { p0 }, Landroid/app/Activity;->finish()V
                    return-void
                """,
            )
        }
    }
}

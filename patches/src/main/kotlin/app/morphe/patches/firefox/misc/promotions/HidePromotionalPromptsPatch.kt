/*
 * Copyright (C) 2026 heval99
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from heval99/Heval-Morphe-Patches:
 * https://github.com/heval99/Heval-Morphe-Patches/commit/28097b43dbc8de99bb516a44f84708ae6cbb0f64
 * Commit 28097b43dbc8de99bb516a44f84708ae6cbb0f64 (2026-10-25),
 * patches/src/main/kotlin/app/template/patches/firefox/misc/promotions/HidePromotionalPromptsPatch.kt
 */
package app.morphe.patches.firefox.misc.promotions

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.firefox.misc.ui.PATCH_SETTINGS
import app.morphe.patches.firefox.shared.forceSettingsPreference
import app.morphe.patches.firefox.shared.listInSheet
import app.morphe.patches.firefox.shared.readPreferenceAsFalse
import app.morphe.patches.firefox.shared.whenEnabled
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val HIDE_PROMOS = "$PATCH_SETTINGS->hidePromos()Z"

@Suppress("unused")
val hidePromotionalPromptsPatch = bytecodePatch(
    name = "Hide promotional prompts",
    description = "Hides default browser, rating, sign-in, survey and promo prompts.",
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    extendWith("extensions/extension.mpe")

    execute {
        listInSheet("hidePromos")

        NextMessageFingerprint.matchSingle().method.whenEnabled(HIDE_PROMOS) { (value) ->
            """
                const/16 $value, 0x0
                return-object $value
            """
        }

        forceSettingsPreference("pref_key_microsurvey_feature_enabled", false, HIDE_PROMOS)

        MessageNotificationWorkFingerprint.matchSingle().method.whenEnabled(HIDE_PROMOS) { (result) ->
            """
                new-instance $result, $WORKER_RESULT_SUCCESS
                invoke-direct { $result }, $WORKER_RESULT_SUCCESS-><init>()V
                return-object $result
            """
        }

        DefaultBrowserPromptFingerprint.matchSingle().let { match ->
            val (_, moveResult) = match.instructionMatches
            val register = moveResult.getInstruction<OneRegisterInstruction>().registerA
            match.method.whenEnabled(HIDE_PROMOS, moveResult.index + 1, exclude = register) {
                "const/4 v$register, 0x1"
            }
        }

        OnboardingCardFilterFingerprint.matchSingle().let { match ->
            val (_, _, cardTypeCast) = match.instructionMatches
            val cardType = cardTypeCast.getInstruction<OneRegisterInstruction>().registerA
            match.method.whenEnabled(HIDE_PROMOS, cardTypeCast.index + 1, exclude = cardType) { (scratch) ->
                """
                    sget-object $scratch, $ONBOARDING_CARD_TYPE->DEFAULT_BROWSER:$ONBOARDING_CARD_TYPE
                    if-eq v$cardType, $scratch, :skip_card
                    sget-object $scratch, $ONBOARDING_CARD_TYPE->SYNC_SIGN_IN:$ONBOARDING_CARD_TYPE
                    if-ne v$cardType, $scratch, :hx_stock
                    :skip_card
                    sget-object $scratch, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
                    return-object $scratch
                """
            }
        }

        listOf("pref_key_show_menu_banner", "pref_key_setup_checklist_complete").forEach {
            readPreferenceAsFalse(it, HIDE_PROMOS)
        }

        ContinuousOnboardingFingerprint.matchSingle().method.whenEnabled(HIDE_PROMOS) { "return-void" }

        ReviewPromptStateFingerprint.matchSingle().method.whenEnabled(HIDE_PROMOS) { (unit) ->
            """
                sget-object $unit, Lkotlin/Unit;->INSTANCE:Lkotlin/Unit;
                return-object $unit
            """
        }
    }
}

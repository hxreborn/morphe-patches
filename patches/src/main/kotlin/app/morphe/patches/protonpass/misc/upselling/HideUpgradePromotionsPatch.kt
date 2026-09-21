/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.misc.upselling

import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private val Match.firstIndex
    get() = instructionMatches.first().index

private val Match.firstRegister
    get() = instructionMatches.first().getInstruction<OneRegisterInstruction>().registerA

@Suppress("unused")
val hideUpgradePromotionsPatch = bytecodePatch(
    name = "Hide upgrade promotions",
    description = "Hides the Upgrade buttons, upgrade prompts and the welcome offer after signing in. " +
        "Plan limits still apply.",
) {
    compatibleWith(AppCompatibilities.PROTON_PASS)

    execute {
        UpgradeInfoConstructorFingerprint.matchSingle().run {
            method.addInstruction(firstIndex, "const/4 v$firstRegister, 0x0")
        }

        PlanLimitReachedFingerprint.matchSingle().run {
            method.replaceInstruction(firstIndex, "const/4 v$firstRegister, 0x1")
        }

        OnboardingRouteFingerprint.matchSingle().run {
            val onboardingRoute = instructionMatches.last().getInstruction<ReferenceInstruction>().reference

            method.replaceInstruction(firstIndex, "sget-object v$firstRegister, $onboardingRoute")
        }
    }
}

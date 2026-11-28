/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.what3words.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities

private const val FREE_PLAN = "FREE"
private const val PRO_PLAN = "PRO_ANNUAL"

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks Live Scan, photo import and navigation. Chat and Voice AI are not included.",
) {
    compatibleWith(AppCompatibilities.WHAT3WORDS)

    execute {
        UpdatePlanInfoFingerprint.method.apply {
            val planType = parameterTypes.single().toString()
            val planFields = classDefBy(planType).staticFields.map { it.name }
            if (FREE_PLAN !in planFields || PRO_PLAN !in planFields) {
                throw PatchException("Plan enum has no $FREE_PLAN and $PRO_PLAN")
            }

            val freeRegister = getFreeRegisterProvider(0, 1).getFreeRegister4Bit()

            addInstructionsWithLabels(
                0,
                """
                    sget-object v$freeRegister, $planType->$FREE_PLAN:$planType
                    if-ne p1, v$freeRegister, :keep
                    sget-object p1, $planType->$PRO_PLAN:$planType
                """,
                ExternalLabel("keep", getInstruction(0)),
            )
        }
    }
}

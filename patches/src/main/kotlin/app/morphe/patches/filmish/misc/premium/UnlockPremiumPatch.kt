/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.filmish.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks Pro filters and tools, and exports without a watermark.",
) {
    compatibleWith(AppCompatibilities.FILMISH)
    dependsOn(removePairipProtectionPatch)

    execute {
        ProStateInitializerFingerprint.match().apply {
            val (_, _, booleanValueCall) = instructionMatches
            val booleanRegister = booleanValueCall.getInstruction<FiveRegisterInstruction>().registerC
            method.addInstructions(booleanValueCall.index, "const/4 v$booleanRegister, 0x1")
        }

        PurchaseRefreshFingerprint.match().apply {
            val (_, booleanValueCall) = instructionMatches
            val booleanRegister = booleanValueCall.getInstruction<FiveRegisterInstruction>().registerC
            method.addInstructions(booleanValueCall.index, "const/4 v$booleanRegister, 0x1")
        }
    }
}

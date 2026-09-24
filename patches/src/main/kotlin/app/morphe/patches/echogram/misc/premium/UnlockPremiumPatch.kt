/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.echogram.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.util.getReference
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks all premium features.",
) {
    dependsOn(removePairipProtectionPatch)
    compatibleWith(AppCompatibilities.ECHOGRAM)

    execute {
        val premiumStateClass = EntitlementSyncFingerprint.matchSingle().instructionMatches.last()
            .getInstruction<ReferenceInstruction>()
            .getReference<FieldReference>()!!
            .definingClass

        premiumStateInitializerFingerprint(premiumStateClass).matchSingle().apply {
            val mutableStateFlowFactory = instructionMatches.first().getInstruction<ReferenceInstruction>().reference
            val asStateFlowCall = instructionMatches.last()
            val register = asStateFlowCall.getInstruction<FiveRegisterInstruction>().registerC

            method.addInstructions(
                asStateFlowCall.index,
                """
                    sget-object v$register, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                    invoke-static { v$register }, $mutableStateFlowFactory
                    move-result-object v$register
                """,
            )
        }
    }
}

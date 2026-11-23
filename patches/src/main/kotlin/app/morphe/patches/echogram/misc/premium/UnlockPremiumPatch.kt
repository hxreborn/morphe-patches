/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.echogram.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks all premium features.",
) {
    dependsOn(removePairipProtectionPatch)
    compatibleWith(AppCompatibilities.ECHOGRAM)

    execute {
        val (_, entitlementStore) = EntitlementSyncFingerprint.match().instructionMatches
        val premiumStateClass = entitlementStore.fieldReference.definingClass

        premiumStateInitializerFingerprint(premiumStateClass).match().apply {
            val (mutableStateFlowCall, _, asStateFlowCall) = instructionMatches
            val mutableStateFlowFactory = mutableStateFlowCall.methodReference
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

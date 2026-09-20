/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.atvtools.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.atvtools.misc.fix.signature.disableSignatureCheckPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.getFreeRegisterProvider
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks all features and removes the ads.",
) {
    compatibleWith(AppCompatibilities.ATVTOOLS)
    dependsOn(disableSignatureCheckPatch)

    execute {
        val purchaseType = PurchaseGetProductsFingerprint.matchSingle().classDef.type

        BillingHelperConstructorFingerprint.matchSingle().apply {
            val purchasesFlowStore = instructionMatches.last()
            val purchasesFlowField = purchasesFlowStore
                .getInstruction<ReferenceInstruction>()
                .getReference<FieldReference>()!!

            method.apply {
                val index = purchasesFlowStore.index + 1
                val registers = getFreeRegisterProvider(index, 3)
                val purchase = registers.getFreeRegister4Bit()
                val scratch = registers.getFreeRegister4Bit()
                val signature = registers.getFreeRegister4Bit()

                addInstructions(
                    index,
                    """
                        new-instance v$purchase, $purchaseType
                        const-string v$scratch, "{}"
                        const-string v$signature, "morphe"
                        invoke-direct { v$purchase, v$scratch, v$signature }, $purchaseType-><init>(Ljava/lang/String;Ljava/lang/String;)V
                        invoke-static { v$purchase }, Ljava/util/Collections;->singletonList(Ljava/lang/Object;)Ljava/util/List;
                        move-result-object v$scratch
                        invoke-static { v$scratch }, Lkotlinx/coroutines/flow/StateFlowKt;->MutableStateFlow(Ljava/lang/Object;)Lkotlinx/coroutines/flow/MutableStateFlow;
                        move-result-object v$scratch
                        iput-object v$scratch, p0, $purchasesFlowField
                    """,
                )
            }
        }

        PremiumActivityOnCreateFingerprint.matchSingle().method.apply {
            val superCallIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.INVOKE_SUPER &&
                    getReference<MethodReference>()?.name == "onCreate"
            }
            addInstructions(
                superCallIndex + 1,
                """
                    invoke-virtual { p0 }, Ldev/vodik7/atvtools/PremiumActivity;->finish()V
                    return-void
                """,
            )
        }
    }
}

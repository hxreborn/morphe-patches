/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.atvtools.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patches.atvtools.misc.fix.signature.disableSignatureCheckPatch
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks all features and removes the ads.",
) {
    compatibleWith(AppCompatibilities.ATVTOOLS)
    dependsOn(disableSignatureCheckPatch)

    execute {
        val purchaseType = PurchaseGetProductsFingerprint.match().classDef.type

        BillingHelperConstructorFingerprint.match().apply {
            val (_, _, purchasesFlowStore) = instructionMatches
            val purchasesFlowField = purchasesFlowStore.fieldReference

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

        PremiumActivityOnCreateFingerprint.match().apply {
            val (superOnCreate) = instructionMatches
            method.addInstructions(
                superOnCreate.index + 1,
                """
                    invoke-virtual { p0 }, Ldev/vodik7/atvtools/PremiumActivity;->finish()V
                    return-void
                """,
            )
        }
    }
}

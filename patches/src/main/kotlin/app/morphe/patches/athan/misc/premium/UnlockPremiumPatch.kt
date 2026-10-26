/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.athan.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.athan.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val PREMIUM_SUBSCRIPTION_JSON =
    """{\"type\":\"PROMO_CODE\",\"status\":1,\"migratedProUser\":true,\"subscriptionPackName\":\"Athan Premium\"}"""

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks Athan Premium, including the premium Quran themes and athans, and removes ads.",
) {
    compatibleWith(AppCompatibilities.ATHAN)

    dependsOn(spoofSignaturePatch)

    execute {
        val method = StoredSubscriptionFingerprint.matchSingle().method
        val returns = method.instructions.withIndex()
            .filter { it.value.opcode == Opcode.RETURN_OBJECT }
            .map { it.index }
            .reversed()

        returns.forEach { index ->
            val subscription = method.getInstruction<OneRegisterInstruction>(index).registerA
            val registers = method.getFreeRegisterProvider(index, 2, listOf(subscription))
            val gson = registers.getFreeRegister4Bit()
            val type = registers.getFreeRegister4Bit()

            method.addInstructionsWithLabels(
                index,
                """
                    if-nez v$subscription, :stored
                    new-instance v$gson, Lcom/google/gson/Gson;
                    invoke-direct { v$gson }, Lcom/google/gson/Gson;-><init>()V
                    const-string v$subscription, "$PREMIUM_SUBSCRIPTION_JSON"
                    const-class v$type, $SUBSCRIPTION_CLASS
                    invoke-virtual { v$gson, v$subscription, v$type }, Lcom/google/gson/Gson;->fromJson(Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/Object;
                    move-result-object v$subscription
                    check-cast v$subscription, $SUBSCRIPTION_CLASS
                """,
                ExternalLabel("stored", method.getInstruction(index)),
            )
        }
    }
}

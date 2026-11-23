/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ivoox.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val NIBBLE_REGISTER_LIMIT = 16

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Removes audio, banner, full-screen and splash screen ads, Premium and Plus offers, " +
        "the player subscribe banner and Home alerts, " +
        "and enables car mode, volume boost and up to 20 smart lists.",
) {
    compatibleWith(AppCompatibilities.IVOOX)

    execute {
        UserSkillEnumFingerprint.match()

        StoredUserSkillsFingerprint.match().apply {
            val (_, _, readStoredKeys) = instructionMatches
            val keysRegister = readStoredKeys.getInstruction<OneRegisterInstruction>().registerA
            if (keysRegister >= NIBBLE_REGISTER_LIMIT) {
                throw PatchException("Stored skill keys register v$keysRegister needs a range call")
            }
            val insertIndex = readStoredKeys.index + 1
            val registers = method.getFreeRegisterProvider(insertIndex, 2, keysRegister)
            val keysCopyRegister = registers.getFreeRegister4Bit()
            val keyRegister = registers.getFreeRegister4Bit()
            val addSubscriberKeysSmali = SUBSCRIBER_SKILL_KEYS.joinToString("\n") { key ->
                """
                    const-string v$keyRegister, "$key"
                    invoke-interface { v$keysCopyRegister, v$keyRegister }, Ljava/util/Set;->add(Ljava/lang/Object;)Z
                """
            }

            method.addInstructionsWithLabels(
                insertIndex,
                """
                    new-instance v$keysCopyRegister, Ljava/util/LinkedHashSet;
                    invoke-direct { v$keysCopyRegister }, Ljava/util/LinkedHashSet;-><init>()V
                    if-eqz v$keysRegister, :no_stored_keys
                    invoke-interface { v$keysCopyRegister, v$keysRegister }, Ljava/util/Set;->addAll(Ljava/util/Collection;)Z
                    :no_stored_keys
                    $addSubscriberKeysSmali
                    move-object v$keysRegister, v$keysCopyRegister
                """,
            )
        }

        PremiumPurchasedFingerprint.method.returnEarly(true)
        ProContractFingerprint.method.returnEarly(true)
        HidePlusFingerprint.method.returnEarly(true)
        DynamicAlertClosedFingerprint.method.returnEarly(true)

        PlayerCtaBannerFingerprint.method.apply {
            val resultConstructorIndex = indexOfFirstInstructionReversedOrThrow {
                val reference = getReference<MethodReference>()
                opcode == Opcode.INVOKE_DIRECT && reference?.name == "<init>" && reference.parameterTypes.size == 1
            }
            val resultConstructor = getInstruction(resultConstructorIndex).getReference<MethodReference>()!!
            val registers = getFreeRegisterProvider(0, 2)
            val resultRegister = registers.getFreeRegister4Bit()
            val noBannerRegister = registers.getFreeRegister4Bit()

            addInstructions(
                0,
                """
                    new-instance v$resultRegister, ${resultConstructor.definingClass}
                    const/4 v$noBannerRegister, 0x0
                    invoke-direct { v$resultRegister, v$noBannerRegister }, $resultConstructor
                    return-object v$resultRegister
                """,
            )
        }

        ActiveSplashAdFingerprint.method.returnEarly(null)
        PlayerMediaItemFingerprint.method.addInstruction(0, "const/4 p1, 0x0")
    }
}

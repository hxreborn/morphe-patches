/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.clink.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.removeInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.util.findInstructionIndicesReversed
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.util.MethodUtil

@Suppress("unused")
val unlockProPatch = bytecodePatch(
    name = "Unlock Pro",
    description = "Unlocks Clink Pro.",
) {
    compatibleWith(AppCompatibilities.CLINK)
    dependsOn(removePairipProtectionPatch)

    execute {
        val publish = PublishEntitlementFingerprint.matchSingle()
        val holder = publish.classDef
        val purchasedField = publish.instructionMatches.first().instruction.getReference<FieldReference>()!!
        val purchasedFieldDescriptor = descriptor(purchasedField)

        var writes = 0
        holder.methods.forEach { method ->
            if (method.implementation == null) return@forEach

            method.findInstructionIndicesReversed {
                opcode == Opcode.IPUT_BOOLEAN && getReference<FieldReference>() == purchasedField
            }.forEach { index ->
                val write = method.getInstruction<TwoRegisterInstruction>(index)
                method.addInstructions(
                    index + 1,
                    """
                        const/4 v${write.registerA}, 0x1
                        iput-boolean v${write.registerA}, v${write.registerB}, $purchasedFieldDescriptor
                    """,
                )
                writes++
            }
        }
        check(writes > 0) { "No writes to $purchasedFieldDescriptor found" }

        ApplyPurchasesFingerprint.match(holder).let { apply ->
            val ownsLifetimeResult = apply.instructionMatches.last()
            val register = ownsLifetimeResult.getInstruction<OneRegisterInstruction>().registerA
            apply.method.replaceInstruction(ownsLifetimeResult.index, "const/4 v$register, 0x1")
        }

        LoadStoredEntitlementFingerprint.match(holder).method.apply {
            val publishCallIndex = indexOfFirstInstructionReversedOrThrow {
                opcode == Opcode.INVOKE_VIRTUAL &&
                    getReference<MethodReference>()?.let { MethodUtil.methodSignaturesMatch(it, publish.method) } == true
            }
            removeInstruction(indexOfFirstInstructionReversedOrThrow(publishCallIndex, Opcode.IF_EQZ))
        }
    }
}

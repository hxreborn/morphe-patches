/*
 * Copyright (C) 2026 Bogat25
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.pocketwhip.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.descriptor
import app.morphe.util.findInstructionIndicesReversed
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks all whips.",
) {
    compatibleWith(AppCompatibilities.POCKET_WHIP)

    execute {
        val toString = WhipToStringFingerprint.matchSingle()
        val whipClass = toString.classDef

        val append = toString.instructionMatches.last()
        val flagRegister = append.getInstruction<FiveRegisterInstruction>().registerD
        val ownedField = toString.method.let { method ->
            val readIndex = method.indexOfFirstInstructionReversedOrThrow(append.index) {
                opcode == Opcode.IGET_BOOLEAN &&
                    (this as TwoRegisterInstruction).registerA == flagRegister &&
                    getReference<FieldReference>()?.definingClass == whipClass.type
            }
            method.getInstruction(readIndex).getReference<FieldReference>()!!
        }
        val ownedFieldDescriptor = descriptor(ownedField)

        var writes = 0
        whipClass.methods.forEach { method ->
            if (method.implementation == null) return@forEach

            method.findInstructionIndicesReversed {
                opcode == Opcode.IPUT_BOOLEAN && getReference<FieldReference>() == ownedField
            }.forEach { index ->
                val write = method.getInstruction<TwoRegisterInstruction>(index)

                method.addInstructions(
                    index + 1,
                    """
                        const/4 v${write.registerA}, 0x1
                        iput-boolean v${write.registerA}, v${write.registerB}, $ownedFieldDescriptor
                    """,
                )
                writes++
            }
        }
        check(writes > 0) { "No writes to $ownedFieldDescriptor found" }
    }
}

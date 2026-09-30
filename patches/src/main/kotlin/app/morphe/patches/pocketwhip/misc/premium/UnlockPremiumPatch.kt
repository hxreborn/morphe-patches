/*
 * Copyright (C) 2026 Bogat25
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.pocketwhip.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionReversedOrThrow
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.util.ReferenceUtil

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock premium",
    description = "Unlocks all whips.",
) {
    compatibleWith(AppCompatibilities.POCKET_WHIP)

    execute {
        val toString = WhipToStringFingerprint.matchSingle()
        val whipClass = toString.classDef

        // The ownership flag is the field whose value toString() appends after the ", isAvailable=" label.
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
        val ownedFieldDescriptor = ReferenceUtil.getFieldDescriptor(ownedField)

        // The app only reads ownership from this field, both through its getters and when unlocking
        // purchases. Storing true after every write makes each whip owned as soon as it is created, on
        // every launch, without depending on product IDs, saved purchases or which whips exist.
        var writes = 0
        whipClass.methods.forEach { method ->
            val instructions = method.implementation?.instructions?.toList() ?: return@forEach

            instructions.withIndex().filter { (_, instruction) ->
                instruction.opcode == Opcode.IPUT_BOOLEAN &&
                    instruction.getReference<FieldReference>() == ownedField
            }.reversed().forEach { (index, instruction) ->
                val write = instruction as TwoRegisterInstruction

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

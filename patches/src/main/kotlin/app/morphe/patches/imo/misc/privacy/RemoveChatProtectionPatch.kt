/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.matchAllMethodIndicesForEach
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val removeChatProtectionPatch = bytecodePatch(
    name = "Remove chat protection",
    description = "Allows copying, sharing, forwarding and downloading messages protected by the sender's chat privacy settings or Time Machine.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        val protectedMessageMethod = ProtectedMessageFingerprint.matchSingle().method
        val protectionFlag = protectedMessageMethod
            .getInstruction(protectedMessageMethod.indexOfFirstInstructionOrThrow(protectionFlagRead))
            .getReference<FieldReference>()!!

        fieldAccess(protectionFlag, Opcode.SPUT_BOOLEAN).matchAllMethodIndicesForEach { index ->
            val register = getInstruction<OneRegisterInstruction>(index).registerA
            addInstructionsAtControlFlowLabel(index, "const/4 v$register, 0x0")
        }
        protectedMessageMethod.returnEarly(false)

        CopyAvailabilityFingerprint.matchSingle().method.apply {
            val checkIndex = indexOfFirstInstructionOrThrow(copyProtectionCheck)
            val resultIndex = indexOfFirstInstructionOrThrow(checkIndex, Opcode.MOVE_RESULT)
            val register = getInstruction<OneRegisterInstruction>(resultIndex).registerA
            addInstruction(resultIndex + 1, "const/4 v$register, 0x0")
        }
    }
}

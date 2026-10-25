/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchAllMethodIndicesForEach
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.imo.shared.enumConstantField
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.addInstructionsAtControlFlowLabel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val removeChatProtectionPatch = bytecodePatch(
    name = "Remove chat protection",
    description = "Allows copying, sharing, forwarding and downloading messages in chats with chat privacy or Time Machine turned on.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        val protectedMessage = ProtectedMessageFingerprint.matchSingle()
        val protectionFlag = protectedMessage.instructionMatches.first().instruction.getReference<FieldReference>()!!

        fieldAccess(protectionFlag, Opcode.SPUT_BOOLEAN).matchAllMethodIndicesForEach { index ->
            val register = getInstruction<OneRegisterInstruction>(index).registerA
            addInstructionsAtControlFlowLabel(index, "const/4 v$register, 0x0")
        }
        protectedMessage.method.returnEarly(false)

        copyAvailabilityFingerprint(enumConstantField("COPY_DISABLE_BY_PRIVACY")).matchSingle().apply {
            val (_, copyCheckResult) = instructionMatches
            val register = copyCheckResult.getInstruction<OneRegisterInstruction>().registerA
            method.addInstruction(copyCheckResult.index + 1, "const/4 v$register, 0x0")
        }
    }
}

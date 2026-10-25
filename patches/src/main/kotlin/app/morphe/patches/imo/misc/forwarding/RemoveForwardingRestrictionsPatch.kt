/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.forwarding

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.addInstructionsAtControlFlowLabel
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.indexOfFirstInstructionReversed
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val removeForwardingRestrictionsPatch = bytecodePatch(
    name = "Remove forwarding restrictions",
    description = "Allows forwarding disappearing and view-once text messages.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        ShareAvailabilityFingerprint.matchSingle().method.apply {
            val shareableParameter = implementation!!.registerCount - 1
            val shareableCheckIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.IF_EQZ && (this as OneRegisterInstruction).registerA == shareableParameter
            }
            val afterEarlyReturnIndex = indexOfFirstInstructionReversed(shareableCheckIndex, Opcode.RETURN_VOID) + 1
            addInstructionsAtControlFlowLabel(
                afterEarlyReturnIndex,
                "goto :shareable",
                ExternalLabel("shareable", getInstruction(shareableCheckIndex)),
            )
        }
    }
}

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.forwarding

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.indexOfFirstInstructionOrThrow
import app.morphe.util.matchSingle
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
            val privacyIndex = indexOfFirstInstructionOrThrow(privacyShareDisable)
            val shareableCheckIndex = indexOfFirstInstructionOrThrow(privacyIndex, Opcode.IF_EQZ)
            val shareableBranch = getInstruction<OneRegisterInstruction>(shareableCheckIndex)
            if (shareableBranch.registerA != implementation!!.registerCount - 1) {
                throw PatchException("Share action check does not test the shareable parameter")
            }
            addInstructionsWithLabels(0, "goto :shareable", ExternalLabel("shareable", shareableBranch))
        }
    }
}

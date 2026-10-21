/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.graphmessenger.misc.update

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.graphmessenger.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.util.matchSingle
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val removeForcedUpdatePatch = bytecodePatch(
    name = "Remove forced update",
    description = "Removes the dialog that blocks the app when Graph marks this version unsupported.",
) {
    compatibleWith(AppCompatibilities.GRAPH_MESSENGER)
    availability(requireArm64)

    dependsOn(spoofSignaturePatch)

    execute {
        val match = ForcedUpdateVersionFingerprint.matchSingle()
        val moveResult = match.instructionMatches.last().index
        val register = match.method.getInstruction<OneRegisterInstruction>(moveResult).registerA
        match.method.addInstruction(moveResult + 1, "const/16 v$register, 0x0")
    }
}

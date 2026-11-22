/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.graphmessenger.misc.update

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patches.graphmessenger.misc.fix.signature.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val removeForcedUpdatePatch = patch(
    name = "Remove forced update",
    description = "Removes the dialog that blocks the app when Graph marks this version unsupported.",
) {
    compatibleWith(AppCompatibilities.GRAPH_MESSENGER)
    availability(requireArm64)

    dependsOn(spoofSignaturePatch)

    execute {
        val match = ForcedUpdateVersionFingerprint.match()
        val (_, _, forcedUpdateVersionResult) = match.instructionMatches
        val moveResult = forcedUpdateVersionResult.index
        val register = forcedUpdateVersionResult.getInstruction<OneRegisterInstruction>().registerA
        match.method.addInstruction(moveResult + 1, "const/16 v$register, 0x0")
    }
}

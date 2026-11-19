/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.allvideoplayer.resume

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getReference
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.smali.descriptor
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

@Suppress("unused")
val resumeAfterScreenLockPatch = bytecodePatch(
    name = "Resume after screen lock",
    description = "Resumes videos where playback stopped after the screen locks, including videos the library " +
        "has not listed yet.",
) {
    compatibleWith(AppCompatibilities.ALL_VIDEO_PLAYER)

    execute {
        RestoreSavedPositionFingerprint.matchSingle().apply {
            val (_, _, missingEntry, _, sessionPosition) = instructionMatches
            val read = sessionPosition.getInstruction<TwoRegisterInstruction>()
            val field = method.getInstruction(sessionPosition.index).getReference<FieldReference>()!!
            method.replaceInstruction(
                missingEntry.index,
                "iget-object v${read.registerA}, v${read.registerB}, ${descriptor(field)}",
            )
        }
    }
}

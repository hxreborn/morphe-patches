/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.story

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val allowStoryRecordingInCallPatch = bytecodePatch(
    name = "Allow story recording during calls",
    description = "Allows recording and posting stories while a call is active.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        DisableRecordStoryInCallFingerprint.matchSingle().method.returnEarly(false)

        CameraCallConflictFingerprint.matchAll(2..2).forEach { match ->
            match.instructionMatches.filter { it.instruction.opcode == Opcode.MOVE_RESULT }.forEach { inCallResult ->
                val register = inCallResult.getInstruction<OneRegisterInstruction>().registerA
                match.method.replaceInstruction(inCallResult.index, "const/4 v$register, 0x0")
            }
        }
    }
}

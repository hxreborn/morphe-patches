/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.wattpad.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val hideAdsPatch = bytecodePatch(
    name = "Hide ads",
    description = "Hides ads in the library, story pages, reader and comments. " +
        "Rewarded videos for coins still play.",
) {
    compatibleWith(AppCompatibilities.WATTPAD)

    execute {
        val match = AdsFreeHourOverrideFingerprint.matchSingle()
        val adsFreeHourActive = match.instructionMatches.first { it.instruction.opcode == Opcode.MOVE_RESULT }
        val register = adsFreeHourActive.getInstruction<OneRegisterInstruction>().registerA
        match.method.addInstruction(adsFreeHourActive.index + 1, "const/4 v$register, 0x1")
    }
}

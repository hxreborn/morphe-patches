/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.allvideoplayer.splash

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val skipSplashDelayPatch = bytecodePatch(
    name = "Skip splash delay",
    description = "Opens the app without waiting on the splash screen.",
) {
    compatibleWith(AppCompatibilities.ALL_VIDEO_PLAYER)

    execute {
        SplashDelayFingerprint.matchSingle().apply {
            val delay = instructionMatches.first()
            val register = delay.getInstruction<OneRegisterInstruction>().registerA
            method.replaceInstruction(delay.index, "const/16 v$register, 0x0")
        }
    }
}

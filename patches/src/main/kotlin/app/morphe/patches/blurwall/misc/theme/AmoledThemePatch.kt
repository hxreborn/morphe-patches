/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.blurwall.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val amoledThemePatch = patch(
    name = "AMOLED dark theme",
    description = "Replaces the dark theme background with pure black.",
) {
    compatibleWith(AppCompatibilities.BLURWALL)

    execute {
        ColorPaletteFingerprint.match().apply {
            val backgroundColor = instructionMatches.single()
            val register = backgroundColor.getInstruction<OneRegisterInstruction>().registerA
            method.replaceInstruction(backgroundColor.index, "const-wide v$register, 0xff000000L")
        }
    }
}

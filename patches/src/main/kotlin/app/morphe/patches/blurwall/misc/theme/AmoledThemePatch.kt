/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.blurwall.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.indexOfFirstLiteralInstructionOrThrow
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val amoledThemePatch = bytecodePatch(
    name = "AMOLED dark theme",
    description = "Replaces the dark theme background with pure black.",
) {
    compatibleWith(AppCompatibilities.BLURWALL)

    execute {
        ColorPaletteFingerprint.matchSingle().method.apply {
            val index = indexOfFirstLiteralInstructionOrThrow(DARK_BACKGROUND_COLOR)
            val register = getInstruction<OneRegisterInstruction>(index).registerA
            replaceInstruction(index, "const-wide v$register, 0xff000000L")
        }
    }
}

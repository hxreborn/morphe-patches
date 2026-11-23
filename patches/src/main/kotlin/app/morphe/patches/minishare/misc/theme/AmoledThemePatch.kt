/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.minishare.misc.theme

import app.morphe.patcher.InstructionFilter
import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

@Suppress("unused")
val amoledThemePatch = patch(
    name = "AMOLED dark theme",
    description = "Replaces the light theme with a pure black dark theme.",
) {
    compatibleWith(AppCompatibilities.MINISHARE)

    dependsOn(amoledThemeResourcesPatch)

    execute {
        HomeBackgroundPaintFingerprint.match().let { match ->
            val setColorIndex = match.instructionMatches.single { it.filter === homeBackgroundSetColor }.index
            val colorRegister = match.method.getInstruction<FiveRegisterInstruction>(setColorIndex).registerD
            match.method.addInstruction(setColorIndex, "const v$colorRegister, $AMOLED_BLACK")
        }

        StatusBarTintFingerprint.match().clearTranslucentStatusFlag(statusBarTintTranslucentFlag)
        SplashTranslucentStatusFingerprint.match().clearTranslucentStatusFlag(splashTranslucentStatusFlag)

        BrandHeaderBackgroundFingerprint.matchAll(2..2).forEach { it.replaceColorString(AMOLED_BLACK) }
        RadarLabelColorFingerprint.match().replaceColorString(radarLabelInkColor())
    }
}

private fun Match.clearTranslucentStatusFlag(flagFilter: InstructionFilter) {
    val index = instructionMatches.single { it.filter === flagFilter }.index
    val register = method.getInstruction<OneRegisterInstruction>(index).registerA
    method.replaceInstruction(index, "const/16 v$register, 0x0")
}

private fun Match.replaceColorString(argb: Int) {
    val (colorString, _, _) = instructionMatches
    val index = colorString.index
    val register = colorString.getInstruction<OneRegisterInstruction>().registerA
    method.replaceInstruction(index, "const-string v$register, \"#%08x\"".format(argb))
}

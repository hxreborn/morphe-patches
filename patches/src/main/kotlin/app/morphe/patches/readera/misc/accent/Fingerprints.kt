/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.readera.misc.accent

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patches.readera.shared.AppCompatAttachBaseContextFingerprint
import com.android.tools.smali.dexlib2.Opcode

internal const val JUMP_BAR_COLOR = 0xFF33B5E5.toInt()

internal object AppCompatConfigurationChangedFingerprint : Fingerprint(
    classFingerprint = AppCompatAttachBaseContextFingerprint,
    name = "onConfigurationChanged",
    returnType = "V",
    parameters = listOf("Landroid/content/res/Configuration;"),
    filters = listOf(
        methodCall(opcode = Opcode.INVOKE_SUPER, name = "onConfigurationChanged"),
    ),
)

internal object JumpBarColorFingerprint : Fingerprint(
    definingClass = "Lorg/readera/read/widget/ReadJumpbar;",
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        literal(JUMP_BAR_COLOR),
        methodCall(definingClass = "Landroid/graphics/Paint;", name = "setColor", location = MatchAfterImmediately()),
        methodCall(definingClass = "Landroid/widget/ProgressBar;", name = "getProgressDrawable"),
        methodCall(definingClass = "Landroid/widget/AbsSeekBar;", name = "getThumb"),
    ),
)

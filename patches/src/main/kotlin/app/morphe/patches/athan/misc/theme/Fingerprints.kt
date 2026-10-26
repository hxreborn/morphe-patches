/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.athan.misc.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

private const val SET_APPLICATION_NIGHT_MODE = "Landroid/app/UiModeManager;->setApplicationNightMode(I)V"

internal object SetApplicationNightModeFingerprint : Fingerprint(
    filters = listOf(methodCall(smali = SET_APPLICATION_NIGHT_MODE)),
)

internal object ComposeDisallowForceDarkFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/view/View;"),
    filters = listOf(methodCall(smali = "Landroid/view/View;->setForceDarkAllowed(Z)V")),
)

internal object DefaultNightModeFingerprint : Fingerprint(
    name = "onCreate",
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        opcode(Opcode.SGET),
        literal(1, location = MatchAfterImmediately()),
        opcode(Opcode.IF_EQ, location = MatchAfterImmediately()),
        opcode(Opcode.SPUT, location = MatchAfterImmediately()),
    ),
    strings = listOf("uimode"),
)

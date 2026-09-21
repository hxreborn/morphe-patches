/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.misc.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

private const val COLOR_ROLE_COUNT = 68
private const val BRUSH_COUNT = 2

internal object DarkPassColorsFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(
        methodCall(
            name = "<init>",
            parameters = List(COLOR_ROLE_COUNT) { "J" } + List(BRUSH_COUNT) { "L" },
            returnType = "V",
            opcode = Opcode.INVOKE_DIRECT_RANGE,
        ),
        fieldAccess(name = "Dark", opcode = Opcode.SPUT_OBJECT, location = MatchAfterWithin(4)),
    ),
)

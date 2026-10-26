/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.arcplayer.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object PremiumStateInitFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(
        string("isPremiumUser"),
        opcode(Opcode.MOVE_RESULT),
        opcode(Opcode.SPUT_BOOLEAN, location = MatchAfterImmediately()),
    ),
)

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.allvideoplayer.rate

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.literal
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object RatingPromptGateFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        string("rateShow"),
        string("folderRate"),
        literal(3),
        opcode(Opcode.IF_EQ, location = MatchAfterImmediately()),
    ),
)

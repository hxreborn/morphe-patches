/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.allvideoplayer.resume

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal object PlayVideoFingerprint : Fingerprint(
    definingClass = "Lcom/allformatplayer/streamvideoplayer/feature/player/MyPlayerActivity;",
    returnType = "L",
    parameters = listOf("Landroid/net/Uri;"),
)

internal object RestoreSavedPositionFingerprint : Fingerprint(
    strings = listOf("ResumeVideoPly"),
    filters = listOf(
        opcode(Opcode.NEW_INSTANCE),
        opcode(Opcode.GOTO),
        opcode(Opcode.MOVE_OBJECT, location = MatchAfterImmediately()),
        opcode(Opcode.GOTO, location = MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.IGET_OBJECT, type = "Ljava/lang/Long;", location = MatchAfterImmediately()),
    ),
)

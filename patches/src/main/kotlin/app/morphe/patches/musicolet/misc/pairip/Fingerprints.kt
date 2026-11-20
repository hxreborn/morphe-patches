/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.musicolet.misc.pairip

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

private const val MUSIC_ACTIVITY = "Lin/krosbits/musicolet/MusicActivity;"
private const val HASH_MAP = "Ljava/util/HashMap;"
internal const val CALENDAR = "Ljava/util/Calendar;"

internal object DateChangeRefreshFingerprint : Fingerprint(
    name = "onPostExecute",
    returnType = "V",
    parameters = listOf("Ljava/lang/Object;"),
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET_OBJECT, definingClass = MUSIC_ACTIVITY, type = MUSIC_ACTIVITY),
        fieldAccess(opcode = Opcode.IGET_BOOLEAN),
        fieldAccess(opcode = Opcode.SGET_OBJECT, type = HASH_MAP),
        methodCall(definingClass = HASH_MAP, name = "clear", location = MatchAfterImmediately()),
    ),
    custom = { _, classDef -> classDef.superclass == "Landroid/os/AsyncTask;" },
)

internal object TodayCalendarInitFingerprint : Fingerprint(
    name = "<clinit>",
    returnType = "V",
    filters = listOf(
        methodCall(definingClass = CALENDAR, name = "getInstance", parameters = emptyList()),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        fieldAccess(opcode = Opcode.SPUT_OBJECT, type = CALENDAR, location = MatchAfterImmediately()),
    ),
)

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vpnsuper.misc.notification

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal const val STOP_CONNECTION_ACTION = "com.superunlimited.ACTION_STOP_CONNECTION"
private const val STOP_SERVICE_ACTION = "com.superunlimited.ACTION_STOP_SERVICE"
private const val DISCONNECT_DEEP_LINK = "vpnsuper:///notification/click_stop_connection"

internal object DisconnectActionFingerprint : Fingerprint(
    filters = listOf(
        string(DISCONNECT_DEEP_LINK),
        methodCall(opcode = Opcode.INVOKE_STATIC, returnType = "Landroid/content/Intent;", location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT_OBJECT, location = MatchAfterImmediately()),
        methodCall(definingClass = "Landroid/app/PendingIntent;", name = "getActivity", location = MatchAfterImmediately()),
    ),
)

internal object StopActionStringsFingerprint : Fingerprint(
    strings = listOf(STOP_CONNECTION_ACTION, STOP_SERVICE_ACTION),
)

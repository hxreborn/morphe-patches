/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vpnsuper.misc.notification

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal const val STOP_CONNECTION_ACTION = "com.superunlimited.ACTION_STOP_CONNECTION"
private const val STOP_SERVICE_ACTION = "com.superunlimited.ACTION_STOP_SERVICE"
private const val DISCONNECT_DEEP_LINK = "vpnsuper:///notification/click_stop_connection"
internal const val WIDGET_ACTION_RECEIVER_CLASS =
    "Lcom/superunlimited/feature/widget/vpnConnection/presentation/ConnectionWidgetActionReceiver;"
private const val FLAG_WRITE_WINDOW = 3

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

internal object ManualDisconnectFlagWriteFingerprint : Fingerprint(
    definingClass = WIDGET_ACTION_RECEIVER_CLASS,
    filters = listOf(
        fieldAccess(opcode = Opcode.IGET_OBJECT, definingClass = "this"),
        methodCall(
            opcode = Opcode.INVOKE_INTERFACE,
            name = "getValue",
            returnType = "Ljava/lang/Object;",
            location = MatchAfterImmediately(),
        ),
        methodCall(
            opcode = Opcode.INVOKE_VIRTUAL,
            parameters = listOf("Z"),
            returnType = "V",
            location = MatchAfterWithin(FLAG_WRITE_WINDOW),
        ),
    ),
)

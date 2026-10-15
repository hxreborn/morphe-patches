/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.externalip

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patches.netanalyzer.shared.composable
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method

internal const val EXTERNAL_IP_ROW = "ExternalIpRow"
private const val BUTTON_ROW = "ButtonRow"
private const val STATE_VALUE_METHOD = "getValue"

internal object ButtonRowFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(composable(BUTTON_ROW)),
)

internal fun externalIpRowFingerprint(buttonRow: Method) = Fingerprint(
    filters = listOf(
        composable(EXTERNAL_IP_ROW),
        methodCall(name = STATE_VALUE_METHOD, opcode = Opcode.INVOKE_INTERFACE),
        methodCall(reference = buttonRow),
    ),
)

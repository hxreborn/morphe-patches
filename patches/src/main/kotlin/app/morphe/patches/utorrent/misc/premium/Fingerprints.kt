/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.utorrent.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

private const val PRO_LABEL_DISTANCE = 3

internal object ProNavigationFingerprint : Fingerprint(
    definingClass = "Lcom/bittorrent/app/main/MainActivity;",
    returnType = "V",
    parameters = emptyList(),
    filters = listOf(
        methodCall(parameters = emptyList(), returnType = "Z", opcode = Opcode.INVOKE_STATIC),
        fieldAccess(name = "str_pro_setting", opcode = Opcode.SGET, location = MatchAfterWithin(PRO_LABEL_DISTANCE)),
    ),
)

internal fun proStatusGetterFingerprint(statusType: String) = Fingerprint(
    returnType = statusType,
    parameters = emptyList(),
)

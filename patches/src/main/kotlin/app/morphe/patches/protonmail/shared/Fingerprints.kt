/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object IoDispatcherFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    strings = listOf("Dispatchers.IO"),
)

internal object CoroutineSuspendedFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(string("COROUTINE_SUSPENDED"), fieldAccess(opcode = Opcode.SPUT_OBJECT)),
)

internal object ResultFailureFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    filters = listOf(string("Failure(")),
)

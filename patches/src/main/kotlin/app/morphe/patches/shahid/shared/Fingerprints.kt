/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shahid.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.Opcode

internal object PlayoutToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    strings = listOf(" : url=", "; advertisements=["),
)

internal object PlayoutMediaToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    strings = listOf("PlayoutMedia(url=", ", fhdResolution="),
)

internal fun fieldGetterFingerprint(classFingerprint: Fingerprint, field: String) = Fingerprint(
    classFingerprint = classFingerprint,
    parameters = emptyList(),
    filters = listOf(fieldAccess(opcode = Opcode.IGET_OBJECT, name = field)),
    custom = { method, _ -> method.returnType.startsWith("L") && method.name != "toString" },
)

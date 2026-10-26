/*
 * Copyright (C) 2026 heval99
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from heval99/Heval-Morphe-Patches:
 * https://github.com/heval99/Heval-Morphe-Patches/commit/28097b43dbc8de99bb516a44f84708ae6cbb0f64
 * Commit 28097b43dbc8de99bb516a44f84708ae6cbb0f64 (2026-10-25),
 * patches/src/main/kotlin/app/template/patches/firefox/shared/FirefoxBytecode.kt
 */
package app.morphe.patches.firefox.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val SETTINGS = "Lorg/mozilla/fenix/utils/Settings;"
private const val STRING_RESOURCES = "Lorg/mozilla/fenix/R\$string;"

internal fun settingsPreferenceDelegateFingerprint(preferenceKey: String) = Fingerprint(
    definingClass = SETTINGS,
    name = "<init>",
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET, definingClass = STRING_RESOURCES, name = preferenceKey),
        fieldAccess(opcode = Opcode.IPUT_OBJECT, definingClass = SETTINGS, location = MatchAfterWithin(10)),
    ),
)

internal fun settingsPreferenceGetterFingerprint(delegate: FieldReference) = Fingerprint(
    definingClass = SETTINGS,
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(fieldAccess(reference = delegate, opcode = Opcode.IGET_OBJECT)),
)

internal fun settingsPreferenceRowFingerprint(preferenceKey: String) = Fingerprint(
    filters = listOf(
        fieldAccess(opcode = Opcode.SGET, definingClass = STRING_RESOURCES, name = preferenceKey),
        methodCall(
            definingClass = "Lorg/mozilla/fenix/settings/ExtensionsKt;",
            name = "requirePreference",
            location = MatchAfterImmediately(),
        ),
        opcode(Opcode.MOVE_RESULT_OBJECT, MatchAfterImmediately()),
        opcode(Opcode.CHECK_CAST, MatchAfterImmediately()),
    ),
)

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val SETTINGS_DELEGATE_SUFFIX = "SettingsDelegate;"

internal abstract class RemoteSettingFingerprint(getterName: String) :
    Fingerprint(definingClass = SETTINGS_DELEGATE_SUFFIX, name = getterName)

internal fun storedFlagGetterFingerprint(flag: FieldReference) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(fieldAccess(flag, Opcode.SGET_OBJECT)),
)

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.netanalyzer.misc.devicenames

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private const val DEVICE_NAME_MAP_TYPE = "Ljava/util/HashMap;"
private const val BUILD_CLASS = "Landroid/os/Build;"
private const val BUILD_MODEL = "MODEL"

internal object LanDeviceNameFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
    filters = listOf(
        fieldAccess(type = DEVICE_NAME_MAP_TYPE, opcode = Opcode.SGET_OBJECT),
        fieldAccess(definingClass = BUILD_CLASS, name = BUILD_MODEL, opcode = Opcode.SGET_OBJECT),
    ),
)

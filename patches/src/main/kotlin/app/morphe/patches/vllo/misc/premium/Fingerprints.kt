/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vllo.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object EntitlementEnumFingerprint : Fingerprint(
    name = "<clinit>",
    strings = listOf("ALL_FEATURES", "REMOVE_ADS", "WATERMARK"),
    filters = listOf(
        string("ALL_FEATURES"),
        fieldAccess(opcode = Opcode.SPUT_OBJECT),
    ),
)

internal fun premiumEntitlementCheckFingerprint(entitlementClass: String, allFeaturesField: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        fieldAccess(definingClass = entitlementClass, name = allFeaturesField, opcode = Opcode.SGET_OBJECT),
        methodCall(name = "contains"),
    ),
)

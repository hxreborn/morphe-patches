/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.forwarding

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal val privacyShareDisable = fieldAccess(name = "SHARE_DISABLE_BY_PRIVACY", opcode = Opcode.SGET_OBJECT)

internal object ShareAvailabilityFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf("L", "Ljava/util/LinkedHashSet;", "Z"),
    filters = listOf(
        fieldAccess(name = "SHARE_DISABLE_BY_BURN", opcode = Opcode.SGET_OBJECT),
        privacyShareDisable,
        opcode(Opcode.IF_EQZ),
    ),
)

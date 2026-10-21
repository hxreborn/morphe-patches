/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.graphmessenger.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patches.graphmessenger.shared.licenseDaysLeft
import app.morphe.patches.graphmessenger.shared.licenseExpired
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal object UserIsPremiumFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IGET_BOOLEAN,
            definingClass = "Lorg/telegram/tgnet/TLRPC\$User;",
            name = "premium",
        ),
    ),
)

internal object LicenseDaysLeftFingerprint : Fingerprint(
    filters = listOf(licenseDaysLeft),
)

internal object LicenseExpiredFingerprint : Fingerprint(
    filters = listOf(licenseExpired),
)

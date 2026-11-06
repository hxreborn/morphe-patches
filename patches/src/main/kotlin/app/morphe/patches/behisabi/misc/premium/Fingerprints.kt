/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.behisabi.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object SubscriptionRepositoryFingerprint : Fingerprint(
    strings = listOf("behisabi_subscription_prefs"),
)

internal fun publishProStateFingerprint(tierType: String) = Fingerprint(
    classFingerprint = SubscriptionRepositoryFingerprint,
    isStatic = false,
    filters = listOf(
        methodCall(definingClass = "Ljava/lang/Boolean;", name = "valueOf"),
        fieldAccess(opcode = Opcode.IGET_BOOLEAN),
        fieldAccess(type = tierType, opcode = Opcode.IGET_OBJECT),
    ),
)

internal object LifetimeTierFingerprint : Fingerprint(
    filters = listOf(
        string("LIFETIME"),
        fieldAccess(opcode = Opcode.SPUT_OBJECT),
    ),
)

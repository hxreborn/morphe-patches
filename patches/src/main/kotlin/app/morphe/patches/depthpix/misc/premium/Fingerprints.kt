/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.depthpix.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal object UserToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    filters = listOf(
        string("User(name="),
        string(", is_subscribed="),
        fieldAccess(definingClass = "this", type = "Z", opcode = Opcode.IGET_BOOLEAN),
        string(", is_subscription_lifetime="),
        fieldAccess(definingClass = "this", type = "Ljava/lang/Boolean;", opcode = Opcode.IGET_OBJECT),
    ),
)

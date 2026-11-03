/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.forwarding

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

internal object ShareAvailabilityFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    filters = listOf(methodCall(definingClass = "Ljava/util/Set;", name = "add")),
    custom = { method, _ ->
        method.parameterTypes.map(CharSequence::toString).drop(1).take(2) == listOf("Ljava/util/LinkedHashSet;", "Z")
    },
)

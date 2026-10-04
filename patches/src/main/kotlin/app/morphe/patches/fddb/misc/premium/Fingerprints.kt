/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.fddb.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object HasPremiumFingerprint : Fingerprint(
    name = "getHasPremium",
    returnType = "Z",
    parameters = emptyList(),
)

internal fun premiumMembershipCheckFingerprint(hasPremiumGetter: MethodReference) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "Z",
    parameters = listOf(hasPremiumGetter.definingClass),
    filters = listOf(methodCall(hasPremiumGetter)),
)

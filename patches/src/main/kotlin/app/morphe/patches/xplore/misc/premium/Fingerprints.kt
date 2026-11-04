/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.xplore.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import com.android.tools.smali.dexlib2.Opcode

internal object BillingInitializerFingerprint : Fingerprint(
    name = "<clinit>",
    strings = listOf("X-plore unlocker", "X-plore shop"),
)

internal object HasDonationFingerprint : Fingerprint(
    classFingerprint = BillingInitializerFingerprint,
    isStatic = true,
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        methodCall(definingClass = "Ljava/util/Collection;", name = "isEmpty"),
        opcode(Opcode.XOR_INT_LIT8),
    ),
)

internal object FeatureRestrictionFingerprint : Fingerprint(
    classFingerprint = BillingInitializerFingerprint,
    isStatic = true,
    returnType = "Z",
    parameters = listOf("L"),
    filters = listOf(
        methodCall(definingClass = "Ljava/util/EnumMap;", name = "get"),
        methodCall(definingClass = "Ljava/util/EnumMap;", name = "remove"),
    ),
)

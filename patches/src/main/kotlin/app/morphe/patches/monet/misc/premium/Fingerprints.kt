/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.monet.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

internal object BillingManagerConstructorFingerprint : Fingerprint(
    name = "<init>",
    strings = listOf("billing_prefs", "license_blob_v1"),
    filters = listOf(
        methodCall(name = "<init>", parameters = listOf("Z", "Z")),
    ),
)

internal fun premiumStateConstructorFingerprint(stateType: String) = Fingerprint(
    definingClass = stateType,
    name = "<init>",
    parameters = listOf("Z", "Z"),
    filters = listOf(
        methodCall(definingClass = stateType, returnType = "Z", parameters = emptyList()),
    ),
)

internal fun isPremiumFingerprint(stateType: String, name: String) = Fingerprint(
    definingClass = stateType,
    name = name,
    returnType = "Z",
    parameters = emptyList(),
)

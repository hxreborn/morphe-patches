/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.wattpad.misc.premium

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

internal object SubscriptionStatusToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    filters = listOf(
        string("SubscriptionStatus(hadPreviousSubscription="),
        string(", tier="),
    ),
)

internal object HeaderSectionToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    filters = listOf(
        string(", subscribePrompt="),
        string("HeaderSectionData(greeting="),
    ),
)

internal fun constructorFingerprint(definingClass: String, vararg parameters: String) = Fingerprint(
    definingClass = definingClass,
    name = "<init>",
    parameters = parameters.toList(),
)

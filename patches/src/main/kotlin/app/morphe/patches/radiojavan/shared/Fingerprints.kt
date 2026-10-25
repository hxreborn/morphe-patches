/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.radiojavan.shared

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.matchSingle

internal object SubscriptionDetailsToStringFingerprint : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    strings = listOf("UserSubscriptionDetails(subscription="),
)

internal fun BytecodePatchContext.subscriptionGetterFingerprint(getter: String) = Fingerprint(
    definingClass = SubscriptionDetailsToStringFingerprint.matchSingle().classDef.type,
    name = getter,
    parameters = emptyList(),
)

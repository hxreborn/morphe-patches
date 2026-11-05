/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.revenuecat

import app.morphe.patcher.Fingerprint

internal object BuildCustomerInfoFingerprint : Fingerprint(
    strings = listOf("non_subscriptions", "original_app_user_id", "first_seen", "request_date"),
    custom = { method, _ -> method.parameterTypes.firstOrNull() == "Lorg/json/JSONObject;" },
)

internal object InvalidateCustomerInfoCacheFingerprint : Fingerprint(
    definingClass = "Lcom/revenuecat/purchases/hybridcommon/CommonKt;",
    name = "invalidateCustomerInfoCache",
    isStatic = true,
    parameters = emptyList(),
    returnType = "V",
)

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.revenuecat

import app.morphe.patcher.Fingerprint

internal object BuildCustomerInfoFingerprint : Fingerprint(
    definingClass = "Lcom/revenuecat/purchases/common/CustomerInfoFactory;",
    name = "buildCustomerInfo",
    returnType = "Lcom/revenuecat/purchases/CustomerInfo;",
    custom = { method, _ -> method.parameterTypes.firstOrNull() == "Lorg/json/JSONObject;" },
)

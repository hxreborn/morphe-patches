/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.allinonecalculator.misc.premium

import app.morphe.patcher.Fingerprint

internal object BuildCustomerInfoFingerprint : Fingerprint(
    definingClass = "Lcom/revenuecat/purchases/common/CustomerInfoFactory;",
    name = "buildCustomerInfo",
    returnType = "Lcom/revenuecat/purchases/CustomerInfo;",
    parameters = listOf(
        "Lorg/json/JSONObject;",
        "Ljava/util/Date;",
        "Lcom/revenuecat/purchases/VerificationResult;",
        "Lcom/revenuecat/purchases/CustomerInfoOriginalSource;",
        "Z",
    ),
)

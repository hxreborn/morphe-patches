/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.groundnews.misc.premium

import app.morphe.patcher.Fingerprint

internal object UserSubscriptionStatusFingerprint : Fingerprint(
    definingClass = "Lcom/ground/multiplatform/ui/core/AuthUser;",
    name = "getSubscriptionStatus",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
)

internal object ServerSubscriptionStatusFingerprint : Fingerprint(
    definingClass = "Lcom/ground/multiplatform/repository/dto/UserDTO;",
    name = "getSubscriptionStatus",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
)

internal object PaidFeatureEnabledFingerprint : Fingerprint(
    definingClass = "Lcom/ground/multiplatform/repository/dao/objects/PaidFeature;",
    name = "getEnabled",
    returnType = "Z",
    parameters = emptyList(),
)

internal object PaidFeatureLimitFingerprint : Fingerprint(
    definingClass = "Lcom/ground/multiplatform/repository/dao/objects/PaidFeature;",
    name = "getLimit",
    returnType = "I",
    parameters = emptyList(),
)

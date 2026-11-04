/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.patreon.misc.featureflags

import app.morphe.patcher.Fingerprint

private const val FEATURE_FLAG_REPOSITORY = "Lcom/patreon/android/featureflag/impl/FeatureFlagRepositoryImpl;"

internal object UserFlagCheckFingerprint : Fingerprint(
    definingClass = FEATURE_FLAG_REPOSITORY,
    name = "isFeatureFlagEnabledForUser",
    returnType = "Z",
)

internal object CampaignFlagCheckFingerprint : Fingerprint(
    definingClass = FEATURE_FLAG_REPOSITORY,
    name = "isFeatureFlagEnabledForCampaign",
    returnType = "Z",
)

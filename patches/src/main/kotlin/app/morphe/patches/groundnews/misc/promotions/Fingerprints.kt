/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.groundnews.misc.promotions

import app.morphe.patcher.Fingerprint

internal object SubscriptionBannerVisibilityFingerprint : Fingerprint(
    definingClass = "Lvc/ucic/state/SubscriptionBannerState;",
    name = "isVisible",
    returnType = "Z",
    parameters = emptyList(),
)

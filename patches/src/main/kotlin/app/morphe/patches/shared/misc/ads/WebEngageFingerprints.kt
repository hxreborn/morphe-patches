/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.ads

import app.morphe.patcher.Fingerprint

internal object WebEngageShouldRenderFingerprint : Fingerprint(
    definingClass = "Lcom/webengage/sdk/android/actions/render/InAppNotificationData;",
    name = "shouldRender",
    returnType = "Z",
    parameters = emptyList(),
)

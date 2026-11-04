/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.patreon.misc.prompts

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

internal object StartupNotificationRequestFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(),
    filters = listOf(string("android.permission.POST_NOTIFICATIONS")),
    custom = { _, classDef -> classDef.sourceFile == "StartupNotificationPermissionRequester.kt" },
)

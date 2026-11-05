/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.misc.settings

import app.morphe.patcher.Fingerprint

internal object ApplicationContextFingerprint : Fingerprint(
    definingClass = "Lcom/viber/voip/ManifestViberApplication;",
    name = "attachBaseContext",
    parameters = listOf("Landroid/content/Context;"),
    returnType = "V",
)

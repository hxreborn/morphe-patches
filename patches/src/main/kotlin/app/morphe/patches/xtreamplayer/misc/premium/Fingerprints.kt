/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.xtreamplayer.misc.premium

import app.morphe.patcher.Fingerprint

internal object VerifyLicenseFingerprint : Fingerprint(
    isStatic = true,
    parameters = listOf("Landroid/content/Context;", "Ljava/util/function/Consumer;"),
    strings = listOf("run_count", "check"),
)

internal object GuardActivityFingerprint : Fingerprint(
    classFingerprint = VerifyLicenseFingerprint,
    parameters = listOf("Landroid/app/Activity;"),
    returnType = "Z",
)

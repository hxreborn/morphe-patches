/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.gsmarena.ads

import app.morphe.patcher.Fingerprint

internal object CanLoadAdsFingerprint : Fingerprint(
    returnType = "Z",
    strings = listOf("Skipping ad load: Mobile Ads SDK not initialized (consent flow not finished)."),
)

internal object SponsorImageFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", "Lcom/google/android/material/navigation/NavigationView;"),
    strings = listOf("Server_SponsorImage", "Server_SponsorURL"),
)

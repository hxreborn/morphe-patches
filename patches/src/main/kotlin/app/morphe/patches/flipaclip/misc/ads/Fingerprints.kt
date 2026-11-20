/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.flipaclip.misc.ads

import app.morphe.patcher.Fingerprint

internal object ShowInterstitialFingerprint : Fingerprint(
    isStatic = false,
    returnType = "V",
    strings = listOf("loadInterstitialAdActivityResult", "ad_box_placement"),
)

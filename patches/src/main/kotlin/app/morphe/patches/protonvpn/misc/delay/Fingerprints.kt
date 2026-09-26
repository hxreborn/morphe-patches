/*
 * Copyright (C) 2026 Hoo-dles
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from hoo-dles/morphe-patches:
 * https://github.com/hoo-dles/morphe-patches/commit/3ad54cf13090739041b9f74c64c95e9994a0d980
 * Commit 3ad54cf13090739041b9f74c64c95e9994a0d980 (2026-07-27),
 * patches/src/main/kotlin/hoodles/morphe/patches/protonvpn/delay/Fingerprints.kt
 */
package app.morphe.patches.protonvpn.misc.delay

import app.morphe.patcher.Fingerprint

private const val APP_CONFIG = "Lcom/protonvpn/android/appconfig/AppConfigResponse;"
private const val LEGACY_APP_CONFIG = "Lcom/protonvpn/android/appconfig/AppConfigResponseLegacyStorage;"

internal val changeServerDelayFingerprints = listOf(APP_CONFIG, LEGACY_APP_CONFIG).flatMap { config ->
    listOf("getChangeServerLongDelayInSeconds", "getChangeServerShortDelayInSeconds").map { getter ->
        Fingerprint(definingClass = config, name = getter, returnType = "I", parameters = emptyList())
    }
}

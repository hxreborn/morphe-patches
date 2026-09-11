/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.quranify.misc.integrity

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall

internal object ApiEndpointsFingerprint : Fingerprint(
    strings = listOf("appBaseUrl", "apiHost", "mediaHost"),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/mchutov/core/security/Obfuscated;",
            name = "decode",
        ),
    ),
)

internal object FetchSessionTokenFingerprint : Fingerprint(
    strings = listOf(
        "check-integrity returned no session_token",
        "Device not recognized by Google Play",
    ),
)

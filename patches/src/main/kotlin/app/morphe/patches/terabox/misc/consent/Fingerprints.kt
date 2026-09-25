/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.consent

import app.morphe.patcher.Fingerprint

internal const val CONSENT_REQUEST_URL = "https://fundingchoicesmessages.google.com/a/consent"

internal object ConsentRequestFingerprint : Fingerprint(
    strings = listOf(CONSENT_REQUEST_URL),
)

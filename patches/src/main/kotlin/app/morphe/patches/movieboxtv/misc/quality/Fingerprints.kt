/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.movieboxtv.misc.quality

import app.morphe.patcher.Fingerprint

internal object CountryHeaderFingerprint : Fingerprint(
    strings = listOf("CountryIsoCodeHeader"),
)

internal object ResponseToStringFingerprint : Fingerprint(
    name = "toString",
    strings = listOf("Response{protocol="),
)

internal object RequestToStringFingerprint : Fingerprint(
    name = "toString",
    strings = listOf("Request{method="),
)

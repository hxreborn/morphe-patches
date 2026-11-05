/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.xtreamplayer.misc.tracking

import app.morphe.patcher.Fingerprint

internal object CommonLoggingFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("Failed to covert logging to UTF-8 byte array"),
)

internal object TranslationLoggingClassFingerprint : Fingerprint(
    isStatic = true,
    strings = listOf("Failed to covert logging to UTF-8 byte array"),
)

internal object TranslationLoggingFingerprint : Fingerprint(
    classFingerprint = TranslationLoggingClassFingerprint,
    returnType = "V",
    parameters = listOf("L"),
)

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.typingbanner

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

internal object TypingPrivacyBannerRepositoryFingerprint : Fingerprint(
    filters = listOf(string("viber_plus_hide_is_typing_ftue_dismissed")),
)

internal object TypingPrivacyBannerEligibilityFingerprint : Fingerprint(
    classFingerprint = TypingPrivacyBannerRepositoryFingerprint,
    returnType = "Z",
    filters = listOf(methodCall(definingClass = "Ljava/lang/Boolean;", name = "booleanValue")),
)

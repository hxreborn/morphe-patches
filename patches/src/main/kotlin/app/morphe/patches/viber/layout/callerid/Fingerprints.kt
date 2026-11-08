/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.callerid

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

internal object CallerIdIntroductionFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(string("tag_introducing_caller_id")),
)

internal object CallerIdCallLogIntroductionFingerprint : Fingerprint(
    classFingerprint = CallerIdIntroductionFingerprint,
    returnType = "V",
    filters = listOf(string("gsm_call_log_ftue_fragment_tag")),
)

internal object CallerIdBannerEligibilityFingerprint : Fingerprint(
    name = "isEnabled",
    returnType = "Z",
    filters = listOf(
        methodCall(definingClass = "Ljava/lang/Number;", name = "intValue"),
        methodCall(definingClass = "Ljava/lang/Number;", name = "longValue"),
    ),
)

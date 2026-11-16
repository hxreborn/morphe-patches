/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.viber.layout.remix

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

internal object ImageRemixManagerFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(
        string("getDisplayFeaturePref()Lcom/viber/voip/core/pref/BooleanAppPreference;"),
        string("getDebugOptions()Lcom/viber/voip/feature/image/remix/presentation/debug/ImageRemixDebugOptions;"),
    ),
)

internal object ImageRemixAvailabilityFingerprint : Fingerprint(
    classFingerprint = ImageRemixManagerFingerprint,
    returnType = "Z",
    parameters = listOf("L", "Z"),
    filters = listOf(methodCall(definingClass = "Ljava/lang/Boolean;", name = "booleanValue")),
)

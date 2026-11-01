/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.neetprepessential.misc.screenshot

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

internal object ScreenProtectorSecureFlagFingerprint : Fingerprint(
    returnType = "V",
    filters = listOf(
        string("preventScreenshotOn"),
        methodCall(definingClass = "Landroid/view/Window;", name = "setFlags"),
    ),
)

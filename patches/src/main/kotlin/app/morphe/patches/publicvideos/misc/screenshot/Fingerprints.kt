/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.publicvideos.misc.screenshot

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

internal object SecureWindowFlagFingerprint : Fingerprint(
    name = "onResume",
    returnType = "V",
    filters = listOf(
        string("ENABLE_SCREENSHOTS"),
        methodCall(definingClass = "Landroid/view/Window;", name = "setFlags"),
    ),
)

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.connected2.misc.screenshot

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

internal fun timedMediaOnCreateFingerprint(timedExtra: String) = Fingerprint(
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
    filters = listOf(
        string(timedExtra),
        methodCall(definingClass = "Landroid/view/Window;", name = "setFlags"),
    ),
)

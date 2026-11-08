/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.presets

import app.morphe.patcher.Fingerprint

internal class ActivityResultFingerprint(owner: String) : Fingerprint(
    definingClass = owner,
    name = "onActivityResult",
    strings = listOf("onActivityResult"),
    parameters = listOf("I", "I", "Landroid/content/Intent;"),
    returnType = "V",
)

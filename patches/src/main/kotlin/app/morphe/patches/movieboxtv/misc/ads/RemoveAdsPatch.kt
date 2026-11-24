/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.movieboxtv.misc.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

private object TeenModeFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    strings = listOf("mmkv_key_teen_mode"),
)

@Suppress("unused")
val removeAdsPatch = patch(
    name = "Remove ads",
    description = "Removes ads.",
) {
    compatibleWith(AppCompatibilities.MOVIEBOX_TV)

    execute {
        TeenModeFingerprint.method.returnEarly(true)
    }
}

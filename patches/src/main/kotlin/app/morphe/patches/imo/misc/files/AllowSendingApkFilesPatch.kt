/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.files

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val allowSendingApkFilesPatch = bytecodePatch(
    name = "Allow sending APK files",
    description = "Adds the Apps option to the chat file menu for sending installed apps as APK files.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        HideSendApkFingerprint.matchSingle().method.returnEarly(false)
    }
}

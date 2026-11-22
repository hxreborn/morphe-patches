/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.update

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val disableUpdateCheckPatch = patch(
    name = "Disable update check",
    description = "Removes the forced update screen and the update prompts.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        UpdateDialogFingerprint.method.returnEarly(false)
        UpdateScreenLauncherFingerprint.method.returnEarly()
        VersionCheckResultFingerprint.method.returnEarly(false)
        InAppUpdateCheckFingerprint.method.returnEarly()
    }
}

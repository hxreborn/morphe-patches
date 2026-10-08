/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.update

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

@Suppress("unused")
val disableUpdateCheckPatch = bytecodePatch(
    name = "Disable update check",
    description = "Removes the forced update screen and the update prompts.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        UpdateDialogFingerprint.matchSingle().method.returnEarly(false)
        UpdateScreenLauncherFingerprint.matchSingle().method.returnEarly()
        VersionCheckResultFingerprint.matchSingle().method.returnEarly(false)
        InAppUpdateCheckFingerprint.matchSingle().method.returnEarly()
    }
}

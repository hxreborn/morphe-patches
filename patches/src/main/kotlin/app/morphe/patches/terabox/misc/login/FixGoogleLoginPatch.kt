/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.terabox.misc.login

import app.morphe.patcher.patch.Patch
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.terabox.misc.settings.markPatchApplied
import app.morphe.patches.terabox.misc.settings.patchesSettingsPatch

@Suppress("unused", "DEPRECATION")
@Deprecated("Use GmsCore support")
val fixGoogleLoginPatch: Patch = patch(
    name = "Fix Google login",
    description = "Restores signing in with a Google account.",
    default = false,
) {
    compatibleWith(AppCompatibilities.TERABOX)
    dependsOn(webLoginPagePatch, patchesSettingsPatch)

    execute {
        markPatchApplied("fixGoogleLogin", fixGoogleLoginPatch)
    }
}

/*
 * Copyright (C) 2026 heval99
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from heval99/Heval-Morphe-Patches:
 * https://github.com/heval99/Heval-Morphe-Patches/commit/28097b43dbc8de99bb516a44f84708ae6cbb0f64
 * Commit 28097b43dbc8de99bb516a44f84708ae6cbb0f64 (2026-10-25),
 * patches/src/main/kotlin/app/template/patches/firefox/misc/aboutconfig/EnableAboutConfigPatch.kt
 */
package app.morphe.patches.firefox.misc.aboutconfig

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.firefox.misc.ui.PATCH_SETTINGS
import app.morphe.patches.firefox.shared.listInSheet
import app.morphe.patches.firefox.shared.whenEnabled
import app.morphe.patches.shared.compat.AppCompatibilities

private const val ABOUT_CONFIG = "$PATCH_SETTINGS->aboutConfig()Z"

@Suppress("unused")
val enableAboutConfigPatch = bytecodePatch(
    name = "Enable about:config",
    description = "Shows the full preference list on about:config.",
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    extendWith("extensions/extension.mpe")

    execute {
        listInSheet("aboutConfig")

        AboutConfigEnabledFingerprint.matchSingle().method.whenEnabled(ABOUT_CONFIG) { "const/4 p1, 0x1" }
    }
}

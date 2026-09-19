/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.theme.webview

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonmail.misc.settings.patchesSettingsPatch
import app.morphe.util.matchSingle

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/protonmail/WebSettingsTheme;"

internal val webSettingsThemePatch = bytecodePatch {
    dependsOn(patchesSettingsPatch)
    extendWith("extensions/extension.mpe")

    execute {
        WebSettingsCreatedFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static { p2 }, $EXTENSION_CLASS->hideBeforeStyling(Landroid/webkit/WebView;)V",
        )

        WebSettingsPageFinishedFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static { p1 }, $EXTENSION_CLASS->injectEnabledStyles(Landroid/webkit/WebView;)V",
        )
    }
}

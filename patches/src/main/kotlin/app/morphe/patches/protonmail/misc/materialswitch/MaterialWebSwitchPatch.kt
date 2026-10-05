/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.materialswitch

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonmail.misc.theme.webview.WebSettingsPageFinishedFingerprint
import app.morphe.util.matchSingle

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/protonmail/WebMaterialSwitch;"

internal val materialWebSwitchPatch = bytecodePatch {
    extendWith("extensions/extension.mpe")

    execute {
        WebSettingsPageFinishedFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static { p1 }, $EXTENSION_CLASS->apply(Landroid/webkit/WebView;)V",
        )
    }
}

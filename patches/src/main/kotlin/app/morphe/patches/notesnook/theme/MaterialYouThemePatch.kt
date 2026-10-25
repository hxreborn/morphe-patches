/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.notesnook.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/notesnook/MaterialYouTheme;"

@Suppress("unused")
val materialYouThemePatch = bytecodePatch(
    name = "Material You theme",
    description = "Applies the system accent colours to the default light and dark themes. " +
        "Colours refresh when the app restarts, and start from the second launch. " +
        "Requires Android 12 or later.",
) {
    compatibleWith(AppCompatibilities.NOTESNOOK)
    extendWith("extensions/extension.mpe")

    execute {
        MmkvInstallFingerprint.matchSingle().apply {
            val index = instructionMatches[0].index
            val register = method.getFreeRegisterProvider(index, 1).getFreeRegister4Bit()

            method.addInstructions(
                index,
                "invoke-virtual { p0 }, Lcom/facebook/react/bridge/ReactContextBaseJavaModule;->" +
                    "getReactApplicationContext()Lcom/facebook/react/bridge/ReactApplicationContext;\n" +
                    "move-result-object v$register\n" +
                    "invoke-static { v$register }, $EXTENSION_CLASS->recolorDefaultThemes(Landroid/content/Context;)V",
            )
        }
    }
}

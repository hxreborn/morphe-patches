/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.firefox.misc.addons

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.firefox.shared.listInSheet
import app.morphe.patches.shared.compat.AppCompatibilities

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/firefox/AddonSite;"
private const val SESSION = "Lorg/mozilla/geckoview/GeckoSession;"
private const val SETTINGS = "Lorg/mozilla/geckoview/GeckoSessionSettings;"
private const val LOAD_REQUEST = "Lorg/mozilla/geckoview/GeckoSession\$NavigationDelegate\$LoadRequest;"

@Suppress("unused")
val unlockAllExtensionsPatch = bytecodePatch(
    name = "Unlock all extensions",
    description = "Lets addons.mozilla.org offer desktop-only extensions for install. Some may not work on Android.",
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    extendWith("extensions/extension.mpe")

    execute {
        listInSheet("unlockAllExtensions")

        NavigationLoadRequestFingerprint.matchSingle().let { match ->
            val registers = match.method.getFreeRegisterProvider(0, 4)
            val uri = registers.getFreeRegister4Bit()
            val settings = registers.getFreeRegister4Bit()
            val target = registers.getFreeRegister4Bit()
            val mode = registers.getFreeRegister4Bit()
            match.method.addInstructions(
                0,
                """
                    iget-object v$uri, p2, $LOAD_REQUEST->uri:Ljava/lang/String;
                    iget v$target, p2, $LOAD_REQUEST->target:I
                    invoke-virtual { p1 }, $SESSION->getSettings()$SETTINGS
                    move-result-object v$settings
                    invoke-virtual { v$settings }, $SETTINGS->getUserAgentMode()I
                    move-result v$mode
                    invoke-static { p1, v$uri, v$target, v$mode }, $EXTENSION_CLASS->userAgentMode(Ljava/lang/Object;Ljava/lang/String;II)I
                    move-result v$mode
                    invoke-virtual { v$settings, v$mode }, $SETTINGS->setUserAgentMode(I)V
                """,
            )
        }
    }
}

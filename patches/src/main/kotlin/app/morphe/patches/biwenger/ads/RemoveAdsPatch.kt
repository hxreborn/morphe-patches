/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.biwenger.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.getFreeRegisterProvider
import app.morphe.patches.biwenger.shared.PageStartedFingerprint
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.biwengerFetchRewriteExtensionPatch
import app.morphe.patches.shared.misc.extension.biwengerRemoveAdsExtensionPatch

private const val AD_CONFIG_INSTALL = "Lapp/hxreborn/extension/biwenger/AdConfig;->install(Landroid/webkit/WebView;)V"

private const val AD_REQUEST_MATCHES = "Lapp/hxreborn/extension/biwenger/AdRequest;->matches(Ljava/lang/String;)Z"

@Suppress("unused")
val removeAdsPatch = patch(
    name = "Remove ads",
    description = "Removes banner, interstitial, rewarded, native and app open ads.",
) {
    dependsOn(biwengerFetchRewriteExtensionPatch, biwengerRemoveAdsExtensionPatch)
    compatibleWith(AppCompatibilities.BIWENGER)

    execute {
        PageStartedFingerprint.method.addInstructions(
            0,
            "invoke-static { p1 }, $AD_CONFIG_INSTALL",
        )

        val method = BridgePostMessageFingerprint.method
        val resultRegister = method.getFreeRegisterProvider(0, 1).getFreeRegister4Bit()

        method.addInstructionsWithLabels(
            0,
            """
                invoke-static { p1 }, $AD_REQUEST_MATCHES
                move-result v$resultRegister
                if-eqz v$resultRegister, :forward
                return-void
                :forward
                nop
            """,
        )
    }
}

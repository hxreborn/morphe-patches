/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.exiftool.misc.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.sharedRevenuecatExtensionPatch
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.revenuecat.grantRevenueCatEntitlements

@Suppress("unused")
val removeAdsPatch = patch(
    name = "Remove ads",
    description = "Removes ads.",
) {
    compatibleWith(AppCompatibilities.EXIFTOOL)
    dependsOn(sharedRevenuecatExtensionPatch, removePairipProtectionPatch)

    execute {
        grantRevenueCatEntitlements("noads" to "noads")

        listOf(
            BannerLoadFingerprint,
            NativePreloadFingerprint,
            AppOpenPreloadFingerprint,
            InterstitialPreloadFingerprint,
        ).forEach { it.match().method.addInstruction(0, "return-void") }

        NativeLoadFingerprint.matchOrNull()?.method?.addInstructions(
            0,
            """
                invoke-virtual { p2 }, Landroid/view/ViewGroup;->removeAllViews()V
                const/16 v0, 0x8
                invoke-virtual { p2, v0 }, Landroid/view/ViewGroup;->setVisibility(I)V
                return-void
            """,
        )

        val interstitialShow = InterstitialShowFingerprint.match().method
        val callbackType = interstitialShow.parameterTypes.single()
        interstitialShow.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                invoke-interface { p1, v0 }, $callbackType->invoke(Ljava/lang/Object;)Ljava/lang/Object;
                return-void
            """,
        )
    }
}

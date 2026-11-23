/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.flipaclip.misc.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.flipaclip.misc.billing.ProductPurchaseFingerprint
import app.morphe.patches.flipaclip.shared.restoreProjectExportPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.pairip.removePairipVirtualizationPatch
import app.morphe.patches.shared.misc.requireArm64

private val FEATURE_PRODUCTS = listOf(
    "com.vblast.flipaclip.iap.feature.build.watermark",
    "com.vblast.flipaclip.iap.feature.onion",
    "com.vblast.flipaclip.iap.feature.grid",
    "com.vblast.flipaclip.iap.feature.customcanvas",
    "com.vblast.flipaclip.iap.feature.build.pngseq",
    "com.vblast.flipaclip.iap.feature.projectbackup",
    "com.vblast.flipaclip.iap.feature.morelayers",
    "com.vblast.flipaclip.iap.feature.importvideo",
    "com.vblast.flipaclip.iap.feature.importaudio",
)

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks paid editing tools and export options. Voice maker and Magic Cut are excluded.",
) {
    compatibleWith(AppCompatibilities.FLIPACLIP)
    dependsOn(removePairipVirtualizationPatch, removePairipProtectionPatch, restoreProjectExportPatch)
    availability(requireArm64)

    execute {
        val featureChecks = FEATURE_PRODUCTS.joinToString("\n") { product ->
            """
                const-string v0, "$product"
                invoke-virtual {v0, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                move-result v0
                if-nez v0, :grant
            """.trimIndent()
        }
        ProductPurchaseFingerprint.method.apply {
            val original = getInstruction(0)
            addInstructionsWithLabels(
                0,
                """
                    const-string v0, "com.vblast.flipaclip.iap.premium"
                    invoke-virtual {v0, p1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                    move-result v0
                    if-nez v0, :grant
                    if-eqz p1, :original
                    $featureChecks
                    goto :original
                    :grant
                    const/4 v0, 0x1
                    return v0
                """,
                ExternalLabel("original", original),
            )
        }
    }
}

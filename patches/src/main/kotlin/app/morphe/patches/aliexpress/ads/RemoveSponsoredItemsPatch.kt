/*
 * Copyright (C) 2026 Quantro100
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from Quantro100/Morphe-patches:
 * https://github.com/Quantro100/Morphe-patches/commit/f384d233384d2bc3d63d9f5011507f774d586b6f
 * Commit f384d233384d2bc3d63d9f5011507f774d586b6f (2026-06-02),
 * patches/src/main/kotlin/app/morphe/patches/aliexpress/ads/HideSponsoredItemsPatch.kt
 */
package app.morphe.patches.aliexpress.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.aliexpress.misc.settings.markPatchApplied
import app.morphe.patches.aliexpress.misc.signin.spoofSignaturePatch
import app.morphe.patches.shared.compat.AppCompatibilities

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/aliexpress/SponsoredProducts;"

private const val DESCRIPTION = "Removes paid placements from search results and product feeds."

@Suppress("unused")
val removeSponsoredItemsPatch = bytecodePatch(
    name = "Remove sponsored items",
    description = DESCRIPTION,
) {
    compatibleWith(AppCompatibilities.ALIEXPRESS)
    dependsOn(spoofSignaturePatch)
    extendWith("extensions/extension.mpe")

    execute {
        markPatchApplied("removeSponsoredItems", DESCRIPTION)
        ProductArrayFingerprint.matchSingle().method.addInstruction(
            0,
            "invoke-static/range { p0 .. p1 }, " +
                "$EXTENSION_CLASS_DESCRIPTOR->filterIfEnabled(Ljava/util/Map;Ljava/lang/String;)V",
        )
    }
}

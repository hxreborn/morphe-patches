/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.fvfileexplorer.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities

private const val VIEW_GONE = 8

@Suppress("unused")
val removeBannerAdsPatch = patch(
    name = "Remove banner ads",
    description = "Removes banner ads from the home screen and archive viewer.",
) {
    compatibleWith(AppCompatibilities.FV_FILE_EXPLORER)

    execute {
        BannerAdLoadFingerprint.method.addInstructions(
            0,
            """
                const/16 p1, $VIEW_GONE
                invoke-virtual/range { p0 .. p1 }, Landroid/view/View;->setVisibility(I)V
                return-void
            """,
        )
    }
}

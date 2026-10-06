/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.allvideoplayer.offline

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle

@Suppress("unused")
val allowOfflineUsePatch = bytecodePatch(
    name = "Allow offline use",
    description = "Opens the app without an internet connection.",
) {
    compatibleWith(AppCompatibilities.ALL_VIDEO_PLAYER)

    execute {
        val startNextActivity = StartNextActivityFingerprint.matchSingle().method
        ShowOfflineDialogFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-virtual { p0 }, $startNextActivity
                return-void
            """,
        )
    }
}

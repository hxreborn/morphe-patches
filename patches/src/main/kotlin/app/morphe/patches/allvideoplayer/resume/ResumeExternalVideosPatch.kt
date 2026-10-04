/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.allvideoplayer.resume

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/allvideoplayer/MediaStoreVideoUri;"

@Suppress("unused")
val resumeExternalVideosPatch = bytecodePatch(
    name = "Resume videos opened from other apps",
    description = "Resumes videos opened from a file manager or gallery where playback stopped.",
) {
    compatibleWith(AppCompatibilities.ALL_VIDEO_PLAYER)
    extendWith("extensions/extension.mpe")

    execute {
        PlayVideoFingerprint.matchSingle().method.addInstructions(
            0,
            "invoke-static { p0, p1 }, $EXTENSION_CLASS->resolve(Landroid/app/Activity;Landroid/net/Uri;)Landroid/net/Uri;\n" +
                "move-result-object p1",
        )
    }
}

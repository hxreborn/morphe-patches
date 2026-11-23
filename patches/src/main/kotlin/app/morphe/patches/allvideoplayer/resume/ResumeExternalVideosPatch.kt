/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.allvideoplayer.resume

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.extension.allvideoplayerResumeVideosOpenedFromOtherAppsExtensionPatch

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/allvideoplayer/MediaStoreVideoUri;"

@Suppress("unused")
val resumeExternalVideosPatch = patch(
    name = "Resume videos opened from other apps",
    description = "Resumes videos opened from a file manager or gallery where playback stopped.",
) {
    dependsOn(allvideoplayerResumeVideosOpenedFromOtherAppsExtensionPatch)
    compatibleWith(AppCompatibilities.ALL_VIDEO_PLAYER)
    execute {
        PlayVideoFingerprint.method.addInstructions(
            0,
            "invoke-static { p0, p1 }, $EXTENSION_CLASS->resolve(Landroid/app/Activity;Landroid/net/Uri;)Landroid/net/Uri;\n" +
                "move-result-object p1",
        )
    }
}

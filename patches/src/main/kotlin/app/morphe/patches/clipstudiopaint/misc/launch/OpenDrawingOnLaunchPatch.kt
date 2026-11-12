/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.clipstudiopaint.misc.launch

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.clipstudiopaint.misc.rewriteNativeLibrary
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64

private const val NOP = 0xd503201f.toInt()

@Suppress("unused")
val openDrawingOnLaunchPatch = rawResourcePatch(
    name = "Open drawing on launch",
    description = "Opens the selected Studio or Simple Mode workspace on launch.",
) {
    compatibleWith(AppCompatibilities.CLIP_STUDIO_PAINT)
    availability(requireArm64)

    execute {
        rewriteNativeLibrary { buffer ->
            buffer.putInt(StartupHomeFingerprint.match(buffer), NOP)
        }
    }
}

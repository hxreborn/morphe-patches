/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.channel

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

private const val CHANNEL_VIDEO_MINUTES = 1_440L

@Suppress("unused")
val raiseChannelVideoLimitPatch = patch(
    name = "Raise channel video limit",
    description = "Raises the length limit on videos posted to a channel to 24 hours.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        ChannelVideoDurationFingerprint.method.returnEarly(CHANNEL_VIDEO_MINUTES)
    }
}

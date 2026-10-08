/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.limits

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

private const val MEDIA_LIMIT = 100

@Suppress("unused")
val raiseMediaSendingLimitPatch = bytecodePatch(
    name = "Raise media sending limit",
    description = "Raises the limit on photos, videos and files sent at once to 100.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        GalleryMediaLimitFingerprint.matchSingle().method.returnEarly(MEDIA_LIMIT)
        FileCountLimitFingerprint.matchSingle().method.returnEarly(MEDIA_LIMIT)
    }
}

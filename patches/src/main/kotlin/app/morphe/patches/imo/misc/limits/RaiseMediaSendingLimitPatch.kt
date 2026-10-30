/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.limits

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

private const val MEDIA_LIMIT = 100
private const val GALLERY_SIZE_LIMIT_BYTES = 3L * 1024 * 1024 * 1024

@Suppress("unused")
val raiseMediaSendingLimitPatch = bytecodePatch(
    name = "Raise media sending limit",
    description = "Raises the limit on photos, videos and files sent at once to 100 " +
        "and the size of each photo and video to 3 GB.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        GalleryMediaLimitFingerprint.matchSingle().method.returnEarly(MEDIA_LIMIT)
        FileCountLimitFingerprint.matchSingle().method.returnEarly(MEDIA_LIMIT)
        GalleryPhotoMaxSizeFingerprint.matchSingle().method.returnEarly(GALLERY_SIZE_LIMIT_BYTES)
        GalleryVideoMaxSizeFingerprint.matchSingle().method.returnEarly(GALLERY_SIZE_LIMIT_BYTES)
    }
}

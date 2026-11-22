/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.story

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

private const val GALLERY_DURATION_SECONDS = 86_400
private const val RECORD_DURATION_MILLIS = 3_600_000

@Suppress("unused")
val removeStoryVideoLimitPatch = patch(
    name = "Remove story video limit",
    description = "Removes the length limit on story videos picked from the gallery or recorded.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        StoryGalleryDurationFingerprint.method.returnEarly(GALLERY_DURATION_SECONDS)
        StoryRecordDurationFingerprint.method.returnEarly(RECORD_DURATION_MILLIS)
    }
}

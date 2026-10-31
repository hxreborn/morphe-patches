/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.limits

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

private const val GIF_LIMIT_BYTES = 100L * 1024 * 1024

@Suppress("unused")
val raiseGifSizeLimitPatch = bytecodePatch(
    name = "Raise GIF size limit",
    description = "Raises the size limit on GIFs picked for custom stickers to 100 MB.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        EmojiUploadGifLimitFingerprint.matchSingle().method.returnEarly(GIF_LIMIT_BYTES)
    }
}

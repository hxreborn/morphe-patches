/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.unread

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val enableJumpToUnreadPatch = bytecodePatch(
    name = "Enable jump to first unread",
    description = "Jumps to the first unread chat when the Chats tab is double-tapped.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        EnableJumpToUnreadFingerprint.matchSingle().method.returnEarly(true)
    }
}

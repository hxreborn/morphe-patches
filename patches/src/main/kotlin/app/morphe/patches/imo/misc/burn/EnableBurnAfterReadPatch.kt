/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.burn

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val enableBurnAfterReadPatch = bytecodePatch(
    name = "Enable burn-after-read messages",
    description = "Adds the burn-after-read option to voice messages in private and group chats.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        EnableBurnAfterReadForSendFingerprint.matchSingle().method.returnEarly(true)
        EnableGroupBurnAfterReadForSendFingerprint.matchSingle().method.returnEarly(true)
    }
}

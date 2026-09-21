/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.realmelink.session

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.matchSingle
import app.morphe.util.returnEarly

@Suppress("unused")
val bypassSessionExpiryPatch = bytecodePatch(
    name = "Bypass session expiry",
    description = "Stops the security prompt that signs the account out.",
) {
    compatibleWith(AppCompatibilities.REALME_LINK)

    execute {
        ReLoginPromptFingerprint.matchSingle().method.returnEarly(null)
    }
}

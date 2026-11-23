/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.botim.layout.ai

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patcher.util.returnEarly

@Suppress("unused")
val removeBotimAiPatch = patch(
    name = "Remove botim AI",
    description = "Removes Chat with botim AI from New chat and New call.",
    default = false,
) {
    compatibleWith(AppCompatibilities.BOTIM)

    execute {
        BotimAiNavigationItemFingerprint.method.returnEarly(null)
    }
}

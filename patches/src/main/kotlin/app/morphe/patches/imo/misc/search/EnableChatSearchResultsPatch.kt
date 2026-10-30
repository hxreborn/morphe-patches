/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.search

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val enableChatSearchResultsPatch = bytecodePatch(
    name = "Enable chat search results",
    description = "Adds a results list to in-chat search.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        ChatRecordSearchFingerprint.matchSingle().method.returnEarly(true)
    }
}

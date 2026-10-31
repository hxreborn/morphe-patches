/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.invisiblefriends

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.imo.shared.enumConstantField
import app.morphe.patches.imo.shared.storedFlagGetterFingerprint
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val showInvisibleFriendsInContactsPatch = bytecodePatch(
    name = "Show invisible friends in contacts",
    description = "Adds the Invisible Friends entry to the contacts list. Requires at least one invisible friend.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        storedFlagGetterFingerprint(enumConstantField("ENABLE_ENTRANCE_IN_CONTACT_LIST"))
            .matchSingle().method.returnEarly(true)
    }
}

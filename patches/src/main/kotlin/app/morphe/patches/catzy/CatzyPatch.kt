/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.catzy

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchAvailability
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.catzy.misc.protection.removeAppProtectionPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.arm64DeltaAvailability

internal fun catzyPatch(
    name: String,
    description: String,
    selection: PatchAvailability = PatchAvailability.ENABLED,
    block: BytecodePatchContext.() -> Unit,
) = bytecodePatch(name, description) {
    compatibleWith(AppCompatibilities.CATZY)

    availability(arm64DeltaAvailability(selection))

    dependsOn(removeAppProtectionPatch)

    execute(block)
}

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.storywise.misc.update

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.dart.DART_AOT_LIBRARY
import app.morphe.patches.shared.misc.dart.dartAotLibrary
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.replaceMasked

private val BUILD_COMPARISON = "4a010054a0835df88000203760974091006443f90300001460974091006843f9e20300aa03000014".hexToByteArray()
private val BUILD_COMPARISON_MASK = "ffffffffff0fe0ffffffffffff03c0ffff03c0ffffffffffff03c0ffff03c0ffffffffffffffffff".hexToByteArray()
private val BRANCH_TO_UP_TO_DATE = "0a000014".hexToByteArray()

@Suppress("unused")
val removeForcedUpdatePatch = resourcePatch(
    name = "Remove forced update",
    description = "Removes the update screen that blocks the app when a newer build is published.",
) {
    compatibleWith(AppCompatibilities.STORYWISE)
    availability(requireArm64)
    dependsOn(removePairipProtectionPatch)

    execute {
        val library = dartAotLibrary()

        if (!library.replaceMasked(BUILD_COMPARISON, BUILD_COMPARISON_MASK, mapOf(0 to BRANCH_TO_UP_TO_DATE))) {
            throw PatchException("Could not find the build comparison in $DART_AOT_LIBRARY")
        }
    }
}

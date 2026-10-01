/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.memoneet.misc.premium

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.requireArm64Delta
import app.morphe.patches.shared.replaceMasked

private const val FLUTTER_LIBRARY = "libapp.so"
private const val ARM64 = "arm64-v8a"

internal val PRODUCT_ACCESS_GATE =
    "fd79bfa9fd030faaefc100d1e00301aaa1031ff8502740f9ff0110eb290e005403b042b8".hexToByteArray()

internal val PRODUCT_ACCESS_GATE_MASK =
    "ffffffffffffffffffffffffffffffffffffffffff03c0ffffffffff1f0000ffffffffff".hexToByteArray()

internal val RETURN_TRUE = "c0820091c0035fd6".hexToByteArray()

@Suppress("unused")
val unlockPremiumPatch = resourcePatch(
    name = "Unlock premium",
    description = "Unlocks the premium question banks, notes and previous-year papers, " +
        "with no energy cost or ads. Features that need a signed-in account are not included.",
) {
    compatibleWith(AppCompatibilities.MEMONEET)
    availability(requireArm64Delta)

    execute {
        val library = get("lib/$ARM64/$FLUTTER_LIBRARY")
        if (!library.exists()) throw PatchException("Could not find $FLUTTER_LIBRARY for $ARM64")

        if (!library.replaceMasked(PRODUCT_ACCESS_GATE, PRODUCT_ACCESS_GATE_MASK, mapOf(0 to RETURN_TRUE))) {
            throw PatchException("Could not find the product access check in $FLUTTER_LIBRARY")
        }
    }
}

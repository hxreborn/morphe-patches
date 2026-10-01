/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.memoneet.misc.premium

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.replaceMasked

private const val DART_AOT_LIBRARY = "libapp.so"
private const val ARM64 = "arm64-v8a"

internal val PRODUCT_ACCESS_CHECK_PROLOGUE =
    "fd79bfa9fd030faaefc100d1e00301aaa1031ff8502740f9ff0110eb290e005403b042b8".hexToByteArray()

internal val PRODUCT_ACCESS_CHECK_PROLOGUE_MASK =
    "ffffffffffffffffffffffffffffffffffffffffff03c0ffffffffff1f0000ffffffffff".hexToByteArray()

internal val COMBO_PURCHASE_CHECK_PROLOGUE =
    ("fd79bfa9fd030faaef2101d1a2031ff8502740f9ff0110eba90e0054" +
        "40f043b800801c8b7093409110fa45f91f00106b").hexToByteArray()

internal val COMBO_PURCHASE_CHECK_PROLOGUE_MASK =
    ("ffffffffffffffffffffffffffffffffffffffffffffffff1f0000ff" +
        "ffffffffffffffffff03c0ffff03c0ffffffffff").hexToByteArray()

internal val RETURN_TRUE = "c0820091c0035fd6".hexToByteArray()

@Suppress("unused")
val unlockPremiumPatch = resourcePatch(
    name = "Unlock premium",
    description = "Unlocks the premium question banks, notes, test series, previous-year papers " +
        "and shop plans, with no energy cost or ads. Signing in requires GmsCore support.",
) {
    compatibleWith(AppCompatibilities.MEMONEET)
    availability(requireArm64)

    execute {
        val library = get("lib/$ARM64/$DART_AOT_LIBRARY")
        if (!library.exists()) throw PatchException("Could not find $DART_AOT_LIBRARY for $ARM64")

        mapOf(
            "product access check" to (PRODUCT_ACCESS_CHECK_PROLOGUE to PRODUCT_ACCESS_CHECK_PROLOGUE_MASK),
            "combo purchase check" to (COMBO_PURCHASE_CHECK_PROLOGUE to COMBO_PURCHASE_CHECK_PROLOGUE_MASK),
        ).forEach { (check, prologue) ->
            val (pattern, mask) = prologue
            if (!library.replaceMasked(pattern, mask, mapOf(0 to RETURN_TRUE))) {
                throw PatchException("Could not find the $check in $DART_AOT_LIBRARY")
            }
        }
    }
}

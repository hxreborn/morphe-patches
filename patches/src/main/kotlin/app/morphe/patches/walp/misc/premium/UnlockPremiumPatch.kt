/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.walp.misc.premium

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.replaceMasked
import app.morphe.patches.shared.replaceMaskedEvery

private const val DART_AOT_LIBRARY = "lib/arm64-v8a/libapp.so"

private const val ADS_REMOVED_READ_SITES = 5

internal val ADS_REMOVED_PREFERENCE_READ = "62bb4bf900000094".hexToByteArray()

internal val ADS_REMOVED_PREFERENCE_READ_MASK = "ffffffff000000fc".hexToByteArray()

internal val PURCHASE_STATE_COPY_WITH_TAIL =
    ("da250b94a1035df8017000b8a1835df801b000b8a1035ef801f000b8a1835ef8013001b8" +
        "a1035ff8017001b8a1835ff801b001b8ef031daafd79c1a8").hexToByteArray()

internal val PURCHASE_STATE_COPY_WITH_TAIL_MASK =
    ("000000fc" + "ff".repeat(PURCHASE_STATE_COPY_WITH_TAIL.size - 4)).hexToByteArray()

private val LOAD_TRUE_INTO_X0 = "c0820091".hexToByteArray()

private val LOAD_TRUE_INTO_X1 = "c1820091".hexToByteArray()

@Suppress("unused")
val unlockPremiumPatch = resourcePatch(
    name = "Unlock premium",
    description = "Unlocks Pro filters and wallpaper rotation from categories, and removes ads.",
) {
    compatibleWith(AppCompatibilities.WALP)
    availability(requireArm64)
    dependsOn(removePairipProtectionPatch)

    execute {
        val library = get(DART_AOT_LIBRARY)
        if (!library.exists()) throw PatchException("Could not find $DART_AOT_LIBRARY")

        val reads = library.replaceMaskedEvery(
            ADS_REMOVED_PREFERENCE_READ,
            ADS_REMOVED_PREFERENCE_READ_MASK,
            4,
            LOAD_TRUE_INTO_X0,
        )
        if (reads != ADS_REMOVED_READ_SITES) {
            throw PatchException("Expected $ADS_REMOVED_READ_SITES Pro preference reads, found $reads")
        }

        val copyPatched = library.replaceMasked(
            PURCHASE_STATE_COPY_WITH_TAIL,
            PURCHASE_STATE_COPY_WITH_TAIL_MASK,
            mapOf(4 to LOAD_TRUE_INTO_X1),
        )
        if (!copyPatched) throw PatchException("Could not find the purchase state copy in $DART_AOT_LIBRARY")
    }
}

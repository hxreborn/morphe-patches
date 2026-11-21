/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.magicearth.misc.premium

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.dart.DART_AOT_LIBRARY
import app.morphe.patches.shared.misc.dart.dartAotLibrary
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.replaceMasked

internal val STORE_SUBSCRIPTION_COPY_WITH =
    ("fd79bfa9fd030faaefe100d1bf00166ba1000054207040b800801c8be40300aa" +
        "02000014e40305aaa00b40f9a4831cf8").hexToByteArray()

internal val STORE_SUBSCRIPTION_COPY_WITH_MASK =
    ("ffffffffffffffffffffffffffffffff1f0000ffffffffffffffffffffffffff" +
        "000000fcffffffffffffffffffffffff").hexToByteArray()

internal val LOAD_TRUE = "c4820091".hexToByteArray()

internal val BRANCH_PAST_DEFAULT = "06000014".hexToByteArray()

private const val IS_ACTIVE_NULL_CHECK_OFFSET = 12

private const val IS_ACTIVE_BRANCH_OFFSET = 16

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks offline maps, real-time traffic and weather, elevation maps, activity recording, " +
        "vehicle profiles, personalized voices and Android Auto.",
) {
    compatibleWith(AppCompatibilities.MAGIC_EARTH)
    availability(requireArm64)

    execute {
        val library = dartAotLibrary()

        if (!library.replaceMasked(
                STORE_SUBSCRIPTION_COPY_WITH,
                STORE_SUBSCRIPTION_COPY_WITH_MASK,
                mapOf(
                    IS_ACTIVE_NULL_CHECK_OFFSET to LOAD_TRUE,
                    IS_ACTIVE_BRANCH_OFFSET to BRANCH_PAST_DEFAULT,
                ),
            )
        ) {
            throw PatchException("Could not find the store subscription state in $DART_AOT_LIBRARY")
        }
    }
}

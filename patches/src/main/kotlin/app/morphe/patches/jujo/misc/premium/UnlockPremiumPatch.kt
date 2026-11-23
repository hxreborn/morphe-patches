/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.jujo.misc.premium

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.dart.DART_AOT_LIBRARY
import app.morphe.patches.shared.misc.dart.dartAotLibrary
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.replaceMasked

internal val SUBSCRIPTION_FACTORY =
    ("fd79bfa9fd030faaef6100d1a3031ff8500340f9ff0110eb090000545f00166b" +
        "01000054e00303aa").hexToByteArray()

internal val SUBSCRIPTION_FACTORY_MASK =
    ("ffffffffffffffffffffffffffffffffff03c0ffffffffff1f0000ffffffffff" +
        "1f0000ffffffffff").hexToByteArray()

internal val SUBSCRIPTION_LOOKUP =
    ("00000094e10300aaa1031ef800000094a1035ff8a0031ef800000094a2035ef8" +
        "e30300aae10316aa00000094a0031df801f05ff8217c4cd3d10184d23f0011eb" +
        "01000054a4035ff8").hexToByteArray()

internal val SUBSCRIPTION_LOOKUP_MASK =
    ("000000fcffffffffffffffff000000fcffffffffffffffff000000fcffffffff" +
        "ffffffffffffffff000000fcffffffffffffffffffffffff1f00e0ffffffffff" +
        "1f0000ffffffffff").hexToByteArray()

internal val BRANCH_TO_ACTIVE_SUBSCRIPTION = "24000014".hexToByteArray()

internal val LOAD_NULL_SUBSCRIPTION_DOCUMENT = "e00316aa".hexToByteArray()

internal val BRANCH_TO_RETURN_FACTORY_RESULT = "52000014".hexToByteArray()

private const val FACTORY_NULL_CHECK_OFFSET = 28

private const val LOOKUP_DOCUMENT_FETCH_OFFSET = 0

private const val LOOKUP_CLASS_CHECK_OFFSET = 64

@Suppress("unused")
val unlockPremiumPatch = patch(
    name = "Unlock premium",
    description = "Unlocks unlimited journals and widgets, premium templates, brushes, papers, stamp options, " +
        "HD video export without a watermark, more pictures per page, longer PDF imports " +
        "and sync for every journal.",
) {
    compatibleWith(AppCompatibilities.JUJO)
    dependsOn(removePairipProtectionPatch)
    availability(requireArm64)

    execute {
        val library = dartAotLibrary()

        if (!library.replaceMasked(
                SUBSCRIPTION_FACTORY,
                SUBSCRIPTION_FACTORY_MASK,
                mapOf(FACTORY_NULL_CHECK_OFFSET to BRANCH_TO_ACTIVE_SUBSCRIPTION),
            )
        ) {
            throw PatchException("Could not find the subscription factory in $DART_AOT_LIBRARY")
        }

        if (!library.replaceMasked(
                SUBSCRIPTION_LOOKUP,
                SUBSCRIPTION_LOOKUP_MASK,
                mapOf(
                    LOOKUP_DOCUMENT_FETCH_OFFSET to LOAD_NULL_SUBSCRIPTION_DOCUMENT,
                    LOOKUP_CLASS_CHECK_OFFSET to BRANCH_TO_RETURN_FACTORY_RESULT,
                ),
            )
        ) {
            throw PatchException("Could not find the subscription lookup in $DART_AOT_LIBRARY")
        }
    }
}

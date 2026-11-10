/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.freedom.misc.upsell

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.dart.DART_AOT_LIBRARY
import app.morphe.patches.shared.misc.dart.dartAotLibrary
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.replaceMasked

internal val ONBOARDING_PAYWALL_CALL =
    ("403f40f9002c4af9504b40f91f00106b810000546237409142cc40f9a8f5199401f040b8" +
        "21801c8b3f00166b20160054207044b800801c8b504b40f91f00106ba015005401f05ff8" +
        "217c4cd3f00300aae00301aae10310aa62834091424841f9c3c200911e0440d1be7a7ef8" +
        "c0033fd6e10300aaa1831df803f31994").hexToByteArray()

internal val ONBOARDING_PAYWALL_CALL_MASK =
    ("ffffffffff03c0ffffffffffffffffff1f0000ffff03c0ffff03c0ff000000fcffffffff" +
        "ffffffffffffffff1f0000ffff0fe0ffffffffffffffffffffffffff1f0000ffffffffff" +
        "ffffffffffffffffffffffffffffffffff03c0ffff03c0ffffffffffffffffffffffffff" +
        "ffffffffffffffffffffffff000000fc").hexToByteArray()

internal val OUTRO_STEP_LIST =
    ("e10316aac20080d2325f1794e20300aaa0035ff8a2831ff840f000b8a0035ef8403001b8" +
        "a0035df8407001b86183409121b843f95e5a1794e10300aaa0835ff8a1031ff820f000b8" +
        "c00080d220b000b80b000094a1035ff8017000b8").hexToByteArray()

internal val OUTRO_STEP_LIST_MASK =
    ("ffffffffffffffff000000fcffffffffffffffffffffffffffffffffffffffffffffffff" +
        "ffffffffffffffffff03c0ffff03c0ff000000fcffffffffffffffffffffffffffffffff" +
        "ffffffffffffffff000000fcffffffffffffffff").hexToByteArray()

internal val TODAY_FREE_USER_CHECK =
    ("a2835ff841f040b821801c8b233041b863801c8be10303aa1d01009420012037a0835ff8" +
        "01f040b821801c8b023041b8").hexToByteArray()

internal val TODAY_FREE_USER_CHECK_MASK =
    ("ffffffffffffffffffffffffffffffffffffffffffffffff000000fcffffffffffffffff" +
        "ffffffffffffffffffffffff").hexToByteArray()

internal val SETTINGS_FREE_USER_CHECK =
    ("40000054f08a0c94813041b821801c8b2a050094c0042037a0835df8a1835ff8a2035ff8" +
        "68040094e20300aaa0835df8").hexToByteArray()

internal val SETTINGS_FREE_USER_CHECK_MASK =
    ("1f0000ff000000fcffffffffffffffff000000fcffffffffffffffffffffffffffffffff" +
        "000000fcffffffffffffffff").hexToByteArray()

internal val LOAD_FALSE = "c0c20091".hexToByteArray()

internal const val TODAY_FREE_USER_CALL_OFFSET = 24

internal const val SETTINGS_FREE_USER_CALL_OFFSET = 16

internal val BRANCH_PAST_ONBOARDING_PAYWALL = "1f000014".hexToByteArray()

internal val OUTRO_LENGTH_ONE_STEP = "400080d2".hexToByteArray()

internal const val OUTRO_LENGTH_OFFSET = 72

@Suppress("unused")
val removeUpsellsPatch = resourcePatch(
    name = "Remove upsells",
    description = "Removes the sign-up trial offers and the Unlock Premium prompts on Today and in Settings.",
) {
    compatibleWith(AppCompatibilities.FREEDOM)
    availability(requireArm64)

    execute {
        val library = dartAotLibrary()

        val callPatched = library.replaceMasked(
            ONBOARDING_PAYWALL_CALL,
            ONBOARDING_PAYWALL_CALL_MASK,
            mapOf(0 to BRANCH_PAST_ONBOARDING_PAYWALL),
        )
        if (!callPatched) throw PatchException("Could not find the onboarding paywall call in $DART_AOT_LIBRARY")

        val outroPatched = library.replaceMasked(
            OUTRO_STEP_LIST,
            OUTRO_STEP_LIST_MASK,
            mapOf(OUTRO_LENGTH_OFFSET to OUTRO_LENGTH_ONE_STEP),
        )
        if (!outroPatched) throw PatchException("Could not find the onboarding outro steps in $DART_AOT_LIBRARY")

        val todayPatched = library.replaceMasked(
            TODAY_FREE_USER_CHECK,
            TODAY_FREE_USER_CHECK_MASK,
            mapOf(TODAY_FREE_USER_CALL_OFFSET to LOAD_FALSE),
        )
        if (!todayPatched) throw PatchException("Could not find the Today free user check in $DART_AOT_LIBRARY")

        val settingsPatched = library.replaceMasked(
            SETTINGS_FREE_USER_CHECK,
            SETTINGS_FREE_USER_CHECK_MASK,
            mapOf(SETTINGS_FREE_USER_CALL_OFFSET to LOAD_FALSE),
        )
        if (!settingsPatched) throw PatchException("Could not find the Settings free user check in $DART_AOT_LIBRARY")
    }
}

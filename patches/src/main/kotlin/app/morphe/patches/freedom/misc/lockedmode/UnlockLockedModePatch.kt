/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.freedom.misc.lockedmode

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.dart.DART_AOT_LIBRARY
import app.morphe.patches.shared.misc.dart.dartAotLibrary
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.replaceMasked
import java.io.File

internal val LOCKED_MODE_PREMIUM_CHECK =
    ("227042b842801c8b413041b821801c8b227042b842801c8b413041b821801c8b70634091" +
        "109245f93f00106bd0820091d1c200910002919ac0035fd6").hexToByteArray()

internal val LOCKED_MODE_PREMIUM_CHECK_MASK =
    ("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff03c0ff" +
        "ff03c0ffffffffffffffffffffffffffffffffffffffffff").hexToByteArray()

internal val LOCKED_MODE_TAP_CHECK =
    ("413041b821801c8b227042b842801c8b413041b821801c8b70634091109245f93f00106b" +
        "6002005401b040b821801c8b").hexToByteArray()

internal val LOCKED_MODE_TAP_CHECK_MASK =
    ("ffffffffffffffffffffffffffffffffffffffffffffffffff03c0ffff03c0ffffffffff" +
        "1f0000ffffffffffffffffff").hexToByteArray()

internal val WEEKLY_EARLY_EXIT_TAP_CHECK =
    ("643041b884801c8b837042b863801c8b643041b884801c8b70634091109245f99f00106b" +
        "40020054").hexToByteArray()

internal val WEEKLY_EARLY_EXIT_TAP_CHECK_MASK =
    ("ffffffffffffffffffffffffffffffffffffffffffffffffff03c0ffff03c0ffffffffff" +
        "1f0000ff").hexToByteArray()

internal val ENDING_LOCKED_MODE_CHECK =
    ("227042b842801c8b413041b821801c8b227042b842801c8b413041b821801c8b70634091" +
        "109245f93f00106b21010054417042b821801c8b3f00166bd0820091d1c200910212919a" +
        "e10302aa02000014c1c20091").hexToByteArray()

internal val ENDING_LOCKED_MODE_CHECK_MASK =
    ("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff03c0ff" +
        "ff03c0ffffffffff1f0000ffffffffffffffffffffffffffffffffffffffffffffffffff" +
        "ffffffffffffffffffffffff").hexToByteArray()

internal val WEEKLY_EARLY_EXIT_ENABLED_CHECK =
    ("207042b800801c8b023041b842801c8b407042b800801c8b023041b842801c8b70634091" +
        "109245f95f00106b61000054b2ffff9702000014c0c20091ef031daafd79c1a8c0035fd6").hexToByteArray()

internal val WEEKLY_EARLY_EXIT_ENABLED_CHECK_MASK =
    ("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff03c0ff" +
        "ff03c0ffffffffff1f0000ff000000fcffffffffffffffffffffffffffffffffffffffff").hexToByteArray()

internal val LOCKED_MODE_RESET =
    "a0835ef8013041b821801c8b70634091109245f93f00106be0000054a1035ff8c2c20091".hexToByteArray()

internal val LOCKED_MODE_RESET_MASK =
    "ffffffffffffffffffffffffff03c0ffff03c0ffffffffffffffffffffffffffffffffff".hexToByteArray()

internal val RETURN_TRUE = "c0820091c0035fd6".hexToByteArray()

internal val COMPARE_W1_WITH_ITSELF = "3f00016b".hexToByteArray()

internal val COMPARE_W2_WITH_ITSELF = "5f00026b".hexToByteArray()

internal val COMPARE_W4_WITH_ITSELF = "9f00046b".hexToByteArray()

internal val SKIP_LOCKED_MODE_RESET = "07000014".hexToByteArray()

internal const val TAP_COMPARE_OFFSET = 32

private const val GETTER_COMPARE_OFFSET = 40

private const val LOCKED_MODE_RESET_BRANCH_OFFSET = 24

private fun File.replaceMaskedOrThrow(
    pattern: ByteArray,
    mask: ByteArray,
    offset: Int,
    replacement: ByteArray,
    site: String,
) {
    if (!replaceMasked(pattern, mask, mapOf(offset to replacement))) {
        throw PatchException("Could not find the $site in $DART_AOT_LIBRARY")
    }
}

@Suppress("unused")
val unlockLockedModePatch = resourcePatch(
    name = "Unlock Locked Mode",
    description = "Unlocks Locked Mode on free accounts.",
) {
    compatibleWith(AppCompatibilities.FREEDOM)
    availability(requireArm64)

    execute {
        val library = dartAotLibrary()

        library.replaceMaskedOrThrow(
            LOCKED_MODE_PREMIUM_CHECK,
            LOCKED_MODE_PREMIUM_CHECK_MASK,
            0,
            RETURN_TRUE,
            "Locked Mode premium check",
        )
        library.replaceMaskedOrThrow(
            LOCKED_MODE_TAP_CHECK,
            LOCKED_MODE_TAP_CHECK_MASK,
            TAP_COMPARE_OFFSET,
            COMPARE_W1_WITH_ITSELF,
            "Locked Mode tap check",
        )
        library.replaceMaskedOrThrow(
            WEEKLY_EARLY_EXIT_TAP_CHECK,
            WEEKLY_EARLY_EXIT_TAP_CHECK_MASK,
            TAP_COMPARE_OFFSET,
            COMPARE_W4_WITH_ITSELF,
            "weekly early exit tap check",
        )
        library.replaceMaskedOrThrow(
            ENDING_LOCKED_MODE_CHECK,
            ENDING_LOCKED_MODE_CHECK_MASK,
            GETTER_COMPARE_OFFSET,
            COMPARE_W1_WITH_ITSELF,
            "Locked Mode ending check",
        )
        library.replaceMaskedOrThrow(
            WEEKLY_EARLY_EXIT_ENABLED_CHECK,
            WEEKLY_EARLY_EXIT_ENABLED_CHECK_MASK,
            GETTER_COMPARE_OFFSET,
            COMPARE_W2_WITH_ITSELF,
            "weekly early exit enabled check",
        )
        library.replaceMaskedOrThrow(
            LOCKED_MODE_RESET,
            LOCKED_MODE_RESET_MASK,
            LOCKED_MODE_RESET_BRANCH_OFFSET,
            SKIP_LOCKED_MODE_RESET,
            "Locked Mode reset",
        )
    }
}

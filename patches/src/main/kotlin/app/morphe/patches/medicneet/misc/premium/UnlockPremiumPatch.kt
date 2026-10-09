/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.medicneet.misc.premium

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.shared.replaceMasked

private const val DART_AOT_LIBRARY = "libapp.so"
private const val ARM64 = "arm64-v8a"

internal class DartCheck(
    val name: String,
    val pattern: ByteArray,
    val mask: ByteArray,
    val replacementOffset: Int,
    val replacement: ByteArray,
)

internal val RETURN_TRUE = "c0820091c0035fd6".hexToByteArray()

internal val LOAD_FALSE = "c0c20091".hexToByteArray()

internal val FULL_ACCESS_CHECK = DartCheck(
    name = "full access check",
    pattern = (
        "fd79bfa9fd030faaef4100d1500340f9ff0110eb09000054640340f900000094" +
            "e20300aae10316aa00000094e10300aa000000941f00166b01000054e20316aa"
        ).hexToByteArray(),
    mask = (
        "ffffffffffffffffffffffffff03c0ffffffffff1f0000ffff03c0ff000000fc" +
            "ffffffffffffffff000000fcffffffff000000fcffffffff1f0000ffffffffff"
        ).hexToByteArray(),
    replacementOffset = 0,
    replacement = RETURN_TRUE,
)

internal val PREDICTED_BATCH_TEST_PANEL_CALL = DartCheck(
    name = "predicted batch test panel check",
    pattern = "a1031ff8000000940000203700000094".hexToByteArray(),
    mask = "ffffffff000000fc1f00f8ff000000fc".hexToByteArray(),
    replacementOffset = 4,
    replacement = LOAD_FALSE,
)

internal val PYQ_GENERATOR_TESTER_CALL = DartCheck(
    name = "PYQ generator tester check",
    pattern = "a0031ff800000094a0031df800002037".hexToByteArray(),
    mask = "ffffffff000000fcffffffff1f00f8ff".hexToByteArray(),
    replacementOffset = 4,
    replacement = LOAD_FALSE,
)

internal val DART_CHECKS = listOf(FULL_ACCESS_CHECK, PREDICTED_BATCH_TEST_PANEL_CALL, PYQ_GENERATOR_TESTER_CALL)

@Suppress("unused")
val unlockPremiumPatch = resourcePatch(
    name = "Unlock premium",
    description = "Unlocks the AIMTS test series, the PYQ generator, video lectures and tier 1 and 2 " +
        "questions in every topic. Tier 3 questions and flashcards are not included.",
) {
    compatibleWith(AppCompatibilities.MEDICNEET)
    availability(requireArm64)

    dependsOn(removePairipProtectionPatch, openQuestionTiersPatch)

    execute {
        val library = get("lib/$ARM64/$DART_AOT_LIBRARY")
        if (!library.exists()) throw PatchException("Could not find $DART_AOT_LIBRARY for $ARM64")

        DART_CHECKS.forEach { check ->
            if (!library.replaceMasked(check.pattern, check.mask, mapOf(check.replacementOffset to check.replacement))) {
                throw PatchException("Could not find the ${check.name} in $DART_AOT_LIBRARY")
            }
        }
    }
}

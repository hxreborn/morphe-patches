/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.narababy.misc.rate

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.hermes.hermesVariantsPatch

private const val PREDICATE_283 =
    "6C 05 01 6C 03 02 77 01 B9 A9 00 00 00 05 01 37 " +
        "02 05 01 F3 6D 37 04 05 02 6B 55 37 09 05 03 64"
private const val PREDICATE_283_RETURNS_FALSE =
    "79 00 5C 00 79 00 77 01 B9 A9 00 00 00 05 01 37 " +
        "02 05 01 F3 6D 37 04 05 02 6B 55 37 09 05 03 64"

private const val VIEW_283 =
    "7C 15 36 02 15 01 C5 36 09 02 02 E0 29 10 01 2E " +
        "00 10 02 36 01 00 03 28 2E 00 10 0C 76 04 54 08"
private const val VIEW_283_RETURNS_NULL =
    "77 00 5C 00 6E 00 00 36 09 02 02 E0 29 10 01 2E " +
        "00 10 02 36 01 00 03 28 2E 00 10 0C 76 04 54 08"

private const val PREDICATE_290 =
    "6C 05 01 6C 03 02 77 01 B9 A9 00 00 00 05 01 37 " +
        "02 05 01 77 6E 37 04 05 02 CF 55 37 09 05 03 BC"
private const val PREDICATE_290_RETURNS_FALSE =
    "79 00 5C 00 79 00 77 01 B9 A9 00 00 00 05 01 37 " +
        "02 05 01 77 6E 37 04 05 02 CF 55 37 09 05 03 BC"

private const val VIEW_290 =
    "7C 15 36 02 15 01 C5 36 09 02 02 E0 29 10 01 2E " +
        "00 10 02 36 01 00 03 6A 2E 00 10 0C 76 04 54 08"
private const val VIEW_290_RETURNS_NULL =
    "77 00 5C 00 6E 00 00 36 09 02 02 E0 29 10 01 2E " +
        "00 10 02 36 01 00 03 6A 2E 00 10 0C 76 04 54 08"

@Suppress("unused")
val removeRatingPromptsPatch = patch(
    name = "Remove rating prompts",
    description = "Removes the prompts asking for a rating.",
) {
    compatibleWith(AppCompatibilities.NARA_BABY)

    dependsOn(
        hermesVariantsPatch(
            setOf(PREDICATE_283 to PREDICATE_283_RETURNS_FALSE, VIEW_283 to VIEW_283_RETURNS_NULL),
            setOf(PREDICATE_290 to PREDICATE_290_RETURNS_FALSE, VIEW_290 to VIEW_290_RETURNS_NULL),
        ),
    )
}

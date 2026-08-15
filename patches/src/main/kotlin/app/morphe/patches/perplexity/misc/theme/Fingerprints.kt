/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.perplexity.misc.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal

internal val DARK_BACKGROUND_COLORS = listOf(
    0xFF171615L, // pages and bottom sheets
    0xFF1E1D1CL, // composer and cards
    0xB3171615L,
    0xFF121211L,
)

internal object DarkColorSchemeFingerprint : Fingerprint(
    name = "<init>",
    filters = DARK_BACKGROUND_COLORS.map { literal(it) },
)

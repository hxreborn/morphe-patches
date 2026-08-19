/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.perplexity.misc.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal

private const val SURFACE_COLOR = 0xFF1E1D1CL

private val DARK_COLORS = listOf(
    0xFF171615L,
    SURFACE_COLOR,
    0xB3171615L,
    0xFF121211L,
)

internal val DARK_BACKGROUND_COLORS = DARK_COLORS - SURFACE_COLOR

internal object DarkColorSchemeFingerprint : Fingerprint(
    name = "<init>",
    filters = DARK_COLORS.map { literal(it) },
)

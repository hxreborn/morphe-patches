/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.perplexity.misc.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal

private const val OPAQUE_ALPHA = 0xFF000000L
private const val SCRIM_ALPHA = 0xB3000000L

internal class DarkPalette(
    val background: Long,
    raised: Long,
    sunken: Long,
    val tint: Long,
    tintAlphas: List<Long>,
) {
    val backgrounds = listOf(OPAQUE_ALPHA or background, SCRIM_ALPHA or background, OPAQUE_ALPHA or sunken)
    val tints = tintAlphas.map { (it shl 24) or tint }
    val fingerprint = Fingerprint(
        name = "<init>",
        filters = listOf(
            OPAQUE_ALPHA or background,
            OPAQUE_ALPHA or raised,
            SCRIM_ALPHA or background,
            OPAQUE_ALPHA or sunken,
        ).map { literal(it) },
    )
}

internal val darkPalettes = listOf(
    DarkPalette(
        background = 0x171615L,
        raised = 0x1E1D1CL,
        sunken = 0x121211L,
        tint = 0xD6D5D4L,
        tintAlphas = listOf(0x09L, 0x12L, 0x1BL, 0x24L, 0x33L),
    ),
    DarkPalette(
        background = 0x161616L,
        raised = 0x1D1D1DL,
        sunken = 0x121212L,
        tint = 0xDEDEDEL,
        tintAlphas = listOf(0x08L, 0x09L, 0x0FL, 0x12L, 0x1BL, 0x24L, 0x33L),
    ),
    DarkPalette(
        background = 0x15171BL,
        raised = 0x1B1D21L,
        sunken = 0x101216L,
        tint = 0xDBDEE3L,
        tintAlphas = listOf(0x08L, 0x09L, 0x0FL, 0x12L, 0x1BL, 0x24L, 0x33L),
    ),
)

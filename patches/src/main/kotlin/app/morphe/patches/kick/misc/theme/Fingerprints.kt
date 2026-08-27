/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.kick.misc.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal

internal const val TV_DARK_BACKGROUND = 0xFF070809L

internal object TvPaletteFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(literal(TV_DARK_BACKGROUND)),
)

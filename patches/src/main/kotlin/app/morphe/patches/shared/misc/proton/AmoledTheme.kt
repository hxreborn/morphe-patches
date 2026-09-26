/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.proton

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.literal
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.util.matchSingle

internal object BalticSeaPaletteFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(literal(ProtonPalette.BALTIC_SEA)),
)

internal fun BytecodePatchContext.transformCoreDarkBackground() {
    val palette = BalticSeaPaletteFingerprint.matchSingle()
    palette.method.injectColorTransformCall(
        palette.instructionMatches.first().index,
        "$AMOLED_THEME_CLASS->transformBackground(J)J",
    )
}

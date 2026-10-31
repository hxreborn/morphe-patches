/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.translation

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val enableMessageTranslationPatch = bytecodePatch(
    name = "Enable message translation",
    description = "Enables translating received messages.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        TranslationEnabledFingerprint.matchSingle().method.returnEarly(true)
    }
}

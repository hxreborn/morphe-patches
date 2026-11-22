/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.imo.misc.translation

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val enableMessageTranslationPatch = patch(
    name = "Enable message translation",
    description = "Enables translating received messages.",
) {
    compatibleWith(AppCompatibilities.IMO)

    execute {
        TranslationEnabledFingerprint.method.returnEarly(true)
    }
}

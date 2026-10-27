/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.raindrop.layout.settings

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.hermes.hermesVariantsPatch

private const val LINKS_FORM =
    "6F 11 13 01 12 11 5A 10 11 03 52 02 10 00 6F 02 0F 01 08 02 8B 00 11"
private const val LINKS_FORM_NULL =
    "6F 11 13 01 12 11 5A 10 11 03 52 02 10 00 94 02 94 02 94 02 8B 00 11"

@Suppress("unused")
val hideSettingsLinksPatch = rawResourcePatch(
    name = "Hide Settings links",
    description = "Hides the Web and Desktop app, Blog, Help and Support links in Settings.",
) {
    compatibleWith(AppCompatibilities.RAINDROP)

    dependsOn(hermesVariantsPatch(setOf(LINKS_FORM to LINKS_FORM_NULL)))
}

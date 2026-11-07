/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.ultimea.misc.controls

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.requireArm64
import app.morphe.patches.ultimea.misc.settings.patchesSettingsPatch

@Suppress("unused")
val xupmixPatch = bytecodePatch(
    name = "Xupmix",
    description = "Enables Xupmix control on Poseidon D80.",
) {
    compatibleWith(AppCompatibilities.ULTIMEA)
    availability(requireArm64)
    dependsOn(patchesSettingsPatch, xupmixReadbackPatch)
}

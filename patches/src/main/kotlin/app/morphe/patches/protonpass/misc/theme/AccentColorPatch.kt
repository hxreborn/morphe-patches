/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonpass.misc.theme

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.protonpass.misc.settings.patchesSettingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.ACCENT_COLOR_CLASS
import app.morphe.patches.shared.misc.proton.CORE_BRAND_COLORS
import app.morphe.patches.shared.misc.proton.transformBrandColors

private object PassPalette {
    const val BLUE_IRIS = 0xFF9292F9L
    const val CORNFLOWER = 0xFF7777F8L
    const val SLATE_BLUE = 0xFF6464CEL
    const val EBONY_CLAY = 0xFF282848L
    const val YANKEES_BLUE = 0xFF202038L
    const val ELECTRIC_INDIGO = 0xFF6243E6L
    const val LAVENDER_BLOOM = 0xFFA792FFL
    const val LAVENDER_MIST = 0xFFE6E0FEL
    const val BABY_BLUE_EYES = 0xFFF2EFFFL
}

private val PASS_BRAND_COLORS = listOf(
    PassPalette.BLUE_IRIS,
    PassPalette.CORNFLOWER,
    PassPalette.SLATE_BLUE,
    PassPalette.EBONY_CLAY,
    PassPalette.YANKEES_BLUE,
    PassPalette.ELECTRIC_INDIGO,
    PassPalette.LAVENDER_BLOOM,
    PassPalette.LAVENDER_MIST,
    PassPalette.BABY_BLUE_EYES,
)

private const val LOGIN_INTERACTION_NORM_MAJOR_1 = 6
private const val LOGIN_INTERACTION_NORM_MAJOR_2 = 7
private const val LOGIN_INTERACTION_NORM = 8
private const val LOGIN_INTERACTION_NORM_MINOR_1 = 9
private const val LOGIN_INTERACTION_NORM_MINOR_2 = 10

private val DARK_LOGIN_INTERACTION_ROLES = mapOf(
    LOGIN_INTERACTION_NORM_MAJOR_1 to "OrchidHue",
    LOGIN_INTERACTION_NORM_MAJOR_2 to "AmethystHaze",
    LOGIN_INTERACTION_NORM to "LavenderHaze",
    LOGIN_INTERACTION_NORM_MINOR_1 to "MysticNight",
    LOGIN_INTERACTION_NORM_MINOR_2 to "DeepBirch",
)

private val LIGHT_LOGIN_INTERACTION_ROLES = mapOf(
    LOGIN_INTERACTION_NORM_MAJOR_1 to "OrchidPink",
    LOGIN_INTERACTION_NORM_MAJOR_2 to "RoyalPurple",
    LOGIN_INTERACTION_NORM to "LavenderFloral",
    LOGIN_INTERACTION_NORM_MINOR_1 to "LavenderPink",
    LOGIN_INTERACTION_NORM_MINOR_2 to "LilacMist",
)

private const val TRANSFORM_PACKED_BRAND_COLOR = "$ACCENT_COLOR_CLASS->transformPackedBrandColor(J)J"

@Suppress("unused")
val accentColorPatch = bytecodePatch(
    name = "Custom accent color",
    description = "Changes the accent color. Choose a color in the patches menu.",
) {
    compatibleWith(AppCompatibilities.PROTON_PASS)
    dependsOn(patchesSettingsPatch)

    execute {
        transformBrandColors(CORE_BRAND_COLORS + PASS_BRAND_COLORS)

        with(PassColorsInitializerFingerprint.method) {
            transformPassColors(DARK_COLORS_FIELD, DARK_LOGIN_INTERACTION_ROLES, TRANSFORM_PACKED_BRAND_COLOR)
            transformPassColors(LIGHT_COLORS_FIELD, LIGHT_LOGIN_INTERACTION_ROLES, TRANSFORM_PACKED_BRAND_COLOR)
        }
    }
}

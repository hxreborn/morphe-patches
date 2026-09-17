/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.opcode
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

private object ProtonPalette {
    const val EERIE_BLACK = 0xFF191927L
    const val MIDNIGHT_BLUE = 0xFF222230L

    const val CHAMBRAY = 0xFF372580L
    const val SAN_MARINO = 0xFF4D34B3L
    const val PURPLE_HEART = 0xFF5C3FD9L
    const val CORNFLOWER_BLUE = 0xFF6D4AFFL
    const val PORTAGE = 0xFF8A6EFFL
    const val PERANO = 0xFFC4B7FFL
    const val BLUE_CHALK = 0xFFEAE5FFL
    const val MAGNOLIA = 0xFFF5F2FFL

    const val PALE_BLUE = 0xFFD0D0FFL
    const val PERIWINKLE = 0xFFADADFBL
    const val BLUE_BELL = 0xFF9292F9L
    const val LIGHT_SLATE_BLUE = 0xFF7777F8L
    const val LIGHT_VIOLET_BLUE = 0xFF6464CEL
    const val DUSKY_INDIGO = 0xFF4D4D9CL
    const val RHINO = 0xFF35356AL
    const val DARK_BLUE = 0xFF282848L

    const val HAITI = 0xFF1B1340L
    const val VALHALLA = 0xFF271B54L
    const val JACARTA = 0xFF2E2260L
    const val ENZIAN_BASE = 0xFF5252CCL
    const val PURPLE_BASE = 0xFF8080FFL
}

internal const val MAILBOX_BACKGROUND = ProtonPalette.EERIE_BLACK
internal const val SETTINGS_BACKGROUND = ProtonPalette.MIDNIGHT_BLUE

internal val DARK_BACKGROUND_COLORS = listOf(MAILBOX_BACKGROUND, SETTINGS_BACKGROUND)

internal const val SIDEBAR_STRUCTURE_COLOR = SETTINGS_BACKGROUND

private val LIGHT_BRAND_TONES = listOf(
    ProtonPalette.CHAMBRAY,
    ProtonPalette.SAN_MARINO,
    ProtonPalette.PURPLE_HEART,
    ProtonPalette.CORNFLOWER_BLUE,
    ProtonPalette.PORTAGE,
    ProtonPalette.PERANO,
    ProtonPalette.BLUE_CHALK,
    ProtonPalette.MAGNOLIA,
)

private val DARK_BRAND_TONES = listOf(
    ProtonPalette.PALE_BLUE,
    ProtonPalette.PERIWINKLE,
    ProtonPalette.BLUE_BELL,
    ProtonPalette.LIGHT_SLATE_BLUE,
    ProtonPalette.LIGHT_VIOLET_BLUE,
    ProtonPalette.DUSKY_INDIGO,
    ProtonPalette.RHINO,
    ProtonPalette.DARK_BLUE,
)

private val CORE_BRAND_TONES = listOf(
    ProtonPalette.HAITI,
    ProtonPalette.VALHALLA,
    ProtonPalette.JACARTA,
    ProtonPalette.ENZIAN_BASE,
    ProtonPalette.PURPLE_BASE,
)

internal val BRAND_COLORS = LIGHT_BRAND_TONES + DARK_BRAND_TONES + CORE_BRAND_TONES

private const val BRAND_NORM = ProtonPalette.CORNFLOWER_BLUE

private const val COLOR_PARAMETER_COUNT = 45

internal val PROTON_COLORS_PARAMETERS = listOf("Z") + List(COLOR_PARAMETER_COUNT) { "J" }

internal object DarkPaletteFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(literal(DARK_BACKGROUND_COLORS.first())),
)

internal object BrandPaletteFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(literal(BRAND_NORM)),
)

internal object ColorSchemeFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(
        methodCall(
            name = "<init>",
            parameters = PROTON_COLORS_PARAMETERS,
            returnType = "V",
            opcode = Opcode.INVOKE_DIRECT_RANGE,
        ),
    ),
)

internal object UpsellingDarkBackgroundFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    filters = listOf(literal(DARK_BACKGROUND_COLORS.first())),
)


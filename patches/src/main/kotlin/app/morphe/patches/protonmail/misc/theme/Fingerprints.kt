/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonmail.misc.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation
import app.morphe.patcher.InstructionLocation.MatchAfterAnywhere
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.opcode
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import app.morphe.patches.all.misc.resources.ResourceType
import app.morphe.patches.all.misc.resources.resourceLiteral
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
internal const val CORE_BACKGROUND_NORM = 0xFF1C1B24L

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

internal object CoreBackgroundNormFingerprint : Fingerprint(
    filters = listOf(literal(CORE_BACKGROUND_NORM)),
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

private const val DESIGN_THEME_PACKAGE = "Lch/protonmail/android/design/compose/theme/"

private const val CONTACT_LIST_SCREEN_GROUP_KEY = -0x1a6b0b89
private const val CONTACT_LIST_TOP_BAR_GROUP_KEY = -0x692e2b18
private const val CONTACT_SEARCH_SCREEN_GROUP_KEY = 0x4cef0155
private const val CONTACT_SEARCH_TOP_BAR_GROUP_KEY = 0x58ded95e
private const val CONTACT_CARD_GROUP_KEY = -0xca78f09

private const val SWIPE_BOX_THEME_READ_DISTANCE = 20

private fun themeColorRead(location: InstructionLocation = MatchAfterAnywhere()) = listOf(
    methodCall(definingClass = DESIGN_THEME_PACKAGE, parameters = listOf(), returnType = "J", location = location),
    opcode(Opcode.MOVE_RESULT_WIDE, MatchAfterImmediately()),
)

internal object ContactListScreenBackgroundFingerprint : Fingerprint(
    filters = listOf(literal(CONTACT_LIST_SCREEN_GROUP_KEY)) + themeColorRead(),
)

internal object ContactListTopBarBackgroundFingerprint : Fingerprint(
    filters = listOf(literal(CONTACT_LIST_TOP_BAR_GROUP_KEY)) + themeColorRead(),
)

internal object ContactSearchScreenBackgroundFingerprint : Fingerprint(
    filters = listOf(literal(CONTACT_SEARCH_SCREEN_GROUP_KEY)) + themeColorRead(),
)

internal object ContactSearchTopBarBackgroundFingerprint : Fingerprint(
    filters = listOf(literal(CONTACT_SEARCH_TOP_BAR_GROUP_KEY)) + themeColorRead(),
)

internal object ContactSearchFieldBackgroundFingerprint : Fingerprint(
    filters = listOf(resourceLiteral(ResourceType.STRING, "contact_search_placeholder")) + themeColorRead(),
)

internal object ContactCardSurfaceFingerprint : Fingerprint(
    filters = listOf(literal(CONTACT_CARD_GROUP_KEY)) + themeColorRead(),
)

internal object ContactSwipeBoxSurfaceFingerprint : Fingerprint(
    filters = listOf(string("\$this\$SwipeToDismissBox")) +
        themeColorRead(MatchAfterWithin(SWIPE_BOX_THEME_READ_DISTANCE)),
)

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
import app.morphe.patches.shared.misc.proton.CORE_BRAND_COLORS
import app.morphe.patches.shared.misc.proton.ProtonPalette
import com.android.tools.smali.dexlib2.Opcode

private object MailPalette {
    const val EERIE_BLACK = 0xFF191927L
    const val MIDNIGHT_BLUE = 0xFF222230L
    const val PURPLE_HEART = 0xFF5C3FD9L
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
}

internal const val MAILBOX_BACKGROUND = MailPalette.EERIE_BLACK
internal const val SETTINGS_BACKGROUND = MailPalette.MIDNIGHT_BLUE

internal val DARK_BACKGROUND_COLORS = listOf(MAILBOX_BACKGROUND, SETTINGS_BACKGROUND)

internal const val SIDEBAR_PRESSED_AND_SEPARATOR_COLOR = SETTINGS_BACKGROUND

private val LIGHT_BRAND_COLORS = listOf(
    ProtonPalette.CHAMBRAY,
    ProtonPalette.SAN_MARINO,
    MailPalette.PURPLE_HEART,
    ProtonPalette.CORNFLOWER_BLUE,
    ProtonPalette.PORTAGE,
    ProtonPalette.PERANO,
    MailPalette.BLUE_CHALK,
    MailPalette.MAGNOLIA,
)

private val DARK_BRAND_COLORS = listOf(
    MailPalette.PALE_BLUE,
    MailPalette.PERIWINKLE,
    MailPalette.BLUE_BELL,
    MailPalette.LIGHT_SLATE_BLUE,
    MailPalette.LIGHT_VIOLET_BLUE,
    MailPalette.DUSKY_INDIGO,
    MailPalette.RHINO,
    MailPalette.DARK_BLUE,
)

internal val BRAND_COLORS = LIGHT_BRAND_COLORS + DARK_BRAND_COLORS + CORE_BRAND_COLORS

private const val COLOR_PARAMETER_COUNT = 45

internal val PROTON_COLORS_PARAMETERS = listOf("Z") + List(COLOR_PARAMETER_COUNT) { "J" }

internal object DarkPaletteFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(literal(DARK_BACKGROUND_COLORS.first())),
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

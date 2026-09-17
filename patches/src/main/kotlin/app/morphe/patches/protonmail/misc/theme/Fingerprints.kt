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

internal val DARK_BACKGROUND_COLORS = listOf(
    0xFF191927L, // mailbox, message and composer
    0xFF222230L, // settings and bottom sheets
)

internal val SIDEBAR_STRUCTURE_COLOR = DARK_BACKGROUND_COLORS.last()

internal val BRAND_COLORS = listOf(
    0xFF372580L, 0xFF4D34B3L, 0xFF5C3FD9L, 0xFF6D4AFFL,
    0xFF8A6EFFL, 0xFFC4B7FFL, 0xFFEAE5FFL, 0xFFF5F2FFL,
    0xFFD0D0FFL, 0xFFADADFBL, 0xFF9292F9L, 0xFF7777F8L,
    0xFF6464CEL, 0xFF4D4D9CL, 0xFF35356AL, 0xFF282848L,
    0xFF1B1340L, 0xFF271B54L, 0xFF2E2260L, 0xFF5252CCL,
    0xFF8080FFL,
)

private const val BRAND_NORM = 0xFF6D4AFFL

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


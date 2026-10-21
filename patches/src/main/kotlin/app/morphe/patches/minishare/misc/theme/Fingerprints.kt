/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.minishare.misc.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.InstructionLocation.MatchAfterWithin
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import app.morphe.patcher.opcode
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

internal const val FLAG_TRANSLUCENT_STATUS = 0x4000000

private const val BRAND_COLOR = "#ff2cadc7"
internal const val STOCK_LABEL_COLOR = "#ff282828"

private const val LAYOUT_PARAMS_CLASS = "Landroid/view/WindowManager\$LayoutParams;"
private const val COLOR_CLASS = "Landroid/graphics/Color;"

internal object HomeBackgroundPaintFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(),
    filters = listOf(
        literal(0x2c),
        literal(0xad, location = MatchAfterImmediately()),
        literal(0xc7, location = MatchAfterImmediately()),
        methodCall(definingClass = COLOR_CLASS, name = "rgb", location = MatchAfterImmediately()),
        opcode(Opcode.MOVE_RESULT, location = MatchAfterImmediately()),
        methodCall(definingClass = "Landroid/graphics/Paint;", name = "setColor", location = MatchAfterImmediately()),
    ),
)

internal object BrandHeaderBackgroundFingerprint : Fingerprint(
    filters = listOf(
        string(BRAND_COLOR),
        methodCall(definingClass = COLOR_CLASS, name = "parseColor", location = MatchAfterImmediately()),
        methodCall(name = "setBackgroundColor", location = MatchAfterWithin(2)),
    ),
)

internal object RadarLabelColorFingerprint : Fingerprint(
    filters = listOf(
        string(STOCK_LABEL_COLOR),
        methodCall(definingClass = COLOR_CLASS, name = "parseColor", location = MatchAfterImmediately()),
        methodCall(definingClass = "Landroid/widget/TextView;", name = "setTextColor", location = MatchAfterWithin(2)),
    ),
)

internal object StatusBarTintFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", "I", "I"),
    filters = listOf(
        fieldAccess(definingClass = LAYOUT_PARAMS_CLASS, name = "flags", opcode = Opcode.IGET),
        literal(FLAG_TRANSLUCENT_STATUS),
        fieldAccess(
            definingClass = LAYOUT_PARAMS_CLASS,
            name = "flags",
            opcode = Opcode.IPUT,
            location = MatchAfterWithin(2),
        ),
        methodCall(definingClass = "Landroid/view/Window;", name = "setAttributes", location = MatchAfterImmediately()),
    ),
)

internal object SplashTranslucentStatusFingerprint : Fingerprint(
    filters = listOf(
        string("arg_is_running"),
        methodCall(definingClass = "Landroid/app/Activity;", name = "getWindow"),
        literal(FLAG_TRANSLUCENT_STATUS, location = MatchAfterWithin(1)),
        methodCall(definingClass = "Landroid/view/Window;", name = "addFlags", location = MatchAfterImmediately()),
    ),
)

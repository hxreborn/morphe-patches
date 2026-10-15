/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonauthenticator.misc.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.InstructionLocation.MatchAfterImmediately
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.literal
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

internal const val BACKGROUND_GRADIENT_TOP_FIELD = "backgroundGradientTop"
private const val BACKGROUND_GRADIENT_BOTTOM_FIELD = "backgroundGradientBottom"
private const val BACKGROUND_TOP_BAR_FIELD = "backgroundTopBar"
private const val BUTTON_SHADOW_COLOR = 0x40995EFF

internal object ThemeColorsInitializerFingerprint : Fingerprint(
    name = "<clinit>",
    filters = listOf(BACKGROUND_GRADIENT_TOP_FIELD, BACKGROUND_GRADIENT_BOTTOM_FIELD, BACKGROUND_TOP_BAR_FIELD).map {
        fieldAccess(name = it, type = "J", opcode = Opcode.SPUT_WIDE)
    },
)

internal object EmptyStateImageFingerprint : Fingerprint(
    filters = listOf(
        fieldAccess(name = "ic_placeholder_saturn", type = "I", opcode = Opcode.SGET),
        methodCall(
            definingClass = "Landroidx/compose/foundation/ImageKt;",
            name = "Image",
            opcode = Opcode.INVOKE_STATIC_RANGE,
        ),
    ),
)

internal object ButtonShadowColorFingerprint : Fingerprint(
    name = "<init>",
    filters = listOf(
        literal(BUTTON_SHADOW_COLOR.toLong()),
        methodCall(
            definingClass = "Landroidx/compose/ui/graphics/ColorKt;",
            name = "Color",
            parameters = listOf("I"),
            location = MatchAfterImmediately(),
        ),
    ),
)

internal object MaterialSwitchFingerprint : Fingerprint(
    definingClass = "Landroidx/compose/material3/SwitchKt;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    name = "Switch",
    returnType = "V",
    parameters = listOf(
        "Z",
        "Lkotlin/jvm/functions/Function1;",
        "Landroidx/compose/ui/Modifier;",
        "Lkotlin/jvm/functions/Function2;",
        "Z",
        "Landroidx/compose/material3/SwitchColors;",
        "Landroidx/compose/foundation/interaction/MutableInteractionSource;",
        "Landroidx/compose/runtime/Composer;",
        "I",
        "I",
    ),
)

internal fun androidViewFingerprint(function: String, modifier: String, composer: String) = Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    name = "AndroidView",
    returnType = "V",
    parameters = listOf(function, modifier, function, composer, "I", "I"),
)

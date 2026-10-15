/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonauthenticator.misc.theme

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode

internal const val BACKGROUND_GRADIENT_TOP_FIELD = "backgroundGradientTop"
private const val BACKGROUND_GRADIENT_BOTTOM_FIELD = "backgroundGradientBottom"
private const val BACKGROUND_TOP_BAR_FIELD = "backgroundTopBar"

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

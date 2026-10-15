/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.protonauthenticator.shared

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patches.shared.misc.generated.descriptor
import app.morphe.util.matchSingle

private const val THEME_INSTANCE_FIELD = "INSTANCE"
private const val COLOR_SCHEME_CHANGED_FLAGS = 6

internal fun BytecodePatchContext.themeColorReadInstructions(color: String): String {
    val colorScheme = ColorSchemeGetterFingerprint.matchSingle()
    val theme = colorScheme.classDef
    val themeInstance = theme.fields.singleOrNull { it.name == THEME_INSTANCE_FIELD && it.type == theme.type }
        ?: throw PatchException("No $THEME_INSTANCE_FIELD in ${theme.type}")
    val themeColors = colorScheme.method.returnType
    val getterPrefix = "get${color.replaceFirstChar(Char::uppercase)}-"
    val getter = classDefBy(themeColors).methods.singleOrNull { it.name.startsWith(getterPrefix) && it.returnType == "J" }
        ?: throw PatchException("No $color getter in $themeColors")

    return """
        sget-object v0, ${descriptor(themeInstance)}
        const/4 v1, $COLOR_SCHEME_CHANGED_FLAGS
        invoke-virtual { v0, p0, v1 }, ${descriptor(colorScheme.method)}
        move-result-object v0
        invoke-virtual { v0 }, ${descriptor(getter)}
        move-result-wide v0
    """
}

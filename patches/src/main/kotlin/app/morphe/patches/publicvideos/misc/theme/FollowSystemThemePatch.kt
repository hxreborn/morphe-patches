/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.publicvideos.misc.theme

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities

private const val THEME_MODE_READ_COUNT = 2

private const val MODE_NIGHT_FOLLOW_SYSTEM = -1

private const val UI_MODE_NIGHT_MASK = 0x30

private const val UI_MODE_NIGHT_YES = 0x20

@Suppress("unused")
val followSystemThemePatch = bytecodePatch(
    name = "Follow system theme",
    description = "Switches the app between light and dark with the system theme. " +
        "The app's Night Mode setting has no effect.",
) {
    compatibleWith(AppCompatibilities.PUBLIC)

    execute {
        ThemeModeReadFingerprint.matchAll(THEME_MODE_READ_COUNT..THEME_MODE_READ_COUNT).forEach { match ->
            match.method.addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, Landroid/content/res/Resources;->getSystem()Landroid/content/res/Resources;
                    move-result-object v0
                    invoke-virtual { v0 }, Landroid/content/res/Resources;->getConfiguration()Landroid/content/res/Configuration;
                    move-result-object v0
                    iget v0, v0, Landroid/content/res/Configuration;->uiMode:I
                    and-int/lit8 v0, v0, $UI_MODE_NIGHT_MASK
                    const/16 v1, $UI_MODE_NIGHT_YES
                    if-ne v0, v1, :system_light
                    const/4 v0, 0x1
                    return v0
                    :system_light
                    const/4 v0, 0x0
                    return v0
                """,
            )
        }

        SetDefaultNightModeFingerprint.matchSingle().method.addInstruction(
            0,
            "const/4 p0, $MODE_NIGHT_FOLLOW_SYSTEM",
        )
    }
}

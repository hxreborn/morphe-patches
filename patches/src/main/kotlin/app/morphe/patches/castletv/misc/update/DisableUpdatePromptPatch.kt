/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.castletv.misc.update

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val disableUpdatePromptPatch = patch(
    name = "Disable update prompt",
    description = "Disables the new version dialog.",
) {
    compatibleWith(AppCompatibilities.CASTLE_FOR_TV)

    execute {
        VersionCodeFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                invoke-static { v0 }, Ljava/lang/Integer;->valueOf(I)Ljava/lang/Integer;
                move-result-object v0
                return-object v0
            """,
        )
        UpdateTypeFingerprint.method.returnEarly(0)
    }
}

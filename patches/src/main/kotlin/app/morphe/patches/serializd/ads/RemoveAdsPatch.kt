/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.serializd.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.playwire.stopPlaywireSdkPatch

private const val EXTENSION_CLASS = "Lapp/hxreborn/extension/serializd/AdFlags;"

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes ad placements and the advertising consent prompt.",
) {
    compatibleWith(AppCompatibilities.SERIALIZD)
    extendWith("extensions/extension.mpe")

    dependsOn(stopPlaywireSdkPatch)

    execute {
        TextResponseReceivedFingerprint.matchSingle().method.addInstructions(
            0,
            """
                invoke-static { p2 }, $EXTENSION_CLASS->disable(Ljava/lang/String;)Ljava/lang/String;
                move-result-object p2
            """,
        )
    }
}

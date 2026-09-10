/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.gstarmc.misc.rating

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patches.gstarmc.misc.jiagu.editPayloadDexes
import app.morphe.patches.gstarmc.misc.jiagu.jiaguRuntimePatch
import app.morphe.patches.shared.compat.AppCompatibilities

private const val HOME = "Lcom/stone/app/ui/activity/MainActivityHome;"

private val PROMPTS = listOf("showPraiseView", "showPraiseViewNew", "showPraiseViewNewComment")

@Suppress("unused")
val hideRatingDialogPatch = rawResourcePatch(
    name = "Hide rating dialog",
    description = "Removes the prompt asking for a store review.",
) {
    compatibleWith(AppCompatibilities.DWG_FASTVIEW)
    dependsOn(jiaguRuntimePatch)

    execute {
        val modifiedDexes = editPayloadDexes { editor ->
            if (!editor.defines(HOME)) return@editPayloadDexes
            PROMPTS.forEach { editor.forceReturnVoid(HOME, it) }
        }

        if (modifiedDexes == 0) throw PatchException("Missing class data in payload: $HOME")
    }
}

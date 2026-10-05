/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.keepa.misc.priceincrease

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.pairip.removePairipProtectionPatch

private const val DROP_OPTIONS_PROPERTY = "showDropOptions"
private const val GATED_TRACKING_TYPE = "!this.hasData&&this.isDropSelected||(this.showDropOptions=!0);"
private const val UNGATED_TRACKING_TYPE = "this.showDropOptions=!0;"

@Suppress("unused")
val unlockPriceIncreaseTrackingPatch = resourcePatch(
    name = "Unlock price increase tracking",
    description = "Adds the rise option when creating or editing a price watch.",
) {
    compatibleWith(AppCompatibilities.KEEPA)
    dependsOn(removePairipProtectionPatch)

    execute {
        val chunks = get("assets/app").listFiles { file -> file.extension == "mjs" }
            ?: throw PatchException("Could not read the application bundle directory.")

        val editorChunks = chunks.mapNotNull { chunk ->
            val source = chunk.readText()
            if (source.contains(DROP_OPTIONS_PROPERTY)) chunk to source else null
        }
        if (editorChunks.isEmpty()) {
            throw PatchException("Could not find the threshold editor.")
        }

        editorChunks.forEach { (chunk, source) ->
            if (source.split(GATED_TRACKING_TYPE).size != 2) {
                throw PatchException("Could not uniquely find the tracking type gate in ${chunk.name}.")
            }
            chunk.writeText(source.replace(GATED_TRACKING_TYPE, UNGATED_TRACKING_TYPE))
        }
    }
}

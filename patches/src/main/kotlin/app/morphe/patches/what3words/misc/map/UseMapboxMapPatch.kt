/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.what3words.misc.map

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.classDefBy
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities

private const val MAP_TYPE = "Lcom/what3words/preferences/AppPrefsManager\$MapType;"
private const val MAPBOX = "MAPBOX"

@Suppress("unused")
val useMapboxMapPatch = patch(
    name = "Use Mapbox map",
    description = "Draws the map with Mapbox instead of Google Maps.",
) {
    compatibleWith(AppCompatibilities.WHAT3WORDS)

    execute {
        if (classDefBy(MAP_TYPE).staticFields.none { it.name == MAPBOX }) {
            throw PatchException("Map type enum has no $MAPBOX")
        }

        GetMapTypeFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, $MAP_TYPE->$MAPBOX:$MAP_TYPE
                invoke-virtual { v0 }, $MAP_TYPE->ordinal()I
                move-result v0
                return v0
            """,
        )
    }
}

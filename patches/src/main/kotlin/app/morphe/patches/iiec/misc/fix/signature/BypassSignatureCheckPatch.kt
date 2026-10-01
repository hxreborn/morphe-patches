/*
 * Copyright (C) 2026 hoo-dles
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from hoo-dles/morphe-patches:
 * https://github.com/hoo-dles/morphe-patches/commit/3ad54cf13090739041b9f74c64c95e9994a0d980
 * Commit 3ad54cf13090739041b9f74c64c95e9994a0d980 (2026-07-27),
 * patches/src/main/kotlin/hoodles/morphe/patches/pydroid/misc/meta/IncludeOriginalMetadataPatch.kt
 */
package app.morphe.patches.iiec.misc.fix.signature

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch

internal val bypassSignatureCheckPatch = rawResourcePatch {
    dependsOn(removeSharedUserIdPatch)

    execute {
        val metaInf = get("META-INF", true)
        if (!metaInf.isDirectory) throw PatchException("META-INF not found in the input APK")
        if (!metaInf.renameTo(metaInf.resolveSibling("META-iNF"))) {
            throw PatchException("Could not rename META-INF to META-iNF")
        }
    }
}

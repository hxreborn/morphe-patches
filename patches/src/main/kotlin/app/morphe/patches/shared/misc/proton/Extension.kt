/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.proton

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.util.returnEarly

internal const val PROTON_EXTENSION_PACKAGE = "Lapp/hxreborn/extension/proton/"
internal const val ACCENT_COLOR_CLASS = "${PROTON_EXTENSION_PACKAGE}AccentColor;"
internal const val AMOLED_THEME_CLASS = "${PROTON_EXTENSION_PACKAGE}AmoledTheme;"
internal const val UPSELLING_VISIBILITY_CLASS = "${PROTON_EXTENSION_PACKAGE}UpsellingVisibility;"
internal const val PATCHES_MENU_CLASS = "${PROTON_EXTENSION_PACKAGE}PatchesMenu;"
private const val APPLIED_PATCHES_CLASS = "${PROTON_EXTENSION_PACKAGE}AppliedPatches;"

internal fun BytecodePatchContext.markFeaturePatched(featureClass: String) =
    mutableClassDefBy(featureClass).methods
        .single { it.name == "isPatched" }
        .returnEarly(true)

internal fun BytecodePatchContext.markPatchApplied(methodName: String) =
    mutableClassDefBy(APPLIED_PATCHES_CLASS).methods
        .single { it.name == methodName }
        .returnEarly(true)

internal fun appliedPatchMarkerPatch(methodName: String) = bytecodePatch {
    extendWith("extensions/extension.mpe")

    execute { markPatchApplied(methodName) }
}

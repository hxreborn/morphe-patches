/*
 * Copyright (C) 2026 Morphe
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Ported from MorpheApp/morphe-patches:
 * https://github.com/MorpheApp/morphe-patches/commit/99d0f41e5ca1a043d8b512556b077acf35ba5db6
 * Commit 99d0f41e5ca1a043d8b512556b077acf35ba5db6 (2026-07-25),
 * patches/src/main/kotlin/app/morphe/patches/reddit/misc/fix/signature/SpoofSignaturePatch.kt
 */
package app.morphe.patches.protonmail.misc.fix.signature

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.proton.PROTON_EXTENSION_PACKAGE
import app.morphe.patches.shared.misc.signature.spoofSignature
import app.morphe.patches.shared.misc.extension.morpheLibraryCoreExtensionPatch
import app.morphe.patches.shared.misc.extension.protonPatchApplicationExtensionPatch
import app.morphe.patches.shared.misc.extension.protonSettingsThemeExtensionPatch
import app.morphe.patches.shared.misc.extension.protonSettingsExtensionPatch
import app.morphe.patches.shared.misc.extension.sharedNightModeExtensionPatch
import app.morphe.patches.shared.misc.extension.sharedSignatureExtensionPatch

private const val APPLICATION_CLASS = "Lch/protonmail/android/App;"
private const val PATCH_APPLICATION_CLASS = "${PROTON_EXTENSION_PACKAGE}PatchApplication;"

val spoofSignaturePatch = patch {
    dependsOn(morpheLibraryCoreExtensionPatch, protonPatchApplicationExtensionPatch, protonSettingsThemeExtensionPatch, protonSettingsExtensionPatch, sharedNightModeExtensionPatch, sharedSignatureExtensionPatch)
    compatibleWith(AppCompatibilities.PROTON_MAIL)
    execute {
        spoofSignature(APPLICATION_CLASS, hostClass = PATCH_APPLICATION_CLASS)
    }
}

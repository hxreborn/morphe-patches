/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.photoeditorpro.misc.fix.signature

import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.photoeditorpro.shared.PATCH_APPLICATION_CLASS
import app.morphe.patches.photoeditorpro.shared.markPatchInstalled
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.signature.spoofSignature
import app.morphe.patches.shared.misc.extension.morpheLibraryCoreExtensionPatch
import app.morphe.patches.shared.misc.extension.photoeditorproSettingsExtensionPatch
import app.morphe.patches.shared.misc.extension.sharedActivityCallbacksAdapterExtensionPatch
import app.morphe.patches.shared.misc.extension.sharedDimensionsExtensionPatch
import app.morphe.patches.shared.misc.extension.sharedSignatureExtensionPatch

private const val APPLICATION_CLASS = "Lcom/camerasideas/collagemaker/activity/CollageMakerApplication;"

val spoofSignaturePatch = patch {
    dependsOn(morpheLibraryCoreExtensionPatch, photoeditorproSettingsExtensionPatch, sharedActivityCallbacksAdapterExtensionPatch, sharedDimensionsExtensionPatch, sharedSignatureExtensionPatch)
    compatibleWith(AppCompatibilities.PHOTO_EDITOR_PRO)
    execute {
        spoofSignature(APPLICATION_CLASS, hostClass = PATCH_APPLICATION_CLASS)

        InitializeLicenseCheckFingerprint.methodOrNull?.returnEarly()

        markPatchInstalled("pep_spoof_signature")
    }
}

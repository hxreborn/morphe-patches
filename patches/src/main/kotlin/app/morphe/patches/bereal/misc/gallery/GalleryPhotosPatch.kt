/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.gallery

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.bereal.misc.settings.BEREAL_EXTENSION_PACKAGE
import app.morphe.patches.bereal.misc.settings.settingsPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.insertFirst
import app.morphe.util.setExtensionIsPatchIncluded

private val mediaPermissions = listOf(
    "android.permission.READ_MEDIA_IMAGES",
    "android.permission.READ_MEDIA_VISUAL_USER_SELECTED",
)

private val addMediaPermissionsPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            val manifest = document.documentElement
            mediaPermissions.forEach { permission ->
                manifest.insertFirst(
                    document.createElement("uses-permission").apply {
                        setAttribute("android:name", permission)
                    },
                )
            }
        }
    }
}

@Suppress("unused")
val galleryPhotosPatch = bytecodePatch(
    name = "Post from gallery",
    description = "Adds a gallery button to the camera for picking the back and front photo of your BeReal.",
) {
    compatibleWith(AppCompatibilities.BEREAL)
    dependsOn(settingsPatch, addMediaPermissionsPatch)

    execute {
        setExtensionIsPatchIncluded("${BEREAL_EXTENSION_PACKAGE}GalleryPhotos;")
        val cameraViewModel = CameraViewModelFingerprint.matchSingle().classDef
        cameraBindFingerprint(cameraViewModel.type).matchSingle().method.addInstruction(
            0,
            "invoke-static/range { p0 .. p2 }, ${BEREAL_EXTENSION_PACKAGE}GalleryPhotos;->cameraOpened(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V",
        )
        CapturedPhotoTransformFingerprint.matchSingle().method.apply {
            addInstructionsWithLabels(
                0,
                """
                    invoke-static/range { p2 .. p2 }, ${BEREAL_EXTENSION_PACKAGE}GalleryPhotos;->replace(Z)Landroid/graphics/Bitmap;
                    move-result-object v0
                    if-eqz v0, :camera
                    move-object/from16 p0, v0
                    const/16 p1, 0x0
                    const/16 p2, 0x0
                """,
                ExternalLabel("camera", getInstruction(0)),
            )
        }
    }
}

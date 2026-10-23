/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.bereal.misc.gallery

import app.morphe.patcher.Fingerprint

internal object CapturedPhotoTransformFingerprint : Fingerprint(
    isStatic = true,
    parameters = listOf("Landroid/graphics/Bitmap;", "F", "Z", "I", "I"),
    strings = listOf("single pass transform + crop", "flipHorizontally"),
)

internal object CameraViewModelFingerprint : Fingerprint(
    strings = listOf("BTS finalization failed before upload; continuing with photos only"),
)

internal fun cameraBindFingerprint(cameraViewModel: String) = Fingerprint(
    definingClass = cameraViewModel,
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Landroid/content/Context;", "Landroidx/lifecycle/LifecycleOwner;", "L"),
    strings = listOf("Unknown error while binding camera"),
)

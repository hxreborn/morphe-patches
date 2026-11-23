/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.shared.misc.liapp

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.misc.extension.sharedProxyHandlerExtensionPatch
import app.morphe.patches.shared.misc.extension.sharedLiappPackageFilterExtensionPatch

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/hxreborn/extension/shared/LiappPackageFilter;"

val liappPackageFilterPatch = patch {
    dependsOn(sharedProxyHandlerExtensionPatch, sharedLiappPackageFilterExtensionPatch)
    execute {
        LiappInitFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->install(Landroid/content/Context;)V",
        )
    }
}

/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.atvtools.misc.fix.signature

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch

internal val disableSignatureCheckPatch = rawResourcePatch {
    execute {
        val library = get(AtvToolsSignatureCheckTarget.ARM32, true)
        if (!library.exists()) {
            throw PatchException(
                "atvTools patches only armeabi-v7a: " +
                    "${AtvToolsSignatureCheckTarget.ARM32} is missing from this APK",
            )
        }

        val bytes = library.readBytes()
        AtvToolsSignatureCheckTarget.applyArm32(bytes)
        library.writeBytes(bytes)
    }
}

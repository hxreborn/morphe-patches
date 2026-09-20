/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.atvtools.misc.fix.signature

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch

internal val disableSignatureCheckPatch = rawResourcePatch {
    execute {
        val library = get(AtvToolsSignatureCheckTarget.ARM32_LIBRARY, true)
        if (!library.exists()) {
            throw PatchException(
                "atvTools de-tamper supports only armeabi-v7a; " +
                    "${AtvToolsSignatureCheckTarget.ARM32_LIBRARY} is not present in this APK",
            )
        }

        val bytes = library.readBytes()
        AtvToolsSignatureCheckTarget.disarmArm32(bytes)
        library.writeBytes(bytes)
    }
}

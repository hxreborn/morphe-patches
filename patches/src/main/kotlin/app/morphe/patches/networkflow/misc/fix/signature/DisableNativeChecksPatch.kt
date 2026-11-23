/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.networkflow.misc.fix.signature

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile

internal val disableNativeChecksPatch = patch {
    execute {
        val target = NetworkFlowNativeCheckTarget.ARM64
        val library = resourceFile(target.libraryPath, true)
        if (!library.exists()) throw PatchException("${target.libraryPath} is missing")

        val bytes = library.readBytes()
        target.applyTo(bytes)
        library.writeBytes(bytes)
    }
}

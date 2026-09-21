/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.atvtools.misc.fix.signature

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.rawResourcePatch

internal val disableSignatureCheckPatch = rawResourcePatch {
    execute {
        val present = AtvToolsSignatureCheckTarget.targets
            .map { it to get(it.library, true) }
            .filter { (_, library) -> library.exists() }

        if (present.isEmpty()) {
            throw PatchException(
                "no atvTools native library found: expected one of " +
                    AtvToolsSignatureCheckTarget.targets.joinToString { it.library },
            )
        }

        present.forEach { (target, library) ->
            val bytes = library.readBytes()
            target.applyTo(bytes)
            library.writeBytes(bytes)
        }
    }
}

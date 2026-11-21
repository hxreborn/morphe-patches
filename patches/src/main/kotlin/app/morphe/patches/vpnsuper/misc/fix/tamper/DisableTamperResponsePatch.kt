/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.vpnsuper.misc.fix.tamper

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.patch
import app.morphe.patcher.patch.resourceFile
import app.morphe.patches.shared.misc.liapp.disableLiappTamperAlertPatch

internal val disableTamperResponsePatch = patch {
    dependsOn(disableLiappTamperAlertPatch)

    execute {
        val presentLibraries = LiappTamperResponseTarget.targets
            .map { it to resourceFile(it.libraryPath, true) }
            .filter { (_, library) -> library.exists() }

        if (presentLibraries.isEmpty()) {
            throw PatchException(
                "no LIAPP native library found: expected one of " +
                    LiappTamperResponseTarget.targets.joinToString { it.libraryPath },
            )
        }

        presentLibraries.forEach { (target, library) ->
            val bytes = library.readBytes()
            target.applyTo(bytes)
            library.writeBytes(bytes)
        }
    }
}

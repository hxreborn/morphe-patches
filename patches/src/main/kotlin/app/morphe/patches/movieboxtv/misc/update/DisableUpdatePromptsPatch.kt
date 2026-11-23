/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.movieboxtv.misc.update

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.ijiami.editIjiamiPayload
import app.morphe.patches.shared.misc.ijiami.ijiamiPatch
import app.morphe.patches.shared.misc.requireArm

private const val REMOTE_VERSION_INFO = "Lcom/transsion/version/update/RemoteVersionInfo;"

@Suppress("unused")
val disableUpdatePromptsPatch = patch(
    name = "Disable update prompts",
    description = "Removes update prompts and forced updates. Requires Android 10 or later.",
) {
    compatibleWith(AppCompatibilities.MOVIEBOX_TV)
    dependsOn(ijiamiPatch)
    availability(requireArm)

    execute {
        editIjiamiPayload {
            listOf("getForceUpdate", "getHasUpdate").forEach {
                method(REMOTE_VERSION_INFO, it).returnEarly(false)
            }
        }
    }
}

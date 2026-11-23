/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.moviebox.misc.permanentnotification

import app.morphe.patcher.patch.patch
import app.morphe.patches.moviebox.misc.playback.playbackPatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.ijiami.editIjiamiPayload
import app.morphe.patches.shared.misc.requireArm64StandardInstall

private const val PERMANENT_CONFIG = "Lcom/transsion/mpush/core/config/PermanentConfig;"

@Suppress("unused")
val hidePermanentNotificationPatch = patch(
    name = "Hide permanent notification",
    description = "Hides the permanent notification.",
) {
    compatibleWith(AppCompatibilities.MOVIEBOX)
    dependsOn(playbackPatch)
    availability(requireArm64StandardInstall)

    execute {
        editIjiamiPayload {
            method(PERMANENT_CONFIG, "getEnable").returnEarly(false)
        }
    }
}

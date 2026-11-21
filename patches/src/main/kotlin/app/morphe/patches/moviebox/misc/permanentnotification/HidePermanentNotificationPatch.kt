/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.moviebox.misc.permanentnotification

import app.morphe.patcher.patch.patch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.ijiami.editIjiamiPayload
import app.morphe.patches.shared.misc.ijiami.ijiamiPatch
import app.morphe.patches.shared.misc.requireArm64

private const val PERMANENT_CONFIG = "Lcom/transsion/mpush/core/config/PermanentConfig;"

@Suppress("unused")
val hidePermanentNotificationPatch = patch(
    name = "Hide permanent notification",
    description = "Hides the permanent notification. Requires All-In-One.",
) {
    compatibleWith(AppCompatibilities.MOVIEBOX)
    dependsOn(ijiamiPatch)
    availability(requireArm64)

    execute {
        editIjiamiPayload {
            method(PERMANENT_CONFIG, "getEnable").returnEarly(false)
        }
    }
}

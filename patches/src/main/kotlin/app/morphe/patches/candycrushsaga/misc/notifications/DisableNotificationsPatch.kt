/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.misc.notifications

import app.morphe.patcher.patch.document
import app.morphe.patcher.patch.patch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.manifest.removeUsesPermissions
import app.morphe.patches.shared.misc.requireStandardInstall

private val removeNotificationPermissionPatch = patch {
    execute {
        document("AndroidManifest.xml").use { document ->
            document.removeUsesPermissions(setOf("android.permission.POST_NOTIFICATIONS"))
        }
    }
}

@Suppress("unused")
val disableNotificationsPatch = patch(
    name = "Disable notifications",
    description = "Stops game reminders and the notification permission prompt.",
) {
    compatibleWith(AppCompatibilities.CANDY_CRUSH_SAGA, AppCompatibilities.CANDY_CRUSH_SODA_SAGA)
    availability(requireStandardInstall)
    dependsOn(removeNotificationPermissionPatch)

    execute {
        EnableNotificationFingerprint.method.returnEarly()
        ScheduleLocalNotificationFingerprint.method.returnEarly()
    }
}

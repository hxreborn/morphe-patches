/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.misc.notifications

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.shared.misc.manifest.removeUsesPermissions
import app.morphe.patches.shared.misc.requireStandardInstall

private val removeNotificationPermissionPatch = resourcePatch {
    execute {
        document("AndroidManifest.xml").use { document ->
            document.removeUsesPermissions(setOf("android.permission.POST_NOTIFICATIONS"))
        }
    }
}

@Suppress("unused")
val disableNotificationsPatch = bytecodePatch(
    name = "Disable notifications",
    description = "Blocks game reminders and notification permission prompts. Requires a standard install.",
) {
    compatibleWith(AppCompatibilities.CANDY_CRUSH_SAGA, AppCompatibilities.CANDY_CRUSH_SODA_SAGA)
    availability(requireStandardInstall)
    dependsOn(removeNotificationPermissionPatch)

    execute {
        EnableNotificationFingerprint.matchSingle().method.returnEarly()
        ScheduleLocalNotificationFingerprint.matchSingle().method.returnEarly()
    }
}

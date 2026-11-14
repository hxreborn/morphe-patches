/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.candycrushsaga.misc.notifications

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.returnEarly
import app.morphe.patches.shared.compat.AppCompatibilities

@Suppress("unused")
val disableNotificationsPatch = bytecodePatch(
    name = "Disable notifications",
    description = "Blocks game reminders and the prompt asking to allow notifications.",
) {
    compatibleWith(AppCompatibilities.CANDY_CRUSH_SAGA)

    execute {
        EnableNotificationFingerprint.method.returnEarly()
        ScheduleLocalNotificationFingerprint.method.returnEarly()
    }
}

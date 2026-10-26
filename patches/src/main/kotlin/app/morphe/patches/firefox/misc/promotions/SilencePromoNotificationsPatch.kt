/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.morphe.patches.firefox.misc.promotions

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patches.firefox.misc.ui.PATCH_SETTINGS
import app.morphe.patches.firefox.shared.forceSettingsPreference
import app.morphe.patches.firefox.shared.listInSheet
import app.morphe.patches.firefox.shared.whenEnabled
import app.morphe.patches.shared.compat.AppCompatibilities

private const val SILENCE = "$PATCH_SETTINGS->silencePromoNotifications()Z"

@Suppress("unused")
val silencePromoNotificationsPatch = bytecodePatch(
    name = "Silence promo notifications",
    description = "Stops promotional and weekly privacy report notifications.",
    default = true,
) {
    compatibleWith(AppCompatibilities.FIREFOX)

    extendWith("extensions/extension.mpe")

    execute {
        listInSheet("silencePromoNotifications")

        MessageNotificationWorkFingerprint.matchSingle().method.whenEnabled(SILENCE) { (result) ->
            """
                new-instance $result, $WORKER_RESULT_SUCCESS
                invoke-direct { $result }, $WORKER_RESULT_SUCCESS-><init>()V
                return-object $result
            """
        }

        forceSettingsPreference("pref_key_enable_weekly_privacy_notification", false, SILENCE)
    }
}

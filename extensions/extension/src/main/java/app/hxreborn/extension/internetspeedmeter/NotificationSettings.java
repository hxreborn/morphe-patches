/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Additional term under GPLv3 Section 7(b): When conveying this material
 * or modified versions, preserve this notice and credit hxreborn for this
 * material in any Appropriate Legal Notices displayed by works containing it.
 */
package app.hxreborn.extension.internetspeedmeter;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;

@SuppressWarnings("unused")
public final class NotificationSettings {

    private static final int ACCENT_LIGHT = 2;

    private static final int ACCENT_DARK = 3;

    private static final float ACCENT_DARKENING = 0.1f;

    private static PendingIntent overviewIntent;

    private static PendingIntent mainIntent;

    private NotificationSettings() {

    }

    public static void applyAccentAndTapAction(Context context, Notification.Builder builder) {
        SharedPreferences preferences = AppResources.preferences(context);
        builder.setColor(accentColor(context, preferences,
                preferences.getInt(PremiumSettings.ACCENT, PremiumSettings.ACCENT_THEME)));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setColorized(preferences.getBoolean(PremiumSettings.ACCENT_BACKGROUND, false));
        }
        builder.setContentIntent((preferences.getInt(PremiumSettings.TAP_ACTION,
                PremiumSettings.TAP_SHOWS_DIALOG) == PremiumSettings.TAP_SHOWS_DIALOG) ? overviewIntent(context)
                        : mainIntent(context));
    }

    private static int accentColor(Context context, SharedPreferences preferences, int accent) {
        switch (accent) {
            case PremiumSettings.ACCENT_THEME:
                float[] hsv = AppResources.headerHsv(preferences);
                hsv[1] -= ACCENT_DARKENING;
                hsv[2] -= ACCENT_DARKENING;
                return Color.HSVToColor(hsv);
            case ACCENT_LIGHT:
                return context.getColor(AppResources.id(context, "color", "notif_bg_white"));
            case ACCENT_DARK:
                return context.getColor(AppResources.id(context, "color", "notif_bg_black"));
            default:
                return Notification.COLOR_DEFAULT;
        }
    }

    private static PendingIntent overviewIntent(Context context) {
        if (overviewIntent == null) {
            overviewIntent = activityIntent(context, new Intent(context, QuickOverviewActivity.class));
        }
        return overviewIntent;
    }

    private static PendingIntent mainIntent(Context context) {
        if (mainIntent == null) {
            mainIntent = activityIntent(context, new Intent().setClassName(context, AppResources.MAIN_ACTIVITY));
        }
        return mainIntent;
    }

    private static PendingIntent activityIntent(Context context, Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(context, intent.getComponent().getClassName().hashCode(), intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

}

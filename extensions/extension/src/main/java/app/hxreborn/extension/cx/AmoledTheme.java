/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.cx;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Configuration;

@SuppressWarnings("unused")
public final class AmoledTheme {
    private static final String THEME_PREFERENCE_KEY = "night_mode";
    private static final String AMOLED_PREFERENCE_KEY = "hx_amoled";
    private static final String OVERLAY_STYLE = "hx_amoled_theme_overlay";

    private AmoledTheme() {}

    public static String themeKey(String key) {
        return AMOLED_PREFERENCE_KEY.equals(key) ? THEME_PREFERENCE_KEY : key;
    }

    public static void applyOverlay(Activity activity) {
        if (!isAmoled(activity)) {
            return;
        }

        int style = activity.getResources()
                .getIdentifier(OVERLAY_STYLE, "style", activity.getPackageName());

        if (style != 0) {
            activity.getTheme().applyStyle(style, true);
        }
    }

    private static boolean isAmoled(Activity activity) {
        SharedPreferences preferences = DarkTheme.preferences(activity);

        if (preferences == null) {
            return false;
        }

        try {
            return preferences.getBoolean(AMOLED_PREFERENCE_KEY, false) && isNight(activity);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean isNight(Activity activity) {
        int uiMode = activity.getResources().getConfiguration().uiMode;

        return (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }
}

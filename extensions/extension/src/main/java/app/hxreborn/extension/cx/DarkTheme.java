/*
 * SPDX-FileCopyrightText: 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.cx;

import android.content.Context;
import android.content.SharedPreferences;

@SuppressWarnings("unused")
public final class DarkTheme {
    private static final String PREFERENCE_FILE_SUFFIX = "_preferences";
    private static final String THEME_PREFERENCE_KEY = "night_mode";
    private static final int MODE_NIGHT_FOLLOW_SYSTEM = -1;
    private static final int MODE_NIGHT_AUTO_TIME = 0;
    private static final int MODE_NIGHT_NO = 1;
    private static final int MODE_NIGHT_YES = 2;
    private static final String DEFAULT_MODE = String.valueOf(MODE_NIGHT_NO);

    private DarkTheme() {}

    public static int nightMode(Context context) {
        SharedPreferences preferences = preferences(context);

        if (preferences == null) {
            return MODE_NIGHT_NO;
        }

        int mode;

        try {
            String value = preferences.getString(THEME_PREFERENCE_KEY, DEFAULT_MODE);

            mode = value == null ? MODE_NIGHT_NO : Integer.parseInt(value);
        } catch (RuntimeException e) {
            return MODE_NIGHT_NO;
        }

        return isThemeMode(mode) ? mode : MODE_NIGHT_NO;
    }

    private static boolean isThemeMode(int mode) {
        return mode == MODE_NIGHT_FOLLOW_SYSTEM
                || mode == MODE_NIGHT_AUTO_TIME
                || mode == MODE_NIGHT_NO
                || mode == MODE_NIGHT_YES;
    }

    static SharedPreferences preferences(Context context) {
        if (context == null) {
            return null;
        }

        try {
            return context.getSharedPreferences(
                    context.getPackageName() + PREFERENCE_FILE_SUFFIX,
                    Context.MODE_PRIVATE
            );
        } catch (RuntimeException e) {
            return null;
        }
    }
}

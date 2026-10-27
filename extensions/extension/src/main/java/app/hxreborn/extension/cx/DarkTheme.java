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

    private DarkTheme() {

    }

    public static int nightMode(Context context) {
        SharedPreferences preferences = preferences(context);

        if (preferences == null) {
            return MODE_NIGHT_NO;
        }

        int mode;

        try {
            String value = preferences.getString(THEME_PREFERENCE_KEY, DEFAULT_MODE);

            mode = (value != null) ? Integer.parseInt(value) : MODE_NIGHT_NO;
        } catch (RuntimeException ex) {
            return MODE_NIGHT_NO;
        }

        return (isThemeMode(mode)) ? mode : MODE_NIGHT_NO;
    }

    private static boolean isThemeMode(int mode) {
        return mode == MODE_NIGHT_FOLLOW_SYSTEM || mode == MODE_NIGHT_AUTO_TIME || mode == MODE_NIGHT_NO
                || mode == MODE_NIGHT_YES;
    }

    static SharedPreferences preferences(Context context) {
        if (context == null) {
            return null;
        }

        try {
            return context.getSharedPreferences(context.getPackageName() + PREFERENCE_FILE_SUFFIX,
                    Context.MODE_PRIVATE);
        } catch (RuntimeException ex) {
            return null;
        }
    }

}

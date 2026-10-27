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

import android.app.Activity;
import android.content.SharedPreferences;

import app.hxreborn.extension.shared.NightMode;

@SuppressWarnings("unused")
public final class AmoledTheme {

    private static final String THEME_PREFERENCE_KEY = "night_mode";

    private static final String AMOLED_PREFERENCE_KEY = "hx_amoled";

    private static final String OVERLAY_STYLE = "hx_amoled_theme_overlay";

    private AmoledTheme() {

    }

    public static String themeKey(String key) {
        return (AMOLED_PREFERENCE_KEY.equals(key)) ? THEME_PREFERENCE_KEY : key;
    }

    public static void applyOverlay(Activity activity) {
        if (!isAmoled(activity)) {
            return;
        }

        int style = activity.getResources().getIdentifier(OVERLAY_STYLE, "style", activity.getPackageName());

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
            return preferences.getBoolean(AMOLED_PREFERENCE_KEY, false) && NightMode.isActive(activity);
        } catch (RuntimeException ex) {
            return false;
        }
    }

}

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
package app.hxreborn.extension.utorrent;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

@SuppressWarnings("unused")
public final class PatchSettings {

    static final String LOG_TAG = "hxUTorrent";
    static final String AMOLED_KEY = "prefer_amoled_dark_mode";

    private static final String PREFERENCES_NAME = "hx_utorrent_patches";

    private static volatile SharedPreferences preferences;

    private PatchSettings() {
    }

    public static void initialize(Context context) {
        if (preferences == null && context != null) {
            preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        }
    }

    public static boolean isEnabled() {
        return preferences == null || preferences.getBoolean(AMOLED_KEY, true);
    }

    static boolean setEnabled(boolean enabled) {
        boolean saved = preferences.edit().putBoolean(AMOLED_KEY, enabled).commit();
        if (!saved) {
            Log.w(LOG_TAG, "Failed to save the AMOLED setting");
        }
        return saved;
    }

    static boolean resetToDefaults() {
        return setEnabled(true);
    }

    static boolean isDark(Context context) {
        return false;
    }

}

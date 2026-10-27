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
package app.hxreborn.extension.proton;

import android.content.Context;
import android.content.SharedPreferences;
import app.morphe.extension.shared.Utils;

public final class PatchSettings {

    public static final String PREFERENCES_NAME = "hx_protonmail_patches";

    private PatchSettings() {

    }

    static boolean isFeatureEnabled(boolean patched, String key) {
        return patched && isEnabled(key, true);
    }

    public static boolean isEnabled(String key, boolean fallback) {
        final SharedPreferences preferences = preferences();
        return (preferences != null) ? preferences.getBoolean(key, fallback) : fallback;
    }

    public static void setEnabled(String key, boolean enabled) {
        final SharedPreferences preferences = preferences();
        if (preferences == null) {
            return;
        }

        preferences.edit().putBoolean(key, enabled).apply();
    }

    public static String getString(String key, String fallback) {
        final SharedPreferences preferences = preferences();
        return (preferences != null) ? preferences.getString(key, fallback) : fallback;
    }

    public static void setString(String key, String value) {
        final SharedPreferences preferences = preferences();
        if (preferences == null) {
            return;
        }

        preferences.edit().putString(key, value).apply();
    }

    private static SharedPreferences preferences() {
        try {
            final Context context = Utils.getContext();
            return (context != null) ? context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE) : null;
        } catch (Throwable ex) {
            return null;
        }
    }

}

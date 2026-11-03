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
package app.hxreborn.extension.patreon;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public final class PatchSettings {

    static final String LOG_TAG = "hxPatreon";

    static final String PREFERENCES_NAME = "hx_patreon_patches";

    private PatchSettings() {
    }

    static boolean isEnabled(String key, boolean fallback) {
        SharedPreferences preferences = preferences();
        return (preferences != null) ? preferences.getBoolean(key, fallback) : fallback;
    }

    static void setEnabled(String key, boolean enabled) {
        SharedPreferences preferences = preferences();
        if (preferences != null) {
            preferences.edit().putBoolean(key, enabled).apply();
        }
    }

    static void resetToDefaults() {
        SharedPreferences preferences = preferences();
        if (preferences != null) {
            preferences.edit().clear().commit();
        }
    }

    @SuppressLint({ "PrivateApi", "DiscouragedPrivateApi" })
    static Context application() {
        try {
            return (Context) Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null);
        } catch (ReflectiveOperationException ex) {
            Log.w(LOG_TAG, "ActivityThread.currentApplication failed", ex);
            return null;
        }
    }

    private static SharedPreferences preferences() {
        Context context = application();
        return (context != null) ? context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE) : null;
    }

}

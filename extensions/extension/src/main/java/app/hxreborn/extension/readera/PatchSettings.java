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
package app.hxreborn.extension.readera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public final class PatchSettings {

    static final String LOG_TAG = "hxReadEra";

    static final String SPEECH_PREFERENCES_NAME = "hx_readera_speech";

    static final String FOOTNOTES = "pref_tts_speak_notes";

    static final String SKIP_HEADERS = "pref_tts_skip_colontitles";

    static final String FULL_LIBRARY = "pref_full_library";

    private PatchSettings() {
    }

    static SharedPreferences nativePreferences() {
        Context context = application();
        return (context != null)
                ? context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE) : null;
    }

    static void reloadNativeSettings() {
    }

    static boolean nativeEnabled(String key, boolean fallback) {
        SharedPreferences preferences = nativePreferences();
        return preferences != null && preferences.getBoolean(key, fallback);
    }

    static void setNativeEnabled(String key, boolean enabled) {
        SharedPreferences preferences = nativePreferences();
        if (preferences != null) {
            preferences.edit().putBoolean(key, enabled).apply();
            reloadNativeSettings();
        }
    }

    static String appliedPatches() {
        return "";
    }

    static void resetToDefaults() {
        SharedPreferences nativePreferences = nativePreferences();
        if (nativePreferences != null) {
            nativePreferences.edit().putBoolean(FOOTNOTES, true).putBoolean(SKIP_HEADERS, true).commit();
            reloadNativeSettings();
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

    static SharedPreferences speechPreferences(Context context) {
        return context.getSharedPreferences(SPEECH_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

}

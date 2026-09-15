/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

import android.content.Context;
import android.content.SharedPreferences;

import app.morphe.extension.shared.Utils;

final class PatchSettings {

    static final String PREFERENCES_NAME = "hx_protonmail_patches";

    private PatchSettings() {}

    static boolean isFeatureEnabled(boolean patched, String key) {
        return patched && isEnabled(key, true);
    }

    static boolean isEnabled(String key, boolean fallback) {
        final SharedPreferences preferences = preferences();
        return preferences == null ? fallback : preferences.getBoolean(key, fallback);
    }

    static void setEnabled(String key, boolean enabled) {
        final SharedPreferences preferences = preferences();
        if (preferences == null) return;

        preferences.edit().putBoolean(key, enabled).apply();
    }

    private static SharedPreferences preferences() {
        try {
            final Context context = Utils.getContext();
            return context == null ? null : context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        } catch (Throwable t) {
            return null;
        }
    }
}

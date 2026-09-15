/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.protonmail;

import android.content.Context;
import android.content.SharedPreferences;

final class ScheduledDeletionSettings {

    static final int OFF = 0;
    static final int MINIMUM_SECONDS = 60;
    static final int MAXIMUM_SECONDS = 365 * 86400;

    private static final String INTERVAL_SECONDS = "interval_seconds";
    private static final String LAST_EMPTIED_MS = "last_emptied_ms";
    private static final String SHOW_TOAST = "show_toast";

    private ScheduledDeletionSettings() {}

    static boolean isActive(Context context) {
        return AppliedPatches.scheduledDeletion() && intervalSeconds(context) > OFF;
    }

    static int intervalSeconds(Context context) {
        final SharedPreferences preferences = preferences(context);
        return preferences == null ? OFF : clamp(preferences.getInt(INTERVAL_SECONDS, OFF));
    }

    static boolean saveIntervalSeconds(Context context, int seconds) {
        if (seconds != OFF && (seconds < MINIMUM_SECONDS || seconds > MAXIMUM_SECONDS)) {
            return false;
        }
        final SharedPreferences preferences = preferences(context);
        if (preferences == null) return false;

        final int previousSeconds = clamp(preferences.getInt(INTERVAL_SECONDS, OFF));
        final SharedPreferences.Editor editor =
                preferences.edit().putInt(INTERVAL_SECONDS, seconds);

        if (seconds != previousSeconds) {
            final long now = System.currentTimeMillis();
            for (String seen : preferences.getAll().keySet()) {
                if (seen.startsWith(LAST_EMPTIED_MS)) editor.putLong(seen, now);
            }
        }
        editor.apply();
        return true;
    }

    static boolean due(Context context, String accountId, String label, long intervalMs) {
        final SharedPreferences preferences = preferences(context);
        if (preferences == null || accountId == null) return false;

        final long lastEmptiedMs = preferences.getLong(key(label, accountId), 0L);
        final long elapsed = System.currentTimeMillis() - lastEmptiedMs;
        if (lastEmptiedMs <= 0L || elapsed < 0L) {
            recordEmptied(context, accountId, label);
            return false;
        }
        return elapsed >= intervalMs;
    }

    static boolean anyLabelDue(Context context, String accountId, String[] labels,
                               long intervalMs) {
        for (String label : labels) {
            if (due(context, accountId, label, intervalMs)) return true;
        }
        return false;
    }

    static void recordEmptied(Context context, String accountId, String label) {
        final SharedPreferences preferences = preferences(context);
        if (preferences == null || accountId == null) return;

        preferences.edit()
                .putLong(key(label, accountId), System.currentTimeMillis())
                .apply();
    }

    static boolean showsToast(Context context) {
        final SharedPreferences preferences = preferences(context);
        return preferences == null || preferences.getBoolean(SHOW_TOAST, true);
    }

    static void saveShowsToast(Context context, boolean show) {
        final SharedPreferences preferences = preferences(context);
        if (preferences == null) return;

        preferences.edit().putBoolean(SHOW_TOAST, show).apply();
    }

    private static String key(String label, String accountId) {
        return LAST_EMPTIED_MS + "_" + label + "_" + accountId;
    }

    private static int clamp(int seconds) {
        if (seconds <= OFF) return OFF;
        if (seconds < MINIMUM_SECONDS) return MINIMUM_SECONDS;
        return Math.min(seconds, MAXIMUM_SECONDS);
    }

    private static SharedPreferences preferences(Context context) {
        return context == null
                ? null
                : context.getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE);
    }
}

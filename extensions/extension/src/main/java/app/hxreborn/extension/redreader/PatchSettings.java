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
package app.hxreborn.extension.redreader;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

@SuppressWarnings("unused")
public final class PatchSettings {

    static final String LOG_TAG = "hxRedReader";

    static final String PREFERENCES_NAME = "hx_redreader_patches";

    static final String TITLES_KEY = "titles";

    static final String SUBREDDITS_KEY = "subreddits";

    static final String FLAIRS_KEY = "flairs";

    private static SharedPreferences preferences;

    private static volatile FilterRules rules = new FilterRules("", "", "");

    private PatchSettings() {
    }

    public static synchronized void initialize(Context context) {
        if (preferences != null) {
            return;
        }
        preferences = context.getApplicationContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        try {
            rules = new FilterRules(value(TITLES_KEY), value(SUBREDDITS_KEY), value(FLAIRS_KEY));
        } catch (IllegalArgumentException ex) {
            Log.w(LOG_TAG, "Saved post filter rules are invalid", ex);
        }
    }

    static synchronized String value(String key) {
        return (preferences != null) ? preferences.getString(key, "") : "";
    }

    static synchronized void save(String titles, String subreddits, String flairs) {
        FilterRules updated = new FilterRules(titles, subreddits, flairs);
        if (preferences == null) {
            throw new IllegalStateException("Post filter preferences are not initialized");
        }
        preferences.edit()
            .putString(TITLES_KEY, titles)
            .putString(SUBREDDITS_KEY, subreddits)
            .putString(FLAIRS_KEY, flairs)
            .apply();
        rules = updated;
    }

    static boolean isFiltered(String title, String subreddit, String flair) {
        return rules.matches(title, subreddit, flair);
    }

    static void resetToDefaults() {
        save("", "", "");
    }

}

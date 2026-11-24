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
package app.hxreborn.extension.outlook.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

@SuppressWarnings("unused")
public final class PatchSettings {

    public static final String LOG_TAG = "hxOutlook";

    static final String KEEP_MESSAGES_UNREAD = "keep_messages_unread";

    static final String REMOVE_INBOX_ADS = "remove_inbox_ads";

    static final String OPEN_EMAIL_TEMPLATES = "open_email_templates";

    static final String SELECT_MULTIPLE_SEARCH_RESULTS = "select_multiple_search_results";

    static final String HIDE_COPILOT = "hide_copilot";

    static final String ADD_UNREAD_SHORTCUT = "add_unread_shortcut";

    static final String HIDE_CALENDAR_TAB = "hide_calendar_tab";

    static final String HIDE_APPS_TAB = "hide_apps_tab";

    static final String HIDE_NAVIGATION_BAR = "hide_navigation_bar";

    static final boolean HIDE_NAVIGATION_BAR_DEFAULT = false;

    private static final String PREFERENCES_NAME = "hx_outlook_patches";

    private static volatile SharedPreferences preferences;

    private PatchSettings() {
    }

    public static void initialize(Context context) {
        if (preferences == null) {
            preferences = context.getApplicationContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        }
    }

    public static boolean keepMessagesUnread() {
        return isEnabled(KEEP_MESSAGES_UNREAD);
    }

    public static boolean removeInboxAds() {
        return isEnabled(REMOVE_INBOX_ADS);
    }

    public static boolean openEmailTemplates() {
        return isEnabled(OPEN_EMAIL_TEMPLATES);
    }

    public static boolean selectMultipleSearchResults() {
        return isEnabled(SELECT_MULTIPLE_SEARCH_RESULTS);
    }

    public static boolean searchActionBarEnabled(boolean featureEnabled) {
        return featureEnabled || selectMultipleSearchResults();
    }

    public static boolean hideCopilot() {
        return isEnabled(HIDE_COPILOT);
    }

    public static boolean addUnreadShortcut() {
        return isEnabled(ADD_UNREAD_SHORTCUT);
    }

    public static boolean hideCalendarTab() {
        return isEnabled(HIDE_CALENDAR_TAB);
    }

    public static boolean hideAppsTab() {
        return isEnabled(HIDE_APPS_TAB);
    }

    public static boolean hideNavigationBar() {
        return isEnabled(HIDE_NAVIGATION_BAR, HIDE_NAVIGATION_BAR_DEFAULT);
    }

    static void updateUnreadShortcut(Context context) {
    }

    static boolean isEnabled(String key) {
        return isEnabled(key, true);
    }

    static boolean isEnabled(String key, boolean defaultValue) {
        return (preferences != null) ? preferences.getBoolean(key, defaultValue) : defaultValue;
    }

    static boolean setEnabled(String key, boolean enabled) {
        boolean saved = preferences.edit().putBoolean(key, enabled).commit();
        if (!saved) {
            Log.w(LOG_TAG, "Failed to save " + key);
        }
        return saved;
    }

    static boolean resetToDefaults() {
        boolean saved = preferences.edit().clear().commit();
        if (!saved) {
            Log.w(LOG_TAG, "Failed to reset the patch settings");
        }
        return saved;
    }

}

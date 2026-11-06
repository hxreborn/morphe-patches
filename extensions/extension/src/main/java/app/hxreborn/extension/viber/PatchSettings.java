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
package app.hxreborn.extension.viber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.MenuItem;

@SuppressWarnings("unused")
public final class PatchSettings {

    static final String LOG_TAG = "hxViber";

    private static final String SELECTED_BUSINESS_CHATS = "selected_business_chats";

    private static final int RAISED_FOLDER_LIMIT = 10;

    private static SharedPreferences preferences;

    private PatchSettings() {
    }

    public static void initialize(Context context) {
        preferences = context.getSharedPreferences("hx_viber_patches", Context.MODE_PRIVATE);
    }

    static boolean isEnabled(String key) {
        return preferences == null || preferences.getBoolean(key, true);
    }

    static void setEnabled(String key, boolean enabled) {
        if (preferences != null) {
            preferences.edit().putBoolean(key, enabled).commit();
        }
    }

    static void resetToDefaults() {
        if (preferences != null) {
            preferences.edit().clear().commit();
        }
    }

    public static boolean removeChatPayments() {
        return AppliedPatches.removeChatPayments() && isEnabled("Remove chat payment actions");
    }

    public static boolean disableAiFeatures() {
        return AppliedPatches.disableAiFeatures() && isEnabled("Disable AI features");
    }

    public static boolean hideAds() {
        return AppliedPatches.hideAds() && isEnabled(PatchSetting.HIDE_ADS.key);
    }

    public static boolean hidePromotions() {
        return AppliedPatches.hidePromotions() && isEnabled(PatchSetting.HIDE_PROMOTIONS.key);
    }

    public static boolean showPromotion(boolean shown) {
        return shown && !hidePromotions();
    }

    public static boolean unlockViberPlus() {
        return AppliedPatches.unlockViberPlus() && isEnabled(PatchSetting.UNLOCK_VIBER_PLUS.key);
    }

    public static boolean unlockTypingPrivacy(boolean original) {
        return original || unlockViberPlus();
    }

    public static boolean removeOnlineStatusLimit() {
        return AppliedPatches.removeOnlineStatusLimit() && isEnabled(PatchSetting.REMOVE_ONLINE_STATUS_LIMIT.key);
    }

    public static long lastOnlineChangeTime(long lastChanged) {
        return removeOnlineStatusLimit() ? 0L : lastChanged;
    }

    public static boolean disableUpdatePrompts() {
        return AppliedPatches.disableUpdatePrompts() && isEnabled(PatchSetting.DISABLE_UPDATE_PROMPTS.key);
    }

    public static boolean allowUpdatePrompt(boolean allowed) {
        return allowed && !disableUpdatePrompts();
    }

    public static boolean hideMoreItems() {
        return AppliedPatches.hideMoreItems() && isEnabled(PatchSetting.HIDE_MORE_ITEMS.key);
    }

    public static boolean hideNavigationButtons() {
        return AppliedPatches.hideNavigationButtons() && isEnabled(PatchSetting.HIDE_NAVIGATION_BUTTONS.key);
    }

    public static boolean unfiledChats() {
        return AppliedPatches.unfiledChats() && isEnabled(PatchSetting.UNFILED_CHATS.key);
    }

    public static boolean raiseFolderLimit() {
        return AppliedPatches.raiseFolderLimit() && isEnabled(PatchSetting.RAISE_FOLDER_LIMIT.key);
    }

    public static int folderLimit(int limit) {
        return raiseFolderLimit() ? Math.max(limit, RAISED_FOLDER_LIMIT) : limit;
    }

    static Set<String> selectedBusinessChats() {
        return (preferences != null)
                ? new HashSet<>(preferences.getStringSet(SELECTED_BUSINESS_CHATS, Collections.emptySet()))
                : Collections.emptySet();
    }

    static void setSelectedBusinessChats(Set<String> selected) {
        if (preferences != null) {
            preferences.edit().putStringSet(SELECTED_BUSINESS_CHATS, selected).commit();
        }
    }

    public static boolean filterBusinessChats() {
        return AppliedPatches.filterBusinessChats() && isEnabled(PatchSetting.FILTER_BUSINESS_CHATS.key);
    }

    public static boolean disableAnalytics() {
        return AppliedPatches.disableAnalytics() && isEnabled(PatchSetting.DISABLE_ANALYTICS.key);
    }

    public static List<?> filterMoreItems(List<?> original) {
        if (!hideMoreItems()) {
            return original;
        }
        List<Object> filtered = new ArrayList<>(original.size());
        for (Object item : original) {
            if (!isHiddenMoreItem(itemId(item))) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    public static int itemId(Object item) {
        return 0;
    }

    public static boolean isHiddenMoreItem(int id) {
        return false;
    }

    public static boolean isHiddenMainMenuItem(int id) {
        return false;
    }

    public static boolean mainMenuItemVisible(MenuItem item, boolean visible) {
        if (!visible || item == null || !hideMoreItems()) {
            return visible;
        }
        return !isHiddenMainMenuItem(item.getItemId());
    }

    public static boolean disableAnalyticsKey(String key) {
        return disableAnalytics() && ("firebase_analytics_collection_enabled".equals(key)
                || "google_analytics_adid_collection_enabled".equals(key)
                || "google_analytics_ssaid_collection_enabled".equals(key));
    }

}

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
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;

@SuppressWarnings("unused")
public final class PatchSettings {

    static final String LOG_TAG = "hxViber";

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

    public static boolean hideAds() {
        return AppliedPatches.hideAdsDescription() != null && isEnabled("Hide ads");
    }

    public static boolean hideMoreItems() {
        return AppliedPatches.hideMoreItemsDescription() != null && isEnabled("Hide More screen items");
    }

    public static boolean hideNavigationButtons() {
        return AppliedPatches.hideNavigationButtonsDescription() != null && isEnabled("Hide navigation buttons");
    }

    public static boolean disableAnalytics() {
        return AppliedPatches.disableAnalyticsDescription() != null && isEnabled("Disable analytics");
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

    public static boolean disableAnalyticsKey(String key) {
        return disableAnalytics() && ("firebase_analytics_collection_enabled".equals(key)
                || "google_analytics_adid_collection_enabled".equals(key)
                || "google_analytics_ssaid_collection_enabled".equals(key));
    }

}

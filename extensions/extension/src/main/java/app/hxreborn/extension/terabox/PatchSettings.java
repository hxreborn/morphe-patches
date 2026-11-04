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
package app.hxreborn.extension.terabox;

import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import app.morphe.extension.shared.Utils;

@SuppressWarnings("unused")
public final class PatchSettings {

    static final String LOG_TAG = "hxTeraBox";

    static final String PREFERENCES_NAME = "hx_terabox_patches";

    private static final List<String> SWITCH_KEYS = Arrays.asList(AppliedPatches.HIDE_ADS,
            AppliedPatches.HIDE_PROMOTIONS, AppliedPatches.HIDE_HOME_SHORTCUTS,
            AppliedPatches.HIDE_VIDEO_RECOMMENDATIONS, AppliedPatches.UNLOCK_PREMIUM,
            AppliedPatches.REMOVE_RECENT_PANEL);

    private PatchSettings() {
    }

    public static boolean isAdsHidden() {
        return isFeatureEnabled(AppliedPatches.hideAds(), AppliedPatches.HIDE_ADS);
    }

    public static boolean isPromotionsHidden() {
        return isFeatureEnabled(AppliedPatches.hidePromotions(), AppliedPatches.HIDE_PROMOTIONS);
    }

    public static boolean isHomeShortcutsHidden() {
        return isFeatureEnabled(AppliedPatches.hideHomeShortcuts(), AppliedPatches.HIDE_HOME_SHORTCUTS);
    }

    public static boolean isVideoRecommendationsHidden() {
        return isFeatureEnabled(AppliedPatches.hideVideoRecommendations(), AppliedPatches.HIDE_VIDEO_RECOMMENDATIONS);
    }

    public static boolean isPremiumUnlocked() {
        return isFeatureEnabled(AppliedPatches.unlockPremium(), AppliedPatches.UNLOCK_PREMIUM);
    }

    public static boolean isRecentPanelRemoved() {
        return isFeatureEnabled(AppliedPatches.removeRecentPanel(), AppliedPatches.REMOVE_RECENT_PANEL);
    }

    public static long promotionAutoShow(long configured) {
        return isPromotionsHidden() ? 0 : configured;
    }

    static boolean isFeatureEnabled(boolean patched, String key) {
        return patched && isEnabled(key);
    }

    static boolean isEnabled(String key) {
        SharedPreferences preferences = preferences();
        return preferences == null || preferences.getBoolean(key, true);
    }

    static void setEnabled(String key, boolean enabled) {
        SharedPreferences preferences = preferences();
        if (preferences != null) {
            preferences.edit().putBoolean(key, enabled).commit();
        }
    }

    static void resetToDefaults() {
        SharedPreferences preferences = preferences();
        if (preferences != null) {
            SharedPreferences.Editor editor = preferences.edit();
            for (String key : SWITCH_KEYS) {
                editor.putBoolean(key, true);
            }
            editor.commit();
        }
    }

    private static SharedPreferences preferences() {
        Context context = Utils.getContext();
        return (context != null) ? context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE) : null;
    }

}

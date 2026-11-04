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
package app.hxreborn.extension.aliexpress;

import android.content.Context;
import android.content.SharedPreferences;

@SuppressWarnings("unused")
public final class PatchSettings {

    static final String LOG_TAG = "hxAliExpress";

    static final String PREFERENCES_NAME = "hx_aliexpress_patches";

    private static volatile SharedPreferences preferences;

    private static volatile Context applicationContext;

    private PatchSettings() {
    }

    public static void initialize(Context context) {
        SharedPreferences stored = context.getApplicationContext()
            .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        preferences = stored;
        applicationContext = context.getApplicationContext();
        if (AppliedPatches.disableAnalytics()) {
            initializeAnalytics(applicationContext, !disableAnalytics());
        }
    }

    private static void initializeAnalytics(Context context, boolean enabled) {
    }

    public static boolean removeSponsoredItems() {
        return isFeatureEnabled(AppliedPatches.removeSponsoredItems(), AppliedPatches.REMOVE_SPONSORED_ITEMS);
    }

    public static boolean disableAnalytics() {
        return isFeatureEnabled(AppliedPatches.disableAnalytics(), AppliedPatches.DISABLE_ANALYTICS);
    }

    public static boolean disableUpdatePrompts() {
        return isFeatureEnabled(AppliedPatches.disableUpdatePrompts(), AppliedPatches.DISABLE_UPDATE_PROMPTS);
    }

    public static boolean enableImageSaving() {
        return isFeatureEnabled(AppliedPatches.enableImageSaving(), AppliedPatches.ENABLE_IMAGE_SAVING);
    }

    public static boolean removePromotionalPopups() {
        return isFeatureEnabled(AppliedPatches.removePromotionalPopups(), AppliedPatches.REMOVE_PROMOTIONAL_POPUPS);
    }

    public static boolean skipSplashPromotions() {
        return isFeatureEnabled(AppliedPatches.skipSplashPromotions(), AppliedPatches.SKIP_SPLASH_PROMOTIONS);
    }

    public static boolean saveButtonHidden(boolean hidden) {
        return hidden && !enableImageSaving();
    }

    static boolean isFeatureEnabled(boolean applied, String key) {
        return applied && isEnabled(key);
    }

    static boolean isEnabled(String key) {
        SharedPreferences stored = preferences;
        return stored == null || stored.getBoolean(key, true);
    }

    static void setEnabled(String key, boolean enabled) {
        SharedPreferences stored = preferences;
        if (stored != null) {
            stored.edit().putBoolean(key, enabled).apply();
        }
        applyLive(key);
    }

    static void resetToDefaults() {
        SharedPreferences stored = preferences;
        if (stored == null) {
            return;
        }
        SharedPreferences.Editor editor = stored.edit();
        for (String key : AppliedPatches.SWITCH_KEYS) {
            editor.remove(key);
        }
        editor.apply();
        applyLive(AppliedPatches.DISABLE_ANALYTICS);
    }

    private static void applyLive(String key) {
        Context context = applicationContext;
        if (AppliedPatches.DISABLE_ANALYTICS.equals(key) && context != null && AppliedPatches.disableAnalytics()) {
            initializeAnalytics(context, !disableAnalytics());
        }
    }

}

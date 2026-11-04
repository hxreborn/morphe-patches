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
package app.hxreborn.extension.duolingo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import app.morphe.extension.shared.Utils;

@SuppressWarnings("unused")
public final class PatchSettings {

    static final String LOG_TAG = "hxDuolingo";

    static final String PREFERENCES_NAME = "hx_duolingo_patches";

    static final String SUBSCRIPTION_KEY = "Subscription";

    static final Subscription SUBSCRIPTION_DEFAULT = Subscription.SUPER;

    private static final List<String> SWITCH_KEYS = Arrays.asList(AppliedPatches.UNLOCK_PREMIUM,
            AppliedPatches.DISABLE_DYNAMIC_APP_ICON, AppliedPatches.ENABLE_DEBUG_MODE,
            AppliedPatches.UNLIMITED_RAMPUP_TIME);

    private static final Set<String> OFF_BY_DEFAULT = Collections.singleton(AppliedPatches.ENABLE_DEBUG_MODE);

    private static final int UNLIMITED_RAMPUP_SECONDS = 3600;

    private PatchSettings() {
    }

    public static boolean isPremiumUnlocked() {
        return isFeatureEnabled(AppliedPatches.unlockPremium(), AppliedPatches.UNLOCK_PREMIUM);
    }

    public static boolean isMaxUnlocked() {
        return isPremiumUnlocked() && subscription() == Subscription.MAX;
    }

    public static int energyOrMax(int energy, int maxEnergy) {
        return isPremiumUnlocked() ? maxEnergy : energy;
    }

    public static boolean isDynamicAppIconDisabled() {
        return isFeatureEnabled(AppliedPatches.disableDynamicAppIcon(), AppliedPatches.DISABLE_DYNAMIC_APP_ICON);
    }

    public static boolean isDebugModeEnabled() {
        return isFeatureEnabled(AppliedPatches.enableDebugMode(), AppliedPatches.ENABLE_DEBUG_MODE);
    }

    public static int rampUpSecondsOrUnlimited(int seconds) {
        return isFeatureEnabled(AppliedPatches.unlimitedRampUpTime(), AppliedPatches.UNLIMITED_RAMPUP_TIME)
                ? UNLIMITED_RAMPUP_SECONDS : seconds;
    }

    static boolean isFeatureEnabled(boolean patched, String key) {
        return patched && isEnabled(key, defaultEnabled(key));
    }

    static boolean defaultEnabled(String key) {
        return !OFF_BY_DEFAULT.contains(key);
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

    static Subscription subscription() {
        SharedPreferences preferences = preferences();
        String stored = (preferences != null) ? preferences.getString(SUBSCRIPTION_KEY, null) : null;
        if (stored == null) {
            return SUBSCRIPTION_DEFAULT;
        }
        try {
            return Subscription.valueOf(stored);
        } catch (IllegalArgumentException ex) {
            Log.w(LOG_TAG, "Unknown stored subscription " + stored, ex);
            return SUBSCRIPTION_DEFAULT;
        }
    }

    static void setSubscription(Subscription subscription) {
        SharedPreferences preferences = preferences();
        if (preferences != null) {
            preferences.edit().putString(SUBSCRIPTION_KEY, subscription.name()).apply();
        }
    }

    static void resetToDefaults() {
        SharedPreferences preferences = preferences();
        if (preferences == null) {
            return;
        }
        SharedPreferences.Editor editor = preferences.edit();
        for (String key : SWITCH_KEYS) {
            editor.putBoolean(key, defaultEnabled(key));
        }
        editor.putString(SUBSCRIPTION_KEY, SUBSCRIPTION_DEFAULT.name());
        editor.commit();
    }

    private static SharedPreferences preferences() {
        Context context = Utils.getContext();
        return (context != null) ? context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE) : null;
    }

    enum Subscription {

        SUPER("Super", "Removes ads and keeps energy full."),

        MAX("Max", "Adds Explain My Mistake and the Max badge. Video Call does not work.");

        final String label;

        final String description;

        Subscription(String label, String description) {
            this.label = label;
            this.description = description;
        }

    }

}

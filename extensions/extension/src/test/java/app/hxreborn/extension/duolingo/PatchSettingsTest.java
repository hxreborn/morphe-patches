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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import app.morphe.extension.shared.Utils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public final class PatchSettingsTest {

    private static final int ENERGY = 3;

    private static final int MAX_ENERGY = 25;

    private static final int UNLIMITED_RAMPUP_SECONDS = 3600;

    private static final int RAMPUP_SECONDS = 90;

    private static final String UNKNOWN_SUBSCRIPTION = "GOLD";

    @Before
    public void useApplicationContext() {
        Utils.setContext(RuntimeEnvironment.getApplication());
    }

    private static void storeEnabled(String key, boolean enabled) {
        RuntimeEnvironment.getApplication()
            .getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key, enabled)
            .commit();
    }

    @Test
    public void returnsFalseForUnpatchedFeatureWhenSwitchIsOn() {
        storeEnabled(AppliedPatches.UNLOCK_PREMIUM, true);

        assertFalse(PatchSettings.isPremiumUnlocked());
        assertEquals(ENERGY, PatchSettings.energyOrMax(ENERGY, MAX_ENERGY));
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void enablesPatchedFeaturesByDefault() {
        assertTrue(PatchSettings.isPremiumUnlocked());
        assertTrue(PatchSettings.isDynamicAppIconDisabled());
        assertTrue(PatchSettings.isDebugModeEnabled());
        assertEquals(MAX_ENERGY, PatchSettings.energyOrMax(ENERGY, MAX_ENERGY));
        assertEquals(UNLIMITED_RAMPUP_SECONDS, PatchSettings.rampUpSecondsOrUnlimited(RAMPUP_SECONDS));
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void keepsAppValuesWhenSwitchesAreOff() {
        storeEnabled(AppliedPatches.UNLOCK_PREMIUM, false);
        storeEnabled(AppliedPatches.UNLIMITED_RAMPUP_TIME, false);

        assertFalse(PatchSettings.isPremiumUnlocked());
        assertEquals(ENERGY, PatchSettings.energyOrMax(ENERGY, MAX_ENERGY));
        assertEquals(RAMPUP_SECONDS, PatchSettings.rampUpSecondsOrUnlimited(RAMPUP_SECONDS));
        assertTrue(PatchSettings.isDebugModeEnabled());
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void persistsSwitchState() {
        PatchSettings.setEnabled(AppliedPatches.ENABLE_DEBUG_MODE, false);

        assertFalse(PatchSettings.isDebugModeEnabled());
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void unlocksSuperWithoutMaxByDefault() {
        assertEquals(PatchSettings.Subscription.SUPER, PatchSettings.subscription());
        assertFalse(PatchSettings.isMaxUnlocked());
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void unlocksMaxWhenMaxIsChosen() {
        PatchSettings.setSubscription(PatchSettings.Subscription.MAX);

        assertTrue(PatchSettings.isMaxUnlocked());
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void keepsMaxLockedWhenUnlockPremiumIsOff() {
        PatchSettings.setSubscription(PatchSettings.Subscription.MAX);
        storeEnabled(AppliedPatches.UNLOCK_PREMIUM, false);

        assertFalse(PatchSettings.isMaxUnlocked());
    }

    @Test
    public void fallsBackToSuperForUnknownStoredSubscription() {
        RuntimeEnvironment.getApplication()
            .getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PatchSettings.SUBSCRIPTION_KEY, UNKNOWN_SUBSCRIPTION)
            .commit();

        assertEquals(PatchSettings.Subscription.SUPER, PatchSettings.subscription());
    }

    @Test
    public void returnsFallbackAndStoresNothingWithoutContext() {
        // given
        Utils.setContext(null);

        // when
        PatchSettings.setEnabled(AppliedPatches.UNLOCK_PREMIUM, false);

        // then
        assertFalse(PatchSettings.isEnabled(AppliedPatches.UNLOCK_PREMIUM, false));
        assertTrue(PatchSettings.isEnabled(AppliedPatches.UNLOCK_PREMIUM, true));
    }

    @Test
    public void resetWritesEverySwitchBackToItsDefault() {
        // given
        storeEnabled(AppliedPatches.UNLOCK_PREMIUM, false);
        storeEnabled(AppliedPatches.ENABLE_DEBUG_MODE, false);
        PatchSettings.setSubscription(PatchSettings.Subscription.MAX);

        // when
        PatchSettings.resetToDefaults();

        // then
        assertEquals(PatchSettings.Subscription.SUPER, PatchSettings.subscription());
        for (String key : new String[] { AppliedPatches.UNLOCK_PREMIUM, AppliedPatches.DISABLE_DYNAMIC_APP_ICON,
                AppliedPatches.ENABLE_DEBUG_MODE, AppliedPatches.UNLIMITED_RAMPUP_TIME }) {
            assertTrue(key, PatchSettings.isEnabled(key, false));
        }
    }

}

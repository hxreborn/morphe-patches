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
package app.hxreborn.extension.liftoff;

import android.content.Context;
import app.morphe.extension.shared.Utils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public final class PatchSettingsTest {

    @Before
    public void attachApplication() {
        Utils.setContext(RuntimeEnvironment.getApplication());
        clearPreferences();
    }

    @After
    public void clearPreferences() {
        RuntimeEnvironment.getApplication()
            .getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit();
    }

    @Test
    public void disablesUnappliedFeatures() {
        Assert.assertFalse(PatchSettings.isPremiumUnlocked());
        Assert.assertFalse(PatchSettings.areCustomizationsUnlocked());
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void enablesAppliedFeaturesByDefault() {
        Assert.assertTrue(PatchSettings.isPremiumUnlocked());
        Assert.assertTrue(PatchSettings.areCustomizationsUnlocked());
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void preservesDisabledSwitches() {
        PatchSettings.setEnabled(AppliedPatches.UNLOCK_PREMIUM, false);
        PatchSettings.setEnabled(AppliedPatches.UNLOCK_CUSTOMIZATIONS, false);

        Assert.assertFalse(PatchSettings.isPremiumUnlocked());
        Assert.assertFalse(PatchSettings.areCustomizationsUnlocked());
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void restoresSwitchDefaults() {
        PatchSettings.setEnabled(AppliedPatches.UNLOCK_PREMIUM, false);
        PatchSettings.setEnabled(AppliedPatches.UNLOCK_CUSTOMIZATIONS, false);

        PatchSettings.resetToDefaults();

        Assert.assertTrue(PatchSettings.isPremiumUnlocked());
        Assert.assertTrue(PatchSettings.areCustomizationsUnlocked());
    }

}

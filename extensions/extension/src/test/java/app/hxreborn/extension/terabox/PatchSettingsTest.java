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
@Config(sdk = { 23, 36 })
public final class PatchSettingsTest {

    private static final long AUTO_SHOW_DELAY = 30;

    @Before
    public void setContext() {
        Utils.setContext(RuntimeEnvironment.getApplication());
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
    public void keepsUnappliedFeaturesOff() {
        Assert.assertFalse(PatchSettings.isAdsHidden());
        Assert.assertFalse(PatchSettings.isPromotionsHidden());
        Assert.assertFalse(PatchSettings.isHomeShortcutsHidden());
        Assert.assertFalse(PatchSettings.isVideoRecommendationsHidden());
        Assert.assertFalse(PatchSettings.isPremiumUnlocked());
        Assert.assertFalse(PatchSettings.isRecentPanelRemoved());
        Assert.assertFalse(PatchSettings.isBackupCountShown());
        Assert.assertEquals(AUTO_SHOW_DELAY, PatchSettings.promotionAutoShow(AUTO_SHOW_DELAY));
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void enablesAppliedFeaturesByDefault() {
        Assert.assertTrue(PatchSettings.isAdsHidden());
        Assert.assertTrue(PatchSettings.isPromotionsHidden());
        Assert.assertTrue(PatchSettings.isHomeShortcutsHidden());
        Assert.assertTrue(PatchSettings.isVideoRecommendationsHidden());
        Assert.assertTrue(PatchSettings.isPremiumUnlocked());
        Assert.assertTrue(PatchSettings.isRecentPanelRemoved());
        Assert.assertTrue(PatchSettings.isBackupCountShown());
        Assert.assertEquals(0, PatchSettings.promotionAutoShow(AUTO_SHOW_DELAY));
    }

    @Test
    @Config(shadows = EveryPatchApplied.class)
    public void persistsIndependentSwitchesAndResetsDefaults() {
        PatchSettings.setEnabled(AppliedPatches.UNLOCK_PREMIUM, false);
        PatchSettings.setEnabled(AppliedPatches.HIDE_PROMOTIONS, false);
        PatchSettings.setEnabled(AppliedPatches.REMOVE_RECENT_PANEL, false);
        PatchSettings.setEnabled(AppliedPatches.SHOW_BACKUP_COUNT, false);

        Assert.assertFalse(PatchSettings.isPremiumUnlocked());
        Assert.assertFalse(PatchSettings.isPromotionsHidden());
        Assert.assertFalse(PatchSettings.isRecentPanelRemoved());
        Assert.assertFalse(PatchSettings.isBackupCountShown());
        Assert.assertTrue(PatchSettings.isHomeShortcutsHidden());
        Assert.assertEquals(AUTO_SHOW_DELAY, PatchSettings.promotionAutoShow(AUTO_SHOW_DELAY));
        PatchSettings.resetToDefaults();
        Assert.assertTrue(PatchSettings.isPremiumUnlocked());
        Assert.assertTrue(PatchSettings.isPromotionsHidden());
        Assert.assertTrue(PatchSettings.isRecentPanelRemoved());
        Assert.assertTrue(PatchSettings.isBackupCountShown());
    }

}

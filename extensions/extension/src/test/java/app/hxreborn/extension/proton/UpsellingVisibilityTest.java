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
package app.hxreborn.extension.proton;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
public final class UpsellingVisibilityTest {

    private Context context;

    @Before
    public void useApplicationContext() {
        this.context = PatchedBuild.useApplicationContext();
    }

    @Test
    public void unpatchedBuildHidesNothing() {
        assertFalse(UpsellingVisibility.isPatched());
        assertFalse(UpsellingVisibility.isHidden());
    }

    @Test
    public void unpatchedBuildIgnoresAStoredHideSetting() {
        // when
        PatchedBuild.setUpgradePromotionsHidden(true);

        // then
        assertFalse(UpsellingVisibility.isHidden());
        assertTrue(UpsellingVisibility.resolveUpgradeAvailable(true));
    }

    @Test
    public void upgradeAvailabilityPassesThroughWhenNothingIsHidden() {
        assertTrue(UpsellingVisibility.resolveUpgradeAvailable(true));
        assertFalse(UpsellingVisibility.resolveUpgradeAvailable(false));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void patchedBuildHidesUntilTurnedOff() {
        assertTrue(UpsellingVisibility.isPatched());
        assertTrue(UpsellingVisibility.isHidden());

        PatchedBuild.setUpgradePromotionsHidden(false);
        assertFalse(UpsellingVisibility.isHidden());

        PatchedBuild.setUpgradePromotionsHidden(true);
        assertTrue(UpsellingVisibility.isHidden());
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void hiddenUpgradesResolveToUnavailable() {
        assertFalse(UpsellingVisibility.resolveUpgradeAvailable(true));
        assertFalse(UpsellingVisibility.resolveUpgradeAvailable(false));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void shownUpgradesPassThrough() {
        PatchedBuild.setUpgradePromotionsHidden(false);

        assertTrue(UpsellingVisibility.resolveUpgradeAvailable(true));
        assertFalse(UpsellingVisibility.resolveUpgradeAvailable(false));
    }

    @Test
    @Config(shadows = { PatchedBuild.Amoled.class, PatchedBuild.Accent.class, PatchedBuild.Upselling.class,
            PatchedBuild.Applied.class })
    public void settingIsStoredInThePatchPreferences() {
        // when
        PatchedBuild.setUpgradePromotionsHidden(false);

        // then
        assertFalse(this.context.getSharedPreferences(PatchSettings.PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getBoolean("hide_upgrade_promotions", true));
    }

}

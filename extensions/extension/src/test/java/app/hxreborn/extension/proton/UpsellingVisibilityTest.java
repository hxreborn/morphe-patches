/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
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

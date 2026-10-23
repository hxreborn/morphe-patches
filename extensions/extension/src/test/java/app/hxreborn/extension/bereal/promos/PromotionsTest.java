/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal.promos;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import app.hxreborn.extension.bereal.PatchSettings;
import app.morphe.extension.shared.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class PromotionsTest {

    @Before
    public void attachContext() {
        Utils.setContext(RuntimeEnvironment.getApplication());
    }

    @After
    public void restoreDefaults() {
        PatchSettings.REMOVE_PROMOTIONS.save(true);
    }

    @Test
    public void removesPromotionsByDefault() {
        assertTrue(Promotions.removed());
    }

    @Test
    public void hidesAShownPromotionWhenRemoved() {
        assertFalse(Promotions.shown(true));
    }

    @Test
    public void keepsAnAbsentPromotionAbsentWhenShown() {
        PatchSettings.REMOVE_PROMOTIONS.save(false);

        assertFalse(Promotions.shown(false));
    }

    @Test
    public void keepsAShownPromotionWhenNotRemoved() {
        PatchSettings.REMOVE_PROMOTIONS.save(false);

        assertTrue(Promotions.shown(true));
    }

    @Test
    public void dismissesAPromotionWhenRemoved() {
        assertTrue(Promotions.dismissed(false));
    }

    @Test
    public void keepsADismissedPromotionDismissedWhenNotRemoved() {
        PatchSettings.REMOVE_PROMOTIONS.save(false);

        assertTrue(Promotions.dismissed(true));
        assertFalse(Promotions.dismissed(false));
    }

}

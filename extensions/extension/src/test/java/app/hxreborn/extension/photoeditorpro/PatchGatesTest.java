/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.photoeditorpro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import app.morphe.extension.shared.Utils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

@RunWith(RobolectricTestRunner.class)
public final class PatchGatesTest {

    private static final int FOUR_MIB = 4 * 1024 * 1024;

    @Before
    public void attachContext() {
        Utils.setContext(RuntimeEnvironment.getApplication());
    }

    @After
    public void restoreDefaults() {
        PatchSettings.HIDE_ADS.save(true);
        PatchSettings.UNLOCK_PREMIUM.save(true);
    }

    @Test
    public void adsAreHiddenByDefault() {
        assertTrue(PatchGates.hideAds());
    }

    @Test
    public void premiumIsUnlockedByDefault() {
        assertTrue(PatchGates.unlockPremium());
    }

    @Test
    public void hideAdsFollowsItsSetting() {
        PatchSettings.HIDE_ADS.save(false);
        assertFalse(PatchGates.hideAds());
        PatchSettings.HIDE_ADS.save(true);
        assertTrue(PatchGates.hideAds());
    }

    @Test
    public void unlockPremiumFollowsItsSetting() {
        PatchSettings.UNLOCK_PREMIUM.save(false);
        assertFalse(PatchGates.unlockPremium());
        PatchSettings.UNLOCK_PREMIUM.save(true);
        assertTrue(PatchGates.unlockPremium());
    }

    @Test
    public void theTwoGatesAreIndependent() {
        PatchSettings.HIDE_ADS.save(false);
        assertTrue(PatchGates.unlockPremium());
        PatchSettings.HIDE_ADS.save(true);
        PatchSettings.UNLOCK_PREMIUM.save(false);
        assertTrue(PatchGates.hideAds());
    }

    @Test
    public void pollIntervalIsAQuarterOfStock() {
        assertEquals(250L, PatchGates.pollIntervalMs(1000L));
        assertEquals(500L, PatchGates.pollIntervalMs(2000L));
        assertEquals(2L, PatchGates.pollIntervalMs(8L));
    }

    @Test
    public void pollIntervalFloorsAtOneMillisecond() {
        assertEquals(1L, PatchGates.pollIntervalMs(3L));
        assertEquals(1L, PatchGates.pollIntervalMs(4L));
        assertEquals(1L, PatchGates.pollIntervalMs(0L));
        assertEquals(1L, PatchGates.pollIntervalMs(-400L));
    }

    @Test
    public void pollIntervalTruncatesTowardZero() {
        assertEquals(1L, PatchGates.pollIntervalMs(7L));
        assertEquals(2L, PatchGates.pollIntervalMs(11L));
    }

    @Test
    public void pollBudgetIsFourTimesStock() {
        assertEquals(40, PatchGates.pollBudget(10));
        assertEquals(0, PatchGates.pollBudget(0));
        assertEquals(4, PatchGates.pollBudget(1));
    }

    @Test
    public void pollIntervalTimesBudgetEqualsTheStockWindow() {
        // given
        long stockInterval = 2000L;
        int stockBudget = 30;

        // when
        long window = PatchGates.pollIntervalMs(stockInterval) * PatchGates.pollBudget(stockBudget);

        // then
        assertEquals(stockInterval * stockBudget, window);
    }

    @Test
    public void uploadChunkIsRaisedToFourMebibytes() {
        assertEquals(FOUR_MIB, PatchGates.uploadChunkBytes(0));
        assertEquals(FOUR_MIB, PatchGates.uploadChunkBytes(1024 * 1024));
        assertEquals(FOUR_MIB, PatchGates.uploadChunkBytes(FOUR_MIB - 1));
    }

    @Test
    public void uploadChunkKeepsLargerStockValues() {
        assertEquals(FOUR_MIB, PatchGates.uploadChunkBytes(FOUR_MIB));
        assertEquals(FOUR_MIB + 1, PatchGates.uploadChunkBytes(FOUR_MIB + 1));
        assertEquals(Integer.MAX_VALUE, PatchGates.uploadChunkBytes(Integer.MAX_VALUE));
    }

}

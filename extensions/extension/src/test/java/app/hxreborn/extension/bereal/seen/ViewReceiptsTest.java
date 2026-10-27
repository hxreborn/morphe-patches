/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal.seen;

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
public final class ViewReceiptsTest {

    @Before
    public void useApplicationContext() {
        Utils.setContext(RuntimeEnvironment.getApplication());
    }

    @After
    public void restoreDefaults() {
        PatchSettings.DISABLE_VIEW_RECEIPTS.resetToDefault();
    }

    @Test
    public void keepsReportingByDefault() {
        assertFalse(ViewReceipts.skipped());
    }

    @Test
    public void skipsOnceTurnedOn() {
        // given
        PatchSettings.DISABLE_VIEW_RECEIPTS.save(true);

        // when
        final boolean skipped = ViewReceipts.skipped();

        // then
        assertTrue(skipped);
    }

    @Test
    public void reportsAgainOnceTurnedOff() {
        // given
        PatchSettings.DISABLE_VIEW_RECEIPTS.save(true);
        PatchSettings.DISABLE_VIEW_RECEIPTS.save(false);

        // when
        final boolean skipped = ViewReceipts.skipped();

        // then
        assertFalse(skipped);
    }

    @Test
    public void reportsPatchAsNotIncludedUntilApplied() {
        assertFalse(ViewReceipts.isPatchIncluded());
    }

}

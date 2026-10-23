/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.bereal;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class PatchSummaryTest {

    @Test
    public void reportsOnOffAndAppliedCounts() {
        // when
        String status = PatchSummary.status(12, 5, 17);

        // then
        assertEquals("12 on • 5 off • 17 applied", status);
    }

    @Test
    public void countsAlwaysOnRowsAsOn() {
        // when
        int on = PatchSummary.countOn(3, true, false, true);

        // then
        assertEquals(5, on);
    }

    @Test
    public void countsNothingOnWhenEverySwitchIsOff() {
        // when
        int on = PatchSummary.countOn(0, false, false);

        // then
        assertEquals(0, on);
    }

}

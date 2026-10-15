/*
 * Copyright (C) 2026 hxreborn
 * SPDX-License-Identifier: GPL-3.0-only
 */
package app.hxreborn.extension.netanalyzer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class ChartAxisTest {

    private static final double TOLERANCE = 1e-6;

    private static ChartView.Axis axis(double min, double max, int labelCount, boolean forced) {
        final ChartView.Axis axis = new ChartView.Axis();
        axis.setRange(min, max);
        axis.setLabelCount(labelCount, forced);
        return axis;
    }

    @Test
    public void placesIdleSpeedLabelsEveryTwoMegabits() {
        // when
        final double[] entries = axis(0, 1e7, 6, false).entries();

        // then
        assertArrayEquals(new double[] { 0, 2e6, 4e6, 6e6, 8e6, 1e7 }, entries, TOLERANCE);
    }

    @Test
    public void roundsALiveSpeedRangeToWholeTensOfMegabits() {
        // when
        final double[] entries = axis(22.4e6, 72.6e6, 6, false).entries();

        // then
        assertArrayEquals(new double[] { 3e7, 4e7, 5e7, 6e7, 7e7 }, entries, TOLERANCE);
    }

    @Test
    public void spreadsForcedLabelsEvenlyAcrossTheRange() {
        // when
        final double[] entries = axis(0, 150, 15, true).entries();

        // then
        assertEquals(15, entries.length);
        assertEquals(150.0 / 14, entries[1], TOLERANCE);
    }

    @Test
    public void placesPingLabelsEveryTwentyMilliseconds() {
        // when
        final double[] entries = axis(0, 114.2 * 1.05, 6, false).entries();

        // then
        assertArrayEquals(new double[] { 0, 20, 40, 60, 80, 100 }, entries, TOLERANCE);
    }

}
